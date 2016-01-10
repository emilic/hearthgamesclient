package com.hearthgames.client.ws;

public class RecordGameResponse {
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
