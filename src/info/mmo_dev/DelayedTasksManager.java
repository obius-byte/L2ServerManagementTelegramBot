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

    public final Gson _json = new GsonBuilder().setPrettyPrinting().create();

    private AbstractEmulator _emulator;

    private RequestApi _api;

    //private boolean isShutdown = false;

    //private boolean isFullShutdown = false;

    private static String[] _arguments;

    private volatile int _lastItemsDelayedId = 0;

    private final ScheduledThreadPoolExecutor _threadPool = new ScheduledThreadPoolExecutor(2);

    private static final Map<String, String> _actualMenu = new HashMap<String, String>() {{
        put("add_item", "Выдача предметов");
        put("online", "Вывод текущего онлайна персонажей");
        //put("statistics", "");
        put("items_delayed_status", "Статус выдачи предметов");
        put("shutdown", "Выключение сервера");
        put("restart", "Рестарт сервера");
        put("shutdown_abort", "Отмена действий /shutdown и /restart");
        put("thread_pool_status", "Текущий статус пула потоков");
        //put("characters_list", "Список персонажей");
        put("ban_account", "Блокировка аккаунта");
        put("unban_account", "Разблокировка аккаунта");
        //put("/ban_character", "Блокировка персонажа");
        //put("/unban_character", "Разблокировка персонажа");
        //put("/shutdown_mode", "Режим завершение работы");
        //put("/start_server", "Запустить сервер");
    }};

    private DelayedTasksManager() {
        /*Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("DelayedTasksManager: ShutdownHook");
            if (isShutdown) {
                while (!isFullShutdown) {

                }
            }
        }));*/

        try {
            EmulatorType emulatorType = EmulatorType.valueOf(Config.EMULATOR_TYPE);
            switch (emulatorType) {
                case L2jMobius: _emulator = new L2jMobiusEmulator(); break;
                case L2Scripts: _emulator = new L2ScriptsEmulator(); break;
                case PwSoft: _emulator = new PwSoftEmulator(); break;
                case Rebellion: _emulator = new RebellionEmulator(); break;
                case Lucera: _emulator = new LuceraEmulator(); break;
                case PainTeam: _emulator = new PainTeamEmulator(); break;
                case L2jEternity: _emulator = new L2jEternityEmulator(); break;
                case L2cccp: _emulator = new L2cccpEmulator(); break;
                case Acis: _emulator = new AcisEmulator(); break;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("DelayedTasksManager: Emulator detected[" + _emulator.getType() + "]");

        _api = new RequestApi(Config.BOT_TOKEN);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            for (long userId: Config.USER_IDS) {
                _api.sendMessage(userId, "<pre>" + Utils.getStackTrace(e) + "</pre>");
            }
        });

        ResponseApi<WebhookInfo> webhookInfo = _api.getWebhookInfo();
        if (!webhookInfo.ok) {
            System.out.println("DelayedTasksManager: " + webhookInfo.description);
            return;
        }

        if (webhookInfo.result.url != null && webhookInfo.result.url.length() > 0) {
            System.out.println("DelayedTasksManager: " + _api.setWebhook().description);
        }

        _threadPool.scheduleWithFixedDelay(() -> {
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

        if (Config.DELAYED_ITEMS_LISTENER) {
            startDelayedItemsListener();
        }
    }

    private void startDelayedItemsListener() {
        String sqlSelect;
        String sqlLastId;
        String charIdColumn = DatabaseHelper.getTable("characters").contains("charId") ? "charId" : "obj_Id";

        if (DatabaseHelper.tableExists("items_delayed")) {
            sqlLastId = "SELECT payment_id FROM items_delayed ORDER BY payment_id DESC LIMIT 1";
            sqlSelect = "SELECT i.*, c.char_name FROM items_delayed AS i LEFT JOIN characters AS c ON (i.owner_id = c." + charIdColumn + ") WHERE i.payment_id > ? ORDER BY i.payment_id DESC";
        } else if (DatabaseHelper.tableExists("z_queued_items")) {
            sqlLastId = "SELECT id FROM z_queued_items ORDER BY id DESC LIMIT 1";
            sqlSelect = "SELECT c.char_name, z.id AS payment_id, z.char_id AS owner_id, z.item_id, z.item_count AS count, z.name AS description, z.status AS payment_status FROM z_queued_items AS z LEFT JOIN characters AS c ON (z.char_id = c." + charIdColumn + ") WHERE z.id > ? ORDER BY z.id DESC";
        } else if (DatabaseHelper.tableExists("character_donate")) {
            sqlLastId = "SELECT id FROM character_donate ORDER BY id DESC LIMIT 1";
            sqlSelect = "SELECT c.char_name, cd.id AS payment_id, cd.obj_Id AS owner_id, cd.item_id, cd.count, cd.given AS payment_status FROM character_donate AS cd LEFT JOIN characters AS c ON (cd.obj_Id = c." + charIdColumn + ") WHERE cd.id > ? ORDER BY cd.id DESC";
        } else if (DatabaseHelper.tableExists("character_items")) {
            sqlLastId = "SELECT id FROM character_items ORDER BY id DESC LIMIT 1";
            sqlSelect = "SELECT c.char_name, i.id AS payment_id, i.owner_id, i.item_id, i.count, i.status AS payment_status FROM character_items AS i LEFT JOIN characters AS c ON (i.owner_id = c." + charIdColumn + ") WHERE i.id > ? ORDER BY i.id DESC";
        } else {
            System.out.println("DelayedTasksManager::startDelayedItemsListener: Table `items_delayed` and `z_queued_items` and `character_donate` and `character_items` not found!");
            return;
        }

        try (Connection connection = DatabaseHelper.getGameConnection();
             PreparedStatement select = connection.prepareStatement(sqlLastId);
             ResultSet resultSet = select.executeQuery()) {
            if (resultSet.next()) {
                _lastItemsDelayedId = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            for (long userId : Config.USER_IDS) {
                _api.sendMessage(userId, "<pre>" + Utils.getStackTrace(e) + "</pre>");
            }
        }

        _threadPool.scheduleWithFixedDelay(() -> {
            try (Connection connection = DatabaseHelper.getGameConnection();
                 PreparedStatement select = connection.prepareStatement(sqlSelect)) {
                select.setInt(1, _lastItemsDelayedId);
                try (ResultSet resultSet = select.executeQuery()) {
                    while (resultSet.next()) {
                        String charName = resultSet.getString("char_name");
                        if (charName == null) {
                            charName = String.valueOf(resultSet.getInt("owner_id"));
                        }
                        int itemId = resultSet.getInt("item_id");

                        if (Config.DELAYED_ITEMS_LISTENER_EXCLUDE_ITEM_IDS.contains(itemId))
                            continue;

                        int itemCount = resultSet.getInt("count");

                        _lastItemsDelayedId = resultSet.getInt("payment_id");

                        String text = "<b>New Delayed Items record</b>";
                        text += "\n\nCharacter: " + charName;
                        text += "\nItem ID: " + itemId;
                        text += "\nCount: " + itemCount;

                        for (long userId : Config.USER_IDS) {
                            _api.sendMessage(userId, "<pre>" + text + "</pre>");
                        }
                    }
                }
            } catch (Exception e) {
                if (Config.DEBUG)
                    e.printStackTrace();

                for (long userId : Config.USER_IDS) {
                    _api.sendMessage(userId, "<pre>" + Utils.getStackTrace(e) + "</pre>");
                }
            }
        }, 60, Config.DELAYED_ITEMS_LISTENER_DELAY, TimeUnit.SECONDS);
    }

    private void handleUpdate(Update update) {
        if (update == null)
            return;

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

                switch (command[0]) {
                    case "/start": {
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
                        } else {
                            _api.deleteMyCommands(botCommandScopeChat, "ru");

                            List<BotCommand> botCommandList = new ArrayList<>();
                            _actualMenu.forEach((k, v) -> botCommandList.add(new BotCommand(k, v)));

                            _api.setMyCommands(botCommandList, botCommandScopeChat, "ru");
                        }
                        break;
                    }
                    case "/add_item": {
                        if (command.length == 4) {
                            _api.sendMessage(userId, DatabaseHelper.addItem(command[1], Integer.parseInt(command[2]), Integer.parseInt(command[3])));
                        } else {
                            _api.sendMessage(userId,"/add_item", new ForceReply(true, "nickName itemId itemCount", false)
                            );
                        }
                        break;
                    }
                    case "/online": {
                        _api.sendMessage(userId, DatabaseHelper.getOnline());
                        break;
                    }
                    case "/items_delayed_status": {
                        _api.sendMessage(userId, "<pre>" + DatabaseHelper.getItemsDelayedStatus() + "</pre>");
                        break;
                    }
                    case "/restart": case "/shutdown": {
                        if (command.length == 2) {
                            String text = _emulator.executeShutdownSchedule(Integer.parseInt(command[1]), command[0].equals("/restart"), false);
                            _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        } else {
                            _api.sendMessage(userId, command[0], new ForceReply(true, "seconds", false));
                        }
                        break;
                    }
                    case "/shutdown_abort": {
                        String text = _emulator.executeShutdownSchedule(0, false, true);
                        _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        break;
                    }
                    case "/thread_pool_status": {
                        _api.sendMessage(userId, "<pre>" + _emulator.getThreadPoolStatus() + "</pre>");
                        break;
                    }
                    /*case "/characters_list": {
                        // TODO: command[1] - offset, command[2] - limit?
                        ResponseApi<Message> response = _api.sendMessage(userId, "<pre>" + DatabaseHelper.getCharactersList() + "</pre>");

                        if (!response.ok)
                            System.out.println("message: " + _json.toJson(response));
                        break;
                    }*/
                    case "/ban_account": case "/unban_account": {
                        if (command.length == 2) {
                            String text = _emulator.banAccount(command[1], command[0].equals("/unban_account"));
                            _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        } else {
                            _api.sendMessage(userId, command[0], new ForceReply(true, "account name", false));
                        }
                        break;
                    }
                    /*case "/ban_character": case "/unban_character": {
                        if (command.length == 2) {
                            String text = _emulator.banCharacter(command[1], command[0].equals("/unban_character"));
                            _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        } else {
                            _api.sendMessage(userId, command[0], new ForceReply(true, "character name", false));
                        }
                        break;
                    }
                    case "/shutdown_mode": {
                        isFullShutdown = !isFullShutdown;
                        _api.sendMessage(userId, "<pre>Режим завершение работы: " + ( isFullShutdown ? "сервер и бот" : "только сервер" ) + "</pre>");
                        break;
                    }
                    case "/start_server": {
                        startServer();
                        return;
                    }*/
                    default: {
                        if (Config.DEBUG) {
                            _api.sendMessage(userId, "<pre>update:\n" + _json.toJson(update) + "</pre>");
                        } else {
                            _api.sendMessage(userId, "<pre>Unregistered command: [" + messageText + "]</pre>");
                        }
                    }
                }
            } catch (Exception e) {
                _api.sendMessage(userId, "<pre>" + Utils.getStackTrace(e) + "</pre>");
                if (Config.DEBUG) {
                    e.printStackTrace();
                    _api.sendMessage(userId, "<pre>update:\n" + _json.toJson(update) + "</pre>");
                }
            }
        }
    }

    private static void startServer() {
        Class<?> clazz = null;

        try {
            clazz = Class.forName(_arguments[0]);
        } catch (ClassNotFoundException e) {
            //
        }

        if (clazz == null) {
            if (!Config.DEBUG) {
                System.out.println("DelayedTasksManager: Main class not found : " + _arguments[0] + "!");
                Runtime.getRuntime().exit(0);
            }
        } else {
            try {
                Method main = clazz.getDeclaredMethod("main", String[].class);
                main.invoke(clazz, new Object[]{ Arrays.copyOfRange(_arguments, 1, _arguments.length) });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String... args) throws Exception {
        Config.initialize();
        DatabaseHelper.initialize();

        _arguments = args;

        if (_arguments.length == 0) {
            if (!Config.DEBUG) {
                System.out.println("DelayedTasksManager: Main class not specified!");
                return;
            }
        } else {
            startServer();
        }

        new DelayedTasksManager();
    }
}
