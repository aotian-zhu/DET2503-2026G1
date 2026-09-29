drop policy if exists "volunteers update own tasks" on public.tasks;

create or replace function public.update_own_task_status(task_id text, new_status text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if new_status not in ('TODO', 'IN_PROGRESS', 'DONE') then
        raise exception 'Invalid task status';
    end if;

    update public.tasks
    set status = new_status
    where id = task_id
      and volunteer_id = auth.uid();

    if not found then
        raise exception 'Task not found or not assigned to current user';
    end if;
end;
$$;

revoke all on function public.update_own_task_status(text, text) from public;
grant execute on function public.update_own_task_status(text, text) to authenticated;

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, display_name, phone, assigned_function_ids)
    values (
        new.id,
        coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), split_part(coalesce(new.email, ''), '@', 1), 'Volunteer'),
        coalesce(new.raw_user_meta_data ->> 'phone', ''),
        '{}'
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();
