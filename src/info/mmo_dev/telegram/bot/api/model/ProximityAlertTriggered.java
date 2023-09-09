package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#proximityalerttriggered">ProximityAlertTriggered</a>
 */
public class ProximityAlertTriggered {
    public User traveler;

    public User watcher;

    public int distance;
}
