/*
 * Copyright 2019-2020 Alex Stockinger
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

package com.dajudge.proxybase.config;

import java.io.InputStream;
import java.util.function.Supplier;

public class UpstreamSslConfig {
    public static final UpstreamSslConfig NO_SSL = new UpstreamSslConfig(
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false
    );

    private final boolean enabled;
    private final Supplier<InputStream> trustStore;
    private final String trustStorePassword;
    private final Supplier<InputStream> keyStore;
    private final String keyStorePassword;
    private final String keyPassword;
    private final boolean clientAuthRequired;
    private final String keyStoreType;
    private final String trustStoreType;

    public UpstreamSslConfig(
            final boolean enabled,
            final Supplier<InputStream> trustStore,
            final String trustStorePassword,
            final String trustStoreType,
            final Supplier<InputStream> keyStore,
            final String keyStorePassword,
            final String keyPassword,
            final String keyStoreType,
            final boolean clientAuthRequired
    ) {
        this.enabled = enabled;
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.trustStoreType = trustStoreType;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.keyStoreType = keyStoreType;
        this.clientAuthRequired = clientAuthRequired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Supplier<InputStream> getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public Supplier<InputStream> getKeyStore() {
        return keyStore;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public boolean isClientAuthRequired() {
        return clientAuthRequired;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }
}
