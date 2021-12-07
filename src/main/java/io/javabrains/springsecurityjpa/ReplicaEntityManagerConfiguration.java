package io.javabrains.springsecurityjpa;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "io.javabrains.springsecurityjpa",
        includeFilters= @ComponentScan.Filter(ReadReplicaOnlyRepository.class),
        entityManagerFactoryRef = "readReplicaEntityManagerFactory"
)
public class ReplicaEntityManagerConfiguration {
    @Value("${username}")
    private String username;

    @Value("${password}")
    private String password;

    @Value("${host1}")
    private String readUrl;

    @Bean
    public DataSource readDataSource() throws Exception {
        return DataSourceBuilder.create()
                .url(readUrl)
                .username(username)
                .password(password)
                .driverClassName("com.mysql.jdbc.Driver")
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean readReplicaEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("readDataSource") DataSource dataSource) {
        return builder.dataSource(dataSource)
                .packages("io.javabrains.springsecurityjpa")
                .persistenceUnit("read")
                .build();
    }
}
