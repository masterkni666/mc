package com.tynker.mc;

import java.util.Arrays;
import org.bukkit.event.EventHandler;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;


public class GameManager extends JavaPlugin implements Listener{
   
    private int gameTimer = 10;
    private int playersPerLot = 2;
    private List<Player> waitingList;
    private Boolean[] worldList;
    private BossBar[] barList;
    private BossBar lobbyBar;
    private WorldCreator[] worldCreators;
    private int borderSize = 40;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        //games = new Game[3];
        //BossBar thisBossBar;
        //WorldCreator thisWorldCreator;
        //for(int c = 0;c < games.length;c++){
        //    thisBossBar = Bukkit.getServer().createBossBar("Counting down: 5.", org.bukkit.boss.BarColor.YELLOW, org.bukkit.boss.BarStyle.SOLID);
        //    thisWorldCreator = new WorldCreator("world"+i);
        //    Bukkit.createWorld(thisWorldCreator);
        //    games[i] = new Game(thisBossBar, thisWorldCreator);
        //}

        //setting up lobby wrold
        World world = Bukkit.getWorld("world");
        world.setSpawnFlags(false,false);
        world.setPVP(false);
        buildRoom(24, world.getBlockAt(0,125,0).getLocation());

        //setting up world list
        worldList = new Boolean[3];
        Arrays.fill(worldList, false);
        worldCreators = new WorldCreator[worldList.length];

        //setting up lobby promo message
        lobbyBar = Bukkit.getServer().createBossBar("Please wait for game to start. Waiting for " + playersPerLot + "  more players.", org.bukkit.boss.BarColor.PURPLE, org.bukkit.boss.BarStyle.SEGMENTED_6);
        lobbyBar.setProgress(0.0);

        //setting up game message bar
        barList = new BossBar[worldList.length];

        //initializing everything
        waitingList = new ArrayList<Player>();
        WorldBorder thisWorldBorder;
        //World thisWorld;
        for(int i = 0;i < worldList.length;i++){
            worldCreators[i] = new WorldCreator("world"+i).type(org.bukkit.WorldType.FLAT).generateStructures(false).seed(0);//.generatorSettings("{\"seaLevel\":0}");
            //thisWorld = Bukkit.createWorld(worldCreators[i]);
            thisWorldBorder = Bukkit.createWorld(worldCreators[i]).getWorldBorder();
            thisWorldBorder.setCenter(0,0);
            thisWorldBorder.setSize(borderSize);
            Bukkit.unloadWorld("world"+i,true);
            Bukkit.createWorld(worldCreators[i]).setSpawnFlags(false, false);
            barList[i] = Bukkit.getServer().createBossBar("Get Ready!", org.bukkit.boss.BarColor.YELLOW, org.bukkit.boss.BarStyle.SOLID);
        }

        //setting up interval for launching game
        final org.bukkit.plugin.Plugin thisPlugin = this;
        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){

                world.setTime(0);
                world.setStorm(false);
                world.setThundering(false);

                if(waitingList.size() >= playersPerLot){
                    int lotNumber = getFreeLot();

                    if(lotNumber >= 0){
                        World targetWorld;
                        Player player;
                        BossBar thisBar;

                        lobbyBar.removeAll();

                        while(waitingList.size() >= playersPerLot && lotNumber >= 0){
                            synchronized(worldList){
                                worldList[lotNumber] = true;
                            }
                            targetWorld = Bukkit.getWorld("world"+lotNumber);
                            targetWorld.setTime(0);
                            thisBar = barList[lotNumber];

                            System.out.println("launching game in "+ lotNumber + " " + targetWorld);

                            synchronized(waitingList){
                                for(int c = 0;c < playersPerLot;c++){
                                    player = waitingList.remove(0);
                                    player.setCustomName("gaming");
                                    player.teleport(targetWorld.getHighestBlockAt(0,0).getLocation());
                                    thisBar.addPlayer(player);
                                }
                            }

                            launchGame(lotNumber);
                            lotNumber = getFreeLot();
                        }

                        synchronized(waitingList){
                            lobbyBar.setProgress((double)waitingList.size()/playersPerLot);

                            waitingList.forEach(leftover->{
                                lobbyBar.addPlayer(leftover);
                            });
                        }
                    }
                }

            Bukkit.getScheduler().runTaskLater(thisPlugin, this, (long)(1000 /50));
            }

        }, (long)(1000 / 50)); 
    }

    public void buildRoom(int d, Location location){
        World world = location.getWorld();
        Block block = world.getBlockAt(location.clone().add(Math.round(-d/2),0,Math.round(-d/2)));
        int x,y,z;

        world.getEntitiesByClass(org.bukkit.entity.EnderCrystal.class).forEach(entity->entity.remove());
        world.spawnEntity(location.add(0,2,0),org.bukkit.entity.EntityType.ENDER_CRYSTAL);

        for (x = 0; x < d; x++) {
            for (z = 0; z < d; z++) {
                block.getRelative(x, 0, z).setType(org.bukkit.Material.GLASS);
            }
        }

        for (x = -1; x < d+1; x++) {
            for (y = 1; y < 3; y++) {
                block.getRelative(x, y, -1).setType(org.bukkit.Material.THIN_GLASS);
                block.getRelative(x, y, d).setType(org.bukkit.Material.THIN_GLASS);
            }
        }   

        for (z = 0; z < d; z++) {
            for (y = 1; y < 3; y++) {
                block.getRelative(-1, y, z).setType(org.bukkit.Material.IRON_FENCE);
                block.getRelative(d, y, z).setType(org.bukkit.Material.IRON_FENCE);
            }
        }
    }


    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    public void launchGame(int lotNumber){
        final int thisLotNumber = lotNumber;
        final org.bukkit.plugin.Plugin thisPlugin = this;
        final BossBar thisBar = barList[lotNumber];

        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
            @Override
            public void run(){
                thisBar.setTitle("Counting down: 5.");
                Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                    @Override
                    public void run(){
                        thisBar.setTitle("Counting down: 4.");
                        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                            @Override
                            public void run(){
                                thisBar.setTitle("Counting down: 3.");
                                Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                                    @Override
                                    public void run(){
                                        thisBar.setTitle("Counting down: 2.");
                                        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                                            @Override
                                            public void run(){
                                                thisBar.setTitle("Counting down: 1.");
                                                Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                                                    @Override
                                                    public void run(){
                                                        thisBar.setTitle("Start!");
                                                        Boolean temp = true;

                                                        for(Player player: Bukkit.getWorld("world"+thisLotNumber).getPlayers()){
                                                            player.setHealth(player.getMaxHealth());
                                                            player.getInventory().addItem(new org.bukkit.inventory.ItemStack((temp)?org.bukkit.Material.EGG:org.bukkit.Material.SNOW_BALL)); 
                                                            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD)); 
                                                            temp = !temp;
                                                        }

                                                        Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                                                            @Override
                                                            public void run(){
                                                                //thisBar.setTitle("Game Over");
                                                                endGame(thisLotNumber);
                                                            }
                                                        }, (long)(gameTimer*1000/50));
                                                    }
                                                },(long)(1000 / 50));
                                            }
                                        }, (long)(1000/50));
                                    }
                                }, (long)(1000/50));
                            }
                        }, (long)(1000/50));
                    }
                }, (long)(1000/50));
            }
        }, (long)(10000/50));
    }   

    //@EventHandler
    //public void PlayerCommand(PlayerCommandPreprocessEvent event) {
    //        if(event.getMessage().equals("/buy")){
    //        event.getPlayer().teleport(Bukkit.getWorld("world").getBlockAt(0,200,0).getLocation());
    //        Bukkit.unloadWorld("new_world", false);
    //        WorldCreator nw = new WorldCreator("new_world").environment(Environment.NORMAL).generateStructures(false).seed(0);//.generator(new WorldChunkGenerator());
    //        Bukkit.createWorld(nw);
    //        event.getPlayer().teleport(Bukkit.getWorld("new_world").getBlockAt(0,200,0).getLocation());
    //    }
    //}

    public void endGame(int lotNumber){
        final int thisLotNumber = lotNumber;
        final World thisWorld = Bukkit.getWorld("world"+lotNumber);
        final List<Player> players = thisWorld.getPlayers();        
        final org.bukkit.plugin.Plugin thisPlugin = this;

        players.forEach(player->player.setCustomName(null));
        calculateScore(lotNumber);

        Bukkit.getScheduler().runTaskLater(this, new Runnable(){
            @Override
            public void run(){
                BossBar thisBar = barList[lotNumber];
                thisBar.removeAll();
                thisBar.setTitle("Get Ready!");

                players.forEach(player->{
                    player.getInventory().clear();
                    player.setHealth(player.getMaxHealth());
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.teleport(Bukkit.getWorld("world").getBlockAt(0,126,-5).getLocation());
                });

                Bukkit.getScheduler().runTaskLater(thisPlugin, new Runnable(){
                    @Override
                    public void run(){
                        Bukkit.unloadWorld(thisWorld, false);
                        Bukkit.createWorld(worldCreators[thisLotNumber]).setSpawnFlags(false, false);
                        //Bukkit.createWorld(new WorldCreator("world"+thisLotNumber));
                        synchronized(worldList){
                            worldList[thisLotNumber] = false;
                        }                   
                    }
                }, (long)(1000/50));
            }
        }, (long)(10000/50));
    }

    public int getFreeLot(){
        int i;

        synchronized(worldList){
            for(i = 0; i < worldList.length; i++){
                if(worldList[i] == false){
                    return i;
                } 
            }
        }

        return -1;
    }

    @EventHandler
    public void PlayerJoin(PlayerJoinEvent event) {
	    Player player = event.getPlayer();
        player.setCustomName(null);
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        player.teleport(Bukkit.getWorld("world").getBlockAt(0,126,-5).getLocation());
    }

    @EventHandler
    public void FoodLevelChange(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void PlayerDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        Player player = event.getEntity();
        player.setHealth(1.0);
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        player.teleport(player.getLocation().add(0,10,0));
    }

    @EventHandler
    public void PlayerQuit(PlayerQuitEvent event) {
	    Player player = event.getPlayer();
        System.out.println("waiting : " + waitingList);

        if("waiting".equals(player.getCustomName())){
            synchronized(waitingList){
                waitingList.remove(player);
            }
        }

        System.out.println("waiting : " + waitingList);
    }

    @EventHandler
    public void BlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void PlayerEggThrow(PlayerEggThrowEvent event) {
        event.setHatching(false);
    }

    @EventHandler
    public void EntityDamageEntity(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof org.bukkit.entity.EnderCrystal){
		    Player player = (Player)event.getDamager();
            if(player.getCustomName() == null){
                player.setCustomName("waiting");
                int size;
                synchronized(waitingList){
                    waitingList.add(player);
                    size = waitingList.size();
                    if(size <= playersPerLot){
                        lobbyBar.addPlayer(player);
                        lobbyBar.setProgress((double)size/playersPerLot);    
                        player.sendMessage(org.bukkit.ChatColor.AQUA + "You have joined the game. ");
                        lobbyBar.setTitle("Please wait for game to start. Waiting for " + (playersPerLot - size) + " more player(s).");    
                    } else player.sendMessage(org.bukkit.ChatColor.AQUA + "You have joined the line. " + size + ((size>1)?" persons are":" person is") +  " currently waiting.");
                }
            } else {
                player.sendMessage(org.bukkit.ChatColor.RED + "You are already in line.");
            }

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void ProjectileHit(ProjectileHitEvent event){
        Projectile projectile = event.getEntity();
        Player player = (Player)projectile.getShooter();

        if(player.getCustomName() != null){ 

            if(projectile instanceof org.bukkit.entity.Snowball){
                paint(org.bukkit.DyeColor.BLUE, projectile.getWorld().getBlockAt(projectile.getLocation()), 2);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SNOW_BALL)); 
            }else if(projectile instanceof org.bukkit.entity.Egg){
                paint(org.bukkit.DyeColor.ORANGE, projectile.getWorld().getBlockAt(projectile.getLocation()), 2);
                player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.EGG)); 
            }

        }
    }

    public void paint(org.bukkit.DyeColor color, Block block, int size){
        Block t_block;

        for (int x = (0 - size); x < size; x ++) {
            for (int y = (0 - size); y < size; y ++) {
                for (int z = (0 - size); z < size; z ++) {
                    t_block = block.getRelative(x,y,z);
                    if(!t_block.isEmpty() && !t_block.isLiquid()){
                        t_block.setType(org.bukkit.Material.WOOL);
                        org.bukkit.block.BlockState b_state = t_block.getState();
                        ((org.bukkit.material.Wool)b_state.getData()).setColor(color);
                        b_state.update();
                    }                
                }
            }        
        }
    }
    
    public void calculateScore(int lotNumber){
        World world = Bukkit.getWorld("world"+lotNumber);
        int halfBorder = borderSize/2;
        int seaLevel = world.getSeaLevel();
        Block block;
        int redScore = 0;
        int blueScore = 0;
        for(int x = -halfBorder;x < halfBorder;x++){
            for(int z = -halfBorder;z < halfBorder;z++){
                for(int y = 0;y < 10;y++){
                    block = world.getBlockAt(x,y,z);
                    if(block.getType() == org.bukkit.Material.WOOL){
                        org.bukkit.DyeColor color = ((org.bukkit.material.Wool)block.getState().getData()).getColor();
                        if(color == org.bukkit.DyeColor.BLUE){
                            blueScore++;
                        } else if(color == org.bukkit.DyeColor.ORANGE){
                            redScore++;
                        };
                    }
                }
            }
        }
        String endMessage = "Orange Team Score : " + redScore + ". Blue Team Score : " + blueScore + ". ";    
        if(redScore == blueScore){
            endMessage = endMessage + "Tied Game.";
        } else if(redScore > blueScore){
            endMessage = endMessage + "Orange Team Won.";
        } else {
            endMessage = endMessage + "Blue Team Won.";
        }
        barList[lotNumber].setTitle(endMessage);    
    }
}
