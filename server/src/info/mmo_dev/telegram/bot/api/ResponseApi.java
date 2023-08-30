package info.mmo_dev.telegram.bot.api;

public class ResponseApi<T> {

    public boolean ok;

    public int error_code;

    public String description;

    public T result;
}
