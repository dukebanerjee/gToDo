package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskListResult {
    private final JSONObject result;

    public TaskListResult(JSONObject result) {
        this.result = result;
    }

    public String getId() {
        try {
            return result.getString("id");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public String getName() {
        try {
            return result.getString("name");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public long getLastModified() {
        try {
            return result.getLong("last_modified");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
}
