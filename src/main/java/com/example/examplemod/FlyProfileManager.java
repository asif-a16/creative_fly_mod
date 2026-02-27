package com.example.examplemod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.util.Mth;
import net.neoforged.fml.loading.FMLPaths;

public final class FlyProfileManager {
    public static final int PROFILE_COUNT = 3;
    public static final float DEFAULT_FLIGHT_SPEED = 0.05F;
    public static final float MIN_FLIGHT_SPEED = 0.005F;
    public static final float MAX_FLIGHT_SPEED = 1.00F;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.GAMEDIR.get().resolve("config").resolve("creativeflymod-flight-profiles.json");

    private static final List<FlyProfile> PROFILES = new ArrayList<>();
    private static int selectedProfileIndex = 0;
    private static boolean autoArmOnJoin = true;

    private FlyProfileManager() {
    }

    public static void load() {
        resetToDefaults();

        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            StoredProfiles data = GSON.fromJson(reader, StoredProfiles.class);
            if (data != null && data.profiles != null) {
                for (int i = 0; i < PROFILE_COUNT; i++) {
                    if (i >= data.profiles.size() || data.profiles.get(i) == null) {
                        continue;
                    }

                    StoredProfile stored = data.profiles.get(i);
                    setProfileName(i, stored.name);
                    setProfileSpeed(i, stored.speed);
                }
            }

            if (data != null) {
                setSelectedProfileIndex(data.selectedProfileIndex);
                setAutoArmOnJoin(data.autoArmOnJoin);
            }
        } catch (Exception exception) {
            CreativeFlyMod.LOGGER.warn("Failed to load fly profiles, using defaults", exception);
        }
    }

    public static void save() {
        StoredProfiles data = new StoredProfiles();
        data.selectedProfileIndex = selectedProfileIndex;
        data.autoArmOnJoin = autoArmOnJoin;
        data.profiles = new ArrayList<>();

        for (FlyProfile profile : PROFILES) {
            StoredProfile stored = new StoredProfile();
            stored.name = profile.name;
            stored.speed = profile.speed;
            data.profiles.add(stored);
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            CreativeFlyMod.LOGGER.warn("Failed to save fly profiles", exception);
        }
    }

    public static List<FlyProfile> getProfiles() {
        return PROFILES;
    }

    public static FlyProfile getProfile(int index) {
        return PROFILES.get(index);
    }

    public static int getSelectedProfileIndex() {
        return selectedProfileIndex;
    }

    public static void setSelectedProfileIndex(int index) {
        selectedProfileIndex = Mth.clamp(index, 0, PROFILE_COUNT - 1);
    }

    public static void setProfileName(int index, String name) {
        FlyProfile profile = getProfile(index);
        String sanitized = name == null ? "" : name.trim();
        if (sanitized.isEmpty()) {
            sanitized = defaultName(index);
        }
        profile.name = sanitized;
    }

    public static void setProfileSpeed(int index, float speed) {
        getProfile(index).speed = Mth.clamp(speed, MIN_FLIGHT_SPEED, MAX_FLIGHT_SPEED);
    }

    public static float getSelectedProfileSpeed() {
        return getProfile(selectedProfileIndex).speed;
    }

    public static boolean isAutoArmOnJoin() {
        return autoArmOnJoin;
    }

    public static void setAutoArmOnJoin(boolean enabled) {
        autoArmOnJoin = enabled;
    }

    private static void resetToDefaults() {
        PROFILES.clear();
        PROFILES.add(new FlyProfile("Profile 1", DEFAULT_FLIGHT_SPEED));
        PROFILES.add(new FlyProfile("Fast", DEFAULT_FLIGHT_SPEED * 3.0F));
        PROFILES.add(new FlyProfile("Super Fast", DEFAULT_FLIGHT_SPEED * 10.0F));
        selectedProfileIndex = 0;
        autoArmOnJoin = true;
    }

    private static String defaultName(int index) {
        return switch (index) {
            case 0 -> "Profile 1";
            case 1 -> "Fast";
            case 2 -> "Super Fast";
            default -> "Profile";
        };
    }

    public static final class FlyProfile {
        private String name;
        private float speed;

        private FlyProfile(String name, float speed) {
            this.name = name;
            this.speed = speed;
        }

        public String name() {
            return name;
        }

        public float speed() {
            return speed;
        }
    }

    private static final class StoredProfiles {
        private int selectedProfileIndex;
        private boolean autoArmOnJoin;
        private List<StoredProfile> profiles;
    }

    private static final class StoredProfile {
        private String name;
        private float speed;
    }
}
