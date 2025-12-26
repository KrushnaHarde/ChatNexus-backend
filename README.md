# ChatNexus v2.0 💬

A real-time 1-to-1 chat application built with Spring Boot and WebSocket technology.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-green)
![MongoDB](https://img.shields.io/badge/MongoDB-7-brightgreen)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-blue)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![Version](https://img.shields.io/badge/Version-2.0-purple)

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Running the Application](#-running-the-application)
- [Deployment](#-deployment)
- [API Endpoints](#-api-endpoints)
- [WebSocket Endpoints](#-websocket-endpoints)
- [Project Structure](#-project-structure)
- [License](#-license)

## ✨ Features

- **Real-time Messaging**: Instant message delivery using WebSocket and STOMP protocol
- **User Presence**: See who's online in real-time
- **1-to-1 Private Chat**: Secure private conversations between users
- **Message Persistence**: All messages are stored in MongoDB
- **User Status Management**: Automatic online/offline status updates
- **Responsive UI**: Clean and modern user interface
- **Chat Room Management**: Automatic chat room creation for user pairs
- **Read Receipts** *(v2.0)*: Real-time read status with double blue ticks when messages are read
- **Message Timestamps** *(v2.0)*: Hover over messages to see sent and read timestamps
- **JWT Authentication**: Secure user authentication with JSON Web Tokens

## 🆕 What's New in v2.0

### Read Receipts
- Single tick (✓) - Message sent
- Double tick (✓✓) - Message delivered
- Blue double tick (✓✓) - Message read
- **Real-time updates**: See read status instantly without refreshing

### Message Timestamps
- Hover over any message to see detailed timestamps
- Shows "Sent: [time]" for all messages
- Shows "Read: [time]" for read messages
- Smart date formatting (time only for today, date + time for older messages)

### JWT Authentication & Security
- Secure user registration and login
- Token-based authentication for all API requests
- Password encryption using BCrypt
- Protected WebSocket connections
- Automatic session management with localStorage

## 🛠 Tech Stack

### Backend
- **Java 17** - Programming language
- **Spring Boot 4.0.1** - Application framework
- **Spring WebSocket** - Real-time bidirectional communication
- **Spring Data MongoDB** - Database operations
- **Lombok** - Reducing boilerplate code
- **STOMP** - Simple Text Oriented Messaging Protocol
- **JWT** - JSON Web Tokens for authentication

### Frontend
- **HTML5/CSS3** - Structure and styling
- **JavaScript** - Client-side logic
- **SockJS** - WebSocket fallback
- **STOMP.js** - STOMP client library

### Database
- **MongoDB** - NoSQL database for storing messages and user data

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration

## 🏗 Architecture

```
┌─────────────────┐     WebSocket/STOMP     ┌─────────────────┐
│                 │◄──────────────────────► │                 │
│   Web Client    │                         │  Spring Boot    │
│   (Browser)     │     REST API            │    Server       │
│                 │◄──────────────────────► │                 │
└─────────────────┘                         └────────┬────────┘
                                                     │
                                                     │ Spring Data
                                                     │
                                            ┌────────▼────────┐
                                            │                 │
                                            │    MongoDB      │
                                            │                 │
                                            └─────────────────┘
```

## 📦 Prerequisites

Before running this application, make sure you have the following installed:

- **Java 17** or higher
- **Maven 3.6+**
- **Docker & Docker Compose** (for MongoDB)
- **Git**

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ChatNexus.git
   cd ChatNexus
   ```

2. **Start MongoDB using Docker Compose**
   ```bash
   docker-compose up -d
   ```

3. **Build the project**
   ```bash
   # On Windows
   mvnw.cmd clean install

   # On Linux/Mac
   ./mvnw clean install
   ```

## ⚙ Configuration

The application configuration is located in `src/main/resources/application.yml`:

```yaml
spring:
  data:
    mongodb:
      username: krushna
      password: krushna
      host: localhost
      port: 27017
      database: ChatNexus
      authentication-database: admin

server:
  port: 8080
```

### Environment Variables

You can override the default configuration using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGODB_HOST` | MongoDB host | localhost |
| `MONGODB_PORT` | MongoDB port | 27017 |
| `MONGODB_DATABASE` | Database name | ChatNexus |
| `MONGODB_USERNAME` | MongoDB username | krushna |
| `MONGODB_PASSWORD` | MongoDB password | krushna |
| `SERVER_PORT` | Application port | 8080 |

## 🏃 Running the Application

1. **Ensure MongoDB is running**
   ```bash
   docker-compose up -d
   ```

2. **Run the Spring Boot application**
   ```bash
   # On Windows
   mvnw.cmd spring-boot:run

   # On Linux/Mac
   ./mvnw spring-boot:run
   ```

3. **Access the application**
   - Open your browser and navigate to: `http://localhost:8080`
   - MongoDB Express UI (optional): `http://localhost:8081`

## 📡 API Endpoints

### REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/users` | Get all connected (online) users |
| `GET` | `/messages/{senderId}/{recipientId}` | Get chat history between two users |

## 🔌 WebSocket Endpoints

### Connection
- **WebSocket Endpoint**: `/ws` (with SockJS fallback)

### Message Mappings

| Destination | Description |
|-------------|-------------|
| `/app/user.addUser` | Register a new user and broadcast to all |
| `/app/user.disconnectUser` | Disconnect user and broadcast to all |
| `/app/chat` | Send a private message |

### Subscriptions

| Destination | Description |
|-------------|-------------|
| `/topic/public` | Receive user connect/disconnect notifications |
| `/user/{username}/queue/messages` | Receive private messages |

## 📁 Project Structure

```
ChatNexus/
├── src/
│   ├── main/
│   │   ├── java/com/project/ChatNexus/
│   │   │   ├── ChatNexusApplication.java    # Main application class
│   │   │   ├── chat/
│   │   │   │   ├── ChatController.java      # Chat message controller
│   │   │   │   ├── ChatMessage.java         # Chat message entity
│   │   │   │   ├── ChatMessageRepository.java
│   │   │   │   ├── ChatMessageService.java
│   │   │   │   └── ChatNotification.java    # Notification DTO
│   │   │   ├── chatroom/
│   │   │   │   ├── ChatRoom.java            # Chat room entity
│   │   │   │   ├── ChatRoomRepository.java
│   │   │   │   └── ChatRoomService.java
│   │   │   ├── config/
│   │   │   │   └── WebSocketConfig.java     # WebSocket configuration
│   │   │   └── user/
│   │   │       ├── Status.java              # User status enum
│   │   │       ├── User.java                # User entity
│   │   │       ├── UserController.java      # User controller
│   │   │       ├── UserRepository.java
│   │   │       └── UserService.java
│   │   └── resources/
│   │       ├── application.yml              # Application configuration
│   │       ├── static/
│   │       │   ├── index.html               # Main HTML page
│   │       │   ├── css/main.css             # Styles
│   │       │   └── js/main.js               # Client-side JavaScript
│   │       └── templates/
│   └── test/
│       └── java/com/project/ChatNexus/
│           └── ChatNexusApplicationTests.java
├── docker-compose.yml                        # Docker services configuration
├── pom.xml                                   # Maven dependencies
└── README.md
```

## 🔧 Docker Services

The `docker-compose.yml` includes:

- **MongoDB**: Database server on port 27017
- **Backend**: Spring Boot application on port 8080

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f
```

## 🚀 Deployment

### Docker Deployment (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/ChatNexus.git
   cd ChatNexus
   ```

2. **Create environment file**
   ```bash
   cp .env.example .env
   # Edit .env with your production values
   ```

3. **Build and start containers**
   ```bash
   # Development
   docker-compose up -d --build

   # Production
   docker-compose -f docker-compose.prod.yml up -d --build
   ```

4. **Verify deployment**
   ```bash
   # Check container status
   docker-compose ps

   # View logs
   docker-compose logs -f backend
   ```

5. **Access the application**
   - Application: `http://localhost:8080`

### Manual Docker Build

```bash
# Build the image
docker build -t chatnexus:latest .

# Run with external MongoDB
docker run -d \
  --name chatnexus \
  -p 8080:8080 \
  -e MONGODB_URI=mongodb://user:pass@mongodb-host:27017/chat_nexus?authSource=admin \
  chatnexus:latest
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGODB_URI` | MongoDB connection string | `mongodb://krushna:krushna@localhost:27017/chat_nexus?authSource=admin` |
| `PORT` | Application port | `8080` |
| `MONGO_USERNAME` | MongoDB root username | `krushna` |
| `MONGO_PASSWORD` | MongoDB root password | `krushna` |
| `JAVA_OPTS` | JVM options | `-Xmx256m -Xms128m` |

### Production Considerations

1. **Security**
   - Change default MongoDB credentials
   - Use environment variables for sensitive data
   - Consider adding HTTPS with a reverse proxy (nginx/traefik)

2. **Scaling**
   - MongoDB can be replaced with a managed service (MongoDB Atlas)
   - Application can be scaled horizontally behind a load balancer

3. **Monitoring**
   - Add health check endpoint monitoring
   - Consider adding logging aggregation (ELK stack)

## 🤝 How It Works

1. **User Connection**: When a user enters their nickname and connects, they are registered in MongoDB with an ONLINE status
2. **User Discovery**: The client fetches all online users via REST API and subscribes to `/topic/public` for real-time updates
3. **Sending Messages**: Messages are sent via WebSocket to `/app/chat`, stored in MongoDB, and delivered to the recipient's private queue
4. **Receiving Messages**: Users receive messages on their personal subscription `/user/{nickname}/queue/messages`
5. **Disconnection**: When a user disconnects, their status is updated to OFFLINE and all clients are notified

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Made with ❤️ using Spring Boot and WebSocket
