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

import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ProxyApplication implements AutoCloseable {
    public static final String LOGGING_CONTEXT_HANDLER = ProxyApplication.class.getName() + "#loggingContext";
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApplication.class);
    private final NioEventLoopGroup serverGroup;
    private final NioEventLoopGroup upstreamGroup;
    private final NioEventLoopGroup downstreamGroup;
    private final Collection<Channel> serverChannels = new ArrayList<>();

    public ProxyApplication(final Consumer<ProxyChannelFactory> callback) {
        this.serverGroup = new NioEventLoopGroup();
        this.upstreamGroup = new NioEventLoopGroup();
        this.downstreamGroup = new NioEventLoopGroup();
        final UpstreamChannelFactory upstreamFactory = new UpstreamChannelFactory(serverGroup, upstreamGroup);
        final DownstreamChannelFactory downstreamFactory = new DownstreamChannelFactory(downstreamGroup);
        callback.accept(new ProxyChannelFactory(upstreamFactory, downstreamFactory, serverChannels::add));
    }


    @Override
    public void close() {
        serverChannels.forEach(ch -> {
            final InetSocketAddress address = (InetSocketAddress) ch.localAddress();
            try {
                ch.close().await(5, TimeUnit.SECONDS);
                LOG.debug("Server channel closed: {}:{}", address.getHostString(), address.getPort());
            } catch (final InterruptedException e) {
                LOG.error("Failed to close server channel: {}:{}", address.getHostString(), address.getPort(), e);
            }
        });
        serverGroup.shutdownGracefully();
        upstreamGroup.shutdownGracefully();
        downstreamGroup.shutdownGracefully();
    }
}
