package org.graylog.plugins.outputs.neo4j.transport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.graylog.plugins.outputs.neo4j.Neo4jStatement;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.Test;

class AbstractNeo4jTransportTest {

	@Test
	void shouldExecuteQueryWithValidArguments() {
		Neo4jTransportStub transport = new Neo4jTransportStub(new Neo4jStatement("CREATE (t:Test {value:$value})"));
		transport.send(new Message(ImmutableMap.of("_id", "id value", "value", "foo")));
		assertTrue(transport.queryPosted);
	}

	@Test
	void shouldNotExecuteQueryWithMissingArgument() {
		Neo4jTransportStub transport = new Neo4jTransportStub(new Neo4jStatement("CREATE (t:Test {value:$value})"));
		transport.send(new Message(ImmutableMap.of("_id", "id value")));
		assertFalse(transport.queryPosted);
	}

	private static class Neo4jTransportStub extends AbstractNeo4jTransport {

		boolean queryPosted = false;

		public Neo4jTransportStub(Neo4jStatement statement) {
			super(statement);
		}

		@Override
		protected void postQuery(String queryString, Map<String, Object> parameters) {
			queryPosted = true;
		}

		@Override
		public boolean trySend(Message message) {
			return false;
		}

		@Override
		public void stop() {

		}
	}
}