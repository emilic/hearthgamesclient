package com.hearthgames.client.log;

import com.hearthgames.client.match.GameData;
import com.hearthgames.client.match.event.RetryGameRecordedEvent;
import com.hearthgames.client.log.listener.LogListener;
import com.hearthgames.client.ws.HearthGamesClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Collection;

@Component
public class LogManager {

    private static final Logger logger = LoggerFactory.getLogger(LogManager.class);

    private LogListener logListener;
    private File logFile;
    private HearthGamesClient client;

    @Autowired
    public LogManager(LogListener logListener,
                      File logFile,
                      HearthGamesClient client) {
        this.logListener = logListener;
        this.logFile = logFile;
        this.client = client;
    }

    public void start() throws InterruptedException, FileNotFoundException, UnsupportedEncodingException {
        uploadCachedLogs();
        if (!logFile.exists()) {
            logger.error("Cannot find file : " + logFile.getName());
            return;
        }
        Tailer tailer = new Tailer(logFile, logListener, 1000, true);
        Thread thread = new Thread(tailer);
        thread.start();
    }

    private void uploadCachedLogs() {
        Collection<File> files = FileUtils.listFiles(new File(System.getProperty("java.io.tmpdir")), new String[]{"chl"}, false);
        if (files.size() > 0) {
            logger.info("Found " + files.size() + " recorded game files that haven't been uploaded.");
        }
        for (File file: files) {
            try {
                byte[] data = FileUtils.readFileToByteArray(file);
                GameData gameData = new GameData();
                if (file.getName().startsWith("nonranked")) {
                    logger.info("Found Non Ranked match for upload : " + file.getName());
                    String[] gameInfo = file.getName().replace(".chl","").split("_");
                    gameData.setData(data);
                    gameData.setStartTime(Long.parseLong(gameInfo[1]));
                    gameData.setEndTime(Long.parseLong(gameInfo[2]));

                } else if (file.getName().startsWith("ranked")) {
                    logger.info("Found Ranked match for upload : " + file.getName());
                    String[] gameInfo = file.getName().replace(".chl","").split("_");
                    gameData.setData(data);
                    gameData.setStartTime(Long.parseLong(gameInfo[1]));
                    gameData.setEndTime(Long.parseLong(gameInfo[2]));
                    gameData.setRank(gameInfo[3]);
                }
                client.handleRetryGameRecorded(new RetryGameRecordedEvent(this, gameData, file));
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failed to read file : " + file.getName());
            }
        }
    }
}