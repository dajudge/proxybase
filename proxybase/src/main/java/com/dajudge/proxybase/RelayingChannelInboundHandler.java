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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelayingChannelInboundHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(RelayingChannelInboundHandler.class);
    private final String direction;
    private final Channel fwd;

    public RelayingChannelInboundHandler(final String direction, final Channel fwd) {
        this.direction = direction;
        this.fwd = fwd;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Relaying message {}: {}", direction, msg);
        }
        fwd.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        LOG.error("Exception in {} channel.", direction, cause);
        ctx.close();
    }
}
