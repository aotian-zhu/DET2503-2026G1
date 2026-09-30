create extension if not exists pgcrypto;

alter table public.messages add column if not exists sender_id uuid references auth.users(id);
alter table public.messages alter column id set default gen_random_uuid()::text;

create table public.checkpoints (
    id uuid primary key default gen_random_uuid(),
    shift_id text not null references public.shifts(id) on delete cascade,
    location_id text references public.locations(id),
    label text not null,
    token_hash text not null unique,
    active boolean not null default true
);

create table public.check_ins (
    id uuid primary key default gen_random_uuid(),
    volunteer_id uuid not null references public.profiles(id) on delete cascade,
    shift_id text not null references public.shifts(id) on delete cascade,
    checkpoint_id uuid not null references public.checkpoints(id) on delete cascade,
    checked_in_at timestamptz not null default now(),
    unique (volunteer_id, shift_id, checkpoint_id)
);

create table public.incidents (
    id uuid primary key default gen_random_uuid(),
    volunteer_id uuid not null references public.profiles(id) on delete cascade,
    category text not null check (category in ('CROWD','SAFETY','MEDICAL','SIGNAGE','ACCESSIBILITY','OTHER')),
    description text not null check (char_length(description) between 3 and 2000),
    location_id text references public.locations(id),
    task_id text references public.tasks(id),
    photo_path text,
    submitted_at timestamptz not null default now()
);

alter table public.checkpoints enable row level security;
alter table public.check_ins enable row level security;
alter table public.incidents enable row level security;
create policy "volunteers read assigned checkpoints" on public.checkpoints for select to authenticated using (exists (select 1 from public.shifts s where s.id = checkpoints.shift_id and s.volunteer_id = auth.uid()));
create policy "volunteers read own checkins" on public.check_ins for select to authenticated using (volunteer_id = auth.uid());
create policy "volunteers read own incidents" on public.incidents for select to authenticated using (volunteer_id = auth.uid());

create or replace function public.mark_announcement_read(announcement_id text) returns void language plpgsql security definer set search_path = public as $$
begin
  if not exists (select 1 from public.announcements a where a.id = announcement_id) then raise exception 'Announcement not found'; end if;
  insert into public.announcement_reads(announcement_id, volunteer_id) values (announcement_id, auth.uid()) on conflict do nothing;
end $$;

create or replace function public.send_task_message(task_id text, message_body text) returns text language plpgsql security definer set search_path = public as $$
declare conversation_id text; display_name text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if char_length(trim(message_body)) not between 1 and 2000 then raise exception 'Message must contain 1-2000 characters'; end if;
  if not exists (select 1 from public.tasks t where t.id = task_id and t.volunteer_id = auth.uid()) then raise exception 'Task not assigned to current user'; end if;
  select c.id into conversation_id from public.conversations c where c.task_id = send_task_message.task_id limit 1;
  if conversation_id is null then
    conversation_id := gen_random_uuid()::text;
    insert into public.conversations(id, task_id, coordinator_name) values (conversation_id, task_id, 'Shift coordinator');
  end if;
  select p.display_name into display_name from public.profiles p where p.id = auth.uid();
  insert into public.messages(conversation_id, sender, sender_id, body, is_from_volunteer) values (conversation_id, coalesce(display_name, 'Volunteer'), auth.uid(), trim(message_body), true);
  return conversation_id;
end $$;

-- 唯一约束配合重复查询实现幂等签到：重复扫码返回原记录，而不是制造第二次签到。
create or replace function public.check_in_with_qr(qr_payload text) returns table(id text, checked_in_at text, duplicate boolean) language plpgsql security definer set search_path = public as $$
declare payload_parts text[]; checkpoint uuid; shift text; existing public.check_ins; inserted public.check_ins;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  payload_parts := string_to_array(qr_payload, ':');
  if array_length(payload_parts,1) <> 5 or payload_parts[1] <> 'arena-checkin' or payload_parts[2] <> 'v1' then raise exception 'Invalid check-in QR code'; end if;
  shift := payload_parts[3]; checkpoint := payload_parts[4]::uuid;
  if not exists (select 1 from public.checkpoints cp join public.shifts s on s.id=cp.shift_id where cp.id=checkpoint and cp.shift_id=shift and cp.active and cp.token_hash=encode(digest(payload_parts[5], 'sha256'),'hex') and s.volunteer_id=auth.uid()) then raise exception 'QR code is invalid or not assigned to your shift'; end if;
  select * into existing from public.check_ins ci where ci.volunteer_id=auth.uid() and ci.shift_id=shift and ci.checkpoint_id=checkpoint;
  if found then return query select existing.id::text, existing.checked_in_at::text, true; return; end if;
  insert into public.check_ins(volunteer_id,shift_id,checkpoint_id) values(auth.uid(),shift,checkpoint) returning * into inserted;
  return query select inserted.id::text, inserted.checked_in_at::text, false;
end $$;

-- RPC 不信任客户端关联：任务必须属于本人，照片路径首段也必须是当前用户 ID。
create or replace function public.submit_incident(incident_category text, incident_description text, incident_location_id text default null, incident_task_id text default null, incident_photo_path text default null) returns table(id text, submitted_at text, photo_path text) language plpgsql security definer set search_path = public as $$
declare created public.incidents;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if incident_category not in ('CROWD','SAFETY','MEDICAL','SIGNAGE','ACCESSIBILITY','OTHER') then raise exception 'Invalid category'; end if;
  if char_length(trim(incident_description)) not between 3 and 2000 then raise exception 'Description must contain 3-2000 characters'; end if;
  if incident_task_id is not null and not exists(select 1 from public.tasks t where t.id=incident_task_id and t.volunteer_id=auth.uid()) then raise exception 'Task not assigned to current user'; end if;
  if incident_location_id is not null and not exists(select 1 from public.locations l where l.id=incident_location_id) then raise exception 'Location not found'; end if;
  if incident_photo_path is not null and split_part(incident_photo_path,'/',1) <> auth.uid()::text then raise exception 'Invalid photo path'; end if;
  insert into public.incidents(volunteer_id,category,description,location_id,task_id,photo_path) values(auth.uid(),incident_category,trim(incident_description),incident_location_id,incident_task_id,incident_photo_path) returning * into created;
  return query select created.id::text, created.submitted_at::text, created.photo_path;
end $$;

revoke all on function public.mark_announcement_read(text) from public;
revoke all on function public.send_task_message(text,text) from public;
revoke all on function public.check_in_with_qr(text) from public;
revoke all on function public.submit_incident(text,text,text,text,text) from public;
grant execute on function public.mark_announcement_read(text), public.send_task_message(text,text), public.check_in_with_qr(text), public.submit_incident(text,text,text,text,text) to authenticated;

-- 私有 bucket 与目录所有权策略共同保证志愿者只能管理自己 ID 目录下的照片。
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values('incident-photos','incident-photos',false,5242880,array['image/jpeg','image/png','image/webp']) on conflict(id) do update set public=false,file_size_limit=excluded.file_size_limit,allowed_mime_types=excluded.allowed_mime_types;
create policy "volunteers upload own incident photos" on storage.objects for insert to authenticated with check (bucket_id='incident-photos' and (storage.foldername(name))[1]=auth.uid()::text);
create policy "volunteers read own incident photos" on storage.objects for select to authenticated using (bucket_id='incident-photos' and (storage.foldername(name))[1]=auth.uid()::text);
create policy "volunteers delete own incident photos" on storage.objects for delete to authenticated using (bucket_id='incident-photos' and (storage.foldername(name))[1]=auth.uid()::text);
