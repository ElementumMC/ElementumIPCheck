package me.jam.ipcheck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EventsManager implements Listener {
    private DatabaseManager db = IPCheck.plugin.getDatabase();

    private final List<Set<UUID>> toNotify = new LinkedList<>();

    @EventHandler
    public void onJoin(PlayerLoginEvent event){
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String ip = event.getAddress().getHostAddress();
                    Player player = event.getPlayer();
                    String uuid = player.getUniqueId().toString();
                    String name = player.getName();
                    UUID evtUUID = player.getUniqueId();

                    ResultSet rs = db.query("SELECT * FROM log WHERE IP = ? AND UUID != ?", ip, uuid);

                    List<String> names = new LinkedList<>();
                    while (rs.next()) {
                        UUID uniqueId = UUID.fromString(rs.getString("UUID"));
                        names.add(Bukkit.getOfflinePlayer(uniqueId).getName());

                        boolean found = false;
                        for (Set<UUID> s : toNotify) {
                            if (s.contains(uniqueId)) {
                                s.add(evtUUID);
                                found = true;
                            } else if (s.contains(evtUUID)) {
                                s.add(uniqueId);
                                found = true;
                            }
                        }
                        if (found) continue;
                        if (!player.hasPlayedBefore()) {
                            HashSet<UUID> set = new HashSet<>();
                            set.add(evtUUID);
                            set.add(uniqueId);

                            toNotify.add(set);
                        }
                    }
                    if (!player.hasPlayedBefore() && names.size() > 0) Bukkit.broadcast(
                            IPCheck.prefix + ChatColor.YELLOW + name + ChatColor.GRAY + " has alts of "
                                    + ChatColor.YELLOW + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, names),
                            "ipcheck.notify");

                    if (!db.query("SELECT * FROM log WHERE username = ? AND IP = ? AND UUID = ?",
                            name, ip, uuid).next()) {
                        db.exec("INSERT INTO log(UUID,username,IP,seen) VALUES(?,?,?,date('now'))",
                                uuid, name, ip);
                    }

                    if (names.size() > 0) Bukkit.getScheduler().runTask(IPCheck.plugin, () ->
                            Bukkit.getPluginManager().callEvent(new AltAccountLoginEvent(evtUUID, name, event.getAddress(), event.getResult(), names))
                    );
                } catch(SQLException e){
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(IPCheck.plugin);
    }

    @EventHandler
    public void onStaffJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        if(player.hasPermission("ipcheck.notify")) {
            for(Set<UUID> set : toNotify) {
                LinkedList<String> list = new LinkedList<>();
                for(UUID uuid : set) list.add(Bukkit.getOfflinePlayer(uuid).getName());

                player.sendMessage(
                        IPCheck.prefix + ChatColor.YELLOW + list.get(0) + ChatColor.GRAY + " has alts of: " + ChatColor.YELLOW
                                + String.join(ChatColor.GRAY + ", " + ChatColor.YELLOW, list.subList(1, list.size()))
                );
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();

        //add them to the DB if they are not in it, if they are update seen
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (!db.query("SELECT * FROM log WHERE username = ? AND IP = ? AND UUID = ?",
                            player.getName(), player.getAddress().getAddress().getHostAddress(), player.getUniqueId().toString()).next()) {
                        db.exec("INSERT INTO log(UUID,username,IP,seen) VALUES(?,?,?,date('now'))",
                                player.getUniqueId().toString(), player.getName(), player.getAddress().toString());
                    } else db.exec("UPDATE log SET seen = date('now') WHERE username = ? AND IP = ? AND UUID = ?",
                            player.getName(), player.getAddress().getAddress().getHostAddress(), player.getUniqueId().toString());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(IPCheck.plugin);
    }
}
