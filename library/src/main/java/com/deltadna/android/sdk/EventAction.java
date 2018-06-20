/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
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
 */

package com.deltadna.android.sdk;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An action associated with an event on which {@link EventActionHandler}s can
 * be registered for handling actions triggered as a result of the event having
 * been recorded.
 * <p>
 * The handlers are registered through
 * {@link #add(EventActionHandler)} and they can be evaluated by calling
 * {@link #run()}. The evaluation happens locally, as such it is instantaneous.
 */
public class EventAction {
    
    static final EventAction EMPTY = new EventAction(
            new Event("noop"),
            Collections.unmodifiableSortedSet(new TreeSet<>())) {
        
        @Override
        public EventAction add(EventActionHandler handler) {
            return this;
        }
        
        @Override
        public void run() {}
    };
    
    private final Event event;
    private final SortedSet<EventTrigger> triggers;
    
    private final Set<EventActionHandler> handlers = new LinkedHashSet<>();
    
    EventAction(Event event, SortedSet<EventTrigger> triggers) {
        this.event = event;
        this.triggers = triggers;
    }
    
    /**
     * Register a handler to handle the parametrised action.
     *
     * @param handler the handler to register
     *
     * @return this {@link EventAction} instance
     */
    public EventAction add(EventActionHandler<?> handler) {
        handlers.add(handler);
        return this;
    }
    
    /**
     * Evaluates the registered handlers against the event and triggers
     * associated for the event.
     */
    public void run() {
        for (final EventTrigger trigger : triggers) {
            if (trigger.evaluate(event)) {
                for (final EventActionHandler handler : handlers) {
                    if (handler.handle(trigger)) return;
                }
            }
        }
    }
}
