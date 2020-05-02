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

import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public abstract class ProxyApplication<UI, UO, DI, DO> {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApplication.class);
    private final UpstreamSslConfig upstreamSslConfig;
    private final DownstreamSslConfig downstreamSslConfig;
    private final CertificateAuthority certificateAuthority;
    private Runnable shutdownRunnable;

    protected ProxyApplication(
            final UpstreamSslConfig upstreamSslConfig,
            final DownstreamSslConfig downstreamSslConfig,
            final CertificateAuthority certificateAuthority
    ) {
        this.upstreamSslConfig = upstreamSslConfig;
        this.downstreamSslConfig = downstreamSslConfig;
        this.certificateAuthority = certificateAuthority;
    }

    public void shutdown() {
        if (shutdownRunnable == null) {
            throw new IllegalStateException("must invoke start() first");
        }
        shutdownRunnable.run();
    }

    public ProxyApplication<UI, UO, DI, DO> start() {
        final NioEventLoopGroup serverWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup upstreamWorkerGroup = new NioEventLoopGroup();
        final NioEventLoopGroup downstreamWorkerGroup = new NioEventLoopGroup();
        final ProxyChannelFactory<UI, UO, DI, DO> proxyChannelFactory = new ProxyChannelFactory<>(
                downstreamWorkerGroup,
                serverWorkerGroup,
                upstreamWorkerGroup,
            upstreamSslConfig,
            downstreamSslConfig,
                certificateAuthority
        );
        final Collection<ProxyChannel<UI, UO, DI, DO>> proxyChannels =
                initializeProxyChannels(proxyChannelFactory);
        shutdownRunnable = () -> {
            proxyChannels.stream()
                    .map(ProxyChannel::close)
                    .collect(toList())
                    .forEach(future -> {
                        try {
                            future.sync();
                        } catch (final Exception e) {
                            LOG.error("Failed to sync with proxy channel", e);
                        }
                    });
            serverWorkerGroup.shutdownGracefully();
            upstreamWorkerGroup.shutdownGracefully();
            downstreamWorkerGroup.shutdownGracefully();
        };
        return this;
    }

    protected abstract Collection<ProxyChannel<UI, UO, DI, DO>> initializeProxyChannels(
            final ProxyChannelFactory<UI, UO, DI, DO> proxyChannelFactory
    );

    public abstract class SimpleProxyApplication extends ProxyApplication<ByteBuf, ByteBuf, ByteBuf, ByteBuf> {
        protected SimpleProxyApplication(
                final UpstreamSslConfig upstreamSslConfig,
                final DownstreamSslConfig downstreamSslConfig,
                final CertificateAuthority certificateAuthority
        ) {
            super(upstreamSslConfig, downstreamSslConfig, certificateAuthority);
        }
    }
}
