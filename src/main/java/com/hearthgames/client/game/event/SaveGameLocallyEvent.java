package com.hearthgames.client.game.event;

import com.hearthgames.client.game.GameData;
import org.springframework.context.ApplicationEvent;

public class SaveGameLocallyEvent extends ApplicationEvent {

    private GameData data;

    public SaveGameLocallyEvent(Object source, GameData data) {
        super(source);
        this.data = data;
    }

    public GameData getData() {
        return data;
    }
}
