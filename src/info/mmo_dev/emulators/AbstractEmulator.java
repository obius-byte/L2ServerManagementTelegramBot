package info.mmo_dev.emulators;

public abstract class AbstractEmulator implements EmulatorAdapter {

    private Object _shutdownObject = null;

    private Object _threadPoolObject = null;

    private Object _playerObject = null;

    protected void setShutdownObject(Object shutdownObject) {
        _shutdownObject = shutdownObject;
    }

    protected Object getShutdownObject() {
        return _shutdownObject;
    }

    protected void setThreadPoolObject(Object threadPoolObject) {
        _threadPoolObject = threadPoolObject;
    }

    protected Object getThreadPoolObject() {
        return _threadPoolObject;
    }

    protected void setPlayerObject(Object playerObject) {
        _playerObject = playerObject;
    }

    protected Object getPlayerObject() {
        return _playerObject;
    }

    @Override
    public String executeShutdownSchedule(int seconds, boolean isRestart) {
        return "Not implemented [" + getType() + "]";
    }

    @Override
    public String executeShutdownAbort() {
        return "Not implemented [" + getType() + "]";
    }

    @Override
    public String getThreadPoolStatus() {
        return "Not implemented [" + getType() + "]";
    }

    @Override
    public String banAccount(String accountName, boolean cancel) {
        return "Not implemented [" + getType() + "]";
    }

    @Override
    public String banCharacter(String characterName, boolean cancel) {
        return "Not implemented [" + getType() + "]";
    }
}
