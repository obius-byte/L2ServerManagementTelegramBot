package info.mmo_dev;

public class SqlQueries {

    public static final String SELECT_ONLINE_COUNT_CHARACTERS = "SELECT COUNT(*) FROM characters WHERE online = 1";

    public static final String SELECT_OBJ_ID_CHARACTERS = "SELECT obj_Id FROM characters WHERE char_name = ?";

    public static final String INSERT_ITEMS_DELAYED = "INSERT INTO `items_delayed` ( `owner_id`, `item_id`, `count`, `payment_status`, `description` ) VALUES ( ?, ?, ?, 0, 'Telegram Bot' )";

    public static final String SELECT_ITEMS_DELAYED = "SELECT * FROM items_delayed ORDER BY payment_id DESC";
}
