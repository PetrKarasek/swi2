# SWI22025 - Chat Application

A real-time chat application built with Spring Boot backend and React frontend, featuring WebSocket communication, RabbitMQ messaging, and PostgreSQL database.

## Features

- **Real-time messaging** using WebSocket (STOMP over SockJS)
- **Message persistence** with PostgreSQL database
- **Message queuing** with RabbitMQ
- **User authentication** (signup/login)
- **Modern UI** with Material-UI components
- **TypeScript** for type safety

## Tech Stack

### Backend
- Spring Boot 3.5.6
- Java 17
- Spring WebSocket (STOMP)
- Spring Data JPA
- PostgreSQL
- RabbitMQ
- Lombok

### Frontend
- React 19
- TypeScript
- Material-UI (MUI)
- SockJS & STOMP.js
- Axios
- Vite

## Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL
- RabbitMQ
- Maven

## Setup Instructions

### 1. Database Setup

Install PostgreSQL and create a database:

```sql
CREATE DATABASE chatdb;
CREATE USER postgres WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE chatdb TO postgres;
```

### 2. RabbitMQ Setup

Install RabbitMQ and ensure it's running on default ports:
- Host: localhost
- Port: 5672
- Username: guest
- Password: guest

### 3. Backend Setup

Navigate to the project root and run:

```bash
# Build the backend
./mvnw clean install

# Run the Spring Boot application
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8081`

### 4. Frontend Setup

Navigate to the frontend directory:

```bash
cd src/main/frontend

# Install dependencies
npm install

# Run the development server
npm run dev
```

The frontend will start on `http://localhost:5173`

## Usage

1. **Sign up**: Create a new user account
2. **Login**: Authenticate with your credentials
3. **Chat**: Start sending messages in the public chat room
4. **Real-time**: Messages appear instantly for all connected users

## API Endpoints

### Authentication
- `POST /signup` - Register a new user
- `POST /login` - Authenticate user

### Chat
- `POST /app/message` (WebSocket) - Send a message to chat room
- `POST /app/private-message` (WebSocket) - Send a private message
- `GET /api/queue?userId={userId}` - Retrieve queued messages

### Database
- `GET /users` - Get all users
- `GET /chatrooms?username={username}` - Get user's chat rooms

## WebSocket Endpoints

- **Connect**: `ws://localhost:8081/ws` (with SockJS fallback)
- **Subscribe**: `/chatroom/{roomId}` - Subscribe to chat room messages
- **Send**: `/app/message` - Send messages to chat room

## Configuration

### Database Configuration (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/chatdb
spring.datasource.username=postgres
spring.datasource.password=password
```

### RabbitMQ Configuration
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

## Architecture

- **Backend**: Spring Boot with WebSocket support for real-time communication
- **Frontend**: React SPA consuming WebSocket API
- **Database**: PostgreSQL for message persistence
- **Message Queue**: RabbitMQ for message distribution
- **Communication**: STOMP protocol over WebSocket

## Development

### Running Tests
```bash
# Backend tests
./mvnw test

# Frontend tests
cd src/main/frontend
npm test
```

### Building for Production
```bash
# Backend
./mvnw clean package

# Frontend
cd src/main/frontend
npm run build
```

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   - Ensure PostgreSQL is running
   - Check database credentials in `application.properties`
   - Verify database exists

2. **RabbitMQ Connection Error**
   - Ensure RabbitMQ service is running
   - Check RabbitMQ credentials
   - Verify default ports are accessible

3. **WebSocket Connection Issues**
   - Check backend is running on port 8081
   - Verify CORS configuration
   - Check browser console for errors

4. **Frontend Build Issues**
   - Clear node_modules and reinstall: `rm -rf node_modules package-lock.json && npm install`
   - Check Node.js version compatibility

## License

This project is for educational purposes as part of SWI22025 course.
