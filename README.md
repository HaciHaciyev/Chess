# Chess Game Backend

This is a backend application for a chess game, built using Java and the Quarkus framework.

## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Technologies Used](#technologies-used)
- [API Documentation](#api-documentation)
    - [Authentication](#authentication)
        - [Login](#login)
        - [Registration](#registration)
        - [Email Verification](#email-verification)
    - [Game Management](#game-management)
        - [Start Game](#start-game)
        - [Join Game](#join-game)
        - [Make Move](#make-move)
        - [Resign](#resign)
        - [Agree to Draw](#agree-to-draw)
        - [Threefold Repetition](#threefold-repetition)
        - [Return Move](#return-move)
    - [Chat](#chat)
        - [Send Message](#send-message)
- [Contributing](#contributing)

## Introduction
The Chess Game Backend is an application that provides the core functionality for a chess game. It handles user authentication, game management, and real-time chat between players.

## Features
- User registration and login
- Email verification for new users
- Creating and joining chess games
- Real-time updates on game state and chat messages
- Handling game actions (moves, resignations, agreements, etc.)
- Storing game history and results

## Technologies Used
- Java 21
- Quarkus framework
- WebSocket (JSR-356)
- JSON Web Tokens (JWT)
- Jackson for JSON processing
- PostgreSQL database
- Maven for build management

# Pre-requirements

To run the Chess Game Backend application, the following prerequisites are required:

1. **Java 21**: Ensure that you have Java 21 installed on your system.

2. **Maven**: Install Maven, a build automation tool for Java projects.

3. **GraalVM**: Download and install GraalVM, a high-performance runtime for running Java applications.

4. **Docker**: Install Docker, a containerization platform, to run the application and its dependencies.

5. **Generate RSA Keys**: Create the required RSA keys in PEM format by running the following commands in the terminal:

   ```bash
   openssl genrsa -out rsaPrivateKey.pem 2048
   openssl rsa -pubout -in rsaPrivateKey.pem -out publicKey.pem
   openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
   ```

   Place the generated `privateKey.pem` and `publicKey.pem` files in the `src/main/resources/` directory of your project.

6. **Create `infrastructure.env` File**: Create an `infrastructure.env` file in the root directory of your project with the following content:

   ```
   POSTGRES_USER=
   POSTGRES_URL=
   POSTGRES_PASSWORD=
   POSTGRES_DB=
   PGDATA=/data/postgres
   EMAIL_PASSWORD=
   ```

   Optionally, you can also create a `pgadmin.env` file in the root directory with the following content:

   ```
   PGADMIN_DEFAULT_EMAIL=
   PGADMIN_DEFAULT_PASSWORD=
   ```

7. **Create `application.properties` File**: Create an `application.properties` file in the `src/main/resources/` directory with the following content:

   ```properties
   quarkus.http.port=8080
   quarkus.smallrye-jwt.enabled=true
   quarkus.native.resources.includes[0]=privateKey.pem
   quarkus.native.resources.includes[1]=publicKey.pem

   quarkus.datasource.db-kind=postgresql
   quarkus.flyway.enabled=true
   quarkus.flyway.migrate-at-start=true
   quarkus.datasource.username=
   quarkus.datasource.password=
   quarkus.datasource.jdbc.url=
   quarkus.datasource.jdbc.max-size=16
   quarkus.datasource.jdbc.min-size=2

   smallrye.jwt.sign.key.location=privateKey.pem
   mp.jwt.verify.publickey.location=publicKey.pem
   mp.jwt.verify.issuer=

   quarkus.mailer.from=
   quarkus.mailer.host=
   quarkus.mailer.port=
   quarkus.mailer.username=
   quarkus.mailer.password=
   quarkus.mailer.start-tls=
   quarkus.mailer.auth-methods=
   ```

   Update the properties with your specific values, such as the PostgreSQL connection details and SMTP server settings.

8. **Running application** Use commands provided in files in src/main/docker directory.

After completing these steps, you should have all the necessary prerequisites to run the Chess Game Backend application.

## API Documentation

### Authentication

#### Login
- **Endpoint**: `POST /chessland/account/login`
- **Request Body**:
  ```json
  {
    "username": "example_user",
    "password": "password123"
  }
  ```
- **Response**: JWT token

#### Registration
- **Endpoint**: `POST /chessland/account/registration`
- **Request Body**:
  ```json
  {
    "username": "new_user",
    "email": "new_user@example.com",
    "password": "password123",
    "passwordConfirmation": "password123"
  }
  ```
- **Response**: JWT token

#### Email Verification
- **Endpoint**: `PATCH /chessland/account/token/verification?token={token}`
- **Response**: 200 OK

### Game Management

#### Start Game
- **Endpoint**: `POST /chess-game/start-game?color={color}&type={type}`
- **Response**: Game ID

#### Join Game
- **Endpoint**: `GET /chess-game/{gameId}`
- **WebSocket Endpoint**: `/chess-game/{gameId}`

#### Make Move
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "MOVE",
    "from": "e2",
    "to": "e4",
    "inCaseOfPromotion": "QUEEN"
  }
  ```
- **Response**: Updated chess board state

#### Resign
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "RESIGNATION"
  }
  ```
- **Response**: Game result

#### Agree to Draw
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "AGREEMENT"
  }
  ```
- **Response**: Game result

#### Threefold Repetition
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "TREE_FOLD"
  }
  ```
- **Response**: Game result

#### Return Move
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "RETURN_MOVE"
  }
  ```
- **Response**: Updated chess board state

### CHAT

#### Send Message
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "MESSAGE",
    "message": "Hello, opponent!"
  }
  ```
- **Response**: Updated chat messages

## Contributing
We welcome contributions to the Chess Game Backend project. If you find any issues or have suggestions for improvements, please feel free to open an issue or submit a pull request.