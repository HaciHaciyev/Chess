package core.project.chess.infrastructure.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class TelemetryService {

    private final Tracer tracer;

    TelemetryService(Tracer tracer) {
        this.tracer = tracer;
    }

    public void startWithSpan(String spanName, Runnable action) {
        Span span = tracer.spanBuilder(spanName).startSpan();

        try (Scope scope = span.makeCurrent()) {
            action.run();
        } catch (Exception e) {
            span.recordException(e);
        } finally {
            span.end();
        }
    }

    public void startWithSpan(String spanName, Map<String, String> attributes, Runnable action) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        attributes.forEach(span::setAttribute);

        try (Scope scope = span.makeCurrent()) {
            action.run();
        } catch (Exception e) {
            span.recordException(e);
        } finally {
            span.end();
        }
    }
}
