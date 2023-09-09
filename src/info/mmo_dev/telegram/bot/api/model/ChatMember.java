package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#chatmember">ChatMember</a>
 */
public class ChatMember {

    // ChatMemberOwner, ChatMemberAdministrator, ChatMemberMember, ChatMemberRestricted, ChatMemberLeft, ChatMemberBanned
    public String status;

    // ChatMemberOwner, ChatMemberAdministrator, ChatMemberMember, ChatMemberRestricted, ChatMemberLeft, ChatMemberBanned
    public User user;

    // ChatMemberOwner, ChatMemberAdministrator
    public boolean is_anonymous;

    // ChatMemberOwner, ChatMemberAdministrator
    public String custom_title;

    // ChatMemberAdministrator
    public boolean can_be_edited;

    // ChatMemberAdministrator
    public boolean can_manage_chat;

    // ChatMemberAdministrator
    public boolean can_delete_messages;

    // ChatMemberAdministrator
    public boolean can_manage_video_chats;

    // ChatMemberAdministrator
    public boolean can_restrict_members;

    // ChatMemberAdministrator
    public boolean can_promote_members;

    // ChatMemberAdministrator, ChatMemberRestricted
    public boolean can_change_info;

    // ChatMemberAdministrator, ChatMemberRestricted
    public boolean can_invite_users;

    // ChatMemberAdministrator
    public boolean can_post_messages;

    // ChatMemberAdministrator
    public boolean can_edit_messages;

    // ChatMemberAdministrator, ChatMemberRestricted
    public boolean can_pin_messages;

    // ChatMemberAdministrator, ChatMemberRestricted
    public boolean can_manage_topics;

    // ChatMemberRestricted
    public boolean is_member;

    // ChatMemberRestricted
    public boolean can_send_messages;

    // ChatMemberRestricted
    public boolean can_send_audios;

    // ChatMemberRestricted
    public boolean can_send_documents;

    // ChatMemberRestricted
    public boolean can_send_photos;

    // ChatMemberRestricted
    public boolean can_send_videos;

    // ChatMemberRestricted
    public boolean can_send_video_notes;

    // ChatMemberRestricted
    public boolean can_send_voice_notes;

    // ChatMemberRestricted
    public boolean can_send_polls;

    // ChatMemberRestricted
    public boolean can_send_other_messages;

    // ChatMemberRestricted
    public boolean can_add_web_page_previews;

    // ChatMemberRestricted, ChatMemberBanned
    public int until_date;
}
