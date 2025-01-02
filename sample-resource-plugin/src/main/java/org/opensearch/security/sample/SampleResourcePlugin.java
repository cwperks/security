/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.sample;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.action.ActionRequest;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.core.action.ActionResponse;
import org.opensearch.core.common.io.stream.NamedWriteableRegistry;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.indices.SystemIndexDescriptor;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.ResourcePlugin;
import org.opensearch.plugins.SystemIndexPlugin;
import org.opensearch.plugins.resource.SharableResourceType;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.script.ScriptService;
import org.opensearch.security.sample.actions.impl.create.CreateSampleResourceAction;
import org.opensearch.security.sample.actions.impl.create.CreateSampleResourceRestAction;
import org.opensearch.security.sample.actions.impl.create.CreateSampleResourceTransportAction;
import org.opensearch.security.sample.actions.impl.get.GetSampleResourceAction;
import org.opensearch.security.sample.actions.impl.get.GetSampleResourceRestAction;
import org.opensearch.security.sample.actions.impl.get.GetSampleResourceTransportAction;
import org.opensearch.security.sample.actions.impl.list.ListSampleResourceAction;
import org.opensearch.security.sample.actions.impl.list.ListSampleResourceRestAction;
import org.opensearch.security.sample.actions.impl.list.ListSampleResourceTransportAction;
import org.opensearch.security.sample.actions.update.UpdateSampleResourceAction;
import org.opensearch.security.sample.actions.update.UpdateSampleResourceRestAction;
import org.opensearch.security.sample.actions.update.UpdateSampleResourceTransportAction;
import org.opensearch.security.sample.resource.SampleResourceType;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

/**
 * Sample Resource Plugin.
 *
 * It use ".sample_resources" index to manage its resources, and exposes a REST API
 *
 */
public class SampleResourcePlugin extends Plugin implements ActionPlugin, SystemIndexPlugin, ResourcePlugin {
    private static final Logger log = LogManager.getLogger(SampleResourcePlugin.class);

    public static final String RESOURCE_INDEX_NAME = ".sample_resources";

    private Client client;

    @Override
    public Collection<Object> createComponents(
        Client client,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        this.client = client;
        return Collections.emptyList();
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        return List.of(
            new CreateSampleResourceRestAction(),
            new GetSampleResourceRestAction(),
            new ListSampleResourceRestAction(),
            new UpdateSampleResourceRestAction()
        );
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return List.of(
            new ActionHandler<>(CreateSampleResourceAction.INSTANCE, CreateSampleResourceTransportAction.class),
            new ActionHandler<>(GetSampleResourceAction.INSTANCE, GetSampleResourceTransportAction.class),
            new ActionHandler<>(ListSampleResourceAction.INSTANCE, ListSampleResourceTransportAction.class),
            new ActionHandler<>(UpdateSampleResourceAction.INSTANCE, UpdateSampleResourceTransportAction.class)
        );
    }

    @Override
    public Collection<SystemIndexDescriptor> getSystemIndexDescriptors(Settings settings) {
        final SystemIndexDescriptor systemIndexDescriptor = new SystemIndexDescriptor(RESOURCE_INDEX_NAME, "Example index with resources");
        return Collections.singletonList(systemIndexDescriptor);
    }

    @Override
    public List<SharableResourceType> getResourceTypes() {
        return List.of(SampleResourceType.getInstance());
    }
}
