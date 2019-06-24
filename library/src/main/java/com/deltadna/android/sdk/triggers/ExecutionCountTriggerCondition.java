package com.deltadna.android.sdk.triggers;

import com.deltadna.android.sdk.EventTriggeredCampaignMetricStore;

public class ExecutionCountTriggerCondition extends ExecutionCountBasedTriggerCondition {

    private final long executionsRequiredCount;

    public ExecutionCountTriggerCondition(long executionsRequiredCount, EventTriggeredCampaignMetricStore metricStore, long variantId) {
        super(variantId, metricStore);
        this.executionsRequiredCount = executionsRequiredCount;
    }

    public boolean canExecute() {
        return executionsRequiredCount == getCurrentExecutionCount();
    }
}