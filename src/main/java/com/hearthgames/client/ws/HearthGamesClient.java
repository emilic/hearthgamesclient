package com.hearthgames.client.ws;

import com.hearthgames.client.game.GameRecorder;
import com.hearthgames.client.game.event.SaveGameLocallyEvent;
import com.hearthgames.client.config.ApplicationProperties;
import com.hearthgames.client.game.GameData;
import com.hearthgames.client.game.event.GameRecordedEvent;
import com.hearthgames.client.game.event.RetryGameRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.ConnectException;

@Component
public class HearthGamesClient {

    private static final Logger logger = LoggerFactory.getLogger(HearthGamesClient.class);

    private RestTemplate restTemplate;
    private ApplicationProperties properties;
    private GameRecorder gameRecorder;

    @Autowired
    public HearthGamesClient(RestTemplate restTemplate,
                             ApplicationProperties properties,
                             GameRecorder gameRecorder) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.gameRecorder = gameRecorder;
    }

    public void handleGameRecorded(GameRecordedEvent event) {
        recordGame(event.getData());
    }

    public void handleRetryGameRecorded(RetryGameRecordedEvent event) {
        retryRecordGame(event.getData(), event.getFile());
    }

    private RecordGameRequest createRequestFromData(GameData data) {
        RecordGameRequest request = new RecordGameRequest();
        request.setData(data.getData());
        request.setRank(data.getRank());
        request.setStartTime(data.getStartTime());
        request.setEndTime(data.getEndTime());
        return request;
    }

    private void retryRecordGame(GameData gameData, File file) {
        try {
            ResponseEntity<RecordGameResponse> response = postGameToServer(createRequestFromData(gameData));
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Game recorded: " + response.getBody().getUrl());
                deleteFile(file);
            }
        } catch (RestClientException e) {
            if (e.getCause() instanceof ConnectException) {
                logger.info("Not able to save game to HearthGames.com at this time because the server is offline.");
            } else {
                logger.info("Server returned error and will not process game. If a valid game was uploaded it will be queued for analysis and reprocessing on the server.");
            }
            logger.info(e.getMessage());
        }
    }

    private void recordGame(GameData gameData) {
        try {
            ResponseEntity<RecordGameResponse> response = postGameToServer(createRequestFromData(gameData));
            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Game recorded: " + response.getBody().getUrl());
            }
        } catch (RestClientException e) {
            if (e.getCause() instanceof ConnectException) {
                logger.info("Not able to save game to HearthGames.com at this time because the server is offline.");
                logger.info(e.getMessage());
                logger.info("Attempting to save game to local cache for later upload, on restart of the client.");
                gameRecorder.handleData(new SaveGameLocallyEvent(this, gameData));
            } else {
                logger.info("Server returned error and will not process game. If a valid game was uploaded it will be queued for analysis and reprocessing on the server.");
            }
            logger.info(e.getMessage());
        }
    }

    private ResponseEntity<RecordGameResponse> postGameToServer(RecordGameRequest request) throws RestClientException {
        if (request.getRank() == null) {
            logger.info("Posting Non-Ranked Play Mode game to the server...");
        } else {
            logger.info("Posting Ranked Play Mode game to the server");
            logger.info("Rank detected : " + request.getRank());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "HearthGamesClient");
        HttpEntity<RecordGameRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForEntity(this.properties.getUploadUrl(), entity, RecordGameResponse.class);
    }

    private void deleteFile(File file) {
        try {
            logger.info("Attempting to delete temporary file: " + file.getAbsolutePath());
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("Deleted temporary file : " + file.getAbsolutePath());
            } else {
                logger.error("Failed to delete temporary file : " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("Failed to delete temporary file : " + file.getAbsolutePath());
        }
    }
}