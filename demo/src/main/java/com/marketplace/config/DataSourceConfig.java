package com.marketplace.config;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    // 1. Master DataSource 

    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    // 2. Replica DataSource 

    @Bean(name = "replicaDataSource")
    @ConfigurationProperties(prefix = "spring.datasource-replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    // 3. Routing DataSource 

    @Bean(name = "routingDataSource")
    @SuppressWarnings("null")
    public DataSource routingDataSource(
            @Qualifier("masterDataSource")  DataSource master,
            @Qualifier("replicaDataSource") DataSource replica) {

        AbstractRoutingDataSource routing = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
                return isReadOnly ? "replica" : "master";
            }
        };

        routing.setDefaultTargetDataSource(master);
        routing.setTargetDataSources(Map.of(
                "master",  master,
                "replica", replica
        ));
        routing.afterPropertiesSet();
        return routing;
    }

    // 4. Make routingDataSource the primary one Spring Boot uses

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(
            @Qualifier("routingDataSource") DataSource routing) {
        return routing;
    }
}
