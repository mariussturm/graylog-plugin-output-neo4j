package org.graylog.plugins.outputs.neo4j.transport;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;

/**
 * Created by dev on 16/08/16.
 */
public class Neo4JBoltTransport implements INeo4jTransport {
    public Neo4JBoltTransport(Configuration config) {
    }

    @Override
    public void send(Message message) throws InterruptedException {

    }

    @Override
    public boolean trySend(Message message) {
        return false;
    }

    @Override
    public void stop() {

    }
}
