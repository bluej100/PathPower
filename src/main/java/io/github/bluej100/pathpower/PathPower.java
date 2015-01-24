package io.github.bluej100.pathpower;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
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
  public PrintWriter writer;
  
  @Override
  public void onEnable() {
    openWriter();
    scheduleXP();
  }
  
  @Override
  public void onDisable() {
    if (writer != null) writer.close();
  }
  
  public void openWriter() {
    try {
      File dataFolder = this.getDataFolder();
      dataFolder.mkdir();
      File logFile = new File(dataFolder, "log.csv");
      boolean isNew = logFile.createNewFile();
      writer = new PrintWriter(new FileWriter(logFile, true));
      if (isNew) {
        writer.println("Date,Player,Path,Energy,Time");
      }
    } catch (IOException e) {
      getLogger().warning("Could not open log file");
    }
  }
  
  public void scheduleXP() {
    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
        @Override
        public void run() {
            for (Map.Entry<String, Path> entry: paths.entrySet()) {
              Player player = getPlayer(entry.getKey());
              if (player == null) return;              
              float energy = getEnergy(player);
              Path path = entry.getValue();
              player.setExp(Math.min(path.lastEnergy - energy, 1.0f));
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
      String humanTime = String.format("%.1f", timeUsed / 1000f);
      String humanEnergy = String.format("%.1f", path.startEnergy - energy);
      String humanDate = String.format("%tF %<tT", new Date());
      
      if (writer != null) {
        writer.println(humanDate+","+playerName+","+path.name+","+humanEnergy+","+humanTime);
      }
      player.sendMessage("Path "+path.name+" complete in "+humanTime+"s!");
      player.sendMessage("Energy used: "+humanEnergy);    
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
