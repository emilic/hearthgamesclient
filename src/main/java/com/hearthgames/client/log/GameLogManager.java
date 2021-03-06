package com.hearthgames.client.log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * The Game Log Manager orchestrates the reading and recording of log files.
 *
 * At application start up it looks for log files that have been saved locally (logs that failed to upload because the server was offline).
 *
 */
@Component
public class GameLogManager {

    private static final Logger logger = LoggerFactory.getLogger(GameLogManager.class);

    private GameRecorder gameRecorder;
    private File logFile;
    private GameUploader client;

    @Autowired
    public GameLogManager(GameRecorder gameRecorder,
                          File logFile,
                          GameUploader client) {
        this.gameRecorder = gameRecorder;
        this.logFile = logFile;
        this.client = client;
    }

    public void start() throws InterruptedException, IOException {
        uploadLocallySavedLogs();
        if (!logFile.exists()) {
            boolean created = logFile.createNewFile();
            if (!created) {
                logger.error("Could not find log file, tried to create empty file : " + logFile.getName() + " but was unable.  Please start Hearthstone before this App, then restart this App.");
                System.exit(-1);
            }
        }
        Tailer tailer = new Tailer(logFile, gameRecorder, 1000, true);
        Thread thread = new Thread(tailer);
        thread.start();
    }

    private void uploadLocallySavedLogs() {
        Collection<File> files = FileUtils.listFiles(new File(System.getProperty("java.io.tmpdir")), new String[]{"chl"}, false);
        if (files.size() > 0) {
            logger.info("Found " + files.size() + " recorded game files that haven't been uploaded.");
        }
        for (File file: files) {
            try {
                byte[] data = FileUtils.readFileToByteArray(file);
                GameData gameData = new GameData();

                if (file.getName().startsWith("game")) {
                    logger.info("Found game for upload : " + file.getName());
                    String[] gameInfo = file.getName().replace(".chl","").split("_");
                    gameData.setData(data);
                    gameData.setStartTime(Long.parseLong(gameInfo[1]));
                    gameData.setEndTime(Long.parseLong(gameInfo[2]));

                    boolean recorded = client.uploadGame(gameData);
                    if (recorded) {
                        deleteFile(file);
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
                logger.error("Failed to read file : " + file.getName());
            }
        }
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