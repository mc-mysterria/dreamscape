package net.mysterria.dreamscape.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.mysterria.dreamscape.Dreamscape;
import net.mysterria.dreamscape.manager.DreamscapeManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DreamscapeStorage {
    private final Dreamscape plugin;
    private final File dataFile;
    private final Gson gson;

    public DreamscapeStorage(Dreamscape plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "dreamscape_state.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveState(DreamscapeManager manager) {
        JsonObject json = new JsonObject();
        json.addProperty("active", manager.isActive());
        json.addProperty("dreamPhase", manager.isDreamPhase());

        if (manager.getTargetTime() != null) {
            json.addProperty("targetTime", manager.getTargetTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
        }

        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save dreamscape state: " + e.getMessage());
        }
    }

    public void loadState(DreamscapeManager manager) {
        if (!dataFile.exists()) return;

        try (FileReader reader = new FileReader(dataFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            if (json != null) {
                manager.setActive(json.get("active").getAsBoolean());
                manager.setDreamPhase(json.get("dreamPhase").getAsBoolean());

                if (json.has("targetTime") && !json.get("targetTime").isJsonNull()) {
                    String timeStr = json.get("targetTime").getAsString();
                    manager.setTargetTime(ZonedDateTime.parse(timeStr, DateTimeFormatter.ISO_ZONED_DATE_TIME));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load dreamscape state: " + e.getMessage());
        }
    }
}