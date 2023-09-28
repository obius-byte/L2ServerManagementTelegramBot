package info.mmo_dev.emulators;

import info.mmo_dev.Config;
import info.mmo_dev.Utils;

import java.lang.reflect.Method;

public class PwSoftEmulator extends AbstractEmulator {

    private static final String _basePackage = "net.sf.l2j.gameserver";

    public PwSoftEmulator() {
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
        return EmulatorType.PwSoft;
    }

    @Override
    public String executeShutdownSchedule(int seconds, boolean isRestart) {
        Object shutdownObject = getShutdownObject();
        if (shutdownObject == null)
            return "shutdownObject == null [" + getType() + "]";

        String result = null;
        try {
            Method method = shutdownObject.getClass().getDeclaredMethod("startTelnetShutdown", String.class, int.class, boolean.class);
            method.invoke(shutdownObject, "127.0.0.1", seconds, isRestart);
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
            Method method = shutdownObject.getClass().getDeclaredMethod("telnetAbort", String.class);
            method.invoke(shutdownObject, "127.0.0.1");
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
            Method method = threadPoolObject.getClass().getDeclaredMethod("getTelemetry");
            result = (String) method.invoke(threadPoolObject);
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }

    /*@Override
    public String banCharacter(String characterName, boolean cancel) {
        String result = null;

        String sqlSelect = "SELECT char_name FROM characters WHERE char_name = ?";
        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement statement = connection.prepareStatement(sqlSelect)) {
            statement.setString(1, characterName);
            try (ResultSet characterSet = statement.executeQuery()) {
                if (!characterSet.next()) {
                    return "Character " + characterName + " not found!";
                }
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            return Utils.getStackTrace(e);
        }

        try {
            Class<?> classWorld = Class.forName(_basePackage + ".model.L2World");
            Method methodInstance = classWorld.getDeclaredMethod("getInstance");
            Object worldInstance = methodInstance.invoke(null);
            Method getPlayer = worldInstance.getClass().getDeclaredMethod("getPlayer", String.class);
            Object target = getPlayer.invoke(worldInstance, characterName);
            if (target != null) {
                Method setAccessLevel = target.getClass().getDeclaredMethod("setAccessLevel", int.class);
                setAccessLevel.invoke(target, cancel ? 0 : -100);
                Method kick = target.getClass().getDeclaredMethod("kick");
                kick.invoke(target);
            } else {
                String sqlUpdate = "UPDATE `characters` SET `accesslevel` = ? WHERE `char_name` = ?";
                try (Connection connection = DatabaseConnectionFactory.getGameConnection();
                     PreparedStatement update = connection.prepareStatement(sqlUpdate)) {
                    update.setInt(1, cancel ? 0 : -100);
                    update.setString(2, characterName);
                    update.execute();
                }
            }
        } catch (Exception e) {
            if (Config.DEBUG)
                e.printStackTrace();

            result = Utils.getStackTrace(e);
        }

        return result;
    }*/
}
