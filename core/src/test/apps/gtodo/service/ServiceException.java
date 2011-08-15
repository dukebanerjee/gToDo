package test.apps.gtodo.service;

public class ServiceException extends Exception
        implements TaskListsResult, TasksResult {
    private static final long serialVersionUID = 1L;
    private final ServiceResult serviceResult;

    public ServiceException(ServiceResult result) {
        this.serviceResult = result;
    }

    public boolean isRetryable() {
        return serviceResult.getRequestResult().isRetryable();
    }

    public int getErrorCode() {
        return serviceResult.getRequestResult().getErrorCode();
    }
    
    public int getTaskListCount() {
        return serviceResult.getTaskListCount();
    }
    
    public TaskListResult getTaskList(int i) {
        return serviceResult.getTaskList(i);
    }
    
    public int getTaskCount() {
        return serviceResult.getTaskCount();
    }
    
    public TaskResult getTask(int i) {
        return serviceResult.getTask(i);
    }
}
