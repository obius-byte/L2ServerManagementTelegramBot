package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#chatjoinrequest">ChatJoinRequest</a>
 */
public class ChatJoinRequest {
    public Chat chat;

    public User from;

    public int user_chat_id;

    public int date;

    public String bio;

    public ChatInviteLink invite_link;
}
