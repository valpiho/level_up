package com.example.demo;

import com.example.demo.domain.AdjustmentRequest;
import com.example.demo.domain.EngineData;
import com.example.demo.domain.InitRequest;
import com.example.demo.domain.InitResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class EngineService {

    private final String LEVEL_UP_HOST = "https://warp-regulator-bd7q33crqa-lz.a.run.app/api/";
    private String authorizationCode;
    private int timer = 0;
    private int engineStartsCount = 0;

    private final RestTemplate restTemplate;

    public EngineService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.errorHandler(new RestTemplateResponseErrorHandler()).build();
    }

    @PostConstruct
    private void startEngine() throws InterruptedException {
        log.warn("Starting the engine in:");
        Thread.sleep(1000);
        log.warn("3");
        Thread.sleep(1000);
        log.warn("2");
        Thread.sleep(1000);
        log.warn("1");
        Thread.sleep(1000);
        ResponseEntity<InitResponse> response = null;
        try {
            response = restTemplate.postForEntity(
                    LEVEL_UP_HOST + "start",
                    new InitRequest("Valentin Piho", "val.piho@gmail.com"),
                    InitResponse.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        assert response != null;
        authorizationCode = Objects.requireNonNull(response.getBody()).getAuthorizationCode();
        engineStartsCount++;
        getStatus();
    }

    private void getStatus() throws InterruptedException {
        Thread.sleep(1000);
        EngineData response = null;
        Map<String, String> params = new HashMap<>();
        params.put("authorizationCode", authorizationCode);
        try {
            response = restTemplate.getForObject(
                    LEVEL_UP_HOST + "status?authorizationCode={authorizationCode}",
                    EngineData.class, params
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        assert response != null;

        log.info("-------------------------------");
        log.info("Engine has ben started " + engineStartsCount + " time(s)");
        log.info("Seconds from start: " + timer++ );
        log.info(response.toString());

        if (response.getFlowRate() == null || response.getIntermix() <= 0.0) {
            log.warn("Houston, we have a problem. Engine is stopped. Restarting");
            timer = 0;
            startEngine();
        }

        addMatter(response.getIntermix(), response.getFlowRate());
    }

    private void addMatter(double intermix, String flowRate) throws InterruptedException {
        switch (flowRate) {
            case "LOW" -> {
                if (intermix < 0.5) {
                    sendMatters(0.05, -0.03);
                }else if (intermix > 0.5) {
                    sendMatters(-0.03, 0.05);
                } else {
                    sendMatters(0.05, 0.05);
                }
            }
            case "OPTIMAL" -> {
                if (intermix < 0.5) {
                    sendMatters(0.1, -0.07);
                }else if (intermix > 0.5) {
                    sendMatters(-0.07, 0.1);
                }
            }
            case "HIGH" -> {
                if (intermix < 0.5) {
                    sendMatters(0.05, -0.1);
                } else if (intermix > 0.5) {
                    sendMatters(-0.1, 0.05);
                } else {
                    sendMatters(-0.1, -0.1);
                }
            }

        }

        getStatus();
    }

    private void sendMatters(double matter, double antimatter) {
        try {
            restTemplate.postForLocation(
                    LEVEL_UP_HOST + "adjust/matter",
                    new AdjustmentRequest(authorizationCode, matter));
            restTemplate.postForLocation(
                    LEVEL_UP_HOST + "adjust/antimatter",
                    new AdjustmentRequest(authorizationCode, antimatter));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
