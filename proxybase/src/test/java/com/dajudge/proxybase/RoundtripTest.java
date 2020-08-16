package com.dajudge.proxybase;

import com.dajudge.proxybase.TestSslConfiguration.OneWaySslConfiguration;
import com.dajudge.proxybase.TestSslConfiguration.PlaintextSslConfiguration;
import com.dajudge.proxybase.TestSslConfiguration.SslConfiguration;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.ca.test.TestCertificationAuthority;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static com.dajudge.proxybase.DownstreamSocketAssertions.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@RunWith(Parameterized.class)
public class RoundtripTest extends BaseProxyTest {

    private static final TestCertificationAuthority DOWNSTREAM_SERVER_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamServerCA");
    private static final TestCertificationAuthority DOWNSTREAM_CLIENT_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamClientCA");
    private static final TestCertificationAuthority UPSTREAM_SERVER_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamServerCA");
    private static final TestCertificationAuthority UPSTREAM_CLIENT_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamClientCA");
    private static final KeyStoreWrapper DOWNSTREAM_SERVER_KEYSTORE =
            DOWNSTREAM_SERVER_CA.createNewKeyStore("cn=downstreamServer");
    private static final KeyStoreWrapper DOWNSTREAM_CLIENT_KEYSTORE =
            DOWNSTREAM_CLIENT_CA.createNewKeyStore("cn=downstreamClient");
    private static final KeyStoreWrapper UPSTREAM_SERVER_KEYSTORE =
            UPSTREAM_SERVER_CA.createNewKeyStore("cn=upstreamServer");
    private static final KeyStoreWrapper UPSTREAM_CLIENT_KEYSTORE =
            UPSTREAM_CLIENT_CA.createNewKeyStore("cn=upstreamClient");

    private static final SslConfiguration PLAINTEXT = new PlaintextSslConfiguration();
    private static final SslConfiguration DOWNSTREAM_TLS = new OneWaySslConfiguration(
            () -> new KeyStoreWrapper(DOWNSTREAM_SERVER_CA.getTrustStore("jks"), null),
            () -> DOWNSTREAM_SERVER_KEYSTORE
    );
    private static final SslConfiguration DOWNSTREAM_MTLS = new TestSslConfiguration.TwoWaySslConfiguration(
            () -> DOWNSTREAM_CLIENT_KEYSTORE,
            () -> new KeyStoreWrapper(DOWNSTREAM_SERVER_CA.getTrustStore("jks"), null),
            () -> DOWNSTREAM_SERVER_KEYSTORE,
            () -> new KeyStoreWrapper(DOWNSTREAM_CLIENT_CA.getTrustStore("jks"), null)
    );
    private static final SslConfiguration UPSTREAM_TLS = new OneWaySslConfiguration(
            () -> new KeyStoreWrapper(UPSTREAM_SERVER_CA.getTrustStore("jks"), null),
            () -> UPSTREAM_SERVER_KEYSTORE
    );
    private static final SslConfiguration UPSTREAM_MTLS = new TestSslConfiguration.TwoWaySslConfiguration(
            () -> UPSTREAM_CLIENT_KEYSTORE,
            () -> new KeyStoreWrapper(UPSTREAM_SERVER_CA.getTrustStore("jks"), null),
            () -> UPSTREAM_SERVER_KEYSTORE,
            () -> new KeyStoreWrapper(UPSTREAM_CLIENT_CA.getTrustStore("jks"), null)
    );

    @Parameterized.Parameters
    public static Collection<TestSslConfiguration> data() {
        return asList(
                new TestSslConfiguration(
                        PLAINTEXT,
                        PLAINTEXT,
                        singletonList(isNotSsl())
                ),
                new TestSslConfiguration(
                        PLAINTEXT,
                        DOWNSTREAM_TLS,
                        asList(isSsl(), hasNoCert())
                ),
                new TestSslConfiguration(
                        PLAINTEXT,
                        DOWNSTREAM_MTLS,
                        asList(isSsl(), hasCertFor("CN=downstreamClient"))
                ),
                new TestSslConfiguration(
                        UPSTREAM_TLS,
                        PLAINTEXT,
                        singletonList(isNotSsl())
                ),
                new TestSslConfiguration(
                        UPSTREAM_TLS,
                        DOWNSTREAM_TLS,
                        asList(isSsl(), hasNoCert())
                ),
                new TestSslConfiguration(
                        UPSTREAM_TLS,
                        DOWNSTREAM_MTLS,
                        asList(isSsl(), hasCertFor("CN=downstreamClient"))
                ),
                new TestSslConfiguration(
                        UPSTREAM_MTLS,
                        PLAINTEXT,
                        singletonList(isNotSsl())
                ),
                new TestSslConfiguration(
                        UPSTREAM_MTLS,
                        DOWNSTREAM_TLS,
                        asList(isSsl(), hasNoCert())
                ),
                new TestSslConfiguration(
                        UPSTREAM_MTLS,
                        DOWNSTREAM_MTLS,
                        asList(isSsl(), hasCertFor("CN=downstreamClient"))
                )
        );
    }

    @Parameterized.Parameter
    public TestSslConfiguration config;

    @Test
    public void works_without_proxy() {
        assertRoundtripWorksWithoutProxy(
                config.getDownstream(),
                config.getDownstreamSocketAssertions()
        );
    }

    @Test
    public void works_with_proxy() {
        assertRoundtripWorksWithProxy(
                config.getUpstream(),
                config.getDownstream(),
                config.getDownstreamSocketAssertions()
        );
    }
}
