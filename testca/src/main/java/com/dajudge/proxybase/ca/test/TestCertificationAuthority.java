package com.dajudge.proxybase.ca.test;

import com.dajudge.proxybase.ca.Helpers;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
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

    public TestCertificationAuthority(final Supplier<Long> clock, final String dn) {
        super(createCaKeyStore(dn, clock), KEY_ALIAS);
        this.clock = clock;
    }

    private static KeyStoreManager createCaKeyStore(final String dn, final Supplier<Long> clock) {
        final String keyPassword = UUID.randomUUID().toString();
        final KeyStoreWrapper wrapper = new KeyStoreWrapper(
                Helpers.createJks(keyStore -> {
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
                }, "jks"),
                keyPassword.toCharArray()
        );
        return () -> wrapper;
    }

    public KeyStoreWrapper createNewKeyStore(final String dn) {
        final String keyPassword = UUID.randomUUID().toString();
        return new KeyStoreWrapper(
                createKeyStore(
                        dn,
                        "SHA256withRSA",
                        keyPassword.toCharArray(),
                        Helpers.now(clock),
                        Helpers.plus(Helpers.now(clock), Duration.ofDays(1)),
                        "jks"
                ),
                keyPassword.toCharArray()
        );
    }
}
