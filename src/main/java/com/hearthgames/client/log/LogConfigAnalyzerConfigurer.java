package com.hearthgames.client.log;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class Analyzes the log.config file and adds the Loggers required keeping any existing Loggers configured intact.
 */
@Component
public class LogConfigAnalyzerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(LogConfigAnalyzerConfigurer.class);

    @Autowired
    private File logConfigFile;

    public void configure() throws IOException {
        if (!logConfigFile.exists()) {
            boolean created = logConfigFile.createNewFile();
            if (!created) {
                String error = logConfigFile + " did not exist so we tried to create it, but failed.";
                logger.error(error);
            }
        }
        List<String> lines = FileUtils.readLines(logConfigFile);
        List<String> newLines = analyze(lines);
        FileUtils.writeLines(logConfigFile, newLines);
    }

    private List<String> analyze(List<String> originalLines) {
        List<String> newLogConfig = new ArrayList<>();
        boolean ignoreLogger = false;
        for (String line: originalLines) {
            if (line.startsWith("[")) {
                ignoreLogger = line.startsWith(GameLogger.Power.getName()) ||
                        line.startsWith(GameLogger.Asset.getName()) ||
                        line.startsWith(GameLogger.Bob.getName()) ||
                        line.startsWith(GameLogger.LoadingScreen.getName()) ||
                        line.startsWith(GameLogger.Achievements.getName()) ||
                        line.startsWith(GameLogger.Arena.getName()) ||
                        line.startsWith(GameLogger.Rachelle.getName());
            }
            if (!ignoreLogger) {
                newLogConfig.add(line);
            }
        }
        addLogger(GameLogger.Power.getName(), newLogConfig);
        addLogger(GameLogger.Asset.getName(), newLogConfig);
        addLogger(GameLogger.Bob.getName(), newLogConfig);
        addLogger(GameLogger.LoadingScreen.getName(), newLogConfig);
        addLogger(GameLogger.Achievements.getName(), newLogConfig);
        addLogger(GameLogger.Arena.getName(), newLogConfig);
        addLogger(GameLogger.Rachelle.getName(), newLogConfig);
        return newLogConfig;
    }

    private void addLogger(String loggerName, List<String> newLogConfig) {
        newLogConfig.add(loggerName);
        newLogConfig.add("LogLevel=1");
        newLogConfig.add("FilePrinting=false");
        newLogConfig.add("ConsolePrinting=true");
        newLogConfig.add("ScreenPrinting=false");
    }
}