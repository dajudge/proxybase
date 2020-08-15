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

package com.dajudge.proxybase.ca;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

public class ClientCertCertificateAuthority implements CertificateAuthority {
    private final KeyStoreWrapper wrapper;

    public ClientCertCertificateAuthority(final ClientCertificateConfig sslConfig) {
        try (final InputStream is = sslConfig.getKeyStore().get()) {
            final KeyStore keyStore = KeyStore.getInstance(sslConfig.getType());
            keyStore.load(is, sslConfig.getKeyStorePassword());
            wrapper = new KeyStoreWrapper(keyStore, sslConfig.getKeyPassword());
        } catch (final IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to load client key store", e);
        }
    }

    @Override
    public KeyStoreWrapper createClientCertificate(
            final UpstreamCertificateSupplier certificateSupplier
    ) {
        return wrapper;
    }

    public static class ClientCertificateConfig {
        private final Supplier<InputStream> keyStore;
        private final char[] keyStorePassword;
        private final char[] keyPassword;
        private final String type;

        public static final ClientCertificateConfig DISABLED = new ClientCertificateConfig(
                null,
                null,
                null,
                null
        );

        public ClientCertificateConfig(
                final Supplier<InputStream> keyStore,
                final char[] keyStorePassword,
                final char[] keyPassword,
                final String type
        ) {
            this.keyStore = keyStore;
            this.keyStorePassword = keyStorePassword;
            this.keyPassword = keyPassword;
            this.type = type;
        }

        public Supplier<InputStream> getKeyStore() {
            return keyStore;
        }

        public char[] getKeyStorePassword() {
            return keyStorePassword;
        }

        public char[] getKeyPassword() {
            return keyPassword;
        }

        public String getType() {
            return type;
        }
    }
}
