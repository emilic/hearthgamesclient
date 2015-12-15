package com.hearthgames.client.match.event;

import com.hearthgames.client.match.GameData;
import org.springframework.context.ApplicationEvent;

import java.io.File;

public class RetryGameRecordedEvent extends ApplicationEvent {

    private GameData data;
    private File file;

    public RetryGameRecordedEvent(Object source, GameData data, File file) {
        super(source);
        this.data = data;
        this.file = file;
    }

    public GameData getData() {
        return data;
    }

    public File getFile() {
        return file;
    }
}
