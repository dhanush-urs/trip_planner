package com.tripforge.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient geminiWebClient(GeminiProperties props) {
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
                .build();
    }
}
