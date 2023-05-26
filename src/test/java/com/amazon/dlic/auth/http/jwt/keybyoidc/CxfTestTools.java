/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package com.amazon.dlic.auth.http.jwt.keybyoidc;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;

class CxfTestTools {

    static String toJson(JsonMapObject jsonMapObject) {
        return new JsonMapObjectReaderWriter().toJson(jsonMapObject);
    }
}
