package test.apps.gtodo.service;

import org.json.JSONException;
import org.json.JSONObject;

public class NewEntityResult extends Result {
	public NewEntityResult(JSONObject result) {
		super(result);
	}

	public String getNewId() {
		try {
			return result.getString("new_id");
		} catch (JSONException e) {
			throw new UnexpectedResponseException(e);
		}
	}
}
