package io.javabrains.springsecurityjpa;


import io.javabrains.springsecurityjpa.models.User;
import io.javabrains.springsecurityjpa.models.UserPic;
import org.hibernate.cfg.Configuration;
import org.hibernate.SessionFactory;

public class DAO
{
//    private static final SessionFactory sessionFactory;
    private static final SessionFactory sessionFactoryReplica;
//    public static SessionFactory getSessionFactory() {
//        return DAO.sessionFactory;
//    }
    public static SessionFactory getSessionFactoryReplica() {
        return DAO.sessionFactoryReplica;
    }

    static {

        final Configuration cfgReplica = new Configuration();
        String connection_url = System.getenv("host1")+"?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        cfgReplica.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5Dialect");
//        cfgReplica.setProperty("hibernate.naming-strategy", "org.hibernate.cfg.ImprovedNamingStrategy");
        cfgReplica.setProperty("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        cfgReplica.setProperty("hibernate.connection.url", connection_url);
        cfgReplica.setProperty("hibernate.connection.username", System.getenv("username"));
        cfgReplica.setProperty("hibernate.connection.password", System.getenv("password"));
        cfgReplica.setProperty("hibernate.show_sql", "true");
//        cfgReplica.addAnnotatedClass(UserPic.class);
        cfgReplica.addAnnotatedClass(User.class);
        sessionFactoryReplica = cfgReplica.buildSessionFactory();
    }
}