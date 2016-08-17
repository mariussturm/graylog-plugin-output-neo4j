package org.graylog.plugins.outputs.neo4j.transport;

import org.graylog.plugins.outputs.neo4j.Neo4jOutput;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.neo4j.driver.v1.*;
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
        try {
            driver = GraphDatabase.driver( config.getString(Neo4jOutput.CK_NEO4J_URL),
                    AuthTokens.basic(config.getString(Neo4jOutput.CK_NEO4J_USER), config.getString(Neo4jOutput.CK_NEO4J_PASSWORD)) );
            Session session = driver.session();

            //run initialization query only once
            String createQueryOnce = config.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY);

            if (createQueryOnce.length() > 0)
                session.run(createQueryOnce);
            session.close();
        }
        catch (Exception e){
            throw new MessageOutputConfigurationException("Malformed neo4j configuration: " + e );
        }
        //get message fields needed by cypher query
        String createQuery = config.getString(Neo4jOutput.CK_NEO4J_QUERY);
        LOG.info("Bolt protocol, create query: " + createQuery);

        Matcher m = Pattern.compile("\\{([^{}]*)\\}").matcher(createQuery);
        while (m.find()) {
            fields.add(m.group(1));
            LOG.info("Found field: " + m.group(1));
        }
        LOG.info("Identified " + fields.size() + " fields in graph create query: ");

        parsedCreateQery = parseQuery(createQuery);


    }

    private void postQuery(String query, Map<String, Object> mapping) {
        Session session = null;
        try {
            session = driver.session();
            session.run(query, mapping);
        }
        catch (Exception e) {
            LOG.info("Could not push message to Graph Database: " + e.getMessage());
        }
        finally{
            if (session != null)
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
        //TODO: only convert field values if necessary
        //identify relevant messages (message need at least one field of given cypher query) and convert values to string if exist
        Boolean isMessageRelevant = false;

        for (String field : fields){
            if (message.hasField(field)) {
                Object valueForField = message.getField(field);
                if (valueForField != null) {
                    isMessageRelevant = true;
                    convertedFields.put(field, String.valueOf(valueForField));
                }
            }
            else{
                convertedFields.put(field, null);
            }
        }
        if (isMessageRelevant){
            postQuery(parsedCreateQery, convertedFields);

        }
        else{
            LOG.debug("Message skipped: " + message.getMessage().toString());
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
