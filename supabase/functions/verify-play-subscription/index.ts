// La app envía el token de compra de Google Play; aquí se verifica con Google y se guarda el estado.
import { acknowledge, admin, getSubscription, googleAccessToken, json, mapSubscription, saveState } from "../_shared/play.ts";

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const db = admin();
  const jwt = (req.headers.get("Authorization") ?? "").replace("Bearer ", "");
  const { data: auth } = await db.auth.getUser(jwt);
  const uid = auth.user?.id;
  if (!uid) return json({ error: "unauthorized" }, 401);
  const { purchaseToken, productId } = await req.json().catch(() => ({}));
  if (!purchaseToken || !productId) return json({ error: "missing_token" }, 400);

  const { data: client } = await db.from("clients").select("id").eq("owner_id", uid).order("created_at", { ascending: false }).limit(1).maybeSingle();
  if (!client) return json({ error: "no_property" }, 404);

  const { data: other } = await db.from("subscriptions").select("client_id").eq("purchase_token", purchaseToken).maybeSingle();
  if (other && other.client_id !== client.id) return json({ error: "token_in_use" }, 409);

  const token = await googleAccessToken();
  if (!token) return json({ error: "not_configured" }, 503);
  try {
    const sub = await getSubscription(token, purchaseToken);
    const m = mapSubscription(sub);
    if (m.account && m.account !== uid) return json({ error: "account_mismatch" }, 403);
    await saveState(db, client.id, purchaseToken, m, sub);
    if (m.needs_ack) await acknowledge(token, productId, purchaseToken);
    return json({ status: m.status, plan_id: m.plan_id, current_period_end: m.current_period_end, trial_ends_at: m.trial_ends_at });
  } catch (e) {
    return json({ error: "play_error", detail: String(e) }, 502);
  }
});
