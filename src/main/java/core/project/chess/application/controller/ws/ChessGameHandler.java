package core.project.chess.application.controller.ws;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.application.service.WSAuthService;
import core.project.chess.domain.commons.containers.Result;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.commons.value_objects.Username;
import core.project.chess.domain.user.entities.User;
import core.project.chess.infrastructure.ws.MessageDecoder;
import core.project.chess.infrastructure.ws.MessageEncoder;
import core.project.chess.infrastructure.ws.RateLimiter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;

import static core.project.chess.application.util.WSUtilities.closeSession;
import static core.project.chess.application.util.WSUtilities.sendMessage;

@ServerEndpoint(value = "/chessland/chess-game", decoders = MessageDecoder.class, encoders = MessageEncoder.class)
public class ChessGameHandler {

    private final RateLimiter rateLimiter;

    private final WSAuthService authService;

    private final ChessGameService chessGameService;

    ChessGameHandler(WSAuthService authService,
                     RateLimiter rateLimiter,
                     ChessGameService chessGameService) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.chessGameService = chessGameService;
    }

    @OnOpen
    @WithSpan("CHESS OPEN")
    public void onOpen(final Session session) {
        Thread.startVirtualThread(() ->
                authService.validateToken(session)
                        .handle(token -> chessGameService.onOpen(session, new Username(token.getName())),
                                throwable -> closeSession(session, Message.error(throwable.getLocalizedMessage())))
        );
    }

    @OnMessage
    @WithSpan("CHESS MESSAGE")
    public void onMessage(final Session session, final Message message) {
        Span.current().setAttribute("message.type", message.type().name());

        Thread.startVirtualThread(() -> {
            Result<JsonWebToken, IllegalStateException> parseResult = authService.validateToken(session);
            if (!parseResult.success()) {
                closeSession(session, Message.error(parseResult.throwable().getLocalizedMessage()));
                return;
            }

            Username username = new Username(parseResult.value().getName());
            Optional<Pair<Session, User>> findUser = chessGameService.user(username);
            if (findUser.isEmpty()) {
                closeSession(session, Message.error("Session do not contains user account."));
                return;
            }

            User user = findUser.get().getSecond();
            final boolean isRateDoNotLimited = rateLimiter.tryAcquire(user);
            if (!isRateDoNotLimited) {
                sendMessage(session, Message.error("You expose of message limits per time unit."));
                return;
            }

            chessGameService.onMessage(session, username, message);
        });
    }

    @OnClose
    @WithSpan("CHESS CLOSE")
    public void onClose(final Session session) {
        Thread.startVirtualThread(() ->
                authService.validateToken(session)
                        .handle(token -> chessGameService.onClose(session, new Username(token.getName())),
                                throwable -> closeSession(session, Message.error(throwable.getLocalizedMessage())))
        );
    }
}
