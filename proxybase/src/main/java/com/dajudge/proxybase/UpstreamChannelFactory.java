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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;

class UpstreamChannelFactory {
    private static final Logger LOG = LoggerFactory.getLogger(UpstreamChannelFactory.class);

    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;

    UpstreamChannelFactory(
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup
    ) {
        this.bossGroup = bossGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
    }

    Channel create(
            final Endpoint endpoint,
            final Consumer<SocketChannel> inizializer
    ) {
        final List<SocketChannel> openChildChannels = new ArrayList<>();
        try {
            final Channel channel = new ServerBootstrap()
                    .group(bossGroup, upstreamWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(final SocketChannel ch) throws Exception {
                            LOG.debug("Incoming connection on {}:{} from {}:{}",
                                    ch.localAddress().getHostString(),
                                    ch.localAddress().getPort(),
                                    ch.remoteAddress().getHostString(),
                                    ch.remoteAddress().getPort());

                            openChildChannels.add(ch);
                            inizializer.accept(ch);
                            ch.closeFuture().addListener(future -> openChildChannels.remove(ch));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .bind(endpoint.getHost(), endpoint.getPort())
                    .sync()
                    .channel();
            channel.closeFuture().addListener(future -> closeChildChannels(openChildChannels));
            final InetSocketAddress address = (InetSocketAddress) channel.localAddress();
            LOG.debug("Upstream channel bound: {}:{}", address.getHostString(), address.getPort());
            return channel;
        } catch (final InterruptedException e) {
            throw new RuntimeException("Failed to bind upstream channel: " + endpoint, e);
        }
    }

    private static void closeChildChannels(final List<SocketChannel> openChildChannels) {
        new ArrayList<>(openChildChannels).stream()
                .map(SocketChannel::closeFuture)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .forEach(UpstreamChannelFactory::awaitChildClosed);
    }

    private static void awaitChildClosed(final ChannelFuture childFuture) {
        final InetSocketAddress address = (InetSocketAddress) childFuture.channel().remoteAddress();
        try {
            childFuture.await(5, SECONDS);
            LOG.debug("Closed upstream channel for {}:{}", address.getAddress(), address.getPort());
        } catch (final InterruptedException e) {
            LOG.error(
                    "Failed to close upstream channel for {}:{}",
                    address.getAddress(),
                    address.getPort(),
                    e
            );
        }
    }

}
