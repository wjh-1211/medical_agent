package com.medicalagent.evaluation;

import com.medicalagent.config.ModelConfig;
import com.medicalagent.model.LocalModelGateway;
import com.medicalagent.model.LocalModelLoadListener;
import com.medicalagent.model.LocalModelRequest;
import com.medicalagent.model.LocalModelResponse;

public class RecordingLocalModelGateway implements LocalModelGateway {

    private final LocalModelGateway delegate;
    private long decisionMillis;
    private long mainModelMillis;

    public RecordingLocalModelGateway(LocalModelGateway delegate) {
        this.delegate = delegate;
    }

    @Override
    public String provider() {
        return delegate.provider();
    }

    @Override
    public ModelConfig descriptor() {
        return delegate.descriptor();
    }

    @Override
    public void preload() {
        delegate.preload();
    }

    @Override
    public void preload(LocalModelLoadListener listener) {
        delegate.preload(listener);
    }

    @Override
    public LocalModelResponse generate(LocalModelRequest request) {
        LocalModelResponse response = delegate.generate(request);
        if (request.prompt().contains("# Knowledge Retrieval Decision Agent")) {
            decisionMillis += response.elapsedMillis();
        } else {
            mainModelMillis += response.elapsedMillis();
        }
        return response;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    public void reset() {
        decisionMillis = 0L;
        mainModelMillis = 0L;
    }

    public long decisionMillis() {
        return decisionMillis;
    }

    public long mainModelMillis() {
        return mainModelMillis;
    }
}
