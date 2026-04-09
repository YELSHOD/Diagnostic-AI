package com.yelshod.diagnosticserviceai.logs;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventAssembler {

    private static final int CONTEXT_SIZE = 20;
    private static final int MAX_NON_STACK_LINES = 8;
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("([\\w.$]+(?:Exception|Error))");

    private final EventAssemblyTimeoutScheduler timeoutScheduler;
    private final Map<String, State> states = new HashMap<>();

    public synchronized Optional<ErrorEvent> process(ParsedLogLine line, Consumer<ErrorEvent> timeoutConsumer) {
        State state = states.computeIfAbsent(line.service(), key -> new State());
        state.pushContext(line.raw());

        if (state.current == null) {
            if (isEventStart(line.raw())) {
                state.current = Draft.from(line, List.copyOf(state.context));
                rescheduleTimeout(state, line.service(), timeoutConsumer);
            }
            return Optional.empty();
        }

        if (looksLikeTimestampStart(line.raw()) && !isStackLine(line.raw()) && state.current.hasStack()) {
            ErrorEvent completed = state.current.toEvent();
            state.current = isEventStart(line.raw()) ? Draft.from(line, List.copyOf(state.context)) : null;
            if (state.current != null) {
                rescheduleTimeout(state, line.service(), timeoutConsumer);
            } else {
                cancelTimeout(state);
            }
            return Optional.of(completed);
        }

        state.current.addLine(line.raw(), line.timestamp(), line.traceId());
        if (isStackLine(line.raw())) {
            state.current.resetNonStackCounter();
        } else {
            state.current.incrementNonStackCounter();
        }
        rescheduleTimeout(state, line.service(), timeoutConsumer);

        if (state.current.nonStackCounter > MAX_NON_STACK_LINES) {
            ErrorEvent completed = state.current.toEvent();
            state.current = null;
            cancelTimeout(state);
            return Optional.of(completed);
        }
        return Optional.empty();
    }

    public synchronized Optional<ErrorEvent> flushService(String service) {
        return flushServiceInternal(service);
    }

    private synchronized Optional<ErrorEvent> flushServiceInternal(String service) {
        State state = states.get(service);
        if (state == null || state.current == null) {
            return Optional.empty();
        }
        ErrorEvent event = state.current.toEvent();
        state.current = null;
        cancelTimeout(state);
        return Optional.of(event);
    }

    private void rescheduleTimeout(State state, String service, Consumer<ErrorEvent> timeoutConsumer) {
        cancelTimeout(state);
        state.future = timeoutScheduler.schedule(service, () ->
                flushServiceInternal(service).ifPresent(timeoutConsumer));
    }

    private void cancelTimeout(State state) {
        if (state.future != null) {
            state.future.cancel();
            state.future = null;
        }
    }

    private boolean isEventStart(String line) {
        return line.contains("Exception") || line.contains("ERROR") || line.contains("Caused by");
    }

    private boolean isStackLine(String line) {
        String trimmed = line.stripLeading();
        return trimmed.startsWith("at ") || trimmed.startsWith("\\tat") || trimmed.startsWith("Caused by:") || trimmed.startsWith("...");
    }

    private boolean looksLikeTimestampStart(String line) {
        return line.matches("^\\s*\\d{4}-\\d{2}-\\d{2}.*");
    }

    private static final class State {
        private final Deque<String> context = new ArrayDeque<>();
        private Draft current;
        private EventAssemblyTimeoutScheduler.Cancellable future;

        private void pushContext(String line) {
            context.addLast(line);
            while (context.size() > CONTEXT_SIZE) {
                context.removeFirst();
            }
        }
    }

    private static final class Draft {
        private final String service;
        private Instant eventTime;
        private String traceId;
        private String exceptionType;
        private String message;
        private final List<String> stackLines = new ArrayList<>();
        private final List<String> lines = new ArrayList<>();
        private final List<String> context;
        private int nonStackCounter;

        private Draft(String service, Instant eventTime, String traceId, String firstLine, List<String> context) {
            this.service = service;
            this.eventTime = eventTime == null ? Instant.now() : eventTime;
            this.traceId = traceId;
            this.context = new ArrayList<>(context);
            addLine(firstLine, eventTime, traceId);
        }

        static Draft from(ParsedLogLine line, List<String> context) {
            return new Draft(line.service(), line.timestamp(), line.traceId(), line.raw(), context);
        }

        void addLine(String line, Instant timestamp, String lineTraceId) {
            lines.add(line);
            if (this.eventTime == null && timestamp != null) {
                this.eventTime = timestamp;
            }
            if (this.traceId == null && lineTraceId != null) {
                this.traceId = lineTraceId;
            }
            if (line.stripLeading().startsWith("at ") || line.stripLeading().startsWith("\\tat") || line.stripLeading().startsWith("Caused by:")) {
                stackLines.add(line);
            }
            if (this.exceptionType == null) {
                Matcher m = EXCEPTION_PATTERN.matcher(line);
                if (m.find()) {
                    this.exceptionType = m.group(1);
                }
            }
            if (this.message == null && !line.isBlank()) {
                this.message = line;
            }
        }

        boolean hasStack() {
            return !stackLines.isEmpty();
        }

        void incrementNonStackCounter() {
            nonStackCounter++;
        }

        void resetNonStackCounter() {
            nonStackCounter = 0;
        }

        ErrorEvent toEvent() {
            List<String> topFrames = stackLines.stream().limit(10).toList();
            List<String> contextLines = new ArrayList<>(context);
            contextLines.addAll(lines.stream().limit(20).toList());
            return new ErrorEvent(
                    service,
                    eventTime == null ? Instant.now() : eventTime,
                    traceId,
                    exceptionType == null ? "UnknownException" : exceptionType,
                    message,
                    topFrames,
                    String.join("\n", lines),
                    contextLines
            );
        }
    }
}
