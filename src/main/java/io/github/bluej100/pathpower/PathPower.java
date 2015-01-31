package io.github.bluej100.pathpower;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Wool;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class PathPower extends JavaPlugin {
  public Map<String, Path> paths = new HashMap<String, Path>();
  public static final String TOTAL_FILE = "totals.csv";
  public static final String TICK_FILE = "ticks.csv";
  
  @Override
  public void onEnable() {
    writeHeaders();
    scheduleMonitor();
  }
  
  public PrintWriter getTotalWriter() {
    return getPathWriter(TOTAL_FILE);
  }
  
  public PrintWriter getTickWriter() {
    return getPathWriter(TICK_FILE);
  }
  
  public PrintWriter getPathWriter(String name) {
    File logFile = new File(this.getDataFolder(), name);
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return writer;
  }
  
  public void writeHeaders() {
    try {
      File dataFolder = this.getDataFolder();
      dataFolder.mkdir();
      File totalFile = new File(dataFolder, TOTAL_FILE);
      if (totalFile.createNewFile()) {
        PrintWriter writer = getTotalWriter();
        writer.println("Date,Player,Path,Distance,Energy,Time");
        writer.close();
      }
      File tickFile = new File(dataFolder, TICK_FILE);
      if (tickFile.createNewFile()) {
        PrintWriter writer = getTickWriter();
        writer.println("Date,Player,Path,X,Y,Z,Power");
        writer.close();
      }
    } catch (IOException e) {
      getLogger().warning("Could not open log file");
    }
  }
  
  public void scheduleMonitor() {
    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
    scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
        @Override
        public void run() {
            for (Map.Entry<String, Path> entry: paths.entrySet()) {
              String playerName = entry.getKey();
              Path path = entry.getValue();
              Player player = getPlayer(playerName);
              if (player == null) return;
              
              float energy = getEnergy(player);
              float power = path.lastEnergy - energy;
              player.setExp(Math.min(power, 1.0f));
              path.lastEnergy = energy;
              path.updateLocation(player);
              
              Location l = player.getLocation();
              String humanDate = String.format("%tF %<tT", new Date());
              String humanPosition = String.format("%.1f,%.1f,%.1f", l.getX(), l.getY(), l.getZ());
              String humanPower = String.format("%.2f", power);
              
              PrintWriter writer = getTickWriter();
              writer.println(humanDate+","+playerName+","+path.name+","+humanPosition+","+humanPower);
              writer.close();
              
              createSpeedBlock(l, power);
            }
        }
    }, 0L, 20L);
  }
  
  public void createSpeedBlock(Location l, float power) {
      Block floor = l.add(0D,-1D,0D).getBlock();
      if (!floor.isEmpty()) {
        floor.setType(Material.WOOL);
        BlockState bs = floor.getState();
        Wool wool = (Wool) bs.getData();
        DyeColor color = DyeColor.LIME;
        if (power > 0.2) {
          color = DyeColor.YELLOW;
        }
        if (power > 0.4) {
          color = DyeColor.RED;
        }
        wool.setColor(color);
        bs.update();
      }
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
      path.updateLocation(player);
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
      path.updateLocation(player);
      paths.remove(playerName);
      long timeUsed = time - path.startTime;
      
      String humanDate = String.format("%tF %<tT", new Date());
      String humanDistance = String.format("%.1f", path.distance);
      String humanEnergy = String.format("%.1f", path.startEnergy - energy);
      String humanTime = String.format("%.1f", timeUsed / 1000f);
      
      PrintWriter writer = getTotalWriter();
      writer.println(humanDate+","+playerName+","+path.name+","+humanDistance+","+humanEnergy+","+humanTime);
      writer.close();
      player.sendMessage("Path "+path.name+" ("+humanDistance+"m) complete in "+humanTime+"s!");
      player.sendMessage("Energy used: "+humanEnergy+" calories");
      break;
    }
    return true;
  }

  @SuppressWarnings("deprecation")
  private Player getPlayer(String name) {
    return Bukkit.getPlayer(name);
  }
  
  private float getEnergy(Player player) {
    int saturation = (int)Math.ceil(player.getSaturation());
    return (player.getFoodLevel() + saturation) * 4 - player.getExhaustion();
  }
  
  private class Path {
    public String name;
    public float startEnergy;
    public float lastEnergy;
    public long startTime;
    public Location lastLocation;
    public float distance;
    
    public void updateLocation(Player player) {
      Location location = player.getLocation();
      if (lastLocation == null) {
        distance = 0f;
      } else {
        distance += location.distance(lastLocation);
      }
      lastLocation = location;
    }
  }
}
