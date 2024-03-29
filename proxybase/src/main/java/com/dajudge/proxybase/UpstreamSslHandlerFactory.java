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

import com.dajudge.proxybase.certs.Filesystem;
import com.dajudge.proxybase.certs.KeyStoreManager;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import io.netty.channel.ChannelHandler;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Supplier;

import static com.dajudge.proxybase.SslUtils.createKeyManagers;
import static com.dajudge.proxybase.SslUtils.createTrustManagers;
import static com.dajudge.proxybase.certs.ReloadingKeyStoreManager.createReloader;

public class UpstreamSslHandlerFactory {
    public static ChannelHandler createUpstreamSslHandler(
            final UpstreamSslConfig config,
            final Supplier<Long> clock,
            final Filesystem filesystem
    ) {
        return createUpstreamSslHandler(
                config.isClientAuthRequired(),
                config.getTrustStore().map(trustStore -> createReloader(trustStore, clock, filesystem)),
                createReloader(config.getKeyStore(), clock, filesystem)
        );
    }

    public static ChannelHandler createUpstreamSslHandler(
            final boolean enableClientAuth,
            final Optional<KeyStoreManager> trustStoreManager,
            final KeyStoreManager keyStoreManager
    ) {
        try {
            final SSLContext clientContext = SSLContext.getInstance("TLS");
            final TrustManager[] trustManagers = createTrustManagers(trustStoreManager);
            final KeyManager[] keyManagers = createKeyManagers(keyStoreManager);
            clientContext.init(keyManagers, trustManagers, null);
            final SSLEngine engine = clientContext.createSSLEngine();
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(enableClientAuth);
            return new SslHandler(engine);
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to initialize upstream SSL handler", e);
        }
    }
}
