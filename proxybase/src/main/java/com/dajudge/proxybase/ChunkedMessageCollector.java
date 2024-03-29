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

import io.netty.buffer.ByteBuf;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ChunkedMessageCollector<T extends AbstractChunkedMessage> {
    private T currentMessage;
    private final Supplier<T> messageFactory;

    public ChunkedMessageCollector(final Supplier<T> messageFactory) {
        this.messageFactory = messageFactory;
    }

    public void append(final ByteBuf msg, final Consumer<T> messageConsumer) {
        if (currentMessage == null) {
            currentMessage = messageFactory.get();
        }
        final ByteBuf remainingBytes = currentMessage.appendFrom(msg);
        if (currentMessage.isComplete()) {
            messageConsumer.accept(currentMessage);
            currentMessage = null;
            append(remainingBytes, messageConsumer);
        } else {
            assert 0 == remainingBytes.readableBytes();
            remainingBytes.release();
        }
    }

    public void shutdown() {
        if (currentMessage != null) {
            currentMessage.release();
        }
    }
}
