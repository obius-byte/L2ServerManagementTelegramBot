package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#pollanswer">PollAnswer</a>
 */
public class PollAnswer {
    public String poll_id;

    public Chat voter_chat;

    public User user;

    public List<Integer> option_ids;
}
