package info.mmo_dev;

public enum Emulator {
    RebellionTeam("l2r.gameserver"),
    //MobiusDev("org.l2jmobius.gameserver"),
    //L2Scripts("l2s.gameserver"),
    PWSOFT("net.sf.l2j.gameserver");

    private final String _path;

    Emulator(String path) {
        _path = path;
    }

    public String getPath() {
        return _path;
    }

    public static final Emulator[] values = values();
}
