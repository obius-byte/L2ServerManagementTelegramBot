package info.mmo_dev.emulators;

public interface EmulatorAdapter {

    EmulatorType getType();

    String executeShutdownSchedule(int seconds, boolean isRestart, boolean cancel);

    String getThreadPoolStatus();

    String banAccount(String accountName, boolean cancel);

    String banCharacter(String characterName, boolean cancel);
}
