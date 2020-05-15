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

import com.dajudge.proxybase.ProxyContextFactory.ProxyContext;
import com.dajudge.proxybase.ca.CertificateAuthority;
import com.dajudge.proxybase.ca.KeyStoreWrapper;
import com.dajudge.proxybase.ca.UpstreamCertificateSupplier;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetSocketAddress;
import java.util.UUID;

import static com.dajudge.proxybase.LogHelper.withChannelId;
import static com.dajudge.proxybase.ProxySslHandlerFactory.createSslHandler;

public class ProxyChannel<UI, UO, DI, DO> {
    private static final String PREFIX = ProxyChannel.class.getName();
    public static final String DOWNSTREAM_SSL_HANDLER = PREFIX + "/downstream-ssl-handler";
    public static final String DOWNSTREAM_INBOUND_HANDLER = PREFIX + "/downstream-inbound-handler";
    public static final String UPSTREAM_SSL_HANDLER = PREFIX + "/upstream-ssl-handler";
    public static final String UPSTREAM_INBOUND_HANDLER = PREFIX + "/upstream-inbound-handler";
    private static final Logger LOG = LoggerFactory.getLogger(ProxyChannel.class);
    private boolean initialized = false;
    private final Endpoint endpoint;
    private final UpstreamSslConfig sslConfig;
    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup upstreamWorkerGroup;
    private final DownstreamChannelFactory<DI, DO> downstreamSinkFactory;
    private final CertificateAuthority certificateAuthority;
    private final ProxyContextFactory<UI, UO, DI, DO> proxyContextFactory;
    private Channel channel;

    ProxyChannel(
            final Endpoint endpoint,
            final UpstreamSslConfig sslConfig,
            final NioEventLoopGroup bossGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final DownstreamChannelFactory<DI, DO> downstreamSinkFactory,
            final CertificateAuthority certificateAuthority,
            final ProxyContextFactory<UI, UO, DI, DO> proxyContextFactory
    ) {
        this.endpoint = endpoint;
        this.sslConfig = sslConfig;
        this.bossGroup = bossGroup;
        this.upstreamWorkerGroup = upstreamWorkerGroup;
        this.downstreamSinkFactory = downstreamSinkFactory;
        this.certificateAuthority = certificateAuthority;
        this.proxyContextFactory = proxyContextFactory;
    }

    public void start() {
        if (initialized) {
            return;
        }
        initialized = true;
        LOG.info("Starting proxy channel {}", endpoint);
        try {
            channel = new ServerBootstrap()
                    .group(bossGroup, upstreamWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(createProxyInitializer(sslConfig))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .bind(endpoint.getHost(), endpoint.getPort())
                    .sync()
                    .channel();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private ChannelInitializer<SocketChannel> createProxyInitializer(
            final UpstreamSslConfig upstreamSslConfig
    ) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel ch) {
                final String channelId = UUID.randomUUID().toString();
                withChannelId(channelId, () -> {
                    final ProxyContext<UI, UO, DI, DO> proxyContext = proxyContextFactory.createProxyContext();
                    final ChannelPipeline pipeline = ch.pipeline();
                    proxyContext.customizeUpstreamPipeline(pipeline);
                    LOG.debug("Incoming connection on {} from {}", ch.localAddress(), ch.remoteAddress());
                    final ChannelHandler sslHandler = createSslHandler(upstreamSslConfig);
                    final UpstreamInboundHandler<UI> inboundHandler = createDownstreamHandler(
                            channelId,
                            new SocketChannelSink<>(ch),
                            proxyContext
                    );
                    pipeline.addLast(UPSTREAM_SSL_HANDLER, sslHandler);
                    pipeline.addLast(UPSTREAM_INBOUND_HANDLER, inboundHandler);
                });
            }
        };
    }

    private UpstreamInboundHandler<UI> createDownstreamHandler(
            final String channelId,
            final Sink<UO> upstreamSink,
            final ProxyContext<UI, UO, DI, DO> proxyContext
    ) {
        return new UpstreamInboundHandler<>(channelId, certSupplier -> {
            try {
                return proxyContext.downstreamFilter(downstreamSinkFactory.create(
                        channelId,
                        proxyContext.upstreamFilter(upstreamSink),
                        getClientKeystore(certSupplier),
                        proxyContext::customizeDownstreamPipeline
                ));
            } catch (final RuntimeException e) {
                LOG.error("Failed to create downstream channel", e);
                throw e;
            }
        });
    }

    private KeyStoreWrapper getClientKeystore(final UpstreamCertificateSupplier certSupplier) {
        try {
            return certificateAuthority.createClientCertificate(certSupplier);
        } catch (final SSLPeerUnverifiedException e) {
            throw new RuntimeException("Client did not provide valid certificate", e);
        }
    }

    public ChannelFuture close() {
        return channel.close();
    }

    public int getPort() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    public String getBindAddress() {
        return ((InetSocketAddress) channel.localAddress()).getHostName();
    }

}
