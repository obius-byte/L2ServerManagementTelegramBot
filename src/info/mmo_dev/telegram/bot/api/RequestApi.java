package info.mmo_dev.telegram.bot.api;

import com.google.gson.Gson;
import info.mmo_dev.telegram.bot.api.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestApi {
    private final Gson _gson = new Gson();

    private final String _baseUrl;

    private int _lastUpdateId = 0;

    public RequestApi(String token) {
        _baseUrl = "https://api.telegram.org/bot" + token + "/";
    }

    private <T> ResponseApi<T> request(String method, String postData, final Class<T> clazz) {
        String jsonString;

        try {
            URL url = new URL(_baseUrl + method);

            HttpURLConnection req = (HttpURLConnection) url.openConnection();
            req.setRequestMethod("POST");
            req.setRequestProperty("Content-Type", "application/json");
            req.setRequestProperty("Accept", "application/json");
            req.setDoInput(true);
            req.setUseCaches(false);

            if (postData != null) {
                req.setDoOutput(true);

                try (OutputStream os = req.getOutputStream()) {
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            req.connect();

            StringBuilder str = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
                in.lines().forEach(str::append);
            }

            jsonString = str.toString();
        } catch (Exception e) {
            jsonString = "{ok:false,result:false,description: \"" + e.getMessage().replace(_baseUrl, "/") + "\"}";
            e.printStackTrace();
        }

        return _gson.fromJson(jsonString, getResponseType(clazz));
    }

    private Type getResponseType(Class<?> parameter) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{parameter};
            }

            @Override
            public Type getRawType() {
                return ResponseApi.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    public ResponseApi<WebhookInfo> getWebhookInfo() {
        return request("getWebhookInfo", null, WebhookInfo.class);
    }

    public ResponseApi<Boolean> setWebhook(/*String url, InputFile certificate, String ip_address, int max_connections,
                                           List<String> allowed_updates, boolean drop_pending_updates, String secret_token*/) {
        Map<String, Object> parameters = new HashMap<>();
        //parameters.put("url", url);
        //parameters.put("certificate", inputFile); // https://core.telegram.org/bots/api#inputfile
        //parameters.put("ip_address", ip_address);
        //parameters.put("max_connections", max_connections);
        //parameters.put("allowed_updates", allowed_updates);
        //parameters.put("drop_pending_updates", drop_pending_updates);
        //parameters.put("secret_token", secret_token);

        return request("setWebhook", _gson.toJson(parameters), Boolean.class);
    }

    // TODO: Only required fields
    public ResponseApi<Message> sendMessage(long chat_id, String text) {
        return sendMessage(
                chat_id,
                text,
                0,
                "html",
                null,
                true,
                false,
                false,
                0,
                true,
                null
        );
    }

    public ResponseApi<Message> sendMessage(long chat_id, String text, int message_thread_id, String parse_mode,
                                            List<MessageEntity> entities, boolean disable_web_page_preview,
                                            boolean disable_notification, boolean protect_content,
                                            int reply_to_message_id, boolean allow_sending_without_reply,
                                            Object reply_markup) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("chat_id", chat_id);
        parameters.put("text", text);
        parameters.put("message_thread_id", message_thread_id);

        parameters.put("parse_mode", parse_mode);
        parameters.put("entities", entities);
        parameters.put("disable_web_page_preview", disable_web_page_preview);
        parameters.put("disable_notification", disable_notification);
        parameters.put("protect_content", protect_content);
        parameters.put("reply_to_message_id", reply_to_message_id);
        parameters.put("allow_sending_without_reply", allow_sending_without_reply);
        parameters.put("reply_markup", reply_markup);

        return request("sendMessage", _gson.toJson(parameters), Message.class);
    }

    public ResponseApi<Update[]> getUpdates(int limit, int timeout, List<String> allowed_updates) {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("offset", _lastUpdateId);
        parameters.put("limit", limit);
        parameters.put("timeout", timeout);
        parameters.put("allowed_updates", allowed_updates);

        ResponseApi<Update[]> response = request("getUpdates", _gson.toJson(parameters), Update[].class);

        int updatesCount = response.result.length;

        _lastUpdateId = updatesCount > 0 ? response.result[updatesCount - 1].update_id + 1 : 0;

        return response;
    }
}
