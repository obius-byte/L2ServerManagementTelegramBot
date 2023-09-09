package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#replykeyboardmarkup">ReplyKeyboardMarkup</a>
 */
public class ReplyKeyboardMarkup {
    public List<List<KeyboardButton>> keyboard;

    public boolean is_persistent;

    public boolean resize_keyboard;

    public boolean one_time_keyboard;

    public String input_field_placeholder;

    public boolean selective;
}
