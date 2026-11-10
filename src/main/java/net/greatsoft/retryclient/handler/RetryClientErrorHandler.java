package net.greatsoft.retryclient.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public interface RetryClientErrorHandler {

	default void handle(String responseBody, HttpHeaders headers, HttpStatusCode statusCode) {
		
	};
}
