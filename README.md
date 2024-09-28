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
- [License](#license)

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

#### Send Message
- **Endpoint**: `POST /chess-game/{gameId}`
- **Request Body**:
  ```json
  {
    "type": "MESSAGE",
    "content": "Hello, opponent!"
  }
  ```
- **Response**: Updated chat messages

## Contributing
We welcome contributions to the Chess Game Backend project. If you find any issues or have suggestions for improvements, please feel free to open an issue or submit a pull request.