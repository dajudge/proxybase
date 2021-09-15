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

import javax.net.ssl.SSLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.dajudge.proxybase.HostnameCheck.NULL_VERIFIER;
import static com.dajudge.proxybase.SslUtils.createClientSslContext;
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
                createReloader(config.getKeyStore(), clock, filesystem),
                downstreamEndpoint
        );
    }

    public static Function<Channel, ChannelHandler> createDownstreamSslHandler(
            final HostnameCheck hostnameCheck,
            final Optional<? extends KeyStoreManager> trustStoreManager,
            final Optional<? extends KeyStoreManager> keyStoreManager,
            final Endpoint peerEndpoint
    ) {
        try {
            final SslContext context = createClientSslContext(hostnameCheck, trustStoreManager, keyStoreManager);
            return ch -> context.newHandler(ch.alloc(), peerEndpoint.getHost(), peerEndpoint.getPort());
        } catch (final NoSuchAlgorithmException | KeyManagementException | SSLException e) {
            throw new RuntimeException("Failed to initialize downstream SSL handler", e);
        }
    }

}
