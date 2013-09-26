/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.cindy.util;

import java.nio.ByteBuffer;

/**
 * ByteBuffer Utils.
 * 
 * @author Roger Chen
 */
public class ByteBufferUtils {

    private ByteBufferUtils() {
    }

    /**
     * Allocate ByteBuffer.
     * 
     * @param capacity
     * 		ByteBuffer capacity
     * @param direct
     * 		allocate DirectByteBuffer
     * @return
     * 		allocated ByteBuffer
     * @throws IllegalArgumentException
     * 		if capacity is negative
     */
    public static ByteBuffer allocate(int capacity, boolean direct)
            throws IllegalArgumentException {
        if (capacity < 0)
            throw new IllegalArgumentException("capacity can't be negative");
        return direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer
                .allocate(capacity);
    }

    /**
     * Increase ByteBuffer's capacity.
     * 
     * @param buffer
     * 		the ByteBuffer want to increase capacity
     * @param size
     * 		increased size
     * @return
     *		increased capacity ByteBuffer
     * @throws IllegalArgumentException
     * 		if size less than 0 or buffer is null
     */
    public static ByteBuffer increaseCapacity(ByteBuffer buffer, int size)
            throws IllegalArgumentException {
        if (buffer == null)
            throw new IllegalArgumentException("buffer is null");
        if (size < 0)
            throw new IllegalArgumentException("size less than 0");

        int capacity = buffer.capacity() + size;
        ByteBuffer result = allocate(capacity, buffer.isDirect());
        buffer.flip();
        result.put(buffer);
        return result;
    }

    /**
     * Gather ByteBuffers to one ByteBuffer.
     * 
     * @param buffers
     * 		ByteBuffers
     * @return
     * 		the gather ByteBuffer
     */
    public static ByteBuffer gather(ByteBuffer[] buffers) {
        int remaining = 0;
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null)
                remaining += buffers[i].remaining();
        }
        ByteBuffer result = ByteBuffer.allocate(remaining);
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null)
                result.put(buffers[i]);
        }
        result.flip();
        return result;
    }

    /**
     * Judge ByteBuffers have remaining bytes.
     * 
     * @param buffers
     * 		ByteBuffers
     * @return
     * 		have remaining
     */
    public static boolean hasRemaining(ByteBuffer[] buffers) {
        if (buffers == null)
            return false;
        for (int i = 0; i < buffers.length; i++) {
            if (buffers[i] != null && buffers[i].hasRemaining())
                return true;
        }
        return false;
    }

    /**
     * Returns the index within this buffer of the first occurrence of the
     * specified pattern buffer.
     * 
     * @param buffer
     * 		the buffer
     * @param pattern
     * 		the pattern buffer
     * @return
     * 		the position within the buffer of the first occurrence of the 
     * pattern buffer
     */
    public static int indexOf(ByteBuffer buffer, ByteBuffer pattern) {
        int patternPos = pattern.position();
        int patternLen = pattern.remaining();
        int lastIndex = buffer.limit() - patternLen + 1;

        Label: for (int i = buffer.position(); i < lastIndex; i++) {
            for (int j = 0; j < patternLen; j++) {
                if (buffer.get(i + j) != pattern.get(patternPos + j))
                    continue Label;
            }
            return i;
        }
        return -1;
    }

}