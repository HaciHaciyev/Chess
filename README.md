# Chessland
Chessland - A platform for Chess lovers.
The server side consists of two applications:
- https://github.com/HaciHaciyev/Chess
- https://github.com/HaciHaciyev/MessagingService

## Features
- User registration, authentication, and email verification
- Chess game creation and participation (player & observer modes)
- Real-time game updates and chat functionality
- Handling various game actions (moves, undo requests, resignations, draws, threefold repetition, etc.)
- Rating system based on Glicko2
- Partnership management, including partner games
- Storing game history and results

## Technologies Used
- **Programming Language:** Java 21
- **Framework:** Quarkus (3.18.2)
- **Database:** PostgreSQL (JDBC, Agroal, Flyway)
- **Authentication & Security:** SmallRye JWT, Argon2
- **Caching & Distributed Processing:** Redis, Redisson
- **Real-Time Communication:** WebSockets
- **Email Services:** Quarkus Mailer, Mailpit
- **File Parsing:** Apache Tika
- **Logging:** JBoss LogManager
- **Testing:** JUnit 5, Rest-Assured, AssertJ, Awaitility, Testcontainers, DataFaker
- **Build & Dependency Management:** Maven

## Base URL
All API endpoints are prefixed with:
```
/chessland
```

## Authentication & Authorization
Most endpoints require authentication using JWT. Except login, registration endpoints.

---

## Authentication

### Login
**Endpoint:**
```
POST /account/login
```
**Request Body:** (JSON)
```json
{
  "username": "string",
  "password": "string"
}
```
**Responses:**
- `200 OK` – Returns JWT token and refreshToken upon successful authentication. Body:
```json
{
  "token": "string",
  "refreshToken": "string"
}
```
- `400 BAD REQUEST` – If the login form is null.

---

### Registration
**Endpoint:**
```
POST /account/registration
```
**Request Body:** (JSON)
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "passwordConfirmation": "string"
}
```
**Responses:**
- `200 OK` – Registration successful. Email verification is required.
- `400 BAD REQUEST` – If the registration form is null.

---

### Email Verification
**Endpoint:**
```
PATCH /account/token/verification?token={token}
```
**Responses:**
- `200 OK` – Account is now enabled.
- `400 BAD REQUEST` – If the token is null or invalid.

---

### Refresh Token
**Endpoint:**
```
PATCH /account/refresh-token
```
**Headers:**
```
Refresh-Token: string
```
**Responses:**
- `200 OK` – Returns a new access token.
- `400 BAD REQUEST` – If the refresh token is invalid or blank.

---

## User Data

### Get User Properties
**Endpoint:**
```
GET /account/user-properties
```
**Responses:**
- `200 OK` – Returns user properties. Body:
```json
{
  "username": "string",
  "email": "string",
  "rating": "number"
}
```
- `400 BAD REQUEST` – If user properties are not found.

---

## Game History

### Get Game History
**Endpoint:**
```
GET /account/game-history?pageNumber={pageNumber}&pageSize={pageSize}
```
**Responses:**
- `200 OK` – Returns a list of game history. Fixed size of page is 10. Body
```json
{
  "gameHistory": [
    {
      "chessHistoryId": "string (UUID)",
      "pgn": "string",
      "fenRepresentations": "string[]",
      "playerForWhite": {
        "username": "string"
      },
      "playerForBlack": {
        "username": "string"
      },
      "timeControl": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\"",
      "gameResult": "\"DRAW\" | \"WHITE_WIN\" | \"BLACK_WIN\"",
      "whitePlayerRating": "number",
      "blackPlayerRating": "number",
      "gameStart": "string (ISO 8601 datetime)",
      "gameEnd": "string (ISO 8601 datetime)"
    }
  ]
}
```
- `400 BAD REQUEST` – If the user does not exist.

---

## Profile Pictures

### Upload Profile Picture
**Endpoint:**
```
PUT /account/put-profile-picture
```
**Consumes:**
```
application/octet-stream
```
**Request Body:**
Binary image file.

**Responses:**
- `202 ACCEPTED` – Successfully uploaded.
- `400 BAD REQUEST` – If the picture is null or the username is invalid.

---

### Get Profile Picture
**Endpoint:**
```
GET /account/profile-picture
```
**Responses:**
- `200 OK` – Returns the profile picture and its type.
- `400 BAD REQUEST` – If the username is invalid.

---

### Delete Profile Picture
**Endpoint:**
```
DELETE /account/delete-profile-picture
```
**Responses:**
- `202 ACCEPTED` – Successfully deleted.
- `400 BAD REQUEST` – If the username is invalid.

---

## Partners

### Get List of Partners
**Endpoint:**
```
GET /account/partners?pageNumber={pageNumber}&pageSize={pageSize}
```
**Responses:**
- `200 OK` – Returns a list of partners usernames. Fixed size 10.

---

### Remove Partner
**Endpoint:**
```
DELETE /account/remove-partner?partner={partner}
```
**Responses:**
- `204 NO CONTENT` – Partner removed successfully.

---

## Root Endpoint

### Base API Path
All requests are under:
```
/chessland
```

### WebSocket API

### Overview
The WebSocket API includes two endpoints for different services:
1. **User Session Service** - Handles partnerships management.
2. **Chess Game Service** - Allows players to initialize, play and observe chess games.

### 1. User Session WebSocket

**Endpoint:** `/chessland/user-session?token={token}`

When a user connects to the `/chessland/user-session` endpoint, the server:
1. **Token Validation**: The server checks the validity of the user's token.
2. **Session Initialization**: If the token is valid, the server processes the user's session and checks for pending partnership requests.

The server can return a list of **pending partnership requests** as soon as the connection is established,
if there are any requests for partnership or other types of messages while the user was not online:

```json
[
   {
      "type": "PARTNERSHIP_REQUEST",
      "message": "You have a pending partnership request from User123.",
      "partner": "User123"
   }
]
```

The message format for a **partnership request** is as follows:

```json
{
  "type": "PARTNERSHIP_REQUEST",
  "message": "You have a pending partnership request from User123.",
  "partner": "User123"
}
```

In order to respond to a partnership request you must send:
To agree to a partnership:
```json
{
  "type": "PARTNERSHIP_REQUEST",
  "message": "Hi",
  "partner": "UserThatSendYouPartnershipRequest"
}
```
To decline to a partnership:
```json
{
  "type": "PARTNERSHIP_DECLINE",
  "partner": "UserThatSendYouPartnershipRequest"
}
```

---

#### 1. Chess Game WebSocket

**Endpoint:** `/chessland/chess-game?token={token}`

When a user connects to the `/chessland/chess-game` endpoint, the server performs the following actions:
1. **Token Validation**: The server checks if the user's token is valid.
2. **Session Initialization**: If the token is valid, the server initiates a chess game session.
3. **Sending Pending Partnership Invitations**: If there are any pending partnership invitations
   or requests for the user (messages that were sent while the user was offline), these invitations are sent as a list of messages.

The message format for a **partnership invitation** is structured as follows:

```json
{
  "type": "INVITATION",
  "message": "User123 has invited you to a game.",
  "partner": "User123",
  "color": "white",
  "time": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\"",
  "isCasualGame": true
}
```

This response will be returned as a **list of messages** when the user connects:

```json
[
  {
    "type": "INVITATION",
    "message": "User123 has invited you to a game.",
    "partner": "User123",
    "color": "white",
    "time": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\"",
    "isCasualGame": true
  }
]
```

### Chess Game initialization, reconnection and observing

Random game. Always will be with rating changes. For playing chess-game with random opponent you must send:

```json
{
   "type": "GAME_INIT"
}
```

Also, you can define parameters for a game:

```json
{
   "type": "GAME_INIT",
   "color": "\"WHITE\" | \"BLACK\"",
   "time": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\""
}
```

To cancel the game search:

```json
{
   "type": "GAME_INIT",
   "respond": "NO"
}
```

To connect to an existing game you must send a message below. 
If you were a player in this chess game you will be reconnected as a player, otherwise you will be connected as an observer.

```json
{
   "type": "GAME_INIT",
   "gameID": "string (UUID)"
}
```

For partnership games you must send a message below. 
Please note that you must have a partnership with the player you are inviting.
You also cannot invite a player again if the previous invitation was not accepted or declined, or has expired.

```json
{
   "type": "GAME_INIT",
   "partner": "User123"
}
```

You can also define game parameters:
```json
{
   "type": "GAME_INIT",
   "partner": "User123",
   "color": "white",
   "time": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\"",
   "isCasualGame": "boolean",
   "FEN": "string (Forsyth-Edwards Notation)",
   "PGN": "string (Portable Game Notation)"
}
```

To respond to an invitation to a partner chess game:
```json
{
   "type": "GAME_INIT",
   "partner": "UserThatInviteYouToPartnershipGame",
   "respond": "\"YES\" | \"NO\""
}
```

When a player successfully starts a game, the server sends two WebSocket messages containing game-related information.

Message: GAME_START_INFO

Sent when the game starts, providing details about the players and game settings.

Payload Example:
```json
{
  "type": "GAME_START_INFO",
  "gameID": "string (UUID)",
  "whitePlayerUsername": "Player1",
  "blackPlayerUsername": "Player2",
  "whitePlayerRating": 1500,
  "blackPlayerRating": 1450,
  "time": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\""
}
```

Message: FEN_PGN

Sent after the game starts, providing the initial FEN position and PGN history.

Payload Example:
```json
{
  "type": "FEN_PGN",
  "gameID": "string (UUID)",
  "FEN": "string (Forsyth-Edwards Notation)",
  "PGN": "string (Portable Game Notation)"
}
```

Notes

These messages are only sent when the game has successfully started.

Clients should handle these messages to update the game UI accordingly.

### Game features

Chess Board Coordinates format:
```json
{
   "coordinate": "\"a8\" | \"b8\" | \"c8\" | \"d8\" | \"e8\" | \"f8\" | \"g8\" | \"h8\" | \"a7\" | \"b7\" | \"c7\" | \"d7\" | \"e7\" | \"f7\" | \"g7\" | \"h7\" | \"a6\" | \"b6\" | \"c6\" | \"d6\" | \"e6\" | \"f6\" | \"g6\" | \"h6\" | \"a5\" | \"b5\" | \"c5\" | \"d5\" | \"e5\" | \"f5\" | \"g5\" | \"h5\" | \"a4\" | \"b4\" | \"c4\" | \"d4\" | \"e4\" | \"f4\" | \"g4\" | \"h4\" | \"a3\" | \"b3\" | \"c3\" | \"d3\" | \"e3\" | \"f3\" | \"g3\" | \"h3\" | \"a2\" | \"b2\" | \"c2\" | \"d2\" | \"e2\" | \"f2\" | \"g2\" | \"h2\" | \"a1\" | \"b1\" | \"c1\" | \"d1\" | \"e1\" | \"f1\" | \"g1\" | \"h1\""
}
```

For move:
```json
{
   "type": "MOVE",
   "gameID": "string (UUID)",
   "from": "coordinate",
   "to": "coordinate",
   "inCaseOfPromotion": "null | \"Q\" | \"B\" | \"N\" | \"R\" | \"q\" | \"b\" | \"n\" | \"r\""
}
```

For undo move you must send message below.
The opponent will have to send a similar request to agree if he wants, otherwise, he can make a move and the request will be cancelled.
```json
{
   "type": "RETURN_MOVE",
   "gameID": "string (UUID)"
}
```

For write a message for game chat:
```json
{
   "type": "MESSAGE",
   "gameID": "string (UUID)",
   "message": "string"
}
```

For agreement request you must send message below.
The opponent will have to send a similar request to agree if he wants, otherwise, he can make a move and the request will be cancelled.
```json
{
   "type": "AGREEMENT",
   "gameID": "string (UUID)"
}
```

For resignation:
```json
{
   "type": "RESIGNATION",
   "gameID": "string (UUID)"
}
```

If a ThreeFold occurs on the board, either player may request that the game end in a draw:
```json
{
   "type": "TREE_FOLD",
   "gameID": "string (UUID)"
}
```

After a successful move or return of a move, the client will receive:
```json
{
   "type": "FEN_PGN",
   "gameID": "string (UUID)",
   "FEN": "string (Forsyth-Edwards Notation)",
   "PGN": "string (Portable Game Notation)",
   "timeLeft": "\"BULLET\" | \"BLITZ\" | \"RAPID\" | \"CLASSIC\" | \"DEFAULT\"",
   "isThreeFoldActive": "boolean"
}
```

In case of any error, the client will receive:

```json
{
   "type": "ERROR",
   "message": "string"
}
```

or

```json
{
   "type": "ERROR",
   "gameID": "gameID",
   "message": "string"
}
```

## Contributing
We welcome contributions to the Chess Game Backend project. We also welcome front-end developers and designers for the client side.
If you find any issues or have suggestions for improvements, please feel free to open an issue or submit a pull request.

## License

This project is licensed under the **Prosperity Public License 3.0.0**.  
You are free to use, modify, and distribute this software **for non-commercial purposes**.

For commercial use, please contact the author for a separate license agreement.  
The full text of the license is available in the [LICENSE](./LICENSE) file or at [prosperitylicense.com](https://prosperitylicense.com/).
