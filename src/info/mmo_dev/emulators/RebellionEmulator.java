package info.mmo_dev.emulators;

import info.mmo_dev.Config;
import info.mmo_dev.DatabaseHelper;
import info.mmo_dev.Utils;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RebellionEmulator extends AbstractEmulator  {

    private static final String _basePackage = "l2r.gameserver";

    public RebellionEmulator() {
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

        /*try {
            Class<?> classPlayer = Class.forName(_basePackage + ".model.Player");
            Method restorePlayerMethod = classPlayer.getDeclaredMethod("restore", int.class);

            setPlayerObject(restorePlayerMethod.invoke(classPlayer, Config.CHAR_ID));
        } catch (Exception e) {
            //
        }*/
    }

    @Override
    public EmulatorType getType() {
        return EmulatorType.Rebellion;
    }

    @Override
    public String executeShutdownSchedule(int seconds, boolean isRestart) {
        Object shutdownObject = getShutdownObject();
        if (shutdownObject == null)
            return "shutdownObject == null [" + getType() + "]";

        String result = null;
        try {
            Method method = shutdownObject.getClass().getDeclaredMethod("schedule", int.class, int.class);
            method.invoke(shutdownObject, seconds, isRestart ? 2 : 0);
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

        String result = null;
        try {
            Method method = shutdownObject.getClass().getDeclaredMethod("cancel");
            method.invoke(shutdownObject);
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    @Override
    public String getThreadPoolStatus() {
        Object threadPoolObject = getThreadPoolObject();
        if (threadPoolObject == null)
            return "threadPoolObject == null [" + getType() + "]";

        String result;
        try {
            Method method = threadPoolObject.getClass().getDeclaredMethod("getStats");
            result = ((StringBuilder) method.invoke(threadPoolObject)).toString();
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
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
            Class<?> classAuthServerCommunication = Class.forName(_basePackage + ".network.loginservercon.AuthServerCommunication");
            Method methodInstance = classAuthServerCommunication.getDeclaredMethod("getInstance");
            Object authServerCommunicationInstance = methodInstance.invoke(null);

            Object classChangeAccessLevel = Class.forName(_basePackage + ".network.loginservercon.gspackets.ChangeAccessLevel")
                    .getDeclaredConstructor(new Class<?>[]{String.class, int.class, int.class})
                    .newInstance(accountName, cancel ? 0 : -100, 0);

            Class<?> classSendablePacket = Class.forName(_basePackage + ".network.loginservercon.SendablePacket");

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
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }
}
