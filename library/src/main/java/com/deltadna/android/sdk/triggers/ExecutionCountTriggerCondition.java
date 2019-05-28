package com.deltadna.android.sdk.triggers;

import com.deltadna.android.sdk.EventTriggeredCampaignMetricStore;

public class ExecutionCountTriggerCondition extends ExecutionCountBasedTriggerCondition {

    private final long executionsRequired;

    public ExecutionCountTriggerCondition(long executionsRequired, EventTriggeredCampaignMetricStore metricStore, long variantId) {
        super(variantId, metricStore);
        this.executionsRequired = executionsRequired;
    }

    public boolean canExecute() {
        return executionsRequired == getCurrentExecutionCount();
    }
}