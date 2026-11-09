// Ingreso con DNI, celular, usuario o correo + contraseña.
// Busca el correo en el servidor (nunca lo devuelve) y abre la sesión en Supabase Auth.
import { admin, json } from "../_shared/play.ts";

const URL_ = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  const { identifier, password } = await req.json().catch(() => ({}));
  if (!identifier || !password) return json({ error: "invalid_credentials" }, 400);
  const id = String(identifier).trim();
  let email: string | null = null;
  if (id.includes("@")) {
    email = id.toLowerCase();
  } else {
    let digits = id.replace(/\D/g, "");
    if (digits.length === 11 && digits.startsWith("51")) digits = digits.slice(2);
    let q = admin().from("profiles").select("email").limit(1);
    if (/^9\d{8}$/.test(digits)) q = q.eq("phone", digits);
    else if (/^\d{8}$/.test(digits)) q = q.eq("dni", digits);
    else q = q.eq("username", id.toLowerCase());
    const { data } = await q.maybeSingle();
    email = data?.email ?? null;
  }
  if (!email) {
    await new Promise((r) => setTimeout(r, 300));
    return json({ error: "invalid_credentials" }, 400);
  }
  const r = await fetch(`${URL_}/auth/v1/token?grant_type=password`, {
    method: "POST",
    headers: { apikey: ANON, "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  const body = await r.json();
  if (!r.ok) return json({ error: body.error_code ?? "invalid_credentials" }, r.status >= 500 ? 502 : 400);
  return json(body);
});
