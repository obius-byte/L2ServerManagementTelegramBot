package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#location">Location</a>
 */
public class Location {
    public float longitude;

    public float latitude;

    public float horizontal_accuracy;

    public int live_period;

    public int heading;

    public int proximity_alert_radius;
}
