package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#keyboardbutton">KeyboardButton</a>
 */
public class KeyboardButton {
    public String text;

    public KeyboardButtonRequestUser request_user;

    public KeyboardButtonRequestChat request_chat;

    public boolean request_contact;

    public boolean request_location;

    public KeyboardButtonPollType request_poll;

    public WebAppInfo web_app;

    public KeyboardButton(String text) {
        this.text = text;
    }
}
