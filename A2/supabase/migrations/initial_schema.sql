create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null,
    phone text not null default '',
    assigned_function_ids text[] not null default '{}'
);

create table public.locations (
    id text primary key,
    name text not null,
    description text not null,
    latitude double precision not null,
    longitude double precision not null,
    scenario_ids text[] not null default '{}'
);

create table public.shifts (
    id text primary key,
    volunteer_id uuid not null references public.profiles(id) on delete cascade,
    title text not null,
    start_time text not null,
    end_time text not null,
    meeting_point_id text not null references public.locations(id)
);

create table public.tasks (
    id text primary key,
    volunteer_id uuid not null references public.profiles(id) on delete cascade,
    title text not null,
    details text not null,
    location_id text not null references public.locations(id),
    latitude double precision not null,
    longitude double precision not null,
    area text not null,
    floor text,
    scenario_id text not null,
    required_permission_ids text[] not null default '{}',
    due_time text not null,
    status text not null default 'TODO' check (status in ('TODO', 'IN_PROGRESS', 'DONE'))
);

create table public.announcements (
    id text primary key,
    title text not null,
    body text not null,
    sender text not null,
    sent_at timestamptz not null default now(),
    priority text not null default 'NORMAL' check (priority in ('NORMAL', 'IMPORTANT', 'URGENT'))
);

create table public.announcement_reads (
    announcement_id text not null references public.announcements(id) on delete cascade,
    volunteer_id uuid not null references public.profiles(id) on delete cascade,
    primary key (announcement_id, volunteer_id)
);

create table public.conversations (
    id text primary key,
    task_id text not null references public.tasks(id) on delete cascade,
    coordinator_name text not null
);

create table public.messages (
    id text primary key,
    conversation_id text not null references public.conversations(id) on delete cascade,
    sender text not null,
    body text not null,
    sent_at timestamptz not null default now(),
    is_from_volunteer boolean not null default false
);

alter table public.profiles enable row level security;
alter table public.locations enable row level security;
alter table public.shifts enable row level security;
alter table public.tasks enable row level security;
alter table public.announcements enable row level security;
alter table public.announcement_reads enable row level security;
alter table public.conversations enable row level security;
alter table public.messages enable row level security;

create policy "volunteers read own profile" on public.profiles for select using (auth.uid() = id);
create policy "volunteers update own profile" on public.profiles for update using (auth.uid() = id);
create policy "authenticated users read locations" on public.locations for select to authenticated using (true);
create policy "volunteers read own shifts" on public.shifts for select using (auth.uid() = volunteer_id);
create policy "volunteers read own tasks" on public.tasks for select using (auth.uid() = volunteer_id);
create policy "volunteers update own tasks" on public.tasks for update using (auth.uid() = volunteer_id) with check (auth.uid() = volunteer_id);
create policy "authenticated users read announcements" on public.announcements for select to authenticated using (true);
create policy "volunteers manage own announcement reads" on public.announcement_reads for all using (auth.uid() = volunteer_id) with check (auth.uid() = volunteer_id);
create policy "volunteers read own conversations" on public.conversations for select using (exists (select 1 from public.tasks where tasks.id = conversations.task_id and tasks.volunteer_id = auth.uid()));
create policy "volunteers read conversation messages" on public.messages for select using (exists (select 1 from public.conversations join public.tasks on tasks.id = conversations.task_id where conversations.id = messages.conversation_id and tasks.volunteer_id = auth.uid()));
create policy "volunteers send conversation messages" on public.messages for insert with check (is_from_volunteer and exists (select 1 from public.conversations join public.tasks on tasks.id = conversations.task_id where conversations.id = messages.conversation_id and tasks.volunteer_id = auth.uid()));
