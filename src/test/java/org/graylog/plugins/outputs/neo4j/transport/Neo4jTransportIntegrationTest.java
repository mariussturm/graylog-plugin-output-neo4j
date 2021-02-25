package org.graylog.plugins.outputs.neo4j.transport;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.graylog.plugins.outputs.neo4j.Neo4jOutput;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.RestApiBindings;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;

// requires Neo4j running - a container can be run easily from the docker-compose file
public class Neo4jTransportIntegrationTest {

	private static final String NEO4J_USERNAME = "neo4j";
	private static final String NEO4J_PASSWORD = "password";
	private static final String NEO4J_URL = "bolt://localhost";
	private static final String NEO4J_HTTP_URL = "http://localhost:7474/db/neo4j/tx/commit";
	// For neo4j 3.5.x, use the URL syntax is different
//	private static final String NEO4J_HTTP_URL = "http://localhost:7474/db/data/transaction/commit";
	private static final String TEST_CYPHER_QUERY = "CREATE (n:Test {someKey:$someKeyValue}) RETURN n";

	private final Driver neo4jDriver = GraphDatabase.driver(NEO4J_URL, AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));

	@BeforeEach
	void setUp() {
		neo4jDriver.session().run("MATCH (n:Test) DELETE n").consume();
	}

	@ParameterizedTest
	@ArgumentsSource(Neo4jTransportArgumentsProvider.class)
	void shouldExecuteSimpleQuery(INeo4jTransport transport) throws InterruptedException {

		Message message = new Message(ImmutableMap.of("_id", "id value", "someKeyValue", "foo"));

		transport.send(message);

		Record result = neo4jDriver.session().run("MATCH (n:Test) RETURN n").single();
		Assertions.assertEquals("foo", result.get("n").asNode().get("someKey").asString());
	}

	@ParameterizedTest
	@ArgumentsSource(Neo4jTransportArgumentsProvider.class)
	void shouldNotCreateDataWhenMissingQueryParameter(INeo4jTransport transport) throws InterruptedException {

		Message message = new Message(ImmutableMap.of("_id", "id value"));

		transport.send(message);

		Result result = neo4jDriver.session().run("MATCH (n:Test) RETURN n");
		Assertions.assertFalse(result.hasNext());
	}

	@ParameterizedTest
	@ArgumentsSource(Neo4jTransportArgumentsProvider.class)
	void shouldAllowDateTimeDataInMessage(INeo4jTransport transport) throws InterruptedException {

		DateTime now = DateTime.now();
		Message message = new Message(ImmutableMap.of("_id", "id value", "someKeyValue", now));

		transport.send(message);

		Record result = neo4jDriver.session().run("MATCH (n:Test) RETURN n").single();
		Assertions.assertEquals(now.toString(), result.get("n").asNode().get("someKey").asString());
	}

	static class Neo4jTransportArgumentsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

			Map<String, Object> configValues = new HashMap<>();
			configValues.put(Neo4jOutput.CK_PROTOCOL, "bolt");
			configValues.put(Neo4jOutput.CK_NEO4J_URL, NEO4J_URL);
			configValues.put(Neo4jOutput.CK_NEO4J_USER, NEO4J_USERNAME);
			configValues.put(Neo4jOutput.CK_NEO4J_PASSWORD, NEO4J_PASSWORD);
			configValues.put(Neo4jOutput.CK_NEO4J_STARTUP_QUERY, "RETURN 1");
			configValues.put(Neo4jOutput.CK_NEO4J_QUERY, TEST_CYPHER_QUERY);

			Configuration configuration = new Configuration(configValues);
			Neo4JBoltTransport boltTransport = getBoltTransport(configuration);

			configValues.put(Neo4jOutput.CK_NEO4J_URL, NEO4J_HTTP_URL);
			configuration = new Configuration(configValues);
			Neo4JHttpTransport httpTransport = getHttpTransport(configuration);

			return Stream.of(
					Arguments.of(boltTransport),
					Arguments.of(httpTransport)
			);
		}

		private Neo4JHttpTransport getHttpTransport(Configuration configuration) {

			// this is needed, otherwise the REST client init fails
			GuiceInjectorHolder.createInjector(Collections.singletonList(new RestApiBindings()));

			Neo4JHttpTransport httpTransport;
			try {
				httpTransport = new Neo4JHttpTransport(configuration);
			} catch (MessageOutputConfigurationException e) {
				throw new RuntimeException(e);
			}
			return httpTransport;
		}

		private Neo4JBoltTransport getBoltTransport(Configuration configuration) {
			Neo4JBoltTransport boltTransport;
			try {
				boltTransport = new Neo4JBoltTransport(configuration);
			} catch (MessageOutputConfigurationException e) {
				throw new RuntimeException(e);
			}
			return boltTransport;
		}
	}
}
