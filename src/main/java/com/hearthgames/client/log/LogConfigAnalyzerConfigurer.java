package com.hearthgames.client.log;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
        List<String> newLines = new ArrayList<>();
        boolean ignoreLogger = false;
        for (String line: originalLines) {
            if (line.startsWith("[")) {
                ignoreLogger = line.startsWith("[Power]") || line.startsWith("[Asset]") || line.startsWith("[Bob]");
            }
            if (!ignoreLogger) {
                newLines.add(line);
            }
        }
        newLines.add("[Power]");
        newLines.add("LogLevel=1");
        newLines.add("FilePrinting=false");
        newLines.add("ConsolePrinting=true");
        newLines.add("ScreenPrinting=false");
        newLines.add("[Asset]");
        newLines.add("LogLevel=1");
        newLines.add("FilePrinting=false");
        newLines.add("ConsolePrinting=true");
        newLines.add("ScreenPrinting=false");
        newLines.add("[Bob]");
        newLines.add("LogLevel=1");
        newLines.add("FilePrinting=false");
        newLines.add("ConsolePrinting=true");
        newLines.add("ScreenPrinting=false");
        return newLines;
    }
}