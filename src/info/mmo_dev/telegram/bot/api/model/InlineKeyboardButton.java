package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#inlinekeyboardbutton">InlineKeyboardButton</a>
 */
public class InlineKeyboardButton {
    public String text;

    public String url;

    public String callback_data;

    public WebAppInfo web_app;

    public LoginUrl login_url;

    public String switch_inline_query;

    public String switch_inline_query_current_chat;

    public SwitchInlineQueryChosenChat switch_inline_query_chosen_chat;

    public CallbackGame callback_game;

    public boolean pay;

    public InlineKeyboardButton(String text, String callback_data) {
        this.text = text;
        this.callback_data = callback_data;
    }
}
