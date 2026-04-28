package com.tripforge.external.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {

    /** General-purpose RestTemplate — used by most provider classes. */
    @Bean(name = "defaultRestTemplate")
    public RestTemplate defaultRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Provider-specific RestTemplate — marked @Primary so any unqualified
     * RestTemplate injection resolves to this bean without ambiguity.
     */
    @Bean(name = "providerRestTemplate")
    @Primary
    public RestTemplate providerRestTemplate() {
        return new RestTemplate();
    }

    /** Dedicated RestTemplate for Frankfurter FX calls. */
    @Bean(name = "fxRestTemplate")
    public RestTemplate fxRestTemplate() {
        return new RestTemplate();
    }
}
