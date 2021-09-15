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

import com.dajudge.proxybase.certs.KeyStoreConfig;

import java.util.Optional;

public class DownstreamSslConfig {
    private final Optional<KeyStoreConfig> trustStore;
    private final Optional<KeyStoreConfig> keyStore;
    private final boolean hostnameVerificationEnabled;

    public DownstreamSslConfig(
            final Optional<KeyStoreConfig> trustStore,
            final Optional<KeyStoreConfig> keyStore,
            final boolean hostnameVerificationEnabled
    ) {
        this.trustStore = trustStore;
        this.keyStore = keyStore;
        this.hostnameVerificationEnabled = hostnameVerificationEnabled;
    }

    public Optional<KeyStoreConfig> getTrustStore() {
        return trustStore;
    }

    public boolean isHostnameVerificationEnabled() {
        return hostnameVerificationEnabled;
    }

    public Optional<KeyStoreConfig> getKeyStore() {
        return keyStore;
    }
}
