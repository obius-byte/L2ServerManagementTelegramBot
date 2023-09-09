package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#chatinvitelink">ChatInviteLink</a>
 */
public class ChatInviteLink {
    public String invite_link;

    public User creator;

    public boolean creates_join_request;

    public boolean is_primary;

    public boolean is_revoked;

    public String name;

    public int expire_date;

    public int member_limit;

    public int pending_join_request_count;
}
