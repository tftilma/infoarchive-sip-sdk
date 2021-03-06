/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.sdk.server.configuration.yaml;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.opentext.ia.sdk.client.api.ArchiveConnection;
import com.opentext.ia.sdk.client.api.InfoArchiveLinkRelations;
import com.opentext.ia.sdk.dto.Services;
import com.opentext.ia.sdk.server.configuration.ApplicationConfigurer;
import com.opentext.ia.sdk.support.http.HttpClient;
import com.opentext.ia.sdk.support.http.rest.Link;
import com.opentext.ia.sdk.support.http.rest.RestClient;
import com.opentext.ia.test.TestCase;
import com.opentext.ia.test.TestUtil;
import com.opentext.ia.yaml.configuration.YamlConfiguration;
import com.opentext.ia.yaml.core.YamlMap;
import com.opentext.ia.yaml.resource.ResourceResolver;


public class WhenConfiguringServerUsingYaml extends TestCase implements InfoArchiveLinkRelations {

  private final HttpClient httpClient = mock(HttpClient.class);
  private final ArchiveConnection connection = new ArchiveConnection();
  private final ApplicationConfigurer clientSideConfigurer = mock(ApplicationConfigurer.class);
  private final YamlBasedApplicationConfigurer configurer = new YamlBasedApplicationConfigurer(
      new YamlConfiguration("version: 1.0.0"), (yaml, conn) -> clientSideConfigurer);

  @Before
  @SuppressWarnings("unchecked")
  public void init() throws IOException {
    connection.setRestClient(new RestClient(httpClient));
  }

  @Test
  public void shouldDeferToServerWhenItSupportsYamlConfiguration() throws Exception {
    String configurationUri = randomUri();
    Services services = new Services();
    services.getLinks().put(LINK_CONFIGURATION, new Link(configurationUri));
    when(httpClient.get(anyString(), anyObject(), eq(Services.class))).thenReturn(services);

    configurer.configure(connection);

    verify(httpClient).put(eq(configurationUri), anyObject(), eq(String.class), anyString());
    verify(clientSideConfigurer, never()).configure(anyObject());
  }

  @Test
  public void shouldConfigureFromClientWhenServerDoesntSupportsYamlConfiguration() throws Exception {
    Services services = new Services();
    when(httpClient.get(anyString(), anyObject(), eq(Services.class))).thenReturn(services);

    configurer.configure(connection);

    verify(clientSideConfigurer).configure(anyObject());
    verify(httpClient, never()).put(anyString(), anyObject(), anyObject(), anyString());
  }

  @Test
  public void shouldConvertYamlToProperties() throws IOException {
    Map<String, String> expected = loadProperties();
    Map<String, String> actual = yamlToProperties();
    assertEqual(expected, actual);
  }

  private Map<String, String> loadProperties() throws IOException {
    Map<String, String> result = new HashMap<>();
    Properties properties = new Properties();
    try (InputStream input = getClass().getResourceAsStream("/iaif/iaif.properties")) {
      properties.load(input);
    }
    for (String name : properties.stringPropertyNames()) {
      result.put(name, properties.getProperty(name));
    }
    return result;
  }

  private Map<String, String> yamlToProperties() throws IOException {
    try (InputStream input = getClass().getResourceAsStream("/iaif/iaif.yaml")) {
      YamlConfiguration configuration = new YamlConfiguration(input, ResourceResolver.fromClasspath("/iaif"));
      YamlMap yaml = configuration.getMap();
      return new YamlPropertiesMap(yaml);
    }
  }

  private void assertEqual(Map<String, String> expected, Map<String, String> actual) {
    expected.entrySet().stream()
        .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
        .forEach(e -> assertEqual(e, actual));
  }

  private void assertEqual(Entry<String, String> expected, Map<String, String> actual) {
    if (!actual.containsKey(expected.getKey())) {
      fail("Missing key: " + expected.getKey() + "\nGot:\n"
          + actual.keySet().stream().collect(Collectors.joining("\n")));
    }
    TestUtil.assertEquals(expected.getKey(), normalize(expected.getValue()), normalize(actual.get(expected.getKey())));
  }

  private List<String> normalize(String value) {
    return Arrays.asList(value
        .replaceAll("\\n\\s*", " ")
        .replaceAll("\\s+<", "<")
        .replaceAll(">\\s+", ">")
        .split(","));
  }

}
