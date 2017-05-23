package me.yluo.ruisiapp.myhttp;

import android.content.Context;
import android.text.TextUtils;

import java.util.Map;

import me.yluo.ruisiapp.App;

public class HttpUtil {
    private static AsyncHttpClient client = new AsyncHttpClient();
    private static SyncHttpClient syncHttpClient = new SyncHttpClient();
    private static PersistentCookieStore store;

    private static String getUrl(String url) {
        if (url.startsWith("http")) {
            return url;
        } else {
            return App.getBaseUrl() + url;
        }
    }

    public static void get(String url, ResponseHandler handler) {
        init(null);
        client.get(getUrl(url), handler);
    }


    public static void get(Context context, String url, ResponseHandler handler) {
        init(context);
        client.get(getUrl(url), handler);
    }

    public static void post(Context context, String url, Map<String, String> map, ResponseHandler handler) {
        init(context);
        if (!map.containsKey("formhash") || TextUtils.isEmpty(map.get("formhash"))) {
            String hash = context.getSharedPreferences(App.MY_SHP_NAME, Context.MODE_PRIVATE).
                    getString(App.HASH_KEY, "");
            if (!TextUtils.isEmpty(hash)) {
                map.put("formhash", hash);
            }
        }
        client.post(getUrl(url), map, handler);
    }

    public static void head(Context context, String url, Map<String, String> params, ResponseHandler handler) {
        init(context);
        client.head(getUrl(url), params, handler);
    }

    private static void init(Context context) {
        client.setConnectionTimeout(4000);
        if (context != null && store == null) {
            store = new PersistentCookieStore(context.getApplicationContext());
            client.setStore(store);
        }

    }

    //同步
    public static void SyncGet(Context context, String url, ResponseHandler handler) {
        init(context);
        syncHttpClient.get(getUrl(url), handler);
    }

    public static void exit() {
        store.clearCookie();
    }

    public static PersistentCookieStore getStore(Context context) {
        if (store == null) {
            store = new PersistentCookieStore(context);
            return store;
        } else {
            return store;
        }
    }

    public static AsyncHttpClient getClient() {
        return client;
    }
}
