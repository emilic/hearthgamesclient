package com.hearthgames.client.log;

import com.hearthgames.client.ws.HearthGamesClient;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DeflaterOutputStream;

@Component
public class GameRecorder {

    private static final Logger logger = LoggerFactory.getLogger(GameRecorder.class);

    private static final Pattern pattern = Pattern.compile("\\[LoadingScreen\\] LoadingScreen.OnSceneLoaded\\(\\) - prevMode=(.*) currMode=(.*)");

    private static final String CREATE_GAME = "CREATE_GAME";
    private static final String GAME_STATE_COMPLETE = "TAG_CHANGE Entity=GameEntity tag=STATE value=COMPLETE";
    private static final String END_OF_LOGS_FOR_GAME_MARKER = "---RegisterFriendChallenge---";

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
    private HearthGamesClient client;

    private StringBuilder currentGame = new StringBuilder();
    private boolean gameComplete;
    private long startTime;
    private long endTime;
    private GameType gameType = GameType.UNKNOWN;

    public void handleLine(String line) {

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
            GameData gameData = new GameData();
            gameData.setData(compress(currentGame.toString()));
            gameData.setStartTime(startTime);
            gameData.setEndTime(endTime);
            gameData.setGameType(gameType.getType());

            if (!hasGameBeenRecorded(gameData) && isGameValid(currentGame.toString())) {
                logger.info("Detected Game Type = " + gameType.name());
                logger.info("Attempting to upload recorded game to HearthGames.com");
                recordedGames.add(gameData);
                client.recordGame(gameData);
            }

            // Reset the game
            currentGame = new StringBuilder();
            gameComplete = false;
            startTime = 0;
            endTime = 0;

        } else if (currentGame != null) {
            currentGame.append(line).append("\n");
        }
    }

    public void saveGameToFile(GameData gameData) {
        String fileName = System.getProperty("java.io.tmpdir");
        fileName += "game_"+startTime+"_"+endTime+".chl";

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

    private void detectGameMode(String line) {
        if (line.startsWith(GameLogger.Bob.getName())) {
            if (line.contains(RANKED)) {
                gameType = GameType.RANKED;
            } else if (line.contains(ARENA_GAME)) {
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
        Matcher matcher = pattern.matcher(line);
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
}