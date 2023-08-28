package info.mmo_dev.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#user">User</a>
 */
public class User {
    public int id;

    public boolean is_bot;

    public String first_name;

    public String last_name;

    public String username;

    public String language_code;

    // TODO: int?
    public boolean is_premium;

    // TODO: int?
    public boolean added_to_attachment_menu;

    public boolean can_join_groups;

    public boolean can_read_all_group_messages;

    public boolean supports_inline_queries;
}
