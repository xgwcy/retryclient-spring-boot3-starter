package net.greatsoft.retryclient.processor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import net.greatsoft.retryclient.configuration.RetryProperties;
import net.greatsoft.retryclient.interceptor.RetryHttpClientInterceptor;
import net.greatsoft.retryclient.interceptor.StreamClientHttpRequestInterceptor;

public class RetryClientInitializing implements SmartInitializingSingleton {

  private final RetryHttpClientInterceptor interceptor;
  private final RetryProperties retryProperties;
  public RetryClientInitializing(RetryHttpClientInterceptor interceptor, RetryProperties retryProperties) {
    this.interceptor = interceptor;
    this.retryProperties = retryProperties ;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void afterSingletonsInstantiated() {
    RetryClientRegistry.getClients().forEach(client -> {
      try {
        Field field = client.getClass().getDeclaredField("interceptors") ;
        field.setAccessible(true) ;
        
        List<ClientHttpRequestInterceptor> interceptors = (List<ClientHttpRequestInterceptor>) field.get(client) ;
        if (interceptors == null) {
          interceptors = new ArrayList<>() ;
        }
        if (!interceptors.contains(this.interceptor)) {
          interceptors.add(this.interceptor) ;
        }
        if (this.retryProperties.isRepeatread()) {
        	interceptors.add(new StreamClientHttpRequestInterceptor()) ;
        }
        field.set(client, interceptors) ;
      } catch (Exception e) {
        e.printStackTrace() ;
      }
    }) ;
  }
}
