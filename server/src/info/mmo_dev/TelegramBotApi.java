package info.mmo_dev;

import info.mmo_dev.model.*;
import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TelegramBotApi {

    private final String _baseUrl;

    private int _lastUpdateId = 0;

    public TelegramBotApi(String token) {
        _baseUrl = "https://api.telegram.org/bot" + token + "/";
    }

    private String getRequest(String method, String postData) {
        StringBuilder buf = new StringBuilder("");

        try
        {
            URL url = new URL(_baseUrl + method);

            HttpURLConnection req = (HttpURLConnection) url.openConnection();
            req.setRequestMethod("POST");
            req.setRequestProperty("Content-Type", "application/json");
            req.setRequestProperty("Accept", "application/json");
            req.setDoInput( true);
            req.setUseCaches(false);

            if (postData != null) {
                req.setDoOutput(true);

                try(OutputStream os = req.getOutputStream()) {
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            req.connect();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
                in.lines().forEach(buf::append);
            }
        }
        catch(Exception e)
        {
            buf.append("{}");
            e.printStackTrace();
        }

        return buf.toString();
    }

    public WebhookInfo getWebhookInfo() {
        WebhookInfo webhookInfo = null;

        JSONObject obj = getObject(getRequest("getWebhookInfo", null));

        if (obj != null) {
            webhookInfo = new WebhookInfo();

            JSONObject result = obj.getJSONObject("result");

            webhookInfo.url = result.has("url") ? result.getString("url") : null;
            webhookInfo.has_custom_certificate = result.has("has_custom_certificate") && result.getBoolean("has_custom_certificate");
            webhookInfo.pending_update_count = result.has("pending_update_count") ? result.getInt("pending_update_count") : 0;
            webhookInfo.ip_address = result.has("ip_address") ? result.getString("ip_address") : null;
            webhookInfo.last_error_date = result.has("last_error_date") ? result.getInt("last_error_date") : 0;
            webhookInfo.last_error_message = result.has("last_error_message") ? result.getString("last_error_message") : null;
            webhookInfo.last_synchronization_error_date = result.has("last_synchronization_error_date") ? result.getInt("last_synchronization_error_date") : 0;
            webhookInfo.max_connections = result.has("max_connections") ? result.getInt("max_connections") : 0;
            if (result.has("allowed_updates")) {
                List<String> allowed_updates = new ArrayList<>();
                for (Object allowed_update: result.getJSONArray("allowed_updates")) {
                    allowed_updates.add((String) allowed_update);
                }
                webhookInfo.allowed_updates = allowed_updates;
            }
        }

        return webhookInfo;
    }

    public void setWebhook(String url, /*InputFile certificate,*/ String ip_address, int max_connections,
                           List<String> allowed_updates, boolean drop_pending_updates, String secret_token) {
        JSONObject parameters = new JSONObject();
        parameters.put("url", url);
        //parameters.put("certificate", inputFile); // https://core.telegram.org/bots/api#inputfile
        //parameters.put("ip_address", ip_address);
        //parameters.put("max_connections", max_connections);
        parameters.put("allowed_updates", allowed_updates);
        //parameters.put("drop_pending_updates", drop_pending_updates);
        //parameters.put("secret_token", secret_token);

        JSONObject obj = getObject(getRequest("setWebhook", parameters.toString()));

        System.out.println("obj: " + obj);
    }

    public List<Update> getUpdates(int limit, int timeout, List<String> allowed_updates) {
        List<Update> updates = new ArrayList<>();

        JSONObject parameters = new JSONObject();
        parameters.put("offset", _lastUpdateId);
        parameters.put("limit", limit);
        parameters.put("timeout", timeout);
        parameters.put("allowed_updates", allowed_updates);

        JSONObject getUpdates = getObject(getRequest("getUpdates", parameters.toString()));

        if (getUpdates != null) {
            JSONArray result = getUpdates.getJSONArray("result");
            for (int i = 0; i < result.length(); i++) {
                updates.add(getUpdate(result.getJSONObject(i)));
            }
        }

        _lastUpdateId = updates.get(updates.size() - 1).update_id + 1;

        return updates;
    }

    private Update getUpdate(JSONObject obj) {
        Update update = new Update();

        update.update_id = obj.has("update_id") ? obj.getInt("update_id") : 0;
        if (obj.has("message")) {
            JSONObject messageObj = obj.getJSONObject("message");

            update.message = new Message();
            update.message.message_id = messageObj.has("message_id") ? messageObj.getInt("message_id") : 0;
            update.message.message_thread_id = messageObj.has("message_thread_id") ? messageObj.getInt("message_thread_id") : 0;

            if (messageObj.has("from")) {
                JSONObject chatObj = messageObj.getJSONObject("from");

                update.message.from = new User();
                update.message.from.id = chatObj.has("id") ? chatObj.getInt("id") : 0;
                update.message.from.is_bot = chatObj.has("is_bot") && chatObj.getBoolean("is_bot");
                update.message.from.first_name = chatObj.has("first_name") ? chatObj.getString("first_name") : null;
                update.message.from.last_name = chatObj.has("last_name") ? chatObj.getString("last_name") : null;
                update.message.from.username = chatObj.has("username") ? chatObj.getString("username") : null;
                update.message.from.language_code = chatObj.has("language_code") ? chatObj.getString("language_code") : null;
            }

            update.message.date = messageObj.has("date") ? messageObj.getInt("date") : 0;

            if (messageObj.has("chat")) {
                JSONObject chatObj = messageObj.getJSONObject("chat");

                update.message.chat = new Chat();
                update.message.chat.id = chatObj.has("id") ? chatObj.getInt("id") : 0;
                update.message.chat.type = chatObj.has("type") ? chatObj.getString("type") : null;
                update.message.chat.title = chatObj.has("title") ? chatObj.getString("title") : null;
                update.message.chat.username = chatObj.has("username") ? chatObj.getString("username") : null;
                update.message.chat.first_name = chatObj.has("first_name") ? chatObj.getString("first_name") : null;
                update.message.chat.last_name = chatObj.has("last_name") ? chatObj.getString("last_name") : null;
            }

            update.message.text = messageObj.has("text") ? messageObj.getString("text") : null;
        }

        return update;
    }

    private JSONObject getObject(String json) {
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String... args) {
        TelegramBotApi api = new TelegramBotApi("5886145760:AAF8HU6L8sNaeIf3RaO7Ghp_F1-WOBjdids");

        List<String> allowed_updates = new ArrayList<String>(){{add("message");}};

        api.setWebhook("https://google.com", "0.0.0.0", 100, allowed_updates, false, "1234567890");
        System.out.println(api.getWebhookInfo().url);
        /*List<Update> updates = api.getUpdates(10, 0, allowed_updates);
        for (Update update: updates) {
            System.out.println("updateId: " + update.update_id + " chatId: " + update.message.chat.id + " userId: " + update.message.from.id + " text: " + update.message.text);
        }*/
    }
}
