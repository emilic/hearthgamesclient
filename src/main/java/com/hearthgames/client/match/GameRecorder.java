package com.hearthgames.client.match;

import com.hearthgames.client.match.event.GameRecordedEvent;
import com.hearthgames.client.match.event.SaveGameLocallyEvent;
import com.hearthgames.client.log.event.LineReadEvent;
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

    private static final Pattern medalRankPattern = Pattern.compile("name=Medal_Ranked_(.*) family");
    private static final String MEDAL_RANKED = "unloading name=Medal_Ranked";
    private static final String CREATE_GAME = "CREATE_GAME";
    private static final String GAME_STATE_COMPLETE = "TAG_CHANGE Entity=GameEntity tag=STATE value=COMPLETE";
    private static final String REGISTER_FRIEND_CHALLENGE = "---RegisterFriendChallenge---";

    @Autowired
    private HearthGamesClient client;

    private StringBuilder currentMatch;
    private boolean matchComplete;
    private Integer rank;
    private long startTime;
    private long endTime;

    private List<GameData> recordedMatches = new ArrayList<>();

    public void handleLine(LineReadEvent event) {
        String line = event.getLine().trim();
        if (line.startsWith(CREATE_GAME)) {
            matchComplete = false;
            rank = null;
            startTime = System.currentTimeMillis();
            currentMatch = new StringBuilder();
            currentMatch.append(event.getLine()).append("\n");
        } else if (currentMatch != null && line.startsWith(GAME_STATE_COMPLETE)) {
            currentMatch.append(event.getLine()).append("\n");
            matchComplete = true;
            endTime = System.currentTimeMillis();
        } else if (currentMatch != null && matchComplete && line.startsWith(MEDAL_RANKED)) {
            int rankFound = getRank(line);
            if (rank == null || rankFound < rank) {
                rank = rankFound;
            }
        } else if (currentMatch != null && matchComplete && line.startsWith(REGISTER_FRIEND_CHALLENGE)) {
            GameData gameData = new GameData();
            gameData.setData(compress(currentMatch.toString()));
            gameData.setStartTime(startTime);
            gameData.setEndTime(endTime);
            gameData.setRank(rank+"");

            currentMatch = null;
            if (!hasMatchBeenRecorded(gameData)) {
                logger.info("The Game has been recorded. Attempting to record @ HearthGames.com");
                recordedMatches.add(gameData);
                client.handleGameRecorded(new GameRecordedEvent(this, gameData));
            }

        } else if (currentMatch != null && event.isLoggable()) {
            currentMatch.append(event.getLine()).append("\n");
        }
    }

    // This method is needed because of a bug in Tailer that results in the log being re-read from the beginning when
    // hearthstone is exited out. So we have to unfortunately compare previous games data.
    private boolean hasMatchBeenRecorded(GameData data) {
        for (GameData md : recordedMatches) {
            if (Arrays.equals(data.getData(), md.getData())) {
                return true;
            }
        }
        return false;
    }

    private int getRank(String line) {
        int rank = -1;
        Matcher matcher = medalRankPattern.matcher(line);
        if (matcher.find()) {
            String r = null;
            try {
                r = matcher.group(1);
                rank = Integer.parseInt(r);
            } catch (NumberFormatException e) {
                logger.warn("Found a rank that was not parseable : " + r);
            }
        }
        return rank;
    }

    public void handleData(SaveGameLocallyEvent event) {
        saveGameToFile(event.getData());
    }

    private void saveGameToFile(GameData gameData) {
        String fileName = System.getProperty("java.io.tmpdir");
        if (rank != null) {
            fileName += "ranked_"+startTime+"_"+endTime+"_" + rank + ".chl";
        } else {
            fileName += "nonranked_"+startTime+"_"+endTime+".chl";

        }
        File file = new File(fileName);
        logger.info("Saving match to : " + fileName);
        try {
            FileUtils.writeByteArrayToFile(file, gameData.getData());
        } catch (IOException e) {
            logger.error("Error saving match to : " + fileName);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.error(sw.toString());
        }
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