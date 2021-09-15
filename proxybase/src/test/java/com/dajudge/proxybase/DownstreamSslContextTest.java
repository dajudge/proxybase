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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.dajudge.proxybase.HostnameCheck.NULL_VERIFIER;
import static com.dajudge.proxybase.SslUtils.createClientSslContext;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.junit.Assert.*;

public class DownstreamSslContextTest {

    @Test
    public void default_accepts_valid_certificate() throws Throwable {
        httpsGet("www.example.com", 443);
    }

    @Test(expected = DecoderException.class)
    public void default_rejects_invalid_certificate() throws Throwable {
        httpsGet("self-signed.badssl.com", 443);
    }

    private static void httpsGet(final String hostname, final int port) throws Throwable {
        final Function<Channel, SslHandler> handlerFactory = chan -> createContext()
                .newHandler(chan.alloc(), hostname, port);
        httpGet(handlerFactory, hostname, port);
    }

    private static void httpGet(
            final Function<Channel, SslHandler> sslHandlerFactory,
            final String host,
            final int port
    ) throws Throwable {
        final AtomicReference<Throwable> channelThrowable = new AtomicReference<>();
        final ChannelHandler clientHandler = new SimpleChannelInboundHandler<HttpObject>() {
            @Override
            protected void channelRead0(final ChannelHandlerContext ctx, final HttpObject msg) {
                if (msg instanceof HttpResponse) {
                    final HttpResponse response = (HttpResponse) msg;
                    assertEquals(200, response.status().code());
                }
                if (msg instanceof HttpContent) {
                    final HttpContent content = (HttpContent) msg;
                    if (content instanceof LastHttpContent) {
                        ctx.close();
                    }
                }
            }

            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
                channelThrowable.set(cause);
            }
        };
        final ChannelInitializer<SocketChannel> initializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                final ChannelPipeline p = ch.pipeline();
                p.addLast(sslHandlerFactory.apply(ch));
                p.addLast(new HttpClientCodec());
                p.addLast(new HttpContentDecompressor());
                p.addLast(clientHandler);
            }
        };
        final EventLoopGroup group = new NioEventLoopGroup();
        try {
            final Channel ch = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(initializer)
                    .connect(host, port)
                    .sync()
                    .channel();
            final HttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/", EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            ch.writeAndFlush(request);
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }

        if (channelThrowable.get() != null) {
            throw channelThrowable.get();
        }
    }

    private static SslContext createContext() {
        try {
            return createClientSslContext(
                    NULL_VERIFIER,
                    Optional.empty(),
                    Optional.empty()
            );
        } catch (final NoSuchAlgorithmException | KeyManagementException | SSLException e) {
            throw new RuntimeException(e);
        }
    }
}
