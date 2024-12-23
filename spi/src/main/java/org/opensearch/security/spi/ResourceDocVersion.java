/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.spi;

import java.io.IOException;
import java.util.Locale;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.common.io.stream.Writeable;

// TODO Job Scheduler keeps track of doc version. Should this keep track of version similarly?

/**
 * Structure to represent resource document version.
 */
public class ResourceDocVersion implements Comparable<ResourceDocVersion>, Writeable {
    private final long primaryTerm;
    private final long seqNo;
    private final long version;

    public ResourceDocVersion(long primaryTerm, long seqNo, long version) {
        this.primaryTerm = primaryTerm;
        this.seqNo = seqNo;
        this.version = version;
    }

    public ResourceDocVersion(StreamInput in) throws IOException {
        this.primaryTerm = in.readLong();
        this.seqNo = in.readLong();
        this.version = in.readLong();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeLong(this.primaryTerm);
        out.writeLong(this.seqNo);
        out.writeLong(this.version);
    }

    public long getPrimaryTerm() {
        return primaryTerm;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public long getVersion() {
        return version;
    }

    /**
     * Compare two doc versions. Refer to https://github.com/elastic/elasticsearch/issues/10708
     *
     * @param v the doc version to compare.
     * @return -1 if this &lt; v, 0 if this == v, otherwise 1;
     */
    @Override
    public int compareTo(ResourceDocVersion v) {
        if (v == null) {
            return 1;
        }
        if (this.seqNo < v.seqNo) {
            return -1;
        }
        if (this.seqNo > v.seqNo) {
            return 1;
        }
        if (this.primaryTerm < v.primaryTerm) {
            return -1;
        }
        if (this.primaryTerm > v.primaryTerm) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format(
            Locale.getDefault(),
            "{_version: %s, _primary_term: %s, _seq_no: %s}",
            this.version,
            this.primaryTerm,
            this.seqNo
        );
    }
}
