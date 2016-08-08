package org.f0w.fproject.server.config;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class ElasticConfig {
    @Autowired
    private Environment env;

    @Bean
    public Client elastic() throws UnknownHostException {
        Settings settings = Settings.settingsBuilder().put("cluster.name", env.getProperty("elastic.cluster")).build();

        return TransportClient.builder()
                .settings(settings)
                .build()
                .addTransportAddress(new InetSocketTransportAddress(
                        InetAddress.getByName(env.getProperty("elastic.host")),
                        env.getProperty("elastic.port", Integer.class)
                ));
    }
}
