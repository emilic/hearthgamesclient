package com.hearthgames.client.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hearthgames.client.log.LogConfigAnalyzerConfigurer;
import com.hearthgames.client.log.LogManager;
import com.hearthgames.client.ui.SystemTraySupportedJFrame;
import com.hearthgames.client.ui.TextAreaOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

@Configuration
@ComponentScan("com.hearthgames.client")
public class ApplicationConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Autowired
    private ApplicationProperties properties;

    @Bean
    public File logFile() {
        File logFile;
        if (properties.getOsName().toLowerCase().startsWith("windows")) {
            String root = properties.getUserHome().substring(0, properties.getUserHome().indexOf(File.separator));
            logFile = new File(root+"\\Program Files (x86)\\Hearthstone\\Hearthstone_Data\\output_log.txt");
        } else if (properties.getOsName().toLowerCase().startsWith("mac")) {
            logFile = new File(properties.getUserHome()+"/Library/Logs/Unity/Player.log");
        } else { // assume Linux using Wine
            logFile = new File(properties.getUserHome() + "/.wine/drive_c/Program Files (x86)/Hearthstone/Hearthstone_Data/output_log.txt");
        }
        logger.info("log: " + logFile);
        return logFile;
    }

    @Bean
    public File logConfigFile() {
        File logConfigFile;
        if (properties.getOsName().toLowerCase().contains("windows")) {
            logConfigFile = new File(properties.getUserHome() + "\\AppData\\Local\\Blizzard\\Hearthstone\\log.config");
        } else if (properties.getOsName().toLowerCase().contains("mac")) {
            logConfigFile = new File(properties.getUserHome() + "/Library/Preferences/Blizzard/Hearthstone/log.config");
        } else { // assume Linux using Wine
            logConfigFile = new File(properties.getUserHome() + "/.wine/drive_c/users/" + System.getProperty("user.name") + "/Local Settings/Application Data/Blizzard/Hearthstone/log.config");
        }
        logger.info("log config: " + logConfigFile);
        return logConfigFile;
    }

    @Bean
    @SuppressWarnings("all")
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        objectMapper.enableDefaultTyping();
        return objectMapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Autowired
    private LogConfigAnalyzerConfigurer logConfigAnalyzerConfigurer;

    @Autowired
    private LogManager logManager;

    @Bean
    public SystemTraySupportedJFrame frame() throws IOException, InterruptedException {
        SystemTraySupportedJFrame frame = new SystemTraySupportedJFrame("HearthGames.com");
        JTextArea consoleTextArea = new JTextArea(10, 60);

        JScrollPane scrollPane = new JScrollPane(consoleTextArea);
        scrollPane.setBounds(0, 0, 999, 220);
        frame.add(scrollPane);
        frame.setSize(1024, 220);
        frame.setResizable(true);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setState(Frame.ICONIFIED);

        initLogger(consoleTextArea);

        logConfigAnalyzerConfigurer.configure();
        logManager.start();

        return frame;
    }

    private void initLogger(JTextArea consoleTextArea) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{HH:mm:ss} - %msg%n");
        encoder.start();

        OutputStreamAppender<ILoggingEvent> appender= new OutputStreamAppender<>();
        appender.setName( "OutputStream Appender" );
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setOutputStream(new TextAreaOutputStream(consoleTextArea));

        appender.start();

        ch.qos.logback.classic.Logger log = context.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        log.setLevel(Level.ERROR);
        log.setAdditive(false);
        log.addAppender(appender);

        log = context.getLogger("com.hearthgames.client");
        log.setLevel(Level.INFO);
        log.setAdditive(false);
        log.addAppender(appender);
    }
}