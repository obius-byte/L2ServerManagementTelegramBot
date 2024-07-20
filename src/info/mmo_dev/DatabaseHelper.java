package info.mmo_dev;

import java.sql.*;
import java.util.*;

public final class DatabaseHelper {

    public static class SQLTableStruct {
        final String _name;
        final List<SQLColumnStruct> _columns;

        public SQLTableStruct(String name) {
            _name = name;
            _columns = new ArrayList<>();
        }

        String getName() {
            return _name;
        }

        void addColumn(SQLColumnStruct column) {
            _columns.add(column);
        }

        public SQLColumnStruct getColumn(String name) {
            for (SQLColumnStruct column : _columns) {
                if (column.getName().equals(name)) {
                    return column;
                }
            }
            return null;
        }

        public List<SQLColumnStruct> getColumns() {
            return _columns;
        }

        public boolean columnExists(String name) {
            return getColumn(name) != null;
        }
    }

    public static class SQLColumnStruct {
        final String _name;
        final String _type;
        final String _null;
        final String _key;
        final String _default;
        final String _extra;

        public SQLColumnStruct(String name, String type, String _null, String key, String _default, String extra) {
            _name = name;
            _type = type;
            this._null = _null;
            _key = key;
            this._default = _default;
            _extra = extra;
        }

        public String getName() {
            return _name;
        }

        public String getExtra() {
            return _extra;
        }
    }

    private static final List<SQLTableStruct> _tables = new ArrayList<>();

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
                addTable(new SQLTableStruct(tableName));
                try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                     ResultSet resultSet2 = statement2.executeQuery()) {
                    while (resultSet2.next()) {
                        getTable(tableName).addColumn(new SQLColumnStruct(
                                resultSet2.getString("field"),
                                resultSet2.getString("type"),
                                resultSet2.getString("null"),
                                resultSet2.getString("key"),
                                resultSet2.getString("default"),
                                resultSet2.getString("extra")));
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

                    addTable(new SQLTableStruct(tableName));
                    try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                         ResultSet resultSet2 = statement2.executeQuery()) {
                        while (resultSet2.next()) {
                            getTable(tableName).addColumn(new SQLColumnStruct(
                                    resultSet2.getString("field"),
                                    resultSet2.getString("type"),
                                    resultSet2.getString("null"),
                                    resultSet2.getString("key"),
                                    resultSet2.getString("default"),
                                    resultSet2.getString("extra")));
                        }
                    }
                }
            }
        }

        /*for (SQLTableStruct table: _tables) {
            System.out.println(table.getName());
            for (SQLColumnStruct column: table.getColumns())
            {
                System.out.println("\t" + column._key);
            }
        }*/
    }

    public static SQLTableStruct getTable(String name) {
        for (SQLTableStruct table : _tables) {
            if (table.getName().equals(name)) {
                return table;
            }
        }
        return null;
    }

    private static void addTable(SQLTableStruct table) {
        _tables.add(table);
    }

    public static boolean tableExists(String name) {
        return getTable(name) != null;
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
        return executeUpdate(sql, null);
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
        return getEntity(sql, parameters, true);
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
        return getEntities(sql, parameters, true);
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
                    for (String columnName : columnNames) {
                        entity.put(columnName, rs.getString(columnName));
                    }
                    entities.add(entity);
                }
            }
        }

        return entities;
    }
}
