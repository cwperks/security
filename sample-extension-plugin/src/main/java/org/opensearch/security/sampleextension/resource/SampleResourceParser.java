package org.opensearch.security.sampleextension.resource;

import java.io.IOException;

import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.core.xcontent.XContentParserUtils;
import org.opensearch.security.spi.ResourceParser;

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
                default:
                    XContentParserUtils.throwUnknownToken(parser.currentToken(), parser.getTokenLocation());
            }
        }
        return resource;
    }
}
