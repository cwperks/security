/*
 * Copyright 2017 floragunn GmbH
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

package org.opensearch.security.ssl;

import org.opensearch.rest.RestRequest;
import org.opensearch.security.filter.SecurityRequestChannel;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportRequest;

public interface SslExceptionHandler {

    default void logError(Throwable t, RestRequest request, int type) {
        // no-op
    }

    default void logError(Throwable t, boolean isRest) {
        // no-op
    }

    default void logError(Throwable t, final TransportRequest request, String action, Task task, int type) {
        // no-op
    }

    default void logError(Throwable t, SecurityRequestChannel request, int type) {
        this.logError(t, request.asRestRequest().get(), type);
    }
}
