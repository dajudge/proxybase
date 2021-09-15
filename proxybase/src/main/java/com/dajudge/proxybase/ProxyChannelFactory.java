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

import com.dajudge.proxybase.config.Endpoint;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

import java.util.UUID;
import java.util.function.Consumer;

import static com.dajudge.proxybase.ProxyApplication.LOGGING_CONTEXT_HANDLER;

public class ProxyChannelFactory {
    private final UpstreamChannelFactory upstreamFactory;
    private final DownstreamChannelFactory downstreamFactory;
    private final Consumer<Channel> serverChannelRegistry;

    ProxyChannelFactory(
            final UpstreamChannelFactory upstreamFactory,
            final DownstreamChannelFactory downstreamFactory,
            final Consumer<Channel> serverChannelRegistry
    ) {
        this.upstreamFactory = upstreamFactory;
        this.downstreamFactory = downstreamFactory;
        this.serverChannelRegistry = serverChannelRegistry;
    }

    public void createProxyChannel(
            final Endpoint upstreamEndpoint,
            final Endpoint downstreamEndpoint,
            final ProxyChannelInitializer initializer
    ) {
        serverChannelRegistry.accept(
                upstreamFactory.create(
                        upstreamEndpoint,
                        upstreamChannel -> downstreamFactory.create(
                                downstreamEndpoint,
                                downstreamChannel -> initProxyChannel(
                                        initializer,
                                        upstreamChannel,
                                        downstreamChannel
                                )
                        )
                )
        );
    }

    private void initProxyChannel(
            final ProxyChannelInitializer initializer,
            final SocketChannel upstreamChannel,
            final SocketChannel downstreamChannel
    ) {
        final String channelId = UUID.randomUUID().toString();
        upstreamChannel.pipeline().addFirst(
                LOGGING_CONTEXT_HANDLER,
                new LoggingContextHandler(channelId, "upstream")
        );
        downstreamChannel.pipeline().addFirst(
                LOGGING_CONTEXT_HANDLER,
                new LoggingContextHandler(channelId, "downstream")
        );
        initializer.initialize(upstreamChannel, downstreamChannel);
        downstreamChannel.closeFuture().addListener(future -> upstreamChannel.close());
        upstreamChannel.closeFuture().addListener(future -> downstreamChannel.close());
    }

    public interface ProxyChannelInitializer {
        void initialize(Channel upstreamChannel, Channel downstreamChannel);
    }
}
