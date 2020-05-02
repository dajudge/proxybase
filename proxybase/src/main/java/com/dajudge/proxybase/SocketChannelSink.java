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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SocketChannelSink<T> implements Sink<T> {
    private final Logger LOG = LoggerFactory.getLogger(SocketChannelSink.class);
    private final SocketChannel ch;

    public SocketChannelSink(final SocketChannel ch) {
        this.ch = ch;
    }

    @Override
    public ChannelFuture close() {
        LOG.trace("Closing upstream channel.");
        return ch.close().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOG.trace("Upstream channel closed.");
            } else {
                LOG.warn("Cloud not close upstream channel.", future.cause());
            }
        });
    }

    @Override
    public void accept(final T buffer) {
        ch.writeAndFlush(buffer).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                LOG.error("Failed to send message upstream: {}", buffer, future.cause());
            } else {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Sent message upstream: {}", buffer);
                }
            }
        });
    }
}
