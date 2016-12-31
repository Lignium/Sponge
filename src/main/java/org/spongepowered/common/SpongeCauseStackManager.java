/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SpongeCauseStackManager implements CauseStackManager {

    public static final boolean DEBUG_CAUSE_FRAMES = Boolean.valueOf(System.getProperty("sponge.debugcauseframes", "false"));

    public static final SpongeCauseStackManager instance = new SpongeCauseStackManager();

    private final Deque<Object> cause = Queues.newArrayDeque();
    private final Deque<CauseStackFrame> frames = Queues.newArrayDeque();
    private Map<String, Object> ctx = Maps.newHashMap();

    private int min_depth = 0;
    private Cause cached_cause;
    private EventContext cached_ctx;

    private SpongeCauseStackManager() {

    }

    @Override
    public Cause getCurrentCause() {
        if (this.cached_cause == null || this.cached_ctx == null) {
            if(this.cause.isEmpty()) {
                this.cached_cause = Cause.of(getCurrentContext(), SpongeImpl.getGame());
            } else {
                this.cached_cause = Cause.of(getCurrentContext(), this.cause);
            }
        }
        return this.cached_cause;
    }

    @Override
    public EventContext getCurrentContext() {
        if (this.cached_ctx == null) {
            this.cached_ctx = EventContext.of(this.ctx);
        }
        return this.cached_ctx;
    }

    @Override
    public CauseStackManager pushCause(Object obj) {
        this.cached_cause = null;
        this.cause.push(obj);
        return this;
    }

    @Override
    public Object popCause() {
        if (this.cause.size() == this.min_depth) {
            throw new IllegalStateException("Cause stack corruption, tried to pop more objects off than were pushed since last frame.");
        }
        this.cached_cause = null;
        return this.cause.pop();
    }

    @Override
    public void popCauses(int n) {
        for (int i = 0; i < n; i++) {
            popCause();
        }
    }

    @Override
    public Object peekCause() {
        return this.cause.peek();
    }

    @Override
    public CauseStackFrame pushCauseFrame() {
        CauseStackFrame frame = new CauseStackFrame(this.min_depth);
        this.frames.push(frame);
        this.min_depth = this.cause.size();
        if (DEBUG_CAUSE_FRAMES) {
            // Attach an exception to the frame so that if there is any frame
            // corruption we can print out the stack trace of when the frames
            // were created.
            frame.stack_debug = new Exception();
        }
        return frame;
    }

    @Override
    public void popCauseFrame(Object oldFrame) {
        CauseStackFrame frame = this.frames.peek();
        if (frame != oldFrame) {
            // If the given frame is not the top frame then some form of
            // corruption of the stack has occured and we do our best to correct
            // it.

            // If the target frame is still in the stack then we can pop frames
            // off the stack until we reach it, otherwise we have no choice but
            // to simply throw an error.
            int offset = -1;
            int i = 0;
            for (CauseStackFrame f : this.frames) {
                if (f == oldFrame) {
                    offset = i;
                    break;
                }
                i++;
            }
            if (offset == -1) {
                throw new IllegalStateException("Cause stack frame corruption! Attempted to pop a frame which was not on the frame stack.");
            }
            SpongeImpl.getLogger().warn("Cause stack frame corruption! Found " + (offset + 1) + " frames left on the stack. Clearing them all.");
            if (!DEBUG_CAUSE_FRAMES) {
                SpongeImpl.getLogger().warn("  Add -Dsponge.debugcauseframes to your startup flags to enable further debugging output.");
            } else {
                SpongeImpl.getLogger().warn("Attempted to pop frame:");
                frame.stack_debug.printStackTrace();
                SpongeImpl.getLogger().warn("Frames being popped are:");
            }
            while (offset >= 0) {
                CauseStackFrame f = this.frames.peek();
                if (DEBUG_CAUSE_FRAMES && offset > 0) {
                    SpongeImpl.getLogger().warn("Stack frame in position " + offset + ":");
                    f.stack_debug.printStackTrace();
                }
                popCauseFrame(f);
                offset--;
            }
            return;
        }
        this.frames.pop();
        // Remove new values
        boolean ctx_invalid = false;
        if (frame.hasNew()) {
            for (String key : frame.getNew()) {
                this.ctx.remove(key);
            }
            ctx_invalid = true;
        }
        // Restore old values
        if (frame.hasStoredValues()) {
            for (Map.Entry<String, Object> e : frame.getStoredValues()) {
                this.ctx.put(e.getKey(), e.getValue());
            }
            ctx_invalid = true;
        }
        if (ctx_invalid) {
            this.cached_ctx = null;
        }
        this.min_depth = frame.old_min_depth;
        // If there were any objects left on the stack then we pop them off
        while (this.cause.size() > this.min_depth) {
            this.cause.pop();
        }
    }

    @Override
    public AutoCloseable createCauseFrame() {
        return pushCauseFrame();
    }

    @Override
    public CauseStackManager addContext(String key, Object value) {
        this.cached_ctx = null;
        Object existing = this.ctx.put(key, value);
        if (!this.frames.isEmpty()) {
            CauseStackFrame frame = this.frames.peek();
            if (existing == null) {
                frame.markNew(key);
            } else if (!frame.isNew(key) && !frame.isStored(key)) {
                frame.store(key, existing);
            }
        }
        return this;
    }

    @Override
    public Optional<?> getContext(String key) {
        return Optional.ofNullable(this.ctx.get(key));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getContext(String key, Class<T> expectedType) {
        Object obj = this.ctx.get(key);
        if (expectedType.isInstance(obj)) {
            return Optional.of((T) obj);
        }
        return Optional.empty();
    }

    @Override
    public Optional<?> clearContext(String key) {
        this.cached_ctx = null;
        Object existing = this.ctx.remove(key);
        if (existing != null && !this.frames.isEmpty()) {
            CauseStackFrame frame = this.frames.peek();
            if (!frame.isNew(key)) {
                frame.store(key, existing);
            }
        }
        return Optional.ofNullable(existing);
    }

    // TODO could pool these for more fasts
    public static class CauseStackFrame implements AutoCloseable {

        // lazy loaded
        private Map<String, Object> stored_ctx_values;
        private Set<String> new_ctx_values;
        public int old_min_depth;

        public Exception stack_debug = null;

        public CauseStackFrame(int old_depth) {
            this.old_min_depth = old_depth;
        }

        public boolean isStored(String key) {
            return this.stored_ctx_values != null && this.stored_ctx_values.containsKey(key);
        }

        public Set<Map.Entry<String, Object>> getStoredValues() {
            return this.stored_ctx_values.entrySet();
        }

        public boolean hasStoredValues() {
            return this.stored_ctx_values != null && !this.stored_ctx_values.isEmpty();
        }

        public void store(String key, Object existing) {
            if (this.stored_ctx_values == null) {
                this.stored_ctx_values = new HashMap<>();
            }
            this.stored_ctx_values.put(key, existing);
        }

        public boolean isNew(String key) {
            return this.new_ctx_values != null && this.new_ctx_values.contains(key);
        }

        public Set<String> getNew() {
            return this.new_ctx_values;
        }

        public boolean hasNew() {
            return this.new_ctx_values != null && !this.new_ctx_values.isEmpty();
        }

        public void markNew(String key) {
            if (this.new_ctx_values == null) {
                this.new_ctx_values = new HashSet<>();
            }
            this.new_ctx_values.add(key);
        }

        @Override
        public void close() {
            SpongeCauseStackManager.instance.popCauseFrame(this);
        }

    }

}
