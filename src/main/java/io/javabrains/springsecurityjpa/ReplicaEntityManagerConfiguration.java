package io.javabrains.springsecurityjpa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
//@EnableJpaRepositories(
//        basePackages = "io.javabrains.springsecurityjpa",
//        includeFilters= @ComponentScan.Filter(ReadReplicaOnlyRepository.class),
//        entityManagerFactoryRef = "readReplicaEntityManagerFactory"
//)
public class ReplicaEntityManagerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${username}")
    private String username;

    @Value("${password}")
    private String password;

    @Value("${host1}")
    private String readUrl;

    @Bean
    public DataSource readDataSource() {
        try{
            logger.info("**********Read Data Source !**********");
            return DataSourceBuilder.create()
                    .url(readUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .build();
        } catch (Exception e){
            logger.info("**********Read Data Source Error !**********");
            logger.info(e.getStackTrace().toString());
            return DataSourceBuilder.create()
                    .url(readUrl)
                    .username(username)
                    .password(password)
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .build();
        }

    }

//    @Bean
//    public LocalContainerEntityManagerFactoryBean readReplicaEntityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("readDataSource") DataSource dataSource) {
//        try {
//            logger.info("**********Replica Entity Manager !**********");
//            return builder.dataSource(dataSource)
//                    .packages("io.javabrains.springsecurityjpa")
//                    .persistenceUnit("read")
//                    .build();
//        } catch (Exception e){
//            logger.info("**********Replica Entity Manager Error!**********");
//            return builder.dataSource(dataSource)
//                    .packages("io.javabrains.springsecurityjpa")
//                    .persistenceUnit("read")
//                    .build();
//        }
//    }
}
