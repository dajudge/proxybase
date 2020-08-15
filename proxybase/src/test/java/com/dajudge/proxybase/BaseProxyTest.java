package com.dajudge.proxybase;

import com.dajudge.proxybase.ProxyChannelFactory.ProxyChannelInitializer;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.channel.ChannelPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public abstract class BaseProxyTest {
    private static final Logger LOG = LoggerFactory.getLogger(BaseProxyTest.class);

    private void withDownstreamServer(final Consumer<ServerSocket> consumer) {
        try (final ServerSocket downstreamServer = new ServerSocket()) {
            downstreamServer.bind(new InetSocketAddress("127.0.0.1", 0));
            new Thread(() -> {
                try {
                    while (true) {
                        final Socket socket = downstreamServer.accept();
                        handleSocket(socket);
                    }
                } catch (final SocketException e) {
                    LOG.debug("Downstream server socket closed: " + e.getMessage());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }).start();
            consumer.accept(downstreamServer);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void assertRoundtripWorksWithoutProxy() {
        withDownstreamServer(downstreamServer -> {
            assertRoundtripWorks(downstreamServer.getLocalSocketAddress());
        });
    }

    protected void assertRoundtripWorksWithProxy(
            final Consumer<ChannelPipeline> upstreamChannelModifier,
            final Consumer<ChannelPipeline> downstreamChannelModifier
    ) {
        withDownstreamServer(downstreamServer -> {
            final int port = freePort();
            try (final ProxyApplication ignored = new ProxyApplication(factory -> {
                final ProxyChannelInitializer initializer = (upstreamChannel, downstreamChannel) -> {
                    upstreamChannel.pipeline()
                            .addLast(new RelayingChannelInboundHandler("downstream", downstreamChannel));
                    downstreamChannel.pipeline()
                            .addLast(new RelayingChannelInboundHandler("upstream", upstreamChannel));

                    upstreamChannelModifier.accept(upstreamChannel.pipeline());
                    downstreamChannelModifier.accept(downstreamChannel.pipeline());
                };
                factory.createProxyChannel(
                        new Endpoint("127.0.0.1", port),
                        new Endpoint("127.0.0.1", downstreamServer.getLocalPort()),
                        initializer
                );
            })) {
                assertRoundtripWorks(new InetSocketAddress("127.0.0.1", port));
            }
        });
    }

    private int freePort() {
        try (final ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));
            return serverSocket.getLocalPort();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertRoundtripWorks(final SocketAddress endpoint) {
        final String message = "Hello, world";
        final byte[] messageBytes = message.getBytes(UTF_8);
        try (final Socket socket = new Socket()) {
            socket.connect(endpoint);
            socket.getOutputStream().write(messageBytes);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bos.size() < messageBytes.length) {
                final byte[] buffer = new byte[1024];
                final int len = socket.getInputStream().read(buffer);
                bos.write(buffer, 0, len);
            }
            final String actual = new String(bos.toByteArray(), UTF_8);
            assertEquals(message, actual);
        } catch (final IOException e) {
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
