package com.deltadna.android.sdk.triggers;

import com.deltadna.android.sdk.EventTriggeredCampaignMetricStore;

public class ExecutionRepeatTriggerCondition extends ExecutionCountBasedTriggerCondition {

    private final long repeatInterval;
    private long repeatTimesLimit;

    public ExecutionRepeatTriggerCondition(long repeatInterval, long repeatTimesLimit, EventTriggeredCampaignMetricStore metricStore, long variantId) {
        super(variantId, metricStore);
        this.repeatInterval = repeatInterval;
        this.repeatTimesLimit = repeatTimesLimit;
    }

    @Override
    public boolean canExecute() {
        long execCount = getCurrentExecutionCount();
        return (execCount % repeatInterval == 0)
                &&
                (!(repeatTimesLimit > 0) || repeatTimesLimit * repeatInterval >= execCount);
    }
}
