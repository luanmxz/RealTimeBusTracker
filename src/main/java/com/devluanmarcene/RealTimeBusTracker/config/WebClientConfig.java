package com.devluanmarcene.RealTimeBusTracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient getWebClient() {
        int sizeInBytes = 16 * 1024 * 1024; // TODO: Remover, temporário
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(sizeInBytes)).build();

        return WebClient.builder().exchangeStrategies(strategies).build();
    }
}
