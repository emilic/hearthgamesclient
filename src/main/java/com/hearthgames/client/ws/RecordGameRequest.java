package com.hearthgames.client.ws;

import java.io.Serializable;

public class RecordGameRequest implements Serializable {
    private static final long serialVersionUID = 1;

    private byte[] data;
    private String rank;
    private long startTime;
    private long endTime;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
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
