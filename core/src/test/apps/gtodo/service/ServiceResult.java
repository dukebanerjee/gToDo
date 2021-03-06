package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class ServiceResult implements TaskListsResult, TasksResult {
    private final JSONObject response;

    public ServiceResult(JSONObject response) {
        this.response = response;
    }
    
    public boolean hasTaskLists() {
        return response.has("lists");
    }

    public int getTaskListCount() {
        try {
            return response.getJSONArray("lists").length();
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public TaskListResult getTaskList(int i) {
        try {
            return new TaskListResult(response.getJSONArray("lists").getJSONObject(i));
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
    
    public boolean hasTasks() {
        return response.has("tasks");
    }

    public int getTaskCount() {
        try {
            return response.getJSONArray("tasks").length();
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public TaskResult getTask(int i) {
        try {
            return new TaskResult(response.getJSONArray("tasks").getJSONObject(i));
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public boolean hasResult() {
        try {
            return response.has("results") && response.getJSONArray("results").length() > 0;
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public RequestResult getRequestResult() {
        try {
            JSONObject result = response.getJSONArray("results").getJSONObject(0);
            return new RequestResult(result);
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
}
