package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#audio">Audio</a>
 */
public class Audio {
    public String file_id;

    public String file_unique_id;

    public int duration;

    public String performer;

    public String title;

    public String file_name;

    public String mime_type;

    public String file_size;
}
