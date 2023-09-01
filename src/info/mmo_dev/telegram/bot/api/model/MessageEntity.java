package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#messageentity">MessageEntity</a>
 */
public class MessageEntity {
    public String type;

    public int offset;

    public int length;

    public String url;

    public User user;

    public String language;

    public String custom_emoji_id;
}
