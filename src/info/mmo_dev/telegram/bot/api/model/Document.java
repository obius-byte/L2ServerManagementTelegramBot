package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#document">Document</a>
 */
public class Document {
    public String file_id;

    public String file_unique_id;

    public PhotoSize thumbnail;

    public String file_name;

    public String mime_type;

    public int file_size;
}
