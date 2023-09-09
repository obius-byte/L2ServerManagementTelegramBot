package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#game">Game</a>
 */
public class Game {
    public String title;

    public String description;

    public List<PhotoSize> photo;

    public String text;

    public List<MessageEntity> text_entities;

    public Animation animation;
}
