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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.buffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static java.util.stream.Collectors.toList;

public abstract class AbstractChunkedMessage {
    protected static final int NO_MORE_CHUNKS = -1;
    private final Logger LOG = LoggerFactory.getLogger(AbstractChunkedMessage.class);

    private final List<ByteBuf> chunks = new ArrayList<>();
    private boolean complete;

    public AbstractChunkedMessage(final int initialChunkSize) {
        chunks.add(buffer(initialChunkSize, initialChunkSize));
    }

    public AbstractChunkedMessage(final List<ByteBuf> chunks) {
        this.chunks.addAll(chunks);
        complete = true;
    }

    /**
     * Appends bytes to this message. If not all bytes are required to complete the message, the returned buffer
     * contains the remaining bytes after its current readable index.
     * <p>
     * Must not be called when {@link #isComplete()} returns {@code true}.
     *
     * @param buffer the buffer to read the message from.
     * @return the buffer with the remaining bytes. Reference ownership belongs to the caller who thus is
     * responsible for releasing it.
     */
    public ByteBuf appendFrom(final ByteBuf buffer) {
        if (complete) {
            throw new IllegalStateException("Cannot append to already completed message");
        }
        final ByteBuf currentChunk = currentChunk();
        final int bytesToCopy = Math.min(buffer.readableBytes(), currentChunk.writableBytes());
        LOG.trace("Available bytes: {}", buffer.readableBytes());
        LOG.trace("Required bytes for chunk {}: {}", chunks.size() - 1, currentChunk.writableBytes());
        currentChunk.writeBytes(buffer, bytesToCopy);
        if (currentChunk.writableBytes() > 0) {
            LOG.trace(
                    "{} bytes remaining to complete chunk {}. Deferring for next buffer.",
                    currentChunk.writableBytes(),
                    chunks.size() - 1
            );
        } else {
            final int nextChunkSize = nextChunkSize(chunks);
            LOG.trace("Chunk {} complete. Next chunk size: {}", chunks.size() - 1, nextChunkSize);
            if (nextChunkSize != NO_MORE_CHUNKS) {
                chunks.add(buffer(nextChunkSize, nextChunkSize));
                return appendFrom(buffer);
            } else {
                LOG.trace("All {} chunks read. Message complete.", chunks.size());
                complete = true;
            }
        }
        return buffer;
    }

    protected abstract int nextChunkSize(final List<ByteBuf> chunks);

    private ByteBuf currentChunk() {
        return chunks.get(chunks.size() - 1);
    }

    public boolean isComplete() {
        return complete;
    }

    /**
     * Releases all buffers associated to the message.
     * <p>
     * This message may only be called is reference count ownership of the buffers has not been transferred to
     * some other owner, yet.
     */
    public void release() {
        chunks.forEach(ByteBuf::release);
    }

    public List<ByteBuf> getChunks() {
        if (!isComplete()) {
            throw new IllegalStateException("Message is not complete.");
        }
        return chunks;
    }

    public byte[] getChunkAsArray(final int index) {
        final ByteBuf chunk = getChunks().get(index);
        final byte[] buffer = new byte[chunk.readableBytes()];
        chunk.readBytes(buffer, 0, buffer.length);
        chunk.resetReaderIndex();
        return buffer;
    }

    /**
     * Returns all data of the message.
     * <p>
     * Can only be called when {@link #isComplete()} returns {@code true}.
     *
     * @return A buffer that contains the entire message. Reference count ownership is transferred to the returned
     * buffer. The caller is responsible of releasing it.
     */
    public ByteBuf all() {
        if (!isComplete()) {
            throw new IllegalStateException("Message is not complete.");
        }

        return wrappedBuffer(chunks.toArray(new ByteBuf[]{}));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "readableBytes=" + chunks.stream().map(ByteBuf::readableBytes).reduce(Integer::sum) +
                ", chunks=" + chunks.stream().map(ByteBuf::readableBytes).collect(toList()) +
                ", complete=" + complete +
                '}';
    }
}
