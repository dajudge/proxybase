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

package com.dajudge.proxybase.ca.selfsign;

import com.dajudge.proxybase.ca.Helpers;
import com.dajudge.proxybase.certs.KeyStoreManager;
import com.dajudge.proxybase.certs.KeyStoreWrapper;

import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertificateAuthority {
    private final KeyStoreManager keyStoreManager;
    private final String keyAlias;

    public CertificateAuthority(
            final KeyStoreManager keyStoreManager,
            final String keyAlias
    ) {
        this.keyStoreManager = keyStoreManager;
        this.keyAlias = keyAlias;
    }


    private PrivateKey caPrivateKey() {
        final KeyStoreWrapper wrapper = keyStoreManager.getKeyStore();
        final KeyStore keyStore = wrapper.getKeyStore();
        final char[] password = wrapper.getKeyPassword();
        return loadKey(keyStore, keyAlias, password);
    }

    public KeyStore getTrustStore(final String type) {
        return Helpers.createKeyStore(keyStore -> {
            keyStore.setCertificateEntry("ca", caCert());
        }, type);
    }

    public KeyStore createKeyStore(
            final String certificateDn,
            final String algorithm,
            final char[] keyPassword,
            final Date notBefore,
            final Date notAfter,
            final String type
    ) {
        try {
            final KeyStore keystore = KeyStore.getInstance(type);
            keystore.load(null, null);
            final KeyPair keyPair = Helpers.keyPair();
            final X509Certificate cert = Helpers.sign(
                    certificateDn,
                    caCert().getSubjectDN().getName(),
                    caPrivateKey(),
                    algorithm,
                    keyPair.getPublic(),
                    notBefore,
                    notAfter,
                    false
            );
            keystore.setKeyEntry("key", keyPair.getPrivate(), keyPassword, new Certificate[]{cert});
            return keystore;
        } catch (final KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            throw new RuntimeException("Failed to create certificate", e);
        }
    }

    private X509Certificate caCert() {
        try {
            return (X509Certificate) keyStoreManager.getKeyStore().getKeyStore().getCertificate(keyAlias);
        } catch (final KeyStoreException e) {
            throw new IllegalArgumentException("Failed to recover certificate from keystore", e);
        }
    }

    private static PrivateKey loadKey(final KeyStore keyStore, final String alias, final char[] keyPassword) {
        try {
            return (PrivateKey) keyStore.getKey(alias, keyPassword);
        } catch (final UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to recover key from keystore", e);
        }
    }
}
