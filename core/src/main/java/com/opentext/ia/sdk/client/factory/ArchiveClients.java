/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.sdk.client.factory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.opentext.ia.sdk.client.api.ArchiveClient;
import com.opentext.ia.sdk.client.api.ArchiveConnection;
import com.opentext.ia.sdk.client.api.AuthenticationStrategyFactory;
import com.opentext.ia.sdk.client.api.InfoArchiveLinkRelations;
import com.opentext.ia.sdk.client.impl.ApplicationIngestionResourcesCache;
import com.opentext.ia.sdk.client.impl.InfoArchiveRestClient;
import com.opentext.ia.sdk.dto.*;
import com.opentext.ia.sdk.server.configuration.InfoArchiveConfigurer;
import com.opentext.ia.sdk.support.NewInstance;
import com.opentext.ia.sdk.support.datetime.Clock;
import com.opentext.ia.sdk.support.datetime.DefaultClock;
import com.opentext.ia.sdk.support.http.HttpClient;
import com.opentext.ia.sdk.support.http.apache.ApacheHttpClient;
import com.opentext.ia.sdk.support.http.rest.AuthenticationStrategy;
import com.opentext.ia.sdk.support.http.rest.LinkContainer;
import com.opentext.ia.sdk.support.http.rest.RestClient;
import com.opentext.ia.sdk.support.io.RuntimeIoException;


/**
 * Factory methods for creating {@linkplain ArchiveClient}s.
 */
public final class ArchiveClients {

  private ArchiveClients() {
    // Utility class
  }

  /**
   * Returns an ArchiveClient instance and configures the InfoArchive server that it communicates with.
   * @param configurer How to configure InfoArchive
   * @return An ArchiveClient
   */
  public static ArchiveClient configuringServerUsing(InfoArchiveConfigurer configurer) {
    return configuringServerUsing(configurer, null);
  }

  /**
   * Returns an ArchiveClient instance and configures the InfoArchive server that it communicates with.
   * @param configurer How to configure InfoArchive
   * @param restClient The REST client to use for communication with the server
   * @return An ArchiveClient
   */
  public static ArchiveClient configuringServerUsing(InfoArchiveConfigurer configurer, RestClient restClient) {
    return configuringServerUsing(configurer, restClient, null);
  }

  /**
   * Returns an ArchiveClient instance and configures the InfoArchive server that it communicates with.
   * @param configurer How to configure InfoArchive
   * @param optionalClient The REST client to use for communication with the server
   * @param optionalClock The clock to use
   * @return An ArchiveClient
   */
  public static ArchiveClient configuringServerUsing(InfoArchiveConfigurer configurer, RestClient optionalClient,
      Clock optionalClock) {
    ArchiveConnection connection = configurer.getArchiveConnection();
    Clock clock = Optional.ofNullable(optionalClock).orElseGet(DefaultClock::new);
    RestClient client = Optional.ofNullable(optionalClient).orElseGet(
        () -> createRestClient(connection, clock));
    configurer.configure();
    return usingAlreadyConfiguredServer(client, connection, configurer.getApplicationName(), clock);
  }

  private static RestClient createRestClient(ArchiveConnection connection, Clock clock) {
    HttpClient httpClient = NewInstance.of(connection.getHttpClientClassName(),
        ApacheHttpClient.class.getName()).as(HttpClient.class);
    AuthenticationStrategy authentication = new AuthenticationStrategyFactory(connection).getAuthenticationStrategy(
        () -> httpClient, () -> clock);
    RestClient result = new RestClient(httpClient);
    result.init(authentication);
    return result;
  }

  /**
   * Creates a new ArchiveClient instance without installing any artifacts in the archive.
   * @param restClient The RestClient used to interact with the InfoArchive REST API.
   * @param connection How to communicate with the InfoArchive Server
   * @param applicationName The name of the already configured application to use
   * @return An ArchiveClient
   */
  public static ArchiveClient usingAlreadyConfiguredServer(RestClient restClient, ArchiveConnection connection,
      String applicationName) {
    return usingAlreadyConfiguredServer(restClient, connection, applicationName, new DefaultClock());
  }

  private static ArchiveClient usingAlreadyConfiguredServer(RestClient restClient, ArchiveConnection connection,
      String applicationName, Clock clock) {
    return new InfoArchiveRestClient(restClient, appResourceCache(restClient, connection, applicationName), clock);
  }

  private static ApplicationIngestionResourcesCache appResourceCache(RestClient restClient,
      ArchiveConnection connection, String applicationName) {
    try {
      ApplicationIngestionResourcesCache resourceCache = new ApplicationIngestionResourcesCache(
          applicationName);
      Services services = restClient.get(connection.getBillboardUri(), Services.class);
      Tenant tenant = getTenant(restClient, services);
      Application application = getApplication(restClient, tenant, applicationName);
      cacheResourceUris(restClient, application, resourceCache);
      return resourceCache;
    } catch (IOException e) {
      throw new RuntimeIoException(e);
    }
  }

  private static Tenant getTenant(RestClient restClient, Services services) throws IOException {
    return Objects.requireNonNull(restClient.follow(services, InfoArchiveLinkRelations.LINK_TENANT, Tenant.class),
        "Tenant not found.");
  }

  private static Application getApplication(RestClient restClient, Tenant tenant, String applicationName)
      throws IOException {
    Applications applications = restClient.follow(tenant, InfoArchiveLinkRelations.LINK_APPLICATIONS,
        Applications.class);
    return Objects.requireNonNull(applications.byName(applicationName),
        "Application named " + applicationName + " not found.");
  }

  private static void cacheResourceUris(RestClient restClient,
      Application application, ApplicationIngestionResourcesCache resourceCache) throws IOException {
    Aics aics = restClient.follow(application, InfoArchiveLinkRelations.LINK_AICS, Aics.class);
    LinkContainer aips = restClient.follow(application, InfoArchiveLinkRelations.LINK_AIPS, LinkContainer.class);

    Map<String, String> dipResourceUriByAicName = new HashMap<>();
    aics.getItems()
        .forEach(aic -> dipResourceUriByAicName.put(aic.getName(), aic.getUri(InfoArchiveLinkRelations.LINK_DIP)));
    resourceCache.setDipResourceUriByAicName(dipResourceUriByAicName);
    resourceCache.setCiResourceUri(application.getUri(InfoArchiveLinkRelations.LINK_CI));
    resourceCache.setAipResourceUri(application.getUri(InfoArchiveLinkRelations.LINK_AIPS));
    resourceCache.setAipIngestDirectResourceUri(aips.getUri(InfoArchiveLinkRelations.LINK_INGEST_DIRECT));
  }

  /**
   * Creates a new ArchiveClient instance without installing any artifacts in the archive using the default RestClient.
   * @param connection How to communicate with the server
   * @param applicationName The name of the already configured application to use
   * @return An ArchiveClient
   */
  public static ArchiveClient usingAlreadyConfiguredServer(ArchiveConnection connection, String applicationName) {
    Clock clock = new DefaultClock();
    RestClient restClient = createRestClient(connection, clock);
    return usingAlreadyConfiguredServer(restClient, connection, applicationName, clock);
  }

}