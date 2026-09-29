do $$
declare
    volunteer_uuid uuid;
begin
    select id into volunteer_uuid
    from auth.users
    where lower(email) = lower('3059682344@qq.com')
    limit 1;

    if volunteer_uuid is null then
        raise exception 'Test user 3059682344@qq.com was not found in Authentication > Users';
    end if;

    insert into public.profiles (id, display_name, phone, assigned_function_ids)
    values (
        volunteer_uuid,
        'Test Volunteer',
        '',
        array['guest-service']
    )
    on conflict (id) do update set
        display_name = excluded.display_name,
        assigned_function_ids = excluded.assigned_function_ids;

    insert into public.locations (id, name, description, latitude, longitude, scenario_ids)
    values
        ('main-entrance', 'Main Entrance', 'Primary visitor entrance and volunteer meeting point.', 59.911491, 10.757933, array['visitor-guidance']),
        ('information-desk', 'Information Desk', 'Visitor support desk inside the main hall.', 59.911850, 10.758420, array['visitor-guidance']),
        ('arena-section-a', 'Arena Section A', 'Seating and crowd assistance area.', 59.912120, 10.757400, array['crowd-support'])
    on conflict (id) do update set
        name = excluded.name,
        description = excluded.description,
        latitude = excluded.latitude,
        longitude = excluded.longitude,
        scenario_ids = excluded.scenario_ids;

    insert into public.shifts (id, volunteer_id, title, start_time, end_time, meeting_point_id)
    values ('shift-test-001', volunteer_uuid, 'Morning Event Shift', '08:00', '14:00', 'main-entrance')
    on conflict (id) do update set
        volunteer_id = excluded.volunteer_id,
        title = excluded.title,
        start_time = excluded.start_time,
        end_time = excluded.end_time,
        meeting_point_id = excluded.meeting_point_id;

    insert into public.tasks (
        id, volunteer_id, title, details, location_id, latitude, longitude,
        area, floor, scenario_id, required_permission_ids, due_time, status
    )
    values
        ('task-test-001', volunteer_uuid, 'Prepare the information desk', 'Check signs and visitor materials before opening.', 'information-desk', 59.911850, 10.758420, 'Main Hall', 'Ground floor', 'visitor-guidance', array[]::text[], '08:30', 'TODO'),
        ('task-test-002', volunteer_uuid, 'Assist arriving visitors', 'Guide visitors from the entrance to the correct arena section.', 'main-entrance', 59.911491, 10.757933, 'Entrance Zone', null, 'visitor-guidance', array[]::text[], '09:00', 'TODO'),
        ('task-test-003', volunteer_uuid, 'Check Section A', 'Report crowding, accessibility or safety issues in Section A.', 'arena-section-a', 59.912120, 10.757400, 'Arena A', 'Level 1', 'crowd-support', array[]::text[], '10:30', 'TODO')
    on conflict (id) do update set
        volunteer_id = excluded.volunteer_id,
        title = excluded.title,
        details = excluded.details,
        location_id = excluded.location_id,
        latitude = excluded.latitude,
        longitude = excluded.longitude,
        area = excluded.area,
        floor = excluded.floor,
        scenario_id = excluded.scenario_id,
        required_permission_ids = excluded.required_permission_ids,
        due_time = excluded.due_time;

    insert into public.announcements (id, title, body, sender, sent_at, priority)
    values
        ('announcement-test-001', 'Welcome to today''s shift', 'Please check in at the main entrance before beginning your first task.', 'Shift coordinator', now() - interval '20 minutes', 'IMPORTANT'),
        ('announcement-test-002', 'Section A update', 'Expect increased visitor traffic around Section A after 10:00.', 'Operations team', now() - interval '5 minutes', 'NORMAL')
    on conflict (id) do update set
        title = excluded.title,
        body = excluded.body,
        sender = excluded.sender,
        sent_at = excluded.sent_at,
        priority = excluded.priority;

    insert into public.conversations (id, task_id, coordinator_name)
    values ('conversation-test-001', 'task-test-001', 'Alex – Shift coordinator')
    on conflict (id) do update set
        task_id = excluded.task_id,
        coordinator_name = excluded.coordinator_name;

    insert into public.messages (id, conversation_id, sender, body, sent_at, is_from_volunteer, sender_id)
    values (
        'message-test-001',
        'conversation-test-001',
        'Alex – Shift coordinator',
        'Please let me know when the information desk is ready.',
        now() - interval '10 minutes',
        false,
        null
    )
    on conflict (id) do update set
        body = excluded.body,
        sent_at = excluded.sent_at;

    insert into public.checkpoints (id, shift_id, location_id, label, token_hash, active)
    values (
        '11111111-1111-4111-8111-111111111111'::uuid,
        'shift-test-001',
        'main-entrance',
        'Main Entrance Check-in',
        encode(digest('volunteer-test-checkin-2026', 'sha256'), 'hex'),
        true
    )
    on conflict (id) do update set
        shift_id = excluded.shift_id,
        location_id = excluded.location_id,
        label = excluded.label,
        token_hash = excluded.token_hash,
        active = excluded.active;
end
$$;

select
    u.email,
    p.display_name,
    s.title as shift_title,
    count(distinct t.id) as task_count
from auth.users u
join public.profiles p on p.id = u.id
left join public.shifts s on s.volunteer_id = u.id
left join public.tasks t on t.volunteer_id = u.id
where lower(u.email) = lower('3059682344@qq.com')
group by u.email, p.display_name, s.title;
