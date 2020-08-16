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
import io.netty.channel.ChannelPipeline;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;
import java.util.function.Consumer;

import static com.dajudge.proxybase.DownstreamSslHandlerFactory.createDownstreamSslHandler;
import static com.dajudge.proxybase.ProxyApplication.LOGGING_CONTEXT_HANDLER;
import static com.dajudge.proxybase.UpstreamSslHandlerFactory.createUpstreamSslHandler;

public class TestSslConfiguration {
    private final SslConfiguration upstream;
    private final SslConfiguration downstream;
    private final List<Consumer<Socket>> downstreamSocketAssertions;

    public TestSslConfiguration(
            final SslConfiguration upstream,
            final SslConfiguration downstream,
            final List<Consumer<Socket>> downstreamSocketAssertions
    ) {
        this.upstream = upstream;
        this.downstream = downstream;
        this.downstreamSocketAssertions = downstreamSocketAssertions;
    }

    public List<Consumer<Socket>> getDownstreamSocketAssertions() {
        return downstreamSocketAssertions;
    }

    public SslConfiguration getUpstream() {
        return upstream;
    }

    public SslConfiguration getDownstream() {
        return downstream;
    }

    public interface SslConfiguration {
        ServerSocket serverSocket()
                throws IOException, NoSuchAlgorithmException, KeyStoreException,
                KeyManagementException, UnrecoverableKeyException;

        Socket clientSocket()
                throws IOException, NoSuchAlgorithmException, KeyStoreException,
                KeyManagementException, UnrecoverableKeyException;

        void configureUpstreamPipeline(ChannelPipeline pipeline);

        void configureDownstreamPipeline(ChannelPipeline pipeline);
    }

    public interface SocketFactory {
        Socket create() throws IOException, NoSuchAlgorithmException, KeyStoreException,
                KeyManagementException, UnrecoverableKeyException;
    }

    public static class PlaintextSslConfiguration implements SslConfiguration {

        @Override
        public ServerSocket serverSocket() throws IOException {
            return new ServerSocket();
        }

        @Override
        public Socket clientSocket() {
            return new Socket();
        }

        @Override
        public void configureUpstreamPipeline(final ChannelPipeline pipeline) {
        }

        @Override
        public void configureDownstreamPipeline(final ChannelPipeline pipeline) {
        }
    }

    public static class TwoWaySslConfiguration extends OneWaySslConfiguration {

        private final KeyStoreManager clientKeyStoreManager;
        private final KeyStoreManager serverTrustStoreManager;

        public TwoWaySslConfiguration(
                final KeyStoreManager clientKeyStoreManager,
                final KeyStoreManager clientTrustStoreManager,
                final KeyStoreManager serverKeyStoreManager,
                final KeyStoreManager serverTrustStoreManager
        ) {
            super(clientTrustStoreManager, serverKeyStoreManager);
            this.clientKeyStoreManager = clientKeyStoreManager;
            this.serverTrustStoreManager = serverTrustStoreManager;
        }

        @Override
        protected KeyStoreManager getClientKeyStoreManager() {
            return clientKeyStoreManager;
        }

        @Override
        protected KeyStoreManager getServerTrustStoreManager() {
            return serverTrustStoreManager;
        }

        @Override
        protected TrustManager[] createServerTrustManagers()
                throws KeyStoreException, NoSuchAlgorithmException {
            return createTrustManagers(serverTrustStoreManager);
        }

        @Override
        protected KeyManager[] createClientKeyManagers()
                throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
            return createKeyManagers(clientKeyStoreManager);
        }

        @Override
        public SSLServerSocket serverSocket()
                throws IOException, NoSuchAlgorithmException, KeyStoreException,
                KeyManagementException, UnrecoverableKeyException {
            final SSLServerSocket ret = super.serverSocket();
            ret.setWantClientAuth(true);
            return ret;
        }
    }

    public static class OneWaySslConfiguration implements SslConfiguration {
        private final KeyStoreManager serverKeyStoreManager;
        private final KeyStoreManager clientTrustStoreManager;

        public OneWaySslConfiguration(
                final KeyStoreManager clientTrustStoreManager,
                final KeyStoreManager serverKeyStoreManager
        ) {
            this.clientTrustStoreManager = clientTrustStoreManager;
            this.serverKeyStoreManager = serverKeyStoreManager;
        }

        @Override
        public SSLServerSocket serverSocket()
                throws IOException, NoSuchAlgorithmException, KeyStoreException,
                KeyManagementException, UnrecoverableKeyException {
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(createServerKeyManagers(), createServerTrustManagers(), null);
            return (SSLServerSocket) context.getServerSocketFactory().createServerSocket();
        }

        @Override
        public SSLSocket clientSocket() throws IOException, NoSuchAlgorithmException,
                KeyStoreException, KeyManagementException, UnrecoverableKeyException {
            final SSLContext context = SSLContext.getInstance("TLS");
            context.init(createClientKeyManagers(), createClientTrustManagers(), null);
            return (SSLSocket) context.getSocketFactory().createSocket();
        }

        @Override
        public void configureUpstreamPipeline(final ChannelPipeline pipeline) {
            final KeyStoreManager serverTrustStoreManager = getServerTrustStoreManager();
            pipeline.addAfter(
                    LOGGING_CONTEXT_HANDLER,
                    "SSL",
                    createUpstreamSslHandler(
                            serverTrustStoreManager != null,
                            serverTrustStoreManager,
                            serverKeyStoreManager
                    )
            );
        }

        @Override
        public void configureDownstreamPipeline(final ChannelPipeline pipeline) {
            pipeline.addAfter(
                    LOGGING_CONTEXT_HANDLER,
                    "SSL",
                    createDownstreamSslHandler(
                            HostnameCheck.NULL_VERIFIER,
                            clientTrustStoreManager,
                            getClientKeyStoreManager()
                    )
            );
        }

        protected KeyStoreManager getClientKeyStoreManager() {
            return null;
        }

        protected KeyStoreManager getServerTrustStoreManager() {
            return null;
        }

        protected TrustManager[] createServerTrustManagers()
                throws KeyStoreException, NoSuchAlgorithmException {
            return null;
        }

        protected KeyManager[] createClientKeyManagers()
                throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
            return null;
        }

        private KeyManager[] createServerKeyManagers()
                throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
            return createKeyManagers(serverKeyStoreManager);
        }

        private TrustManager[] createClientTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
            return createTrustManagers(clientTrustStoreManager);
        }

        protected static KeyManager[] createKeyManagers(final KeyStoreManager keyStoreManager)
                throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
            if (keyStoreManager == null) {
                return null;
            }
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(
                    keyStoreManager.getKeyStore().getKeyStore(),
                    keyStoreManager.getKeyStore().getKeyPassword()
            );
            return keyManagerFactory.getKeyManagers();
        }

        protected static TrustManager[] createTrustManagers(final KeyStoreManager keyStoreManager)
                throws NoSuchAlgorithmException, KeyStoreException {
            if (keyStoreManager == null) {
                return null;
            }
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStoreManager.getKeyStore().getKeyStore());
            return trustManagerFactory.getTrustManagers();
        }
    }
}
