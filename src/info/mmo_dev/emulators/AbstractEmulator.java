package info.mmo_dev.emulators;

import info.mmo_dev.Config;
import info.mmo_dev.DatabaseHelper;
import info.mmo_dev.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractEmulator implements EmulatorAdapter {

    private Object _shutdownObject = null;

    private Object _threadPoolObject = null;

    private Object _playerObject = null;

    private final String _basePackage;

    protected AbstractEmulator(String basePackage) {
        _basePackage = basePackage;
    }

    protected void setShutdownObject(Object shutdownObject) {
        _shutdownObject = shutdownObject;
    }

    protected Object getShutdownObject() {
        return _shutdownObject;
    }

    protected void setThreadPoolObject(Object threadPoolObject) {
        _threadPoolObject = threadPoolObject;
    }

    protected Object getThreadPoolObject() {
        return _threadPoolObject;
    }

    protected void setPlayerObject(Object playerObject) {
        _playerObject = playerObject;
    }

    protected Object getPlayerObject() {
        return _playerObject;
    }

    protected String getBasePackage() {
        return _basePackage;
    }

    @Override
    public String executeShutdownSchedule(int seconds, boolean isRestart, boolean cancel) {
        String result = null;

        Object shutdownObject = getShutdownObject();
        if (shutdownObject != null) {
            try {
                switch (getType()) {
                    case Acis: {
                        Object playerObject = getPlayerObject();
                        if (playerObject != null) {
                            Method method = shutdownObject.getClass().getDeclaredMethod("startShutdown", playerObject.getClass(), String.class, int.class, boolean.class);
                            method.invoke(shutdownObject, playerObject, null, seconds, isRestart);
                        } else {
                            result = "playerObject == null [" + getType() + "]";
                        }
                        break;
                    }
                    case L2jMobius: case L2jEternity: {
                        Object playerObject = getPlayerObject();
                        if (playerObject != null) {
                            if (cancel) {
                                Method method = shutdownObject.getClass().getDeclaredMethod("abort", playerObject.getClass());
                                method.invoke(shutdownObject, playerObject);
                            } else {
                                Method method = shutdownObject.getClass().getDeclaredMethod("startShutdown", playerObject.getClass(), int.class, boolean.class);
                                method.invoke(shutdownObject, playerObject, seconds, isRestart);
                            }
                        } else {
                            result = "playerObject == null [" + getType() + "]";
                        }
                        break;
                    }
                    case Lucera: {
                        if (cancel) {
                            Method method = shutdownObject.getClass().getDeclaredMethod("abort");
                            method.invoke(shutdownObject);
                        } else {
                            Class<?> shutdownModeType = Class.forName(shutdownObject.getClass().getName() + "$ShutdownModeType");
                            Object[] enums = shutdownModeType.getEnumConstants(); // TODO: 0 - SIGTERM, 1 - SHUTDOWN, 2 - RESTART, 3 - ABORT
                            Method method = shutdownObject.getClass().getDeclaredMethod("startShutdown", String.class, int.class, shutdownModeType);
                            method.invoke(shutdownObject, "Telegram bot", seconds, isRestart ? enums[2] : enums[1]);
                        }
                        break;
                    }
                    case PwSoft: {
                        if (cancel) {
                            Method method = shutdownObject.getClass().getDeclaredMethod("telnetAbort", String.class);
                            method.invoke(shutdownObject, "127.0.0.1");
                        } else {
                            Method method = shutdownObject.getClass().getDeclaredMethod("startTelnetShutdown", String.class, int.class, boolean.class);
                            method.invoke(shutdownObject, "127.0.0.1", seconds, isRestart);
                        }
                        break;
                    }
                    case Rebellion: case PainTeam: case L2Scripts: case L2cccp: {
                        if (cancel) {
                            Method method = shutdownObject.getClass().getDeclaredMethod("cancel");
                            method.invoke(shutdownObject);
                        } else {
                            Method method = shutdownObject.getClass().getDeclaredMethod("schedule", int.class, int.class);
                            method.invoke(shutdownObject, seconds, isRestart ? 2 : 0);
                        }
                        break;
                    }
                    default: {
                        result = "Not implemented [" + getType() + "]";
                    }
                }
            } catch (Exception e) {
                if (Config.DEBUG)
                    e.printStackTrace();

                result = Utils.getStackTrace(e);
            }
        } else {
            result = "shutdownObject == null [" + getType() + "]";
        }

        return result;
    }

    @Override
    public String getThreadPoolStatus() {
        String result;

        Object threadPoolObject = getThreadPoolObject();
        if (threadPoolObject != null) {
            try {
                switch (getType()) {
                    case L2jMobius: {
                        Method method = threadPoolObject.getClass().getDeclaredMethod("getStats");
                        result = String.join("\n", (String[]) method.invoke(threadPoolObject));
                        break;
                    }
                    case PwSoft: {
                        Method method = threadPoolObject.getClass().getDeclaredMethod("getTelemetry");
                        result = (String) method.invoke(threadPoolObject);
                        break;
                    }
                    case Rebellion: case PainTeam: case Lucera: case L2Scripts: case L2cccp: {
                        Method method = threadPoolObject.getClass().getDeclaredMethod("getStats");
                        result = ((StringBuilder) method.invoke(threadPoolObject)).toString();
                        break;
                    }
                    /*case L2jEternity: case Acis: {
                        Method method = threadPoolObject.getClass().getDeclaredMethod("getInfo"); // TODO: get method that does not return data! wtf!?
                        method.invoke(threadPoolObject);
                        break;
                    }*/
                    default: {
                        result = "Not implemented [" + getType() + "]";
                    }
                }
            } catch (Exception e) {
                if (Config.DEBUG)
                    e.printStackTrace();

                result = Utils.getStackTrace(e);
            }
        } else {
            return "threadPoolObject == null [" + getType() + "]";
        }

        return result;
    }

    @Override
    public String banAccount(String accountName, boolean cancel) {
        try (Connection connection = DatabaseHelper.getAuthConnection();
             PreparedStatement accountsStatement = connection.prepareStatement("SELECT login FROM accounts WHERE login = ?")) {
            accountsStatement.setString(1, accountName);
            try (ResultSet account = accountsStatement.executeQuery()) {
                if (!account.next()) {
                    return "Account " + accountName + " not found!";
                }
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            return Utils.getStackTrace(e);
        }

        String result = null;
        try {
            switch (getType()) {
                case L2jEternity: {
                    Class<?> classAuthServerCommunication = Class.forName(getBasePackage() + ".network.communication.AuthServerCommunication");
                    Method methodInstance = classAuthServerCommunication.getDeclaredMethod("getInstance");
                    Object authServerCommunicationInstance = methodInstance.invoke(null);

                    Object classChangeAccessLevel = Class.forName(getBasePackage() + ".network.communication.gameserverpackets.ChangeAccessLevel")
                            .getDeclaredConstructor(new Class<?>[]{String.class, int.class, int.class})
                            .newInstance(accountName, cancel ? 0 : -100, 0);

                    Class<?> classSendablePacket = Class.forName(getBasePackage() + ".network.communication.SendablePacket");

                    Method methodSendPacket = authServerCommunicationInstance.getClass().getDeclaredMethod("sendPacket", classSendablePacket);
                    methodSendPacket.invoke(authServerCommunicationInstance, classChangeAccessLevel);

                    if (!cancel) {
                        Class<?> classGameObjectsStorage = Class.forName(getBasePackage() + ".model.GameObjectsStorage");
                        Method methodGetPlayers = classGameObjectsStorage.getDeclaredMethod("getPlayers");
                        Collection<Object> playerList = (Collection<Object>) methodGetPlayers.invoke(null);

                        for (Object target : playerList) {
                            Method methodGetAccountName = target.getClass().getDeclaredMethod("getAccountName");
                            String targetAccountName = (String) methodGetAccountName.invoke(target);

                            if (accountName.equals(targetAccountName)) {
                                Method methodKick = target.getClass().getDeclaredMethod("kick");
                                methodKick.invoke(target);
                            }
                        }
                    }
                    break;
                }
                case L2jMobius: {
                    Class<?> classLoginServerThread = Class.forName(getBasePackage() + ".gameserver.LoginServerThread");
                    Method methodLoginServerThreadInstance = classLoginServerThread.getDeclaredMethod("getInstance");
                    Object loginServerThreadInstance = methodLoginServerThreadInstance.invoke(null);
                    Method sendAccessLevel = loginServerThreadInstance.getClass().getDeclaredMethod("sendAccessLevel", String.class, int.class);
                    sendAccessLevel.invoke(loginServerThreadInstance, accountName, cancel ? 0 : -100);

                    if (!cancel) {
                        Class<?> classWorld = Class.forName(getBasePackage() + ".gameserver.model.World");
                        Method methodInstance = classWorld.getDeclaredMethod("getInstance");
                        Object worldInstance = methodInstance.invoke(classWorld);
                        Method methodGetAllPlayers = worldInstance.getClass().getDeclaredMethod("getPlayers");
                        Collection<Object> playerList = (Collection<Object>) methodGetAllPlayers.invoke(worldInstance);

                        Class<?> classLeaveWorld = Class.forName(getBasePackage() + ".gameserver.network.serverpackets.LeaveWorld");
                        Field fieldStaticPacket = classLeaveWorld.getDeclaredField("STATIC_PACKET");
                        Object staticPacket = fieldStaticPacket.get(classLeaveWorld);

                        Class<?> classServerPacket = Class.forName(getBasePackage() + ".gameserver.network.serverpackets.ServerPacket");

                        for (Object target : playerList) {
                            Method methodGetAccountName = target.getClass().getDeclaredMethod("getAccountName");
                            String targetAccountName = (String) methodGetAccountName.invoke(target);

                            if (accountName.equals(targetAccountName)) {
                                Method methodSendPacket = target.getClass().getDeclaredMethod("sendPacket", classServerPacket);
                                methodSendPacket.invoke(target, staticPacket);
                            }
                        }
                    }
                    break;
                }
                case Lucera: case PainTeam: {
                    Class<?> classLoginServerThread = Class.forName(getBasePackage() + ".LoginServerThread");
                    Method methodLoginServerThreadInstance = classLoginServerThread.getDeclaredMethod("getInstance");
                    Object loginServerThreadInstance = methodLoginServerThreadInstance.invoke(null);
                    Method sendAccessLevel = loginServerThreadInstance.getClass().getDeclaredMethod("sendAccessLevel", String.class, int.class);
                    sendAccessLevel.invoke(loginServerThreadInstance, accountName, cancel ? 0 : -100);

                    if (!cancel) {
                        Class<?> classWorld = Class.forName(getBasePackage() + ".model.L2World");
                        Method methodInstance = classWorld.getDeclaredMethod("getInstance");
                        Object worldInstance = methodInstance.invoke(classWorld);
                        Method methodGetAllPlayers = worldInstance.getClass().getDeclaredMethod("getAllPlayers");
                        Collection<Object> playerList = (Collection<Object>) methodGetAllPlayers.invoke(worldInstance);

                        for (Object target : playerList) {
                            Method methodGetAccountName = target.getClass().getDeclaredMethod("getAccountName");
                            String targetAccountName = (String) methodGetAccountName.invoke(target);

                            if (accountName.equals(targetAccountName)) {
                                Method methodKick = getType() == EmulatorType.Lucera
                                        ? target.getClass().getDeclaredMethod("logout")
                                        : target.getClass().getDeclaredMethod("kick");
                                methodKick.invoke(target);
                            }
                        }
                    }
                    break;
                }
                case Rebellion: {
                    Class<?> classAuthServerCommunication = Class.forName(getBasePackage() + ".network.loginservercon.AuthServerCommunication");
                    Method methodInstance = classAuthServerCommunication.getDeclaredMethod("getInstance");
                    Object authServerCommunicationInstance = methodInstance.invoke(null);

                    Object classChangeAccessLevel = Class.forName(getBasePackage() + ".network.loginservercon.gspackets.ChangeAccessLevel")
                            .getDeclaredConstructor(new Class<?>[]{String.class, int.class, int.class})
                            .newInstance(accountName, cancel ? 0 : -100, 0);

                    Class<?> classSendablePacket = Class.forName(getBasePackage() + ".network.loginservercon.SendablePacket");

                    Method methodSendPacket = authServerCommunicationInstance.getClass().getDeclaredMethod("sendPacket", classSendablePacket);
                    methodSendPacket.invoke(authServerCommunicationInstance, classChangeAccessLevel);

                    if (!cancel) {
                        Method methodGetAuthedClient = authServerCommunicationInstance.getClass().getDeclaredMethod("getAuthedClient", String.class);
                        Object target = methodGetAuthedClient.invoke(authServerCommunicationInstance, accountName);
                        if (target != null) {
                            Method methodGetActiveChar = target.getClass().getDeclaredMethod("getActiveChar");
                            Object activeChar = methodGetActiveChar.invoke(target);

                            Method methodKick = activeChar.getClass().getDeclaredMethod("kick");
                            methodKick.invoke(activeChar);
                        }
                    }
                    break;
                }
                default: {
                    result ="Not implemented [" + getType() + "]";
                }
            }
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    @Override
    public String banCharacter(String characterName, boolean cancel) {
        return "Not implemented [" + getType() + "]";
    }

    @Override
    public int getCountOnline() throws SQLException {
        Map<String, String> entity = DatabaseHelper.getEntity("SELECT COUNT(*) AS count FROM characters WHERE online = 1");

        return entity.size() > 0 ? Integer.parseInt(entity.get("count")) : 0;
    }

    @Override
    public String getItemsDelayedStatus() throws SQLException {
        String sqlSelect;
        String charIdColumn = DatabaseHelper.getTable("characters").contains("charId") ? "charId" : "obj_Id";

        if (DatabaseHelper.tableExists("items_delayed")) {
            sqlSelect = "SELECT i.*, c.char_name FROM items_delayed AS i LEFT JOIN characters AS c ON (i.owner_id = c." + charIdColumn + ") ORDER BY i.payment_id DESC LIMIT 20";
        } else if (DatabaseHelper.tableExists("z_queued_items")) {
            sqlSelect = "SELECT c.char_name, z.char_id AS owner_id, z.item_id, z.item_count AS count, z.name AS description, z.status AS payment_status FROM z_queued_items AS z LEFT JOIN characters AS c ON (z.char_id = c." + charIdColumn + ") ORDER BY z.id DESC LIMIT 20";
        } else if (DatabaseHelper.tableExists("character_donate")) {
            sqlSelect = "SELECT c.char_name, cd.obj_Id AS owner_id, cd.item_id, cd.count, cd.given AS payment_status FROM character_donate AS cd LEFT JOIN characters AS c ON (cd.obj_Id = c." + charIdColumn + ") ORDER BY cd.id DESC LIMIT 20";
        } else if (DatabaseHelper.tableExists("character_items")) {
            sqlSelect = "SELECT c.char_name, i.owner_id, i.item_id, i.count, i.status AS payment_status FROM character_items AS i LEFT JOIN characters AS c ON (i.owner_id = c." + charIdColumn + ") ORDER BY id DESC LIMIT 20";
        } else {
            return "Table `items_delayed` and `z_queued_items` and `character_donate` and `character_items` not found!";
        }

        String result;
        List<Map<String, String>> entities = DatabaseHelper.getEntities(sqlSelect);
        if (entities.size() > 0) {
            result = Utils.column("Name/Id", true, 16)
                    + Utils.column("Item", false, 11)
                    + Utils.column("Count", false, 10)
                    + Utils.column("Status", false, 8) + "\n";
            for (Map<String, String> entity: entities) {
                String charName = entity.get("char_name");
                if (charName == null)
                    charName = entity.get("owner_id");

                result += Utils.column(charName, true, 16)
                        + Utils.column(entity.get("item_id"), false, 11)
                        + Utils.column(entity.get("count"), false, 10)
                        + Utils.column(entity.get("payment_status").equals("1") ? "ok" : "pending", false, 8)
                        + "\n";
            }
        } else {
            result = "empty";
        }

        return result;
    }

    @Override
    public String addItem(String charName, int itemId, int itemCount) throws SQLException {
        // TODO: character_premium_items???
        if (!DatabaseHelper.tableExists("items_delayed")
                && !DatabaseHelper.tableExists("z_queued_items")
                && !DatabaseHelper.tableExists("character_donate")
                && !DatabaseHelper.tableExists("character_items")
                && !DatabaseHelper.tableExists("items")) {
            return "Table `items_delayed` and `z_queued_items` and `character_donate` and `character_items` and `items` not found!";
        }

        List<Object> parameters = new ArrayList<>();
        parameters.add(charName);

        String charIdColumn = DatabaseHelper.getTable("characters").contains("obj_Id") ? "obj_Id" : "charId";

        Map<String, String> character = DatabaseHelper.getEntity("SELECT " + charIdColumn + " FROM characters WHERE char_name = ?", parameters);
        if (character.size() > 0) {
            int charId = Integer.parseInt(character.get(charIdColumn));

            if (DatabaseHelper.tableExists("items_delayed")
                    || DatabaseHelper.tableExists("z_queued_items")
                    || DatabaseHelper.tableExists("character_items")
                    || DatabaseHelper.tableExists("character_donate")) {
                String sqlInsert;
                if (DatabaseHelper.tableExists("items_delayed")) {
                    sqlInsert = "INSERT INTO `items_delayed` ( `owner_id`, `item_id`, `count`, `payment_status`, `description` ) VALUES ( ?, ?, ?, 0, 'Telegram Bot' )";
                } else if (DatabaseHelper.tableExists("z_queued_items")) {
                    sqlInsert = "INSERT INTO `z_queued_items` ( `char_id`, `name`, `item_id`, `item_count`, `status` ) VALUES ( ?, 'Telegram Bot', ?, ?, 0 )";
                } else if (DatabaseHelper.tableExists("character_donate")) {
                    sqlInsert = "INSERT INTO `character_donate` ( `obj_Id`, `char_name`, `item_id`, `count`, `enchant`, `given` ) VALUES ( ?, '', ?, ?, 0, 0 )";
                } else {
                    sqlInsert = "INSERT INTO `character_items` ( `owner_id`, `item_id`, `count`, `status` ) VALUES ( ?, ?, ?, 0 )";
                }

                parameters.clear();
                parameters.add(charId);
                parameters.add(itemId);
                parameters.add(itemCount);

                DatabaseHelper.executeUpdate(sqlInsert, parameters);
            } else { // TODO: table items! check character on online?
                parameters.clear();
                parameters.add(charId);
                parameters.add(itemId);

                Map<String, String> item = DatabaseHelper.getEntity("SELECT object_id FROM items WHERE owner_id = ? AND item_id = ?", parameters);
                if (item.size() > 0) {
                    parameters.clear();
                    parameters.add(itemCount);
                    parameters.add(Integer.parseInt(item.get("object_id")));

                    DatabaseHelper.executeUpdate("UPDATE items SET count = count + ? WHERE object_id = ?", parameters);
                } else {
                    Map<String, String> entity = DatabaseHelper.getEntity("SELECT MAX(object_id) AS max_object_id FROM items");
                    if (entity.size() > 0) {
                        int maxObjectId = Integer.parseInt(entity.get("max_object_id"));

                        parameters.clear();
                        parameters.add(maxObjectId + 1);
                        parameters.add(charId);
                        parameters.add(itemId);
                        parameters.add(itemCount);
                        // TODO: Field 'loc_data' doesn't have a default value?

                        DatabaseHelper.executeUpdate("INSERT INTO items (object_id, owner_id, item_id, count, enchant_level, loc) VALUES ( ?, ?, ?, ?, 0, 'INVENTORY' )", parameters);
                    }
                }
            }
        } else {
            return "Character " + charName + " not found!";
        }

        return "Successfully!";
    }
}
