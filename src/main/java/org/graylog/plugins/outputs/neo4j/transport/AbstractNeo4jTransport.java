package org.graylog.plugins.outputs.neo4j.transport;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.graylog.plugins.outputs.neo4j.Neo4jStatement;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNeo4jTransport  implements INeo4jTransport {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractNeo4jTransport.class);

	private final Neo4jStatement statement;

	public AbstractNeo4jTransport(Neo4jStatement statement) {
		this.statement = statement;
	}


	@Override
	public void send(Message message) {

		Collection<String> missingFieldNames = statement.parameterNames().stream()
				.filter(it -> !message.hasField(it))
				.collect(Collectors.toSet());
		if (!missingFieldNames.isEmpty()) {
			LOG.warn("Unable to execute query because of missing parameters in context : {}", missingFieldNames);
			return;
		}

		Map<String, Object> convertedFields = message.getFields().entrySet().stream()
				.filter(it -> statement.parameterNames().contains(it.getKey()))
				.collect(toMap(Entry::getKey, it -> String.valueOf(it.getValue())));

		postQuery(statement.sanitizedQuery(), convertedFields);
	}

	protected abstract void postQuery(String queryString, Map<String, Object> parameters);
}
