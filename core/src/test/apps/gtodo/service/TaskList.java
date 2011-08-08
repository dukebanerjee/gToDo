package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class TaskList {
	private final JSONObject encoding;

	public TaskList(JSONObject encoding) {
		this.encoding = encoding;
	}

	public String getName() {
		try {
			return encoding.getString("name");
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}
}
