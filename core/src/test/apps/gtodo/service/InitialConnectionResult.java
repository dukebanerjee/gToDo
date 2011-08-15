package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class InitialConnectionResult implements TaskListsResult, TasksResult {
    public final JSONObject response;

    public InitialConnectionResult(JSONObject response) {
        this.response = response;
    }

    public long getClientVersion() {
        try {
            return response.getLong("v");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public long getLatestSyncPoint() {
        try {
            return response.getJSONObject("t").getLong("latest_sync_point");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public int getTaskListCount() {
        try {
            return response.getJSONObject("t").getJSONArray("lists").length();
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public TaskListResult getTaskList(int i) {
        try {
            return new TaskListResult(response.getJSONObject("t").getJSONArray("lists").getJSONObject(i));
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public int getTaskCount() {
        try {
            return response.getJSONObject("t").getJSONArray("tasks").length();
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public TaskResult getTask(int i) {
        try {
            return new TaskResult(response.getJSONObject("t").getJSONArray("tasks").getJSONObject(i));
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }

    public String getDefaultListId() {
        try {
            return response.getJSONObject("t").getJSONObject("user").getString("default_list_id");
        } catch (JSONException e) {
            throw new UnexpectedResponseException(e);
        }
    }
}