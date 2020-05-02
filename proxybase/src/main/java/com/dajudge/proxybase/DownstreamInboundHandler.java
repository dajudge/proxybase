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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.dajudge.proxybase.LogHelper.withChannelId;

class DownstreamInboundHandler<T> extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(DownstreamInboundHandler.class);
    private final String channelId;
    private final Sink<T> messageSink;

    DownstreamInboundHandler(
            final String channelId,
            final Sink<T> messageSink
    ) {
        this.channelId = channelId;
        this.messageSink = messageSink;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        withChannelId(channelId, () -> {
            @SuppressWarnings("unchecked") final T m = (T) msg; // In the user we trust
            if (LOG.isTraceEnabled()) {
                LOG.trace("Received message from downstream: {}", m);
            }
            try {
                messageSink.accept(m);
            } catch (final ProxyInternalException e) {
                LOG.error("Internal proxy error processing message from downstream. Killing channel.", e);
                ctx.close();
            } catch (final Exception e) {
                LOG.debug("Exception processing message from downstrean. Killing channel.", e);
                ctx.close();
            }
        });
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        messageSink.close();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        withChannelId(channelId, () -> {
            LOG.debug("Uncaught exception processing message from downstream. Killing channel.", cause);
            ctx.close();
        });
    }
}
