package test.apps.gtodo.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
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

        // The following are needed for subsequent communication with the web service
        assertTrue(result.getClientVersion() > 0);
        assertTrue(result.getLatestSyncPoint() > 0);

        // The following are needed for initial population of the UI
        assertHasTaskList(result, "Default List", true);
        assertHasTaskWithName(result, "", true);
        assertTrue(result.getDefaultListId().length() > 0);
    }

    @Test
    public void testRefresh() throws IOException {
        InitialConnectionResult initialResult = tasksService.connect();
        ServiceResult result = tasksService.refresh(initialResult.getDefaultListId());

        assertHasTaskList(result, "Default List", true);
        assertHasTaskWithName(result, "", true);
    }

    @Test
    public void testListOperations() throws IOException {
        String taskList = "Test List " + System.currentTimeMillis();

        // Make sure the new task list does not already exist
        InitialConnectionResult initialResult = tasksService.connect();
        assertHasTaskList(initialResult, taskList, false);

        ServiceResult result;

        // Add the task list and verify that we got a new id
        result = tasksService.addTaskList(taskList, initialResult.getTaskListCount());
        assertEquals(1, result.getResultCount());
        assertTrue(result.getResult(0) instanceof NewEntityResult);
        assertTrue(((NewEntityResult) result.getResult(0)).getNewId().length() > 0);

        // Refresh and verify that the new id is in the list of task list ids
        String expectedTaskId = ((NewEntityResult) result.getResult(0)).getNewId();
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
        tasksService.deleteObject(expectedTaskId);

        // Refresh and verify that the list is gone
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskListWithId(result, expectedTaskId, false);
    }

    @Test
    public void testTaskOperations() throws IOException {
        String taskName = "Test Task " + System.currentTimeMillis();

        // Make sure the new task does not already exist
        InitialConnectionResult initialResult = tasksService.connect();
        assertHasTaskList(initialResult, taskName, false);

        ServiceResult result;

        // Add the task and verify that we got a new id
        int index = initialResult.getTaskCount();
        String priorSiblingId = initialResult.getTask(index - 1).getId();
        result = tasksService.addTask(initialResult.getDefaultListId(), taskName, 0, priorSiblingId);
        assertEquals(1, result.getResultCount());
        assertTrue(result.getResult(0) instanceof NewEntityResult);
        assertTrue(((NewEntityResult) result.getResult(0)).getNewId().length() > 0);

        // Refresh and verify that the new id is in the list of task ids
        String expectedTaskId = ((NewEntityResult) result.getResult(0)).getNewId();
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, expectedTaskId, true);

        Task task = getTaskWithIdFromResult(result, expectedTaskId);

        // Verify initial values
        assertEquals(false, task.getCompleted());
        assertNull(task.getDueDate());
        assertEquals("", task.getNotes());
        
        // Update the task
        task.setName(taskName + "_New");
        task.setCompleted(true);
        task.setDueDate(new GregorianCalendar(2011, Calendar.AUGUST, 8).getTime());
        task.setNotes("A Note");
        tasksService.updateTask(initialResult.getDefaultListId(), task);
        
        // Verify updates to task
        result = tasksService.refresh(initialResult.getDefaultListId());
        task = getTaskWithIdFromResult(result, expectedTaskId);
        assertEquals(taskName + "_New", task.getName());
        assertEquals(true, task.getCompleted());
        assertEquals(new GregorianCalendar(2011, Calendar.AUGUST, 8).getTime(), task.getDueDate());
        assertEquals("A Note", task.getNotes());
        
        // Reset the due date
        task.setDueDate(null);
        tasksService.updateTask(initialResult.getDefaultListId(), task);
        
        // Verify no due date
        result = tasksService.refresh(initialResult.getDefaultListId());
        task = getTaskWithIdFromResult(result, expectedTaskId);
        assertNull(task.getDueDate());

        // Finally, delete the task
        tasksService.deleteObject(expectedTaskId);

        // Refresh and verify that the task is gone
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, expectedTaskId, false);
    }

    private Task getTaskWithIdFromResult(ServiceResult result, String id) {
        Task task = null;
        for(int i = 0; i < result.getTaskCount(); i++) {
            task = result.getTask(i);
            if(task.getId().equals(id)) {
                break;
            }
        }
        return task;
    }
    
    private void assertHasTaskList(final TaskListsResult result, String expectedTaskList, boolean expected) {
        assertFoundInList(expectedTaskList, expected, new ListAccessor() {
            public Object getListValue(int i) {
                return result.getTaskList(i).getName();
            }
            
            public int getCount() {
                return result.getTaskListCount();
            }
        });
    }

    private void assertHasTaskListWithId(final TaskListsResult result, String expectedId, boolean expected) {
        assertFoundInList(expectedId, expected, new ListAccessor() {
            public Object getListValue(int i) {
                return result.getTaskList(i).getId();
            }
            
            public int getCount() {
                return result.getTaskListCount();
            }
        });
    }

    private void assertHasTaskWithId(final TasksResult result, String expectedId, boolean expected) {
        assertFoundInList(expectedId, expected, new ListAccessor() {
            public Object getListValue(int i) {
                return result.getTask(i).getId();
            }
            
            public int getCount() {
                return result.getTaskCount();
            }
        });
    }

    private void assertHasTaskWithName(final TasksResult result, String expectedName, boolean expected) {
        assertFoundInList(expectedName, expected, new ListAccessor() {
            public Object getListValue(int i) {
                return result.getTask(i).getName();
            }
            
            public int getCount() {
                return result.getTaskCount();
            }
        });
    }
    
    private void assertFoundInList(Object value, boolean expectedToBeFound, ListAccessor listAccessor) {
        assertTrue(listAccessor.getCount() > 0);
        boolean foundValue = false;
        for (int i = 0; i < listAccessor.getCount(); i++) {
            if (value.equals(listAccessor.getListValue(i))) {
                foundValue = true;
                break;
            }
        }
        assertEquals(expectedToBeFound, foundValue);
    }
    
    private interface ListAccessor {
        public int getCount();
        public Object getListValue(int i);
    }

    private static String readAuthToken() throws IOException {
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(new HttpGet(TasksService.INITIAL_SERVICE_URL));
            assertEquals(HTTP_OK, response.getStatusLine().getStatusCode());

            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(new InputStreamReader(response.getEntity().getContent())));
            Document doc = parser.getDocument();
            Element form = doc.getElementById("gaia_loginform");
            assertNotNull("Expecting login form", form);

            List<NameValuePair> formValues = new ArrayList<NameValuePair>();
            NodeList formInputs = form.getElementsByTagName("input");
            for (int i = 0; i < formInputs.getLength(); i++) {
                Element formInput = (Element) formInputs.item(i);
                if ("hidden".equals(formInput.getAttribute("type"))) {
                    formValues.add(new BasicNameValuePair(
                            formInput.getAttribute("name"), 
                            formInput.getAttribute("value")));
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
                response = client.execute(new HttpGet(response.getHeaders("Location")[0].getValue()));
                response.getEntity().getContent().close();
            }
            assertEquals(HTTP_OK, response.getStatusLine().getStatusCode());

            for (Cookie cookie : client.getCookieStore().getCookies()) {
                if ("GTL".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            throw new IllegalStateException("Could not read authentication token from response");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
