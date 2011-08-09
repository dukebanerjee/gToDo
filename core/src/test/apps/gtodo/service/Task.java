package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {
    private final JSONObject encoding;

    public Task(JSONObject encoding) {
        this.encoding = encoding;
    }

    public String getId() {
        try {
            return encoding.getString("id");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public String getName() {
        try {
            return encoding.getString("name");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

}
