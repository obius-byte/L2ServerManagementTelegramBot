package info.mmo_dev.emulators;

import java.lang.reflect.Method;

public class LuceraEmulator extends AbstractEmulator {

    public LuceraEmulator() {
        super("ru.catssoftware.gameserver");

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
        return EmulatorType.Lucera;
    }
}
