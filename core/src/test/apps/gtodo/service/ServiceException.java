package test.apps.gtodo.service;

public class ServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    private final RequestResult result;

    public ServiceException(RequestResult encoding) {
        this.result = encoding;
    }

    public boolean isRetryable() {
        return result.isRetryable();
    }

    public int getErrorCode() {
        return result.getErrorCode();
    }
}
