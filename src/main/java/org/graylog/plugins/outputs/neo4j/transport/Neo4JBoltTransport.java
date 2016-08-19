package org.graylog.plugins.outputs.neo4j.transport;

import org.graylog.plugins.outputs.neo4j.Neo4jOutput;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dev on 16/08/16.
 */
public class Neo4JBoltTransport implements INeo4jTransport {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4JBoltTransport.class);

    private Driver driver;
    private Configuration configuration;
    private String parsedCreateQery = null;
    List<String> fields;


    public Neo4JBoltTransport(Configuration config) throws MessageOutputConfigurationException {


        configuration = config;
        fields = new LinkedList<String>();;
        Session session = null;

        try {
            driver = GraphDatabase.driver( config.getString(Neo4jOutput.CK_NEO4J_URL),
                    AuthTokens.basic(config.getString(Neo4jOutput.CK_NEO4J_USER), config.getString(Neo4jOutput.CK_NEO4J_PASSWORD)) );
            session = driver.session();

            //run initialization query only once
            String createQueryOnce = config.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY);

            if (createQueryOnce.length() > 0)
                session.run(createQueryOnce);
        }
        catch (ClientException e){
            throw new MessageOutputConfigurationException("Malformed neo4j configuration: " + e );
        }
        finally {
            session.close();
        }
        //get message fields needed by cypher query
        String createQuery = config.getString(Neo4jOutput.CK_NEO4J_QUERY);
        LOG.debug("Bolt protocol, create query: " + createQuery);

        Matcher m = Pattern.compile("\\{([^{}]*)\\}").matcher(createQuery);
        while (m.find()) {
            fields.add(m.group(1));
            LOG.debug("Found field in cypher statement: " + m.group(1));
        }
        LOG.info("Identified " + fields.size() + " fields in graph create query.");

        parsedCreateQery = parseQuery(createQuery);


    }

    private void postQuery(String query, Map<String, Object> mapping) {
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

    private String parseQuery(String queryString) {

        queryString = queryString.replace("\n", " ");
        queryString = queryString.replace("\t", " ");
        queryString = queryString.replace("\r", "");
        queryString = queryString.replace(";", "");

        return queryString;
    }

    @Override
    public void send(Message message) throws InterruptedException {

        HashMap<String, Object> convertedFields = new HashMap<String, Object>(){};

        for (String field : fields){
            if (message.hasField(field)) {
                Object valueForField = message.getField(field);
                convertedFields.put(field, String.valueOf(valueForField));
            }
            else
            {
                convertedFields.put(field, null);
            }
        }
            postQuery(parsedCreateQery, convertedFields);

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
