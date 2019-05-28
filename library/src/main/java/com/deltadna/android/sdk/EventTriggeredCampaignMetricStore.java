package com.deltadna.android.sdk;

public class EventTriggeredCampaignMetricStore {

    private final DatabaseHelper db;

    EventTriggeredCampaignMetricStore(DatabaseHelper db){
        this.db = db;
    }

    void recordETCExecution(long variantId){
        db.recordETCExecution(variantId);
    }

    public long getETCExecutionCount(long variantId){
        return db.getETCExecutionCount(variantId);
    }

    public void clear(){
        db.clearETCExecutions();
    }


}
