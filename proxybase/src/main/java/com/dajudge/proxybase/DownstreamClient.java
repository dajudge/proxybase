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
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static com.dajudge.proxybase.ClientSslHandlerFactory.createHandler;
import static com.dajudge.proxybase.ProxyChannel.DOWNSTREAM_INBOUND_HANDLER;
import static com.dajudge.proxybase.ProxyChannel.DOWNSTREAM_SSL_HANDLER;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;


class DownstreamClient<I, O> implements Sink<O> {
    private static final Logger LOG = LoggerFactory.getLogger(DownstreamClient.class);
    private final Channel channel;

    DownstreamClient(
            final String channelId,
            final Endpoint endpoint,
            final DownstreamSslConfig sslConfig,
            final Sink<I> messageSink,
            final EventLoopGroup workerGroup,
            final KeyStoreWrapper keyStore,
            final Consumer<ChannelPipeline> pipelineCustomizer
    ) {
        final ChannelHandler sslHandler = createHandler(sslConfig, endpoint, keyStore);
        final DownstreamInboundHandler<I> inboundHandler = new DownstreamInboundHandler<>(channelId, messageSink);
        try {
            channel = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(DOWNSTREAM_SSL_HANDLER, sslHandler);
                            pipeline.addLast(DOWNSTREAM_INBOUND_HANDLER, inboundHandler);
                            pipelineCustomizer.accept(pipeline);
                        }
                    })
                    .connect(endpoint.getHost(), endpoint.getPort()).sync().channel();
            LOG.trace("Downstream channel established: {}", endpoint);
            channel.closeFuture().addListener(future -> {
                LOG.trace("Downstream channel closed: {}", endpoint);
                messageSink.close();
            });
            LOG.trace("Downstream connection established to {}", endpoint);
        } catch (final InterruptedException e) {
            LOG.debug("Failed to establish downstream connection to {}", endpoint, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ChannelFuture close() {
        return channel.close();
    }

    @Override
    public void accept(final O msg) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending message downstream: {}", msg);
        }
        channel.writeAndFlush(msg).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to send message downstream: {}", msg, future.cause());
                }
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sent message downstream: {}", msg);
                }
            }
        });
    }
}
