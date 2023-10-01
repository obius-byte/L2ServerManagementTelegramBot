package info.mmo_dev;

import java.sql.*;
import java.util.*;

public final class DatabaseHelper {

    private static final Map<String, List<String>> _tables = new Hashtable<>();

    public static Connection getAuthConnection() throws SQLException {
        return DriverManager.getConnection(Config.DB_AUTH_URL, Config.DB_AUTH_USER, Config.DB_AUTH_PASSWORD);
    }

    public static Connection getGameConnection() throws SQLException {
        return DriverManager.getConnection(Config.DB_GAME_URL, Config.DB_GAME_USER, Config.DB_GAME_PASSWORD);
    }

    public static void initialize() throws SQLException {
        try (Connection connection = getGameConnection();
             PreparedStatement statement1 = connection.prepareStatement("SHOW TABLES");
             ResultSet resultSet1 = statement1.executeQuery()) {
            while (resultSet1.next()) {
                String tableName = resultSet1.getString(1);
                putTable(tableName, new ArrayList<>());
                try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                     ResultSet resultSet2 = statement2.executeQuery()) {
                    while (resultSet2.next()) {
                        String columnName = resultSet2.getString(1);
                        getTable(tableName).add(columnName);
                    }
                }
            }
        }

        if (!Config.DB_AUTH_URL.equals(Config.DB_GAME_URL)) {
            try (Connection connection = getAuthConnection();
                 PreparedStatement statement1 = connection.prepareStatement("SHOW TABLES");
                 ResultSet resultSet1 = statement1.executeQuery()) {
                while (resultSet1.next()) {
                    String tableName = resultSet1.getString(1);

                    if (tableExists(tableName)) {
                        System.out.println("DelayedTasksManager: Duplicate of table " + tableName + " found. Skipped!");
                        continue;
                    }

                    putTable(tableName, new ArrayList<>());
                    try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                         ResultSet resultSet2 = statement2.executeQuery()) {
                        while (resultSet2.next()) {
                            String columnName = resultSet2.getString(1);
                            getTable(tableName).add(columnName);
                        }
                    }
                }
            }
        }
    }

    public static List<String> getTable(String key) {
        return _tables.get(key);
    }

    private static void putTable(String key, List<String> value) {
        _tables.put(key, value);
    }

    public static boolean tableExists(String key) {
        return _tables.containsKey(key);
    }

    private static PreparedStatement getPreparedStatement(Connection connection, String sql, List<Object> parameters)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        if (parameters != null && parameters.size() > 0) {
            for (int i = 0; i < parameters.size(); i++) {
                Object parameter = parameters.get(i);
                if (parameter instanceof Integer) {
                    statement.setInt(i + 1, (int) parameter);
                } else if (parameter instanceof String) {
                    statement.setString(i + 1, (String) parameter);
                } else if (parameter instanceof Long) {
                    statement.setLong(i + 1, (long) parameter);
                } else {
                    throw new SQLException("DatabaseHelper: Unknown parameter type: " + parameter.getClass());
                }
            }
        }

        return statement;
    }

    public static int executeUpdate(String sql) throws SQLException {
        return executeUpdate(sql,null);
    }

    public static int executeUpdate(String sql, List<Object> parameters) throws SQLException {
        return executeUpdate(sql, parameters, true);
    }

    public static int executeUpdate(String sql, List<Object> parameters, boolean inGameDB) throws SQLException {
        try (Connection connection = inGameDB ? getGameConnection() : getAuthConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, parameters)) {
            return statement.executeUpdate();
        }
    }

    public static Map<String, String> getEntity(String sql) throws SQLException {
        return getEntity(sql, null);
    }

    public static Map<String, String> getEntity(String sql, List<Object> parameters) throws SQLException {
        return getEntity(sql, parameters,true);
    }

    public static Map<String, String> getEntity(String sql, List<Object> parameters, boolean inGameDB) throws SQLException {
        Map<String, String> entities = new HashMap<>();

        try (Connection connection = inGameDB ? getGameConnection() : getAuthConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, parameters)) {
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String key = meta.getColumnLabel(i); // TODO: getColumnName???
                        String value = rs.getString(key);

                        entities.put(key, value);
                    }
                }
            }
        }

        return entities;
    }

    public static List<Map<String, String>> getEntities(String sql) throws SQLException {
        return getEntities(sql, null);
    }

    public static List<Map<String, String>> getEntities(String sql, List<Object> parameters) throws SQLException {
        return getEntities(sql, parameters,true);
    }

    public static List<Map<String, String>> getEntities(String sql, List<Object> parameters, boolean inGameDB) throws SQLException {
        List<Map<String, String>> entities = new ArrayList<>();

        try (Connection connection = inGameDB ? getGameConnection() : getAuthConnection();
             PreparedStatement statement = getPreparedStatement(connection, sql, parameters)) {
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(meta.getColumnLabel(i)); // TODO: getColumnName???
                }

                while (rs.next()) {
                    Map<String, String> entity = new HashMap<>();
                    for (String columnName: columnNames) {
                        entity.put(columnName, rs.getString(columnName));
                    }
                    entities.add(entity);
                }
            }
        }

        return entities;
    }
}
