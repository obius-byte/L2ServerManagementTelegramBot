package info.mmo_dev;

import info.mmo_dev.emulators.*;
import info.mmo_dev.telegram.bot.api.RequestApi;
import info.mmo_dev.telegram.bot.api.ResponseApi;
import info.mmo_dev.telegram.bot.api.model.Update;
import info.mmo_dev.telegram.bot.api.model.WebhookInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DelayedTasksManager {

    private static boolean DEBUG;

    private EmulatorAdapter _emulator;

    private RequestApi _telegramApi;

    private final Map<String, List<String>> _tables = new HashMap<>();

    private DelayedTasksManager() {
        try {
            Config.initialize();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement statement1 = connection.prepareStatement("SHOW TABLES");
             ResultSet resultSet1 = statement1.executeQuery()) {
            while (resultSet1.next()) {
                String tableName = resultSet1.getString(1);
                _tables.put(tableName, new ArrayList<>());
                try (PreparedStatement statement2 = connection.prepareStatement("SHOW COLUMNS FROM " + tableName);
                     ResultSet resultSet2 = statement2.executeQuery()) {
                    while (resultSet2.next()) {
                        String columnName = resultSet2.getString(1);
                        _tables.get(tableName).add(columnName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        EmulatorAdapter[] emulatorList = new EmulatorAdapter[] {
                new PwSoftEmulator(),
                new RebellionEmulator(),
                new LuceraEmulator(),
                new L2ScriptsEmulator()
        };

        for (EmulatorAdapter emulator: emulatorList) {
            if (Package.getPackage(emulator.getBasePackage()) != null) {
                _emulator = emulator;
                break;
            }
        }

        System.out.println("DelayedTasksManager: Emulator " + (_emulator != null ? "detected[" + _emulator.getType() + "]" : "not detected!"));

        _telegramApi = new RequestApi(Config.BOT_TOKEN);

        ResponseApi<WebhookInfo> webhookInfo = _telegramApi.getWebhookInfo();
        if (!webhookInfo.ok) {
            System.out.println("DelayedTasksManager: " + webhookInfo.description);
            return;
        }

        if (webhookInfo.result.url != null && webhookInfo.result.url.length() > 0) {
            System.out.println("DelayedTasksManager: " + _telegramApi.setWebhook().description);
        }

        ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1);
        threadPool.scheduleWithFixedDelay(() -> {
            try {
                ResponseApi<Update[]> response = _telegramApi.getUpdates(10, 0, new ArrayList<String>() {{
                    add("message");
                }});
                Update[] updates = response.result;
                if (updates.length > 0) {
                    for (Update update : updates) {
                        long userId = update.message.from.id;
                        if (Config.USER_IDS.contains(userId)) {
                            handleUpdate(update);
                        } else {
                            _telegramApi.sendMessage(
                                    userId,
                                    "Ты кто такой? Давай, до свидания!",
                                    0,
                                    "html",
                                    null,
                                    true,
                                    false,
                                    false,
                                    update.message.message_id, // reply_to_message_id
                                    true,
                                    null
                            );
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void handleUpdate(Update update) {
        long userId = update.message.from.id;
        String messageText = update.message.text;
        if (messageText != null && messageText.length() > 0) {
            String[] command = messageText.split("\\s+");

            List<String> menuCommands = Arrays.asList("/start", "/menu", "/help");

            if (menuCommands.contains(command[0])) {
                String text = "<b>Сommand list</b>";
                text += "\n\n/add_item {char name} {item id} {item count}";
                text += "\n/online";
                //text += "\n/statistics";
                text += "\n/items_delayed_status";
                text += "\n/shutdown {seconds}";
                text += "\n/restart {seconds}";
                text += "\n/shutdown_abort";
                text += "\n/thread_pool_status";
                text += "\n/chars";

                _telegramApi.sendMessage(userId, text);
            } else if (command[0].equals("/add_item")) {
                if (command.length == 4) {
                    _telegramApi.sendMessage(userId, addItem(command[1], Integer.parseInt(command[2]), Integer.parseInt(command[3])));
                }
            } else if (command[0].equals("/online")) {
                _telegramApi.sendMessage(userId, getOnline());
            } else if (command[0].equals("/items_delayed_status")) {
                _telegramApi.sendMessage(userId, "<pre>" + getItemsDelayedStatus() + "</pre>");
            } else if (command[0].equals("/restart") || command[0].equals("/shutdown")) {
                if (command.length == 2) {
                    String text = executeShutdownSchedule(Integer.parseInt(command[1]), command[0].equals("/restart"));
                    _telegramApi.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                } else {
                    // TODO: throw new ArrayIndexOutOfBoundsException???
                }
            } else if (command[0].equals("/shutdown_abort")) {
                String text = executeShutdownAbort();
                _telegramApi.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
            } else if (command[0].equals("/thread_pool_status")) {
                _telegramApi.sendMessage(userId, "<pre>" + getThreadPoolStatus() + "</pre>");
            } else if (command[0].equals("/chars")) {
                // TODO: command[1] - offset
                _telegramApi.sendMessage(userId, "<pre>" + getChars() + "</pre>");
            } else {
                _telegramApi.sendMessage(userId, "<pre>Unregistered command: [" + messageText + "]</pre>");
            }
        }
    }

    private String addItem(String charName, int itemId, int itemCount) {
        String text = "Successfully!";
        String sqlSelect = "SELECT * FROM characters WHERE char_name = ?";

        String sqlInsert;
        if (_tables.containsKey("items_delayed")) {
            sqlInsert = "INSERT INTO `items_delayed` ( `owner_id`, `item_id`, `count`, `payment_status`, `description` ) VALUES ( ?, ?, ?, 0, 'Telegram Bot' )";
        } else if (_tables.containsKey("z_queued_items")) {
            sqlInsert = "INSERT INTO `z_queued_items` ( `char_id`, `name`, `item_id`, `item_count`, `status` ) VALUES ( ?, 'Telegram Bot', ?, ?, 0 )";
        } else {
            return "Table `items_delayed` and `z_queued_items` not found!";
        }

        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(sqlSelect)) {
            select.setString(1, charName);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    int charId = resultSet.getInt(_tables.get("characters").contains("obj_Id") ? "obj_Id" : "charId");

                    try (PreparedStatement insert = connection.prepareStatement(sqlInsert)) {
                        insert.setInt(1, charId);
                        insert.setInt(2, itemId);
                        insert.setInt(3, itemCount);
                        insert.execute();
                    }
                } else {
                    text = "Character " + charName + " not found!";
                }
            }
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    private String getItemsDelayedStatus() {
        String text = "";
        String sqlSelect;
        if (_tables.containsKey("items_delayed")) {
                sqlSelect = "SELECT * FROM items_delayed ORDER BY payment_id DESC";
        } else if (_tables.containsKey("z_queued_items")) {
            sqlSelect = "SELECT item_id, item_count AS count, name AS description, status AS payment_status FROM z_queued_items ORDER BY id DESC";
        } else {
            return "Table `items_delayed` and `z_queued_items` not found!";
        }

        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(sqlSelect);
             ResultSet resultSet = select.executeQuery()) {
            text = "|" + /*column( 'Char name', true ) .*/ Utils.column( "Item Id" )
                    + Utils.column("Count")
                    + Utils.column("Description", false, 40)
                    + Utils.column("Status") + "\n";
            while (resultSet.next()) {
                text += "|";
                //$text .= column( $entity['char_name'], true );
                text += Utils.column(String.valueOf(resultSet.getInt("item_id")));
                text += Utils.column(String.valueOf(resultSet.getInt("count")));
                text += Utils.column(resultSet.getString("description"), false, 40);
                text += Utils.column(resultSet.getBoolean("payment_status") ? "ok" : "pending") + "\n";
            }
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    private String getOnline() {
        String text = "";
        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement("SELECT COUNT(*) FROM characters WHERE online = 1");
             ResultSet resultSet = select.executeQuery()) {
            if (resultSet.next()) {
                text = "Current online: <b>" + resultSet.getInt(1) + "</b>";
            }
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    // TODO: not impl (limit, offset - pagination)
    private String getChars() {
        String text = "";
        String sql = "SELECT * FROM characters";
        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(sql);
             ResultSet resultSet = select.executeQuery()) {
            text = "|" + /*column( 'Char name', true ) .*/ Utils.column( "Item Id" )
                    + Utils.column("Char name")
                    + Utils.column("Description", false, 40)
                    + Utils.column("Status") + "\n";
            while (resultSet.next()) {
                text += "|";
                //$text .= column( $entity['char_name'], true );
                text += Utils.column(String.valueOf(resultSet.getInt("char_name")));
                text += Utils.column(String.valueOf(resultSet.getInt("count")));
                text += Utils.column(resultSet.getString("description"), false, 40);
                text += Utils.column(resultSet.getBoolean("payment_status") ? "ok" : "pending") + "\n";
            }
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    private String executeShutdownSchedule(int seconds, boolean isRestart) {
        String text = null;
        Object shutdownObj = _emulator.getShutdownObject();
        if (shutdownObj != null) {
            try {
                if (_emulator.getType() == EmulatorType.Rebellion) {
                    Method method = shutdownObj.getClass().getDeclaredMethod("schedule", int.class, int.class);
                    method.invoke(shutdownObj, seconds, isRestart ? 2 : 0);
                } /*else if (_emulator == Emulator.MobiusDev) {
                    //Class<?> player = Class.forName("org.l2jmobius.gameserver.model.actor.Player");

                    //Method method = player.getDeclaredMethod("startShutdown", player, int.class, boolean.class);
                    for ( Method method1: _shutdownInstance.getClass().getDeclaredMethods() ) {
                        System.out.println("method: " + method1.getName());
                        for ( Class<?> type: method1.getParameterTypes())
                            System.out.println("\ttype: " + type.getName());
                    }

                    //Method method = _shutdownInstance.getClass().getDeclaredMethod("startShutdown", player, int.class, boolean.class);
                    //method.invoke(_shutdownInstance, player.newInstance(), seconds, isRestart);
                } else if (_emulator == Emulator.L2Scripts) {

                }*/ else if (_emulator.getType() == EmulatorType.PwSoft) {
                    Method method = shutdownObj.getClass().getDeclaredMethod("startTelnetShutdown", String.class, int.class, boolean.class);
                    method.invoke(shutdownObj, "127.0.0.1", seconds, isRestart);
                } else if (_emulator.getType() == EmulatorType.Lucera) {
                    Class<?> shutdownModeType = Class.forName(shutdownObj.getClass().getName() + "$ShutdownModeType");
                    Object[] enums = shutdownModeType.getEnumConstants(); // TODO: 0 - SIGTERM, 1 - SHUTDOWN, 2 - RESTART, 3 - ABORT
                    Method method = shutdownObj.getClass().getDeclaredMethod("startShutdown", String.class, int.class, shutdownModeType);
                    method.invoke(shutdownObj, "Telegram bot", seconds, isRestart ? enums[2] : enums[1]);
                }
            } catch (Exception e) {
                text = e.getMessage();
            }
        } else {
            text = "shutdownObject == null [" + _emulator.getType() + "]";
        }
        return text;
    }

    private String executeShutdownAbort() {
        String text = null;
        Object shutdownObj = _emulator.getShutdownObject();
        if (shutdownObj != null) {
            try {
                if (_emulator.getType() == EmulatorType.Rebellion) {
                    Method method = shutdownObj.getClass().getMethod("cancel");
                    method.invoke(shutdownObj);
                } /*else if (_emulator == Emulator.MobiusDev) {
                    Class<?> player = _emulator == Emulator.MobiusDev
                            ? Class.forName("org.l2jmobius.gameserver.model.actor.Player")
                            : Class.forName("net.sf.l2j.gameserver.model.actor.instance.L2PcInstance");

                    Method method = _shutdownInstance.getClass().getMethod("abort", player);
                    method.invoke(_shutdownInstance, player);
                } else if (_emulator == Emulator.L2Scripts) {
                    //
                }*/ else if (_emulator.getType() == EmulatorType.PwSoft) {
                    Method method = shutdownObj.getClass().getMethod("telnetAbort", String.class);
                    method.invoke(shutdownObj, "127.0.0.1");
                } else if (_emulator.getType() == EmulatorType.Lucera) {
                    Method method = shutdownObj.getClass().getMethod("abort");
                    method.invoke(shutdownObj);
                }
            } catch (Exception e) {
                text = e.getMessage();
            }
        } else {
            text = "shutdownObject == null [" + _emulator.getType() + "]";
        }
        return text;
    }

    private String getThreadPoolStatus() {
        String text = "threadPoolObject == null [" + _emulator.getType() + "]";
        Object threadPoolObj = _emulator.getThreadPoolObject();
        if (threadPoolObj != null) {
            try {
                if (_emulator.getType() == EmulatorType.Rebellion || _emulator.getType() == EmulatorType.Lucera) {
                    Method method = threadPoolObj.getClass().getMethod("getStats");
                    text = ((StringBuilder) method.invoke(threadPoolObj)).toString();
                } else if (_emulator.getType() == EmulatorType.PwSoft /*|| _emulator.getType() == EmulatorType.Lucera*/) {
                    Method method = threadPoolObj.getClass().getMethod("getTelemetry");
                    text = (String) method.invoke(threadPoolObj);
                }
            } catch (Exception e) {
                text = e.getMessage();
            }
        }
        return text;
    }

    public static void main(String... args)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (args.length == 0) {
            System.out.println("DelayedTasksManager: Main class not specified!");
            return;
        }

        DEBUG = Arrays.asList(args).contains("-debug");

        if (!DEBUG) {
            Class<?> clazz = null;

            try {
                clazz = Class.forName(args[0]);
            } catch (ClassNotFoundException e) {
                // ignore
            }

            if (clazz == null) {
                System.out.println("DelayedTasksManager: Main class not found : " + args[0] + "!");
                return;
            }

            Method main = clazz.getDeclaredMethod("main", String[].class);
            args = Arrays.copyOfRange(args, 1, args.length);
            main.invoke(clazz, new Object[]{args});
        }

        new DelayedTasksManager();
    }
}
