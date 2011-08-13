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
        tasksService = createService();
    }

    private TasksService createService() {
        return new TasksService(new DefaultHttpClient(), authToken);
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
        result = tasksService.addTaskList(initialResult.getDefaultListId(), taskList, initialResult.getTaskListCount());
        assertEquals(1, result.getResultCount());
        assertTrue(result.getResult(0).isNewEntity());
        assertTrue(result.getResult(0).getNewId().length() > 0);

        // Refresh and verify that the new id is in the list of task list ids
        String expectedTaskId = result.getResult(0).getNewId();
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskListWithId(result, expectedTaskId, true);

        // Rename the list
        String renamedTaskList = "Test List " + System.currentTimeMillis();
        assertNotSame(taskList, renamedTaskList);
        tasksService.renameTaskList(initialResult.getDefaultListId(), expectedTaskId, renamedTaskList);

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
        assertTrue(result.getResult(0).isNewEntity());
        assertTrue(result.getResult(0).getNewId().length() > 0);

        // Refresh and verify that the new id is in the list of task ids
        String expectedTaskId = result.getResult(0).getNewId();
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, expectedTaskId, true);

        TaskResult taskResult = getTaskWithIdFromResult(result, expectedTaskId);

        // Verify initial values
        assertEquals(false, taskResult.getCompleted());
        assertNull(taskResult.getDueDate());
        assertEquals("", taskResult.getNotes());
        
        // Update the task
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setName(taskName + "_New");
        taskRequest.setCompleted(true);
        taskRequest.setDueDate(new GregorianCalendar(2011, Calendar.AUGUST, 8).getTime());
        taskRequest.setNotes("A Note");
        tasksService.updateTask(initialResult.getDefaultListId(), expectedTaskId, taskRequest);
        
        // Verify updates to task
        result = tasksService.refresh(initialResult.getDefaultListId());
        taskResult = getTaskWithIdFromResult(result, expectedTaskId);
        assertEquals(taskName + "_New", taskResult.getName());
        assertEquals(true, taskResult.getCompleted());
        assertEquals(new GregorianCalendar(2011, Calendar.AUGUST, 8).getTime(), taskResult.getDueDate());
        assertEquals("A Note", taskResult.getNotes());
        
        // Reset the due date
        taskRequest = new TaskRequest();
        taskRequest.setDueDate(null);
        tasksService.updateTask(initialResult.getDefaultListId(), expectedTaskId, taskRequest);
        
        // Verify no due date
        result = tasksService.refresh(initialResult.getDefaultListId());
        taskResult = getTaskWithIdFromResult(result, expectedTaskId);
        assertNull(taskResult.getDueDate());

        // Finally, delete the task
        tasksService.deleteObject(expectedTaskId);
        
        // Refresh and verify that the task is gone
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, expectedTaskId, false);
    }
    
    @Test
    public void testMoveTask() throws IOException {
        InitialConnectionResult initialResult = tasksService.connect();
        
        ServiceResult result;

        // Create new task in the default list
        String task = "Test Task  " + System.currentTimeMillis();
        result = tasksService.addTask(initialResult.getDefaultListId(), task, 0, null);
        String taskId = result.getResult(0).getNewId();

        // Create new task list to move the new task to
        String taskList = "Test List " + System.currentTimeMillis();
        result = tasksService.addTaskList(initialResult.getDefaultListId(), taskList, initialResult.getTaskListCount());
        String taskListId = result.getResult(0).getNewId();

        // Move the task
        tasksService.moveTask(taskId, initialResult.getDefaultListId(), taskListId);
        
        // Should no longer be in the default list
        result = tasksService.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, taskId, false);
        
        // Should now be in the new task list
        result = tasksService.refresh(taskListId);
        assertHasTaskWithId(result, taskId, true);
        
        // Remove the new task and list
        tasksService.deleteObject(taskId);
        tasksService.deleteObject(taskListId);
    }
    
    @Test
    public void testConcurrentTaskModification() throws IOException {
        TasksService service1 = createService();
        TasksService service2 = createService();
        
        InitialConnectionResult initialResult = service1.connect();
        service2.connect();
        
        String task1Name = "Test Task " + System.currentTimeMillis();
        String task2Name = "Test Task " + (System.currentTimeMillis() + 1);
        
        ServiceResult result;
        
        // Create Task #1 on Service Connection #1
        result = service1.addTask(initialResult.getDefaultListId(), task1Name, 0, null);
        assertFalse(result.hasTasks());
        String task1Id = result.getResult(0).getNewId();
        
        // Create Task #2 on Service Connection #2
        result = service2.addTask(initialResult.getDefaultListId(), task2Name, 0, null);
        // Verify that Service Connection #2 is notified about creation of Task #1
        assertTrue(result.hasTasks());
        assertHasTaskWithId(result, task1Id, true);
        String task2Id = result.getResult(0).getNewId();
        
        // Update Task #1 on Service Connection #1
        TaskRequest taskRequest = new TaskRequest();
        taskRequest.setNotes("ABC");
        result = service1.updateTask(initialResult.getDefaultListId(), task1Id, taskRequest);
        // Verify that Service Connection #1 is notified about creation of Task #2
        assertTrue(result.hasTasks());
        assertHasTaskWithId(result, task2Id, true);

        // Update Task #2 on Service Connection #2
        taskRequest = new TaskRequest();
        taskRequest.setCompleted(true);
        result = service2.updateTask(initialResult.getDefaultListId(), task2Id, taskRequest);
        // Verify that Service Connection #2 is notified about update to Task #1
        assertTrue(result.hasTasks());
        assertHasTaskWithId(result, task1Id, true);
        assertEquals("ABC", getTaskWithIdFromResult(result, task1Id).getNotes());
        
        // Remove the new tasks
        tasksService.deleteObject(task1Id);
        tasksService.deleteObject(task2Id);
    }

    @Test
    public void testConcurrentTaskListModification() throws IOException {
        TasksService service1 = createService();
        TasksService service2 = createService();
        
        InitialConnectionResult initialResult = service1.connect();
        service2.connect();
        
        String list1Name = "Test List " + System.currentTimeMillis();
        String list2Name = "Test List " + (System.currentTimeMillis() + 1);
        
        ServiceResult result;
        
        // Create Task List #1 on Service Connection #1
        result = service1.addTaskList(initialResult.getDefaultListId(), list1Name, 0);
        assertFalse(result.hasTaskLists());
        String list1Id = result.getResult(0).getNewId();
        
        // Create Task List #2 on Service Connection #2
        result = service2.addTaskList(initialResult.getDefaultListId(), list2Name, 0);
        // Verify that Service Connection #2 is notified about creation of Task List #1
        assertTrue(result.hasTaskLists());
        assertHasTaskListWithId(result, list1Id, true);
        String list2Id = result.getResult(0).getNewId();
        
        // Rename Task List #1 on Service Connection #1
        result = service1.renameTaskList(initialResult.getDefaultListId(), list1Id, list1Name + " Changed");
        // Verify that Service Connection #1 is notified about creation of Task List #2
        assertTrue(result.hasTaskLists());
        assertHasTaskListWithId(result, list2Id, true);

        // Rename Task List #2 on Service Connection #2
        result = service2.renameTaskList(initialResult.getDefaultListId(), list2Id, list2Name + " Changed");
        // Verify that Service Connection #2 is notified about Task List #1 being renamed
        assertTrue(result.hasTaskLists());
        assertHasTaskListWithId(result, list1Id, true);
        assertEquals(list1Name + " Changed", getTaskListWithIdFromResult(result, list1Id).getName());
        
        // Remove the new tasks
        tasksService.deleteObject(list1Id);
        tasksService.deleteObject(list2Id);
    }
    
    @Test
    public void testConcurrentTaskSourceListModification() throws IOException {
        TasksService service1 = createService();
        TasksService service2 = createService();
        
        InitialConnectionResult initialResult = service1.connect();
        service2.connect();
        
        String targetList1Name = "Test List " + System.currentTimeMillis();
        String targetList2Name = "Test List " + (System.currentTimeMillis() + 1);
        
        ServiceResult result;
        
        // Create Task List #1 on Service Connection #1
        result = service1.addTaskList(initialResult.getDefaultListId(), targetList1Name, 0);
        String targetList1Id = result.getResult(0).getNewId();
        
        // Create Task List #2 on Service Connection #2
        result = service2.addTaskList(initialResult.getDefaultListId(), targetList2Name, 0);
        String targetList2Id = result.getResult(0).getNewId();
        
        // Create Task on Service Connection #1 in default list
        result = service1.addTask(initialResult.getDefaultListId(), "Foo", 0, null);
        String taskId = result.getResult(0).getNewId();
        
        // Verify existence of Task on Service Connection #1 in Default List
        result = service1.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, taskId, true);

        // Verify existence of Task on Service Connection #2 in Default List
        result = service2.refresh(initialResult.getDefaultListId());
        assertHasTaskWithId(result, taskId, true);
        
        // Move Task on Service Connection #1 from Default List to Task List #1
        result = service1.moveTask(taskId, initialResult.getDefaultListId(), targetList1Id);
        // Verify existence of Task on Service Connection #1 in Task List #1
        result = service1.refresh(targetList1Id);
        assertHasTaskWithId(result, taskId, true);
        
        // Move Task on Service Connection #2 to from Default List (don't know about first move) to Task List #2
        result = service2.moveTask(taskId, initialResult.getDefaultListId(), targetList2Id);
        // Verify existence of Task on Service Connection #1 in Task List #2
        result = service2.refresh(targetList2Id);
        assertHasTaskWithId(result, taskId, true);

        // Remove the task and lists
        tasksService.deleteObject(taskId);
        tasksService.deleteObject(targetList1Id);
        tasksService.deleteObject(targetList2Id);
    }
    
    // TODO: Test deleting lists
    
    private TaskList getTaskListWithIdFromResult(ServiceResult result, String id) {
        TaskList list = null;
        for(int i = 0; i < result.getTaskListCount(); i++) {
            list = result.getTaskList(i);
            if(list.getId().equals(id)) {
                break;
            }
        }
        return list;
    }
    
    private TaskResult getTaskWithIdFromResult(ServiceResult result, String id) {
        TaskResult task = null;
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
