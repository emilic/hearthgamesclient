package com.hearthgames.client.log;

/**
 * Stores game data such as the log file (compressed), game type (ARENA, CASUAL, etc...) and the start and end time of the game.
 */
public class GameData {

    private byte[] data;
    private int gameType;
    private long startTime;
    private long endTime;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
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
