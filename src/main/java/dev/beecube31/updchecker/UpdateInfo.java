package dev.beecube31.updchecker;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class UpdateInfo {
    private final String version;
    private final String downloadUrl;
    private final Map<String, String> changelogs;

    public UpdateInfo(String version, String downloadUrl, Map<String, String> changelogs) {
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.changelogs = changelogs;
    }

    public String getVersion() {
        return version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getLocalizedChangelog(String currentLocale) {
        if (changelogs.isEmpty()) return "";

        currentLocale = currentLocale.toLowerCase();

        if (changelogs.containsKey(currentLocale)) {
            return changelogs.get(currentLocale);
        }

        String langPrefix = currentLocale.split("_")[0];
        for (Map.Entry<String, String> entry : changelogs.entrySet()) {
            if (entry.getKey().startsWith(langPrefix)) {
                return entry.getValue();
            }
        }

        // Fallback
        if (changelogs.containsKey("en_us")) {
            return changelogs.get("en_us");
        }

        return changelogs.values().iterator().next();
    }

    public static UpdateInfo fromJson(JsonObject json) {
        String version = json.has("version") ? json.get("version").getAsString() : "";
        String downloadUrl = json.has("download_url") ? json.get("download_url").getAsString() : "";

        Map<String, String> changelogs = new HashMap<>();
        if (json.has("changelog") && json.get("changelog").isJsonObject()) {
            JsonObject chJson = json.getAsJsonObject("changelog");
            for (String key : chJson.keySet()) {
                changelogs.put(key.toLowerCase(), chJson.get(key).getAsString());
            }
        }

        return new UpdateInfo(version, downloadUrl, changelogs);
    }
}
