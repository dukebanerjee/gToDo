package test.apps.core;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HelloWorldTest {
	private static final String INITIAL_SERVICE_URL = "https://mail.google.com/tasks/ig";
	private static final String SERVICE_URL = "https://mail.google.com/tasks/r/ig";
	
	private static final String username = "dbanerje1979@gmail.com";
	private static final String password = "xDuB324x";

	private DefaultHttpClient client;
	private String authToken;
	private int actionId;
	private int clientVersion;
	private long latestSyncPoint;
	private JSONArray taskLists;
	private String currentListId;
	
	public HelloWorldTest() throws IOException {
		client = new DefaultHttpClient();
		readAuthToken();
		initializeService();
	}

	public static void main(String[] args) throws Exception {
		HelloWorldTest test1 = new HelloWorldTest();
		HelloWorldTest test2 = new HelloWorldTest();
		
		JSONObject request;
		JSONObject response;

		request = new JSONObject().put("action_type", "create")
								  .put("action_id", test1.nextActionId())
                                  .put("index", test1.taskLists.length())
                                  .put("entity_delta", 
                                		  new JSONObject().put("name", "Test")
                                		                  .put("entity_type", "GROUP"));
		response = test1.executeRequest(request);
		test1.currentListId = response.getJSONArray("results").getJSONObject(0).getString("new_id");
		System.out.println(response);

		request = new JSONObject().put("action_type", "get_all")
				                  .put("action_id", test1.nextActionId())
								  .put("list_id", test1.currentListId)
								  .put("get_deleted", Boolean.FALSE);
		System.out.println(test1.executeRequest(request));
//		
//		request = new JSONObject().put("action_type", "CREATE")
//                                  .put("action_id", test1.nextActionId())
//                                  .put("index", 1)
//                                  .put("entity_delta", 
//                                		  new JSONObject().put("name", "Bar")
//                                		                  .put("entity_type", "TASK"))
//				                  .put("parent_id", "06357666721862918015:8:0")
//				                  .put("dest_parent_type", "GROUP")
//				                  .put("list_id", "06357666721862918015:8:0");
//		System.out.println(test1.executeRequest(request));
//
//		request = new JSONObject().put("action_type", "CREATE")
//                .put("action_id", test2.nextActionId())
//                .put("index", 1)
//                .put("entity_delta", 
//              		  new JSONObject().put("name", "Joe")
//              		                  .put("entity_type", "TASK"))
//                .put("parent_id", "06357666721862918015:8:0")
//                .put("dest_parent_type", "GROUP")
//                .put("list_id", "06357666721862918015:8:0");
//		System.out.println(test2.executeRequest(request));
	}

	private int nextActionId() {
		return actionId++;
	}

	private JSONObject executeRequest(JSONObject request) throws IOException {
		HttpPost serviceRequest = new HttpPost(SERVICE_URL);
		JSONObject jsonRequest = null;
		try {
			serviceRequest.addHeader("AT", "1");
			jsonRequest = new JSONObject().put("action_list", new JSONArray(Arrays.asList(request)))
 										  .put("client_version", clientVersion)
 										  .put("latest_sync_point", latestSyncPoint)
			                              .put("current_list_id", currentListId);
		} catch (JSONException e) {
			throw new IllegalStateException("Not possible");
		}
		
		try {
			String jsonRequestValue = jsonRequest.toString();
			System.out.println("Request: " + jsonRequestValue);
			serviceRequest.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair("r", jsonRequestValue))));
			HttpResponse httpResponse = client.execute(serviceRequest);
			return new JSONObject(readEntityAsString(httpResponse.getEntity()));
		} catch (JSONException e) {
			throw new IOException("Unexpected response from service");
		}
	}

	private void initializeService() throws IOException {
		try {
			JSONObject initResponse = readInitialServiceResponse(client);
			System.out.println(initResponse);
			clientVersion = initResponse.getInt("v");
			latestSyncPoint = initResponse.getJSONObject("t").getLong("latest_sync_point");
			taskLists = initResponse.getJSONObject("t").getJSONArray("lists");
			currentListId = initResponse.getJSONObject("t").getJSONObject("user").getString("default_list_id");
			actionId = 1;
		} catch (JSONException e) {
			throw new IOException("Unexpected response from service");
		}
	}

	private static JSONObject readInitialServiceResponse(DefaultHttpClient client) throws IOException {
		HttpResponse response = client.execute(new HttpGet(INITIAL_SERVICE_URL));
		Matcher m = Pattern.compile("_setup\\((.*)\\)").matcher(readEntityAsString(response.getEntity()));
		if(m.find()) {
			try {
				return new JSONObject(m.group(1));
			} catch (JSONException e) {
				// Fall through
			}
		}
		throw new IOException("Unexpected response from service");
	}

	private void readAuthToken() throws IOException {
		HttpResponse response = client.execute(new HttpGet(INITIAL_SERVICE_URL));
		System.out.println(response.getStatusLine());
		Pattern openTag = Pattern.compile("<(/?[A-Za-z]+)");
		Pattern attr = Pattern.compile("/?>|([A-Za-z]+) *= *[\"']?([\\S&&[^\"']]+)[\"']?");
		Matcher m = openTag.matcher(readEntityAsString(response.getEntity()));
		HttpPost loginRequest = null;
		List<NameValuePair> formValues = null;
		while(m.find()) {
			String tag = m.group(1);
			Map<String, String> attributes = new HashMap<String, String>();
			m.usePattern(attr);
			while(m.find() && !m.group(0).endsWith(">")) {
				attributes.put(m.group(1), m.group(2));
			}
			if("form".equalsIgnoreCase(tag) && "gaia_loginform".equals(attributes.get("id"))) {
				loginRequest = new HttpPost(attributes.get("action"));
				formValues = new ArrayList<NameValuePair>();
			}
			else if(formValues != null && "input".equalsIgnoreCase(tag)) {
				if("hidden".equals(attributes.get("type"))) {
					formValues.add(new BasicNameValuePair(attributes.get("name"), attributes.get("value")));
				}
			}
			else if(formValues != null && "/form".equalsIgnoreCase(tag)) {
				formValues.add(new BasicNameValuePair("Email", username));
				formValues.add(new BasicNameValuePair("Passwd", password));
				formValues.add(new BasicNameValuePair("PersistentCookie", "yes"));
				loginRequest.setEntity(new UrlEncodedFormEntity(formValues));
				response = client.execute(loginRequest);
				response.getEntity().getContent().close();
				if(response.getStatusLine().getStatusCode() == 302) {
					response = client.execute(new HttpGet(response.getHeaders("Location")[0].getValue()));
					response.getEntity().getContent().close();
				}
				System.out.println(response.getStatusLine());
				for(Cookie cookie : client.getCookieStore().getCookies()) {
					if("GTL".equals(cookie.getName())) {
						authToken = cookie.getValue();
						System.out.println(authToken);
					}
				}
				break;
			}
			m.usePattern(openTag);
		}
	}
	
	public static String readEntityAsString(HttpEntity entity) throws IOException {
		InputStreamReader r = new InputStreamReader(entity.getContent());
		int length;
		StringBuffer b = new StringBuffer(512);
		char[] buffer = new char[512];
		while((length = r.read(buffer)) != -1) {
			b.append(buffer, 0, length);
		}
		return b.toString();
	}
}
