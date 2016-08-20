package org.f0w.fproject.server.config

import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

import java.net.InetAddress
import java.net.UnknownHostException

@Configuration
open class ElasticConfig {
    @Autowired
    private lateinit var env: Environment

    @Bean
    @Throws(UnknownHostException::class)
    open fun elastic(): Client {
        val settings = Settings.settingsBuilder().put("cluster.name", env.getProperty("elastic.cluster")).build()

        return TransportClient.builder()
                .settings(settings)
                .build()
                .addTransportAddress(
                        InetSocketTransportAddress(
                            InetAddress.getByName(env.getProperty("elastic.host")),
                            env.getProperty("elastic.port", Int::class.java)
                        )
                )
    }
}
