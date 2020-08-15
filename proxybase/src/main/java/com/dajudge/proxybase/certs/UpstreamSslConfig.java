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

package com.dajudge.proxybase.certs;

public class UpstreamSslConfig {
    private final String trustStore;
    private final char[] trustStorePassword;
    private final String trustStorePasswordFile;
    private final String trustStoreType;
    private final long trustStoreUpdateIntervalMsecs;
    private final String keyStore;
    private final char[] keyStorePassword;
    private final String keyStorePasswordFile;
    private final char[] keyPassword;
    private final String keyPasswordFile;
    private final long keyStoreUpdateIntervalMsecs;
    private final String keyStoreType;
    private final boolean clientAuthRequired;

    public UpstreamSslConfig(
            final String trustStore,
            final char[] trustStorePassword,
            final String trustStorePasswordFile,
            final String trustStoreType,
            final long trustStoreUpdateIntervalMsecs,
            final String keyStore,
            final char[] keyStorePassword,
            final String keyStorePasswordFile,
            final char[] keyPassword,
            final String keyPasswordFile,
            final long keyStoreUpdateIntervalMsecs,
            final String keyStoreType,
            final boolean clientAuthRequired
    ) {
        this.trustStore = trustStore;
        this.trustStorePassword = trustStorePassword;
        this.trustStorePasswordFile = trustStorePasswordFile;
        this.trustStoreType = trustStoreType;
        this.trustStoreUpdateIntervalMsecs = trustStoreUpdateIntervalMsecs;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyStorePasswordFile = keyStorePasswordFile;
        this.keyPassword = keyPassword;
        this.keyPasswordFile = keyPasswordFile;
        this.keyStoreUpdateIntervalMsecs = keyStoreUpdateIntervalMsecs;
        this.keyStoreType = keyStoreType;
        this.clientAuthRequired = clientAuthRequired;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getKeyStore() {
        return keyStore;
    }

    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    public char[] getKeyPassword() {
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

    public String getTrustStorePasswordFile() {
        return trustStorePasswordFile;
    }

    public String getKeyStorePasswordFile() {
        return keyStorePasswordFile;
    }

    public String getKeyPasswordFile() {
        return keyPasswordFile;
    }

    public long getKeyStoreUpdateIntervalMsecs() {
        return keyStoreUpdateIntervalMsecs;
    }

    public long getTrustStoreUpdateIntervalMsecs() {
        return trustStoreUpdateIntervalMsecs;
    }
}
