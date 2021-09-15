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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static com.dajudge.proxybase.LogHelper.withChannelId;

class LoggingContextHandler extends ChannelInboundHandlerAdapter {

    private final String channelId;
    private final String channelType;

    public LoggingContextHandler(final String channelId, final String channelType) {
        this.channelId = channelId;
        this.channelType = channelType;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        try {
            withChannelId(channelId, channelType, () -> {
                try {
                    super.channelRead(ctx, msg);
                } catch (final Exception e) {
                    throw new ExceptionWrapper(e);
                }
            });
        } catch (final ExceptionWrapper e) {
            throw e.getCause();
        }
    }

    private static class ExceptionWrapper extends RuntimeException {
        public ExceptionWrapper(final Exception cause) {
            super(cause);
        }

        @Override
        public Exception getCause() {
            return (Exception) super.getCause();
        }
    }
}
