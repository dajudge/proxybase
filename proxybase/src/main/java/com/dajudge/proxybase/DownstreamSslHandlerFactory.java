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
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Stream;

import static com.dajudge.proxybase.SslUtils.createKeyManagers;
import static com.dajudge.proxybase.SslUtils.createTrustManagers;
import static java.util.stream.Collectors.toList;

public class DownstreamSslHandlerFactory {
    public static ChannelHandler createDownstreamSslHandler(
            final HostnameCheck hostnameCheck,
            final KeyStoreManager trustStoreManager,
            final KeyStoreManager keyStoreManager
    ) {
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final X509TrustManager[] trustManagers = {
                    new HostCheckingTrustManager(createDefaultTrustManagers(trustStoreManager), hostnameCheck)
            };
            clientContext.init(createKeyManagers(keyStoreManager), trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine();
            engine.setUseClientMode(true);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize downstream SSL handler", e);
        }
    }

    private static List<X509TrustManager> createDefaultTrustManagers(final KeyStoreManager trustStoreManager) {
        return Stream.of((createTrustManagers(trustStoreManager)))
                .map(it -> (X509TrustManager) it).collect(toList());
    }
}