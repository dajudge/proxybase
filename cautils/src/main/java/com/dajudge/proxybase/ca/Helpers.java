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

package com.dajudge.proxybase.ca;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.function.Supplier;

public final class Helpers {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private Helpers() {
    }

    private static <T> T call(final ThrowingCallable<T> callable) {
        try {
            return callable.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static X509Certificate selfSignedCert(
            final String dn,
            final KeyPair pair,
            final Date notBefore,
            final Date notAfter,
            final String algorithm,
            final boolean isTrustAnchor
    ) {
        return sign(dn, dn, pair.getPrivate(), algorithm, pair.getPublic(), notBefore, notAfter, isTrustAnchor);
    }

    public static X509Certificate sign(
            final String ownerDn,
            final String issuerDn,
            final PrivateKey signingKey,
            final String algorithm,
            final PublicKey publicKey,
            final Date notBefore,
            final Date notAfter,
            final boolean isTrustAnchor
    ) {
        try {
            final AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder()
                    .find(algorithm);
            final AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder()
                    .find(sigAlgId);

            final X509v3CertificateBuilder certGenerator = new X509v3CertificateBuilder(
                    new X500Name(issuerDn),
                    BigInteger.valueOf(SECURE_RANDOM.nextInt()),
                    notBefore,
                    notAfter,
                    new X500Name(ownerDn),
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
            );
            if (isTrustAnchor) {
                certGenerator.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), false, new BasicConstraints(true));
            }
            final ContentSigner sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                    .build(PrivateKeyFactory.createKey(signingKey.getEncoded()));

            final X509CertificateHolder holder = certGenerator.build(sigGen);
            final Certificate eeX509CertificateStructure = holder.toASN1Structure();
            final CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");

            try (final InputStream stream = new ByteArrayInputStream(eeX509CertificateStructure.getEncoded())) {
                return (X509Certificate) cf.generateCertificate(stream);
            }
        } catch (final CertificateException | IOException | OperatorCreationException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to sign certificate", e);
        }
    }

    public static KeyPair keyPair() {
        return call(() -> {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.generateKeyPair();
        });
    }

    public static byte[] serialize(final KeyStore keyStore, final char[] password) {
        return call(() -> {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            keyStore.store(bos, password);
            return bos.toByteArray();
        });
    }

    public static KeyStore createKeyStore(
            final ThrowingConsumer<KeyStore> withKeyStore,
            final String type
    ) {
        return call(() -> {
            final KeyStore keystore = KeyStore.getInstance(type);
            keystore.load(null, null);
            withKeyStore.accept(keystore);
            return keystore;
        });
    }

    public static Date now(final Supplier<Long> clock) {
        return new Date(clock.get());
    }

    public static Date plus(final Date now, final TemporalAmount amount) {
        return new Date(now.toInstant().plus(amount).toEpochMilli());
    }

    public interface ThrowingCallable<T> {
        T call() throws Exception;
    }

    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}
