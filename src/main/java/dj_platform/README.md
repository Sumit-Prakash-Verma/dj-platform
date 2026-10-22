# DJ Platform — AI-Powered Collaborative DJ Platform

## 1. Project Overview

DJ Platform is a web-based collaborative music platform where users can join music rooms, communicate through real-time chat, contribute songs to shared music queues, and vote for their favourite songs in Soundclash competitions.

The application is built using Java, Spring Boot, MySQL, HTML, CSS, and JavaScript. It combines backend APIs, database integration, authentication, and real-time communication to provide a collaborative music experience.

## 2. Objectives

- Provide user registration, login, and logout.
- Allow users to join and leave music rooms.
- Enable room-based real-time chat.
- Support collaborative music queue management.
- Organize Soundclash competitions between two songs.
- Store application data in MySQL.
- Broadcast supported application updates in real time.
- Generate rule-based DJ transition suggestions.
- Provide genre-based music recommendations using saved user preferences.

## 3. Technologies Used

- **Backend:** Java 21, Spring Boot
- **Database:** MySQL
- **Frontend:** HTML5, CSS3, JavaScript
- **Real-Time Communication:** WebSocket and STOMP
- **Build Tool:** Maven
- **Development Environment:** Visual Studio Code
- **Version Control:** Git (optional)

## 4. Main Modules

### 4.1 User Authentication

Supports user registration, login, logout, and authenticated access to protected application features.

### 4.2 Music Rooms

Displays available music rooms and supports room membership management. Listener counts are calculated from current room memberships.

### 4.3 Real-Time Chat

Provides room-based messaging using WebSocket and STOMP, allowing connected users to exchange messages in real time.

### 4.4 Collaborative Music Queue

Allows users to add, edit, and delete queued tracks, with validation and supported live updates.

### 4.5 Soundclash

Supports two-song competitions, vote submission, duplicate-vote protection, and live result updates.

### 4.6 AI DJ Transitions

Generates rule-based DJ transition suggestions using available track information, including BPM and genre. Suggestions depend on the data available for each track.

### 4.7 Music Recommendations

Provides genre-based music recommendations using saved user preferences.

*Note: The current transition and recommendation features use rule-based logic rather than a trained machine-learning model or actual audio mixing.*

## 5. Prerequisites

Install or configure the following before running the application:

- Java JDK 21
- MySQL Server
- MySQL Workbench (recommended for database management)
- Visual Studio Code or another Java-compatible IDE
- Git (optional)

## 6. Database Setup

1. Start MySQL Server.
2. Create or configure the `dj_platform` database.
3. Open `src/main/resources/application.properties`.
4. Configure the database URL, username, and password for your local MySQL setup.
5. Ensure the required tables and initial data are available according to the project's existing database configuration or SQL scripts.

Never publish database passwords, authentication secrets, session cookies, or other sensitive credentials in a public repository.

## 7. How to Run

### Step 1: Open the Project

Open the project root directory in Visual Studio Code and open its terminal. Ensure that the directory contains `pom.xml` and `mvnw.cmd`.

### Step 2: Run Tests

Execute the following command in Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

Wait for Maven to finish and check the final build result.

### Step 3: Start the Application

Execute:

```powershell
.\mvnw.cmd spring-boot:run
```

### Step 4: Open the Application

Open the following address in your browser:

http://localhost:8080

Register or log in to access features that require authentication.

## 8. Testing and Verification

### Automated Testing

In the latest recorded Maven test run:

- Tests run: 1
- Failures: 0
- Errors: 0
- Skipped: 0
- Build result: `BUILD SUCCESS`

This result reflects the automated test suite at the time of that run. It does not mean every application feature has a dedicated automated test.

### Manual Feature Verification

The following checks were reported as passing during development:

- User registration, login, and logout.
- Room membership and listener counts.
- Real-time chat between two browser sessions.
- Music queue operations and supported live updates.
- Soundclash voting and duplicate-vote protection.
- DJ transition suggestions, including handling tracks with missing BPM.
- Recommendation preference persistence.
- Unauthorized access checks.
- Browser console and server log checks.

Repeat the relevant checks against the exact code version being submitted.

## 9. Future Enhancements

- More advanced personalized music recommendations.
- Audio-aware DJ transition assistance.
- Music sentiment analysis.
- Queue ranking and voting analytics.
- Expanded automated integration and end-to-end testing.
- Production deployment, monitoring, and performance improvements.

## 10. Project Status

The core application features have been implemented, and manual verification has been performed during development. Final verification should be completed against the exact version submitted for internship evaluation.

## Conclusion

DJ Platform demonstrates practical skills in Java, Spring Boot, MySQL integration, authentication, REST API development, WebSocket-based real-time communication, collaborative queue management, Soundclash voting, and frontend integration using HTML, CSS, and JavaScript.

The project provides a foundation for further development of collaborative music experiences and more advanced DJ assistance features.
