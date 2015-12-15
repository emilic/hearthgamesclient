package com.hearthgames.client.ws;

import com.hearthgames.client.match.GameRecorder;
import com.hearthgames.client.match.event.SaveGameLocallyEvent;
import com.hearthgames.client.config.ApplicationProperties;
import com.hearthgames.client.match.GameData;
import com.hearthgames.client.match.event.GameRecordedEvent;
import com.hearthgames.client.match.event.RetryGameRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;

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
        recordMatch(event.getData());
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
        ResponseEntity<RecordGameResponse> response = postMatchToServer(createRequestFromData(gameData));
        if (response != null && response.getStatusCode() == HttpStatus.OK) {
            logger.info("Game recorded and available for viewing at: " + response.getBody().getUrl());
            deleteFile(file);
        } else if (response != null && response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE) {
            logger.info("The game type is not recordable. Some how a non-play mode game was uploaded.");
        } else if (response == null) {
            logger.info("Will try to save again on restart of the client.");
        }
    }

    private void recordMatch(GameData gameData) {
        ResponseEntity<RecordGameResponse> response = postMatchToServer(createRequestFromData(gameData));
        if (response != null && response.getStatusCode() == HttpStatus.OK) {
            logger.info("Game recorded: " + response.getBody().getUrl());
        } else if (response != null && response.getStatusCode() == HttpStatus.NOT_ACCEPTABLE) {
            logger.info("The game type is not recordable. Somehow a non-play mode game was uploaded.");
        } else if (response == null){
            logger.info("Attempting to save match to local cache for later upload, on restart of the client.");
            gameRecorder.handleData(new SaveGameLocallyEvent(this, gameData));
        }
    }

    private ResponseEntity<RecordGameResponse> postMatchToServer(RecordGameRequest request) {
        if (request.getRank() == null) {
            logger.info("Posting Non-Ranked Play Mode match to the server...");
        } else {
            logger.info("Posting Ranked Play Mode match to the server");
            logger.info("Rank detected : " + request.getRank());
        }
        ResponseEntity<RecordGameResponse> response = null;
        try {
            response = restTemplate.postForEntity(this.properties.getUploadUrl(), request, RecordGameResponse.class);
        } catch (Exception e) {
            logger.info("Not able to save game to HearthGames.com at this time.");
            logger.info(e.getMessage());
        }
        return response;
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