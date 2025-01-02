package org.opensearch.security.sample.resource;

import java.io.IOException;
import java.time.Instant;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.core.xcontent.XContentParserUtils;
import org.opensearch.plugins.resource.ResourceParser;

public class SampleResourceParser implements ResourceParser<SampleResource> {

    @Override
    public SampleResource parse(XContentParser parser, String id) throws IOException {
        SampleResource resource = new SampleResource();
        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);

        while (!parser.nextToken().equals(XContentParser.Token.END_OBJECT)) {
            String fieldName = parser.currentName();
            parser.nextToken();
            switch (fieldName) {
                case "name":
                    resource.setName(parser.text());
                    break;
                case "last_update_time":
                    resource.setLastUpdateTime(parseInstantValue(parser));
                    break;
                default:
                    XContentParserUtils.throwUnknownToken(parser.currentToken(), parser.getTokenLocation());
            }
        }
        return resource;
    }

    private Instant parseInstantValue(XContentParser parser) throws IOException {
        if (XContentParser.Token.VALUE_NULL.equals(parser.currentToken())) {
            return null;
        }
        if (parser.currentToken().isValue()) {
            return Instant.ofEpochMilli(parser.longValue());
        }
        XContentParserUtils.throwUnknownToken(parser.currentToken(), parser.getTokenLocation());
        return null;
    }
}
