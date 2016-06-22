package com.emc.ia.sdk.sip.ingestion;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;

public class WhenExecutingRestClient {
  
  private HttpClientWrapper wrapper;
  private SimpleRestClient client;
  private static final String URI = "http://identifiers.emc.com/aips";
  private static final List<Header> HEADERS = new ArrayList<Header>();
  
  @Before
  public void init() {
    wrapper = mock(HttpClientWrapper.class);
    client = new SimpleRestClient();
    client.prepare(wrapper); 
     
    HEADERS.add(new BasicHeader("AuthToken", "XYZ123ABC"));
    HEADERS.add(new BasicHeader("Accept", "application/hal+json"));
  }
  
  @Test
  public void shouldExecuteGetSuccessfully() throws ClientProtocolException, IOException {
    
    HttpGet getRequest = new HttpGet();
    IAHomeResource resource = new IAHomeResource();
    when(wrapper.httpGetRequest(URI, HEADERS)).thenReturn(getRequest);
    when(wrapper.execute(getRequest, IAHomeResource.class)).thenReturn(resource);   
    
    assertNotNull(client.get(URI, HEADERS, IAHomeResource.class));
    
    verify(wrapper).httpGetRequest(URI, HEADERS);
    verify(wrapper).execute(any(HttpGet.class), eq(IAHomeResource.class));
  }
  
  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWhenGetIsCalled() throws ClientProtocolException, IOException {
    
    HttpGet getRequest = new HttpGet();
      when(wrapper.httpGetRequest(URI, HEADERS)).thenReturn(getRequest);
      when(wrapper.execute(getRequest, IAHomeResource.class)).thenThrow(ClientProtocolException.class);
      client.get(URI, HEADERS, IAHomeResource.class);
  }
  
  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWhenGetIsCalled2() throws ClientProtocolException, IOException {
    
    HttpGet getRequest = new HttpGet();
      when(wrapper.httpGetRequest(URI, HEADERS)).thenReturn(getRequest);
      when(wrapper.execute(getRequest, IAHomeResource.class)).thenThrow(RuntimeException.class);
      client.get(URI, HEADERS, IAHomeResource.class);    
  }
  
  @Test
  public void shouldExecutePutSuccessfully() throws ClientProtocolException, IOException {
    
    HttpPut putRequest = new HttpPut();
    IAHomeResource resource = new IAHomeResource();
    when(wrapper.httpPutRequest(URI, HEADERS)).thenReturn(putRequest);
    when(wrapper.execute(putRequest, IAHomeResource.class)).thenReturn(resource);
    
    assertNotNull(client.put(URI, HEADERS, IAHomeResource.class));
    
    verify(wrapper).httpPutRequest(URI, HEADERS);
    verify(wrapper).execute(any(HttpPut.class), eq(IAHomeResource.class));
  }
  
  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWhenPutIsCalled() throws ClientProtocolException, IOException {
    
    HttpPut putRequest = new HttpPut();
    when(wrapper.httpPutRequest(URI, HEADERS)).thenReturn(putRequest);
    when(wrapper.execute(putRequest, IAHomeResource.class)).thenThrow(ClientProtocolException.class);
    client.put(URI, HEADERS, IAHomeResource.class);
  }
  
  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWhenPutIsCalled2() throws ClientProtocolException, IOException {
    
    HttpPut putRequest = new HttpPut();
    when(wrapper.httpPutRequest(URI, HEADERS)).thenReturn(putRequest);
    when(wrapper.execute(putRequest, IAHomeResource.class)).thenThrow(RuntimeException.class);
    client.put(URI, HEADERS, IAHomeResource.class);    
  }
  
  @Test
  public void shouldExecutePostSuccessfully() throws ClientProtocolException, IOException {
    
    HttpPost postRequest = new HttpPost();
    ReceptionResponse resource = new ReceptionResponse();
    
    when(wrapper.httpPostRequest(URI, HEADERS)).thenReturn(postRequest);
    when(wrapper.execute(postRequest, ReceptionResponse.class)).thenReturn(resource);
    
    String source = "This is the source of my input stream";
    InputStream in = IOUtils.toInputStream(source, "UTF-8");
    
    assertNotNull(client.post(URI, HEADERS, "This is a test message", in, ReceptionResponse.class));
    
    verify(wrapper).httpPostRequest(URI, HEADERS);
    verify(wrapper).execute(any(HttpPost.class), eq(ReceptionResponse.class));
  }
  
  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWhenPostIsCalled() throws ClientProtocolException, IOException {
    
    HttpPost postRequest = new HttpPost();
    String source = "This is the source of my input stream";
    InputStream in = IOUtils.toInputStream(source, "UTF-8");
    
    when(wrapper.httpPostRequest(URI, HEADERS)).thenReturn(postRequest);
    when(wrapper.execute(postRequest, ReceptionResponse.class)).thenThrow(ClientProtocolException.class);
    client.post(URI, HEADERS, "This is a test message", in, ReceptionResponse.class);
  }
  
  @SuppressWarnings("unchecked")
  @Test (expected = RuntimeException.class)
  public void shouldThrowExceptionWhenPostIsCalled2() throws ClientProtocolException, IOException {
    
    HttpPost postRequest = new HttpPost();
    String source = "This is the source of my input stream";
    InputStream in = IOUtils.toInputStream(source, "UTF-8");
    
    when(wrapper.httpPostRequest(URI, HEADERS)).thenReturn(postRequest);
    when(wrapper.execute(postRequest, ReceptionResponse.class)).thenThrow(RuntimeException.class);
    client.post(URI, HEADERS, "This is a test message", in, ReceptionResponse.class);    
  }
 
  @Test
  public void shouldCloseHttpClientConnection() {
    client.close();
    verify(wrapper).close();
  }
  
}