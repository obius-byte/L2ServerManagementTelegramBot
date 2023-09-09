package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#switchinlinequerychosenchat">SwitchInlineQueryChosenChat</a>
 */
public class SwitchInlineQueryChosenChat {
    public String query;

    public boolean allow_user_chats;

    public boolean allow_bot_chats;

    public boolean allow_group_chats;

    public boolean allow_channel_chats;
}
