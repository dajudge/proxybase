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

package com.dajudge.proxybase.ca.test;

import com.dajudge.proxybase.ca.Helpers;
import com.dajudge.proxybase.certs.KeyStoreWrapper;
import com.dajudge.proxybase.ca.selfsign.CertificateAuthority;
import com.dajudge.proxybase.certs.KeyStoreManager;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

public class TestCertificationAuthority extends CertificateAuthority {
    private static final String KEY_ALIAS = UUID.randomUUID().toString();
    private final Supplier<Long> clock;
    private String keystoreType;

    public TestCertificationAuthority(final Supplier<Long> clock, final String dn, final String keystoreType) {
        super(createCaKeyStore(dn, clock, keystoreType), KEY_ALIAS);
        this.clock = clock;
        this.keystoreType = keystoreType;
    }

    private static KeyStoreManager createCaKeyStore(
            final String dn,
            final Supplier<Long> clock,
            final String keystoreType
    ) {
        final String keyPassword = UUID.randomUUID().toString();
        final KeyStoreWrapper wrapper = new KeyStoreWrapper(
                Helpers.createKeyStore(keyStore -> {
                    final KeyPair keyPair = Helpers.keyPair();
                    final X509Certificate cert = Helpers.selfSignedCert(
                            dn,
                            keyPair,
                            Helpers.now(clock),
                            Helpers.plus(Helpers.now(clock), Duration.ofDays(1)),
                            "SHA256withRSA",
                            true
                    );
                    keyStore.setKeyEntry(
                            KEY_ALIAS,
                            keyPair.getPrivate(),
                            keyPassword.toCharArray(),
                            new Certificate[]{cert}
                    );
                }, keystoreType),
                keyPassword.toCharArray()
        );
        return () -> wrapper;
    }

    public KeyStoreWrapper createNewKeyStore(final String dn, final String keyStoreType) {
        final String keyPassword = UUID.randomUUID().toString();
        return new KeyStoreWrapper(
                createKeyStore(
                        dn,
                        "SHA256withRSA",
                        keyPassword.toCharArray(),
                        Helpers.now(clock),
                        Helpers.plus(Helpers.now(clock), Duration.ofDays(1)),
                        keyStoreType
                ),
                keyPassword.toCharArray()
        );
    }
}
