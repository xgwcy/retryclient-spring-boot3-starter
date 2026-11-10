package net.greatsoft.retryclient.exception;

public class RetryClientException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RetryClientException() {
		super();
	}

	public RetryClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public RetryClientException(String message) {
		super(message);
	}

	public RetryClientException(Throwable cause) {
		super(cause);
	}

}
