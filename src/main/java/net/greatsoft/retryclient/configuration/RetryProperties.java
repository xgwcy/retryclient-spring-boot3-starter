package net.greatsoft.retryclient.configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import net.greatsoft.retryclient.handler.DefaultRetryClientErrorHandler;
import net.greatsoft.retryclient.handler.RetryClientErrorHandler;

@Component
@ConfigurationProperties(prefix = "gx.retryclient")
public class RetryProperties {

	private boolean enabled = true ;
	private Map<String, RetryConfig> mapping = new HashMap<>();
	private Timeout timeout ;
	private boolean repeatread = false ;
	private Class<? extends RetryClientErrorHandler> retryHandlerClass = DefaultRetryClientErrorHandler.class;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public Map<String, RetryConfig> getMapping() {
		return mapping;
	}
	public void setMapping(Map<String, RetryConfig> mapping) {
		this.mapping = mapping;
	}
	public Timeout getTimeout() {
		return timeout;
	}
	public void setTimeout(Timeout timeout) {
		this.timeout = timeout;
	}
	public boolean isRepeatread() {
		return repeatread;
	}
	public void setRepeatread(boolean repeatread) {
		this.repeatread = repeatread;
	}
	public Class<? extends RetryClientErrorHandler> getRetryHandlerClass() {
		return retryHandlerClass;
	}
	public void setRetryHandlerClass(Class<? extends RetryClientErrorHandler> retryHandlerClass) {
		this.retryHandlerClass = retryHandlerClass;
	}
	public static class Timeout {
		private boolean enabled = true ;
		private Long connectTimeout = 1000L ;
		private Long readTimeout = 1000L;
		public boolean isEnabled() {
			return enabled;
		}
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
		public Long getConnectTimeout() {
			return connectTimeout;
		}
		public void setConnectTimeout(Long connectTimeout) {
			this.connectTimeout = connectTimeout;
		}
		public Long getReadTimeout() {
			return readTimeout;
		}
		public void setReadTimeout(Long readTimeout) {
			this.readTimeout = readTimeout;
		}
	}
	public static class RetryConfig {
		private String name ;
		private Class<? extends RetryClientErrorHandler> retryHandlerClass ;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Class<? extends RetryClientErrorHandler> getRetryHandlerClass() {
			return retryHandlerClass;
		}
		public void setRetryHandlerClass(Class<? extends RetryClientErrorHandler> retryHandlerClass) {
			this.retryHandlerClass = retryHandlerClass;
		}
	}
}
