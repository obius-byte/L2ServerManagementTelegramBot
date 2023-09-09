package info.mmo_dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import info.mmo_dev.emulators.*;
import info.mmo_dev.telegram.bot.api.*;
import info.mmo_dev.telegram.bot.api.model.*;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DelayedTasksManager {

    private static boolean DEBUG;

    public final Gson _json;

    private EmulatorAdapter _emulator;

    private RequestApi _api;

    private final Map<String, List<String>> _tables = new HashMap<>();

    private static final Map<String, String> _actualMenu = new HashMap<String, String>() {{
        put("add_item", "Выдача предметов");
        put("online", "Вывод текущего онлайна персонажей");
        //put("statistics", "");
        put("items_delayed_status", "Статус выдачи предметов");
        put("shutdown", "Выключение сервера");
        put("restart", "Рестарт сервера");
        put("shutdown_abort", "Отмена действий /shutdown и /restart");
        put("thread_pool_status", "Текущий статус пула потоков");
        //put("chars", "");
    }};

    private DelayedTasksManager() {
        _json = new GsonBuilder().setPrettyPrinting().create();

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

        _api = new RequestApi(Config.BOT_TOKEN);

        ResponseApi<WebhookInfo> webhookInfo = _api.getWebhookInfo();
        if (!webhookInfo.ok) {
            System.out.println("DelayedTasksManager: " + webhookInfo.description);
            return;
        }

        if (webhookInfo.result.url != null && webhookInfo.result.url.length() > 0) {
            System.out.println("DelayedTasksManager: " + _api.setWebhook().description);
        }

        ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1);
        threadPool.scheduleWithFixedDelay(() -> {
            List<String> allowed_updates = new ArrayList<String>() {{
                    add("message");
                    //add("callback_query");
                }};
            ResponseApi<Update[]> response = _api.getUpdates(10, 0, allowed_updates);

            if (!response.ok) {
                System.out.println("DelayedTasksManager: " + response.description);
                return;
            }

            Update[] updates = response.result;
            if (updates.length > 0) {
                for (Update update : updates) {
                    handleUpdate(update);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void handleUpdate(Update update) {
        long userId;
        int messageId;
        String messageText;
        if (update.message != null) {
            userId = update.message.from.id;
            messageId = update.message.message_id;

            messageText = "";
            if (update.message.reply_to_message != null) {
                messageText = update.message.reply_to_message.text + " ";
            }

            messageText += update.message.text;
        } /*else if (update.callback_query != null) {
            userId = update.callback_query.from.id;
            messageId = update.callback_query.message.message_id;
            messageText = update.callback_query.data;
        }*/ else {
            System.out.println("userId not found! update: " + _json.toJson(update));
            return;
        }

        if (!Config.USER_IDS.contains(userId)) {
            _api.sendMessage(
                    userId,
                    "Ты кто такой? Давай, до свидания!",
                    0,
                    "html",
                    null,
                    true,
                    false,
                    false,
                    messageId, // reply_to_message_id
                    true,
                    null
            );
            return;
        }

        if (messageText.length() > 0) {
            try {
                String[] command = messageText.split("\\s+");

                if (command[0].equals("/start")) {
                    BotCommandScopeChat botCommandScopeChat = new BotCommandScopeChat(userId);
                    ResponseApi<BotCommand[]> response = _api.getMyCommands(botCommandScopeChat, "ru");
                    List<String> currentBotCommands = Stream.of(response.result)
                            .flatMap(c -> Stream.of(c.command))
                            .collect(Collectors.toList());

                    if (_actualMenu.size() == response.result.length) {
                        for (String cmd: _actualMenu.keySet()) {
                            if (!currentBotCommands.contains(cmd)) {
                                _api.deleteMyCommands(botCommandScopeChat, "ru");

                                List<BotCommand> botCommandList = new ArrayList<>();
                                _actualMenu.forEach((k, v) -> botCommandList.add(new BotCommand(k, v)));

                                _api.setMyCommands(botCommandList, botCommandScopeChat, "ru");
                                break;
                            }
                        }
                        /*for (BotCommand cmd : response.result) {
                            if (!_actualMenu.containsKey(cmd.command)) {

                            }
                        }*/
                   } else {
                        _api.deleteMyCommands(botCommandScopeChat, "ru");

                        List<BotCommand> botCommandList = new ArrayList<>();
                        _actualMenu.forEach((k, v) -> botCommandList.add(new BotCommand(k, v)));

                        _api.setMyCommands(botCommandList, botCommandScopeChat, "ru");
                    }
                } else if (command[0].equals("/add_item")) {
                    if (command.length == 4) {
                        _api.sendMessage(userId, addItem(command[1], Integer.parseInt(command[2]), Integer.parseInt(command[3])));
                    } else {
                        _api.sendMessage(
                                userId,
                                "/add_item",
                                0,
                                "html",
                                null,
                                true,
                                false,
                                false,
                                0,
                                true,
                                new ForceReply(true, "nickName itemId itemCount", false)
                        );
                    }
                } else if (command[0].equals("/online")) {
                    _api.sendMessage(userId, getOnline());
                } else if (command[0].equals("/items_delayed_status")) {
                    _api.sendMessage(userId, "<pre>" + getItemsDelayedStatus() + "</pre>");
                } else if (command[0].equals("/restart") || command[0].equals("/shutdown")) {
                    if (command.length == 2) {
                        String text = executeShutdownSchedule(Integer.parseInt(command[1]), command[0].equals("/restart"));
                        _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                    } else {
                        _api.sendMessage(
                                userId,
                                command[0],
                                0,
                                "html",
                                null,
                                true,
                                false,
                                false,
                                0,
                                true,
                                new ForceReply(true, "seconds", false)
                        );
                    }
                } else if (command[0].equals("/shutdown_abort")) {
                    String text = executeShutdownAbort();
                    _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                } else if (command[0].equals("/thread_pool_status")) {
                    _api.sendMessage(userId, "<pre>" + getThreadPoolStatus() + "</pre>");
                } else if (command[0].equals("/chars")) {
                    // TODO: command[1] - offset
                    _api.sendMessage(userId, "<pre>" + getChars() + "</pre>");
                } else {
                    if (DEBUG) {
                        _api.sendMessage(userId, "<pre>update:\n" + _json.toJson(update) + "</pre>");
                    } else
                        _api.sendMessage(userId, "<pre>Unregistered command: [" + messageText + "]</pre>");
                }
            } catch (Exception e) {
                _api.sendMessage(userId, "<pre>" + e.getMessage() + "</pre>");
                e.printStackTrace();
                if (DEBUG) {
                    _api.sendMessage(userId, "<pre>update:\n" + _json.toJson(update) + "</pre>");
                }
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
        } else if (_tables.containsKey("character_items")) {
            sqlInsert = "INSERT INTO `character_items` ( `owner_id`, `item_id`, `count`, `status` ) VALUES ( ?, ?, ?, 0 )";
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
        String tableName;
        if (_tables.containsKey("items_delayed")) {
            tableName = "items_delayed";
            sqlSelect = "SELECT * FROM items_delayed ORDER BY payment_id DESC";
        } else if (_tables.containsKey("z_queued_items")) {
            tableName = "z_queued_items";
            sqlSelect = "SELECT item_id, item_count AS count, name AS description, status AS payment_status FROM z_queued_items ORDER BY id DESC";
        } else if (_tables.containsKey("character_items")) {
            tableName = "character_items";
            sqlSelect = "SELECT item_id, count, status AS payment_status FROM character_items ORDER BY id DESC";
        } else {
            return "Table `items_delayed` and `z_queued_items` not found!";
        }

        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(sqlSelect);
             ResultSet resultSet = select.executeQuery()) {
            text = Utils.column("Item Id", true, 10)
                    + Utils.column("Count", false, 10)
                    + Utils.column("Description", false, 20)
                    + Utils.column("Status", false, 8) + "\n";
            while (resultSet.next()) {
                text += Utils.column(String.valueOf(resultSet.getInt("item_id")), true, 10);
                text += Utils.column(String.valueOf(resultSet.getInt("count")), false, 10);

                String description = "";
                if (!tableName.equals("character_items"))
                    description = resultSet.getString("description");

                text += Utils.column(description, false, 20);
                text += Utils.column(resultSet.getBoolean("payment_status") ? "ok" : "pending", false, 8) + "\n";
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
            text = Utils.column( "Item Id", true, 10 )
                    + Utils.column("Char name", false, 10)
                    + Utils.column("Description", false, 40)
                    + Utils.column("Status", false, 10) + "\n";
            while (resultSet.next()) {
                text += Utils.column(String.valueOf(resultSet.getInt("char_name")), true, 10);
                text += Utils.column(String.valueOf(resultSet.getInt("count")), false, 10);
                text += Utils.column(resultSet.getString("description"), false, 40);
                text += Utils.column(resultSet.getBoolean("payment_status") ? "ok" : "pending", false, 10) + "\n";
            }
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    private String executeShutdownSchedule(int seconds, boolean isRestart) {
        if (_emulator == null) {
            return "_emulator == null";
        }

        Object shutdownObj = _emulator.getShutdownObject();
        if (shutdownObj == null) {
            return "shutdownObject == null [" + _emulator.getType() + "]";
        }

        String text = null;
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

        return text;
    }

    private String executeShutdownAbort() {
        if (_emulator == null) {
            return "_emulator == null";
        }

        Object shutdownObj = _emulator.getShutdownObject();
        if (shutdownObj == null) {
            return "shutdownObject == null [" + _emulator.getType() + "]";
        }

        String text = null;
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

        return text;
    }

    private String getThreadPoolStatus() {
        if (_emulator == null) {
            return "_emulator == null";
        }

        Object threadPoolObj = _emulator.getThreadPoolObject();
        if (threadPoolObj == null) {
            return "threadPoolObject == null [" + _emulator.getType() + "]";
        }

        String text = "";
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

        return text;
    }

    public static void main(String... args) {
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

            try {
                Method main = clazz.getDeclaredMethod("main", String[].class);
                args = Arrays.copyOfRange(args, 1, args.length);
                main.invoke(clazz, new Object[]{args});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        new DelayedTasksManager();
    }
}
