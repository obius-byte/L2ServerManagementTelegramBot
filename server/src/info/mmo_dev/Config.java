package info.mmo_dev;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    public static String URL, USERNAME, PASSWORD;

    public static void init(Emulator emulator) {
        File file = null;
        if (emulator == Emulator.PWSOFT || emulator == Emulator.RebellionTeam || emulator == Emulator.MobiusDev) {
            file = new File("./config/server.properties");
            if (!file.exists()) {
                file = new File("./config/server.cfg");
            }
        }
        else if (emulator == Emulator.L2Scripts) {
            file = new File("./config/database.properties");
            if (!file.exists()) {
                file = new File("./config/server.properties");
            }
        }

        if (file != null && file.exists()) {
            Properties props = new Properties();
            try (FileInputStream is = new FileInputStream(file)) {
                props.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (emulator == Emulator.L2Scripts) {
                URL = props.getProperty("jdbcUrl");
                if (URL != null) {
                    URL = props.getProperty("jdbcUrl");
                    USERNAME = props.getProperty("dataSource.user");
                    PASSWORD = props.getProperty("dataSource.password");
                } else {
                    URL = "jdbc:mysql://" + props.getProperty("DATABASE_HOST");
                    URL += ":" + props.getProperty("DATABASE_PORT");
                    URL += "/" + props.getProperty("DATABASE_NAME");
                    URL += "?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC";
                    USERNAME = props.getProperty("DATABASE_LOGIN");
                    PASSWORD = props.getProperty("DATABASE_PASSWORD");
                }
            } else {
                URL = props.getProperty("URL");
                USERNAME = props.getProperty("Login");
                PASSWORD = props.getProperty("Password");
            }
        } else {
            System.out.println("File " + ( file != null ? file.getPath() : "null" ) + " is not found!" );
        }
    }
}
