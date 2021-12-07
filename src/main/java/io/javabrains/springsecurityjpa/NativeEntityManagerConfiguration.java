//package io.javabrains.springsecurityjpa;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//
//import javax.sql.DataSource;
//
//@Configuration
//@EnableJpaRepositories(
//        basePackages = "io.javabrains.springsecurityjpa",
//        excludeFilters = @ComponentScan.Filter(ReadReplicaOnlyRepository.class),
//        entityManagerFactoryRef = "entityManagerFactory"
//)
//public class NativeEntityManagerConfiguration {
//    @Value("${spring.datasource.username}")
//    private String username;
//
//    @Value("${spring.datasource.password}")
//    private String password;
//
//    @Value("${spring.datasource.url}")
//    private String url;
//
//    @Bean
//    @Primary
//    public DataSource dataSource() throws Exception {
//        return DataSourceBuilder.create()
//                .url(url)
//                .username(username)
//                .password(password)
//                .driverClassName("com.mysql.jdbc.Driver")
//                .build();
//    }
//
//    @Bean
//    @Primary
//    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
//            EntityManagerFactoryBuilder builder,
//            @Qualifier("dataSource") DataSource dataSource) {
//        return builder.dataSource(dataSource)
//                .packages("io.javabrains.springsecurityjpa")
//                .persistenceUnit("main")
//                .build();
//    }
//
//
//}
