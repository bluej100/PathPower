package io.github.bluej100.pathpower;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class PathPower extends JavaPlugin {
  public HashMap<String, Path> paths = new HashMap<String, Path>();
  
  @Override
  public void onEnable() {
    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
        @Override
        public void run() {
            for (Map.Entry<String, Path> entry: paths.entrySet()) {
              Player player = getPlayer(entry.getKey());
              if (player == null) return;              
              float energy = getEnergy(player);
              Path path = entry.getValue();
              player.setExp(path.lastEnergy - energy);
              path.lastEnergy = energy;
            }
        }
    }, 0L, 20L);
  }
  
  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    String cmdName = cmd.getName().toLowerCase();
    if (!Arrays.asList("pathstart", "pathname", "pathend").contains(cmdName)) return false;
    Player player;
    if (sender instanceof Player) {
      player = (Player)sender;
    } else {
      if (args.length == 0) {
        getLogger().warning("No player specified");
        return false;
      }
      player = getPlayer(args[args.length-1]);
      if (player == null) {
        getLogger().warning("Unknown player specified");
        return false;
      }
    }
    String playerName = player.getName();
    float energy = getEnergy(player);
    long time = System.currentTimeMillis();
    switch(cmdName) {
    case "pathstart":
      Path path = new Path();
      path.name = "";
      path.startEnergy = energy;
      path.lastEnergy = energy;
      path.startTime = time;
      paths.put(playerName, path);
      player.sendMessage("Path started!");
      break;
    case "pathname":
      path = paths.get(playerName);
      if (path == null) return false;
      path.name = args[0];
      break;
    case "pathend":
      path = paths.get(playerName);
      if (path == null) return false;
      paths.remove(playerName);
      long timeUsed = time - path.startTime;
      String humanTime = String.format("%.1f", timeUsed / 1000f)+"s";
      float energyUsed = path.startEnergy - energy;
      player.sendMessage("Path "+path.name+" complete in "+humanTime+"!");
      player.sendMessage("Energy used: "+energyUsed);      
      break;
    }
    return true;
  }

  @SuppressWarnings("deprecation")
  private Player getPlayer(String name) {
    return Bukkit.getPlayer(name);
  }
  
  private float getEnergy(Player player) {
    return (player.getFoodLevel() + player.getSaturation()) * 4 - player.getExhaustion();
  }
  
  private class Path {
    public String name;
    public float startEnergy;
    public float lastEnergy;
    public long startTime;
  }
}
