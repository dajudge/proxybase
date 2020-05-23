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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public abstract class AbstractChunkedMessageStreamInboundHandler<T extends AbstractChunkedMessage>
        extends ChannelInboundHandlerAdapter {

    private final ChunkedMessageCollector<T> collector = new ChunkedMessageCollector<>(
            this::createNewMessage
    );

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        collector.append((ByteBuf) msg, parsedMessage -> onMessageComplete(ctx, parsedMessage));
    }

    protected abstract void onMessageComplete(final ChannelHandlerContext ctx, final T message);

    protected abstract T createNewMessage();

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        collector.shutdown();
    }
}
