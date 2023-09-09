package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#keyboardbuttonrequestchat">KeyboardButtonRequestChat</a>
 */
public class KeyboardButtonRequestChat {
    public int request_id;

    public boolean chat_is_channel;

    public boolean chat_is_forum;

    public boolean chat_has_username;

    public boolean chat_is_created;

    public ChatAdministratorRights user_administrator_rights;

    public ChatAdministratorRights bot_administrator_rights;

    public boolean bot_is_member;
}
