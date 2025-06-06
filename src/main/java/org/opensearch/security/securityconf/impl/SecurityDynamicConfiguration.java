/*
 * Copyright 2015-2017 floragunn GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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

package org.opensearch.security.securityconf.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import org.opensearch.ExceptionsHelper;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.security.DefaultObjectMapper;
import org.opensearch.security.securityconf.DynamicConfigFactory;
import org.opensearch.security.securityconf.Hashed;
import org.opensearch.security.securityconf.Hideable;
import org.opensearch.security.securityconf.StaticDefinable;

public class SecurityDynamicConfiguration<T> implements ToXContent {

    public static final int CURRENT_VERSION = 2;

    private static final TypeReference<HashMap<String, Object>> typeRefMSO = new TypeReference<HashMap<String, Object>>() {
    };

    @JsonIgnore
    private final Map<String, T> centries = new HashMap<>();
    @JsonIgnore
    private final Object modificationLock = new Object();
    private long seqNo = -1;
    private long primaryTerm = -1;
    private CType<T> ctype;
    private int version = CURRENT_VERSION;

    public static <T> SecurityDynamicConfiguration<T> empty(CType<T> ctype) {
        SecurityDynamicConfiguration<T> result = new SecurityDynamicConfiguration<T>(ctype);
        result._meta = new Meta();
        result._meta.setType(ctype.toLCString());
        result._meta.setConfig_version(CURRENT_VERSION);
        return result;
    }

    @JsonIgnore
    public boolean notEmpty() {
        return !centries.isEmpty();
    }

    public static <T> SecurityDynamicConfiguration<T> fromJson(String json, CType<T> ctype, int version, long seqNo, long primaryTerm)
        throws IOException {
        return fromJson(json, ctype, version, seqNo, primaryTerm, false);
    }

    /**
     * Creates the SecurityDynamicConfiguration instance from the given JSON. If a config version is found, which
     * is not the current one, it will be automatically converted into the current configuration version.
     */
    public static <T> SecurityDynamicConfiguration<T> fromJson(
        String json,
        CType<T> ctype,
        int version,
        long seqNo,
        long primaryTerm,
        boolean acceptInvalid
    ) throws IOException {
        SecurityDynamicConfiguration<T> sdc = null;
        if (ctype != null) {
            sdc = DefaultObjectMapper.readValue(
                json,
                DefaultObjectMapper.getTypeFactory().constructParametricType(SecurityDynamicConfiguration.class, ctype.getConfigClass())
            );

            validate(sdc, version, ctype);

        } else {
            sdc = new SecurityDynamicConfiguration<T>();
        }

        sdc.ctype = ctype;
        sdc.seqNo = seqNo;
        sdc.primaryTerm = primaryTerm;
        sdc.version = version;

        return sdc;
    }

    /**
     * For testing only
     */
    public static <T> SecurityDynamicConfiguration<T> fromMap(Map<String, Object> map, CType<T> ctype) throws JsonProcessingException {
        SecurityDynamicConfiguration<T> result = DefaultObjectMapper.objectMapper.convertValue(
            map,
            DefaultObjectMapper.getTypeFactory().constructParametricType(SecurityDynamicConfiguration.class, ctype.getConfigClass())
        );
        result.ctype = ctype;
        return result;
    }

    public static void validate(SecurityDynamicConfiguration<?> sdc, int version, CType<?> ctype) throws IOException {
        if (version < 2) {
            throw new IOException("Config version " + version + " is not supported; config type: " + ctype);
        }

        if (sdc.get_meta() == null) {
            throw new IOException("A version of " + version + " must have a _meta key for " + ctype);
        }

        if (ctype == CType.CONFIG && (sdc.getCEntries().size() != 1 || !sdc.getCEntries().keySet().contains("config"))) {
            throw new IOException("A version of " + version + " must have a single toplevel key named 'config' for " + ctype);
        }

    }

    public static <T> SecurityDynamicConfiguration<T> fromNode(JsonNode json, CType<T> ctype, int version, long seqNo, long primaryTerm)
        throws IOException {
        return SecurityDynamicConfiguration.<T>fromJson(
            DefaultObjectMapper.writeValueAsString(json, false),
            ctype,
            version,
            seqNo,
            primaryTerm
        );
    }

    /**
     * For testing only
     */
    public static <T> SecurityDynamicConfiguration<T> fromYaml(String yaml, CType<T> ctype) throws JsonProcessingException {
        Class<T> implementationClass = ctype.getConfigClass();
        SecurityDynamicConfiguration<T> result = DefaultObjectMapper.YAML_MAPPER.readValue(
            yaml,
            DefaultObjectMapper.getTypeFactory().constructParametricType(SecurityDynamicConfiguration.class, implementationClass)
        );
        result.ctype = ctype;
        result.version = 2;
        return result;
    }

    // for Jackson
    private SecurityDynamicConfiguration() {
        super();
    }

    private SecurityDynamicConfiguration(CType<T> ctype) {
        super();
        this.ctype = ctype;
    }

    private Meta _meta;

    public Meta get_meta() {
        return _meta;
    }

    public void set_meta(Meta _meta) {
        this._meta = _meta;
    }

    @JsonAnySetter
    void setCEntries(String key, T value) {
        putCEntry(key, value);
    }

    @JsonAnyGetter
    public Map<String, T> getCEntries() {
        synchronized (modificationLock) {
            return new HashMap<>(centries);
        }
    }

    @JsonIgnore
    public void removeHidden() {
        synchronized (modificationLock) {
            final Iterator<Entry<String, T>> iterator = centries.entrySet().iterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                if (entry.getValue() instanceof Hideable && ((Hideable) entry.getValue()).isHidden()) {
                    iterator.remove();
                }
            }
        }
    }

    @JsonIgnore
    public void removeStatic() {
        synchronized (modificationLock) {
            final Iterator<Entry<String, T>> iterator = centries.entrySet().iterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                if (entry.getValue() instanceof StaticDefinable && ((StaticDefinable) entry.getValue()).isStatic()) {
                    iterator.remove();
                }
            }
        }
    }

    @JsonIgnore
    public void clearHashes() {
        for (Entry<String, T> entry : centries.entrySet()) {
            if (entry.getValue() instanceof Hashed) {
                ((Hashed) entry.getValue()).clearHash();
            }
        }
    }

    public void removeOthers(String key) {
        synchronized (modificationLock) {
            T tmp = this.centries.get(key);
            this.centries.clear();
            this.centries.put(key, tmp);
        }
    }

    @JsonIgnore
    public T putCEntry(String key, T value) {
        synchronized (modificationLock) {
            return centries.put(key, value);
        }
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public void putCObject(String key, Object value) {
        synchronized (modificationLock) {
            centries.put(key, (T) value);
        }
    }

    @JsonIgnore
    public T getCEntry(String key) {
        return centries.get(key);
    }

    @JsonIgnore
    public boolean exists(String key) {
        return centries.containsKey(key);
    }

    @Override
    public String toString() {
        return "SecurityDynamicConfiguration [seqNo="
            + seqNo
            + ", primaryTerm="
            + primaryTerm
            + ", ctype="
            + ctype
            + ", version="
            + version
            + ", centries="
            + centries
            + ", getImplementingClass()="
            + getImplementingClass()
            + "]";
    }

    @Override
    @JsonIgnore
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        final boolean omitDefaults = params != null && params.paramAsBoolean("omit_defaults", false);
        return builder.map(DefaultObjectMapper.readValue(DefaultObjectMapper.writeValueAsString(this, omitDefaults), typeRefMSO));
    }

    @Override
    @JsonIgnore
    public boolean isFragment() {
        return false;
    }

    @JsonIgnore
    public long getSeqNo() {
        return seqNo;
    }

    @JsonIgnore
    public long getPrimaryTerm() {
        return primaryTerm;
    }

    @JsonIgnore
    public CType<T> getCType() {
        return ctype;
    }

    @JsonIgnore
    public int getVersion() {
        return version;
    }

    @JsonIgnore
    public Class<?> getImplementingClass() {
        return getCType() == null ? null : getCType().getConfigClass();
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    public SecurityDynamicConfiguration<T> clone() {
        SecurityDynamicConfiguration<T> result = new SecurityDynamicConfiguration<T>();
        result.version = this.version;
        result.ctype = this.ctype;
        result.primaryTerm = this.primaryTerm;
        result.seqNo = this.seqNo;
        result._meta = this._meta;
        result.centries.putAll(this.centries);
        return result;
    }

    @JsonIgnore
    public SecurityDynamicConfiguration<T> deepClone() {
        try {
            return fromJson(DefaultObjectMapper.writeValueAsString(this, false), ctype, version, seqNo, primaryTerm);
        } catch (Exception e) {
            throw ExceptionsHelper.convertToOpenSearchException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    public SecurityDynamicConfiguration<T> deepCloneWithRedaction() {
        try {
            return fromJson(DefaultObjectMapper.writeValueAsStringAndRedactSensitive(this), ctype, version, seqNo, primaryTerm);
        } catch (Exception e) {
            throw ExceptionsHelper.convertToOpenSearchException(e);
        }
    }

    @JsonIgnore
    public void remove(String key) {
        synchronized (modificationLock) {
            centries.remove(key);
        }
    }

    @JsonIgnore
    public void remove(List<String> keySet) {
        synchronized (modificationLock) {
            keySet.stream().forEach(centries::remove);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean add(SecurityDynamicConfiguration other) {
        synchronized (modificationLock) {
            if (other.ctype == null || !other.ctype.equals(this.ctype)) {
                return false;
            }

            if (other.getImplementingClass() == null || !other.getImplementingClass().equals(this.getImplementingClass())) {
                return false;
            }

            if (other.version != this.version) {
                return false;
            }

            this.centries.putAll(other.centries);
            return true;
        }
    }

    @JsonIgnore
    @SuppressWarnings({ "rawtypes" })
    public boolean containsAny(SecurityDynamicConfiguration other) {
        return !Collections.disjoint(this.getCEntries().keySet(), other.getCEntries().keySet());
    }

    public boolean isHidden(String resourceName) {
        final Object o = centries.get(resourceName);
        return o instanceof Hideable && ((Hideable) o).isHidden();
    }

    @JsonIgnore
    public boolean isStatic(final String resourceName) {
        final Object o = centries.get(resourceName);
        return o instanceof StaticDefinable && ((StaticDefinable) o).isStatic();
    }

    @JsonIgnore
    public boolean isReserved(final String resourceName) {
        final Object o = centries.get(resourceName);
        return o instanceof Hideable && ((Hideable) o).isReserved();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return centries.isEmpty();
    }

    /**
     * Returns a shallow copy of this configuration which additionally contains the static configuration.
     */
    @JsonIgnore
    public SecurityDynamicConfiguration<T> withStaticConfig() {
        return DynamicConfigFactory.addStatics(this.clone());
    }
}
