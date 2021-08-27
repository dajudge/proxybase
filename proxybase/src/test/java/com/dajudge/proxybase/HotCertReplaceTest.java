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

import com.dajudge.proxybase.certs.KeyStoreWrapper;
import com.dajudge.proxybase.util.TestKeyStoreManager;
import com.dajudge.proxybase.util.TestSslConfiguration.SslConfiguration;
import com.dajudge.proxybase.util.TestSslConfiguration.TwoWaySslConfiguration;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

import static com.dajudge.proxybase.util.DownstreamSocketAssertions.hasCertFor;
import static com.dajudge.proxybase.util.DownstreamSocketAssertions.isSsl;
import static java.util.Arrays.asList;

public class HotCertReplaceTest extends BaseProxyTest {
    private final TestKeyStoreManager downstreamClientKeyStore = new TestKeyStoreManager(
            DOWNSTREAM_CLIENT_CA,
            "cn=downstreamClient",
            KEYSTORE_TYPE
    );
    private final SslConfiguration downstreamMtls = new TwoWaySslConfiguration(
            downstreamClientKeyStore,
            () -> new KeyStoreWrapper(DOWNSTREAM_SERVER_CA.getTrustStore(KEYSTORE_TYPE), null),
            new TestKeyStoreManager(DOWNSTREAM_SERVER_CA, "cn=downstreamServer", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(DOWNSTREAM_CLIENT_CA.getTrustStore(KEYSTORE_TYPE), null)
    );
    private final SslConfiguration upstreamMtls = new TwoWaySslConfiguration(
            new TestKeyStoreManager(UPSTREAM_CLIENT_CA, "cn=upstreamClient", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(UPSTREAM_SERVER_CA.getTrustStore(KEYSTORE_TYPE), null),
            new TestKeyStoreManager(UPSTREAM_SERVER_CA, "cn=upstreamServer", KEYSTORE_TYPE),
            () -> new KeyStoreWrapper(UPSTREAM_CLIENT_CA.getTrustStore(KEYSTORE_TYPE), null)
    );

    @Test
    public void can_replace_downstream_cert() {
        class Container {
            String value;

            String getValue() {
                return value;
            }
        }
        final Container dnContainer = new Container();
        final List<Consumer<Socket>> downstreamSocketAssertions = asList(isSsl(), hasCertFor(dnContainer::getValue));
        withProxy(upstreamMtls, downstreamMtls, downstreamSocketAssertions, proxyPort -> {
            dnContainer.value = "CN=lolcats1";
            downstreamClientKeyStore.replaceDn(dnContainer.value);
            assertRoundtripWorks(new InetSocketAddress("127.0.0.1", proxyPort), upstreamMtls::clientSocket);

            dnContainer.value = "CN=lolcats2";
            downstreamClientKeyStore.replaceDn(dnContainer.value);
            assertRoundtripWorks(new InetSocketAddress("127.0.0.1", proxyPort), upstreamMtls::clientSocket);
        });
    }
}
