package org.graylog.plugins.outputs.neo4j;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class Neo4jOutputMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return Neo4jOutput.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "Neo4j Output Plugin";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org/");
    }

    @Override
    public Version getVersion() {
        return new Version(1, 1, 0, "SNAPSHOT");
    }

    @Override
    public String getDescription() {
        return "Output plugin that writes to Neo4j database";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
