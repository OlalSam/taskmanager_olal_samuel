# Task Manager API

Task management system built with Spring Boot that provides task and tag management with Google Calendar integration. The API supports user authentication, advanced filtering, pagination, and real-time calendar synchronization.

## 🚀 Features

### Core Functionality
- **Task Management**: Create, read, update, and delete tasks
- **Tag System**: Automatic tag creation and user-specific organization
- **Advanced Filtering**: Filter tasks by status, tags, and pagination
- **User Isolation**: All data is scoped to authenticated users
- **Concurrent Updates**: Optimistic locking with version control

### Google Calendar Integration
- **Bidirectional Sync**: Tasks automatically sync to Google Calendar
- **Real-time Updates**: Calendar events update when tasks change
- **Webhook Support**: Calendar changes sync back to tasks
- **OAuth2 Authentication**: Secure Google Calendar access

### Technical Features
- **JWT Authentication**: Secure API access via Keycloak
- **PostgreSQL Database**: Reliable data persistence
- **RESTful API**: Clean, well-documented endpoints
- **Comprehensive Testing**: Unit and integration tests
- **API Documentation**: Auto-generated with Spring REST Docs

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.5.6
- **Database**: PostgreSQL
- **Authentication**: Keycloak (OAuth2/JWT)
- **Calendar Integration**: Google Calendar API v3
- **Build Tool**: Maven
- **Documentation**: Spring REST Docs + AsciiDoc

## 📋 Prerequisites

Before running the application, ensure you have:

- **Java 21** or higher
- **Maven 3.6+**
- **PostgreSQL 12+** (running on port 5100 in yml)
- **Keycloak** (running on port 9020 in yml)
- **Google Cloud Console** account (for Calendar integration)

## 🚀 Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd taskmanager
./mvnw clean install
```

### 2. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE taskmanager_db;
CREATE USER user WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE taskmanager_db TO user;
```
OR 

Use postgres in docker-compose.yml
### 3. Configure Application

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
app.database.host=localhost
app.database.port=5100
app.database.name=taskmanager_db
app.database.username=user
app.database.password=password

# Keycloak Configuration
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:9020/realms/sare_africa
app.security.client-id=sare_africa_api

# Google Calendar (Optional - see setup guide below)
google.client-id=YOUR_GOOGLE_CLIENT_ID
google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
google.redirect-uri=http://localhost:8080/api/v1/calendar/callback
```
Or send email I add you to test users
### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`

## 📚 API Documentation

### Base URL
All API endpoints are prefixed with `/api/v1`

### Authentication
All endpoints require JWT authentication via Bearer token:

```http
Authorization: Bearer <your-jwt-token>
```

### Task Management

#### Create Task
```http
POST /api/v1/tasks
Content-Type: application/json

{
  "title": "Complete project documentation",
  "description": "Write comprehensive API documentation",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2024-01-15T14:30:00",
  "tags": ["work", "documentation", "urgent"]
}
```

#### Get All Tasks (with filtering)
```http
GET /api/v1/tasks?status=TODO&tags=work&page=0&size=10&sortBy=dueDate&sortDirection=asc
```

**Query Parameters:**
- `status` - Filter by task status (TODO, IN_PROGRESS, COMPLETED, CANCELLED)
- `tags` - Filter by tag names (can be multiple)
- `page` - Page number (default: 0)
- `size` - Page size (default: 10)
- `sortBy` - Sort field (default: createdAt)
- `sortDirection` - Sort direction: asc/desc (default: desc)

#### Get Task by ID
```http
GET /api/v1/tasks/{id}
```

#### Update Task
```http
PUT /api/v1/tasks/{id}
Content-Type: application/json

{
  "title": "Updated task title",
  "description": "Updated description",
  "status": "IN_PROGRESS",
  "priority": "URGENT",
  "dueDate": "2024-01-20T16:00:00",
  "tags": ["work", "updated"]
}
```

#### Delete Task
```http
DELETE /api/v1/tasks/{id}
```

### Tag Management

#### Get All Tags
```http
GET /api/v1/tags
```

#### Get Tag by ID
```http
GET /api/v1/tags/{id}
```

#### Delete Tag
```http
DELETE /api/v1/tags/{id}
```

### Google Calendar Integration

#### Connect Calendar
```http
GET /api/v1/calendar/connect
```

Returns Google OAuth URL for calendar connection.
#### Calendar Callback
```http
GET /api/v1/calendar/callback?code={auth_code}&state={user_id}
```

Handles OAuth callback after user authorization.

## 📅 Google Calendar Setup

### 1. Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable Google Calendar API
4. Create OAuth 2.0 credentials:
   - Application type: Web application
   - Authorized redirect URIs: `http://localhost:8080/api/v1/calendar/callback`

### 2. Configure OAuth Consent Screen

1. Go to OAuth consent screen
2. Choose "External" for development
3. Add required scopes: `https://www.googleapis.com/auth/calendar`
4. Add test users (your email)

### 3. Update Configuration

Add your credentials to `application.properties`:

```properties
google.client-id=YOUR_ACTUAL_CLIENT_ID
google.client-secret=YOUR_ACTUAL_CLIENT_SECRET
google.redirect-uri=http://localhost:8080/api/v1/calendar/callback
```

### 4. Test Integration

1. Start the application
2. Get auth token from Keycloak
3. Call `/api/v1/calendar/connect` to get OAuth URL
4. Complete OAuth flow in browser
5. Copy the callback uri in your browser to postman
5. Create a task with due date - it will sync to Google Calendar

For detailed setup instructions, see [GOOGLE_CALENDAR_SETUP.md](GOOGLE_CALENDAR_SETUP.md)

## 🧪 Testing

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=TaskControllerTest

# Run with coverage
./mvnw test jacoco:report
```

### Postman Collectio
Import the provided Postman collections for comprehensive API testing:

1. **TaskManager_API.postman_collection.json** - Complete API collection
2. **TaskManager_Environment.postman_environment.json** - Environment variables
3. **Google_Calendar_Operations.postman_collection.json** - Calendar-specific tests

## 📖 API Documentation

### Auto-generated Documentation

Once the application is running, access the interactive API documentation:

- **HTML Documentation**: `http://localhost:8080/docs/index.html`
- **Health Check**: `http://localhost:8081/actuator/health`
- **Application Info**: `http://localhost:8081/actuator/info`

### Response Examples

#### Task Response
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "title": "Complete project documentation",
  "description": "Write comprehensive API documentation",
  "status": "TODO",
  "priority": "HIGH",
  "dueDate": "2024-01-15T14:30:00",
  "tags": ["work", "documentation", "urgent"],
  "createdAt": "2024-01-10T10:00:00",
  "updatedAt": "2024-01-10T10:00:00",
  "version": 0
}
```

#### Paginated Response
```json
{
  "content": [...],
  "totalElements": 25,
  "totalPages": 3,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

## 🔧 Configuration

### Environment Variables

For production deployment, use environment variables:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5100/taskmanager_db"
export SPRING_DATASOURCE_USERNAME="user"
export SPRING_DATASOURCE_PASSWORD="password"
export GOOGLE_CLIENT_ID="your_client_id"
export GOOGLE_CLIENT_SECRET="your_client_secret"
```

### Database Configuration

The application uses PostgreSQL with the following default settings:

- **Host**: localhost
- **Port**: 5100
- **Database**: taskmanager_db
- **Username**: user
- **Password**: password

### Security Configuration

- **JWT Issuer**: Keycloak at `http://localhost:9020/realms/sare_africa`
- **Client ID**: `sare_africa_api`

## 🚨 Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql

# Verify database exists
psql -h localhost -p 5100 -U user -d taskmanager_db -c "\dt"
```

#### 2. Authentication Issues
- Verify Keycloak is running on port 9020
- Check JWT token validity
- Ensure correct realm and client configuration

#### 3. Calendar Integration Issues
- Verify Google Cloud Console setup
- Check OAuth redirect URI matches exactly
- Ensure test users are added to OAuth consent screen


## 🏗️ Development

### Project Structure

```
src/
├── main/
│   ├── java/com/ignium/taskmanager/
│   │   ├── calendar/          # Google Calendar integration
│   │   ├── config/           # Configuration classes
│   │   ├── task/             # Task management
│   │   ├── user/             # User management
│   │   └── TaskmanagerApplication.java
│   └── resources/
│       ├── application.properties
│       └── static/docs/      # Generated API documentation
└── test/
    └── java/com/ignium/taskmanager/
        ├── calendar/         # Calendar integration tests
        ├── task/             # Task management tests
        └── user/             # User management tests
```

### Code Quality

The project includes:

- **Lombok** for reducing boilerplate code
- **Validation** annotations for request validation
- **Exception handling** with global exception handler
- **Logging** with SLF4J and Logback
- **Testing** with JUnit 5 and MockMvc


### Google Calendar → App Sync (Webhook)

* Subscription: When a user connects their calendar, the CalendarWebhookService.subscribeUserToCalendarChanges method is called. It registers a webhook with Google,
  telling Google to send a notification to your /api/v1/calendar/webhook/notifications endpoint whenever the user's primary calendar changes. It correctly stores the
  channelId, resourceId, and an initial syncToken in the CalendarSubscription entity.
* Notification: When a change happens in Google Calendar, Google sends a POST request to your CalendarWebhookController.
* Webhook Handling: The handleNotification method in the controller receives the notification. It correctly identifies the resourceState as exists (meaning a change
  occurred) and calls calendarWebhookService.processEventUpdate asynchronously.
* Incremental Sync: The processIncrementalCalendarSync method is the core of the two-way sync. It uses the stored syncToken from the CalendarSubscription to ask Google
  for only the events that have changed since the last sync. This is far more efficient than re-fetching all events.
* Processing Changes:
   * For each changed event from Google, it checks if the event was "cancelled". If so, it calls handleDeletedEvent to find the corresponding task in your database and
     remove the calendarEventId.
   * If the event was updated, it calls handleUpdatedEvent.
* Conflict Resolution: The shouldUpdateFromCalendar method implements a "Last Write Wins" conflict resolution strategy. It compares the updatedAt timestamp of your Task
  with the updated timestamp from the Google Calendar Event. The task in your database is only updated if the calendar event has a more recent timestamp. This is a
  crucial detail that many developers would miss.
* Updating the Task: If an update is warranted, updateTaskFromCalendarEvent maps the fields from the Google Event (summary, description, start time) back to your Task
  entity and saves it.
* Webhook Renewal: Have a @Scheduled method (renewExpiredWebhooks) to automatically renew webhook subscriptions before they expire (Google webhooks have a limited
  lifetime). This ensures the two-way sync doesn't silently fail after a week.


* Decoupling: Using Spring Events to decouple task management from calendar synchronization.
* Asynchronous Processing: Using @Async for all external communication to keep the API responsive.
* Efficiency: Using Google's syncToken for incremental updates instead of fetching all events every time.
* Resilience: Using @Retryable for outgoing API calls and having a scheduled job (@Scheduled) to automatically renew webhook subscriptions.
* Conflict Resolution: Implementing a clear "Last Write Wins" strategy to handle simultaneous updates.


