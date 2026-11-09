// Recuperar contraseña con código de 6 dígitos.
// action "send":   { email } o { phone } → busca la cuenta y Supabase envía el código al correo.
// action "verify": { email | phone, token } → valida el código y devuelve una sesión temporal
//                  para guardar la nueva contraseña (PUT /auth/v1/user).
// El celular no recibe SMS todavía: el código va al correo de esa cuenta (se muestra enmascarado).
import { admin, json } from "../_shared/play.ts";

const URL_ = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;

const mask = (email: string) => {
  const [user, domain] = email.split("@");
  if (!domain) return email;
  const head = user.slice(0, 1);
  const tail = user.length > 3 ? user.slice(-1) : "";
  return `${head}${"•".repeat(Math.max(3, user.length - head.length - tail.length))}${tail}@${domain}`;
};

async function findEmail(email?: string, phone?: string): Promise<string | null> {
  const db = admin();
  if (email) {
    const { data } = await db.from("profiles").select("email").eq("email", email.trim().toLowerCase()).limit(1).maybeSingle();
    return data?.email ?? null;
  }
  let d = String(phone ?? "").replace(/\D/g, "");
  if (d.length === 11 && d.startsWith("51")) d = d.slice(2);
  if (!/^9\d{8}$/.test(d)) return null;
  const { data } = await db.from("profiles").select("email").eq("phone", d).limit(1).maybeSingle();
  return data?.email ?? null;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const { action, email, phone, token } = await req.json().catch(() => ({}));
  const target = await findEmail(email, phone);
  if (!target) {
    await new Promise((r) => setTimeout(r, 300));
    return json({ error: "not_found" }, 404);
  }

  if (action === "verify") {
    if (!/^\d{6,10}$/.test(String(token ?? ""))) return json({ error: "invalid_code" }, 400);
    const r = await fetch(`${URL_}/auth/v1/verify`, {
      method: "POST",
      headers: { apikey: ANON, "Content-Type": "application/json" },
      body: JSON.stringify({ type: "recovery", email: target, token: String(token) }),
    });
    const body = await r.json().catch(() => ({}));
    if (!r.ok) return json({ error: r.status === 429 ? "rate_limited" : "invalid_code" }, r.status === 429 ? 429 : 400);
    return json(body);
  }

  const r = await fetch(`${URL_}/auth/v1/recover`, {
    method: "POST",
    headers: { apikey: ANON, "Content-Type": "application/json" },
    body: JSON.stringify({ email: target }),
  });
  if (r.status === 429) return json({ error: "rate_limited" }, 429);
  if (!r.ok) return json({ error: "send_failed", detail: await r.text() }, 502);
  return json({ sent: true, destination: email ? target : mask(target) });
});
