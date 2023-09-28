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
                _tables.put(tableName, new ArrayList<>());
                try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                     ResultSet resultSet2 = statement2.executeQuery()) {
                    while (resultSet2.next()) {
                        String columnName = resultSet2.getString(1);
                        _tables.get(tableName).add(columnName);
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

                    if (_tables.containsKey(tableName)) {
                        System.out.println("DelayedTasksManager: Duplicate of table " + tableName + " found. Skipped!");
                        continue;
                    }

                    _tables.put(tableName, new ArrayList<>());
                    try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                         ResultSet resultSet2 = statement2.executeQuery()) {
                        while (resultSet2.next()) {
                            String columnName = resultSet2.getString(1);
                            _tables.get(tableName).add(columnName);
                        }
                    }
                }
            }
        }
    }

    public static List<String> getTable(String key) {
        return _tables.get(key);
    }

    public static boolean tableExists(String key) {
        return _tables.containsKey(key);
    }

    public static String addItem(String charName, int itemId, int itemCount) {
        // TODO: character_premium_items???
        if (!tableExists("items_delayed")
                && !tableExists("z_queued_items")
                && !tableExists("character_donate")
                && !tableExists("character_items")
                && !tableExists("items")) {
            return "Table `items_delayed` and `z_queued_items` and `character_donate` and `character_items` and `items` not found!";
        }

        String sqlSelectChar = "SELECT * FROM characters WHERE char_name = ?";

        try (Connection connection = getGameConnection();
             PreparedStatement select = connection.prepareStatement(sqlSelectChar)) {
            select.setString(1, charName);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    int charId = resultSet.getInt(getTable("characters").contains("obj_Id") ? "obj_Id" : "charId");

                    if (tableExists("items_delayed")
                            || tableExists("z_queued_items")
                            || tableExists("character_items")
                            || tableExists("character_donate")) {
                        String sqlInsert;
                        if (tableExists("items_delayed")) {
                            sqlInsert = "INSERT INTO `items_delayed` ( `owner_id`, `item_id`, `count`, `payment_status`, `description` ) VALUES ( ?, ?, ?, 0, 'Telegram Bot' )";
                        } else if (tableExists("z_queued_items")) {
                            sqlInsert = "INSERT INTO `z_queued_items` ( `char_id`, `name`, `item_id`, `item_count`, `status` ) VALUES ( ?, 'Telegram Bot', ?, ?, 0 )";
                        } else if (tableExists("character_donate")) {
                            sqlInsert = "INSERT INTO `character_donate` ( `obj_Id`, `char_name`, `item_id`, `count`, `enchant`, `given` ) VALUES ( ?, '', ?, ?, 0, 0 )";
                        } else {
                            sqlInsert = "INSERT INTO `character_items` ( `owner_id`, `item_id`, `count`, `status` ) VALUES ( ?, ?, ?, 0 )";
                        }

                        try (PreparedStatement insert = connection.prepareStatement(sqlInsert)) {
                            insert.setInt(1, charId);
                            insert.setInt(2, itemId);
                            insert.setInt(3, itemCount);
                            insert.execute();
                        }
                    } else { // TODO: table items! check character on online?
                        String sqlSelectItem = "SELECT * FROM items WHERE item_id = ? AND owner_id = ?";
                        try (PreparedStatement selectItem = connection.prepareStatement(sqlSelectItem)) {
                            selectItem.setInt(1, itemId);
                            selectItem.setInt(2, charId);
                            try (ResultSet resultSetItem = selectItem.executeQuery()) {
                                if (resultSetItem.next()) {
                                    String sqlUpdateItem = "UPDATE items SET count = count + ? WHERE object_id = ?";
                                    try (PreparedStatement updateItemStatement = connection.prepareStatement(sqlUpdateItem)) {
                                        updateItemStatement.setInt(1, itemCount);
                                        updateItemStatement.setInt(2, resultSetItem.getInt("object_id"));
                                        updateItemStatement.execute();
                                    }
                                } else {
                                    String sqlSelectMaxObjectId = "SELECT MAX(object_id) FROM items";
                                    try (PreparedStatement maxObjectIdStatement = connection.prepareStatement(sqlSelectMaxObjectId);
                                         ResultSet resultMaxObjectId = maxObjectIdStatement.executeQuery()) {
                                        if (resultMaxObjectId.next()) {
                                            int maxObjectId = resultMaxObjectId.getInt(1);
                                            String sqlInsertItem = "INSERT INTO items (object_id, owner_id, item_id, count, enchant_level, loc) VALUES ( ?, ?, ?, ?, 0, 'INVENTORY' )";
                                            try (PreparedStatement insertItemStatement = connection.prepareStatement(sqlInsertItem)) {
                                                insertItemStatement.setInt(1, maxObjectId + 1);
                                                insertItemStatement.setInt(2, charId);
                                                insertItemStatement.setInt(3, itemId);
                                                insertItemStatement.setInt(4, itemCount);
                                                insertItemStatement.execute();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return "Character " + charName + " not found!";
                }
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            return Utils.getStackTrace(e);
        }

        return "Successfully!";
    }

    public static String getItemsDelayedStatus() {
        String result;
        String sqlSelect;

        String charIdColumn = getTable("characters").contains("charId") ? "charId" : "obj_Id";

        if (tableExists("items_delayed")) {
            sqlSelect = "SELECT i.*, c.char_name FROM items_delayed AS i LEFT JOIN characters AS c ON (i.owner_id = c." + charIdColumn + ") ORDER BY i.payment_id DESC LIMIT 20";
        } else if (tableExists("z_queued_items")) {
            sqlSelect = "SELECT c.char_name, z.char_id AS owner_id, z.item_id, z.item_count AS count, z.name AS description, z.status AS payment_status FROM z_queued_items AS z LEFT JOIN characters AS c ON (z.char_id = c." + charIdColumn + ") ORDER BY z.id DESC LIMIT 20";
        } else if (tableExists("character_donate")) {
            sqlSelect = "SELECT c.char_name, cd.obj_Id AS owner_id, cd.item_id, cd.count, cd.given AS payment_status FROM character_donate AS cd LEFT JOIN characters AS c ON (cd.obj_Id = c." + charIdColumn + ") ORDER BY cd.id DESC LIMIT 20";
        } else if (tableExists("character_items")) {
            sqlSelect = "SELECT c.char_name, i.owner_id, i.item_id, i.count, i.status AS payment_status FROM character_items AS i LEFT JOIN characters AS c ON (i.owner_id = c." + charIdColumn + ") ORDER BY id DESC LIMIT 20";
        } else {
            return "Table `items_delayed` and `z_queued_items` and `character_donate` and `character_items` not found!";
        }

        try (Connection connection = getGameConnection();
             PreparedStatement select = connection.prepareStatement(sqlSelect);
             ResultSet resultSet = select.executeQuery()) {
            result = Utils.column("Name/Id", true, 16)
                    + Utils.column("Item", false, 11)
                    + Utils.column("Count", false, 10)
                    + Utils.column("Status", false, 8) + "\n";
            while (resultSet.next()) {
                String charName = resultSet.getString("char_name");
                if (charName == null) {
                    charName = String.valueOf(resultSet.getInt("owner_id"));
                }

                result += Utils.column(charName, true, 16)
                        + Utils.column(String.valueOf(resultSet.getInt("item_id")), false, 11)
                        + Utils.column(String.valueOf(resultSet.getInt("count")), false, 10)
                        + Utils.column(resultSet.getBoolean("payment_status") ? "ok" : "pending", false, 8)
                        + "\n";
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    public static String getOnline() {
        String result = "";

        try (Connection connection = getGameConnection();
             PreparedStatement select = connection.prepareStatement("SELECT COUNT(*) FROM characters WHERE online = 1");
             ResultSet resultSet = select.executeQuery()) {
            if (resultSet.next()) {
                result = "Current online: <b>" + resultSet.getInt(1) + "</b>";
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    // TODO: not impl (limit, offset - pagination)
    public static String getCharactersList() {
        String text = "";
        String sql = "SELECT * FROM characters LIMIT 100";

        try (Connection connection = getGameConnection();
             PreparedStatement select = connection.prepareStatement(sql);
             ResultSet resultSet = select.executeQuery()) {
            text = Utils.column( "Nickname", true, 17 )
                    + Utils.column("Status", false, 10) + "\n";
            while (resultSet.next()) {
                text += Utils.column(resultSet.getString("char_name"), true, 17);
                text += Utils.column(resultSet.getBoolean("online") ? "online" : "offline", false, 10) + "\n";
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            text = Utils.getStackTrace(e);
        }

        return text;
    }
}
