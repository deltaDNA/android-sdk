package com.deltadna.android.sdk.consent;

import android.content.SharedPreferences;

public class ConsentTracker {
    private final SharedPreferences sharedPreferencesInstance;
    private final GeoIpNetworkClient geoIpNetworkClient;

    private final static String useKey = "ddnaPiplUseConsent";
    private final static String exportKey = "ddnaPiplExportConsent";

    public ConsentStatus useConsentStatus;
    public ConsentStatus exportConsentStatus;

    public interface Callback {
        void onSuccess(boolean requiresConsent);
        void onFailure(Throwable exception);
    }

    public ConsentTracker(SharedPreferences sharedPreferences, GeoIpNetworkClient geoIpNetworkClient) {
        this.sharedPreferencesInstance = sharedPreferences;
        this.geoIpNetworkClient = geoIpNetworkClient;

        String useConsent = sharedPreferences.getString(useKey, ConsentStatus.unknown.name());
        useConsentStatus = ConsentStatus.valueOf(useConsent);
        String exportConsent = sharedPreferences.getString(exportKey, ConsentStatus.unknown.name());
        exportConsentStatus = ConsentStatus.valueOf(exportConsent);
    }

    public boolean hasCheckedForConsent() {
        return isACheckedStatus(useConsentStatus) && isACheckedStatus(exportConsentStatus);
    }

    public boolean isConsentDenied() {
        return exportConsentStatus == ConsentStatus.consentDenied || useConsentStatus == ConsentStatus.consentDenied;
    }

    public void isPiplConsentRequired(Callback callback) {
        if (hasCheckedForConsent()) {
            callback.onSuccess(false);
            return;
        }
        geoIpNetworkClient.fetchGeoIpResponse(new GeoIpNetworkClient.Callback() {
            @Override
            public void OnSuccess(String consentIdentifier) {
                boolean isConsentNeeded = consentIdentifier.equals("pipl");
                ConsentStatus status = isConsentNeeded ? ConsentStatus.requiredButUnchecked : ConsentStatus.notRequired;
                useConsentStatus = status;
                exportConsentStatus = status;
                callback.onSuccess(isConsentNeeded);
            }

            @Override
            public void OnFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    public void setConsents(boolean useConsent, boolean exportConsent) {
        useConsentStatus = useConsent ? ConsentStatus.consentGiven : ConsentStatus.consentDenied;
        exportConsentStatus = exportConsent ? ConsentStatus.consentGiven : ConsentStatus.consentDenied;

        sharedPreferencesInstance.edit()
                .putString(useConsentStatus.name(), useKey)
                .putString(exportConsentStatus.name(), exportKey)
                .apply();
    }

    public boolean allConsentsAreMet() {
        return (useConsentStatus == ConsentStatus.consentGiven || useConsentStatus == ConsentStatus.notRequired)
            && (exportConsentStatus == ConsentStatus.consentGiven || exportConsentStatus == ConsentStatus.notRequired);
    }

    private boolean isACheckedStatus(ConsentStatus status) {
        return status == ConsentStatus.consentDenied || status == ConsentStatus.consentGiven || status == ConsentStatus.notRequired;
    }
}
