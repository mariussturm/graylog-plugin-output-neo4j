package org.graylog.plugins.outputs.neo4j.transport;

import java.util.Map;
import java.util.Objects;
import org.graylog.plugins.outputs.neo4j.Neo4jOutput;
import org.graylog.plugins.outputs.neo4j.Neo4jStatement;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dev on 16/08/16.
 */
public class Neo4JBoltTransport extends AbstractNeo4jTransport {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4JBoltTransport.class);

    private final Driver driver;

    public Neo4JBoltTransport(Configuration config) throws MessageOutputConfigurationException {

        super(new Neo4jStatement(config.getString(Neo4jOutput.CK_NEO4J_QUERY)));

        driver = GraphDatabase.driver( config.getString(Neo4jOutput.CK_NEO4J_URL),
                AuthTokens.basic(Objects.requireNonNull(config.getString(Neo4jOutput.CK_NEO4J_USER)),
                        Objects.requireNonNull(config.getString(Neo4jOutput.CK_NEO4J_PASSWORD))) );

        try (Session session = driver.session()) {

            //run initialization query only once
            String createQueryOnce = config.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY);

            if (createQueryOnce != null && createQueryOnce.length() > 0)
                session.run(createQueryOnce).consume();
        } catch (ClientException e) {
            throw new MessageOutputConfigurationException("Malformed neo4j configuration: " + e);
        }

        String createQuery = config.getString(Neo4jOutput.CK_NEO4J_QUERY);
        LOG.debug("Bolt protocol, create query: " + createQuery);
    }

     protected void postQuery(String query, Map<String, Object> mapping) {
        Session session = null;
        try {
            session = driver.session();
            session.run(query, mapping).consume();
        }
        catch (ClientException e) {
            LOG.debug("Could not push message to Graph Database: " + e.getMessage());
        }
        finally{
            if (session != null && session.isOpen())
                session.close();
        }
    }

    @Override
    public boolean trySend(Message message) {
        return false;
    }

    @Override
    public void stop() {
        driver.close();
    }
}
