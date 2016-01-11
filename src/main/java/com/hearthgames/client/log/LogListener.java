package com.hearthgames.client.log;

import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.StringUtils;
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
        if (isLineLoggable(line)) {
            gameRecorder.handleLine(line);
        }
    }

    private boolean isLineLoggable(String line) {
        if (StringUtils.isEmpty(line)) return false;
        if (line.startsWith("[Power] GameState.DebugPrintPower() - ")) return true;
        if (line.startsWith("[Asset] CachedAsset.UnloadAssetObject() - unloading name=Medal_Ranked")) return true;
        if (line.startsWith("[Bob] ---Register")) return true;
        if (line.startsWith("[LoadingScreen] LoadingScreen.OnSceneLoaded() - prevMode=")) return true;
        return false;
    }

    public void handle(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        logger.error(sw.toString());
    }
}
