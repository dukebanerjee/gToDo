package test.apps.gtodo.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskRequest {
    private final SimpleDateFormat taskDateFormat = new SimpleDateFormat("yyyyMMdd");
    public final JSONObject entityDelta = new JSONObject();

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
