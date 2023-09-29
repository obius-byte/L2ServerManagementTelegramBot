package info.mmo_dev.emulators;

import info.mmo_dev.Config;

import java.lang.reflect.Method;

public class L2jEternityEmulator extends AbstractEmulator {

    public L2jEternityEmulator() {
        super("l2e.gameserver");

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

        try {
            Class<?> classCharacterDAO = Class.forName(getBasePackage() + ".data.dao.CharacterDAO");
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
}
