package info.mmo_dev.emulators;

import java.sql.SQLException;

public interface EmulatorAdapter {

    EmulatorType getType();

    String executeShutdownSchedule(int seconds, boolean isRestart, boolean cancel);

    String getThreadPoolStatus();

    String banAccount(String accountName, boolean cancel);

    String banCharacter(String characterName, boolean cancel);

    int getCountOnline() throws SQLException;

    String getItemsDelayedStatus() throws SQLException;

    String addItem(String charName, int itemId, int itemCount) throws SQLException;
}
