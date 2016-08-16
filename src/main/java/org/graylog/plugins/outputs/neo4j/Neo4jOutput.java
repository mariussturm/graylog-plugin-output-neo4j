package org.graylog.plugins.outputs.neo4j;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.outputs.neo4j.transport.INeo4jTransport;
import org.graylog.plugins.outputs.neo4j.transport.Neo4jTransports;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Neo4jOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4jOutput.class);

    private Configuration configuration;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private INeo4jTransport transport;

    public static final String CK_PROTOCOL = "neo4j_protocol";
    public static final String CK_NEO4J_URL = "neo4j_url";
    public static final String CK_NEO4J_STARTUP_QUERY = "neo4j_startup_query";
    public static final String CK_NEO4J_QUERY = "neo4j_query";
    public static final String CK_NEO4J_USER = "neo4j_user";
    public static final String CK_NEO4J_PASSWORD = "neo4j_password";

    @Inject
    public Neo4jOutput(@Assisted Stream stream, @Assisted Configuration config) throws MessageOutputConfigurationException {
        configuration = config;
        final Neo4jTransports transportSelection;
        switch (configuration.getString(CK_PROTOCOL).toUpperCase(Locale.ENGLISH)) {
            case "BOLT":
                transportSelection = Neo4jTransports.BOLT;
                break;
            case "HTTP":
                transportSelection = Neo4jTransports.HTTP;
                break;

            default:
                throw new MessageOutputConfigurationException("Unknown protocol " + configuration.getString(CK_PROTOCOL));
        }

        try {
            transport = Neo4jTransports.create(transportSelection, configuration);
        } catch (Exception e) {
            final String error = "Error initializing " + Neo4JTransport.class;
            LOG.error(error, e);
            throw new MessageOutputConfigurationException(error);
        }


        isRunning.set(true);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        transport.send(message);
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
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

            final Map<String, String> protocols = ImmutableMap.of(
                    "HTTP", "HTTP",
                    "Bolt", "Bolt");

            configurationRequest.addField(new DropdownField(
                            CK_PROTOCOL, "Neo4J Protocol", "HTTP", protocols,
                            "The protocol used to connect to Neo4J",
                            ConfigurationField.Optional.NOT_OPTIONAL));

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

    public enum Neo4JTransport {
        TCP,
        UDP;    }
}
