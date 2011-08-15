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
    private String currentListId;

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
                currentListId = result.getDefaultListId();
                return result;
            } catch (JSONException e) {
                // Fall through
            }
        }
        throw new IOException("Unexpected response from service: " + responseContent);
    }
    
    public void setCurrentListId(String listId) {
        this.currentListId = listId;
    }

    public ServiceResult refresh() throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "get_all")
                .put("action_id", actionId++)
                .put("list_id", currentListId)
                .put("get_deleted", Boolean.FALSE);
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    public ServiceResult addTaskList(String taskList, int index) throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "create")
                .put("action_id", actionId++)
                .put("index", index)
                .put("entity_delta", new JSONObject()
                .put("name", taskList)
                .put("entity_type", "GROUP"));
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    public ServiceResult renameTaskList(String id, String name) throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "update")
                .put("action_id", actionId++)
                .put("id", id)
                .put("entity_delta", new JSONObject()
                    .put("name", name));
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    public ServiceResult deleteObject(String id) throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "update")
                .put("action_id", actionId++)
                .put("id", id)
                .put("entity_delta", new JSONObject()
                    .put("deleted", true));
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }
    
    public ServiceResult addTask(String taskName, int index, String priorSiblingId) throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "create")
                .put("action_id", actionId++)
                .put("index", index)
                .put("entity_delta", new JSONObject()
                    .put("name", taskName)
                    .put("entity_type", "TASK"))
                .put("parent_id", currentListId)
                .put("dest_parent_type", "GROUP")
                .put("list_id", currentListId)
                .put("prior_sibling_id", priorSiblingId);
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }
    
    public ServiceResult updateTask(String taskId, TaskRequest task) throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "update")
                .put("action_id", actionId++)
                .put("id", taskId)
                .put("entity_delta", task.entityDelta);
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }
    
    public ServiceResult moveTask(String taskId, String targetListId) throws IOException, ServiceException {
        try {
            JSONObject request = new JSONObject()
                .put("action_type", "move")
                .put("action_id", actionId++)
                .put("id", taskId)
                .put("source_list", currentListId)
                .put("dest_parent", targetListId)
                .put("dest_list", targetListId);
            return executeRequest(request);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }

    private ServiceResult executeRequest(JSONObject request) throws IOException, ServiceException {
        HttpPost serviceRequest = new HttpPost(SERVICE_URL);
        serviceRequest.addHeader("AT", "1");
        serviceRequest.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                new BasicNameValuePair("r", createServiceRequest(request).toString()))));
        HttpResponse httpResponse = client.execute(serviceRequest, context);
        return createServiceResponse(httpResponse);
    }

    private JSONObject createServiceRequest(JSONObject request) {
        try {
            return new JSONObject()
                .put("action_list", new JSONArray(Arrays.asList(request)))
                .put("client_version", clientVersion)
                .put("latest_sync_point", latestSyncPoint)
                .put("current_list_id", currentListId);
        } catch (JSONException e) {
            throw new IllegalStateException("Not possible");
        }
    }


    private ServiceResult createServiceResponse(HttpResponse httpResponse) throws IOException, ServiceException {
        try {
            JSONObject response = new JSONObject(HttpClientUtils.readEntityAsString(httpResponse.getEntity()));
            if(response.has("latest_sync_point")) {
                latestSyncPoint = response.getLong("latest_sync_point");
            }
            ServiceResult serviceResult = new ServiceResult(response);
            if(serviceResult.hasResult() && serviceResult.getRequestResult().isError()) {
                throw new ServiceException(serviceResult);
            }
            return serviceResult;
        } catch (JSONException e) {
            throw new IllegalStateException("Unexpected response from service");
        }
    }
}
