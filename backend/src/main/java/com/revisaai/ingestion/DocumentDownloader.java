package com.revisaai.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class DocumentDownloader {

    private static final Logger log = LoggerFactory.getLogger(DocumentDownloader.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;

    public DocumentDownloader(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public byte[] download(String url) {
        log.debug("Baixando PDF via URL: {}", url);
        var bytes = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(TIMEOUT);

        if (bytes == null) {
            throw new IllegalStateException("Download sem conteúdo para URL: " + url);
        }
        return bytes;
    }
}
