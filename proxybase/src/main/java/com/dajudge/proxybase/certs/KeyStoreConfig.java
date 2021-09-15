/*
 * Copyright 2019-2021 Alex Stockinger
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

public class KeyStoreConfig {
    private final String keyStorePath;
    private final char[] keyStorePassword;
    private final String keyStorePasswordPath;
    private final char[] keyPassword;
    private final String keyPasswordPath;
    private final String keyStoreType;
    private final long updateIntervalMsecs;

    public KeyStoreConfig(
            final String keyStorePath,
            final char[] keyStorePassword,
            final String keyStorePasswordPath,
            final char[] keyPassword,
            final String keyPasswordPath,
            final String keyStoreType,
            final long updateIntervalMsecs
    ) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.keyStorePasswordPath = keyStorePasswordPath;
        this.keyPassword = keyPassword;
        this.keyPasswordPath = keyPasswordPath;
        this.keyStoreType = keyStoreType;
        this.updateIntervalMsecs = updateIntervalMsecs;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public char[] getKeyStorePassword() {
        return keyStorePassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public String getKeyStorePasswordPath() {
        return keyStorePasswordPath;
    }

    public long getUpdateIntervalMsecs() {
        return updateIntervalMsecs;
    }

    public char[] getKeyPassword() {
        return keyPassword;
    }

    public String getKeyPasswordPath() {
        return keyPasswordPath;
    }
}
