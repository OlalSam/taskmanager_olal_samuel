Task Management API with Tags and Google Calendar Sync
Project Overview
The Task Management API will allow users to create, manage, and organize tasks while
associating them with tags. Each task can have multiple tags, and tags can be linked to
multiple tasks. Additionally, tasks should be synchronized with Google Calendar
according to due dates, ensuring users can view and manage their tasks both inside the
app and in their personal calendars.
This API will enable users to create tasks, add tags, update task statuses, delete tasks,
and manage relationships between tasks and tags, while also handling Google
Calendar integration for synchronization.
Functional Requirements
Task Management
●​ Create Tasks:​
As a user, I want to create tasks with a title, description, due date, and status
(e.g., completed), so that I can track my work.
●​ Update Tasks:​
As a user, I want to update the task title, completion status, due date, and
associated tags, so that my tasks remain current.
●​ Delete Tasks:​
As a user, I want to delete tasks, so that irrelevant items are removed. Deleting a
task should also remove it from Google Calendar but not delete the associated
tags.
●​ View Tasks:​
As a user, I want to retrieve a list of all tasks, with optional filters (e.g., by
completion status or associated tags), so that I can quickly find what I need.
Each task should also return its associated tags.
​
​
​
​
​
​​
Tag Management
●​ Create Tags:​
As a user, I want tags to be created automatically when I use them in a task, so
that I don’t have to manually manage them.
●​ View Tags:​
As a user, I want to retrieve a list of all tags, along with the number of tasks each
tag is associated with, so that I know which tags are most used.
●​ View Tag Details:​
As a user, I want to view details of a specific tag, including all tasks associated
with that tag, so that I can organize work by category.​
Google Calendar Integration
●​ Sync Task to Calendar:​
As a user, I want tasks to create events in Google Calendar, so that I never miss
a deadline.
●​ Update Calendar Events:​
As a user, I want changes to my tasks (title, status, due date) to be reflected in
the corresponding Google Calendar event, so that both systems stay consistent.
●​ Delete Calendar Events:​
As a user, I want deleting a task to also remove its linked Google Calendar
event, so that my calendar stays clean.
●​ Sync from Calendar to Tasks:​
As a user, I want updates in Google Calendar (e.g., changed title or date) to
sync back to my task, so that my app reflects my latest schedule.
●​ Conflict Resolution:​
As a user, I want the system to handle conflicts when a task and calendar event
are updated differently, so that I don’t lose important changes.​
​
​
​
​
​
​
​​
Technical Requirements
●​ Programming Language: Implement the backend using Java with Spring Boot.
●​ Database: Use a relational database (e.g., MySQL, PostgreSQL)
●​ Testing: Write unit and integration tests. Focus on meaningful coverage of
critical paths.
Submission Instructions
●​ GitHub Repository: Create a repository for the backend code and push the
project with meaningful commit messages.
●​ README: Provide setup instructions to:​
​
- Run the application​
​
- Configure Google API credentials​
​
- Test the API using Swagger/Postman
Evaluation Criteria
Your submission will be evaluated based on:
●​ Correctness of the implementation.
●​ Code quality, organization, and clarity.
●​ Handling of edge cases.
●​ Quality of tests (unit and integration).