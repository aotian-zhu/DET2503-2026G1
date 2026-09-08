# Arena Companion
## Detailed Product Document for Mandatory Assignment A1

## 1. Product Overview

Arena Companion is a proposed Android mobile application for volunteers working in and around sports and event arenas within the VM 2029 ecosystem. The product is designed as an operational support tool rather than a general information app. Its purpose is to help volunteers understand their role, locate their assigned areas, complete tasks, report incidents, and stay aligned with coordinators during live event operations.

The core problem is not a lack of information, but the way information is currently experienced in the field. Volunteers often receive fragmented instructions through verbal briefings, paper notes, static schedules, and scattered digital channels. In a busy arena environment this creates uncertainty, duplicate work, missed updates, and slow responses when something changes.

Arena Companion addresses this by turning the volunteer's phone into a compact field assistant. It combines shift overview, map guidance, task execution, reporting, communication, and offline support into one mobile-first workflow.

## 2. Problem Context

Event arenas are fast-moving environments. Volunteers may be new to the venue, unfamiliar with internal routes, and under time pressure. At the same time, coordinators need volunteers to be informed, reachable, and able to react to changing operational conditions.

Several characteristics make this context especially suitable for a mobile solution:

- Users are constantly moving between areas.
- Work is highly situational and time-sensitive.
- Information changes during the shift.
- Connectivity may be unstable in crowded venues.
- Many tasks happen away from desks or fixed workstations.

This means the application should not behave like a full management dashboard. It should behave like a lightweight mobile companion focused on the volunteer's immediate needs in the field.

## 3. Users and Stakeholders

### Primary User: Volunteer
The main user is a volunteer carrying out shift-based tasks in or around an arena. This user needs fast answers to practical questions:

- Where should I be?
- What am I supposed to do right now?
- Who do I contact if something is wrong?
- How do I report an issue quickly?
- What happens if the network is weak?

### Secondary User: Shift Leader
Shift leaders or area coordinators are not the main focus of the app, but they influence what data the app exposes. They need volunteers to receive instructions, confirm presence, and report issues in a structured way.

### Other Stakeholders
- Arena staff
- Event operations managers
- System providers in the VM 2029 ecosystem

These stakeholders shape the data model and integration needs, even if they do not actively use the same mobile interface.

## 4. Product Vision

The product vision is to create a simple and dependable mobile tool that reduces uncertainty for volunteers and improves operational coordination during live events.

A successful version of Arena Companion should make a volunteer feel that:

- their shift is clear,
- their next task is obvious,
- the arena is easier to navigate,
- important updates reach them in time,
- reporting a problem is fast and structured,
- and they can still function when connectivity is poor.

The vision is not to digitize every administrative process. The vision is to strengthen field execution.

## 5. Existing Solutions and What They Teach Us

This project can learn from several categories of existing digital products.

### Volunteer Management Platforms
Products such as Better Impact and Volgistics show how volunteer roles, schedules, and communication can be structured. Their strength is coordination and administration. Their weakness, for this use case, is that they are often built more for planning than for intense field use.

**Lesson:** Arena Companion should borrow structure, but simplify execution for mobile use during live operations.

### Navigation and Mapping Tools
OpenStreetMap-based mobile map solutions show how wayfinding can be made intuitive through live location, markers, and route context without relying on a Google Maps dependency.

**Lesson:** maps should not be included as decoration. They should solve specific arena-related navigation problems, such as finding entrances, meeting points, checkpoints, and task locations.

### Field Service and Workforce Apps
Apps used in logistics, maintenance, and field inspections show how mobile workers benefit from task lists, progress states, check-ins, and photo-based reporting.

**Lesson:** a small set of well-designed workflows is more useful than a large set of weakly connected features.

### QR-Based Access and Check-In Systems
QR workflows are widely used because they reduce friction and improve traceability.

**Lesson:** QR support fits naturally into volunteer operations for check-in, checkpoint validation, or task confirmation.

## 6. Key Scenarios

The following scenarios define the value of the product more clearly than a simple feature list.

### Scenario 1: Starting a Shift
A volunteer arrives at the arena and opens the app. They immediately see their current shift, assigned area, and the first relevant task. If needed, they can open the map and navigate to the correct location.

### Scenario 2: Confirming Presence
At a checkpoint or meeting point, the volunteer scans a QR code or performs a digital check-in. This gives the system a structured record that the volunteer has arrived.

### Scenario 3: Handling Daily Tasks
During the shift, the volunteer views active tasks, marks them as started or completed, and receives updates if priorities change.

### Scenario 4: Reporting an Incident
If something unusual happens, such as crowd congestion, a missing sign, or a blocked entrance, the volunteer creates an incident report. The app allows a quick description, optional photo, location, and timestamp.

### Scenario 5: Working with Weak Connectivity
If the network is unstable, the volunteer can still view cached shift details and complete key actions such as drafting a report or recording a check-in. The app syncs data later when the connection returns.

These scenarios give the product a practical structure and show why mobile device capabilities matter.

## 7. Functional Scope

### Core Features for the MVP

#### 1. Shift Overview
The volunteer can see:
- shift time,
- assigned arena or zone,
- role,
- important instructions,
- and the current status of the shift.

This feature reduces confusion before and during the shift.

#### 2. Arena Map and Wayfinding
The app should provide a clear visual representation of relevant locations, such as:
- entrances,
- help points,
- checkpoints,
- restricted areas,
- and assigned task zones.

This supports volunteers who are unfamiliar with the venue and reduces wasted time.

#### 3. Task List
The volunteer receives a small set of clear, prioritized tasks. Each task should communicate what to do, where to do it, and whether action is still pending.

#### 4. Check-In or Checkpoint Confirmation
QR scanning can be used to confirm arrival or completion at specific locations. This creates a fast and structured interaction that is easy to perform on mobile.

#### 5. Incident Reporting
The user should be able to submit a short incident report with:
- category,
- description,
- optional photo,
- time,
- and location.

This is one of the strongest justifications for using a mobile device.

#### 6. Alerts and Messages
The volunteer should receive important updates during operations, especially when tasks, locations, or safety-related instructions change.

#### 7. Offline Support
Essential content must remain available when the network is poor. This includes current shift details, map references, recent tasks, and queued user actions.

### Secondary Features
- Profile and contact information
- Shift leader contact shortcuts
- Team-wide announcements
- Status updates such as available or on break

### Possible Extensions
- Indoor positioning support
- Beacon-based contextual triggers
- Smart prioritization of tasks
- Volunteer movement analytics for coordinators

## 8. Why Mobile Technology Matters

This application should not merely be a web system placed inside a phone. It should take advantage of mobile-specific capabilities in ways that are directly useful for volunteer operations.

### GPS and Location
Location helps users orient themselves in large venues and connect actions to places. It is especially useful for navigation, incident context, and confirming where something happened.

### Maps
Maps reduce cognitive load. Instead of reading long instructions, volunteers can understand spatial context at a glance.

### Camera
The camera enables fast evidence capture in incident reports and supports QR scanning workflows.

### QR Scanning
QR interactions reduce manual input and support operational traceability.

### Local Storage
Local storage makes the application more reliable in unstable network conditions.

### Notifications
Push notifications make time-sensitive updates visible even when the app is not in the foreground.

### Optional Sensors or Beacons
These are not necessary for the MVP, but could become useful for more advanced context-aware features later in the semester.

## 9. Data Model and Ecosystem Interaction

Arena Companion depends on data from a wider operational ecosystem. The application should be seen as one mobile-facing part of that ecosystem, not an isolated product.

### Important Data Entities
- Volunteer
- Role
- Shift
- Arena
- Zone
- Task
- Incident
- Message
- Check-in
- Location point

### Data Likely Provided by an API
- volunteer identity and assigned role
- shift schedule and status
- arena and zone definitions
- assigned tasks
- operational announcements
- contact information for coordinators

### Data Likely Stored Locally
- cached shift and arena information
- map-related content
- pending check-ins
- draft or queued incident reports
- recently viewed messages

### Synchronization Requirements
- local actions should be stored safely when offline,
- queued records should sync when connectivity returns,
- conflict handling should be simple and predictable,
- and the user should understand whether data has been submitted or is still pending.

This synchronization model is important because trust in the app depends on whether users believe their actions were actually recorded.

### Recommended Technology Stack

For a semester project, the most balanced solution is a mobile-first architecture with a lightweight backend and local offline storage.

#### Mobile Application
- **Platform:** Android
- **Language:** Kotlin
- **UI framework:** Jetpack Compose
- **Architecture:** MVVM
- **Networking:** Retrofit or Ktor Client
- **Local database:** Room
- **Background sync:** WorkManager
- **Login state storage:** DataStore or EncryptedSharedPreferences
- **Maps and location:** OpenStreetMap with an Android-compatible OSM library such as osmdroid, plus Android location services
- **QR scanning:** ML Kit or ZXing
- **Push notifications:** Firebase Cloud Messaging (FCM)

#### Backend and Cloud Services
- **Recommended backend option:** Supabase
- **Authentication:** Supabase Auth
- **Database:** PostgreSQL managed by Supabase
- **API layer:** REST or Supabase client access
- **File storage:** backend storage for incident photos or related uploads

This stack is suitable because it keeps the Android side modern and course-relevant while avoiding the overhead of building a full custom backend from scratch.

### Recommended System Structure

The system should be organized into three layers:

#### 1. Mobile Client
The Android app handles interface, navigation, camera access, QR scanning, map display, notifications, and offline interaction.

#### 2. Backend Services
The backend manages authentication, volunteer identities, shifts, tasks, incidents, messages, and synchronization.

#### 3. Local Offline Layer
The device stores a local working set of important data so the user can continue using the app when connectivity is weak.

This means Arena Companion should not rely entirely on live server access. It should behave as an offline-capable mobile app with delayed synchronization when necessary.

### Authentication and Volunteer Login

The application will likely need user authentication because volunteer-specific information is personal and dynamic. The app should know:
- who the volunteer is,
- which shifts belong to them,
- what tasks they are assigned,
- and what data they are allowed to access.

However, the app should not store plaintext passwords locally.

The recommended flow is:

1. The volunteer logs in through a secure backend authentication service.
2. The backend verifies the identity.
3. The app receives a token or session credential.
4. The phone stores the login state securely.
5. The app uses this identity to load personalized data such as shifts, tasks, and messages.

This approach is more secure and more realistic than storing raw credentials on the device.

### Why a Database Is Needed

A database is likely necessary because the application depends on structured, changing information rather than static content.

Examples of data that should exist in the backend database include:
- users,
- volunteers,
- roles,
- shifts,
- arenas,
- zones,
- tasks,
- check-ins,
- incidents,
- and messages.

The local Room database on the device should not replace the backend database. Instead, it should cache the subset of data needed for reliable mobile use, especially under weak connectivity.

## 10. Information Architecture

The MVP can be structured into four main navigation areas:

### Home
Shows current shift, urgent alerts, and the next recommended action.

### Map
Shows the arena, task locations, checkpoints, and important markers.

### Tasks
Shows assigned tasks, progress, and links to check-in or reporting workflows.

### Profile
Shows the volunteer's role, contact information, and basic settings.

This structure is intentionally simple. In a field context, low navigation complexity is a design advantage.

## 11. UX Principles

The product should follow a few clear design principles:

### Clarity over richness
The app should prioritize clear actions and readable status over dense feature sets.

### Fast interaction
Important workflows should take only a few steps.

### Situational awareness
Users should always understand where they are, what they should do next, and whether something urgent requires attention.

### Reliability
The interface should communicate sync state, errors, and pending actions clearly.

### Stress-tolerant design
The app should remain usable when users are moving quickly or operating under pressure.

## 12. Challenges and Risks

### Connectivity
Arena environments may have overloaded mobile networks. This directly affects sync, notifications, and live updates.

### Battery Usage
Heavy use of GPS, camera, and background services may reduce battery life during long shifts.

### Privacy and Permissions
The app may handle user location and operational reports, which means permissions should be justified and limited.

### Location Accuracy
GPS may be inaccurate near buildings or indoors. Therefore, location-based features should not rely on unrealistic precision.

### Usability Pressure
Volunteers may be tired, busy, or inexperienced. Complex flows would reduce adoption and increase mistakes.

### Backend Dependency
Some features depend on what data and services are actually available from the broader VM 2029 ecosystem.

## 13. Feasibility Assessment

The concept is feasible for a semester project because the MVP can focus on a realistic subset of features:

- shift overview,
- map-based orientation,
- task list,
- QR check-in,
- incident reporting,
- and basic offline behavior.

These features are technically meaningful, clearly connected to mobile devices, and achievable if the project is scoped carefully.

More advanced features such as indoor positioning, richer messaging, or live coordination analytics should be treated as extensions rather than MVP requirements.

## 14. MVP Definition

The MVP should demonstrate one coherent volunteer workflow from arrival to task execution and reporting. A successful MVP would allow the user to:

1. open the app and identify their current shift,
2. navigate to the assigned place,
3. confirm arrival or task progress,
4. receive an update,
5. and report an issue if something goes wrong.

This is a stronger MVP definition than simply listing isolated screens because it proves operational usefulness.

## 15. Semester Development Direction

### Assignment 1
Define the concept, user needs, technical direction, and initial scope.

### Assignment 2
Translate the concept into interface design, wireframes, and a clearer interaction model.

### Prototype Milestones
Build the core Android workflow, data structures, and basic integrations.

### Later Iterations
Improve reliability, offline behavior, and feature depth based on testing and discussion.

## 16. Recommended Four-Page Submission Strategy

To stay within the assignment limit, this detailed document should not be submitted directly as-is. Instead, it should be compressed into four visual pages:

### Page 1: Product Concept and User Context
- problem statement
- target users
- usage context
- product vision

### Page 2: Key Scenarios and Feature Priorities
- one volunteer journey
- feature prioritization
- why mobile capabilities matter

### Page 3: Data, Ecosystem, and Feasibility
- core data entities
- API vs local storage
- offline and sync logic
- risks and limitations

### Page 4: MVP and Semester Plan
- MVP definition
- essential vs optional features
- roadmap for the semester
- open questions for discussion

This structure gives the submission both academic clarity and strong visual potential.
