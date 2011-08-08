package test.apps.gtodo.service;

public class UnexpectedResponseException extends IllegalStateException {
	private static final long serialVersionUID = 1L;

	public UnexpectedResponseException(Exception e) {
		super("Unexpected response", e);
	}
}
