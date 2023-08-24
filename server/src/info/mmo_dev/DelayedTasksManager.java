package info.mmo_dev;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Arrays;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DelayedTasksManager {

    enum Emulator {
        RebellionTeam,
        MobiusDev,
        //L2_Scripts
        PWSOFT
    }

    private Emulator _emulator;

    private Object _shutdownInstance;

    private String _url, _user, _password;

    private DelayedTasksManager() {
        try {
            Class<?> clazz = Class.forName("l2r.gameserver.Shutdown");

            Method method = clazz.getDeclaredMethod("getInstance");

            _shutdownInstance = method.invoke(null);

            _emulator = Emulator.RebellionTeam;
        } catch (Exception e) {
            // ignore case
        }

        try {
            Class<?> clazz = Class.forName("org.l2jmobius.gameserver.Shutdown");

            Method method = clazz.getDeclaredMethod("getInstance");

            _shutdownInstance = method.invoke(null);

            _emulator = Emulator.MobiusDev;
        } catch (Exception e) {
            // ignore case
        }

        try {
            Class<?> clazz = Class.forName("net.sf.l2j.gameserver.Shutdown");

            Method method = clazz.getDeclaredMethod("getInstance");

            _shutdownInstance = method.invoke(null);

            _emulator = Emulator.PWSOFT;
        } catch (Exception e) {
            // ignore case
        }

        /*try {
            Class<?> clazz = Class.forName("l2s.gameserver.Shutdown");

            Method method = clazz.getDeclaredMethod("getInstance");

            _shutdownInstance = method.invoke(null);

            _emulator = Emulator.L2_Scripts;
        } catch (Exception e) {
            // ignore case
        }*/

        if (_emulator != null) {
            Class<?> config = null;

            try {
                switch (_emulator) {
                    case RebellionTeam:
                        config = Class.forName("l2r.gameserver.Config");
                        break;
                    case MobiusDev:
                        config = Class.forName("org.l2jmobius.Config");
                        break;
                    case PWSOFT:
                        config = Class.forName("net.sf.l2j.Config");
                        break;
                    /*case L2_Scripts:

                        break;*/
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            if (config == null) {
                System.out.println("DelayedTasksManager: Config not found!");
                return;
            }

            _url = getFieldValue(config, "DATABASE_URL");
            _user = getFieldValue(config, "DATABASE_LOGIN");
            _password = getFieldValue(config, "DATABASE_PASSWORD");

            ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(1);

            threadPool.scheduleWithFixedDelay(() -> {
                try (Connection connection = DriverManager.getConnection(_url, _user, _password);
                     PreparedStatement select = connection.prepareStatement("SELECT * FROM delayed_tasks");
                     ResultSet resultSet = select.executeQuery()) {
                    while (resultSet.next()) {
                        long taskId = resultSet.getLong("id");
                        String event = resultSet.getString("event");
                        String args = resultSet.getString("args");

                        switch (event) {
                            case "shutdown":
                            case "restart":
                                executeShutdownSchedule(Integer.parseInt(args), event.equals("restart"));
                                break;
                            case "about":
                                executeShutdownAbort();
                                break;
                            default:
                                System.out.println("Unregistered command: " + event);
                        }

                        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM delayed_tasks WHERE id = ?")) {
                            delete.setLong(1, taskId);
                            delete.execute();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, 2, 60, TimeUnit.SECONDS);
        } else {
            System.out.println("DelayedTasksManager: Emulator not detected!");
        }
    }

    private void executeShutdownSchedule(int seconds, boolean isRestart) {
        try {
            if (_emulator == Emulator.RebellionTeam) {
                Method method = _shutdownInstance.getClass().getMethod("schedule", int.class, int.class);
                method.invoke(_shutdownInstance, seconds, isRestart ? 2 : 0);
            } else if (_emulator == Emulator.MobiusDev || _emulator == Emulator.PWSOFT) {
                Method scheduleMethod = _shutdownInstance.getClass().getMethod("startShutdown", int.class, Boolean.class);
                scheduleMethod.invoke(_shutdownInstance, seconds, isRestart);
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
            } else if (_emulator == Emulator.MobiusDev || _emulator == Emulator.PWSOFT) {
                Method method = _shutdownInstance.getClass().getMethod("abort", Object.class);
                method.invoke(_shutdownInstance, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFieldValue(final Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);

            return (String) field.get(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void main(String... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
