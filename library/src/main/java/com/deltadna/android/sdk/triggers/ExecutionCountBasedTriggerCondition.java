package com.deltadna.android.sdk.triggers;

import com.deltadna.android.sdk.EventTriggeredCampaignMetricStore;

abstract class ExecutionCountBasedTriggerCondition implements TriggerCondition {

    private long variantId;
    private EventTriggeredCampaignMetricStore metricStore;

     ExecutionCountBasedTriggerCondition(long variantId, EventTriggeredCampaignMetricStore metricStore){
        this.variantId = variantId;
        this.metricStore = metricStore;
    }

    long getCurrentExecutionCount(){
        return metricStore.getETCExecutionCount(variantId);
    }

}
