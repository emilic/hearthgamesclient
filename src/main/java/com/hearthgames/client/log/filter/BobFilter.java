package com.hearthgames.client.log.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BobFilter implements LogFilter {

    @Override
    public String filter(String line) {
        String filteredLine = null;
        if (!StringUtils.isEmpty(line) && line.startsWith("[Bob] ---Register")) {
            filteredLine = line.replace("[Bob] ", "");
        }
        return filteredLine;
    }
}
