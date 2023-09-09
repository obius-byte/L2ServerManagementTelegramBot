package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#voice">Voice</a>
 */
public class Voice {
    public String file_id;

    public String file_unique_id;

    public int duration;

    public String mime_type;

    public int file_size;
}
