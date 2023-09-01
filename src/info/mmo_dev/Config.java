package info.mmo_dev;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Config {
    public static String DB_AUTH_DRIVER, DB_GAME_DRIVER,
            DB_AUTH_URL, DB_GAME_URL,
            DB_AUTH_USER, DB_GAME_USER,
            DB_AUTH_PASSWORD, DB_GAME_PASSWORD,
            BOT_TOKEN;

    public static List<Long> USER_IDS;

    private static final String PATHNAME = "./config/delayed-tasks-manager.properties";

    public static void initialize() throws Exception {
        File file = new File(PATHNAME);
        if (!file.exists()) {
            throw new FileNotFoundException("File " + file.getCanonicalPath() + " is not found!");
        }

        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream(file)) {
            props.load(is);
        }

        //DB_AUTH_DRIVER = props.getProperty("db.auth.driver");
        DB_AUTH_URL = props.getProperty("db.auth.url");
        DB_AUTH_USER = props.getProperty("db.auth.user");
        DB_AUTH_PASSWORD = props.getProperty("db.auth.password");

        //DB_AUTH_DRIVER = props.getProperty("db.game.driver");
        DB_GAME_URL = props.getProperty("db.game.url");
        DB_GAME_USER = props.getProperty("db.game.user");
        DB_GAME_PASSWORD = props.getProperty("db.game.password");

        BOT_TOKEN = props.getProperty("telegram.bot.api.token");
        String userIdsString = props.getProperty("telegram.bot.api.user.ids");
        USER_IDS = new ArrayList<>();
        if (userIdsString != null && userIdsString.length() > 0) {
            String[] userIds = userIdsString.split("\\s*,\\s*");
            for (String userId: userIds) {
                USER_IDS.add(Long.parseLong(userId));
            }
        }
    }
}
