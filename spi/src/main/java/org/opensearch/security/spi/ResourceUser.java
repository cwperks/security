package org.opensearch.security.spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opensearch.core.ParseField;
import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ConstructingObjectParser;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.ToXContentFragment;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import static org.opensearch.core.xcontent.ConstructingObjectParser.constructorArg;

public class ResourceUser implements NamedWriteable, ToXContentFragment {
    private final String name;

    private final List<String> roles;

    private final List<String> backendRoles;

    public ResourceUser(String name, List<String> roles, List<String> backendRoles) {
        this.name = name;
        this.roles = roles;
        this.backendRoles = backendRoles;
    }

    public ResourceUser(StreamInput in) throws IOException {
        this.name = in.readString();
        this.roles = in.readStringList();
        this.backendRoles = in.readStringList();
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getBackendRoles() {
        return backendRoles;
    }

    @SuppressWarnings("unchecked")
    public static ResourceUser fromSource(Map<String, Object> sourceAsMap) {
        String name = (String) sourceAsMap.get("name");
        List<String> roles = new ArrayList<>((List<String>) sourceAsMap.get("roles"));
        List<String> backendRoles = new ArrayList<>((List<String>) sourceAsMap.get("backend_roles"));
        return new ResourceUser(name, roles, backendRoles);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder.startObject().field("name", name).field("roles", roles).field("backend_roles", backendRoles).endObject();
    }

    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<ResourceUser, Void> PARSER = new ConstructingObjectParser<>(
        "resource_user",
        true,
        a -> new ResourceUser((String) a[0], (List<String>) a[1], (List<String>) a[2])
    );

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("name"));
        PARSER.declareStringArray(constructorArg(), new ParseField("roles"));
        PARSER.declareStringArray(constructorArg(), new ParseField("backend_roles"));
    }

    public static ResourceUser fromXContent(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }

    @Override
    public String getWriteableName() {
        return "resource_user";
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeStringCollection(roles);
        out.writeStringCollection(backendRoles);
    }
}
