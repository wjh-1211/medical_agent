package com.medicalagent.tracing;

import java.util.logging.Logger;

public class StructuredLogTraceSink implements TraceSink {

    private static final Logger LOGGER = Logger.getLogger(StructuredLogTraceSink.class.getName());

    @Override
    public void record(TraceEvent event) {
        LOGGER.info(() -> "[trace] traceId=" + event.traceId()
                + " type=" + event.type()
                + " name=" + event.name()
                + " status=" + event.status()
                + " durationMs=" + event.durationMillis()
                + " error=" + event.errorCategory()
                + " input=" + event.inputSummary()
                + " output=" + event.outputSummary()
                + " attributes=" + event.attributes());
    }
}
