package info.mmo_dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import info.mmo_dev.emulators.*;
import info.mmo_dev.telegram.bot.api.*;
import info.mmo_dev.telegram.bot.api.model.*;

import java.lang.reflect.Method;
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
        put("characters_list", "Список персонажей");
        put("find_character", "Поиск персонажа");
        put("character_info", "Информация о персонаже");
        put("character_inventory", "Инвентарь персонажа");
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

        System.out.println("DelayedTasksManager: Emulator selected [" + _emulator.getType() + "]");

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
            List<String> allowedUpdates = new ArrayList<>();
            //allowedUpdates.add("message");
            //allowedUpdates.add("callback_query");

            ResponseApi<Update[]> response = _api.getUpdates(10, 0, allowedUpdates);

            if (!response.ok) {
                System.out.println("DelayedTasksManager: getUpdates: " + response.description);
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
        String charIdColumn = DatabaseHelper.getTable("characters").columnExists("charId") ? "charId" : "obj_Id";

        if (DatabaseHelper.tableExists("items_delayed")) {
            sqlLastId = "SELECT payment_id AS id FROM items_delayed ORDER BY payment_id DESC LIMIT 1";
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

        try {
            Map<String, String> lastEntity = DatabaseHelper.getEntity(sqlLastId);
            if (lastEntity.size() > 0) {
                _lastItemsDelayedId = Integer.parseInt(lastEntity.get("id"));
            }
        } catch (SQLException e) {
            if (Config.DEBUG)
                e.printStackTrace();

            for (long userId : Config.USER_IDS) {
                _api.sendMessage(userId, "<pre>" + Utils.getStackTrace(e) + "</pre>");
            }
        }

        _threadPool.scheduleWithFixedDelay(() -> {
            try {
                List<Object> parameters = new ArrayList<>();
                parameters.add(_lastItemsDelayedId);

                List<Map<String, String>> entities = DatabaseHelper.getEntities(sqlSelect, parameters);
                for (Map<String, String> entity: entities) {
                    String charName = entity.get("char_name");
                    if (charName == null) {
                        charName = entity.get("owner_id");
                    }
                    int itemId = Integer.parseInt(entity.get("item_id"));

                    if (Config.DELAYED_ITEMS_LISTENER_EXCLUDE_ITEM_IDS.contains(itemId))
                        continue;

                    int itemCount = Integer.parseInt(entity.get("count"));

                    _lastItemsDelayedId = Integer.parseInt(entity.get("payment_id"));

                    String text = "<b>New Delayed Items record</b>";
                    text += "\n\nCharacter: " + charName;
                    text += "\nItem ID: " + itemId;
                    text += "\nCount: " + itemCount;

                    for (long userId : Config.USER_IDS) {
                        _api.sendMessage(userId, "<pre>" + text + "</pre>");
                    }
                }
            } catch (SQLException e) {
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
        } else if (update.callback_query != null) {
            userId = update.callback_query.from.id;
            messageId = update.callback_query.message.message_id;
            messageText = update.callback_query.data;
        } else if (update.edited_message != null) {
            //userId = update.edited_message.from.id;
            //messageId = update.edited_message.message_id;
            //messageText = update.edited_message.text;
            return;
        } else {
            System.out.println("userId not found!\nupdate: " + _json.toJson(update));
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

                ResponseApi<Message> responseApi = null;

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
                            responseApi = _api.sendMessage(userId, _emulator.addItem(command[1], Integer.parseInt(command[2]), Integer.parseInt(command[3])));
                        } else {
                            responseApi = _api.sendMessage(userId,"/add_item", new ForceReply(true, "nickName itemId itemCount", false));
                        }
                        break;
                    }
                    case "/online": {
                        responseApi = _api.sendMessage(userId, String.format("Current online: <b>%s</b>", _emulator.getCountOnline()));
                        break;
                    }
                    case "/items_delayed_status": {
                        responseApi = _api.sendMessage(userId, "<pre>" + _emulator.getItemsDelayedStatus() + "</pre>");
                        break;
                    }
                    case "/restart": case "/shutdown": {
                        if (command.length == 2) {
                            String text = _emulator.executeShutdownSchedule(Integer.parseInt(command[1]), command[0].equals("/restart"), false);
                            responseApi = _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        } else {
                            responseApi = _api.sendMessage(userId, command[0], new ForceReply(true, "seconds", false));
                        }
                        break;
                    }
                    case "/shutdown_abort": {
                        String text = _emulator.executeShutdownSchedule(0, false, true);
                        responseApi = _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        break;
                    }
                    case "/thread_pool_status": {
                        responseApi = _api.sendMessage(userId, "<pre>" + _emulator.getThreadPoolStatus() + "</pre>");
                        break;
                    }
                    case "/characters_list": {
                        String result = "<b>Characters List</b>\n";
                        int currentPage = command.length == 2 ? Integer.parseInt(command[1]) : 1;

                        InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
                        Map<String, String> entity = DatabaseHelper.getEntity("SELECT COUNT(*) AS count FROM characters");
                        if (entity.size() > 0) {
                            int total = Integer.parseInt(entity.get("count"));
                            if (total > 0) {
                                int limit = 50;
                                int offset = limit * (currentPage - 1);

                                String charIdColumn = DatabaseHelper.getTable("characters").columnExists("obj_Id") ? "obj_Id" : "charId";
                                List<Object> parameters = new ArrayList<>();
                                parameters.add(limit);
                                parameters.add(offset);

                                List<Map<String, String>> characterList = DatabaseHelper.getEntities("SELECT char_name FROM characters ORDER BY " + charIdColumn + " LIMIT ? OFFSET ?", parameters);
                                int i = 0;
                                int rowIndex = 0;
                                List<InlineKeyboardButton> buttonList;
                                InlineKeyboardButton button;
                                for (Map<String, String> character: characterList) {
                                    String charName = character.get("char_name");
                                    button = new InlineKeyboardButton(charName, "/character_info " + charName);
                                    if (i++ % 2 == 0) {
                                        buttonList = new ArrayList<>();
                                        buttonList.add(button);
                                        replyMarkup.addRow(rowIndex, buttonList);
                                    } else {
                                        replyMarkup.addCol(rowIndex++, button);
                                    }
                                }

                                if (currentPage > 1) {
                                    buttonList = new ArrayList<>();
                                    buttonList.add(new InlineKeyboardButton("<<< PREV", "/characters_list " + (currentPage - 1)));
                                    replyMarkup.addRow(buttonList);
                                }

                                if (total > limit) {
                                    int numberOfPages = (int) Math.ceil((double) total / limit);

                                    if (currentPage < numberOfPages) {
                                        buttonList = new ArrayList<>();
                                        buttonList.add(new InlineKeyboardButton("NEXT >>>", "/characters_list " + (currentPage + 1)));
                                        replyMarkup.addRow(buttonList);
                                    }
                                }
                            } else {
                                result = "empty";
                            }
                        }

                        responseApi = _api.sendMessage(
                                userId,
                                result,
                                0,
                                "html",
                                null,
                                true,
                                false,
                                false,
                                0,
                                true,
                                replyMarkup
                        );
                        break;
                    }
                    case "/find_character": {
                        if (command.length == 2) {
                            String query = command[1];
                            List<Object> parameters = new ArrayList<>();
                            parameters.add(query);

                            List<Map<String, String>> entities = DatabaseHelper.getEntities("SELECT char_name FROM characters WHERE char_name LIKE CONCAT( '%',?,'%')", parameters);
                            int i = 0;
                            int rowIndex = 0;
                            List<InlineKeyboardButton> buttonList;
                            InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
                            for (Map<String, String> entity: entities) {
                                String charName = entity.get("char_name");
                                InlineKeyboardButton button = new InlineKeyboardButton(charName, "/character_info " + charName);
                                if (i++ % 2 == 0) {
                                    buttonList = new ArrayList<>();
                                    buttonList.add(button);
                                    replyMarkup.addRow(rowIndex, buttonList);
                                } else {
                                    replyMarkup.addCol(rowIndex++, button);
                                }
                            }

                            responseApi = _api.sendMessage(
                                    userId,
                                    "Found characters: " + entities.size(),
                                    0,
                                    "html",
                                    null,
                                    true,
                                    false,
                                    false,
                                    0,
                                    true,
                                    replyMarkup
                            );
                            break;
                        } else {
                            responseApi = _api.sendMessage(userId, command[0], new ForceReply(true, "Enter the character name", false));
                        }
                        break;
                    }
                    case "/character_info": {
                        if (command.length == 2) {
                            String query = command[1];
                            //String charName = command[1];
                            //int charId = charName.matches("\\d*") ? Integer.parseInt(charName) : -1;
                            String result = "Character information:\n";
                            InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
                            String charIdColumn = DatabaseHelper.getTable("characters").columnExists("obj_Id") ? "obj_Id" : "charId";

                            List<Object> parameters = new ArrayList<>();
                            parameters.add(query);
                            parameters.add(query);

                            Map<String, String> character = DatabaseHelper.getEntity("SELECT * FROM characters WHERE char_name = ? OR " + charIdColumn + " = ?", parameters);
                            if (character.size() > 0) {
                                result += Utils.column("key", true, 20)
                                        + Utils.column("value", false, 20)+ "\n";
                                for (Map.Entry<String, String> entry: character.entrySet()) {
                                    String value = entry.getValue();
                                    result += Utils.column(entry.getKey(), true, 20)
                                            + Utils.column(value != null ? value : "null", false, 20)+ "\n";
                                }

                                List<InlineKeyboardButton> buttonList = new ArrayList<>();
                                buttonList.add(new InlineKeyboardButton("Inventory", "/character_inventory " + character.get(charIdColumn)));

                                replyMarkup.addRow(buttonList);
                            } else {
                                result = "Character '" + query + "' not found!";
                            }

                            responseApi = _api.sendMessage(
                                    userId,
                                    "<pre>" + result + "</pre>",
                                    0,
                                    "html",
                                    null,
                                    true,
                                    false,
                                    false,
                                    0,
                                    true,
                                    replyMarkup
                            );
                        } else {
                            responseApi = _api.sendMessage(userId, command[0], new ForceReply(true, "Enter the character name or his ID", false));
                        }
                        break;
                    }
                    case "/character_inventory": {
                        if (command.length > 1) {
                            int currentPage = command.length > 2 ? Integer.parseInt(command[2]) : 1;

                            String result;
                            InlineKeyboardMarkup replyMarkup = new InlineKeyboardMarkup();
                            String charIdColumn = DatabaseHelper.getTable("characters").columnExists("obj_Id") ? "obj_Id" : "charId";
                            List<Object> parameters = new ArrayList<>();
                            parameters.add(command[1]);
                            parameters.add(command[1]);

                            Map<String, String> character = DatabaseHelper.getEntity("SELECT char_name, " + charIdColumn + " FROM characters WHERE char_name = ? OR " + charIdColumn + " = ?", parameters);
                            if (character.size() > 0) {
                                String charName = character.get("char_name");
                                int charId = Integer.parseInt(character.get(charIdColumn));

                                result = "<b>Character Inventory: " + charName + " (" + charId + ")</b>\n";

                                parameters.clear();
                                parameters.add(charId);

                                Map<String, String> entity = DatabaseHelper.getEntity("SELECT COUNT(*) AS count FROM items WHERE owner_id = ?", parameters);
                                if (entity.size() > 0) {
                                    int total = Integer.parseInt(entity.get("count"));
                                    int limit = 90;
                                    int offset = limit * (currentPage - 1);

                                    parameters.clear();
                                    parameters.add(charId);
                                    parameters.add(limit);
                                    parameters.add(offset);

                                    List<Map<String, String>> items = DatabaseHelper.getEntities("SELECT * FROM items WHERE owner_id = ? ORDER BY count DESC LIMIT ? OFFSET ?", parameters);
                                    int i = 0;
                                    int rowIndex = 0;
                                    List<InlineKeyboardButton> buttonList;
                                    for (Map<String, String> item: items) {
                                        String itemId = item.get("item_id");
                                        //int count = Integer.parseInt(item.get("count"));

                                        InlineKeyboardButton btn = new InlineKeyboardButton(
                                                item.get("item_id") + " (" + item.get("count") + ") / " + item.get("loc"),
                                                "/character_inventory_delete_item " + charId + " " + itemId
                                        );

                                        if (i++ % 2 == 0) {
                                            buttonList = new ArrayList<>();
                                            buttonList.add(btn);
                                            replyMarkup.addRow(rowIndex, buttonList);
                                        } else {
                                            replyMarkup.addCol(rowIndex++, btn);
                                        }
                                    }

                                    if (currentPage > 1) {
                                        buttonList = new ArrayList<>();
                                        buttonList.add(new InlineKeyboardButton("<<< PREV", "/character_inventory " + charId + " " + (currentPage - 1)));
                                        replyMarkup.addRow(buttonList);
                                    }

                                    if (total > limit) {
                                        int numberOfPages = (int) Math.ceil((double) total / limit);

                                        if (currentPage < numberOfPages) {
                                            buttonList = new ArrayList<>();
                                            buttonList.add(new InlineKeyboardButton("NEXT >>>", "/character_inventory " + charId + " " + (currentPage + 1)));
                                            replyMarkup.addRow(buttonList);
                                        }
                                    }
                                } else {
                                    result += "empty";
                                }
                            } else {
                                result = "Character " + command[1] + " not found!";
                            }

                            responseApi = _api.sendMessage(
                                    userId,
                                    "<pre>" + result + "</pre>",
                                    0,
                                    "html",
                                    null,
                                    true,
                                    false,
                                    false,
                                    0,
                                    true,
                                    replyMarkup
                            );
                        } else {
                            responseApi = _api.sendMessage(userId, command[0], new ForceReply(true, "Enter the character name or his ID", false));
                        }
                        break;
                    }
                    /*case "/character_inventory_delete_item": {


                        break;
                    }*/
                    case "/ban_account": case "/unban_account": {
                        if (command.length == 2) {
                            String text = _emulator.banAccount(command[1], command[0].equals("/unban_account"));
                            responseApi = _api.sendMessage(userId, "<pre>" + ( text == null ? "Successfully!" : text ) + "</pre>");
                        } else {
                            responseApi = _api.sendMessage(userId, command[0], new ForceReply(true, "account name", false));
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
                        if (update.callback_query != null && update.callback_query.message != null) {
                            update.callback_query.message = null;
                        }
                        String text = "<pre>Unregistered command: [" + messageText + "]" + (Config.DEBUG ? "\nupdate:\n" + _json.toJson(update) : "") + "</pre>";
                        responseApi = _api.sendMessage(userId,text);
                    }
                }

                if (responseApi != null && !responseApi.ok) {
                    _api.sendMessage(userId, "<pre>" + _json.toJson(responseApi) + "</pre>");
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
