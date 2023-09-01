package info.mmo_dev.telegram.bot.api.model;

import java.util.List;

/**
 * @see <a href="https://core.telegram.org/bots/api#webhookinfo">WebhookInfo</a>
 */
public class WebhookInfo {
    public String url;

    public boolean has_custom_certificate;

    public int pending_update_count;

    public String ip_address;

    public int last_error_date;

    public String last_error_message;

    public int last_synchronization_error_date;

    public int max_connections;

    public List<String> allowed_updates;
}
