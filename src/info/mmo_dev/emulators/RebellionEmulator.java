package info.mmo_dev.emulators;

import java.lang.reflect.Method;

public class RebellionEmulator extends AbstractEmulator  {

    public RebellionEmulator() {
        super("l2r.gameserver");

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

        /*try {
            Class<?> classPlayer = Class.forName(getBasePackage() + ".model.Player");
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
}
