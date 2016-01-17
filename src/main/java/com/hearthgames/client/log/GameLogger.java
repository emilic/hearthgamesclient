package com.hearthgames.client.log;

import org.apache.commons.lang3.StringUtils;

public enum GameLogger {

    Power("[Power]", new String[]{"[Power] GameState.DebugPrintPower() - "}),
    Asset("[Asset]", new String[]{"[Asset] CachedAsset.UnloadAssetObject() - "}),
    Bob("[Bob]", new String[]{"[Bob] ---Register"}),
    LoadingScreen("[LoadingScreen]", new String[]{"[LoadingScreen] LoadingScreen.OnSceneLoaded() - prevMode="}),
    Achievements("[Achievements]", new String[]{"[Achievements]"}),
    Arena("[Arena]", new String[]{"[Arena]"}),
    Rachelle("[Rachelle]", new String[]{"[Rachelle]"});

    private String name;
    private String[] filters;

    GameLogger(String name, String[] filters) {
        this.name = name;
        this.filters = filters;
    }

    public String getName() {
        return name;
    }

    public static boolean isLineValid(String line) {
        if (StringUtils.isEmpty(line)) return false;
        for (GameLogger gameLogger : GameLogger.values()) {
            if (matchesLogger(gameLogger, line)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesLogger(GameLogger gameLogger, String line) {
        if (line.contains(gameLogger.name())) {
            for (String filter: gameLogger.filters) {
                if (line.contains(filter)) return true;
            }
        }
        return false;
    }
}
