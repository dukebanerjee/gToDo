package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class ServiceResult implements TaskListsResult, TasksResult {
	private final JSONObject response;

	public ServiceResult(JSONObject response) {
		this.response = response;
	}

	public int getTaskListCount() {
		try {
			return response.getJSONArray("lists").length();
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}

	public TaskList getTaskList(int i) {
		try {
			return new TaskList(response.getJSONArray("lists").getJSONObject(i));
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}
	
	public int getTaskCount() {
		try {
			return response.getJSONArray("tasks").length();
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}

	public Task getTask(int i) {
		try {
			return new Task(response.getJSONArray("tasks").getJSONObject(i));
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}
}
