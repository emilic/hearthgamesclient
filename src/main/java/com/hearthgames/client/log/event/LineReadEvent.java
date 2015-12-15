package com.hearthgames.client.log.event;

import org.springframework.context.ApplicationEvent;

public class LineReadEvent extends ApplicationEvent {

    private String line;
    private boolean loggable;

    public LineReadEvent(Object source, String line, boolean loggable) {
        super(source);
        this.line = line;
        this.loggable = loggable;
    }

    public String getLine() {
        return line;
    }

    public boolean isLoggable() {
        return loggable;
    }
}
