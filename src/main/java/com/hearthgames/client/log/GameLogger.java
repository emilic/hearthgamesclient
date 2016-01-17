package com.hearthgames.client.log;

import org.apache.commons.lang3.StringUtils;

/**
 * The Game Loggers that are supported.  Hearthstone has several loggers that can be enable in the log.config file,
 * the following is a list of loggers and what they contain:
 *
 * [Power]         - contains the game (basically everything that happens in the game happens here.
 * [Asset]         - mainly the unloading of assets are logged here. This is mainly used to detect the Rank of a play mode game.
 * [Bob]           - this logger helps determine the game mode, we see different screens being registered
 * [LoadingScreen] - this logger also helps with determining the game mode
 * [Achievements]  - records the progress of your quests
 * [Arena]         - records the current deck and list of cards each time you play an arena mode game
 * [Rachelle]      - provides info on the progress of adventure mode and contains info about which of your Heros is golden,
 *                   although we can tell which Hero is golden by achievements as well
 */
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
