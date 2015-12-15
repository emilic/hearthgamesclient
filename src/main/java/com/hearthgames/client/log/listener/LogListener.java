package com.hearthgames.client.log.listener;

import com.hearthgames.client.log.filter.BobFilter;
import com.hearthgames.client.log.filter.PowerFilter;
import com.hearthgames.client.log.event.LineReadEvent;
import com.hearthgames.client.log.filter.AssetFilter;
import com.hearthgames.client.match.GameRecorder;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

@Component
public class LogListener extends TailerListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LogListener.class);

    @Autowired
    private GameRecorder gameRecorder;

    private PowerFilter powerFilter;
    private AssetFilter assetFilter;
    private BobFilter bobFilter;

    @Autowired
    public LogListener(PowerFilter powerFilter, AssetFilter assetFilter, BobFilter bobFilter) {
        this.powerFilter = powerFilter;
        this.assetFilter = assetFilter;
        this.bobFilter = bobFilter;
    }


    @Override
    public void handle(String line) {
        if (!StringUtils.isEmpty(line)) {
            LineReadEvent event = null;
            String filteredLine = powerFilter.filter(line);
            if (filteredLine != null) {
                event = new LineReadEvent(this, filteredLine, true);
            }
            filteredLine = assetFilter.filter(line);
            if (filteredLine != null) {
                event = new LineReadEvent(this, filteredLine, false);
            }
            filteredLine = bobFilter.filter(line);
            if (filteredLine != null) {
                event = new LineReadEvent(this, filteredLine, false);
            }
            if (event != null) {
                gameRecorder.handleLine(event);
            }
        }
    }

    public void handle(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        logger.error(sw.toString());
    }
}
