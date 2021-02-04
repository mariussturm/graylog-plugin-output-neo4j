package org.graylog.plugins.outputs.neo4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jStatement {

	private static final Logger LOG = LoggerFactory.getLogger(Neo4jStatement.class);
	private static final Pattern CYPHER_PARAMETER_REGEX = Pattern.compile("\\$([a-zA-Z0-9_]+)");

	private final String statement;
	private final Collection<String> parameterNames;

	public Neo4jStatement(String statement) {
		this.statement = statement;
		Matcher m = CYPHER_PARAMETER_REGEX.matcher(statement);
		parameterNames = Collections.unmodifiableSet(extractParameterNames(m));
	}

	public String sanitizedQuery() {
		return statement.replace("\n", " ")
				.replace("\r", " ");
	}

	public Collection<String> parameterNames() {
		return parameterNames;
	}

	private Set<String> extractParameterNames(Matcher m) {
		Set<String> params = new HashSet<>();
		while (m.find()) {
			params.add(m.group(1));
			LOG.debug("Found field in cypher statement: " + m.group(1));
		}
		LOG.info("Identified " + params.size() + " fields in graph create query.");
		return params;
	}
}
