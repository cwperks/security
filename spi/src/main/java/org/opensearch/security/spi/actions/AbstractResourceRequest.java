package org.opensearch.security.spi.actions;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;

public abstract class AbstractResourceRequest extends ActionRequest {
    protected final String resourceIndex;

    /**
     * Default constructor
     */
    public AbstractResourceRequest(String resourceIndex) {
        this.resourceIndex = resourceIndex;
    }

    public AbstractResourceRequest(StreamInput in) throws IOException {
        this.resourceIndex = in.readString();
    }

    @Override
    public void writeTo(final StreamOutput out) throws IOException {
        out.writeString(resourceIndex);
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getResourceIndex() {
        return this.resourceIndex;
    }
}
