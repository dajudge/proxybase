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

import com.dajudge.proxybase.config.Endpoint;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static io.netty.channel.ChannelOption.SO_KEEPALIVE;

public class DownstreamChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DownstreamChannelFactory.class);
    private final NioEventLoopGroup workerGroup;

    public DownstreamChannelFactory(final NioEventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    public Channel create(
            final Endpoint endpoint,
            final Consumer<SocketChannel> initializer
    ) {
        try {
            final Channel channel = new Bootstrap()
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(final SocketChannel ch) {
                            initializer.accept(ch);
                        }
                    })
                    .connect(endpoint.getHost(), endpoint.getPort())
                    .sync()
                    .channel();
            channel.closeFuture().addListener(future -> {
                if (future.isSuccess()) {
                    LOG.debug("Closed downstream channel for {}:{}", endpoint.getHost(), endpoint.getPort());
                } else {
                    LOG.error("Failed to close downstream channel for {}:{}",
                            endpoint.getHost(),
                            endpoint.getPort(),
                            future.cause());
                }
            });
            LOG.trace("Downstream channel established: {}", endpoint);
            return channel;
        } catch (final InterruptedException e) {
            throw new RuntimeException("Failed to establish downstream channel: " + endpoint, e);
        }
    }
}
