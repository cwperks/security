package org.opensearch.security.plugin;

import java.io.IOException;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.core.common.io.stream.StreamInput;

public class IndexDocumentIntoSystemIndexRequest extends ActionRequest {

    private final String indexName;

    public IndexDocumentIntoSystemIndexRequest(String indexName) {
        this.indexName = indexName;
    }

    public IndexDocumentIntoSystemIndexRequest(StreamInput in) throws IOException {
        super(in);
        this.indexName = in.readString();
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getIndexName() {
        return this.indexName;
    }
}
