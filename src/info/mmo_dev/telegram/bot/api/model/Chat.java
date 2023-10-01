package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#chat">Chat</a>
 */
public class Chat {
    public long id;

    public String type;

    public String title;

    public String username;

    public String first_name;

    public String last_name;

    public boolean is_forum;

    public ChatPhoto photo;

    public List<String> active_usernames;

    public String emoji_status_custom_emoji_id;

    public int emoji_status_expiration_date;

    public String bio;

    public boolean has_private_forwards;

    public boolean has_restricted_voice_and_video_messages;

    public boolean join_to_send_messages;

    public boolean join_by_request;

    public String description;

    public String invite_link;

    public Message pinned_message;

    public ChatPermissions permissions;

    public int slow_mode_delay;

    public int message_auto_delete_time;

    public boolean has_aggressive_anti_spam_enabled;

    public boolean has_hidden_members;

    public boolean has_protected_content;

    public String sticker_set_name;

    public boolean can_set_sticker_set;

    public int linked_chat_id;

    public ChatLocation location;
}
