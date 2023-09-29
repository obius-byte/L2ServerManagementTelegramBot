package info.mmo_dev.emulators;

import java.lang.reflect.Method;

public class PwSoftEmulator extends AbstractEmulator {

    public PwSoftEmulator() {
        super("net.sf.l2j.gameserver");

        try {
            Class<?> classShutdown = Class.forName(getBasePackage() + ".Shutdown");
            Method methodInstance = classShutdown.getDeclaredMethod("getInstance");

            setShutdownObject(methodInstance.invoke(null));
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classThreadPool = Class.forName(getBasePackage() + ".ThreadPoolManager");
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
