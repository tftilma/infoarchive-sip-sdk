/*
 * Copyright (c) 2016-2017 by OpenText Corporation. All Rights Reserved.
 */
package com.opentext.ia.yaml.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.atteo.evo.inflector.English;
import org.junit.Test;

import com.opentext.ia.test.TestCase;
import com.opentext.ia.yaml.core.Value;
import com.opentext.ia.yaml.core.YamlMap;
import com.opentext.ia.yaml.resource.ResourceResolver;


public class WhenUsingYamlConfiguration extends TestCase {

  private static final String FILE_SYSTEM_FOLDERS = "fileSystemFolders";
  private static final String NAME = "name";
  private static final String TYPE = "type";
  private static final String DEFAULT = "default";
  private static final String CONTENT = "content";
  private static final String FORMAT = "format";
  private static final String XML = "xml";
  private static final String RESOURCE = "resource";
  private static final String TEXT = "text";
  private static final String TENANTS = "tenants";
  private static final String TENANT = "tenant";
  private static final String APPLICATIONS = "applications";
  private static final String SPACES = "spaces";
  private static final String HOLDINGS = "holdings";
  private static final String CONFIRMATIONS = "confirmations";
  private static final String NAMESPACES = "namespaces";
  private static final String NAMESPACE = "namespace";
  private static final String PREFIX = "prefix";
  private static final String URI = "uri";
  private static final String QUERIES = "queries";
  private static final String XDB_PDI_CONFIGS = "xdbPdiConfigs";
  private static final String OPERANDS = "operands";
  private static final String INGESTS = "ingests";
  private static final String INGEST = "ingest";
  private static final String START_PROCESSOR = "  <processor>\n";
  private static final String END_PROCESSOR = "  </processor>\n";
  private static final String PDIS = "pdis";
  private static final String DATA = "data";
  private static final String INDEXES = "indexes";
  private static final String PATH_VALUE_INDEX = "path.value.index";
  private static final String PATH = "path";

  private final YamlMap yaml = new YamlMap();
  private ResourceResolver resourceResolver = ResourceResolver.none();

  @Test
  public void shouldInlineResources() throws Exception {
    String expected = someName();
    resourceResolver = name -> expected;
    String singularType = someType();
    String pluralType = English.plural(someType());
    String resource = someName() + ".txt";
    yaml.put(singularType, Arrays.asList(externalContentTo(resource)));
    yaml.put(pluralType, externalContentTo(resource));
    String nonContent = English.plural(someName());
    yaml.put(nonContent, Arrays.asList(new YamlMap().put(CONTENT, Arrays.asList(externalResourceTo(resource)))));

    normalizeYaml();

    assertContentIsInlined("list", expected, yaml.get(singularType, 0));
    assertContentIsInlined("map", expected, yaml.get(pluralType));
    assertValue("Invalid content structure inlined\n" + yaml, resource, yaml.get(nonContent, 0, CONTENT, 0, RESOURCE));
  }

  private String someName() {
    return randomString(5);
  }

  private String someType() {
    return randomString(8);
  }

  private YamlMap externalResourceTo(String resource) {
    return new YamlMap().put(RESOURCE, resource);
  }

  private YamlMap externalContentTo(String resource) {
    return new YamlMap()
        .put(NAME, someName())
        .put(CONTENT, externalResourceTo(resource));
  }

  private void assertContentIsInlined(String type, String expected, Value owner) {
    assertValue("Content in " + type + " not inlined:\n" + yaml, expected,
        owner.toMap().get(CONTENT, TEXT));
  }

  private void normalizeYaml() {
    new YamlConfiguration(yaml, resourceResolver);
  }

  private void assertValue(String message, String expected, Value actual) {
    assertEquals(message, expected, actual.toString());
  }

  @Test
  public void shouldAddDefaultVersionWhenNotSpecified() throws Exception {
    normalizeYaml();

    assertValue("Default version", "1.0.0", yaml.get("version"));
  }

  @Test
  public void shouldNotOverwriteSpecifiedVersion() throws Exception {
    YamlConfiguration configuration = new YamlConfiguration("version: 2.0.0");

    assertValue("Version", "2.0.0", configuration.getMap().get("version"));
  }

  @Test
  public void shouldReplaceSingularTopLevelObjectWithSequence() throws IOException {
    String name = someName();
    String type = "application";
    String otherType = someType();
    String value = someName();
    yaml.put(type, new YamlMap().put(NAME, name))
        .put(otherType, Arrays.asList(value));

    normalizeYaml();

    assertValue("Name", name, yaml.get(English.plural(type), 0, NAME));
    assertValue("Should not be changed", value, yaml.get(otherType, 0));
  }

  @Test
  public void shouldReplaceTopLevelMapOfMapsWithSequence() {
    String name = someName();
    yaml.put(APPLICATIONS, new YamlMap().put(name, new YamlMap().put(TYPE, "ACTIVE_ARCHIVING")));

    normalizeYaml();

    assertValue("Application", name, yaml.get(APPLICATIONS, 0, "name"));
  }

  @Test
  public void shouldConvertEnumValue() {
    yaml.put(APPLICATIONS, Arrays.asList(new YamlMap().put(TYPE, "active archiving")));
    yaml.put(CONFIRMATIONS, Arrays.asList(new YamlMap().put("types", Arrays.asList("receipt", "invalid"))));

    normalizeYaml();

    assertValue(TYPE, "ACTIVE_ARCHIVING", yaml.get(APPLICATIONS, 0, TYPE));
    assertValue("Types", "RECEIPT", yaml.get(CONFIRMATIONS, 0, "types", 0));
  }

  @Test
  public void shouldInsertDefaultReferences() {
    String tenant = someName();
    String application = someName();
    String space = someName();
    String spaceRootFolder = someName();
    String fileSystemRoot = someName();
    String fileSystemFolder = someName();
    yaml.put(TENANTS, Arrays.asList(new YamlMap().put(NAME, tenant)));
    yaml.put(APPLICATIONS, Arrays.asList(
        new YamlMap().put(NAME, someName()),
        new YamlMap().put(NAME, application)
            .put(DEFAULT, true)));
    yaml.put(SPACES, Arrays.asList(new YamlMap().put(NAME, space)));
    yaml.put("spaceRootFolders", Arrays.asList(new YamlMap().put(NAME, spaceRootFolder)));
    yaml.put("fileSystemRoots", Arrays.asList(new YamlMap().put(NAME, fileSystemRoot)));
    yaml.put(FILE_SYSTEM_FOLDERS, Arrays.asList(new YamlMap().put(NAME, fileSystemFolder)));

    normalizeYaml();

    assertValue("Tenant", tenant, yaml.get(APPLICATIONS, 0, TENANT));
    assertValue("Application", application, yaml.get(SPACES, 0, "application"));
    assertValue("Space root folder", spaceRootFolder, yaml.get(FILE_SYSTEM_FOLDERS, 0, "parentSpaceRootFolder"));
  }

  @Test
  public void shouldNotInsertDefaultForExplicitNull() {
    yaml.put(TENANTS, Arrays.asList(new YamlMap().put(NAME, someName())));
    yaml.put(APPLICATIONS, Arrays.asList(new YamlMap()
        .put(NAME, someName())
        .put(TENANT, null)));

    normalizeYaml();

    assertTrue("Explicit null is overridden with default", yaml.get(APPLICATIONS, 0, TENANT).isEmpty());
  }

  @Test
  public void shouldInsertDefaultValues() {
    yaml.put("exportPipelines", Arrays.asList(new YamlMap().put(NAME, someName())));
    yaml.put(HOLDINGS, Arrays.asList(new YamlMap().put(NAME, someName())));
    yaml.put(INGESTS, Arrays.asList(new YamlMap().put(NAME, someName())));
    yaml.put("receiverNodes", Arrays.asList(new YamlMap().put(NAME, someName())));

    normalizeYaml();

    assertTrue("exportPipeline.includesContent", yaml.get("exportPipelines", 0, "includesContent").toBoolean());
    assertValue("holding.xdbMode", "PRIVATE", yaml.get(HOLDINGS, 0, "xdbMode"));
    assertValue("ingest.processors.format", XML, yaml.get(INGESTS, 0, "content", FORMAT));
    assertTrue("ingest.processors.xml", yaml.get(INGESTS, 0, "content", TEXT).toString().contains("sip.download"));
    assertValue("receiverNode.sips.format", "sip_zip", yaml.get("receiverNodes", 0, "sips", 0, FORMAT));
  }

  @Test
  public void shouldReplaceSingularObjectReferenceWithSequenceForReferenceCollections() throws IOException {
    String name = someName();
    yaml.put(HOLDINGS, Arrays.asList(new YamlMap().put(NAME, name)))
        .put(CONFIRMATIONS, Arrays.asList(new YamlMap().put("holding", name)));

    normalizeYaml();

    assertValue("Sequence of references not created", name, yaml.get(CONFIRMATIONS, 0, HOLDINGS, 0));
    assertFalse("Singular reference not removed", yaml.get(CONFIRMATIONS, 0).toMap().containsKey("holding"));
  }

  @Test
  public void shouldReplaceNestedMapOfMapsWithSequence() throws Exception {
    String query = someName();
    String operand = someName();
    String path = someName();
    yaml.put(QUERIES, Arrays.asList(new YamlMap()
        .put(NAME, query)
        .put(XDB_PDI_CONFIGS, new YamlMap()
            .put(OPERANDS, new YamlMap()
                .put(operand, new YamlMap()
                    .put(PATH, path))))));

    normalizeYaml();

    assertValue("Name", query, yaml.get(QUERIES, 0, NAME));
    assertValue("Operand", operand, yaml.get(QUERIES, 0, XDB_PDI_CONFIGS, OPERANDS, 0, NAME));
    assertValue("Path", path, yaml.get(QUERIES, 0, XDB_PDI_CONFIGS, OPERANDS, 0, PATH));
  }

  @Test
  public void shouldAddNamespaceDeclarationsToXquery() throws Exception {
    String prefix = "n";
    String uri = randomUri();
    String text = "current-dateTime()";
    yaml.put(NAMESPACES, Arrays.asList(new YamlMap()
            .put(PREFIX, prefix)
            .put(URI, uri)))
        .put("xdbLibraryPolicies", Arrays.asList(new YamlMap()
            .put(NAME, someName())
            .put("closeHintDateQuery", new YamlMap()
                .put(TEXT, text))));

    normalizeYaml();

    assertValue("Query", String.format("declare namespace %s = \"%s\";%n%s", prefix, uri, text),
        yaml.get("xdbLibraryPolicies", 0, "closeHintDateQuery"));
  }

  @Test
  public void shouldReplacePdiSchemaNamespaceWithName() throws Exception {
    String prefix = "n";
    String uri = randomUri();
    yaml.put(NAMESPACES, Arrays.asList(new YamlMap()
            .put(PREFIX, prefix)
            .put(URI, uri)))
        .put("pdiSchemas", Arrays.asList(new YamlMap()
            .put(CONTENT, new YamlMap()
                .put(FORMAT, XML))));

    normalizeYaml();

    assertValue("Name\n" + yaml, uri, yaml.get("pdiSchemas", 0, NAME));
    assertTrue("Leaves namespace:\n" + yaml, yaml.get("pdiSchemas", 0, NAMESPACE).isEmpty());
  }

  @Test
  public void shouldTranslatePdiYamlToXml() {
    String prefix1 = "n";
    String uri1 = randomUri();
    String prefix2 = "ex";
    String uri2 = randomUri();
    yaml.put(NAMESPACES, Arrays.asList(new YamlMap()
            .put(PREFIX, prefix1)
            .put(URI, uri1),
        new YamlMap()
            .put(PREFIX, prefix2)
            .put(URI, uri2)))
        .put(PDIS, Arrays.asList(new YamlMap()
            .put(NAME, someName())
            .put(CONTENT, new YamlMap()
                .put(FORMAT, "yaml")
                .put(DATA, Arrays.asList(new YamlMap()
                    .put("id", "pdi.index.creator")
                    .put("key.document.name", "xdb.pdi.name")
                    .put(INDEXES, Arrays.asList(new YamlMap()
                        .put(someName(), new YamlMap()
                            .put(TYPE, PATH_VALUE_INDEX)
                            .put(PATH, "/n:gnu/n:gnat")), new YamlMap()
                        .put(someName(), new YamlMap()
                            .put(TYPE, PATH_VALUE_INDEX)
                            .put(PATH, "/n:foo/n:bar[n:baz]")), new YamlMap()
                        .put(someName(), new YamlMap()
                            .put(TYPE, "full.text.index")))),
                new YamlMap()
                    .put("id", "pdi.transformer")
                    .put("result.schema", prefix2)
                    .put("level", 2))))));

    normalizeYaml();

    String xml = yaml.get(PDIS, 0, CONTENT, TEXT).toString();
    assertTrue("path #1", xml.contains(String.format("/{%1$s}gnu/{%1$s}gnat", uri1)));
    assertTrue("path #2", xml.contains(String.format("/{%1$s}foo/{%1$s}bar[{%1$s}baz]", uri1)));
    assertTrue("Default compressed", xml.contains("<compressed>false</compressed>"));
    assertTrue("Default filter.english.stop.words", xml.contains("<filter.english.stop.words>false</filter.english.stop.words>"));
    assertTrue("Schema", xml.contains(String.format("<result.schema>%s</result.schema>", uri2)));
  }

  @Test
  public void shouldTranslateResultConfigurationHelperYamlToXml() {
    yaml.put(NAMESPACES, Arrays.asList(
        new YamlMap()
            .put(PREFIX, "n")
            .put(URI, "urn:eas-samples:en:xsd:phonecalls.1.0")
            .put(DEFAULT, true),
        new YamlMap()
            .put(PREFIX, "pdi")
            .put(URI, "urn:x-emc:ia:schema:pdi")))
        .put("resultConfigurationHelper", new YamlMap()
            .put(NAME, "PhoneCalls-result-configuration-helper")
            .put("propagateChanges", false)
            .put(CONTENT, new YamlMap()
                .put(FORMAT, "yaml")
                .put(NAMESPACES, Arrays.asList("n", "pdi"))
                .put(DATA, Arrays.asList(
                    new YamlMap()
                        .put("id", new YamlMap()
                            .put("label", "ID")
                            .put(PATH, "@pdi:id")
                            .put(TYPE, "id")),
                    new YamlMap()
                        .put("SentToArchiveDate", new YamlMap()
                            .put("label", "Sent to")
                            .put(PATH, "n:SentToArchiveDate")
                            .put(TYPE, "date time"))))));

    normalizeYaml();

    YamlMap content = yaml.get("resultConfigurationHelpers", 0, CONTENT).toMap();
    assertValue("Format", XML, content.get(FORMAT));
    assertTrue("Namespaces are still there", content.get(NAMESPACES).isEmpty());
    String xml = content.get(TEXT).toString();
    assertEquals("XML",
        "<resultConfigurationHelper xmlns:n=\"urn:eas-samples:en:xsd:phonecalls.1.0\" xmlns:pdi=\"urn:x-emc:ia:schema:pdi\">\n"
        + "  <element>\n"
        + "    <label>ID</label>\n"
        + "    <name>id</name>\n"
        + "    <path>@pdi:id</path>\n"
        + "    <type>ID</type>\n"
        + "  </element>\n"
        + "  <element>\n"
        + "    <label>Sent to</label>\n"
        + "    <name>SentToArchiveDate</name>\n"
        + "    <path>n:SentToArchiveDate</path>\n"
        + "    <type>DATE_TIME</type>\n"
        + "  </element>\n"
        + "</resultConfigurationHelper>\n", xml);
  }


  @Test
  public void shouldTranslateIngestYamlToXml() {
    yaml.put(NAMESPACES, Arrays.asList(new YamlMap()
            .put(PREFIX, "ri")
            .put(URI, "urn:x-emc:ia:schema:ri")))
        .put(INGEST, new YamlMap()
            .put(NAME, "PhoneCalls-ingest")
            .put(CONTENT, new YamlMap()
                .put(FORMAT, "yaml")
                .put("processors", Arrays.asList(
                    new YamlMap()
                        .put("id", "sip.download"),
                    new YamlMap()
                        .put("id", "pdi.index.creator")
                        .put(DATA, new YamlMap()
                            .put("key.document.name", "xdb.pdi.name")
                            .put(INDEXES, null)),
                    new YamlMap()
                        .put("id", "ri.index")
                        .put(DATA, new YamlMap()
                            .put("key.document.name", "xdb.ri.name")
                            .put(INDEXES, new YamlMap()
                                .put("key", new YamlMap()
                                    .put(TYPE, PATH_VALUE_INDEX)
                                    .put(PATH, "/ri:ris/ri:ri[@key<STRING>]")))),
                    new YamlMap()
                        .put("id", "ci.hash")
                        .put(DATA, new YamlMap()
                            .put("select.query", new YamlMap()
                                .put(NAMESPACE, "ri")
                                .put(TEXT, "let $uri := replace(document-uri(.), '\\.pdi$', '.ri')\n"
                                    + "for $c in doc($uri)/ri:ris/ri:ri\n"
                                    + "return <content filename=\"{ $c/@key }\">\n"
                                    + "  <hash encoding=\"hex\" algorithm=\"SHA-1\" provided=\"false\" />\n"
                                    + "</content>")))))));

    normalizeYaml();

    YamlMap content = yaml.get(INGESTS, 0, CONTENT).toMap();
    assertValue("Format", XML, content.get(FORMAT));
    String xml = content.get(TEXT).toString();
    assertEquals("XML", "<processors>\n"
        + START_PROCESSOR
        + "    <class>com.emc.ia.ingestion.processor.downloader.SipContentDownloader</class>\n"
        + "    <id>sip.download</id>\n"
        + "    <name>SIP downloader processor</name>\n"
        + END_PROCESSOR
        + START_PROCESSOR
        + "    <class>com.emc.ia.ingestion.processor.index.IndexesCreator</class>\n"
        + "    <data>\n"
        + "      <indexes/>\n"
        + "      <key.document.name>xdb.pdi.name</key.document.name>\n"
        + "    </data>\n"
        + "    <id>pdi.index.creator</id>\n"
        + "    <name>XDB PDI index processor</name>\n"
        + END_PROCESSOR
        + START_PROCESSOR
        + "    <class>com.emc.ia.ingestion.processor.index.IndexesCreator</class>\n"
        + "    <data>\n"
        + "      <indexes>\n"
        + "        <path.value.index>\n"
        + "          <build.without.logging>false</build.without.logging>\n"
        + "          <compressed>false</compressed>\n"
        + "          <concurrent>false</concurrent>\n"
        + "          <name>key</name>\n"
        + "          <path>/{urn:x-emc:ia:schema:ri}ris/{urn:x-emc:ia:schema:ri}ri[@key&lt;STRING>]</path>\n"
        + "          <unique.keys>true</unique.keys>\n"
        + "        </path.value.index>\n"
        + "      </indexes>\n"
        + "      <key.document.name>xdb.ri.name</key.document.name>\n"
        + "    </data>\n"
        + "    <id>ri.index</id>\n"
        + "    <name>RI XDB indexes</name>\n"
        + END_PROCESSOR
        + START_PROCESSOR
        + "    <class>com.emc.ia.ingestion.processor.content.CiHashProcessor</class>\n"
        + "    <data>\n"
        + "      <select.query><![CDATA[\n"
        + "        declare namespace ri = \"urn:x-emc:ia:schema:ri\";\n"
        + "        let $uri := replace(document-uri(.), '\\.pdi$', '.ri')\n"
        + "        for $c in doc($uri)/ri:ris/ri:ri\n"
        + "        return <content filename=\"{ $c/@key }\">\n"
        + "          <hash encoding=\"hex\" algorithm=\"SHA-1\" provided=\"false\" />\n"
        + "        </content>\n"
        + "      ]]></select.query>\n"
        + "    </data>\n"
        + "    <id>ci.hash</id>\n"
        + "    <name>CI hash generator and validator</name>\n"
        + END_PROCESSOR
        + "</processors>\n", xml);
  }

}
