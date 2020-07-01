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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.deltadna.android.sdk.triggers.ExecutionCountTriggerCondition;
import com.deltadna.android.sdk.triggers.ExecutionRepeatTriggerCondition;
import com.deltadna.android.sdk.triggers.TriggerCondition;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.*;

public final class EventTrigger implements Comparable<EventTrigger> {
    
    private static final String TAG = BuildConfig.LOG_TAG
            + ' '
            + EventTrigger.class.getSimpleName();
    
    private final DDNA ddna;
    private final int index;
    private final List<TriggerCondition> campaignTriggerConditions;
    
    private final String eventName;
    private final EventTriggeredCampaignMetricStore etcMetricStore;
    private final JSONObject response;
    
    private final int priority;
    private final int limit;
    private final Object[] condition;
    
    private final long campaignId;
    private final long variantId;

    @Nullable
    private final String campaignName;
    @Nullable
    private final String variantName;

    private int runs;
    
    EventTrigger(DDNA ddna, int index, JSONObject json, EventTriggeredCampaignMetricStore etcMetricStore) {
        this.ddna = ddna;
        this.index = index;
        
        eventName = json.optString("eventName", "");
        this.etcMetricStore = etcMetricStore;
        final JSONObject response = json.optJSONObject("response");
        this.response = (response != null) ? response : new JSONObject();

        priority = json.optInt("priority", 0);
        limit = json.optInt("limit", -1);
        final JSONArray condition = json.optJSONArray("condition");
        if (condition != null) {
            this.condition = new Object[condition.length()];
            for (int i = 0; i < condition.length(); i++) {
                final JSONObject value = condition.optJSONObject(i);
                
                if (value != null && value.has("o")) {
                    this.condition[i] = Op.valueFromBackend(value.optString("o"));
                } else if (value != null) {
                    this.condition[i] = value.opt(value.keys().next());
                }
            }
        } else {
            this.condition = new Object[0];
        }

        campaignId = json.optInt("campaignID", -1);
        variantId = json.optInt("variantID", -1);
        campaignTriggerConditions = parseShowConditions(json.optJSONObject("campaignExecutionConfig"));

        final JSONObject nullableEventParams = this.response.optJSONObject("eventParams");
        final JSONObject eventParams = nullableEventParams != null ? nullableEventParams : new JSONObject();
        campaignName = eventParams.optString("responseEngagementName", null);
        variantName = eventParams.optString("responseVariantName", null);
    }
    
    String getEventName() {
        return eventName;
    }
    
    String getAction() {
        if (response.has("image")) {
            final JSONObject image = response.optJSONObject("image");
            if (image != null && image.length() > 0) {
                return "imageMessage";
            }
        }
        
        return "gameParameters";
    }
    
    JSONObject getResponse() {
        return response;
    }

    long getCampaignId() {
        return campaignId;
    }

    long getVariantId() {
        return variantId;
    }

    @Nullable
    String getCampaignName() {
        return campaignName;
    }

    @Nullable
    String getVariantName() {
        return variantName;
    }


    boolean evaluate(Event event) {
        if (!event.name.equals(eventName)) return false;
        
        final Deque<Object> stack = new ArrayDeque<>();
        for (final Object token : condition) {
            if (token instanceof Op) {
                final Op op = (Op) token;
                final Object right = stack.pop();
                final Object left = stack.pop();
                
                if (left instanceof String) {
                    final Object leftValue = event.params.json.opt((String) left);
                    
                    try {
                        if (leftValue instanceof Boolean) {
                            stack.push(op.evaluate(
                                    (boolean) leftValue,
                                    (boolean) right));
                        } else if (leftValue instanceof Integer) {
                            stack.push(op.evaluate(
                                    (int) leftValue,
                                    (int) right));
                        } else if (leftValue instanceof Long) {
                            if (right instanceof Integer) {
                                stack.push(op.evaluate(
                                        (long) leftValue,
                                        (int) right));
                            } else {
                                stack.push(op.evaluate(
                                        (long) leftValue,
                                        (long) right));
                            }
                        } else if (leftValue instanceof Float) {
                            // floats are actually double precision in JSON
                            stack.push(op.evaluate(
                                    (double) (float) leftValue,
                                    (double) right));
                        } else if (leftValue instanceof Double) {
                            stack.push(op.evaluate(
                                    (double) leftValue,
                                    (double) right));
                        } else if (leftValue instanceof String) {
                            final Class<?> type = event.params.typeOf((String) left);
                            if (type == Date.class) {
                                stack.push(op.evaluate(
                                        DDNA.TIMESTAMP_FORMAT.parse((String) leftValue),
                                        DDNA.TIMESTAMP_FORMAT_ISO.parse((String) right)));
                            } else if (type == String.class) {
                                stack.push(op.evaluate(
                                        (String) leftValue,
                                        (String) right));
                            }
                        } else if (leftValue == null) {
                            Log.w(TAG, "Failed to find " + left + " in parameters");
                            return false;
                        } else {
                            Log.w(TAG, "Unexpected value " + left + " for left side");
                            return false;
                        }
                    } catch (InvalidOperation e) {
                        Log.w(TAG, e.getMessage());
                        return false;
                    } catch (ClassCastException e) {
                        Log.w(TAG, "Unexpected value " + right + " for right side");
                        return false;
                    } catch (ParseException e) {
                        Log.w(TAG, String.format(
                                Locale.ENGLISH,
                                "Failed parsing %s/%s to a date",
                                leftValue,
                                right));
                        return false;
                    }
                } else if (left instanceof Boolean) {
                    try {
                        stack.push(op.evaluate((Boolean) left, (Boolean) right));
                    } catch (InvalidOperation e) {
                        Log.w(TAG, e.getMessage());
                        return false;
                    } catch (ClassCastException e) {
                        Log.w(TAG, "Unexpected value " + right + " for right side");
                        return false;
                    }
                } else {
                    Log.w(TAG, "Unexpected value " + left + " for left side");
                    return false;
                }
            } else if (token == null) {
                Log.w(TAG, "Null token in condition");
                return false;
            } else {
                stack.push(token);
            }
        }




        if (stack.isEmpty() || (boolean) stack.pop()) {



            // Default to true if no conditions exist
            boolean anyCanExecute = campaignTriggerConditions.size() == 0;

            // Only one condition needs to be true to flip conditions to true
            etcMetricStore.recordETCExecution(variantId);
            for (TriggerCondition condition : campaignTriggerConditions){
                if ( condition.canExecute() ) anyCanExecute = true;
            }

            // If none reached return false
            if (!anyCanExecute) {
                return false;
            }
            if (limit != -1 && runs >= limit) return false;

            runs++;
            ddna.recordEvent(new Event("ddnaEventTriggeredAction")
                    .putParam("ddnaEventTriggeredCampaignID", campaignId)
                    .putParam("ddnaEventTriggeredCampaignPriority", priority)
                    .putParam("ddnaEventTriggeredVariantID", variantId)
                    .putParam("ddnaEventTriggeredActionType", getAction())
                    .putParam("ddnaEventTriggeredCampaignName", getCampaignName())
                    .putParam("ddnaEventTriggeredVariantName", getVariantName())
                    .putParam("ddnaEventTriggeredSessionCount", runs));
            return true;
        } else {
            return false;
        }
    }

    private List<TriggerCondition> parseShowConditions(JSONObject campaignLimitsConfig) {
        List<TriggerCondition> showConditions = new ArrayList<>();
        if (campaignLimitsConfig == null) return showConditions;
        if (campaignLimitsConfig.has("showConditions")) {
            JSONArray showConditionsJson = campaignLimitsConfig.optJSONArray("showConditions");
            for (int i = 0; i < showConditionsJson.length(); i++) {
                try {
                    JSONObject currentCondition = showConditionsJson.getJSONObject(i);
                    if (currentCondition.has("executionsRequiredCount")) {
                        long executionsRequiredCount = currentCondition.optLong("executionsRequiredCount", 0L);
                        showConditions.add(new ExecutionCountTriggerCondition(executionsRequiredCount, etcMetricStore,
                                variantId));
                    }
                    if (currentCondition.has("executionsRepeat")) {
                        long repeatOn = currentCondition.optLong("executionsRepeat", 1L);
                        long repeatTimesLimit = currentCondition.optLong("executionsRepeatLimit", -1L);
                        showConditions.add(new ExecutionRepeatTriggerCondition(repeatOn, repeatTimesLimit, etcMetricStore, variantId));
                    }
                } catch (JSONException ignored) {
                }
            }
        }
        return showConditions;
    }


    @Override
    public int compareTo(@NonNull EventTrigger trigger) {
        final int primary = Integer.compare(priority, trigger.priority) * -1;
        if (primary == 0) {
            return Integer.compare(index, trigger.index);
        } else {
            return primary;
        }
    }



    private enum Op {
        
        AND("and") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                throw new InvalidOperation("Cannot convert compare value for %s", this);
            }
            
            @Override
            boolean evaluate(boolean left, boolean right) {
                return left && right;
            }
            
            @Override
            boolean evaluate(long left, long right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on integers", this);
            }
            
            @Override
            boolean evaluate(double left, double right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on floats", this);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
            
            @Override
            boolean evaluate(Date left, Date right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
        },
        OR("or") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                throw new InvalidOperation("Cannot convert compare value for %s", this);
            }
            
            @Override
            boolean evaluate(boolean left, boolean right) {
                return left || right;
            }
            
            @Override
            boolean evaluate(long left, long right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on integers", this);
            }
            
            @Override
            boolean evaluate(double left, double right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on floats", this);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
            
            @Override
            boolean evaluate(Date left, Date right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
        },
        EQ("equal to") {
            @Override
            boolean fromCompare(int value) {
                return value == 0;
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return left.equals(right);
            }
        },
        EQ_IGNORE_CASE("equal to ic") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                return EQ.fromCompare(value);
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return left.equalsIgnoreCase(right);
            }
        },
        NEQ("not equal to") {
            @Override
            boolean fromCompare(int value) {
                return value != 0;
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return !left.equals(right);
            }
        },
        NEQ_IGNORE_CASE("not equal to ic") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                return NEQ.fromCompare(value);
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return !left.equalsIgnoreCase(right);
            }
        },
        GT("greater than") {
            @Override
            boolean fromCompare(int value) {
                return value > 0;
            }
            
            @Override
            boolean evaluate(boolean left, boolean right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on booleans", this);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
        },
        GTE("greater than eq") {
            @Override
            boolean fromCompare(int value) {
                return value >= 0;
            }
            
            @Override
            boolean evaluate(boolean left, boolean right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on booleans", this);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
        },
        LT("less than") {
            @Override
            boolean fromCompare(int value) {
                return value < 0;
            }
            
            @Override
            boolean evaluate(boolean left, boolean right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on booleans", this);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
        },
        LTE("less than eq") {
            @Override
            boolean fromCompare(int value) {
                return value <= 0;
            }
            
            @Override
            boolean evaluate(boolean left, boolean right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on booleans", this);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                throw new InvalidOperation("Cannot perform %s on strings", this);
            }
        },
        CONTAINS("contains") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                throw new InvalidOperation("Cannot convert compare value for %s", this);
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return left.contains(right);
            }
        },
        CONTAINS_IGNORE_CASE("contains ic") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                return CONTAINS.fromCompare(value);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                return CONTAINS.evaluate(
                        left.toLowerCase(Locale.getDefault()),
                        right.toLowerCase(Locale.getDefault()));
            }
        },
        STARTS_WITH("starts with") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                throw new InvalidOperation("Cannot convert compare value for %s", this);
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return left.startsWith(right);
            }
        },
        STARTS_WITH_IGNORE_CASE("starts with ic") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                return STARTS_WITH.fromCompare(value);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                return STARTS_WITH.evaluate(
                        left.toLowerCase(Locale.getDefault()),
                        right.toLowerCase(Locale.getDefault()));
            }
        },
        ENDS_WITH("ends with") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                throw new InvalidOperation("Cannot convert compare value for %s", this);
            }
            
            @Override
            boolean evaluate(String left, String right) {
                return left.endsWith(right);
            }
        },
        ENDS_WITH_IGNORE_CASE("ends with ic") {
            @Override
            boolean fromCompare(int value) throws InvalidOperation {
                return ENDS_WITH.fromCompare(value);
            }
            
            @Override
            boolean evaluate(String left, String right) throws InvalidOperation {
                return ENDS_WITH.evaluate(
                        left.toLowerCase(Locale.getDefault()),
                        right.toLowerCase(Locale.getDefault()));
            }
        };
        
        private final String value;
        
        Op(String value) {
            this.value = value;
        }
        
        boolean evaluate(boolean left, boolean right) throws InvalidOperation {
            return fromCompare(Boolean.compare(left, right));
        }
        
        boolean evaluate(long left, long right) throws InvalidOperation {
            return fromCompare(Long.compare(left, right));
        }
        
        boolean evaluate(double left, double right) throws InvalidOperation {
            return fromCompare(Double.compare(left, right));
        }
        
        boolean evaluate(String left, String right) throws InvalidOperation {
            throw new InvalidOperation("Cannot perform %s on strings", this);
        }
        
        boolean evaluate(Date left, Date right) throws InvalidOperation {
            return fromCompare(left.compareTo(right));
        }
        
        abstract boolean fromCompare(int value) throws InvalidOperation;
        
        @Nullable
        static Op valueFromBackend(String value) {
            for (final Op op : values()) {
                if (op.value.equalsIgnoreCase(value)) return op;
            }
            
            return null;
        }
    }
    
    private static final class InvalidOperation extends Exception {
        
        InvalidOperation(String formatMessage, Op op) {
            super(String.format(Locale.ENGLISH, formatMessage, op));
        }
    }
}
