package net.greatsoft.retryclient.handler;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.ObjectUtils;

import net.greatsoft.retryclient.exception.RetryClientException;

public class DefaultRetryClientErrorHandler implements RetryClientErrorHandler {

	@Override
	public void handle(String responseBody, HttpHeaders headers, HttpStatusCode statusCode) {
		if (statusCode.is4xxClientError() || statusCode.is5xxServerError()) {
			throw new RetryClientException(getErrorMessage(statusCode.value(), responseBody));
		}
	}

	private static String getErrorMessage(int rawStatusCode, String responseBody) {
		String preface = rawStatusCode + ": ";
		if (ObjectUtils.isEmpty(responseBody)) {
			return preface + "[no body]";
		}

		String bodyText = LogFormatUtils.formatValue(responseBody, -1, true);

		return preface + bodyText;
	}
}
