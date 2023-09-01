package info.mmo_dev;

import info.mmo_dev.telegram.bot.api.RequestApi;
import info.mmo_dev.telegram.bot.api.ResponseApi;
import info.mmo_dev.telegram.bot.api.model.Update;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DelayedTasksManager {

    private Emulator _emulator;

    private Object _shutdownInstance;

    private Object _threadPoolInstance;

    private RequestApi _telegramApi;

    private DelayedTasksManager() {
        try {
            Config.initialize();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (Emulator emulator: Emulator.values()) {
            if (Package.getPackage(emulator.getPath()) != null) {
                _emulator = emulator;
                break;
            }
        }

        if (_emulator != null) {
            System.out.println("DelayedTasksManager: Emulator detected[" + _emulator + "]");

            try {
                Class<?> classShutdown = Class.forName(_emulator.getPath() + ".Shutdown");
                Method methodInstance = classShutdown.getDeclaredMethod("getInstance");
                _shutdownInstance = methodInstance.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Class<?> classThreadPool = Class.forName(_emulator.getPath() + ".ThreadPoolManager");
                Method methodInstance = classThreadPool.getDeclaredMethod("getInstance");
                _threadPoolInstance = methodInstance.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            _telegramApi = new RequestApi(Config.BOT_TOKEN);
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

                                        _telegramApi.sendMessage(userId, text);
                                    } else if (command[0].equals("/add_item")) {
                                        if (command.length == 4) {
                                            _telegramApi.sendMessage(userId, addItem(command[1], Integer.parseInt(command[2]), Integer.parseInt(command[3])));
                                        }
                                    } else if (command[0].equals("/online")) {
                                        _telegramApi.sendMessage(userId, getOnline());
                                    } else if (command[0].equals("/items_delayed_status")) {
                                        _telegramApi.sendMessage(userId, getItemsDelayedStatus());
                                    } else if (command[0].equals("/restart") || command[0].equals("/shutdown")) {
                                        if (command.length == 2) {
                                            executeShutdownSchedule(Integer.parseInt(command[1]), command[0].equals("/restart"));
                                        } else {
                                            // TODO: throw new ArrayIndexOutOfBoundsException???
                                        }
                                    } else if (command[0].equals("/shutdown_abort")) {
                                        executeShutdownAbort();
                                    } else if (command[0].equals("/thread_pool_status")) {
                                        _telegramApi.sendMessage(userId, "<pre>" + getThreadPoolStatus() + "</pre>");
                                    }
                                }
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
                                        update.message.message_id,
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
        } else {
            System.out.println("DelayedTasksManager: Emulator not detected!");
        }
    }

    private String addItem(String charName, int itemId, int itemCount) {
        String text = "";
        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(SqlQueries.SELECT_OBJ_ID_CHARACTERS)) {
            select.setString(1, charName);
            try (ResultSet resultSet = select.executeQuery()) {
                if (resultSet.next()) {
                    int charId = resultSet.getInt(1);
                    try (PreparedStatement insert = connection.prepareStatement(SqlQueries.INSERT_ITEMS_DELAYED)) {
                        insert.setInt(1, charId);
                        insert.setInt(2, itemId);
                        insert.setInt(3, itemCount);
                        insert.execute();

                        text = "Successfully!";
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

    private String getOnline() {
        String text = "";
        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(SqlQueries.SELECT_ONLINE_COUNT_CHARACTERS);
             ResultSet resultSet = select.executeQuery()) {
            if (resultSet.next()) {
                text = "Current online: <b>" + resultSet.getInt(1) + "</b>";
            }
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    private String getItemsDelayedStatus() {
        String text = "";
        try (Connection connection = DatabaseConnectionFactory.getGameConnection();
             PreparedStatement select = connection.prepareStatement(SqlQueries.SELECT_ITEMS_DELAYED);
             ResultSet resultSet = select.executeQuery()) {
            text = "<pre>|" + /*column( 'Char name', true ) .*/ Utils.column( "Item Id" )
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
            text += "</pre>";
        } catch (SQLException e) {
            text = e.getMessage();
        }
        return text;
    }

    private void executeShutdownSchedule(int seconds, boolean isRestart) {
        try {
            if (_emulator == Emulator.RebellionTeam) {
                Method method = _shutdownInstance.getClass().getDeclaredMethod("schedule", int.class, int.class);
                method.invoke(_shutdownInstance, seconds, isRestart ? 2 : 0);
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

            }*/ else if (_emulator == Emulator.PWSOFT) {
                Method method = _shutdownInstance.getClass().getDeclaredMethod("startTelnetShutdown", String.class, int.class, boolean.class);
                method.invoke(_shutdownInstance, "127.0.0.1", seconds, isRestart);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeShutdownAbort() {
        try {
            if (_emulator == Emulator.RebellionTeam) {
                Method method = _shutdownInstance.getClass().getMethod("cancel");
                method.invoke(_shutdownInstance);
            } /*else if (_emulator == Emulator.MobiusDev) {
                Class<?> player = _emulator == Emulator.MobiusDev
                        ? Class.forName("org.l2jmobius.gameserver.model.actor.Player")
                        : Class.forName("net.sf.l2j.gameserver.model.actor.instance.L2PcInstance");

                Method method = _shutdownInstance.getClass().getMethod("abort", player);
                method.invoke(_shutdownInstance, player);
            } else if (_emulator == Emulator.L2Scripts) {

            }*/ else if (_emulator == Emulator.PWSOFT) {
                Method method = _shutdownInstance.getClass().getMethod("telnetAbort", String.class);
                method.invoke(_shutdownInstance, "127.0.0.1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getThreadPoolStatus() {
        String text = "";
        try {
            if (_emulator == Emulator.PWSOFT) {
                Method method = _threadPoolInstance.getClass().getMethod("getTelemetry");
                text = (String) method.invoke(_threadPoolInstance);
            } else if (_emulator == Emulator.RebellionTeam) {
                Method method = _threadPoolInstance.getClass().getMethod("getStats");
                text = ((StringBuilder) method.invoke(_threadPoolInstance)).toString();
            }
        } catch (Exception e) {
            text = e.getMessage();
        }
        return text;
    }

    public static void main(String... args)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (args.length == 0) {
            System.out.println("DelayedTasksManager: Main class not specified!");
            return;
        }

        Class<?> clazz = null;
        try {
            clazz = Class.forName(args[0]);
        } catch (Exception e) {
            // ignore
        }

        if (clazz == null) {
            System.out.println("DelayedTasksManager: Main class not found : " + args[0] + "!");
            return;
        }

        Method main = clazz.getDeclaredMethod("main", String[].class);
        args = Arrays.copyOfRange(args, 1, args.length);
        main.invoke(clazz, new Object[]{args});

        new DelayedTasksManager();
    }
}
