package org.graylog.plugins.outputs.neo4j;

import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

import java.util.Arrays;
import java.util.Collection;

public class Neo4jOutputPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new Neo4jOutputMetaData();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Arrays.<PluginModule>asList(new Neo4jOutputModule());
    }
}
