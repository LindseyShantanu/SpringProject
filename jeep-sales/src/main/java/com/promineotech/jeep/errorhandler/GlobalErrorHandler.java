package com.promineotech.jeep.errorhandler;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import lombok.extern.slf4j.Slf4j;

/*
 * global error handlers manages:
 * error messages
 * result codes
 * error logging
 */

@RestControllerAdvice
@Slf4j //lombok logging annotation
public class GlobalErrorHandler {
  private enum LogStatus {
    STACK_TRACE, MESSAGE_ONLY
  }
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(code = HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e, WebRequest webRequest){
    return createExceptionMessage(e, HttpStatus.BAD_REQUEST, webRequest,
        LogStatus.MESSAGE_ONLY);
  }
  
  /*
   * 400 - Bad request
   */
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(code = HttpStatus.BAD_REQUEST)
  private Map<String, Object> handleConstraintViolationException(
      ConstraintViolationException e, WebRequest webRequest){
    return createExceptionMessage(e, HttpStatus.BAD_REQUEST, webRequest,
        LogStatus.MESSAGE_ONLY);
  }
  
  /*
   * 404 - not found
   */
  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(code = HttpStatus.NOT_FOUND)
  public Map<String, Object> handleNoSuchElementException(
      NoSuchElementException e, WebRequest webRequest){
    return createExceptionMessage(e, HttpStatus.NOT_FOUND, webRequest, LogStatus.STACK_TRACE);
  }
   
  /*
   * 500 - any other error/unknown error
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
  public Map<String, Object> handleException(Exception e, WebRequest webRequest){
    return createExceptionMessage(e, HttpStatus.INTERNAL_SERVER_ERROR, webRequest,
        LogStatus.STACK_TRACE);
  }
  
  
  /**
   * @param e
   * @param status
   * @param webRequest
   * @param logStatus
   * @return
   */
  private Map<String, Object> createExceptionMessage(Exception e,
      HttpStatus status,  WebRequest webRequest, LogStatus logStatus){
    Map<String, Object> error = new HashMap<>();
    String timestamp = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    
    if(webRequest instanceof ServletWebRequest) {
      error.put("uri", 
          ((ServletWebRequest)webRequest).getRequest().getRequestURI());
    }
    
    error.put("message", e.toString());
    error.put("status code", status.value());
    error.put("timestamp", timestamp);
    error.put("reason", status.getReasonPhrase());
    
    if(logStatus == LogStatus.MESSAGE_ONLY) {
      log.error("Exception:", e.toString());
    }
    else {
      log.error("Exception:", e);
    }
    
    return error;
  }
}
