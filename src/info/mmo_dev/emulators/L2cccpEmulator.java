package info.mmo_dev.emulators;

import info.mmo_dev.Config;
import info.mmo_dev.Utils;

import java.lang.reflect.Method;

public class L2cccpEmulator extends AbstractEmulator {

    private String _basePackage;

    public L2cccpEmulator() {
        _basePackage = "l2cccp.gameserver";
        if (Package.getPackage(_basePackage) == null)
            _basePackage = "com." + _basePackage;

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
        return EmulatorType.L2cccp;
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
            if (Config.DEBUG) {
                e.printStackTrace();
            }

            result = Utils.getStackTrace(e);
        }

        return result;
    }
}
