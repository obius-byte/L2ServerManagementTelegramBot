package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#getting-updates">CallbackQuery</a>
 */
public class CallbackQuery {
    public String id;

    public User from;

    public Message message;

    public String inline_message_id;

    public String chat_instance;

    public String data;

    public String game_short_name;
}
