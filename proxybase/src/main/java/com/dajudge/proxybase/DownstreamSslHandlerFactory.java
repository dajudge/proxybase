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
import com.dajudge.proxybase.config.Endpoint;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;
import java.util.stream.Stream;

import static com.dajudge.proxybase.SslUtils.createTrustManagers;
import static java.util.stream.Collectors.toList;

public class DownstreamSslHandlerFactory {
    public static ChannelHandler createDownstreamSslHandler(
            final Endpoint endpoint,
            final boolean hostnameVerification,
            final KeyStoreManager trustStoreManager,
            final KeyStoreManager keyStoreManager
    ) {
        try {
            final KeyStoreWrapper keyStore = keyStoreManager.getKeyStore();
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final HostnameCheck hostnameCheck = hostnameVerification
                    ? new HttpClientHostnameCheck(endpoint.getHost())
                    : HostnameCheck.NULL_VERIFIER;
            final TrustManager[] trustManagers = {
                    new HostCheckingTrustManager(createDefaultTrustManagers(trustStoreManager), hostnameCheck)
            };
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm()
            );
            if (keyStore != null) {
                keyManagerFactory.init(
                        keyStore.getKeyStore(),
                        keyStore.getKeyPassword()
                );
            }
            final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            clientContext.init(keyManagers, trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine(endpoint.getHost(), endpoint.getPort());
            engine.setUseClientMode(true);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException
                | KeyManagementException
                | KeyStoreException
                | UnrecoverableKeyException e
        ) {
            throw new RuntimeException("Failed to initialize downstream SSL handler", e);
        }
    }

    private static List<X509TrustManager> createDefaultTrustManagers(final KeyStoreManager trustStoreManager) {
        return Stream.of((createTrustManagers(trustStoreManager)))
                .map(it -> (X509TrustManager) it).collect(toList());
    }
}
