package test.apps.gtodo.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.cyberneko.html.parsers.DOMParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static org.junit.Assert.*;

public class TasksServiceTest {
    private static final int HTTP_OK = 200;
    private static final int HTTP_REDIRECT = 302;

    private TasksService tasksService;
    private static String authToken;

    @BeforeClass
    public static void suiteSetup() throws IOException {
        authToken = readAuthToken();
    }

    @Before
    public void setup() throws IOException {
        tasksService = new TasksService(new DefaultHttpClient(), authToken);
    }

    @Test
    public void testInitialConnection() throws IOException {
        InitialConnectionResult result = tasksService.connect();

        // The following are needed for subsequent communication with the web
        // service
        assertTrue(result.getClientVersion() > 0);
        assertTrue(result.getLatestSyncPoint() > 0);

        // The following are needed for initial population of the UI
        assertHasTaskList(result, "Default List", true);
        assertExpectedEmptyTaskResult(result);
        assertTrue(result.getDefaultListId().length() > 0);
    }

    @Test
    public void testRefresh() throws IOException {
        InitialConnectionResult initialResult = tasksService.connect();
        ServiceResult result = tasksService.refresh(initialResult
                .getDefaultListId());

        assertHasTaskList(result, "Default List", true);
        assertExpectedEmptyTaskResult(result);
    }

    @Test
    public void testListOperations() throws IOException {
        String taskList = "Test List " + System.currentTimeMillis();

        // Make sure the new task list does not already exist
        InitialConnectionResult initialResult = tasksService.connect();
        assertHasTaskList(initialResult, taskList, false);

        ServiceResult result;

        // Add the task list and verify that we got a new id
        result = tasksService.addTaskList(taskList,
                initialResult.getTaskListCount());
        assertEquals(1, result.getResultCount());
        assertTrue(result.getResult(0) instanceof NewEntityResult);
        assertTrue(((NewEntityResult) result.getResult(0)).getNewId().length() > 0);

        // Refresh and verify that the task is in the list of new ids
        String expectedTaskId = ((NewEntityResult) result.getResult(0))
                .getNewId();
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskListWithId(result, expectedTaskId, true);

        // Rename the list
        String renamedTaskList = "Test List " + System.currentTimeMillis();
        assertNotSame(taskList, renamedTaskList);
        tasksService.renameTaskList(expectedTaskId, renamedTaskList);

        // Refresh and verify that the old name is out and the new name is in
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskList(result, taskList, false);
        assertHasTaskList(result, renamedTaskList, true);

        // Finally, delete the list
        tasksService.deleteTaskList(expectedTaskId);

        // Refresh and verify that the list is gone
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskListWithId(result, expectedTaskId, false);
    }

    private void assertHasTaskList(TaskListsResult result,
            String expectedTaskList, boolean expected) {
        assertTrue(result.getTaskListCount() > 0);
        boolean foundDefaultTaskList = false;
        for (int i = 0; i < result.getTaskListCount(); i++) {
            if (expectedTaskList.equals(result.getTaskList(i).getName())) {
                foundDefaultTaskList = true;
                break;
            }
        }
        assertEquals(expected, foundDefaultTaskList);
    }

    private void assertHasTaskListWithId(TaskListsResult result,
            String expectedId, boolean expected) {
        assertTrue(result.getTaskListCount() > 0);
        boolean foundTaskListWithId = false;
        for (int i = 0; i < result.getTaskListCount(); i++) {
            if (expectedId.equals(result.getTaskList(i).getId())) {
                foundTaskListWithId = true;
                break;
            }
        }
        assertEquals(expected, foundTaskListWithId);
    }

    private void assertExpectedEmptyTaskResult(TasksResult result) {
        // This is possibly a not-always-valid assertion. The Google Tasks web
        // client always creates a task with
        // a blank name in new lists, there should be at least one item.
        // However, this is not enforced by the server.
        assertTrue(result.getTaskCount() > 0);
        boolean foundDefaultEmptyTask = false;
        for (int i = 0; i < result.getTaskCount(); i++) {
            if ("".equals(result.getTask(i).getName())) {
                foundDefaultEmptyTask = true;
                break;
            }
        }
        assertTrue(foundDefaultEmptyTask);
    }

    private static String readAuthToken() throws IOException {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(new HttpGet(
                    TasksService.INITIAL_SERVICE_URL));
            assertEquals(HTTP_OK, response.getStatusLine().getStatusCode());

            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(new InputStreamReader(response
                    .getEntity().getContent())));
            Document doc = parser.getDocument();
            Element form = doc.getElementById("gaia_loginform");
            assertNotNull("Expecting login form", form);

            List<NameValuePair> formValues = new ArrayList<NameValuePair>();
            NodeList formInputs = form.getElementsByTagName("input");
            for (int i = 0; i < formInputs.getLength(); i++) {
                Element formInput = (Element) formInputs.item(i);
                if ("hidden".equals(formInput.getAttribute("type"))) {
                    formValues.add(new BasicNameValuePair(formInput
                            .getAttribute("name"), formInput
                            .getAttribute("value")));
                }
            }

            String username = System.getProperty("tasks.service.username");
            String password = System.getProperty("tasks.service.password");
            if (username == null || password == null) {
                throw new IllegalStateException(
                        "tasks.service.username and tasks.service.password need to provided as system properties");
            }
            formValues.add(new BasicNameValuePair("Email", username));
            formValues.add(new BasicNameValuePair("Passwd", password));
            formValues.add(new BasicNameValuePair("PersistentCookie", "yes"));

            HttpPost loginRequest = new HttpPost(form.getAttribute("action"));
            loginRequest.setEntity(new UrlEncodedFormEntity(formValues));
            response = client.execute(loginRequest);
            response.getEntity().getContent().close();
            if (response.getStatusLine().getStatusCode() == HTTP_REDIRECT) {
                response = client.execute(new HttpGet(response
                        .getHeaders("Location")[0].getValue()));
                response.getEntity().getContent().close();
            }
            assertEquals(HTTP_OK, response.getStatusLine().getStatusCode());

            for (Cookie cookie : client.getCookieStore().getCookies()) {
                if ("GTL".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            throw new IllegalStateException(
                    "Could not read authentication token from response");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
