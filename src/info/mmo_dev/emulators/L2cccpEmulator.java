package info.mmo_dev.emulators;

import java.lang.reflect.Method;

public class L2cccpEmulator extends AbstractEmulator {

    public L2cccpEmulator() {
        super(Package.getPackage("l2cccp.gameserver") == null ? "com.l2cccp.gameserver" : "l2cccp.gameserver");

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
        return EmulatorType.L2cccp;
    }
}
