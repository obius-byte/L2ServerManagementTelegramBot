package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#videonote">VideoNote</a>
 */
public class VideoNote {
    public String file_id;

    public String file_unique_id;

    public int length;

    public int duration;

    public PhotoSize thumbnail;

    public int file_size;
}
