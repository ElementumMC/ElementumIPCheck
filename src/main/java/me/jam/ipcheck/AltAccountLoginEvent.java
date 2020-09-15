package me.jam.ipcheck;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;

public class AltAccountLoginEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private UUID uniqueId;
    private String name;
    private InetAddress address;
    private PlayerLoginEvent.Result loginResult;
    private List<String> usernames;

    public AltAccountLoginEvent(UUID uuid, String name, InetAddress address, PlayerLoginEvent.Result result, List<String> names) {
        super();
        this.uniqueId = uuid;
        this.name = name;
        this.address = address;
        this.loginResult = result;
        this.usernames = names;
    }

    public InetAddress getAddress() {
        return address;
    }

    public PlayerLoginEvent.Result getLoginResult() {
        return loginResult;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public String getName() {
        return name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getEventName() {
        return "AltAccountLoginEvent";
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
