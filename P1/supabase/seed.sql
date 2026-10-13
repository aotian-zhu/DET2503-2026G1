-- Optional P1 seed for a DEVELOPMENT database using the A2 schema.
-- Create a test auth user first and replace volunteer@example.org below.
-- Fictional operations and approximate Narvik coordinates; not official venue data.
do $$
declare
    volunteer_email text := 'volunteer@example.org';
    volunteer_uuid uuid;
    shift_key text;
    task_prefix text;
    checkpoint_uuid uuid;
    checkpoint_token text;
begin
    select id into volunteer_uuid from auth.users where lower(email) = lower(volunteer_email) limit 1;
    if volunteer_uuid is null then raise exception 'Create the development auth user and set volunteer_email in this script first'; end if;
    shift_key := 'narvik-morning-' || volunteer_uuid::text;
    task_prefix := 'p1-' || volunteer_uuid::text;
    checkpoint_token := 'narvik-p1-test-' || volunteer_uuid::text;

    insert into public.profiles (id, display_name, phone, assigned_function_ids)
    values (volunteer_uuid, 'Narvik Volunteer', '', array['arrival_guide','route_support','logistics_helper'])
    on conflict (id) do update set assigned_function_ids = excluded.assigned_function_ids;

    insert into public.locations (id, name, description, latitude, longitude, scenario_ids) values
      ('volunteer_hub','Volunteer hub','Briefing and coordination near the town centre.',68.4384,17.4272,array['welcome','logistics']),
      ('station','Narvik station','Arrival meeting point for fans coming by train.',68.4417,17.4411,array['welcome','mobility']),
      ('base_camp','Narvikfjellet base','Venue access and visitor support.',68.4324,17.4454,array['welcome','mobility']),
      ('town_square','Town square','Fan information and a city meeting point.',68.4386,17.4278,array['welcome']),
      ('rest_point','Volunteer rest point','Indoor break and supply point.',68.4361,17.4250,array['logistics'])
    on conflict (id) do update set name=excluded.name, description=excluded.description, latitude=excluded.latitude, longitude=excluded.longitude, scenario_ids=excluded.scenario_ids;

    insert into public.shifts (id,volunteer_id,title,start_time,end_time,meeting_point_id)
    values (shift_key,volunteer_uuid,'Morning · Guest services','08:00','14:00','volunteer_hub')
    on conflict (id) do nothing;

    insert into public.tasks (id,volunteer_id,title,details,location_id,latitude,longitude,area,floor,scenario_id,required_permission_ids,due_time,status) values
      (task_prefix||'-01',volunteer_uuid,'Prepare the welcome desk','Lay out visitor maps, check your radio and collect your badge.','volunteer_hub',68.4384,17.4272,'Welcome desk','Ground floor','welcome',array['visitor_checkin'],'08:00','DONE'),
      (task_prefix||'-02',volunteer_uuid,'Welcome arriving fans','Meet the arriving group and guide them to the shuttle meeting point.','station',68.4415,17.4410,'Station forecourt',null,'welcome',array['task_update'],'09:15','IN_PROGRESS'),
      (task_prefix||'-03',volunteer_uuid,'Check the accessible route','Report blocked paths, missing signs or icy surfaces.','base_camp',68.4326,17.4451,'Step-free entrance',null,'mobility',array['incident_report'],'10:30','TODO'),
      (task_prefix||'-04',volunteer_uuid,'Restock the volunteer hub','Collect information packs and drinking water.','rest_point',68.4361,17.4250,'Supply collection','Ground floor','logistics',array['supply_access'],'12:00','TODO')
    on conflict (id) do nothing;

    insert into public.announcements (id,title,body,sender,priority) values
      ('p1-briefing','Your morning briefing','Meet at the volunteer hub and collect a radio before your first assignment.','Operations team','IMPORTANT'),
      ('p1-arctic','A little Arctic preparation','Wear layers and check current Narvik weather before an outdoor task.','Shift coordinator','NORMAL')
    on conflict (id) do nothing;

    insert into public.conversations (id,task_id,coordinator_name)
    values (task_prefix||'-conversation',task_prefix||'-02','Maya · Shift coordinator') on conflict (id) do nothing;
    insert into public.messages (id,conversation_id,sender,body,is_from_volunteer)
    values (task_prefix||'-welcome-message',task_prefix||'-conversation','Maya','Meet the group on the station forecourt. Let me know when you are ready.',false) on conflict (id) do nothing;

    select id into checkpoint_uuid from public.checkpoints where shift_id=shift_key and label='P1 Volunteer hub check-in' limit 1;
    if checkpoint_uuid is null then
      checkpoint_uuid := gen_random_uuid();
      insert into public.checkpoints (id,shift_id,location_id,label,token_hash,active)
      values (checkpoint_uuid,shift_key,'volunteer_hub','P1 Volunteer hub check-in',encode(digest(checkpoint_token,'sha256'),'hex'),true);
    end if;
    raise notice 'Development QR payload: arena-checkin:v1:%:%:%', shift_key, checkpoint_uuid, checkpoint_token;
end $$;
