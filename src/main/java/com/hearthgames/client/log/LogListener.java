package com.hearthgames.client.log;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

@Component
public class LogListener extends TailerListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LogListener.class);

    @Autowired
    private GameRecorder gameRecorder;

    @Override
    public void handle(String line) {
        if (GameLogger.isLineValid(line)) {
            gameRecorder.handleLine(line);
        }
    }

    public void handle(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        logger.error(sw.toString());
    }
}
