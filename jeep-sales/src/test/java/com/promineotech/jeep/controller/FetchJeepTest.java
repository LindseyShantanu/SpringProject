package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import com.promineotech.jeep.Constants;
import com.promineotech.jeep.controller.support.FetchJeepTestSupport;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;

class FetchJeepTest{

  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
      config = @SqlConfig(encoding = "utf-8"))
  class TestsThatDoNotPolluteTheApplicationContext extends FetchJeepTestSupport{
    /*
     * @Test: All data is contained within 
     * test method or called methods
     */
    @Test
    void testThatJeepsAreReturnedWhenValidModelAndTrimAreSupplied() {
      
      //Given: a valid model, trim, and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Sport";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      //When: a connection is made to the URI
      ResponseEntity<List<Jeep>> response = 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      //Then: a success (OK -200) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      
      //And: the actual list returned is the same as the expected list
      List<Jeep> actual = response.getBody();
      List<Jeep> expected = buildExpected();
      
      actual.forEach(jeep -> jeep.setModelPK(null));  
      assertThat(actual).isEqualTo(expected);
    }
    
    @Test
    void testThatAnErrorMessageIsReturnedWhenAnUnknownValueIsSupplied() {
      
      //Given: a valid model, trim, and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Invalid Value";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      //When: a connection is made to the URI
      ResponseEntity<Map<String, Object>> response = 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      //Then: a not found (404) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
      
      //And: an error message is returned
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
    }    
    
    @ParameterizedTest
    @MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
    void testThatAnErrorMessageIsReturnedWhenAnInvalidValueIsSupplied(
        String model, String trim, String reason) {
      
      //Given: a valid model, trim, and URI
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      //When: a connection is made to the URI
      ResponseEntity<Map<String, Object>> response = 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      //Then: a bad request (400) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      
      //And: an error message is returned
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);
    }
  }
  static Stream<Arguments> parametersForInvalidInput(){
    // @formatter:off
    return Stream.of(
        arguments("WRANGLER", "@#$%^&&%", "Trim contains non-alpha-numeric chars"),
        arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH +1), "Trim length is too long."),
        arguments("INVALID", "Sport", "Model is not enum value")
        );
    // @formatter:on
  }
  
  
  @Nested
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @ActiveProfiles("test")
  @Sql(scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
      "classpath:flyway/migrations/V1.1__Jeep_Data.sql"},
      config = @SqlConfig(encoding = "utf-8"))
  class TestsThatPolluteTheApplicationContext extends FetchJeepTestSupport{
    @MockBean
    private JeepSalesService jeepSalesService; //Have to have the same name as the autowired object?
    
    @Test
    void testThatAnUnplannedErrorResultsInA500Status() {
      
      //Given: a valid model, trim, and URI
      JeepModel model = JeepModel.WRANGLER;
      String trim = "Invalid";
      String uri = 
          String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
      
      doThrow(new RuntimeException("Ohh noo")).when(jeepSalesService)
        .fetchJeeps(model, trim);
      
      //When: a connection is made to the URI
      ResponseEntity<Map<String, Object>> response = 
          getRestTemplate().exchange(uri, HttpMethod.GET, null, 
              new ParameterizedTypeReference<>() {});
      
      //Then: an internal service error (500) status code is returned
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      
      //And: an error message is returned
      Map<String, Object> error = response.getBody();
      
      assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }    
    
  }

 
}
