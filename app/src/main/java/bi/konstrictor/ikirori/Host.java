package bi.konstrictor.ikirori;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by KonstrIctor on 24/03/2020.
 */

class Host {
    public static void refreshToken(final Activity activity){
        logoutIfNoSession(activity);
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String refresh = sharedPreferences.getString("refresh", "");
        String json = "{\"refresh\":\""+refresh+"\"}";
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));

        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getURL(activity)+"/refresh/").newBuilder();

        String url = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(url)
                .post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONObject jsonObject = new  JSONObject(json);
                    String token = jsonObject.getString("access");
                    SharedPreferences.Editor session = sharedPreferences.edit();
                    session.putString("token", token);
                    session.commit();

                } catch (Exception e) {
                    final String message = e.getMessage();
                }
            }
        });
    }

    public static String getURL(Activity activity) {
        Log.i("==== HOST URL ====", getSessionValue(activity, "login_session", "ip"));
        return getSessionValue(activity, "login_session", "ip");
    }

    public static Member extractUser(String token) {
        String[] parts = token.split("\\.");

        byte[] data = Base64.decode(parts[1], Base64.DEFAULT);
        try {
            String text = new String(data, "UTF-8");
            CustomJson json_object = new CustomJson(text);
            Member member = new Member(
                    json_object.getString("user_id"),
                    json_object.getString("username"),
                    json_object.getString("phone"),
                    json_object.getString("email"),
                    json_object.getString("services")
            );
            return member;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void logout(Activity activity){
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
        Intent intent = new Intent(activity, LoginActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }
    public static void logoutIfNoSession(Activity activity){
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String refresh = sharedPreferences.getString("refresh", "");
        if (refresh.trim().isEmpty()) {
            Toast.makeText(activity, "Log in first", Toast.LENGTH_SHORT).show();
            logout(activity);
        }
    }

    public static String getSessionValue(Activity activity, String session, String key) {
        final SharedPreferences sharedPreferences = activity.getSharedPreferences(session, Context.MODE_PRIVATE);
        String value = sharedPreferences.getString(key, "");
        return value;
    }

    public static boolean tokenExpired(String token) throws Exception {
        String[] parts = token.split("\\.");
        byte[] data = Base64.decode(parts[1], Base64.DEFAULT);
        String text = new String(data, "UTF-8");
        CustomJson json_object = new CustomJson(text);
        long right_now = new Date().getTime();
        long date = json_object.getLong("exp");
        Log.i("===== EXPIRATION =====", Long.toString(right_now)+ " "+Long.toString(date));
        return new Date().getTime() > date;
    }
}

