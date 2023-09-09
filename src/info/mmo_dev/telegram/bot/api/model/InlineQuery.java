package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#inlinequery">InlineQuery</a>
 */
public class InlineQuery {
    public String id;

    public User from;

    public String query;

    public String offset;

    public String chat_type;

    public Location location;
}
