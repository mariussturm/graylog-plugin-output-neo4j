package org.graylog.plugins.outputs.neo4j.transport;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;

/**
 * Created by dev on 16/08/16.

/**
 * Factory for building a {@link INeo4jTransport}.
 */
public enum Neo4jTransports {
    BOLT,
    HTTP;

    /**
     * Creates a {@link INeo4jTransport} from the given protocol and configuration.
     *
     * @param transport the transport protocol to use
     * @param config    the {@link Configuration} to pass to the transport
     * @return An initialized and started {@link INeo4jTransport}
     */
    public static INeo4jTransport create(final Neo4jTransports transport, final Configuration config) throws MessageOutputConfigurationException {
        INeo4jTransport neo4JTransport;

        switch (transport) {
            case BOLT:
                neo4JTransport = new Neo4JBoltTransport(config);
                break;
            case HTTP:
                neo4JTransport = new Neo4JHttpTransport(config);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Neo4J transport: " + transport);
        }

        return neo4JTransport;
    }

}