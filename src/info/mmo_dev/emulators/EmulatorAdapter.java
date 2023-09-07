package info.mmo_dev.emulators;

public interface EmulatorAdapter {

    String getBasePackage();

    EmulatorType getType();

    Object getShutdownObject();

    Object getThreadPoolObject();
}
