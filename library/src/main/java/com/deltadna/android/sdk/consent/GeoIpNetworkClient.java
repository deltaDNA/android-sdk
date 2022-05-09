package com.deltadna.android.sdk.consent;

import com.deltadna.android.sdk.listeners.RequestListener;
import com.deltadna.android.sdk.net.NetworkManager;
import com.deltadna.android.sdk.net.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class GeoIpNetworkClient {
    static final String geoIpUrl = "https://pls.prd.mz.internal.unity3d.com/api/v1/user-lookup";

    private final NetworkManager networkManager;

    public GeoIpNetworkClient(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    // Note: In the other SDKs, we deserialize the whole response and return it from the network client.
    // However, due to the JSON library in use in this SDK, and the structure of our main network manager,
    // it is not trivial to do this, so as we only use the identifier anyway and that is easy to extract,
    // we just use that instead.
    interface Callback {
        void OnSuccess(String consentIdentifier);
        void OnFailure(Throwable exception);
    }

    public void fetchGeoIpResponse(Callback callback) {
        networkManager.get(geoIpUrl, new RequestListener<JSONObject>() {
            @Override
            public void onCompleted(Response<JSONObject> response) {
                try {
                    if (response == null) {
                        callback.OnFailure(new Exception("GeoIP returned a null response, please try again."));
                        return;
                    }
                    if (!response.isSuccessful()) {
                        String errorMessage = "GeoIP request failed with error code " + response.code;
                        if (response.error != null) {
                            errorMessage += " " + response.error;
                        }
                        callback.OnFailure(new Exception(errorMessage));
                        return;
                    }
                    if (response.body == null) {
                        callback.OnFailure(new Exception("GeoIP returned a null body but a success code, this may indicate an unexpected response. Please try again."));
                        return;
                    }
                    String identifier = response.body.getString("identifier");
                    callback.OnSuccess(identifier);
                } catch (JSONException e) {
                    callback.OnFailure(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                callback.OnFailure(t);
            }
        });
    }
}
