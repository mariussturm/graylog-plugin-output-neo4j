package org.graylog.plugins.outputs.neo4j.transport;

import com.floreysoft.jmte.Engine;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.graylog.plugins.outputs.neo4j.Neo4jOutput;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Created by dev on 16/08/16.
 */
public class Neo4JHttpTransport implements INeo4jTransport {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4JHttpTransport.class);

    private WebTarget cypher;
    private Configuration configuration;

    public Neo4JHttpTransport(Configuration config) throws MessageOutputConfigurationException {

        this.configuration = config;
        URI databaseUri;
        try {
            URL baseUrl = new URL(configuration.getString(Neo4jOutput.CK_NEO4J_URL));
            databaseUri = new URL(baseUrl, StringUtils.removeEnd(baseUrl.getPath(), "/") + "/transaction/commit").toURI();
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
        if (!configuration.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY).isEmpty()) {
            postQuery(parseQuery(configuration.getString(Neo4jOutput.CK_NEO4J_STARTUP_QUERY)));
        }
    }
    private void postQuery(List<HashMap<String, String>> queries) {
        HashMap<String, List<HashMap<String, String>>> payload = new HashMap<>();
        payload.put("statements", queries);
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

    private List<HashMap<String, String>> parseQuery(String queryString) {
        List<HashMap<String, String>> query = new ArrayList<>();

        queryString = queryString.replace("\n", " ");
        queryString = queryString.replace("\t", " ");
        queryString = queryString.replace("\r", "");
        queryString = queryString.replace(";", "");

        HashMap<String, String> statement = new HashMap<>();
        statement.put("statement", queryString);
        query.add(statement);

        return query;
    }

    @Override
    public void send(Message message) throws InterruptedException {
        Iterator messageFields = message.getFields().entrySet().iterator();
        Map<String, Object> model = new HashMap<>();
        while (messageFields.hasNext()) {
            Map.Entry pair = (Map.Entry) messageFields.next();
            model.put(String.valueOf(pair.getKey()), String.valueOf(pair.getValue()));
        }
        Engine engine = new Engine();
        String queryString = engine.transform(configuration.getString(Neo4jOutput.CK_NEO4J_QUERY), model);
        List<HashMap<String, String>> query = parseQuery(queryString);

        postQuery(query);
    }

    @Override
    public boolean trySend(Message message) {
        return false;
    }

    @Override
    public void stop() {

    }
}
