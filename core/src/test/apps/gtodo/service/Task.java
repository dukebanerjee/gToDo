package test.apps.gtodo.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {
    public final JSONObject encoding;
    public final JSONObject entityDelta;

    private SimpleDateFormat taskDateFormat = new SimpleDateFormat("yyyyMMdd");

    public Task(JSONObject encoding) {
        this.encoding = encoding;
        this.entityDelta = new JSONObject();
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
    
    public boolean getCompleted() {
        try {
            return encoding.getBoolean("completed");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
    
    public Date getDueDate() {
        try {
            return encoding.has("task_date") ? taskDateFormat.parse(encoding.getString("task_date")) : null;
        } catch (Exception e) {
            throw new UnexpectedResponseException(e);
        }
    }
    
    public String getNotes() {
        try {
            return encoding.has("notes") ? encoding.getString("notes") : "";
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public void setName(String name) {
        try {
            entityDelta.put("name", name);
        } catch (JSONException e) {
            throw new IllegalStateException("Impossible");
        }
    }

    public void setCompleted(boolean completed) {
        try {
            entityDelta.put("completed", completed);
        } catch (JSONException e) {
            throw new IllegalStateException("Impossible");
        }
    }

    public void setDueDate(Date dueDate) {
        try {
            entityDelta.put("task_date", dueDate == null ? "" : taskDateFormat.format(dueDate));
        } catch (JSONException e) {
            throw new IllegalStateException("Impossible");
        }
    }

    public void setNotes(String contents) {
        try {
            entityDelta.put("notes", contents);
        } catch (JSONException e) {
            throw new IllegalStateException("Impossible");
        }
    }
}
