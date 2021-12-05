//package io.javabrains.springsecurityjpa;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//
//import javax.sql.DataSource;
//
//@Configuration
//@EnableJpaRepositories(
//        basePackages = "io.javabrains.springsecurityjpa",
//        includeFilters= @ComponentScan.Filter(ReadOnlyRepository.class),
//        entityManagerFactoryRef = "readEntityManagerFactory"
//)
//public class ReplicaEntityManagerConfiguration {
//    @Value("${spring.datasource.username}")
//    private String username;
//
//    @Value("${spring.datasource.password}")
//    private String password;
//
//    @Value("${spring.datasource.readUrl}")
//    private String readUrl;
//
//    @Bean
//    public DataSource readDataSource() throws Exception {
//        return DataSourceBuilder.create()
//                .url(readUrl)
//                .username(username)
//                .password(password)
//                .build();
//    }
//
//    @Bean
//    public LocalContainerEntityManagerFactoryBean readEntityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("readDataSource") DataSource dataSource) {
//        return builder.dataSource(dataSource)
//                .packages("io.javabrains.springsecurityjpa")
//                .persistenceUnit("read")
//                .build();
//    }
//}
