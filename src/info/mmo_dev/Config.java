package info.mmo_dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Config {
    public static String EMULATOR_TYPE,
            DB_AUTH_DRIVER, DB_GAME_DRIVER,
            DB_AUTH_URL, DB_GAME_URL,
            DB_AUTH_USER, DB_GAME_USER,
            DB_AUTH_PASSWORD, DB_GAME_PASSWORD,
            BOT_TOKEN;

    public static int CHAR_ID;

    public static List<Long> USER_IDS;

    public static boolean DELAYED_ITEMS_LISTENER;

    public static int DELAYED_ITEMS_LISTENER_DELAY;

    public static List<Integer> DELAYED_ITEMS_LISTENER_EXCLUDE_ITEM_IDS;

    public static boolean DEBUG;

    private static final String PATHNAME = "delayed-tasks-manager.properties";

    private static final Properties _props = new Properties();

    public static void initialize() throws Exception {
        File file = new File(PATHNAME);
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file.getCanonicalPath() + " is not found!");
        }

        try (FileInputStream is = new FileInputStream(file)) {
            _props.load(is);
        }

        EMULATOR_TYPE = getProperty("emulator.type");

        //DB_AUTH_DRIVER = props.getProperty("db.auth.driver");
        DB_AUTH_URL = getProperty("db.auth.url");
        DB_AUTH_USER = getProperty("db.auth.user");
        DB_AUTH_PASSWORD = getProperty("db.auth.password");

        //DB_AUTH_DRIVER = props.getProperty("db.game.driver");
        DB_GAME_URL = getProperty("db.game.url");
        DB_GAME_USER = getProperty("db.game.user");
        DB_GAME_PASSWORD = getProperty("db.game.password");

        CHAR_ID = Integer.parseInt(getProperty("game.char.id"));

        BOT_TOKEN = getProperty("telegram.bot.api.token");
        String userIdsString = getProperty("telegram.bot.api.user.ids");
        USER_IDS = new ArrayList<>();
        if (userIdsString.length() > 0) {
            String[] userIds = userIdsString.split("\\s*,\\s*");
            for (String userId: userIds) {
                USER_IDS.add(Long.parseLong(userId));
            }
        }

        DELAYED_ITEMS_LISTENER = Boolean.parseBoolean(getProperty("delayed.items.listener"));
        DELAYED_ITEMS_LISTENER_DELAY = Integer.parseInt(getProperty("delayed.items.listener.delay"));

        String excludeItemIdsString = getProperty("delayed.items.listener.exclude.item.ids");
        DELAYED_ITEMS_LISTENER_EXCLUDE_ITEM_IDS = new ArrayList<>();
        if (excludeItemIdsString.length() > 0) {
            String[] excludeItemIds = excludeItemIdsString.split("\\s*,\\s*");
            for (String excludeItemId: excludeItemIds) {
                DELAYED_ITEMS_LISTENER_EXCLUDE_ITEM_IDS.add(Integer.parseInt(excludeItemId));
            }
        }

        DEBUG = Boolean.parseBoolean(getProperty("debug"));

        _props.clear();
    }

    private static String getProperty(String key) {
        String property = _props.getProperty(key);

        if (property == null)
            throw new IllegalArgumentException("Config `" + key + "` not found!");

        return property;
    }
}
