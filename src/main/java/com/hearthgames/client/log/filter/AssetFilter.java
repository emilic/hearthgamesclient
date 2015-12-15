package com.hearthgames.client.log.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AssetFilter implements LogFilter {

    @Override
    public String filter(String line) {
        String filteredLine = null;
        if (!StringUtils.isEmpty(line) && line.startsWith("[Asset] CachedAsset.UnloadAssetObject() - unloading name=Medal_Ranked")) {
            filteredLine = line.replace("[Asset] CachedAsset.UnloadAssetObject() - ", "");
        }
        return filteredLine;
    }
}
