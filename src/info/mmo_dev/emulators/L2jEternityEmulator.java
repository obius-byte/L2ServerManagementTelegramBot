package info.mmo_dev.emulators;

import info.mmo_dev.Config;
import info.mmo_dev.DatabaseHelper;
import info.mmo_dev.Utils;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class L2jEternityEmulator extends AbstractEmulator {

    private static final String _basePackage = "l2e.gameserver";

    public L2jEternityEmulator() {
        try {
            Class<?> classShutdown = Class.forName(_basePackage + ".Shutdown");
            Method methodInstance = classShutdown.getDeclaredMethod("getInstance");

            setShutdownObject(methodInstance.invoke(null));
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classThreadPool = Class.forName(_basePackage + ".ThreadPoolManager");
            Method methodInstance = classThreadPool.getDeclaredMethod("getInstance");

            setThreadPoolObject(methodInstance.invoke(null));
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classCharacterDAO = Class.forName(_basePackage + ".data.dao.CharacterDAO");
            Method characterDAOInstanceMethod = classCharacterDAO.getDeclaredMethod("getInstance");
            Object characterDAOInstance = characterDAOInstanceMethod.invoke(null);
            Method restorePlayerMethod = characterDAOInstance.getClass().getDeclaredMethod("restore", int.class);

            setPlayerObject(restorePlayerMethod.invoke(characterDAOInstance, Config.CHAR_ID));
        } catch (Exception e) {
            //
        }
    }

    @Override
    public EmulatorType getType() {
        return EmulatorType.L2jEternity;
    }

    @Override
    public String executeShutdownSchedule(int seconds, boolean isRestart) {
        Object shutdownObject = getShutdownObject();
        if (shutdownObject == null)
            return "shutdownObject == null [" + getType() + "]";

        Object playerObject = getPlayerObject();
        if (playerObject == null)
            return "playerObject == null [" + getType() + "]";

        String result = null;
        try {
            Method method = shutdownObject.getClass().getDeclaredMethod("startShutdown", playerObject.getClass(), int.class, boolean.class);
            method.invoke(shutdownObject, playerObject, seconds, isRestart);
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    @Override
    public String executeShutdownAbort() {
        Object shutdownObject = getShutdownObject();
        if (shutdownObject == null)
            return "shutdownObject == null [" + getType() + "]";

        Object playerObject = getPlayerObject();
        if (playerObject == null)
            return "playerObject == null [" + getType() + "]";

        String result = null;
        try {
            Method method = shutdownObject.getClass().getDeclaredMethod("abort", playerObject.getClass());
            method.invoke(shutdownObject, playerObject);
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    @Override
    public String getThreadPoolStatus() {
        /*Object threadPoolObject = getThreadPoolObject();
        if (threadPoolObject == null)
            return "threadPoolObject == null [" + getType() + "]";

        try {
            Method method = threadPoolObject.getClass().getDeclaredMethod("getInfo"); // TODO: wtf!?
            method.invoke(threadPoolObject);
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            Utils.getStackTrace(e);
        }*/
        throw new UnsupportedOperationException("Not implemented [" + getType() + "]");
    }

    @Override
    public String banAccount(String accountName, boolean cancel) {
        String result = null;
        String sqlSelect = "SELECT login FROM accounts WHERE login = ?";
        try (Connection connection = DatabaseHelper.getAuthConnection();
             PreparedStatement accountsStatement = connection.prepareStatement(sqlSelect)) {
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

        try {
            Class<?> classAuthServerCommunication = Class.forName(_basePackage + ".network.communication.AuthServerCommunication");
            Method methodInstance = classAuthServerCommunication.getDeclaredMethod("getInstance");
            Object authServerCommunicationInstance = methodInstance.invoke(null);

            Object classChangeAccessLevel = Class.forName(_basePackage + ".network.communication.gameserverpackets.ChangeAccessLevel")
                    .getDeclaredConstructor(new Class<?>[]{String.class, int.class, int.class})
                    .newInstance(accountName, cancel ? 0 : -100, 0);

            Class<?> classSendablePacket = Class.forName(_basePackage + ".network.communication.SendablePacket");

            Method methodSendPacket = authServerCommunicationInstance.getClass().getDeclaredMethod("sendPacket", classSendablePacket);
            methodSendPacket.invoke(authServerCommunicationInstance, classChangeAccessLevel);

            if (!cancel) {
                Class<?> classGameObjectsStorage = Class.forName(_basePackage + ".model.GameObjectsStorage");
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
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }
}
