package com.tmd.backend.external.animalhospital;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AnimalHospitalApiClient {
    private static final int PAGE_SIZE = 100;
    private static final int MAX_ATTEMPTS = 3;

    private final RestClient restClient;
    private final String endpoint;
    private final String serviceKey;
    private final Duration retryDelay;

    public AnimalHospitalApiClient(
        RestClient.Builder builder,
        @Value("${animal-hospital.api.endpoint:https://apis.data.go.kr/1741000/animal_hospitals/info}") String endpoint,
        @Value("${animal-hospital.api.service-key:${tourapi.service-key}}") String serviceKey,
        @Value("${animal-hospital.api.connect-timeout:10s}") Duration connectTimeout,
        @Value("${animal-hospital.api.read-timeout:30s}") Duration readTimeout,
        @Value("${animal-hospital.api.retry-delay:500ms}") Duration retryDelay
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.requestFactory(requestFactory).build();
        this.endpoint = endpoint;
        this.serviceKey = serviceKey;
        this.retryDelay = retryDelay;
    }

    public List<AnimalHospitalItem> getAll() {
        AnimalHospitalApiResponse.Body firstPage = getPage(1);
        int totalCount = firstPage.totalCount() == null ? 0 : firstPage.totalCount();
        if (totalCount <= 0) {
            throw new IllegalStateException("Animal hospital API returned an empty snapshot");
        }

        List<AnimalHospitalItem> result = new ArrayList<>(totalCount);
        result.addAll(firstPage.itemList());
        int totalPages = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        for (int page = 2; page <= totalPages; page++) {
            List<AnimalHospitalItem> items = getPage(page).itemList();
            if (items.isEmpty()) {
                throw new IllegalStateException("Animal hospital API ended before totalCount. pageNo=" + page);
            }
            result.addAll(items);
        }
        if (result.size() != totalCount) {
            throw new IllegalStateException(
                "Animal hospital API count mismatch. totalCount=" + totalCount + ", actual=" + result.size()
            );
        }
        return List.copyOf(result);
    }

    private AnimalHospitalApiResponse.Body getPage(int pageNo) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                AnimalHospitalApiResponse response = restClient.get()
                    .uri(buildUri(pageNo))
                    .retrieve()
                    .body(AnimalHospitalApiResponse.class);
                return validate(response, pageNo);
            } catch (RuntimeException exception) {
                if (!isRetryable(exception) || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                lastFailure = exception;
                log.warn("Animal hospital API request failed temporarily. pageNo={}, attempt={}", pageNo, attempt);
                waitBeforeRetry();
            }
        }
        throw lastFailure == null ? new IllegalStateException("Animal hospital API request failed") : lastFailure;
    }

    private URI buildUri(int pageNo) {
        return UriComponentsBuilder.fromUriString(endpoint)
            .queryParam("serviceKey", serviceKey)
            .queryParam("pageNo", pageNo)
            .queryParam("numOfRows", PAGE_SIZE)
            .queryParam("type", "json")
            .build()
            .toUri();
    }

    private AnimalHospitalApiResponse.Body validate(AnimalHospitalApiResponse response, int pageNo) {
        if (response == null || response.response() == null || response.response().header() == null) {
            throw new IllegalStateException("Animal hospital API response header is missing. pageNo=" + pageNo);
        }
        AnimalHospitalApiResponse.Header header = response.response().header();
        if (!"0".equals(header.resultCode())) {
            throw new IllegalStateException(
                "Animal hospital API request failed. code=" + header.resultCode() + ", message=" + header.resultMsg()
            );
        }
        if (response.response().body() == null) {
            throw new IllegalStateException("Animal hospital API response body is missing. pageNo=" + pageNo);
        }
        return response.response().body();
    }

    private boolean isRetryable(RuntimeException exception) {
        if (exception instanceof ResourceAccessException || exception instanceof HttpServerErrorException) {
            return true;
        }
        return exception instanceof HttpClientErrorException clientError
            && clientError.getStatusCode().value() == 429;
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(retryDelay.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying animal hospital API", interrupted);
        }
    }
}
