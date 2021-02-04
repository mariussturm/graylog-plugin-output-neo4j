package org.graylog.plugins.outputs.neo4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class Neo4jStatementTest {

	@Test
	void shouldRemoveNewLine() {

		Neo4jStatement statement = new Neo4jStatement("\n");
		assertEquals(" ", statement.sanitizedQuery());
	}

	@Test
	void shouldRemoveCarriageReturn() {

		Neo4jStatement statement = new Neo4jStatement("\r");
		assertEquals(" ", statement.sanitizedQuery());
	}

	@Test
	void shouldExtractParameterNames() {

		Neo4jStatement statement = new Neo4jStatement("blah");
		assertEquals(0, statement.parameterNames().size());

		statement = new Neo4jStatement("blah $foo");
		assertEquals(1, statement.parameterNames().size());
		assertEquals(Collections.singleton("foo"), statement.parameterNames());

		statement = new Neo4jStatement("blah $foo $bar");
		Collection<String> expected = new HashSet<>();
		expected.add("foo");
		expected.add("bar");
		assertEquals(expected, statement.parameterNames());

		statement = new Neo4jStatement("blah $foo_bar");
		assertEquals("foo_bar", statement.parameterNames().iterator().next());
	}
}