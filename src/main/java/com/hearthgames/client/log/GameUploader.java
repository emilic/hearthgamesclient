package com.hearthgames.client.log;

import com.hearthgames.client.config.ApplicationProperties;
import org.apache.commons.io.FileUtils;
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

import java.io.*;
import java.net.ConnectException;

/**
 * The Game Uploader handles communication between the server and client, sending completed game to be recorded on the server.
 * If a game fails to record it will save the game to a temporary file in the users temp directory.
 */
@Component
public class GameUploader {

    private static final Logger logger = LoggerFactory.getLogger(GameUploader.class);

    private RestTemplate restTemplate;
    private ApplicationProperties properties;

    @Autowired
    public GameUploader(RestTemplate restTemplate,
                        ApplicationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public boolean uploadGame(GameData gameData) {
        try {
            ResponseEntity<RecordGameResponse> response = postGameToServer(createRequestFromData(gameData));
            if (response.getStatusCode() == HttpStatus.OK) {
                if (response.getBody().isUpgradeRequired()) {
                    logger.info(response.getBody().getMsg());
                } else {
                    logger.info("Game recorded: " + response.getBody().getUrl());
                }
                return true;
            }
        } catch (RestClientException e) {
            if (e.getCause() instanceof ConnectException) {
                logger.info("Not able to save game to HearthGames.com at this time because the server is offline.");
                logger.info(e.getMessage());
                logger.info("Attempting to save game to local cache for later upload, on restart of the client.");
                saveGameToFile(gameData);
            } else {
                logger.info("Server returned error and will not process game. If a valid game was uploaded it will be queued for analysis and reprocessing on the server.");
            }
            logger.info(e.getMessage());
        }
        return false;
    }

    private void saveGameToFile(GameData gameData) {
        String fileName = System.getProperty("java.io.tmpdir");
        fileName += "game_"+gameData.getStartTime()+"_"+gameData.getEndTime()+".chl";

        File file = new File(fileName);
        if (!file.exists()) { // don't need to save the game to file if it's already there
            logger.info("Saving game to : " + fileName);
            try {
                FileUtils.writeByteArrayToFile(file, gameData.getData());
            } catch (IOException e) {
                logger.error("Error saving game to : " + fileName);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.error(sw.toString());
            }
        } else {
            logger.info("File already exists. Skipping.");
        }
    }

    private RecordGameRequest createRequestFromData(GameData data) {
        RecordGameRequest request = new RecordGameRequest();
        request.setVersion(5);
        request.setData(data.getData());
        request.setStartTime(data.getStartTime());
        request.setEndTime(data.getEndTime());
        request.setGameType(data.getGameType());
        return request;
    }

    private ResponseEntity<RecordGameResponse> postGameToServer(RecordGameRequest request) throws RestClientException {
        logger.info("Posting game to the server...");
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "HearthGamesClient");
        HttpEntity<RecordGameRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForEntity(this.properties.getUploadUrl(), entity, RecordGameResponse.class);
    }

    public static class RecordGameRequest implements Serializable {
        private static final long serialVersionUID = 1;

        private int version;
        private int gameType;
        private byte[] data;
        private long startTime;
        private long endTime;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public int getGameType() {
            return gameType;
        }

        public void setGameType(int gameType) {
            this.gameType = gameType;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }
    }

    public static class RecordGameResponse implements Serializable {
        private static final long serialVersionUID = 1;

        private String url;
        private String msg;
        private boolean upgradeRequired;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public boolean isUpgradeRequired() {
            return upgradeRequired;
        }

        public void setUpgradeRequired(boolean upgradeRequired) {
            this.upgradeRequired = upgradeRequired;
        }
    }
}