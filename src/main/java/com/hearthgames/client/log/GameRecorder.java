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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;

/**
 * The Game Recorder is responsible for handling each line read from the Hearthstone log file.  The logic to determine
 * the game type, when the game starts and ends is all here.
 */
@Component
public class GameRecorder extends TailerListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GameRecorder.class);

    private static final Pattern gameModePattern = Pattern.compile("\\[LoadingScreen\\] LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)");

    private static final String CREATE_GAME = "CREATE_GAME";
    private static final String GAME_STATE_COMPLETE = "TAG_CHANGE Entity=GameEntity tag=STATE value=COMPLETE";
    private static final String END_OF_LOGS_FOR_GAME_MARKER = "---RegisterScreenBox---";

    private static final String RANKED = "unloading name=Medal_Ranked";
    private static final String ARENA_GAME = "---RegisterScreenForge---";
    private static final String PLAY_MODE = "---RegisterScreenTourneys---";
    private static final String FRIEND_CHALLENGE = "---RegisterScreenFriendly---";
    private static final String ADVENTURE_MODE = "lo=0]";
    private static final String TAVERN_BRAWL = "TAVERN_BRAWL";
    private static final String TOURNAMENT = "TOURNAMENT";
    private static final String FRIENDLY = "FRIENDLY";
    private static final String ADVENTURE = "ADVENTURE";
    private static final String DRAFT = "DRAFT";

    @Autowired
    private GameUploader gameUploader;

    private StringBuilder currentGame = new StringBuilder();
    private boolean gameComplete;
    private long startTime;
    private long endTime;
    private GameType gameType = GameType.UNKNOWN;

    @Override
    public void handle(String line) {

        if (!GameLogger.isLineValid(line)) return;

        detectGameMode(line);

        if (line.contains(CREATE_GAME)) {
            startTime = System.currentTimeMillis();
            currentGame.append(line).append("\n");
        } else if (currentGame != null && line.contains(GAME_STATE_COMPLETE)) {
            currentGame.append(line).append("\n");
            gameComplete = true;
            endTime = System.currentTimeMillis();
        } else if (currentGame != null && gameComplete && line.contains(END_OF_LOGS_FOR_GAME_MARKER)) {
            currentGame.append(line).append("\n");
            GameData gameData = createGameData(currentGame.toString(), startTime, endTime, gameType);

            if (!hasGameBeenRecorded(gameData) && isGameValid(currentGame.toString())) {
                logger.info("Detected Game Type = " + gameType.name());
                recordedGames.add(gameData);
                gameUploader.uploadGame(gameData);
            }
            resetGame();

        } else if (currentGame != null) {
            currentGame.append(line).append("\n");
        }
    }

    private GameData createGameData(String currentGame, long startTime, long endTime, GameType gameType) {
        GameData gameData = new GameData();
        gameData.setData(compress(currentGame));
        gameData.setStartTime(startTime);
        gameData.setEndTime(endTime);
        gameData.setGameType(gameType.getType());
        return gameData;
    }

    private void resetGame() {
        currentGame = new StringBuilder();
        gameComplete = false;
        startTime = 0;
        endTime = 0;
    }

    private void detectGameMode(String line) {
        if (line.startsWith(GameLogger.Asset.getName())) {
            // check if this happens only once the game completes
            if (line.contains(RANKED) && gameComplete) {
                gameType = GameType.RANKED;
            }
        } else if (line.startsWith(GameLogger.Bob.getName())) {
            if (line.contains(ARENA_GAME)) {
                gameType = GameType.ARENA;
            } else if (line.contains(PLAY_MODE)) {
                gameType = GameType.CASUAL;
            } else if (line.contains(FRIEND_CHALLENGE)) {
                gameType = GameType.FRIENDLY_CHALLENGE;
            }
        } else if (line.startsWith(GameLogger.LoadingScreen.getName())) {
            String mode = getMode(line);
            if (mode != null) {
                if (TAVERN_BRAWL.equals(mode)) {
                    gameType = GameType.TAVERN_BRAWL;
                } else if (TOURNAMENT.equals(mode)) {
                    gameType = GameType.CASUAL;
                } else if (FRIENDLY.equals(mode)) {
                    gameType = GameType.FRIENDLY_CHALLENGE;
                } else if (ADVENTURE.equals(mode)) {
                    gameType = GameType.ADVENTURE;
                } else  if (DRAFT.equals(mode)) {
                    gameType = GameType.ARENA;
                }
            }
        } else if (line.contains(ADVENTURE_MODE)) {
            gameType = GameType.ADVENTURE;
        }
    }

    private String getMode(String line) {
        String mode = null;
        Matcher matcher = gameModePattern.matcher(line);
        if (matcher.find()) {
            mode = matcher.group(2);
        }
        return mode;
    }

    public enum GameType {
        UNKNOWN(0),
        CASUAL(1),
        RANKED(2),
        ARENA(3),
        ADVENTURE(4),
        TAVERN_BRAWL(5),
        FRIENDLY_CHALLENGE(6);

        private int type;
        GameType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

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