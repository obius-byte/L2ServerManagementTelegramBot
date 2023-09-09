package info.mmo_dev.telegram.bot.api.model;

/**
 * @see <a href="https://core.telegram.org/bots/api#shippingquery">ShippingQuery</a>
 */
public class ShippingQuery {
    public String id;

    public User from;

    public String invoice_payload;

    public ShippingAddress shipping_address;
}
