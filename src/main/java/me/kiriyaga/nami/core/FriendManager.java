package me.kiriyaga.nami.core;

import com.google.gson.*;
import me.kiriyaga.nami.core.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static me.kiriyaga.nami.Nami.CHAT_MANAGER;
import static me.kiriyaga.nami.Nami.LOGGER;

public class FriendManager {

    private final ConfigManager configManager;
    private Set<String> friends = new HashSet<>();

    public FriendManager(ConfigManager configManager) {
        this.configManager = configManager;
        load();
    }

    public void load() {
        this.friends = new HashSet<>(configManager.loadFriends());
    }

    public void addFriend(String name) {
        if (name == null) return;
        if (friends.add(name.toLowerCase())) {
            configManager.saveFriends(friends);
            CHAT_MANAGER.sendPersistent(getClass().getName(), "Added friend: §7" + name);
        }
    }

    public void removeFriend(String name) {
        if (name == null) return;
        if (friends.remove(name.toLowerCase())) {
            configManager.saveFriends(friends);
            CHAT_MANAGER.sendPersistent(getClass().getName(), "Removed friend: §7" + name);
        }
    }

    public boolean isFriend(String name) {
        return configManager.isFriend(name);
    }

    public Set<String> getFriends() {
        return Collections.unmodifiableSet(friends);
    }
}
