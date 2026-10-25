-- 0001_init.sql — optional server schema for Hamghadam (Supabase / Postgres 15+)
--
-- Rules this file follows (see docs/12-server-and-agent.fa.md):
--   * Every user-owned row carries user_id and is protected by RLS.
--   * Timestamps are timestamptz (UTC on the wire), never local time.
--   * Deletes are soft (deleted_at); nothing is physically removed by the client.
--   * The table set is a *mirror* for sync/agent auditing. The device database
--     remains authoritative when offline; the server never invents task ids.
--   * Re-runnable: safe to apply twice in local development.

begin;

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- profiles: one row per auth user
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
  id            uuid primary key references auth.users (id) on delete cascade,
  display_name  text not null default '' check (char_length(display_name) <= 80),
  locale        text not null default 'fa' check (locale in ('fa', 'en')),
  timezone      text not null default 'Asia/Tehran' check (char_length(timezone) between 3 and 64),
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- devices: one row per installed app instance (used by the sync cursor later)
-- ---------------------------------------------------------------------------
create table if not exists public.devices (
  id            uuid primary key default gen_random_uuid(),
  user_id       uuid not null references auth.users (id) on delete cascade,
  label         text not null default '' check (char_length(label) <= 80),
  platform      text not null default 'android' check (platform in ('android')),
  app_version   text not null default '' check (char_length(app_version) <= 32),
  last_seen_at  timestamptz not null default now(),
  created_at    timestamptz not null default now()
);

create index if not exists devices_user_idx on public.devices (user_id, last_seen_at desc);

-- ---------------------------------------------------------------------------
-- tasks: server mirror of the local task list
-- ---------------------------------------------------------------------------
create table if not exists public.tasks (
  id               uuid primary key default gen_random_uuid(),
  user_id          uuid not null references auth.users (id) on delete cascade,
  device_id        uuid references public.devices (id) on delete set null,
  title            text not null check (char_length(title) between 1 and 500),
  notes            text not null default '' check (char_length(notes) <= 20000),
  due_at           timestamptz,
  all_day          boolean not null default false,
  priority         smallint not null default 0 check (priority between 0 and 3),
  status           text not null default 'open'
                     check (status in ('open', 'doing', 'blocked', 'done', 'cancelled')),
  starred          boolean not null default false,
  subtasks         jsonb not null default '[]'::jsonb check (jsonb_typeof(subtasks) = 'array'),
  revision         integer not null default 1 check (revision >= 1),
  idempotency_key  text check (idempotency_key is null or char_length(idempotency_key) between 8 and 128),
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  deleted_at       timestamptz
);

-- One outbox retry must never create a second task.
create unique index if not exists tasks_user_idempotency_idx
  on public.tasks (user_id, idempotency_key)
  where idempotency_key is not null;

create index if not exists tasks_user_updated_idx on public.tasks (user_id, updated_at desc);
create index if not exists tasks_user_due_idx on public.tasks (user_id, due_at) where deleted_at is null;

-- ---------------------------------------------------------------------------
-- agent_runs: audit trail for assistant proposals
-- Stores a SHA-256 digest of the prompt, never the raw text.
-- ---------------------------------------------------------------------------
create table if not exists public.agent_runs (
  id              bigint generated always as identity primary key,
  user_id         uuid not null references auth.users (id) on delete cascade,
  model           text not null default '' check (char_length(model) <= 64),
  prompt_digest   text not null check (prompt_digest ~ '^[a-f0-9]{64}$'),
  action_count    integer not null default 0 check (action_count >= 0),
  rejected_count  integer not null default 0 check (rejected_count >= 0),
  tool_names      jsonb not null default '[]'::jsonb check (jsonb_typeof(tool_names) = 'array'),
  created_at      timestamptz not null default now()
);

create index if not exists agent_runs_user_idx on public.agent_runs (user_id, created_at desc);

-- ---------------------------------------------------------------------------
-- updated_at maintenance
-- ---------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

drop trigger if exists tasks_set_updated_at on public.tasks;
create trigger tasks_set_updated_at
  before update on public.tasks
  for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- row level security
-- ---------------------------------------------------------------------------
alter table public.profiles   enable row level security;
alter table public.devices    enable row level security;
alter table public.tasks      enable row level security;
alter table public.agent_runs enable row level security;

drop policy if exists profiles_own_select on public.profiles;
create policy profiles_own_select on public.profiles
  for select to authenticated using ((select auth.uid()) = id);

drop policy if exists profiles_own_insert on public.profiles;
create policy profiles_own_insert on public.profiles
  for insert to authenticated with check ((select auth.uid()) = id);

drop policy if exists profiles_own_update on public.profiles;
create policy profiles_own_update on public.profiles
  for update to authenticated
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

drop policy if exists devices_own_all on public.devices;
create policy devices_own_all on public.devices
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists tasks_own_all on public.tasks;
create policy tasks_own_all on public.tasks
  for all to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- No policies on agent_runs for anon/authenticated: only the service role
-- (which bypasses RLS) may write or read the audit trail.
revoke all on public.agent_runs from anon, authenticated;
grant select, insert, update, delete on public.profiles, public.devices, public.tasks to authenticated;
grant all on public.agent_runs to service_role;
grant usage, select on all sequences in schema public to service_role;

comment on table public.tasks is
  'Server mirror of the local task list. deleted_at = soft delete; the client never hard-deletes.';
comment on column public.tasks.idempotency_key is
  'Outbox key from the device; unique per user so retries cannot duplicate a task.';
comment on table public.agent_runs is
  'AI proposal audit: SHA-256 of the prompt, tool names, counts. Written by the API service role only.';

commit;
