package test.apps.gtodo.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskResult {
    private final SimpleDateFormat taskDateFormat = new SimpleDateFormat("yyyyMMdd");
    private final JSONObject result;
    
    public TaskResult(JSONObject result) {
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
    
    public boolean getCompleted() {
        try {
            return result.getBoolean("completed");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
    
    public Date getDueDate() {
        try {
            return result.has("task_date") ? taskDateFormat.parse(result.getString("task_date")) : null;
        } catch (Exception e) {
            throw new UnexpectedResponseException(e);
        }
    }
    
    public String getNotes() {
        try {
            return result.has("notes") ? result.getString("notes") : "";
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
}
