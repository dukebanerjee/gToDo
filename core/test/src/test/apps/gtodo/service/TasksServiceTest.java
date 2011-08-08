package test.apps.gtodo.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TasksServiceTest {
	private TasksService tasksService;
	private DefaultHttpClient client;

	@Before
	public void setup() throws IOException {
		client = new DefaultHttpClient();
		tasksService = new TasksService(client, readAuthToken());
	}
	
	@Test
	public void testInitialConnection() throws IOException {
		InitialConnectionResult result = tasksService.connect();
		
		// The following are needed for subsequent communication with the web service
		assertTrue(result.getClientVersion() > 0);
		assertTrue(result.getLatestSyncPoint() > 0);
		
		// The following are needed for initial population of the UI
		assertTrue(result.getTaskListCount() > 0);
		boolean foundDefaultTaskList = false;
		for(int i = 0; i < result.getTaskListCount(); i++) {
			if("Default List".equals(result.getTaskList(i).getName())) {
				foundDefaultTaskList = true;
				break;
			}
		}
		assertTrue(foundDefaultTaskList);
		
		// This is possibly a not-always-valid assertion. The Google Tasks web client always creates a task with 
		// a blank name in new lists, there should be at least one item. However, this is not enforced by the server.
		assertTrue(result.getDefaultListTaskCount() > 0);
		boolean foundDefaultEmptyTask = false;
		for(int i = 0; i < result.getDefaultListTaskCount(); i++) {
			if("".equals(result.getTask(i).getName())) {
				foundDefaultEmptyTask = true;
				break;
			}
		}
		assertTrue(foundDefaultEmptyTask);
	}
	
	private String readAuthToken() throws IOException {
		HttpResponse response = client.execute(new HttpGet(TasksService.INITIAL_SERVICE_URL));
		assertEquals(200, response.getStatusLine().getStatusCode());
		Pattern openTag = Pattern.compile("<(/?[A-Za-z]+)");
		Pattern attr = Pattern.compile("/?>|([A-Za-z]+) *= *[\"']?([\\S&&[^\"']]+)[\"']?");
		Matcher m = openTag.matcher(HttpClientUtils.readEntityAsString(response.getEntity()));
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
				String username = System.getProperty("tasks.service.username");
				String password = System.getProperty("tasks.service.password");
				if(username == null || password == null) {
					throw new IllegalStateException("tasks.service.username and tasks.service.password need to provided as system properties");
				}
				
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
				assertEquals(200, response.getStatusLine().getStatusCode());
				for(Cookie cookie : client.getCookieStore().getCookies()) {
					if("GTL".equals(cookie.getName())) {
						return cookie.getValue();
					}
				}
				break;
			}
			m.usePattern(openTag);
		}
		throw new IllegalStateException("Could not read authentication token from response");
	}
}
