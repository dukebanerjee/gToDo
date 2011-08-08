package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class ServiceResult {
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
}
