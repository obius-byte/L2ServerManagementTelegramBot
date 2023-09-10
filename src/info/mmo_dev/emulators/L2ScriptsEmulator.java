package info.mmo_dev.emulators;

import java.lang.reflect.Method;

public class L2ScriptsEmulator implements EmulatorAdapter {
    private static final String _basePackage = "l2s.gameserver";

    private Object _shutdownInstance;

    private Object _threadPoolInstance;

    public L2ScriptsEmulator() {
        try {
            Class<?> classShutdown = Class.forName(_basePackage + ".Shutdown");
            Method methodInstance = classShutdown.getDeclaredMethod("getInstance");
            _shutdownInstance = methodInstance.invoke(null);
        } catch (Exception e) {
            //
        }

        try {
            Class<?> classThreadPool = Class.forName(_basePackage + ".ThreadPoolManager");
            Method methodInstance = classThreadPool.getDeclaredMethod("getInstance");
            _threadPoolInstance = methodInstance.invoke(null);
        } catch (Exception e) {
            //
        }
    }

    @Override
    public String getBasePackage() {
        return _basePackage;
    }

    @Override
    public EmulatorType getType() {
        return EmulatorType.L2Scripts;
    }

    @Override
    public Object getShutdownObject() {
        return _shutdownInstance;
    }

    @Override
    public Object getThreadPoolObject() {
        return _threadPoolInstance;
    }
}
