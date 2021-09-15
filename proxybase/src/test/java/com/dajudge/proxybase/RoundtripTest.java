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

package com.dajudge.proxybase;

import com.dajudge.proxybase.certs.KeyStoreWrapper;
import com.dajudge.proxybase.util.TestKeyStoreManager;
import com.dajudge.proxybase.util.TestSslConfiguration;
import com.dajudge.proxybase.util.TestSslConfiguration.OneWaySslConfiguration;
import com.dajudge.proxybase.util.TestSslConfiguration.PlaintextSslConfiguration;
import com.dajudge.proxybase.util.TestSslConfiguration.SslConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static com.dajudge.proxybase.util.DownstreamSocketAssertions.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@RunWith(Parameterized.class)
public class RoundtripTest extends BaseProxyTest {

    private static final SslConfiguration PLAINTEXT = new PlaintextSslConfiguration();
    protected static final String KEYSTORE_TYPE = "jks";
    private static final SslConfiguration DOWNSTREAM_TLS = new OneWaySslConfiguration(
            () -> new KeyStoreWrapper(DOWNSTREAM_SERVER_CA.getTrustStore(KEYSTORE_TYPE), null),
            new TestKeyStoreManager(DOWNSTREAM_SERVER_CA, "cn=downstreamServer", KEYSTORE_TYPE)
    );
    private static final SslConfiguration DOWNSTREAM_MTLS = new TestSslConfiguration.TwoWaySslConfiguration(
            new TestKeyStoreManager(DOWNSTREAM_CLIENT_CA, "cn=downstreamClient", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(DOWNSTREAM_SERVER_CA.getTrustStore(KEYSTORE_TYPE), null),
            new TestKeyStoreManager(DOWNSTREAM_SERVER_CA, "cn=downstreamServer", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(DOWNSTREAM_CLIENT_CA.getTrustStore(KEYSTORE_TYPE), null)
    );
    private static final SslConfiguration UPSTREAM_TLS = new OneWaySslConfiguration(
            () -> new KeyStoreWrapper(UPSTREAM_SERVER_CA.getTrustStore(KEYSTORE_TYPE), null),
            new TestKeyStoreManager(UPSTREAM_SERVER_CA, "cn=upstreamServer", KEYSTORE_TYPE)
    );
    private static final SslConfiguration UPSTREAM_MTLS = new TestSslConfiguration.TwoWaySslConfiguration(
            new TestKeyStoreManager(UPSTREAM_CLIENT_CA, "cn=upstreamClient", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(UPSTREAM_SERVER_CA.getTrustStore(KEYSTORE_TYPE), null),
            new TestKeyStoreManager(UPSTREAM_SERVER_CA, "cn=upstreamServer", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(UPSTREAM_CLIENT_CA.getTrustStore(KEYSTORE_TYPE), null)
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
