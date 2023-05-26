/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.security.test.helper.cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opensearch.common.transport.TransportAddress;

public class ClusterInfo {
    public int numNodes;
    public String httpHost = null;
    public int httpPort = -1;
    public Set<TransportAddress> httpAdresses = new HashSet<TransportAddress>();
    public String nodeHost;
    public int nodePort;
    public String clustername;
    public List<String> tcpClusterManagerPortsOnly;
}
