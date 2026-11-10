package net.greatsoft.retryclient.configuration;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient.Builder;

import net.greatsoft.retryclient.interceptor.RetryHttpClientInterceptor;

public class InterceptorRestClientCustomizer implements RestClientCustomizer {

  private final RetryHttpClientInterceptor retryHttpClientInterceptor;
  public InterceptorRestClientCustomizer(RetryHttpClientInterceptor retryHttpClientInterceptor) {
    this.retryHttpClientInterceptor = retryHttpClientInterceptor;
  }

  @Override
  public void customize(Builder restClientBuilder) {
    restClientBuilder.requestInterceptors(interceptors -> interceptors.add(retryHttpClientInterceptor)) ;
  }
}
