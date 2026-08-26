# Mandatory Assignment A1 - English Proposal Draft

## 1. Introduction and Product Concept

### Product Name
Arena Companion

### Problem Statement
Volunteers working in and around event arenas often need fast access to shift information, location guidance, task updates, incident reporting, and communication. In busy event settings, information can be fragmented across paper notes, verbal instructions, and multiple digital channels. This creates confusion, delays, and unnecessary pressure on volunteers and coordinators.

### Proposed Solution
Arena Companion is an Android mobile application designed to support volunteers during event operations. The app acts as a personal operational assistant that helps volunteers understand where they need to be, what they need to do, and how to respond when conditions change.

### Primary Users
- Event volunteers
- Team leaders or area coordinators

### Context of Use
The application is intended for use before, during, and after shifts in dynamic arena environments where users may be moving, under time pressure, and sometimes working with unstable network connectivity.

### Purpose
The purpose of the app is to improve coordination, reduce uncertainty, and make volunteer work more efficient, safe, and manageable.

## 2. Existing Solutions and Technology

Several existing platforms provide inspiration for this solution:

- Volunteer management systems such as Better Impact and Volgistics show how scheduling, communication, and role assignment can be organized.
- Navigation apps such as Google Maps demonstrate clear route guidance and location awareness.
- Workforce apps used in logistics or field service show the value of task lists, status updates, and check-in workflows.
- QR-based ticketing and access systems illustrate how fast scanning can support check-in and verification tasks.

### What Can Be Learned
- Simple mobile-first workflows are critical in time-sensitive environments.
- Real-time updates reduce confusion when assignments change.
- Map-based guidance is useful when users operate across large or unfamiliar venues.
- QR scanning is faster and more reliable than manual entry in many operational scenarios.
- Offline resilience is important because connectivity may be limited in crowded venues.

## 3. Proposed Features and Mobile Context

### Prioritized Features

#### High Priority
- Personal shift overview
- Task list for the current shift
- Arena map with points of interest
- QR-based check-in or checkpoint confirmation
- Incident reporting with text and photo
- Push notifications for urgent updates

#### Medium Priority
- Team messaging or broadcast announcements
- Volunteer status updates such as available, busy, or on break
- Contact list for coordinators

#### Lower Priority / Extension
- Indoor positioning enhancements
- Sensor-based context awareness
- Analytics dashboard for coordinators

### Relevant Mobile Technologies
- GPS and maps: help volunteers navigate to arenas, entrances, meeting points, and task locations.
- Camera: supports incident reporting and QR scanning.
- QR scanning: enables fast check-in, checkpoint validation, or task confirmation.
- Notifications: deliver urgent operational changes in real time.
- Local storage: keeps essential shift and task data available offline.
- Connectivity and synchronization: updates data when the device regains network access.

### Why These Technologies Fit
These technologies match the realities of event operations: movement, changing conditions, limited time, and the need for reliable access to practical information on the go.

## 4. Data and Interaction with the Ecosystem

### Core Data Objects
- Volunteers
- Shifts
- Arenas
- Zones or checkpoints
- Tasks
- Incidents
- Messages
- Check-ins
- Positions or location markers

### Likely API Data
- Volunteer profile and assigned role
- Shift schedule
- Arena and zone information
- Task assignments
- Operational announcements
- Coordinator contact data

### Likely Local Data
- Cached shift details
- Cached arena maps
- Recently assigned tasks
- Draft incident reports
- Recent messages

### Synchronization Needs
- Incident reports should upload when connectivity is available.
- Check-in events should be queued offline and synced later if necessary.
- Task status changes should be reconciled with server data.
- Notifications and announcements should refresh whenever the device reconnects.

## 5. Feasibility, Challenges and Limitations

### Main Technical Challenges
- Unstable connectivity in crowded arenas
- Battery consumption from GPS, camera, and notifications
- Permission handling for camera and location
- Privacy concerns related to user position and operational data
- Location accuracy, especially near buildings or indoors
- Usability under stress and time pressure
- Keeping offline and online data consistent

### Most Challenging Areas
The most challenging parts are likely offline synchronization, reliable location support, and designing a workflow that remains simple during high-pressure event situations.

### Limitations
- Full indoor positioning may be difficult without extra infrastructure.
- Real-time collaboration features may depend on backend maturity.
- The first version should avoid feature overload and focus on a strong operational core.

## 6. Product Vision and Semester Plan

### End-of-Semester Vision
By the end of the semester, the goal is to deliver a functional Android prototype that supports a volunteer through a realistic work scenario, including viewing shifts, receiving tasks, navigating to locations, checking in, and reporting incidents.

### Initial MVP
- Login or user identification
- Personal shift overview
- Task list
- Arena map
- QR check-in
- Incident reporting with photo
- Basic offline caching

### Possible Extensions
- Coordinator dashboard integration
- Group communication tools
- Smarter task prioritization
- Enhanced location support
- More advanced synchronization logic

### Relation to Upcoming Work
- Assignment 1 defines the concept, requirements, and technical direction.
- The next milestones can focus on UI design, architecture, data modeling, and implementation of the MVP features.
- Later assignments can refine usability, connectivity handling, and feature depth based on feedback.

## 7. Suggested Structure for the Final PDF

To stay within the four-page limit, the final submission could use this structure:

1. Product concept and user context
2. Existing solutions and lessons learned
3. Prioritized features and mobile technologies
4. Data model and ecosystem interaction
5. Challenges and feasibility
6. MVP and semester roadmap

Sketches, wireframes, tables, and simple diagrams should be used to reduce long paragraphs and make the document easier to read.
