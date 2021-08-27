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

import com.dajudge.proxybase.ProxyChannelFactory.ProxyChannelInitializer;
import com.dajudge.proxybase.ca.test.TestCertificationAuthority;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.util.TestSslConfiguration.SocketFactory;
import com.dajudge.proxybase.util.TestSslConfiguration.SslConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;

public abstract class BaseProxyTest {
    private static final Logger LOG = LoggerFactory.getLogger(BaseProxyTest.class);
    protected static final String KEYSTORE_TYPE = "jks";
    static final TestCertificationAuthority DOWNSTREAM_SERVER_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamServerCA", KEYSTORE_TYPE);
    static final TestCertificationAuthority DOWNSTREAM_CLIENT_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamClientCA", KEYSTORE_TYPE);
    static final TestCertificationAuthority UPSTREAM_SERVER_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamServerCA", KEYSTORE_TYPE);
    static final TestCertificationAuthority UPSTREAM_CLIENT_CA =
            new TestCertificationAuthority(System::currentTimeMillis, "cn=downstreamClientCA", KEYSTORE_TYPE);

    private void withDownstreamServer(
            final SslConfiguration sslConfig,
            final Consumer<ServerSocket> consumer,
            final List<Consumer<Socket>> connectionAssertions
    ) {
        try (final ServerSocket downstreamServer = sslConfig.serverSocket()) {
            downstreamServer.bind(new InetSocketAddress("127.0.0.1", 0));
            new Thread(() -> {
                try {
                    while (true) {
                        final Socket socket = downstreamServer.accept();
                        connectionAssertions.forEach(c -> {
                            try {
                                c.accept(socket);
                            } catch (final Throwable t) {
                                try {
                                    socket.close();
                                    if (t instanceof AssertionError) {
                                        throw (AssertionError) t;
                                    }
                                    throw new AssertionError(t);
                                } catch (final IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        handleSocket(socket);
                    }
                } catch (final SocketException e) {
                    LOG.debug("Downstream server socket closed: " + e.getMessage());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }).start();
            consumer.accept(downstreamServer);
        } catch (final IOException | NoSuchAlgorithmException | KeyStoreException
                | KeyManagementException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertRoundtripWorksWithoutProxy(
            final SslConfiguration directSslConfig,
            final List<Consumer<Socket>> downstreamSocketAssertions
    ) {
        withDownstreamServer(directSslConfig, downstreamServer -> {
            assertRoundtripWorks(downstreamServer.getLocalSocketAddress(), directSslConfig::clientSocket);
        }, downstreamSocketAssertions);
    }

    protected void assertRoundtripWorksWithProxy(
            final SslConfiguration upstreamSslConfig,
            final SslConfiguration downstreamSslConfig,
            final List<Consumer<Socket>> downstreamSocketAssertions
    ) {
        final Consumer<Integer> proxyAssertion = port ->
                assertRoundtripWorks(new InetSocketAddress("127.0.0.1", port), upstreamSslConfig::clientSocket);
        withProxy(upstreamSslConfig, downstreamSslConfig, downstreamSocketAssertions, proxyAssertion);
    }

    protected void withProxy(
            final SslConfiguration upstreamSslConfig,
            final SslConfiguration downstreamSslConfig,
            final List<Consumer<Socket>> downstreamSocketAssertions,
            final Consumer<Integer> proxyConsumer
    ) {
        withDownstreamServer(downstreamSslConfig, downstreamServer -> {
            final int port = freePort();
            try (final ProxyApplication ignored = new ProxyApplication(factory -> {
                final ProxyChannelInitializer initializer = (upstreamChannel, downstreamChannel) -> {
                    upstreamChannel.pipeline()
                            .addLast(new RelayingChannelInboundHandler("downstream", downstreamChannel));
                    upstreamSslConfig.configureUpstreamPipeline(upstreamChannel.pipeline());
                    downstreamChannel.pipeline()
                            .addLast(new RelayingChannelInboundHandler("upstream", upstreamChannel));
                    downstreamSslConfig.configureDownstreamPipeline(downstreamChannel.pipeline());
                };
                factory.createProxyChannel(
                        new Endpoint("127.0.0.1", port),
                        new Endpoint("127.0.0.1", downstreamServer.getLocalPort()),
                        initializer
                );
            })) {
                proxyConsumer.accept(port);
            }
        }, downstreamSocketAssertions);
    }

    private int freePort() {
        try (final ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
            return serverSocket.getLocalPort();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertRoundtripWorks(final SocketAddress endpoint, final SocketFactory socketFactory) {
        final String message = IntStream.range(0, 100)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(joining(","));
        final byte[] messageBytes = message.getBytes(UTF_8);
        try (final Socket socket = socketFactory.create()) {
            socket.connect(endpoint);
            socket.getOutputStream().write(messageBytes);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bos.size() < messageBytes.length) {
                final byte[] buffer = new byte[1024];
                final int len = socket.getInputStream().read(buffer);
                if (len < 0) {
                    throw new AssertionError("Premature end of stream");
                }
                bos.write(buffer, 0, len);
            }
            final String actual = new String(bos.toByteArray(), UTF_8);
            assertEquals(message, actual);
        } catch (final IOException | NoSuchAlgorithmException | KeyStoreException
                | KeyManagementException | UnrecoverableKeyException e) {
            throw new AssertionError(e);
        }
    }

    private void handleSocket(final Socket socket) {
        new Thread(() -> {
            try {
                final InputStream is = socket.getInputStream();
                final byte[] read = new byte[1024];
                int len;
                while ((len = is.read(read)) > 0) {
                    socket.getOutputStream().write(read, 0, len);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
