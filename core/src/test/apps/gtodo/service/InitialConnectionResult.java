package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class InitialConnectionResult {
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
	
	public TaskList getTaskList(int i) {
		try {
			return new TaskList(response.getJSONObject("t").getJSONArray("lists").getJSONObject(i));
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}

	public int getDefaultListTaskCount() {
		try {
			return response.getJSONObject("t").getJSONArray("tasks").length();
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}

	public Task getTask(int i) {
		try {
			return new Task(response.getJSONObject("t").getJSONArray("tasks").getJSONObject(i));
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}
}