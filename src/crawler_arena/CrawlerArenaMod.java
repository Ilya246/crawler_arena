package crawler_arena;

import arc.Events;
import static mindustry.Vars.*;
import mindustry.game.EventType.*;
import mindustry.game.Rules;
import mindustry.mod.Plugin;
import mindustry.gen.*;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.type.UnitType;
import arc.util.Timer;
import arc.util.Time;
import arc.struct.Seq;
import arc.struct.ObjectSet;
import arc.struct.ObjectMap;
import arc.struct.*;
import arc.math.Mathf;
import mindustry.mod.*;
import arc.util.CommandHandler;
import mindustry.ai.types.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import ArenaAI.*;
import ReinforcementAI.*;
import SwarmAI.*;
import mindustry.ai.Pathfinder;
import arc.Core.*;
import mindustry.content.StatusEffects;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.Block;
import mindustry.world.Tile.*;
import arc.util.Log;

public class CrawlerArenaMod extends Plugin {

    public static boolean skipGameOverCheck = true;
    public static int wave = 1;
    public static ObjectMap<String, Unit> units = new ObjectMap<>();
    public static ObjectMap<String, float[]> money = new ObjectMap<>();
    public static Seq<UnitType> upgradeableUnits = new Seq<>();
    public static ObjectMap<String, UnitType> upgradeableUnitNames = new ObjectMap<>();
    public static Seq<int[]> unitCostsBase = new Seq<>();
    public static Seq<Block> bringableBlocks = new Seq<>();
    public static Seq<int[]> bringableBlockAmounts = new Seq<>();
    public static ObjectMap<Block, int[]> aidBlockAmounts = new ObjectMap<>();
    public static ObjectMap<UnitType, int[]> unitCosts = new ObjectMap<>();
    public static boolean waveIsOver = false;
    public static String unitNames = "";
    public static int worldWidth;
    public static int worldHeight;
    public static int worldCenterX;
    public static int worldCenterY;
    public static boolean firstWaveLaunched = false;
    public static float timer = 0f;
    public static float statScaling = 1f;
    public static float polyReplicationCooldown = 400f;

    @Override
    public void init(){
        unitCostsBase.addAll(   new int[]{100}, new int[]{200}, new int[]{200},  new int[]{325},   new int[]{75},   new int[]{500},   new int[]{800},   new int[]{400},  new int[]{1500},    new int[]{2000},  new int[]{2800},  new int[]{1500},   new int[]{2000},  new int[]{1500}, new int[]{2500},   new int[]{25000}, new int[]{15000}, new int[]{18000},  new int[]{18000},   new int[]{22000}, new int[]{50000}, new int[]{30000},  new int[]{200000}, new int[]{175000}, new int[]{250000}, new int[]{325000}, new int[]{250000},    new int[]{250000}, new int[]{250000}, new int[]{1500000}, new int[]{3790000});
        upgradeableUnits.addAll(UnitTypes.nova, UnitTypes.mace, UnitTypes.atrax, UnitTypes.pulsar, UnitTypes.flare, UnitTypes.retusa, UnitTypes.oxynoe, UnitTypes.risso, UnitTypes.fortress, UnitTypes.quasar, UnitTypes.cyerce, UnitTypes.spiroct, UnitTypes.zenith, UnitTypes.mega,  UnitTypes.crawler, UnitTypes.quad,   UnitTypes.vela,   UnitTypes.scepter, UnitTypes.antumbra, UnitTypes.arkyid, UnitTypes.sei,    UnitTypes.aegires, UnitTypes.poly,   UnitTypes.eclipse, UnitTypes.reign,   UnitTypes.toxopid, UnitTypes.corvus, UnitTypes.oct,     UnitTypes.navanax, UnitTypes.omura,    UnitTypes.mono);
        upgradeableUnits.each(u -> {
            unitCosts.put(u, unitCostsBase.get(0));
            upgradeableUnitNames.put(u.name, u);
            unitCostsBase.remove(0);
        });
        bringableBlocks.addAll(Blocks.liquidSource, Blocks.swarmer, Blocks.cyclone, Blocks.tsunami, Blocks.powerSource, Blocks.lancer, Blocks.arc, Blocks.thoriumWallLarge, Blocks.mendProjector, Blocks.spectre, Blocks.overdriveDome, Blocks.forceProjector, Blocks.repairPoint, Blocks.foreshadow, Blocks.ripple);
        bringableBlockAmounts.addAll(new int[]{4}, new int[]{2}, new int[]{1}, new int[]{2}, new int[]{4}, new int[]{4}, new int[]{8}, new int[]{12}, new int[]{2}, new int[]{1}, new int[]{1}, new int[]{1}, new int[]{4}, new int[]{2}, new int[]{4});
        bringableBlocks.each(b -> {
            aidBlockAmounts.put(b, bringableBlockAmounts.get(0));
            bringableBlockAmounts.remove(0);
        });
        upgradeableUnits.each(u -> {
            unitNames += u.name + " " + unitCosts.get(u)[0] + ", ";
            u.defaultController = FlyingAI::new;
        });

        UnitTypes.omura.flying = true;
        UnitTypes.omura.health = 45000f;
        UnitTypes.omura.armor = 20;
        UnitTypes.risso.flying = true;
        UnitTypes.sei.flying = true;
        UnitTypes.retusa.flying = true;
        UnitTypes.oxynoe.flying = true;
        UnitTypes.cyerce.flying = true;
        UnitTypes.aegires.flying = true;
        UnitTypes.navanax.flying = true;
        UnitTypes.fortress.health = 1200f;
        UnitTypes.fortress.armor = 20;
        UnitTypes.crawler.defaultController = ArenaAI::new;
        UnitTypes.crawler.maxRange = 8000f;
        UnitTypes.atrax.defaultController = ArenaAI::new;
        UnitTypes.atrax.maxRange = 8000f;
        UnitTypes.spiroct.defaultController = ArenaAI::new;
        UnitTypes.spiroct.maxRange = 8000f;
        UnitTypes.arkyid.defaultController = ArenaAI::new;
        UnitTypes.arkyid.maxRange = 8000f;
        UnitTypes.toxopid.defaultController = ArenaAI::new;
        UnitTypes.toxopid.maxRange = 8000f;
        UnitTypes.mega.defaultController = ReinforcementAI::new;
        UnitTypes.poly.abilities.add(new UnitSpawnAbility(UnitTypes.poly, polyReplicationCooldown, 0f, -1f));
        UnitTypes.poly.defaultController = SwarmAI::new;
        UnitTypes.poly.maxRange = 1000f;
        UnitTypes.arkyid.weapons.each(w -> {
            if(w.bullet instanceof SapBulletType){
                SapBulletType sapBullet = (SapBulletType)w.bullet;
                sapBullet.sapStrength = 0f;
            };
        });

        Events.on(WorldLoadEvent.class, e -> {
            if(Team.sharded.core() != null){
                arc.Core.app.post(() -> {arc.Core.app.post(() -> {Team.sharded.cores().each(c -> {c.kill();});});});
            };
            arc.Core.app.post(() -> {
                content.blocks().each(b -> {
                    state.rules.bannedBlocks.add(b);
                });
                state.rules.canGameOver = false;
                state.rules.waveTimer = false;
                state.rules.waves = true;
                state.rules.unitCap = 256;
                Call.setRules(state.rules);
            });
            worldWidth = world.width() * 8;
            worldHeight = world.height() * 8;
            worldCenterX = worldWidth / 2;
            worldCenterY = worldHeight / 2;
            firstWaveLaunched = false;
            waveIsOver = true;
            timer = 0f;
            newGame();
        });

        Events.on(PlayerJoin.class, e -> {
            if(!units.containsKey(e.player.uuid()) || !money.containsKey(e.player.uuid())){
                money.put(e.player.uuid(), new float[]{Mathf.pow(2.71f, 1f + wave / 2f + Mathf.pow(wave, 2) / 4000f) * 6f});
                Unit spawnUnit = UnitTypes.dagger.create(Team.sharded);
                units.put(e.player.uuid(), spawnUnit);
                respawnPlayer(e.player);
                e.player.sendMessage("[cyan]Welcome to Crawler Arena. Consider using /info to view key information.");
            }else{
                e.player.sendMessage("[cyan]Seems like you have played in this match before. Your unit and money have been restored.");
                Unit oldUnit = units.get(e.player.uuid());
                if(oldUnit.health > 0f){
                    if(oldUnit.getPlayer() != null){
                        Player invader = oldUnit.getPlayer();
                        if(units.get(invader.uuid()) != null){
                            invader.unit(units.get(invader.uuid()));
                        };
                    }else{
                        rewritePlayers();
                    };
                    e.player.unit(oldUnit);
                };
            };
        });

        Events.on(BlockBuildBeginEvent.class, e -> {
            Building build = e.tile.build;
            if(e.breaking && build instanceof ConstructBuild){
                ConstructBuild cbuild = (ConstructBuild)build;
                Block block = cbuild.current;
                try{
                    e.tile.setNet(block, Team.sharded, 0);
                }catch(Exception ok){
                };
            };
        });

        Events.run(Trigger.update, () -> {
            boolean gameIsOver = !Groups.unit.contains(u -> {return u.team == Team.sharded;});
            if(gameIsOver && !skipGameOverCheck){
                skipGameOverCheck = true;
                if(wave < 25){
                    Call.sendMessage("[red]You have lost.");
                    Timer.schedule(() -> {Events.fire(new GameOverEvent(Team.crux));}, 2);
                }else{
                    Call.sendMessage("[yellow]It's over.");
                    Timer.schedule(() -> {Events.fire(new GameOverEvent(Team.crux));}, 2);
                };
            };
            timer += Time.delta / 60f;
            if(Mathf.chance(1f / 12000f * Time.delta)){
                Call.sendMessage("[cyan]Do /info to view info about upgrading.");
            };
            if(!Groups.unit.contains(u -> {return u.team == Team.crux;}) && !waveIsOver){
                if(wave < 8 || wave % 2 != 0){
                    Call.sendMessage("[red]Next wave in 10 seconds.");
                    Timer.schedule(() -> {nextWave();}, 10);
                    Timer.schedule(() -> {syncPlayers();}, 3);
                }else{
                    Call.sendMessage("[yellow]Next wave in " + String.valueOf(50 + wave * 3) + " seconds.");
                    Timer.schedule(() -> {spawnReinforcements();}, 2);
                    Timer.schedule(() -> {nextWave();}, 50 + wave * 3);
                };
                respawnPlayers();
                waveIsOver = true;
                money.each((p, m) -> {m[0] += Mathf.pow(2.71f, 1f + wave / 2f + Mathf.pow(wave, 2) / 4000f) * 5f;});
            };
            Groups.player.each(p -> {
                try{
                    Call.setHudText(p.con, "Money: " + String.valueOf(Mathf.round(money.get(p.uuid())[0])));
                }catch(Exception why){
                    rewritePlayers();
                };
            });
            UnitTypes.arkyid.speed += state.isPaused() || UnitTypes.arkyid.speed > 2f * statScaling ? 0f : 0.00003f * Time.delta * statScaling;
        });
        Log.info("Crawler arena loaded. Server commands: /killEnemies - kills all enemies.");
    }

    public void newGame(){
        if(firstWaveLaunched){
            return;
        };
        if(Groups.player.size() == 0){
            Timer.schedule(() -> {newGame();}, 1);
            skipGameOverCheck = true;
            return;
        };
        state.wave = 1;
        wave = 1;
        statScaling = 1f;
        Timer.schedule(()->{skipGameOverCheck = false;}, 5);
        UnitTypes.crawler.speed = 0.43f;
        UnitTypes.crawler.health = 60f;
        units.clear();
        money.clear();
        Groups.player.each(p -> {
            UnitType type = p.unit().type;
            if(type == UnitTypes.gamma || type == UnitTypes.beta || type == UnitTypes.alpha){
                p.unit().kill();
            };
        });
        rewritePlayers();
        respawnPlayers();
        Call.sendMessage("[red]First wave in 15 seconds.");
        Timer.schedule(() -> {nextWave();}, 15f);
        firstWaveLaunched = true;
        waveIsOver = true;
    }

    public void spawnReinforcements(){
        Call.sendMessage("[green]Aid package on its way.");
        Seq<Unit> megas = new Seq<>();
        ObjectMap<Block, int[]> blocks = new ObjectMap<>();
        for(int i = 0; i < Math.min(wave * 2 * statScaling, 180); i += 3){
              Unit u = UnitTypes.mega.spawn(32, worldCenterY + Mathf.random(-80, 80));
              u.health = 999999f;
              u.team = Team.derelict;
              megas.add(u);
        };
        int capacity = megas.size;
        int itemSources = Mathf.ceil(wave / 10f);
        for(int i = 0; i < itemSources; i++){blocks.put(Blocks.itemSource, new int[]{4});};
        capacity -= itemSources;
        for(int i = 0; i < capacity; i++){
            int blockID = Mathf.random(0, bringableBlocks.size - 1);
            Block block = bringableBlocks.get(blockID);
            blocks.put(block, aidBlockAmounts.get(block));
        };
        blocks.each((b, a) -> {
            for(int i = 0; i < a[0]; i++){
                Unit mega = megas.get(0);
                if(mega instanceof Payloadc){
                    Payloadc pay = (Payloadc)mega;
                    pay.addPayload(new BuildPayload(b, Team.sharded));
                };
            };
            megas.remove(0);
        });
        Timer.schedule(()->{syncPlayers();}, worldCenterX / 90);
    }

    public void rewritePlayers(){
        Groups.player.each(p -> {
            if(!units.containsKey(p.uuid())){
                units.put(p.uuid(), UnitTypes.dagger.create(Team.sharded));
            };
            if(!money.containsKey(p.uuid())){
                money.put(p.uuid(), new float[]{Mathf.pow(2.71f, 1f + wave / 2f + Mathf.pow(wave, 2) / 4000f) * 6f});
            };
          });
    }

    public void respawnPlayer(Player p){
        Unit playerUnit = units.get(p.uuid());
        if(playerUnit == null){
            rewritePlayers();
            playerUnit = units.get(p.uuid());
        };
        if(p.unit() != units.get(p.uuid()) || p.unit().type == null){
            float sX = (float)worldCenterX;
            float sY = (float)worldCenterY;
            if(world.tile((int)(sX / 8f), (int)(sY / 8f)).solid()){
                int i = 0;
                do{
                    sX = Mathf.random(32f, (float)worldWidth / 8f - 32f);
                    sY = Mathf.random(32f, (float)worldHeight / 8f - 32f);
                    i++;
                }while(world.tile((int)(sX / 8f), (int)(sY / 8f)).solid() && i < 100);
            };
            playerUnit.set(sX, sY);
            if(playerUnit.type.flying){
                playerUnit.elevation = 1f;
            }else{
                playerUnit.elevation = 0f;
            };
            playerUnit.add();
            p.unit(playerUnit);
        };
    }

    public void respawnPlayers(){
        arc.Core.app.post(() -> {
            Groups.unit.each(u -> {
                if(u.getPlayer() == null){
                    u.abilities.clear();
                    u.kill();
                };
            });
        });
        Groups.unit.each(u -> {
            if(u instanceof PayloadUnit){
                PayloadUnit pay = (PayloadUnit)u;
                for(int i = 0; i < pay.payloads.size; i++){
                    if(pay.payloads.get(i) instanceof UnitPayload){
                        pay.payloads.remove(i);
                    };
                };
            };
        });
        Groups.player.each(p -> {
            respawnPlayer(p);
        });
        Groups.unit.each(u -> {
            u.heal();
        });
    }

    public void syncPlayers(){
        Call.worldDataBegin();
        Groups.player.each(p -> {netServer.sendWorldData(p);});
    }
    public void spawnEnemy(Unit u, int spX, int spY){
        int sx = 32;
        int sy = 32;
        int align;
        align = Mathf.random(0, 3);
        switch(align){
            case 0:
                sx = worldWidth - 32;
                sy = worldCenterY + Mathf.random(-spY, spY);
                break;
            case 1:
                sx = worldCenterX + Mathf.random(-spX, spX);
                sy = worldHeight - 32;
                break;
            case 2:
                sx = 32;
                sy = worldCenterY + Mathf.random(-spY, spY);
                break;
            case 3:
                sx = worldCenterX + Mathf.random(-spX, spX);
                sy = 32;
                break;
        };
        u.set(sx, sy);
        u.add();
    }

    public void nextWave(){
        wave++;
        Timer.schedule(()->{waveIsOver = false;}, 1);
        state.wave = wave;
        float crawlers = Mathf.pow(2.71f, 1f + wave / 2f + Mathf.pow(wave, 2f) / 150f) * Groups.player.size() / 20f;
        switch(wave){
            case(21):
                Call.sendMessage("[red]What makes you live for this long?");
                break;
            case(23):
                Call.sendMessage("[red]Why are you still alive?");
                break;
            case(24):
                Call.sendMessage("[#FF0000]Observation is no longer prohibited. It comes.");
                crawlers = 0;
                UnitTypes.reign.maxRange = 8000;
                UnitTypes.reign.defaultController = ArenaAI::new;
                UnitTypes.reign.speed = 3f;
                UnitTypes.scepter.defaultController = ArenaAI::new;
                Unit u = UnitTypes.reign.spawn(Team.crux, 32, 32);
                u.apply(StatusEffects.boss);
                u.health = 5000 * Math.max(Groups.player.size(), 4);
                u.armor = 0;
                u.abilities.add(new UnitSpawnAbility(UnitTypes.scepter, 1800 / Math.max(Groups.player.size(), 1), 0, -32));
                break;
            case(25):
                UnitTypes.reign.speed = 0.35f;
                Call.sendMessage("[green]Victory has been achieved in " + String.valueOf(timer) + " seconds." + "\n" + "[#FF]And now, survive this.");

            default:
                break;
        };
        if(wave > 24 && crawlers > 8800000f){
              crawlers = 8800000f + Math.min(Mathf.pow(crawlers, 0.6f), 3200000f);
              statScaling = 1f + ((float)wave - 24f) / 2f;
        };
        UnitTypes.arkyid.speed = 0.6f * statScaling;
        UnitTypes.crawler.health += 1 * wave * statScaling;
        UnitTypes.crawler.speed += 0.003f * wave * statScaling;
        Ability abil = UnitTypes.poly.abilities.get(1);
        if(abil instanceof UnitSpawnAbility){
            UnitSpawnAbility sability = (UnitSpawnAbility)abil;
            sability.spawnTime = polyReplicationCooldown / statScaling;
        }
        int atraxes = 0;
        int spirocts = 0;
        int arkyids = 0;
        int toxopids = 0;
        int spreadX = Math.max(worldCenterX - 160 - wave * 10, 160);
        int spreadY = Math.max(worldCenterY - 160 - wave * 10, 160);
        if(crawlers >= 100){
            atraxes = Mathf.floor(Math.min(crawlers / 10f, 400f));
            crawlers -= atraxes * 2f;
        };
        if(crawlers >= 400){
            spirocts = Mathf.floor(Math.min(crawlers / 50f, 200f));
            crawlers -= spirocts * 10f;
        };
        if(crawlers >= 30000){
            toxopids = Mathf.floor(crawlers / 30000f);
            crawlers -= toxopids * 8000f;
            arkyids = Mathf.floor(crawlers / 2000f);
            crawlers = Math.min(crawlers, 500f);
        };
        if(crawlers >= 1000){
            arkyids = Mathf.floor(crawlers / 2000f);
            crawlers = Math.min(crawlers, 1000f);
        };
        Unit u;
        while(crawlers > 0f){
            u = UnitTypes.crawler.create(Team.crux);
            spawnEnemy(u, spreadX, spreadY);
            crawlers--;
        };
        while(atraxes > 0){
            u = UnitTypes.atrax.create(Team.crux);
            u.health = 100f * statScaling;
            u.maxHealth = 100f * statScaling;
            u.armor = 0f;
            spawnEnemy(u, spreadX, spreadY);
            atraxes--;
        };
        while(spirocts > 0){
            u = UnitTypes.spiroct.create(Team.crux);
            u.health = 250f * statScaling;
            u.maxHealth = 250f * statScaling;
            u.armor = 0f * statScaling;
            spawnEnemy(u, spreadX, spreadY);
            spirocts--;
        };
        while(arkyids > 0){
            u = UnitTypes.arkyid.create(Team.crux);
            u.health = 1000f * statScaling;
            u.maxHealth = 1000f * statScaling;
            u.armor = 0f;
            spawnEnemy(u, spreadX, spreadY);
            arkyids--;
        };
        while(toxopids > 0){
            u = UnitTypes.toxopid.create(Team.crux);
            u.health = 3000f * statScaling;
            u.maxHealth = 3000f * statScaling;
            u.armor = 0f;
            spawnEnemy(u, spreadX, spreadY);
            toxopids--;
        };
    }
    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("upgrade", "<type>", "Upgrades your unit type.", (args, player) -> {
            if(upgradeableUnitNames.containsKey(args[0])){
                UnitType newUnitType = upgradeableUnitNames.get(args[0]);
                if(money.get(player.uuid())[0] >= unitCosts.get(newUnitType)[0]){
                    money.get(player.uuid())[0] -= unitCosts.get(newUnitType)[0];
                    Unit newUnit = newUnitType.spawn(player.x, player.y);
                    if(newUnit.type == UnitTypes.crawler){
                        newUnit.abilities.add(new UnitSpawnAbility(UnitTypes.crawler, 60f, 0f, -8f));
                        newUnit.health = 400f;
                        newUnit.maxHealth = 400f;
                        newUnit.armor = 10;
                    }else if(newUnit.type == UnitTypes.mono){
                        newUnit.health = 100000f;
                        newUnit.maxHealth = 100000f;
                        newUnit.armor = 10;
                        newUnit.abilities.add(new UnitSpawnAbility(UnitTypes.omura, 30f, 0f, -8f));
                    };
                    newUnit.controller(new FlyingAI());
                    newUnit.add();
                    units.get(player.uuid()).kill();
                    skipGameOverCheck = true;
                    arc.Core.app.post(() -> {
                        units.remove(player.uuid());
                        units.put(player.uuid(), newUnit);
                        player.unit(newUnit);
                        player.sendMessage("Upgrade successful.");
                        skipGameOverCheck = false;
                    });
                }else{
                    player.sendMessage("Not enough money.");
                };
            }else{
                player.sendMessage("You can't upgrade to that unit type. Note that unit names are case-sensitive.");
            };
        });
        handler.<Player>register("units", "Prints all unit types.", (args, player) -> {
            player.sendMessage(unitNames);
        });
        handler.<Player>register("info", "Prints info about the gamemode", (args, player) -> {
            player.sendMessage("Money is gained in equal amounts for every player after a wave ends. Money can only decrease by buying units.\nYour money is displayed at the top of the screen.\n/units - print available units and their prices\n/upgrade - upgrade to a unit type, provided you have the money\nUpgrading while you are dead respawns you");
        });
    }

    public void registerServerCommands(CommandHandler handler){
        handler.register("killEnemies", "Kills all enemies in the current wave.", (args) -> {
            Groups.unit.each(u -> {
                if(u.team == Team.crux){
                    u.kill();
                };
            });
        });
    }
}
