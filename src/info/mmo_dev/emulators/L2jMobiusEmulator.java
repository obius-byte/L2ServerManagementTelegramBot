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

public class L2jMobiusEmulator extends AbstractEmulator {

    private static final String _basePackage = "org.l2jmobius";

    public L2jMobiusEmulator() {
        try {
            Class<?> classShutdown = Class.forName(_basePackage + ".gameserver.Shutdown");
            Method methodInstance = classShutdown.getDeclaredMethod("getInstance");

            setShutdownObject(methodInstance.invoke(null));
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classThreadPool = Class.forName(_basePackage + ".commons.threads.ThreadPool");

            setThreadPoolObject(classThreadPool.newInstance());
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classPlayer = Class.forName(_basePackage + ".gameserver.model.actor.Player");
            Method restorePlayerMethod = classPlayer.getDeclaredMethod("load", int.class);

            setPlayerObject(restorePlayerMethod.invoke(classPlayer, Config.CHAR_ID));
        } catch (Exception e) {
            //
        }
    }

    @Override
    public EmulatorType getType() {
        return EmulatorType.L2jMobius;
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
        Object threadPoolObject = getThreadPoolObject();
        if (threadPoolObject == null)
            return "threadPoolObject == null [" + getType() + "]";

        String result;
        try {
            Method method = threadPoolObject.getClass().getDeclaredMethod("getStats");
            result = String.join("\n", (String[]) method.invoke(threadPoolObject));
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
            Class<?> classLoginServerThread = Class.forName(_basePackage + ".gameserver.LoginServerThread");
            Method methodLoginServerThreadInstance = classLoginServerThread.getDeclaredMethod("getInstance");
            Object loginServerThreadInstance = methodLoginServerThreadInstance.invoke(null);
            Method sendAccessLevel = loginServerThreadInstance.getClass().getDeclaredMethod("sendAccessLevel", String.class, int.class);
            sendAccessLevel.invoke(loginServerThreadInstance, accountName, cancel ? 0 : -100);

            if (!cancel) {
                Class<?> classWorld = Class.forName(_basePackage + ".gameserver.model.World");
                Method methodInstance = classWorld.getDeclaredMethod("getInstance");
                Object worldInstance = methodInstance.invoke(classWorld);
                Method methodGetAllPlayers = worldInstance.getClass().getDeclaredMethod("getPlayers");
                Collection<Object> playerList = (Collection<Object>) methodGetAllPlayers.invoke(worldInstance);

                Class<?> classLeaveWorld = Class.forName(_basePackage + ".gameserver.network.serverpackets.LeaveWorld");
                Field fieldStaticPacket = classLeaveWorld.getDeclaredField("STATIC_PACKET");
                Object staticPacket = fieldStaticPacket.get(classLeaveWorld);

                Class<?> classServerPacket = Class.forName(_basePackage + ".gameserver.network.serverpackets.ServerPacket");

                for (Object target : playerList) {
                    Method methodGetAccountName = target.getClass().getDeclaredMethod("getAccountName");
                    String targetAccountName = (String) methodGetAccountName.invoke(target);

                    if (accountName.equals(targetAccountName)) {
                        Method methodSendPacket = target.getClass().getDeclaredMethod("sendPacket", classServerPacket);
                        methodSendPacket.invoke(target, staticPacket);
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
