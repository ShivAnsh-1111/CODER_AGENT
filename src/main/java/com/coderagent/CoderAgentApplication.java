package com.coderagent;

import com.coderagent.config.OllamaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OllamaProperties.class)
public class CoderAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoderAgentApplication.class, args);
    }
}
