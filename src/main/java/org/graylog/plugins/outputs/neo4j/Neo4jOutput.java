package org.graylog.plugins.outputs.neo4j;

import com.floreysoft.jmte.Engine;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class Neo4jOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4jOutput.class);

    private Configuration configuration;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private WebTarget cypher;

    private static final String CK_NEO4J_URL = "neo4j_url";
    private static final String CK_NEO4J_STARTUP_QUERY = "neo4j_startup_query";
    private static final String CK_NEO4J_QUERY = "neo4j_query";
    private static final String CK_NEO4J_USER = "neo4j_user";
    private static final String CK_NEO4J_PASSWORD = "neo4j_password";

    @Inject
    public Neo4jOutput(@Assisted Stream stream, @Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;

        URI databaseUri;
        try {
            URL baseUrl = new URL(configuration.getString(CK_NEO4J_URL));
            databaseUri = new URL(baseUrl, StringUtils.removeEnd(baseUrl.getPath(), "/") + "/transaction/commit").toURI();
        } catch (URISyntaxException e) {
            throw new MessageOutputConfigurationException("Syntax error in neo4j URL");
        } catch (MalformedURLException e) {
            throw new MessageOutputConfigurationException("Malformed neo4j URL");
        }

        HttpAuthenticationFeature auth = HttpAuthenticationFeature.basic
                (configuration.getString(CK_NEO4J_USER), configuration.getString(CK_NEO4J_PASSWORD));

        Client client = ClientBuilder.newClient();
        client.register(auth);
        cypher = client.target(databaseUri);
        if (! configuration.getString(CK_NEO4J_STARTUP_QUERY).isEmpty()) {
            postQuery(parseQuery(configuration.getString(CK_NEO4J_STARTUP_QUERY)));
        }
        isRunning.set(true);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        Iterator messageFields = message.getFields().entrySet().iterator();
        Map<String, Object> model = new HashMap<>();
        while (messageFields.hasNext()) {
            Map.Entry pair = (Map.Entry) messageFields.next();
            model.put(String.valueOf(pair.getKey()), String.valueOf(pair.getValue()));
        }
        Engine engine = new Engine();
        String queryString = engine.transform(configuration.getString(CK_NEO4J_QUERY), model);
        List<HashMap<String, String>> query = parseQuery(queryString);

        postQuery(query);
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
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

    private void postQuery(List<HashMap<String, String>> queries) {
        HashMap<String, List<HashMap<String, String>>> payload = new HashMap<>();
        payload.put("statements", queries);
        Response response = cypher
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON), Response.class);

        String result = String.format(
                "POST [%s] to [%s], status code [%d], headers: %s, returned data: %s",
                payload, configuration.getString(CK_NEO4J_URL), response.getStatus(), response.getHeaders(),
                response);
        if (response.getStatus() >= 400) {
            LOG.info(result);
        } else {
            LOG.debug(result);
        }
    }

    @Override
    public void stop() {
        LOG.info("Stopping Neo4j output");
        isRunning.set(false);
    }

    public interface Factory extends MessageOutput.Factory<Neo4jOutput> {
        @Override
        Neo4jOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            configurationRequest.addField(new TextField(
                            CK_NEO4J_URL, "Neo4j URL", "http://localhost:7474/db/data",
                            "URL to Neo4j",
                            ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                            CK_NEO4J_STARTUP_QUERY, "Startup Cypher query", "CREATE INDEX ON :HOST(address)",
                            "Query will be executed only once at start up time",
                            ConfigurationField.Optional.OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                            CK_NEO4J_QUERY, "Cypher query",
                            "MERGE (source:HOST { address: '${source}' })\n" +
                                    "MERGE (user_id:USER { user_id: '${user_id}'})\nMERGE (source)-[:CONNECT]->(user_id)",
                            "Query will be executed on every log message. Use template substitutions to access message fields: ${took_ms}",
                            ConfigurationField.Optional.NOT_OPTIONAL, TextField.Attribute.TEXTAREA)
            );

            configurationRequest.addField(new TextField(
                            CK_NEO4J_USER, "API username", "neo4j",
                            "Username used for authorization",
                            ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                            CK_NEO4J_PASSWORD, "API password", "",
                            "Password used for authorization",
                            ConfigurationField.Optional.NOT_OPTIONAL, TextField.Attribute.IS_PASSWORD)
            );

            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Neo4j Output", false, "", "An output plugin that writes log data to Neo4j");
        }
    }
}
