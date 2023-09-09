package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#forcereply">ForceReply</a>
 */
public class ForceReply {
    public boolean force_reply;

    public String input_field_placeholder;

    public boolean selective;

    public ForceReply(boolean forceReply, String inputFieldPlaceholder, boolean selective) {
        force_reply = forceReply;
        input_field_placeholder = inputFieldPlaceholder;
        this.selective = selective;
    }
}
