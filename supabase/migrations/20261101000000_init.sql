-- Convive · esquema inicial
-- Todas las tablas de public tienen RLS. Sin políticas para anon.
-- Las funciones de ayuda viven en el esquema private (no expuesto por la API).

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;
grant usage on schema private to authenticated;

-- ---------------------------------------------------------------- tipos
create type public.app_role as enum ('SUPER_ADMIN', 'ADMIN', 'TENANT');
create type public.template as enum ('CONDOMINIO', 'CASA_DEPAS');
create type public.plan_cycle as enum ('PAGO', 'ANUAL', 'GRATIS');
create type public.incident_status as enum ('PENDIENTE', 'EN_REVISION', 'RESUELTO');
create type public.emergency_status as enum ('PENDIENTE', 'CONFIRMADA', 'DESCARTADA', 'CERRADA');

-- ---------------------------------------------------------------- tablas
create table public.plans (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  price numeric(10,2) not null default 0 check (price >= 0),
  cycle public.plan_cycle not null,
  full_features boolean not null default false,
  trial boolean not null default false,
  reminder boolean not null default false,
  created_at timestamptz not null default now()
);

create table public.clients (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  template public.template not null,
  monthly_fee numeric(10,2) not null default 0 check (monthly_fee >= 0),
  active boolean not null default true,
  modules text[] not null default '{}',
  late_fee_percent numeric(5,2) not null default 5 check (late_fee_percent >= 0),
  grace_days int not null default 3 check (grace_days >= 0),
  owner_id uuid,
  plan public.plan_cycle not null default 'PAGO',
  plan_id uuid references public.plans(id) on delete set null,
  payment_method text,
  since date not null default current_date,
  created_at timestamptz not null default now()
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null,
  username text not null unique check (username = lower(username)),
  role public.app_role not null,
  client_id uuid references public.clients(id) on delete cascade,
  unit text,
  dni text unique,
  phone text,
  photo_path text,
  cover_path text,
  created_at timestamptz not null default now(),
  check ((role = 'SUPER_ADMIN') = (client_id is null))
);

alter table public.clients
  add constraint clients_owner_fk foreign key (owner_id) references public.profiles(id) on delete set null;

create table public.units (
  client_id uuid not null references public.clients(id) on delete cascade,
  unit text not null,
  responsible text not null,
  primary key (client_id, unit)
);

create table public.charges (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  unit text not null,
  concept text not null,
  amount numeric(10,2) not null check (amount >= 0),
  due date not null,
  paid boolean not null default false,
  paid_on date,
  created_at timestamptz not null default now()
);

create table public.incidents (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  unit text not null,
  service text not null,
  description text not null,
  status public.incident_status not null default 'PENDIENTE',
  confirmed boolean,
  created_at timestamptz not null default now()
);

create table public.notices (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  category text not null,
  title text not null,
  body text not null,
  urgent boolean not null default false,
  photo_path text,
  created_at timestamptz not null default now()
);

create table public.emergencies (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  unit text not null,
  type text not null,
  status public.emergency_status not null default 'PENDIENTE',
  created_at timestamptz not null default now()
);

create table public.meter_requests (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  title text not null,
  due date not null,
  open boolean not null default true,
  created_at timestamptz not null default now()
);

create table public.meter_readings (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  request_id uuid references public.meter_requests(id) on delete set null,
  user_id uuid not null references public.profiles(id) on delete cascade,
  unit text not null,
  photo_path text not null,
  reading text not null default '',
  reviewed boolean not null default false,
  sent_at timestamptz not null default now()
);

create table public.contracts (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  client_id uuid not null references public.clients(id) on delete cascade,
  dni text not null,
  start_date date not null,
  end_date date not null check (end_date > start_date),
  guarantee boolean not null default false,
  guarantee_amount numeric(10,2) not null default 0 check (guarantee_amount >= 0),
  monthly_rent numeric(10,2) not null check (monthly_rent >= 0),
  services text[] not null default '{}',
  renew_requested boolean not null default false,
  renew_requested_at timestamptz,
  since date
);

create table public.subscription_payments (
  id uuid primary key default gen_random_uuid(),
  client_id uuid not null references public.clients(id) on delete cascade,
  period text not null check (period ~ '^\d{4}-\d{2}$'),
  amount numeric(10,2) not null check (amount >= 0),
  paid_at timestamptz not null default now(),
  unique (client_id, period)
);

create table public.support_requests (
  id uuid primary key default gen_random_uuid(),
  from_user_id uuid not null references public.profiles(id) on delete cascade,
  client_id uuid references public.clients(id) on delete cascade,
  message text not null check (length(message) between 1 and 2000),
  read boolean not null default false,
  created_at timestamptz not null default now()
);

-- Índices para las columnas usadas por RLS y consultas frecuentes
create index on public.profiles (client_id);
create index on public.clients (owner_id);
create index on public.clients (plan_id);
create index on public.charges (client_id, unit);
create index on public.incidents (client_id);
create index on public.incidents (user_id);
create index on public.notices (client_id);
create index on public.emergencies (client_id);
create index on public.emergencies (user_id);
create index on public.meter_requests (client_id);
create index on public.meter_readings (client_id);
create index on public.meter_readings (user_id);
create index on public.meter_readings (request_id);
create index on public.contracts (client_id);
create index on public.support_requests (from_user_id);
create index on public.support_requests (client_id);

-- ---------------------------------------------------------------- funciones de ayuda
create or replace function private.my_role() returns public.app_role
language sql stable security definer set search_path = '' as $$
  select role from public.profiles where id = (select auth.uid())
$$;

create or replace function private.my_client() returns uuid
language sql stable security definer set search_path = '' as $$
  select client_id from public.profiles where id = (select auth.uid())
$$;

create or replace function private.my_unit() returns text
language sql stable security definer set search_path = '' as $$
  select unit from public.profiles where id = (select auth.uid())
$$;

create or replace function private.is_super() returns boolean
language sql stable security definer set search_path = '' as $$
  select coalesce((select private.my_role()) = 'SUPER_ADMIN', false)
$$;

create or replace function private.is_admin_of(c uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select coalesce((select private.my_role()) = 'ADMIN' and (select private.my_client()) = c, false)
$$;

create or replace function private.is_member_of(c uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select coalesce((select private.my_client()) = c, false)
$$;

revoke all on all functions in schema private from public, anon;
grant execute on all functions in schema private to authenticated;

-- Ingreso con usuario o DNI. Devuelve el email interno sin revelar si la cuenta existe.
create or replace function public.login_email(identifier text) returns text
language sql stable security definer set search_path = '' as $$
  select coalesce(
    (select u.email from auth.users u
       join public.profiles p on p.id = u.id
      where p.username = lower(trim(identifier)) or p.dni = trim(identifier)
      limit 1),
    'nadie-' || md5(lower(trim(identifier))) || '@users.convive.app'
  )
$$;
revoke all on function public.login_email(text) from public;
grant execute on function public.login_email(text) to anon, authenticated;

-- Nadie (salvo el Super Admin) cambia su propio rol, cliente o departamento,
-- y la administración no puede mover cuentas a otro cliente.
create or replace function private.profiles_guard() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  if private.is_super() then
    return new;
  end if;
  if new.client_id is distinct from old.client_id then
    raise exception 'No puedes cambiar el cliente de una cuenta';
  end if;
  if new.id = (select auth.uid())
     and (new.role is distinct from old.role or new.unit is distinct from old.unit) then
    raise exception 'No puedes cambiar tu propio rol ni tu departamento';
  end if;
  return new;
end $$;

create trigger profiles_guard before update on public.profiles
  for each row execute function private.profiles_guard();

-- ---------------------------------------------------------------- RLS
alter table public.plans enable row level security;
alter table public.clients enable row level security;
alter table public.profiles enable row level security;
alter table public.units enable row level security;
alter table public.charges enable row level security;
alter table public.incidents enable row level security;
alter table public.notices enable row level security;
alter table public.emergencies enable row level security;
alter table public.meter_requests enable row level security;
alter table public.meter_readings enable row level security;
alter table public.contracts enable row level security;
alter table public.subscription_payments enable row level security;
alter table public.support_requests enable row level security;

-- plans: todos los autenticados leen; solo el Super Admin edita
create policy plans_read on public.plans for select to authenticated using (true);
create policy plans_super on public.plans for all to authenticated
  using ((select private.is_super())) with check ((select private.is_super()));

-- clients: miembros leen su cliente; el Super Admin gestiona todos
create policy clients_read on public.clients for select to authenticated
  using ((select private.is_super()) or (select private.is_member_of(id)));
create policy clients_super on public.clients for all to authenticated
  using ((select private.is_super())) with check ((select private.is_super()));

-- profiles: uno mismo, la administración de su cliente y el Super Admin
create policy profiles_read on public.profiles for select to authenticated
  using (id = (select auth.uid()) or (select private.is_super()) or (select private.is_admin_of(client_id)));
create policy profiles_update_self on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));
create policy profiles_admin on public.profiles for all to authenticated
  using ((select private.is_admin_of(client_id)) and role <> 'SUPER_ADMIN')
  with check ((select private.is_admin_of(client_id)) and role <> 'SUPER_ADMIN');
create policy profiles_super on public.profiles for all to authenticated
  using ((select private.is_super())) with check ((select private.is_super()));

-- units
create policy units_read on public.units for select to authenticated
  using ((select private.is_member_of(client_id)));
create policy units_admin on public.units for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- charges: el inquilino ve los de su depa; la administración gestiona
create policy charges_tenant on public.charges for select to authenticated
  using ((select private.is_member_of(client_id)) and unit = (select private.my_unit()));
create policy charges_admin on public.charges for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- incidents: el inquilino crea/lee/confirma los suyos; la administración gestiona
create policy incidents_tenant_read on public.incidents for select to authenticated
  using (user_id = (select auth.uid()));
create policy incidents_tenant_insert on public.incidents for insert to authenticated
  with check (user_id = (select auth.uid()) and (select private.is_member_of(client_id)));
create policy incidents_tenant_update on public.incidents for update to authenticated
  using (user_id = (select auth.uid()))
  with check (user_id = (select auth.uid()) and (select private.is_member_of(client_id)));
create policy incidents_admin on public.incidents for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- notices: miembros leen; la administración publica
create policy notices_read on public.notices for select to authenticated
  using ((select private.is_member_of(client_id)));
create policy notices_admin on public.notices for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- emergencies: el inquilino crea y ve las suyas; todos ven las confirmadas
create policy emergencies_read on public.emergencies for select to authenticated
  using (user_id = (select auth.uid())
         or ((select private.is_member_of(client_id)) and status = 'CONFIRMADA'));
create policy emergencies_insert on public.emergencies for insert to authenticated
  with check (user_id = (select auth.uid()) and status = 'PENDIENTE'
              and (select private.is_member_of(client_id)));
create policy emergencies_admin on public.emergencies for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- meter_requests
create policy meter_requests_read on public.meter_requests for select to authenticated
  using ((select private.is_member_of(client_id)));
create policy meter_requests_admin on public.meter_requests for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- meter_readings
create policy meter_readings_tenant_read on public.meter_readings for select to authenticated
  using (user_id = (select auth.uid()));
create policy meter_readings_tenant_insert on public.meter_readings for insert to authenticated
  with check (user_id = (select auth.uid()) and reviewed = false and (select private.is_member_of(client_id)));
create policy meter_readings_admin on public.meter_readings for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- contracts: solo el inquilino y su administración (el Super Admin NO)
create policy contracts_tenant on public.contracts for select to authenticated
  using (user_id = (select auth.uid()));
create policy contracts_admin on public.contracts for all to authenticated
  using ((select private.is_admin_of(client_id))) with check ((select private.is_admin_of(client_id)));

-- subscription_payments: el Super Admin gestiona; la administración ve los de su cliente
create policy sub_payments_admin_read on public.subscription_payments for select to authenticated
  using ((select private.is_admin_of(client_id)));
create policy sub_payments_super on public.subscription_payments for all to authenticated
  using ((select private.is_super())) with check ((select private.is_super()));

-- support_requests: la administración crea y ve los suyos; el Super Admin gestiona
create policy support_admin_read on public.support_requests for select to authenticated
  using (from_user_id = (select auth.uid()));
create policy support_admin_insert on public.support_requests for insert to authenticated
  with check (from_user_id = (select auth.uid()) and (select private.my_role()) = 'ADMIN'
              and client_id = (select private.my_client()));
create policy support_super on public.support_requests for all to authenticated
  using ((select private.is_super())) with check ((select private.is_super()));
