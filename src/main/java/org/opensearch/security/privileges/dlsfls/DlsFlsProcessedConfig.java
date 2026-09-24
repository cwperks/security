/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */
package org.opensearch.security.privileges.dlsfls;

import java.util.SortedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.opensearch.cluster.metadata.IndexAbstraction;
import org.opensearch.cluster.metadata.Metadata;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.security.privileges.ClusterStateMetadataDependentPrivileges;
import org.opensearch.security.privileges.CompiledRoles;

/**
 * Encapsulates the processed DLS/FLS configuration from roles.yml.
 * The current instance is held and managed by DlsFlsValveImpl.
 */
public class DlsFlsProcessedConfig extends ClusterStateMetadataDependentPrivileges {
    /** Allows a role without DLS/FLS/masking restrictions to override restrictions from other roles. */
    public static final Setting<Boolean> DFM_EMPTY_OVERRIDES_ALL = Setting.boolSetting(
        "plugins.security.dfm_empty_overrides_all",
        false,
        Setting.Property.NodeScope,
        Setting.Property.Dynamic,
        Setting.Property.Sensitive
    );

    private static final Logger log = LogManager.getLogger(DlsFlsProcessedConfig.class);

    private final DocumentPrivileges documentPrivileges;
    private final FieldPrivileges fieldPrivileges;
    private final FieldMasking fieldMasking;
    private long metadataVersionEffective = -1;

    public DlsFlsProcessedConfig(
        CompiledRoles compiledRoles,
        SortedMap<String, IndexAbstraction> indexMetadata,
        NamedXContentRegistry xContentRegistry,
        Settings settings,
        FieldMasking.Config fieldMaskingConfig
    ) {
        this.documentPrivileges = new DocumentPrivileges(compiledRoles, indexMetadata, xContentRegistry, settings);
        this.fieldPrivileges = new FieldPrivileges(compiledRoles, indexMetadata, settings);
        this.fieldMasking = new FieldMasking(compiledRoles, indexMetadata, fieldMaskingConfig, settings);
    }

    public DocumentPrivileges getDocumentPrivileges() {
        return this.documentPrivileges;
    }

    public FieldPrivileges getFieldPrivileges() {
        return this.fieldPrivileges;
    }

    public FieldMasking getFieldMasking() {
        return this.fieldMasking;
    }

    @Override
    protected void updateClusterStateMetadata(Metadata metadata) {
        long start = System.currentTimeMillis();
        SortedMap<String, IndexAbstraction> indexLookup = metadata.getIndicesLookup();

        this.documentPrivileges.updateIndices(indexLookup);
        this.fieldPrivileges.updateIndices(indexLookup);
        this.fieldMasking.updateIndices(indexLookup);

        long duration = System.currentTimeMillis() - start;

        log.debug("Updating DlsFlsProcessedConfig took {} ms", duration);
        this.metadataVersionEffective = metadata.version();
    }

    @Override
    protected long getCurrentlyUsedMetadataVersion() {
        return this.metadataVersionEffective;
    }
}
