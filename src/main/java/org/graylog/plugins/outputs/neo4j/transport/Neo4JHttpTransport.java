package org.graylog.plugins.outputs.neo4j.transport;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.graylog.plugins.outputs.neo4j.Neo4jOutput;
import org.graylog.plugins.outputs.neo4j.Neo4jStatement;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dev on 16/08/16.
 */
public class Neo4JHttpTransport extends AbstractNeo4jTransport implements INeo4jTransport {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4JHttpTransport.class);

    private final WebTarget cypher;
    private final Configuration configuration;

    public Neo4JHttpTransport(Configuration config) throws MessageOutputConfigurationException {

        super(new Neo4jStatement(config.getString(Neo4jOutput.CK_NEO4J_QUERY)));

        this.configuration = config;
        URI databaseUri;
        try {
            URL baseUrl = new URL(Objects.requireNonNull(configuration.getString(Neo4jOutput.CK_NEO4J_URL)));
            databaseUri = new URL(baseUrl, StringUtils.removeEnd(baseUrl.getPath(), "/") + "").toURI();
        } catch (URISyntaxException e) {
            throw new MessageOutputConfigurationException("Syntax error in neo4j URL");
        } catch (MalformedURLException e) {
            throw new MessageOutputConfigurationException("Malformed neo4j URL: " + e );
        }

        HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic
                (configuration.getString(Neo4jOutput.CK_NEO4J_USER), configuration.getString(Neo4jOutput.CK_NEO4J_PASSWORD));

        Client client = ClientBuilder.newClient();
        client.register(auth);
        cypher = client.target(databaseUri);
        String startupQuery = configuration.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY);
        if (startupQuery != null && !startupQuery.isEmpty()) {
            String queryString = new Neo4jStatement(configuration.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY)).sanitizedQuery();
            postQuery(queryString, Collections.emptyMap());
        }
    }

    protected void postQuery(String query, Map<String, Object> parameters) {

        Map<String, List<Map<String, Object>>> payload = buildPayload(query, parameters);
        Response response = cypher
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON), Response.class);

        String result = String.format(
                "POST [%s] to [%s], status code [%d], headers: %s, returned data: %s",
                payload, configuration.getString(Neo4jOutput.CK_NEO4J_URL), response.getStatus(), response.getHeaders(),
                response);
        if (response.getStatus() >= 400) {
            LOG.info(result);
        } else {
            LOG.debug(result);
        }
    }

    private Map<String, List<Map<String, Object>>> buildPayload(String queryString, Map<String, Object> parameters) {

        // the JSON payload looks like
//        {
//            "statements" : [ {
//                    "statement" : "MATCH (n) WHERE ID(n) = $nodeId RETURN n",
//                    "parameters" : {
//                      "nodeId" : 5
//                    }
//            } ]
//        }

        Map<String, Object> statementWithParameters = new HashMap<>();
        statementWithParameters.put("statement", queryString);
        statementWithParameters.put("parameters", parameters);
        Map<String, List<Map<String, Object>>> payload = new HashMap<>();
        payload.put("statements", Collections.singletonList(statementWithParameters));
        return payload;
    }

    @Override
    public boolean trySend(Message message) {
        return false;
    }

    @Override
    public void stop() {

    }
}
