package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestResult {
    public final JSONObject result;

    public RequestResult(JSONObject result) {
        this.result = result;
    }

    public boolean isNewEntity() {
        try {
            return "new_entity".equals(result.getString("result_type"));
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
    
    public String getNewId() {
        try {
            return result.getString("new_id");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
}
