package net.greatsoft.retryclient.configuration;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.greatsoft.retryclient.interceptor.RetryHttpClientInterceptor;
import net.greatsoft.retryclient.processor.RetryClientInitializing;
import net.greatsoft.retryclient.processor.RetryClientPostProcessor;

@Configuration
@ConditionalOnProperty(prefix = "gx.retryclient", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RetryClientConfiguration {

  @Bean
  InterceptorRestClientCustomizer interceptorRestClientCustomizer(RetryHttpClientInterceptor retryHttpClientInterceptor) {
    return new InterceptorRestClientCustomizer(retryHttpClientInterceptor) ;
  }
  
  @Bean
  @ConditionalOnProperty(prefix = "gx.retryclient.timeout", name = "enabled", 
  	havingValue = "true", matchIfMissing = false)
  ClientHttpRequestFactorySettings clientHttpRequestFactorySettings(RetryProperties retryProperties) {
    return ClientHttpRequestFactorySettings
        .defaults()
        .withConnectTimeout(
        		Duration.ofMillis(retryProperties.getTimeout().getConnectTimeout()))
        .withReadTimeout(
        		Duration.ofMillis(retryProperties.getTimeout().getReadTimeout())) ;
  }
  
  @Bean
  RetryClientInitializing retryClientInitializing(RetryHttpClientInterceptor retryHttpClientInterceptor, 
		  RetryProperties retryProperties) {
	  return new RetryClientInitializing(retryHttpClientInterceptor, retryProperties) ;
  }
  
  @Bean
  static RetryClientPostProcessor retryClientPostProcessor() {
	  return new RetryClientPostProcessor() ;
  }
}
