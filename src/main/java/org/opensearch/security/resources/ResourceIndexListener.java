/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.resources;

import java.io.IOException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.core.action.ActionListener;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.index.engine.Engine;
import org.opensearch.index.shard.IndexingOperationListener;
import org.opensearch.security.auth.UserSubjectImpl;
import org.opensearch.security.resources.sharing.CreatedBy;
import org.opensearch.security.resources.sharing.ResourceSharing;
import org.opensearch.security.resources.sharing.ShareWith;
import org.opensearch.security.setting.OpensearchDynamicSetting;
import org.opensearch.security.spi.resources.ResourceProvider;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.client.Client;

/**
 * This class implements an index operation listener for operations performed on resources stored in plugin's indices.
 *
 * @opensearch.experimental
 */
public class ResourceIndexListener implements IndexingOperationListener {

    private static final Logger log = LogManager.getLogger(ResourceIndexListener.class);
    private final ResourceSharingIndexHandler resourceSharingIndexHandler;

    private final ThreadPool threadPool;
    private final ResourcePluginInfo resourcePluginInfo;

    private final OpensearchDynamicSetting<Boolean> resourceSharingEnabledSetting;

    public ResourceIndexListener(
        ThreadPool threadPool,
        Client client,
        ResourcePluginInfo resourcePluginInfo,
        OpensearchDynamicSetting<Boolean> resourceSharingEnabledSetting
    ) {
        this.threadPool = threadPool;
        this.resourceSharingIndexHandler = new ResourceSharingIndexHandler(client, threadPool, resourcePluginInfo);
        this.resourcePluginInfo = resourcePluginInfo;
        this.resourceSharingEnabledSetting = resourceSharingEnabledSetting;
    }

    /**
     * Creates a resource sharing entry for the newly created resource.
     */
    @Override
    public void postIndex(ShardId shardId, Engine.Index index, Engine.IndexResult result) {

        if (!resourceSharingEnabledSetting.getDynamicSettingValue()) {
            // feature is disabled
            return;
        }
        String concreteIndex = shardId.getIndexName();

        if (!resourcePluginInfo.isProtectedResourceIndex(concreteIndex)) {
            // type is marked as not protected
            return;
        }

        log.debug("postIndex called on {}", concreteIndex);

        String resourceType = resourcePluginInfo.getResourceTypeForIndexOp(concreteIndex, index);

        String resourceId = index.id();
        ResourceProvider provider = resourcePluginInfo.getResourceProvider(resourceType);

        // For wildcard providers (e.g. .kibana*), use the resolved alias for the sharing index
        // so reads via the alias can find the sharing record.
        // For concrete providers, use the concrete index as before.
        String resolvedIndex = concreteIndex;
        if (provider != null && provider.resourceIndexName().contains("*")) {
            final UserSubjectImpl userSubjectForIndex = (UserSubjectImpl) threadPool.getThreadContext()
                .getPersistent(ConfigConstants.OPENDISTRO_SECURITY_AUTHENTICATED_USER);
            String tenant = (userSubjectForIndex != null) ? userSubjectForIndex.getUser().getRequestedTenant() : null;
            String resolved = resourcePluginInfo.resolveIndexForType(resourceType, tenant);
            if (resolved != null) {
                resolvedIndex = resolved;
            }
        }
        final String resourceIndex = resolvedIndex;
        if (provider == null) {
            log.warn(
                "Failed to create a resource sharing entry for resource: {} with type: {}. The type is not declared as a protected type in plugins.security.experimental.resource_sharing.protected_types.",
                resourceId,
                resourceType
            );
            return;
        }

        // Only proceed if this was a create operation and for primary shard
        if (!index.origin().equals(Engine.Operation.Origin.PRIMARY)) {
            log.debug("Skipping resource sharing entry creation for {} as this operation was on a replica shard", resourceId);
            return;
        }

        if (!result.isCreated()) {
            ActionListener<Void> visibilityListener = ActionListener.wrap(unused -> {
                log.debug(
                    "postIndex: Successfully updated the resource visibility for resource {} within index {}",
                    resourceId,
                    resourceIndex
                );
            }, e -> { log.debug(e.getMessage()); });

            // Check if the resource's parent changed
            if (provider.parentType() != null && provider.parentIdField() != null) {
                String newParentId = ResourcePluginInfo.extractFieldFromIndexOp(provider.parentIdField(), index);
                String normalizedNewParentId = (newParentId == null || newParentId.isBlank()) ? null : newParentId;

                // Compare with the sharing record's current parentId to detect actual changes
                resourceSharingIndexHandler.fetchSharingInfo(resourceIndex, resourceId, ActionListener.wrap(sharingInfo -> {
                    String currentParentId = (sharingInfo != null) ? sharingInfo.getParentId() : null;
                    String normalizedCurrentParentId = (currentParentId == null || currentParentId.isBlank()) ? null : currentParentId;

                    boolean parentChanged = !java.util.Objects.equals(normalizedNewParentId, normalizedCurrentParentId);
                    if (!parentChanged) {
                        // Parent didn't change — just update visibility normally
                        resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(resourceId, resourceIndex, visibilityListener);
                        return;
                    }

                    log.info(
                        "postIndex update: resource={} parentId changed from {} to {}",
                        resourceId,
                        normalizedCurrentParentId,
                        normalizedNewParentId
                    );
                    if (normalizedNewParentId != null) {
                        inheritParentSharing(
                            resourceId,
                            resourceIndex,
                            resourceType,
                            normalizedNewParentId,
                            provider.parentType(),
                            visibilityListener
                        );
                    } else {
                        revertToPrivate(resourceId, resourceIndex, visibilityListener);
                    }
                }, e -> { resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(resourceId, resourceIndex, visibilityListener); }));
                return;
            }

            this.resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(resourceId, resourceIndex, visibilityListener);
            return;
        }

        final UserSubjectImpl userSubject = (UserSubjectImpl) threadPool.getThreadContext()
            .getPersistent(ConfigConstants.OPENDISTRO_SECURITY_AUTHENTICATED_USER);
        final User user = userSubject.getUser();

        try {
            Objects.requireNonNull(user);
            ActionListener<ResourceSharing> listener = ActionListener.wrap(entry -> {
                log.debug(
                    "postIndex: Successfully created a resource sharing entry {} for resource {} within index {}",
                    entry,
                    resourceId,
                    resourceIndex
                );
            }, e -> { log.debug(e.getMessage()); });
            // User.getRequestedTenant() is null if multi-tenancy is disabled
            ResourceSharing.Builder builder = ResourceSharing.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .createdBy(new CreatedBy(user.getName(), user.getRequestedTenant()));
            if (provider.parentType() != null) {
                builder.parentType(provider.parentType())
                    .parentId(ResourcePluginInfo.extractFieldFromIndexOp(provider.parentIdField(), index));
            }
            ResourceSharing sharingInfo = builder.build();

            // Apply default general access if the provider specifies one
            if (provider.defaultGeneralAccess() != null) {
                sharingInfo.setGeneralAccess(provider.defaultGeneralAccess());
            }

            this.resourceSharingIndexHandler.indexResourceSharing(resourceIndex, sharingInfo, listener);

        } catch (IOException e) {
            log.debug("Failed to create a resource sharing entry for resource: {}", resourceId, e);
        }
    }

    /**
     * When a resource is moved into a parent (e.g. document moved into a folder),
     * copy the parent's sharing configuration to the child.
     */
    /**
     * Reverts a resource to private (owner-only) by clearing its share_with.
     * Used when a resource is moved out of a shared folder to ungrouped.
     */
    private void revertToPrivate(String resourceId, String resourceIndex, ActionListener<Void> then) {
        log.info("revertToPrivate: clearing sharing for resource {}", resourceId);
        resourceSharingIndexHandler.replaceSharing(resourceId, resourceIndex, null, null, ActionListener.wrap(result -> {
            log.info("revertToPrivate: successfully reverted resource {} to private", resourceId);
            resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(resourceId, resourceIndex, then);
        }, e -> {
            log.warn("revertToPrivate: failed for resource {}: {}", resourceId, e.getMessage());
            resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(resourceId, resourceIndex, then);
        }));
    }

    private void inheritParentSharing(
        String childResourceId,
        String childResourceIndex,
        String childResourceType,
        String parentId,
        String parentType,
        ActionListener<Void> then
    ) {
        String parentIndex = resourcePluginInfo.indexByType(parentType);
        if (parentIndex == null) {
            log.warn("inheritParentSharing: no index found for parent type {}", parentType);
            then.onResponse(null);
            return;
        }
        String childDefaultAccessLevel = resourcePluginInfo.getDefaultAccessLevel(childResourceType);
        if (childDefaultAccessLevel == null) {
            log.warn("inheritParentSharing: no default access level for child type {}", childResourceType);
            resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then);
            return;
        }
        log.info(
            "inheritParentSharing: child={} parentId={} parentType={} childDefaultAccess={}",
            childResourceId,
            parentId,
            parentType,
            childDefaultAccessLevel
        );
        resourceSharingIndexHandler.fetchSharingInfo(parentIndex, parentId, ActionListener.wrap(parentSharing -> {
            if (parentSharing == null) {
                log.info("inheritParentSharing: no sharing record found for parent {}, clearing child sharing", parentId);
                resourceSharingIndexHandler.replaceSharing(
                    childResourceId,
                    childResourceIndex,
                    null,
                    parentId,
                    ActionListener.wrap(
                        r -> resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then),
                        e -> resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then)
                    )
                );
                return;
            }
            if (parentSharing.getShareWith() == null || parentSharing.getShareWith().isPrivate()) {
                log.info("inheritParentSharing: parent {} has no sharing configured, clearing child sharing", parentId);
                resourceSharingIndexHandler.replaceSharing(
                    childResourceId,
                    childResourceIndex,
                    null,
                    parentId,
                    ActionListener.wrap(
                        r -> resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then),
                        e -> resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then)
                    )
                );
                return;
            }
            log.info("inheritParentSharing: propagating sharing from parent {} to child {}", parentId, childResourceId);
            ShareWith childShareWith = parentSharing.getShareWith().remapToAccessLevel(childDefaultAccessLevel);
            resourceSharingIndexHandler.replaceSharing(
                childResourceId,
                childResourceIndex,
                childShareWith,
                parentId,
                ActionListener.wrap(result -> {
                    log.info("inheritParentSharing: successfully inherited sharing for {}", childResourceId);
                    // Now update visibility AFTER sharing is written
                    resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then);
                }, e -> {
                    log.warn(
                        "Failed to inherit parent sharing for resource {} from parent {}: {}",
                        childResourceId,
                        parentId,
                        e.getMessage()
                    );
                    resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then);
                })
            );
        }, e -> {
            log.warn("Could not fetch parent sharing for {}: {}", parentId, e.getMessage());
            resourceSharingIndexHandler.fetchAndUpdateResourceVisibility(childResourceId, childResourceIndex, then);
        }));
    }

    /**
     * Deletes the resource sharing entry for the deleted resource.
     */
    @Override
    public void postDelete(ShardId shardId, Engine.Delete delete, Engine.DeleteResult result) {
        if (!resourceSharingEnabledSetting.getDynamicSettingValue()) {
            // feature is disabled
            return;
        }
        String resourceIndex = shardId.getIndexName();

        if (!resourcePluginInfo.isProtectedResourceIndex(resourceIndex)) {
            // type is marked as not protected
            return;
        }

        log.debug("postDelete called on {}", resourceIndex);

        String resourceId = delete.id();
        this.resourceSharingIndexHandler.deleteResourceSharingRecord(resourceId, resourceIndex, ActionListener.wrap(deleted -> {
            if (deleted) {
                log.debug("Successfully deleted resource sharing entry for resource {}", resourceId);
            } else {
                log.debug("No resource sharing entry found for resource {}", resourceId);
            }
        }, exception -> log.error("Failed to delete resource sharing entry for resource {}", resourceId, exception)));
    }
}
