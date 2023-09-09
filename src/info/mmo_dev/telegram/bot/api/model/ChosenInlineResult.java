package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#choseninlineresult">ChosenInlineResult</a>
 */
public class ChosenInlineResult {
    public String result_id;

    public User from;

    public Location location;

    public String inline_message_id;

    public String query;
}
