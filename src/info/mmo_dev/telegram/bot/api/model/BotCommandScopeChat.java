package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#botcommandscopechat">BotCommandScopeChat</a>
 */
public class BotCommandScopeChat implements BotCommandScope {
    public String type = "chat";

    public String chat_id;

    public BotCommandScopeChat(long chatId) {
        chat_id = Long.toString(chatId);
    }

    public BotCommandScopeChat(String chatId) {
        chat_id = chatId;
    }
}
