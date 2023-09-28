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

public class PainTeamEmulator extends AbstractEmulator {

    private static final String _basePackage = "l2p.gameserver";

    public PainTeamEmulator() {
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
    }

    @Override
    public EmulatorType getType() {
        return EmulatorType.PainTeam;
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
            Class<?> classLoginServerThread = Class.forName(_basePackage + ".LoginServerThread");
            Method methodLoginServerThreadInstance = classLoginServerThread.getDeclaredMethod("getInstance");
            Object loginServerThreadInstance = methodLoginServerThreadInstance.invoke(null);
            Method sendAccessLevel = loginServerThreadInstance.getClass().getDeclaredMethod("sendAccessLevel", String.class, int.class);
            sendAccessLevel.invoke(loginServerThreadInstance, accountName, cancel ? 0 : -100);

            if (!cancel) {
                Class<?> classWorld = Class.forName(_basePackage + ".model.L2World");
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
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }
}
