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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;
import java.util.Optional;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

class SslUtils {
    static X509TrustManager[] createTrustManagers(final KeyStoreManager trustStoreManager) {
        return createTrustManagers(Optional.of(trustStoreManager));
    }

    static X509TrustManager[] createTrustManagers(final Optional<KeyStoreManager> trustStoreManager) {
        try {
            if (!trustStoreManager.isPresent()) {
                return new X509TrustManager[]{};
            }
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
            factory.init(trustStoreManager.get().getKeyStore().getKeyStore());
            return Arrays.stream(factory.getTrustManagers())
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

    static X509KeyManager[] createKeyManagers(final Optional<KeyStoreManager> keyStoreManager) {
        if (!keyStoreManager.isPresent()) {
            return new X509KeyManager[]{};
        }
        return Arrays.stream(createKeyManagerFactory(keyStoreManager).getKeyManagers())
                .filter(it -> it instanceof X509KeyManager)
                .map(it -> (X509KeyManager) it)
                .toArray(X509KeyManager[]::new);
    }

    public static KeyManagerFactory createKeyManagerFactory(final Optional<KeyStoreManager> keyStoreManager) {
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
}
