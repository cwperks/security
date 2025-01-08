package org.opensearch.security.sampleextension.resource;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

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
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.currentToken(), parser);
                    Deque<XContentParser.Token> ruStack = new ArrayDeque<>();
                    ruStack.add(parser.currentToken());
                    // TODO Complete the parsing here
                    while (!ruStack.isEmpty()) {
                        XContentParser.Token token = parser.nextToken();
                        if (XContentParser.Token.START_OBJECT.equals(token)) {
                            ruStack.add(token);
                        } else if (XContentParser.Token.END_OBJECT.equals(token)) {
                            ruStack.pop();
                        }
                        String field = parser.currentName();
                    }
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.END_OBJECT, parser.currentToken(), parser);
                    break;
                case "share_with":
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_ARRAY, parser.currentToken(), parser);
                    Deque<XContentParser.Token> swStack = new ArrayDeque<>();
                    swStack.add(parser.currentToken());
                    // TODO Complete the parsing here
                    while (!swStack.isEmpty()) {
                        XContentParser.Token token = parser.nextToken();
                        if (XContentParser.Token.START_ARRAY.equals(token)) {
                            swStack.add(token);
                        } else if (XContentParser.Token.END_ARRAY.equals(token)) {
                            swStack.pop();
                        }
                        String field = parser.currentName();
                    }
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.END_ARRAY, parser.currentToken(), parser);
                    break;
                default:
                    XContentParserUtils.throwUnknownToken(parser.currentToken(), parser.getTokenLocation());
            }
        }
        return resource;
    }
}
