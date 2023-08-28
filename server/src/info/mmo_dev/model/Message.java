package info.mmo_dev.model;

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

    // TODO: to be continued...

    public String text;
}
