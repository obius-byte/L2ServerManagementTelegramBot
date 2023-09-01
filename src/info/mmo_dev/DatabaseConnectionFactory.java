package info.mmo_dev;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnectionFactory {
    public static Connection getAuthConnection() throws SQLException {
        return DriverManager.getConnection(Config.DB_AUTH_URL, Config.DB_AUTH_USER, Config.DB_AUTH_PASSWORD);
    }

    public static Connection getGameConnection() throws SQLException {
        return DriverManager.getConnection(Config.DB_GAME_URL, Config.DB_GAME_USER, Config.DB_GAME_PASSWORD);
    }
}
