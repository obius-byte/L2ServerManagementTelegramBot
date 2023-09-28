package info.mmo_dev.emulators;

public interface EmulatorAdapter {

    EmulatorType getType();

    String executeShutdownSchedule(int seconds, boolean isRestart);

    String executeShutdownAbort();

    String getThreadPoolStatus();

    String banAccount(String accountName, boolean cancel);

    String banCharacter(String characterName, boolean cancel);
}
