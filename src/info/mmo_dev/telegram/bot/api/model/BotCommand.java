package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#botcommand">BotCommand</a>
 */
public class BotCommand {
    public String command;

    public String description;

    public BotCommand(String command, String description) {
        this.command = command;
        this.description = description;
    }
}
