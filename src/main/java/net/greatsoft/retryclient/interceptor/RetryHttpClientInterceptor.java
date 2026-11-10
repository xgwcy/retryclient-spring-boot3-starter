package net.greatsoft.retryclient.interceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import net.greatsoft.retryclient.configuration.RetryProperties;
import net.greatsoft.retryclient.configuration.RetryProperties.RetryConfig;
import net.greatsoft.retryclient.handler.RetryClientErrorHandler;

public class RetryHttpClientInterceptor implements ClientHttpRequestInterceptor, ApplicationContextAware {

	private static final String DEFAULT_CONFIG_NAME = "default";

	private final AntPathMatcher matcher = new AntPathMatcher();

	private ApplicationContext context;

	private final RetryRegistry retryRegistry;
	private final Map<Request, RetryConfig> mapping = new HashMap<>();
	private final RetryProperties retryProperties;

	public RetryHttpClientInterceptor(RetryRegistry retryRegistry, RetryProperties retryProperties) {
		this.retryRegistry = retryRegistry;
		this.retryProperties = retryProperties;
		retryProperties.getMapping().forEach((key, value) -> {
			String[] values = key.split(":");
			if (values.length == 2) {
				mapping.put(new Request(values[0].toLowerCase(), values[1]), value);
			}
		});
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		String path = request.getURI().getPath();
		String methodName = request.getMethod().name();
		String retryName = DEFAULT_CONFIG_NAME;
		Class<? extends RetryClientErrorHandler> errorHandlerClass = this.retryProperties.getRetryHandlerClass();

		for (Request req : mapping.keySet()) {
			if (methodName.equalsIgnoreCase(req.methodName) && matcher.match(req.path, path)) {
				RetryConfig retryConfig = mapping.get(req);
				retryName = retryConfig.getName();
				if (retryConfig.getRetryHandlerClass() != null) {
					errorHandlerClass = retryConfig.getRetryHandlerClass();
				}
				break;
			}
		}

		if ("none".equalsIgnoreCase(retryName) || "disabled".equalsIgnoreCase(retryName)) {
			return execution.execute(request, body);
		}

		Retry retry = this.retryRegistry.retry(retryName);
		try {
			final Class<? extends RetryClientErrorHandler> handlerClass = errorHandlerClass;
			return retry.executeCallable(() -> {
				ClientHttpResponse httpResponse = execution.execute(request, body);
				HttpStatusCode statusCode = httpResponse.getStatusCode();
				if (statusCode.isError()) {
					String responseBody = StreamUtils.copyToString(httpResponse.getBody(), StandardCharsets.UTF_8);
					HttpHeaders headers = httpResponse.getHeaders();
					RetryClientErrorHandler errorHandler = context.getBean(handlerClass);
					errorHandler.handle(responseBody, headers, statusCode);
				}
				return httpResponse;
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static record Request(String methodName, String path) {
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}
