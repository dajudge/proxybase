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
import io.netty.channel.ChannelPipeline;

/**
 * Factory for {@link ProxyContext} instances.
 *
 * @param <UI> Upstream inbound type.
 * @param <UO> Upstream outbound type.
 * @param <DI> Downstream inbound type.
 * @param <DO> Downstream outbound type.
 */
public interface ProxyContextFactory<UI, UO, DI, DO> {

    interface ProxyContext<UI, UO, DI, DO> {

        void customizeDownstreamPipeline(ChannelPipeline pipeline);

        void customizeUpstreamPipeline(ChannelPipeline pipeline);

        Sink<UI> downstreamFilter(Sink<DO> downstream);

        Sink<DI> upstreamFilter(Sink<UO> upstream);
    }

    abstract class AbstractSimpleProxyContext
            extends AbstractProxyContext<ByteBuf, ByteBuf, ByteBuf, ByteBuf> {

    }

    abstract class AbstractProxyContext<UI, UO, DI, DO> implements ProxyContext<UI, UO, DI, DO> {

        @Override
        public void customizeDownstreamPipeline(ChannelPipeline pipeline) {
            // Don't customize
        }

        @Override
        public void customizeUpstreamPipeline(ChannelPipeline pipeline) {
            // Don't customize
        }
    }

    ProxyContext<UI, UO, DI, DO> createProxyContext();
}
