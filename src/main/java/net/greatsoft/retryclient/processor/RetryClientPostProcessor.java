package net.greatsoft.retryclient.processor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.web.client.RestClient;

public class RetryClientPostProcessor implements InstantiationAwareBeanPostProcessor {
  
  @Override
  public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) throws BeansException {
    if (bean instanceof RestClient client) {
      RetryClientRegistry.addClient(client) ;
    }
    return pvs ;
  }
}
