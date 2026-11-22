package com.pnl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Global mod settings (not per server). Saved in config/pnl/_settings.json */
public final class Settings {
    public boolean showBoxes = true;
    public int volume = 100; // percent, 0-100
    public boolean dmAlerts = true;
    public boolean mentionAlerts = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings inst;

    public static Settings get() {
        if (inst == null) load();
        return inst;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("pnl").resolve("_settings.json");
    }

    private static void load() {
        Settings s = null;
        try {
            Path f = file();
            if (Files.exists(f)) s = GSON.fromJson(Files.readString(f), Settings.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        inst = s == null ? new Settings() : s;
    }

    public void save() {
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
