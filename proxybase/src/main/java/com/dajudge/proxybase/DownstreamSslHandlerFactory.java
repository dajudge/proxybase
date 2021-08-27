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

import com.dajudge.proxybase.certs.Filesystem;
import com.dajudge.proxybase.certs.KeyStoreManager;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.dajudge.proxybase.HostnameCheck.NULL_VERIFIER;
import static com.dajudge.proxybase.SslUtils.*;
import static com.dajudge.proxybase.certs.ReloadingKeyStoreManager.createReloader;

public class DownstreamSslHandlerFactory {
    public static Function<Channel, ChannelHandler> createDownstreamSslHandler(
            final DownstreamSslConfig config,
            final Endpoint downstreamEndpoint,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        final HostnameCheck hostnameCheck = config.isHostnameVerificationEnabled()
                ? new HttpClientHostnameCheck(downstreamEndpoint.getHost())
                : NULL_VERIFIER;
        return createDownstreamSslHandler(
                hostnameCheck,
                createReloader(config.getTrustStore(), clock, filesystem),
                config.getKeyStore().map(it -> createReloader(it, clock, filesystem)),
                Optional.of(downstreamEndpoint)
        );
    }

    public static Function<Channel, ChannelHandler> createDownstreamSslHandler(
            final HostnameCheck hostnameCheck,
            final KeyStoreManager trustStoreManager,
            final Optional<KeyStoreManager> keyStoreManager,
            final Optional<Endpoint> peerEndpoint
    ) {
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final HostCheckingTrustManager trustManager = new HostCheckingTrustManager(
                    createTrustManagers(trustStoreManager),
                    hostnameCheck
            );
            final X509TrustManager[] trustManagers = {
                    trustManager
            };
            final X509KeyManager[] keyManagers = createKeyManagers(keyStoreManager);
            clientContext.init(keyManagers, trustManagers, null);
            final SSLEngine engine = peerEndpoint
                    .map(it -> clientContext.createSSLEngine(it.getHost(), it.getPort()))
                    .orElse(clientContext.createSSLEngine());
            engine.setUseClientMode(true);
            final SslContext context = SslContextBuilder.forClient()
                    .keyManager(createKeyManagerFactory(keyStoreManager))
                    .trustManager(trustManager)
                    .build();
            if (peerEndpoint.isPresent()) {
                return ch -> {
                    final Endpoint endpoint = peerEndpoint.get();
                    return context.newHandler(ch.alloc(), endpoint.getHost(), endpoint.getPort());
                };
            } else {
                return ch -> context.newHandler(ch.alloc());
            }
        } catch (final NoSuchAlgorithmException | KeyManagementException | SSLException e) {
            throw new RuntimeException("Failed to initialize downstream SSL handler", e);
        }
    }
}
