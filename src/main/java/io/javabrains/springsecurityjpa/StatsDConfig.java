
package io.javabrains.springsecurityjpa;

import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatsDConfig {

    private boolean publishMessage = true;

    @Value("localhost")
    private String metricHost;

    @Value("8080")
    private int portNumber;

    @Value("csye6225")
    private String prefix;

    @Bean
    public StatsDClient metricClient() {
        if (publishMessage)
            return new NonBlockingStatsDClient(prefix, metricHost, portNumber);
        return new NoOpStatsDClient();
    }
}
