package info.mmo_dev.emulators;

import java.lang.reflect.Method;

public class PwSoftEmulator implements EmulatorAdapter {
    private static final String _basePackage = "net.sf.l2j.gameserver";

    private Object _shutdownInstance;

    private Object _threadPoolInstance;

    public PwSoftEmulator() {
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
        return EmulatorType.PwSoft;
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
