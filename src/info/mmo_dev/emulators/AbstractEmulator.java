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
import java.util.Collection;

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
                case Lucera: {
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
                                Method methodKick = target.getClass().getDeclaredMethod("logout");
                                methodKick.invoke(target);
                            }
                        }
                    }
                    break;
                }
                case PainTeam: {
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
                                Method methodKick = target.getClass().getDeclaredMethod("kick");
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
}
