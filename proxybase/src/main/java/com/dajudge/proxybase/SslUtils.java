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

package com.dajudge.proxybase;

import com.dajudge.proxybase.certs.KeyStoreManager;
import com.dajudge.proxybase.certs.KeyStoreWrapper;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.*;
import java.security.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

class SslUtils {
    static X509TrustManager[] createTrustManagers(final KeyStoreManager trustStoreManager) {
        return createTrustManagers(Optional.of(trustStoreManager));
    }

    static X509TrustManager[] createTrustManagers(final Optional<? extends KeyStoreManager> trustStoreManager) {
        try {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
            if (!trustStoreManager.isPresent()) {
                factory.init((KeyStore) null);
                return stream(factory.getTrustManagers())
                        .map(it -> (X509TrustManager) it)
                        .toArray(X509TrustManager[]::new);
            }
            factory.init(trustStoreManager.get().getKeyStore().getKeyStore());
            return stream(factory.getTrustManagers())
                    .filter(it -> it instanceof X509TrustManager)
                    .map(it -> (X509TrustManager) it)
                    .toArray(X509TrustManager[]::new);
        } catch (final KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to setup trust manager", e);
        }
    }

    static X509KeyManager[] createKeyManagers(final KeyStoreManager keyStoreManager) {
        return createKeyManagers(Optional.of(keyStoreManager));
    }

    static X509KeyManager[] createKeyManagers(final Optional<? extends KeyStoreManager> keyStoreManager) {
        if (!keyStoreManager.isPresent()) {
            return new X509KeyManager[]{};
        }
        return stream(createKeyManagerFactory(keyStoreManager).getKeyManagers())
                .filter(it -> it instanceof X509KeyManager)
                .map(it -> (X509KeyManager) it)
                .toArray(X509KeyManager[]::new);
    }

    static KeyManagerFactory createKeyManagerFactory(final Optional<? extends KeyStoreManager> keyStoreManager) {
        try {
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (keyStoreManager.isPresent()) {
                final KeyStoreManager keyStoreWrapper = keyStoreManager.get();
                final KeyStoreWrapper keyStore = keyStoreWrapper.getKeyStore();
                factory.init(keyStore.getKeyStore(), keyStore.getKeyPassword());
            } else {
                factory.init(null, null);
            }
            return factory;
        } catch (final UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to setup key manager", e);
        }
    }

    static SslContext createClientSslContext(
            final HostnameCheck hostnameCheck,
            final Optional<? extends KeyStoreManager> trustStoreManager,
            final Optional<? extends KeyStoreManager> keyStoreManager
    ) throws NoSuchAlgorithmException, KeyManagementException, SSLException {
        final SSLContext clientContext = SSLContext.getInstance("TLS");
        final HostCheckingTrustManager trustManager = new HostCheckingTrustManager(
                createTrustManagers(trustStoreManager),
                hostnameCheck
        );
        final X509TrustManager[] trustManagers = {
                trustManager
        };
        final X509KeyManager[] keyManagers = createKeyManagers(keyStoreManager);
        clientContext.init(keyManagers, trustManagers, null);
        return SslContextBuilder.forClient()
                .keyManager(createKeyManagerFactory(keyStoreManager))
                .trustManager(trustManager)
                .build();
    }
}
