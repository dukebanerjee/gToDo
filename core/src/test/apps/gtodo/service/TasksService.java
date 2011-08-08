package test.apps.gtodo.service;


import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.json.JSONException;
import org.json.JSONObject;

public class TasksService {
	public static final String INITIAL_SERVICE_URL = "https://mail.google.com/tasks/ig";
	private final HttpClient client;
	private BasicHttpContext context;

	public TasksService(HttpClient client, String authToken) {
		this.client = client;
		context = new BasicHttpContext();
		
		BasicCookieStore cookieStore = new BasicCookieStore();
		context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		cookieStore.addCookie(createPersistentAuthCookie(authToken));
	}

	private BasicClientCookie createPersistentAuthCookie(String authToken) {
		BasicClientCookie cookie = new BasicClientCookie("GTL", authToken);
		cookie.setDomain("mail.google.com");
		cookie.setPath("/tasks");
		cookie.setExpiryDate(null);
		cookie.setSecure(true);
		return cookie;
	}

	public InitialConnectionResult connect() throws IOException {
		HttpResponse response = client.execute(new HttpGet(INITIAL_SERVICE_URL), context);
		if(response.getStatusLine().getStatusCode() != 200) {
			throw new IOException("Unexpected HTTP response code from service: " + response.getStatusLine().getStatusCode());
		}
		
		String responseContent = HttpClientUtils.readEntityAsString(response.getEntity());
		Matcher m = Pattern.compile("_setup\\((.*)\\)").matcher(responseContent);
		if(m.find()) {
			try {
				return new InitialConnectionResult(new JSONObject(m.group(1)));
			} catch (JSONException e) {
				// Fall through
			}
		}
		throw new IOException("Unexpected response from service: " + responseContent);
	}
}
