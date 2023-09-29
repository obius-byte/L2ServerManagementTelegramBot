package info.mmo_dev.emulators;

import info.mmo_dev.Config;

import java.lang.reflect.Method;

public class L2jMobiusEmulator extends AbstractEmulator {

    public L2jMobiusEmulator() {
        super("org.l2jmobius");

        try {
            Class<?> classShutdown = Class.forName(getBasePackage() + ".gameserver.Shutdown");
            Method methodInstance = classShutdown.getDeclaredMethod("getInstance");

            setShutdownObject(methodInstance.invoke(null));
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classThreadPool = Class.forName(getBasePackage() + ".commons.threads.ThreadPool");

            setThreadPoolObject(classThreadPool.newInstance());
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classPlayer = Class.forName(getBasePackage() + ".gameserver.model.actor.Player");
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
}
