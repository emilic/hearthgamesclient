package com.hearthgames.client.log.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * This class attempts to filter hearthstone log lines that provide game events only.
 *
 * Examples:
 *   TAG_CHANGE Entity=[name=Acolyte of Pain id=28 zone=HAND zonePos=7 cardId=EX1_007 player=1] tag=ZONE_POSITION value=6
 *
 *   ACTION_START Entity=[name=Ice Lance id=8 zone=HAND zonePos=1 cardId=CS2_031 player=1] BlockType=POWER Index=-1 Target=[name=Azure Drake id=40 zone=PLAY zonePos=4 cardId=EX1_284 player=2]
 *       TAG_CHANGE Entity=[name=Azure Drake id=40 zone=PLAY zonePos=4 cardId=EX1_284 player=2] tag=FROZEN value=1
 *   ACTION_END
 *
 */
@Component
public class PowerFilter implements LogFilter {

    /**
     * The filter method accepts only log lines starting with DebugPrintPower
     *
     * @param line a log line from the Hearthstone log file
     * @return the line stripped of the unwanted prefix, trimmed of leading or trailing whitespace, or null indicating the log line is not relevant
     */
    @Override
    public String filter(String line) {
        String filteredLine = null;
        if (!StringUtils.isEmpty(line) && line.startsWith("[Power] GameState.DebugPrintPower() - ")) {
            filteredLine = line.replace("[Power] GameState.DebugPrintPower() - ", "");
        }
        return filteredLine;
    }
}
