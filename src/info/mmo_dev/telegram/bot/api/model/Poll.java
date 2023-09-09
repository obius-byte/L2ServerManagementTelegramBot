package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#poll">Poll</a>
 */
public class Poll {
    public String id;
    public String question;
    public List<PollOption> options;
    public int total_voter_count;
    public boolean is_closed;
    public boolean is_anonymous;
    public String type;
    public boolean allows_multiple_answers;
    public int correct_option_id;
    public String explanation;
    public List<MessageEntity> explanation_entities;
    public int open_period;
    public int close_date;
}
