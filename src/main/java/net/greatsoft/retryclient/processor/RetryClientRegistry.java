package net.greatsoft.retryclient.processor;

import java.util.HashSet;
import java.util.Set;

import org.springframework.web.client.RestClient;

public class RetryClientRegistry {

  private static final Set<RestClient> clients = new HashSet<>() ;
  
  public static Set<RestClient> getClients() {
    return Set.copyOf(clients) ;
  }
  
  public static void addClient(RestClient client) {
    clients.add(client) ;
  }
}
