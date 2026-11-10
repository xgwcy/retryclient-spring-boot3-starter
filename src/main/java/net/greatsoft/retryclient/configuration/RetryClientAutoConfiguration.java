package net.greatsoft.retryclient.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.github.resilience4j.retry.RetryRegistry;
import net.greatsoft.retryclient.handler.DefaultRetryClientErrorHandler;
import net.greatsoft.retryclient.interceptor.RetryHttpClientInterceptor;


@AutoConfiguration
@ConditionalOnProperty(prefix = "gx.retryclient", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RetryProperties.class)
@Import(RetryClientConfiguration.class)
public class RetryClientAutoConfiguration {

	@Bean
	RetryHttpClientInterceptor retryHttpClientInterceptor(
			RetryRegistry retryRegistry, RetryProperties retryProperties) {
		return new RetryHttpClientInterceptor(retryRegistry, retryProperties) ;
	}
	
	@Bean
	DefaultRetryClientErrorHandler defaultRetryClientErrorHandler() {
		return new DefaultRetryClientErrorHandler() ;
	}
}
