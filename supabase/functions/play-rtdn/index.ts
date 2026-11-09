// Notificaciones en tiempo real de Google Play (Pub/Sub push): renovaciones, cancelaciones, pagos fallidos.
// URL de la suscripción push: https://<proyecto>.supabase.co/functions/v1/play-rtdn?secret=<RTDN_SECRET>
import { admin, getSubscription, googleAccessToken, json, mapSubscription, saveState } from "../_shared/play.ts";

Deno.serve(async (req) => {
  const secret = Deno.env.get("RTDN_SECRET");
  if (!secret) return json({ error: "not_configured" }, 503);
  if (new URL(req.url).searchParams.get("secret") !== secret) return json({ error: "forbidden" }, 403);
  const body = await req.json().catch(() => null);
  const data = body?.message?.data;
  if (!data) return json({ ok: true });
  const msg = JSON.parse(atob(data));
  const n = msg.subscriptionNotification;
  if (!n?.purchaseToken) return json({ ok: true });

  const db = admin();
  const { data: row } = await db.from("subscriptions").select("client_id").eq("purchase_token", n.purchaseToken).maybeSingle();
  if (!row) return json({ ok: true, unknown: true });
  const token = await googleAccessToken();
  if (!token) return json({ error: "not_configured" }, 503);
  try {
    const sub = await getSubscription(token, n.purchaseToken);
    await saveState(db, row.client_id, n.purchaseToken, mapSubscription(sub), sub);
    return json({ ok: true });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
