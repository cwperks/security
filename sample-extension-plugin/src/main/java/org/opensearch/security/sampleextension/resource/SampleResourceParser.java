package org.opensearch.security.sampleextension.resource;

import java.io.IOException;
import java.time.Instant;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.core.xcontent.XContentParserUtils;
import org.opensearch.security.spi.ResourceParser;

public class SampleResourceParser implements ResourceParser<SampleResource> {

    @Override
    public SampleResource parse(XContentParser parser, String id) throws IOException {
        SampleResource resource = new SampleResource();
        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);

        while (!XContentParser.Token.END_OBJECT.equals(parser.nextToken())) {
            String fieldName = parser.currentName();
            parser.nextToken();
            switch (fieldName) {
                case "name":
                    resource.setName(parser.text());
                    break;
                case "last_update_time":
                    resource.setLastUpdateTime(Instant.ofEpochMilli(parser.longValue()));
                    break;
                case "resource_user":
                    // TODO Complete the parsing here
                    while (!XContentParser.Token.END_OBJECT.equals(parser.nextToken())) {
                        String field = parser.currentName();
                    }
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.END_OBJECT, parser.currentToken(), parser);
                    break;
                case "share_with":
                    while (!XContentParser.Token.END_OBJECT.equals(parser.nextToken())) {
                        String field = parser.currentName();
                    }
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.END_OBJECT, parser.currentToken(), parser);
                    break;
                default:
                    XContentParserUtils.throwUnknownToken(parser.currentToken(), parser.getTokenLocation());
            }
        }
        return resource;
    }
}
