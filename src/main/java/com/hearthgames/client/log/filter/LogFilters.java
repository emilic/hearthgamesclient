package com.hearthgames.client.log.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LogFilters {

    private PowerFilter powerFilter;
    private AssetFilter assetFilter;
    private BobFilter bobFilter;

    @Autowired
    public LogFilters(PowerFilter powerFilter, AssetFilter assetFilter, BobFilter bobFilter) {
        this.powerFilter = powerFilter;
        this.assetFilter = assetFilter;
        this.bobFilter = bobFilter;
    }

    public String filter(String line) {
        String filteredLine = powerFilter.filter(line);
        if (filteredLine != null) {
            return filteredLine;
        }
        filteredLine = assetFilter.filter(line);
        if (filteredLine != null) {
            return filteredLine;
        }
        filteredLine = bobFilter.filter(line);
        if (filteredLine != null) {
            return filteredLine;
        }
        return null;
    }
}
