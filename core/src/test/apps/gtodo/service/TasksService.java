package test.apps.gtodo.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TasksService {
    public static final String INITIAL_SERVICE_URL = "https://mail.google.com/tasks/ig";
    private static final String SERVICE_URL = "https://mail.google.com/tasks/r/ig";

    private final HttpClient client;
    private BasicHttpContext context;
    private long clientVersion;
    private long latestSyncPoint;
    private int actionId;

    public TasksService(HttpClient client, String authToken) {
        this.client = client;
        context = new BasicHttpContext();
        actionId = 1;

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
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new IOException("Unexpected HTTP response code from service: "
                    + response.getStatusLine().getStatusCode());
        }

        String responseContent = HttpClientUtils.readEntityAsString(response.getEntity());
        Matcher m = Pattern.compile("_setup\\((.*)\\)").matcher(responseContent);
        if (m.find()) {
            try {
                InitialConnectionResult result = new InitialConnectionResult(new JSONObject(m.group(1)));
                clientVersion = result.getClientVersion();
                latestSyncPoint = result.getLatestSyncPoint();
                return result;
            } catch (JSONException e) {
                // Fall through
            }
        }
        throw new IOException("Unexpected response from service: " + responseContent);
    }

    public ServiceResult refresh(String listId) throws IOException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "get_all")
                .put("action_id", actionId++)
                .put("list_id", listId)
                .put("get_deleted", Boolean.FALSE);
            return new ServiceResult(executeRequest(null, request));
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    public ServiceResult addTaskList(String taskList, int index) throws IOException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "create")
                .put("action_id", actionId++)
                .put("index", index)
                .put("entity_delta", new JSONObject()
                .put("name", taskList)
                .put("entity_type", "GROUP"));
            return new ServiceResult(executeRequest(null, request));
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    public ServiceResult renameTaskList(String id, String name) throws IOException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "update")
                .put("action_id", actionId++)
                .put("id", id)
                .put("entity_delta", new JSONObject()
                    .put("name", name));
            return new ServiceResult(executeRequest(null, request));
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    public ServiceResult deleteObject(String id) throws IOException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "update")
                .put("action_id", actionId++)
                .put("id", id)
                .put("entity_delta", new JSONObject()
                    .put("deleted", true));
            return new ServiceResult(executeRequest(null, request));
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }
    
    public ServiceResult addTask(String listId, String taskName, int index, String priorSiblingId) throws IOException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "create")
                .put("action_id", actionId++)
                .put("index", index)
                .put("entity_delta", new JSONObject()
                    .put("name", taskName)
                    .put("entity_type", "TASK"))
                .put("parent_id", listId)
                .put("dest_parent_type", "GROUP")
                .put("list_id", listId)
                .put("prior_sibling_id", priorSiblingId);
            return new ServiceResult(executeRequest(listId, request));
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }
    
    public ServiceResult updateTask(String listId, String taskId, TaskRequest task) throws IOException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "update")
                .put("action_id", actionId++)
                .put("id", taskId)
                .put("entity_delta", task.entityDelta);
            return new ServiceResult(executeRequest(listId, request));
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    private JSONObject executeRequest(String currentListId, JSONObject... requests) throws IOException {
        HttpPost serviceRequest = new HttpPost(SERVICE_URL);
        JSONObject jsonRequest = null;
        try {
            serviceRequest.addHeader("AT", "1");
            jsonRequest = new JSONObject()
                .put("action_list", new JSONArray(Arrays.asList(requests)))
                .put("client_version", clientVersion)
                .put("latest_sync_point", latestSyncPoint)
                .put("current_list_id", currentListId);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }

        try {
            serviceRequest.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                    new BasicNameValuePair("r", jsonRequest.toString()))));
            HttpResponse httpResponse = client.execute(serviceRequest, context);
            return new JSONObject(HttpClientUtils.readEntityAsString(httpResponse.getEntity()));
        } catch (JSONException e) {
            throw new IOException("Unexpected response from service");
        }
    }
}
