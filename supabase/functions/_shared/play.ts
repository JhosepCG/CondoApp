// Google Play Developer API: verificación de suscripciones de Convive.
// Necesita el secreto GOOGLE_PLAY_SERVICE_ACCOUNT (JSON de una cuenta de servicio con acceso
// a «Ver datos financieros» y «Administrar pedidos y suscripciones» en Play Console).
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

export const PACKAGE = Deno.env.get("PLAY_PACKAGE_NAME") ?? "pe.condoapp";

export const admin = () =>
  createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });

export const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

const b64url = (buf: ArrayBuffer | Uint8Array) =>
  btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/=+$/, "").replace(/\+/g, "-").replace(/\//g, "_");
const encJson = (o: unknown) => b64url(new TextEncoder().encode(JSON.stringify(o)));

let cached: { token: string; exp: number } | null = null;

/** Token OAuth de la cuenta de servicio (JWT firmado con RS256). Devuelve null si falta el secreto. */
export async function googleAccessToken(): Promise<string | null> {
  const raw = Deno.env.get("GOOGLE_PLAY_SERVICE_ACCOUNT");
  if (!raw) return null;
  if (cached && cached.exp > Date.now() + 60_000) return cached.token;
  const sa = JSON.parse(raw);
  const der = Uint8Array.from(
    atob(String(sa.private_key).replace(/-----[^-]+-----/g, "").replace(/\s+/g, "")),
    (c) => c.charCodeAt(0),
  );
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const iat = Math.floor(Date.now() / 1000);
  const unsigned = encJson({ alg: "RS256", typ: "JWT" }) + "." + encJson({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token",
    iat,
    exp: iat + 3600,
  });
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned));
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: unsigned + "." + b64url(sig),
    }),
  });
  const body = await r.json();
  if (!r.ok) throw new Error("google_token: " + JSON.stringify(body));
  cached = { token: body.access_token, exp: Date.now() + body.expires_in * 1000 };
  return cached.token;
}

const API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications";

export async function getSubscription(token: string, purchaseToken: string) {
  const r = await fetch(`${API}/${PACKAGE}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const body = await r.json();
  if (!r.ok) throw new Error("play_get: " + JSON.stringify(body));
  return body;
}

export async function acknowledge(token: string, productId: string, purchaseToken: string) {
  await fetch(`${API}/${PACKAGE}/purchases/subscriptions/${productId}/tokens/${encodeURIComponent(purchaseToken)}:acknowledge`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: "{}",
  });
}

/** Convierte la respuesta de Google en el estado de Convive. */
export function mapSubscription(sub: any) {
  const item = (sub.lineItems ?? [])[0] ?? {};
  const basePlan: string = item.offerDetails?.basePlanId ?? "";
  const offerId: string = item.offerDetails?.offerId ?? "";
  const inTrial = !!item.offerPhase?.freeTrial || /prueba|trial/i.test(offerId);
  const status = (() => {
    switch (sub.subscriptionState) {
      case "SUBSCRIPTION_STATE_ACTIVE": return inTrial ? "prueba" : "activa";
      case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      case "SUBSCRIPTION_STATE_ON_HOLD":
      case "SUBSCRIPTION_STATE_PAUSED":
      case "SUBSCRIPTION_STATE_PENDING": return "pago_pendiente";
      case "SUBSCRIPTION_STATE_CANCELED": return "cancelacion_programada";
      case "SUBSCRIPTION_STATE_EXPIRED": return "vencida";
      case "SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED": return "cancelada";
      default: return "pago_pendiente";
    }
  })();
  return {
    status,
    plan_id: /anual|year|annual/i.test(basePlan) ? "anual" : "mensual",
    product_id: item.productId ?? null,
    base_plan_id: basePlan || null,
    order_id: sub.latestOrderId ?? null,
    current_period_end: item.expiryTime ?? null,
    trial_ends_at: inTrial ? item.expiryTime ?? null : null,
    auto_renewing: item.autoRenewingPlan?.autoRenewEnabled ?? null,
    account: sub.externalAccountIdentifiers?.obfuscatedExternalAccountId ?? null,
    needs_ack: sub.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING",
  };
}

/** Guarda el estado en subscriptions y refleja el plan en clients. */
export async function saveState(db: ReturnType<typeof admin>, clientId: string, purchaseToken: string, m: ReturnType<typeof mapSubscription>, raw: unknown) {
  const { error } = await db.from("subscriptions").update({
    store: "google_play",
    status: m.status,
    plan_id: m.plan_id,
    product_id: m.product_id,
    base_plan_id: m.base_plan_id,
    purchase_token: purchaseToken,
    order_id: m.order_id,
    current_period_end: m.current_period_end,
    trial_ends_at: m.trial_ends_at,
    auto_renewing: m.auto_renewing,
    raw,
    updated_at: new Date().toISOString(),
  }).eq("client_id", clientId);
  if (error) throw error;
  const plan = m.status === "prueba" ? "GRATIS" : m.plan_id === "anual" ? "ANUAL" : "PAGO";
  await db.from("clients").update({ plan, active: !["vencida", "cancelada"].includes(m.status) }).eq("id", clientId);
}
