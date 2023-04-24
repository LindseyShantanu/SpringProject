package com.promineotech.jeep.controller.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import lombok.Getter;

public class BaseTest {
  @LocalServerPort
  private int serverPort;
  
  @Autowired
  @Getter
  private TestRestTemplate restTemplate;
  
  protected String getBaseUriforJeeps() {
    return String.format("http://localhost:%d/jeeps", serverPort);
  }
  
  protected String getBaseUriForOrders() {
    return String.format("http://localhost:%d/orders", serverPort);
  }
}
