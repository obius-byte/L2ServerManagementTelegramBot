package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#sticker">Sticker</a>
 */
public class Sticker {
    public String file_id;

    public String file_unique_id;

    public String type;

    public int width;

    public int height;

    public boolean is_animated;

    public boolean is_video;

    public PhotoSize thumbnail;

    public String emoji;

    public String set_name;

    // TODO: not impl
    //public File premium_animation;

    public MaskPosition mask_position;

    public String custom_emoji_id;

    public boolean needs_repainting;

    public int file_size;
}
