package com.hearthgames.client.log;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

/**
 * The Game Recorder is responsible for handling each line read from the Hearthstone log file and demarking a game
 */
@Component
public class GameRecorder extends TailerListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GameRecorder.class);

    private static final String CREATE_GAME = "CREATE_GAME";
    private static final String GAME_STATE_COMPLETE = "TAG_CHANGE Entity=GameEntity tag=STATE value=COMPLETE";
    private static final String END_OF_GAME = "---RegisterFriendChallenge---";

    @Autowired
    private GameUploader gameUploader;

    private StringBuilder currentGame = new StringBuilder();
    private boolean gameComplete;
    private long startTime;
    private long endTime;

    @Override
    public void handle(String line) {
        if (!GameLogger.isLineValid(line)) return;
        if (line.contains(CREATE_GAME)) {
            startTime = System.currentTimeMillis();
            currentGame.append(line).append("\n");
        } else if (currentGame != null && line.contains(GAME_STATE_COMPLETE)) {
            currentGame.append(line).append("\n");
            gameComplete = true;
            endTime = System.currentTimeMillis();
        } else if (currentGame != null && gameComplete && line.contains(END_OF_GAME)) {
            currentGame.append(line).append("\n");
            GameData gameData = createGameData(currentGame.toString(), startTime, endTime);
            if (!hasGameBeenRecorded(gameData) && isGameValid(currentGame.toString())) {
                recordedGames.add(gameData);
                gameUploader.uploadGame(gameData);
            }
            resetGame();
        } else if (currentGame != null) {
            currentGame.append(line).append("\n");
        }
    }

    private GameData createGameData(String currentGame, long startTime, long endTime) {
        GameData gameData = new GameData();
        gameData.setData(compress(currentGame));
        gameData.setStartTime(startTime);
        gameData.setEndTime(endTime);
        return gameData;
    }

    private void resetGame() {
        currentGame = new StringBuilder();
        gameComplete = false;
        startTime = 0;
        endTime = 0;
    }

    private boolean isGameValid(String game) {
        return game.contains(CREATE_GAME) && game.contains(GAME_STATE_COMPLETE);
    }

    private List<GameData> recordedGames = new ArrayList<>();

    // This method is needed because of a bug in Tailer that results in the log being re-read from the beginning when
    // Hearthstone is exited out. So we have to unfortunately compare previous games data.
    // See https://issues.apache.org/jira/browse/IO-279
    private boolean hasGameBeenRecorded(GameData data) {
        for (GameData md : recordedGames) {
            if (Arrays.equals(data.getData(), md.getData())) {
                return true;
            }
        }
        return false;
    }

    private byte[] compress(String text) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream out = new DeflaterOutputStream(baos)){
            out.write(text.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return baos.toByteArray();
    }

    public void handle(Exception e) {
        logger.error(ExceptionUtils.getStackTrace(e));
    }
}