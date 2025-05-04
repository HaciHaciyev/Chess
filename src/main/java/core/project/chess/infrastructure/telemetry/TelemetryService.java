package core.project.chess.infrastructure.telemetry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.function.Supplier;

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
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
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
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public void startWithChildSpan(String spanName, Runnable action) {
        Span parentSpan = Span.current();
        Span childSpan = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        try (Scope scope = childSpan.makeCurrent()) {
            action.run();
        } catch (Exception e) {
            childSpan.recordException(e);
            childSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            childSpan.end();
        }
    }

    public void startWithChildSpan(String spanName, Map<String, String> attributes, Runnable action) {
        Span parentSpan = Span.current();
        Span childSpan = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        attributes.forEach(childSpan::setAttribute);

        try (Scope scope = childSpan.makeCurrent()) {
            action.run();
        } catch (Exception e) {
            childSpan.recordException(e);
            childSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            childSpan.end();
        }
    }

    public <T> T startWithChildSpan(String spanName, Supplier<T> action) {
        Span parentSpan = Span.current();
        Span childSpan = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        try (Scope scope = childSpan.makeCurrent()) {
            return action.get();
        } catch (Exception e) {
            childSpan.recordException(e);
            childSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            childSpan.end();
        }
    }

    public <T> T startWithChildSpan(String spanName, Map<String, String> attributes, Supplier<T> action) {
        Span parentSpan = Span.current();
        Span childSpan = tracer.spanBuilder(spanName)
                .setParent(Context.current().with(parentSpan))
                .startSpan();

        attributes.forEach(childSpan::setAttribute);

        try (Scope scope = childSpan.makeCurrent()) {
            return action.get();
        } catch (Exception e) {
            childSpan.recordException(e);
            childSpan.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            childSpan.end();
        }
    }

    public Span getCurrentSpan() {
        return Span.current();
    }

    public void addEvent(String eventName) {
        Span.current().addEvent(eventName);
    }

    public void addEvent(String eventName, Map<String, String> attributes) {
        Span span = Span.current().addEvent(eventName);
        attributes.forEach(span::setAttribute);
    }

    public void setSpanAttribute(AttributeKey<String> key, String value) {
        Span.current().setAttribute(key, value);
    }

    public void setSpanAttribute(AttributeKey<String> key, long value) {
        Span.current().setAttribute(key.getKey(), value);
    }

    public void setSpanAttribute(AttributeKey<String> key, boolean value) {
        Span.current().setAttribute(key.getKey(), value);
    }
}