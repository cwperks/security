package org.opensearch.security.spi;

import java.util.Map;
import java.util.Set;

import org.opensearch.core.common.io.stream.NamedWriteable;
import org.opensearch.core.xcontent.ToXContentFragment;

public abstract class AbstractResource implements NamedWriteable, ToXContentFragment {
    protected ResourceUser resourceUser;
    protected String resourceId;

    public AbstractResource() {}

    public String getResourceId() {
        return resourceId;
    }

    public ResourceUser getResourceUser() {
        return resourceUser;
    }

    @SuppressWarnings("unchecked")
    public void fromSource(String resourceId, Map<String, Object> sourceAsMap) {
        this.resourceId = resourceId;
        if (sourceAsMap.containsKey("resource_user")) {
            Map<String, Object> userMap = (Map<String, Object>) sourceAsMap.get("resource_user");
            String username = (String) userMap.get("name");
            Set<String> backendRoles = (Set<String>) userMap.get("backend_roles");
            this.resourceUser = new ResourceUser(username, backendRoles);
        }
    }
}
