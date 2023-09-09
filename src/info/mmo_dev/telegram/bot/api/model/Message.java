package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#message">Message</a>
 */
public class Message {
    public int message_id;

    public int message_thread_id;

    public User from;

    public Chat sender_chat;

    public int date;

    public Chat chat;

    public User forward_from;

    public Chat forward_from_chat;

    public int forward_from_message_id;

    public String forward_signature;

    public String forward_sender_name;

    public int forward_date;

    public boolean is_topic_message;

    public boolean is_automatic_forward;

    public Message reply_to_message;

    public User via_bot;

    public int edit_date;

    public boolean has_protected_content;

    public String media_group_id;

    public String author_signature;

    public String text;

    public List<MessageEntity> entities;

    public Animation animation;

    public Audio audio;

    public Document document;

    public List<PhotoSize> photo;

    public Sticker sticker;

    public Story story;

    public Video video;

    public VideoNote video_note;

    public Voice voice;

    public String caption;

    public List<MessageEntity> caption_entities;

    public boolean has_media_spoiler;

    public Contact contact;

    public Dice dice;

    public Game game;

    public Poll poll;

    public Venue venue;

    public Location location;

    public List<User> new_chat_members;

    public User left_chat_member;

    public String new_chat_title;

    public List<PhotoSize> new_chat_photo;

    public boolean delete_chat_photo;

    public boolean group_chat_created;

    public boolean supergroup_chat_created;

    public boolean channel_chat_created;

    public MessageAutoDeleteTimerChanged message_auto_delete_timer_changed;

    public int migrate_to_chat_id;

    public int migrate_from_chat_id;

    public Message pinned_message;

    public Invoice invoice;

    public SuccessfulPayment successful_payment;

    public UserShared user_shared;

    public ChatShared chat_shared;

    public String connected_website;

    public WriteAccessAllowed write_access_allowed;

    public PassportData passport_data;

    public ProximityAlertTriggered proximity_alert_triggered;

    public ForumTopicCreated forum_topic_created;

    public ForumTopicEdited forum_topic_edited;

    public ForumTopicClosed forum_topic_closed;

    public ForumTopicReopened forum_topic_reopened;

    public GeneralForumTopicHidden general_forum_topic_hidden;

    public GeneralForumTopicUnhidden general_forum_topic_unhidden;

    public VideoChatScheduled video_chat_scheduled;

    public VideoChatStarted video_chat_started;

    public VideoChatEnded video_chat_ended;

    public VideoChatParticipantsInvited video_chat_participants_invited;

    public WebAppData web_app_data;

    public InlineKeyboardMarkup reply_markup;
}
