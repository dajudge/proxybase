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

import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.certs.KeyStoreManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;

class SslUtils {
    static TrustManager[] createTrustManagers(final KeyStoreManager trustStoreManager) {
        try {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(getDefaultAlgorithm());
            factory.init(trustStoreManager.getKeyStore().getKeyStore());
            return factory.getTrustManagers();
        } catch (final KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to setup trust manager", e);
        }
    }

    static KeyManager[] createKeyManagers(final KeyStoreManager keyStoreManager) {
        try {
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            final KeyStoreWrapper keyStore = keyStoreManager.getKeyStore();
            factory.init(keyStore.getKeyStore(), keyStore.getKeyPassword());
            return factory.getKeyManagers();
        } catch (final UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to setup key manager", e);
        }
    }
}
