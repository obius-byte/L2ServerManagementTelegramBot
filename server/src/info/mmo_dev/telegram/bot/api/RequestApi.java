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

    private <T> ResponseApi<T> getRequest(String method, String postData, final Class<T> clazz) {
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

            StringBuilder str = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8))) {
                in.lines().forEach(str::append);
            }

            return _gson.fromJson(str.toString(), getResponseType(clazz));
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return _gson.fromJson("{ok:false,result:false,description: \"" + e.getMessage().replace(_baseUrl, "/") + "\"}", getResponseType(clazz));
        }
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
        return getRequest("getWebhookInfo", null, WebhookInfo.class);
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

        return getRequest("setWebhook", _gson.toJson(parameters), Boolean.class);
    }

    public ResponseApi<Message> sendMessage(int chat_id, String text, int message_thread_id) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("chat_id", chat_id);
        parameters.put("text", text);
        parameters.put("message_thread_id", message_thread_id);

        return getRequest("sendMessage", _gson.toJson(parameters), Message.class);
    }

    public ResponseApi<Update[]> getUpdates(int limit, int timeout, List<String> allowed_updates) {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("offset", _lastUpdateId);
        parameters.put("limit", limit);
        parameters.put("timeout", timeout);
        parameters.put("allowed_updates", allowed_updates);

        ResponseApi<Update[]> response = getRequest("getUpdates", _gson.toJson(parameters), Update[].class);

        _lastUpdateId = response.result[ response.result.length - 1 ].update_id + 1;

        return response;
    }
}
