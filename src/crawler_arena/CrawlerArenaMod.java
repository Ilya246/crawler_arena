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
import ArenaAI.*;
import mindustry.ai.Pathfinder;
import arc.Core.*;
import mindustry.content.StatusEffects;

public class CrawlerArenaMod extends Plugin {

    public static boolean gameIsOver = true;
    public static int wave = 1;
    public static ObjectMap<String, Unit> units = new ObjectMap<>();
    public static ObjectMap<String, float[]> money = new ObjectMap<>();
    public static Seq<UnitType> upgradeableUnits = new Seq<>();
    public static ObjectMap<String, UnitType> upgradeableUnitNames = new ObjectMap<>();
    public static Seq<int[]> unitCostsBase = new Seq<>();
    public static ObjectMap<UnitType, int[]> unitCosts = new ObjectMap<>();
    public static boolean waveIsOver = false;
    public static String unitNames = "";
    public static int worldWidth;
    public static int worldHeight;
    public static int worldCenterX;
    public static int worldCenterY;
    public static boolean firstWaveLaunched = false;
    public static float timer = 0f;

    @Override
    public void init(){
        unitCostsBase.addAll(new int[]{200}, new int[]{200}, new int[]{325}, new int[]{75}, new int[]{400}, new int[]{1500}, new int[]{2750}, new int[]{1500}, new int[]{3000}, new int[]{1500}, new int[]{2500}, new int[]{12000}, new int[]{15000}, new int[]{30000}, new int[]{30000}, new int[]{30000}, new int[]{40000}, new int[]{175000}, new int[]{250000}, new int[]{325000}, new int[]{250000}, new int[]{1500000});
        upgradeableUnits.addAll(UnitTypes.mace, UnitTypes.atrax, UnitTypes.pulsar, UnitTypes.flare, UnitTypes.risso, UnitTypes.fortress, UnitTypes.quasar, UnitTypes.spiroct, UnitTypes.zenith, UnitTypes.mega, UnitTypes.crawler, UnitTypes.quad, UnitTypes.vela, UnitTypes.scepter, UnitTypes.antumbra, UnitTypes.arkyid, UnitTypes.sei, UnitTypes.eclipse, UnitTypes.reign, UnitTypes.toxopid, UnitTypes.corvus, UnitTypes.omura);
        upgradeableUnits.each(u -> {
            unitCosts.put(u, new int[]{unitCostsBase.get(0)[0]});
            upgradeableUnitNames.put(u.name, u);
            unitCostsBase.remove(0);
        });
        upgradeableUnits.each(u -> {
            unitNames += u.name + " " + unitCosts.get(u)[0] + ", ";
        });

        UnitTypes.crawler.maxRange = 8000;
        UnitTypes.omura.flying = true;
        UnitTypes.omura.health = 35000;
        UnitTypes.omura.armor = 20;
        UnitTypes.risso.flying = true;
        UnitTypes.sei.flying = true;
        UnitTypes.fortress.health = 1200;
        UnitTypes.crawler.defaultController = ArenaAI::new;
        UnitTypes.fortress.armor = 20;
        UnitTypes.atrax.defaultController = ArenaAI::new;
        UnitTypes.atrax.maxRange = 8000;
        UnitTypes.spiroct.defaultController = ArenaAI::new;
        UnitTypes.spiroct.maxRange = 8000;
        UnitTypes.arkyid.defaultController = ArenaAI::new;
        UnitTypes.arkyid.maxRange = 8000;
        UnitTypes.toxopid.defaultController = ArenaAI::new;
        UnitTypes.toxopid.maxRange = 8000;

        Events.on(WorldLoadEvent.class, e -> {
            if(Team.sharded.core() != null){
                arc.Core.app.post(() -> {Team.sharded.cores().each(c -> {c.kill();});});
            };
            content.blocks().each(b -> {
                state.rules.bannedBlocks.add(b);
            });
            arc.Core.app.post(() -> {state.rules.canGameOver = false; state.rules.waveTimer = false; state.rules.waves = true; state.rules.unitCap = 100; Call.setRules(state.rules);});
            worldWidth = world.width() * 8;
            worldHeight = world.height() * 8;
            worldCenterX = worldWidth / 2;
            worldCenterY = worldHeight / 2;
            firstWaveLaunched = false;
            timer = 0f;
            newGame();
        });
        Events.on(PlayerJoin.class, e -> {
            if(!units.containsKey(e.player.uuid()) || !money.containsKey(e.player.uuid())){
                money.put(e.player.uuid(), new float[]{Mathf.pow(2.71f, 1f + wave / 2 + Mathf.pow(wave, 2) / 4000f) * 7f});
                Unit spawnUnit = UnitTypes.dagger.spawn(worldCenterX, worldCenterY);
                spawnUnit.add();
                units.put(e.player.uuid(), spawnUnit);
                e.player.unit(spawnUnit);
                e.player.sendMessage("[cyan]Welcome to Crawler Arena. Consider using /info to view key information. Upgrading to a unit will spawn you at the bottomleft of the map as that unit.");
            }else{
                e.player.sendMessage("[cyan]Seems like you have played in this match before. Your unit and money have been restored.");
                Unit oldUnit = units.get(e.player.uuid());
                if(oldUnit.health > 0f){
                    if(findPlayer(oldUnit) != null){
                        Player invader = findPlayer(oldUnit);
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

        Events.run(Trigger.update, () -> {
            boolean doGameOver = !Groups.unit.contains(u -> {return u.team == Team.sharded;});
            if(gameIsOver){
                return;
            } else if(doGameOver){
                Call.sendMessage("[red]You have lost.");
                gameIsOver = true;
                Timer.schedule(() -> {Events.fire(new GameOverEvent(Team.crux));}, 2);
                Groups.unit.each(u -> {u.kill();});
                return;
            };
            timer += Time.delta / 60;
            if(Mathf.chance(1 / 3000 * Time.delta)){
                Call.sendMessage("[cyan]Do /info to view info about upgrading.");
            };
            if(!Groups.unit.contains(u -> {return u.team == Team.crux;}) && !waveIsOver && !gameIsOver){
                Call.sendMessage("[red]Next wave in 10 seconds.");
                respawnPlayers();
                Timer.schedule(() -> {nextWave();}, 10);
                waveIsOver = true;
                money.each((p, m) -> {m[0] += Mathf.pow(2.71f, 1f + wave / 2 + Mathf.pow(wave, 2) / 4000f) * 5f;});
            };
            Groups.player.each(p -> {
                try{
                    Call.setHudText(p.con, "Money: " + String.valueOf(Mathf.round(money.get(p.uuid())[0])));
                }catch(Exception why){
                    rewritePlayers();
                };
            });
        });
    }

    public void newGame(){
        if(firstWaveLaunched){
            return;
        };
        if(Groups.player.size() == 0){
            Timer.schedule(() -> {newGame();}, 1);
            gameIsOver = true;
            return;
        };
        state.wave = 1;
        wave = 1;
        Timer.schedule(()->{gameIsOver = false;}, 1);
        UnitTypes.crawler.speed = 0.43f;
        UnitTypes.crawler.health = 60;
        units.clear();
        money.clear();
        setupUnits();
        respawnPlayers();
        Call.sendMessage("[red]First wave in 15 seconds.");
        Timer.schedule(() -> {nextWave();}, 15);
        firstWaveLaunched = true;
    }

    public void setupUnits(){
        Groups.player.each(p -> {
            p.unit().kill();
            units.put(p.uuid(), UnitTypes.dagger.create(Team.sharded));
            money.put(p.uuid(), new float[]{10f});
        });
    }

    public void rewritePlayers(){
        Groups.player.each(p -> {
            if(!units.containsKey(p.uuid())){
                units.put(p.uuid(), UnitTypes.dagger.create(Team.sharded));
            };
            if(!money.containsKey(p.uuid())){
                money.put(p.uuid(), new float[]{Mathf.pow(2.71f, 1f + wave / 2 + Mathf.pow(wave, 2) / 4000f) * 7f});
            };
          });
    }

    public void respawnPlayers(){
        Groups.unit.each(u -> {
            if(findPlayer(u) == null){
                u.kill();
            };
        });
        Groups.player.each(p -> {
            Unit playerUnit = units.get(p.uuid());
            if(playerUnit == null){
                rewritePlayers();
                playerUnit = units.get(p.uuid());
            };
            if(p.unit().type == null){
                int sX;
                int sY;
                do{
                    sX = worldCenterX + Mathf.random(-80, 80);
                    sY = worldCenterY + Mathf.random(-80, 80);
                }while(world.tile(sX / 8, sY / 8).solid());
                playerUnit.set(sX, sY);
                if(playerUnit.type.flying){
                    playerUnit.elevation = 1;
                };
                playerUnit.add();
                p.unit(playerUnit);
            };
        });
        Groups.unit.each(u -> {
            u.heal();
        });
    }

    public Player findPlayer(Unit u){
        return Groups.player.find(p -> {
            return p.unit() == u;
        });
    }

    public void nextWave(){
        wave++;
        Timer.schedule(()->{waveIsOver = false;}, 1);
        state.wave = wave;
        UnitTypes.crawler.health += 1 * wave;
        UnitTypes.crawler.speed += 0.003 * wave;
        float crawlers = Mathf.pow(2.71f, 1f + wave / 2) * Groups.player.size() / 20;
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
                UnitTypes.reign.speed = 3;
                Unit u = UnitTypes.reign.spawn(Team.crux, 32, 32);
                u.apply(StatusEffects.boss);
                u.health = 15000 * Groups.player.size();
                u.armor = 0;
                u.abilities.add(new UnitSpawnAbility(UnitTypes.scepter, 60, 0, -32));
                break;
            case(25):
                Call.sendMessage("[green]Victory in" + String.valueOf(timer) + "seconds.");
                gameIsOver = true;
                Timer.schedule(() -> {Events.fire(new GameOverEvent(Team.sharded));}, 2);
            default:
                break;
        };
        Call.worldDataBegin();
        Groups.player.each(p -> {netServer.sendWorldData(p);});
        int atraxes = 0;
        int spirocts = 0;
        int arkyids = 0;
        int toxopids = 0;
        int spawnX = worldWidth - 32;
        int spawnY = worldHeight - 32;
        int spreadX = worldCenterX - 160 - wave * 3;
        int spreadY = worldCenterY - 160 - wave * 3;
        if(crawlers >= 100){
            atraxes = Mathf.floor(Math.min(crawlers / 10f, 400));
            crawlers -= atraxes;
        };
        if(crawlers >= 400){
            spirocts = Mathf.floor(Math.min(crawlers / 50f, 200));
            crawlers -= spirocts;
        };
        if(crawlers >= 30000){
            toxopids = Mathf.floor(crawlers / 30000f);
            arkyids = Mathf.floor(crawlers / 2000f);
            crawlers = 500;
        };
        if(crawlers >= 1000){
            arkyids = Mathf.floor(crawlers / (2000f + (wave - 10) * 100));
            crawlers = Math.min(crawlers, 1000);
        };
        for(int i = 0; i < crawlers; i++){
            int sx = 32;
            int sy = 32;
            int align;
            align = Mathf.floor(Mathf.random(0,4));
            switch(align){
                case 0:
                    sx = spawnX;
                    sy = worldCenterY + Mathf.random(-spreadY, spreadY);
                    break;
                case 1:
                    sx = worldCenterX + Mathf.random(-spreadX, spreadX);
                    sy = spawnY;
                    break;
                case 2:
                    sx = 32;
                    sy = worldCenterY + Mathf.random(-spreadY, spreadY);
                    break;
                case 3:
                    sx = worldCenterX + Mathf.random(-spreadX, spreadX);
                    sy = 32;
                    break;
            };
            Unit u;
            UnitTypes.crawler.spawn(Team.crux, sx, sy);
            if(arkyids > 0){
                u = UnitTypes.arkyid.spawn(Team.crux, sx, sy);
                u.health = 1000;
                u.maxHealth = 1000;
                u.armor = 0;
                arkyids--;
            };
            if(spirocts > 0){
                u = UnitTypes.spiroct.spawn(Team.crux, sx, sy);
                u.health = 250;
                u.maxHealth = 250;
                u.armor = 0;
                spirocts--;
            };
            if(atraxes > 0){
                u = UnitTypes.atrax.spawn(Team.crux, sx, sy);
                u.health = 100;
                u.armor = 0;
                atraxes--;
            };
            if(toxopids > 0){
                u = UnitTypes.toxopid.spawn(Team.crux, sx, sy);
                u.health = 3000;
                u.armor = 0;
                toxopids--;
            };
        }
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
                        newUnit.health = 400;
                        newUnit.maxHealth = 400;
                        newUnit.armor = 10;
                    };
                    newUnit.add();
                    units.get(player.uuid()).kill();
                    gameIsOver = true;
                    arc.Core.app.post(() -> {
                        units.remove(player.uuid());
                        units.put(player.uuid(), newUnit);
                        player.unit(newUnit);
                        player.sendMessage("Upgrade successful.");
                        gameIsOver = false;
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
            player.sendMessage("Money is gained in equal amounts for every player after a wave ends. Money can only decrease by buying units.\nYour money is displayed at the top of the screen.\n/units - print available units and their prices\n/upgrade - upgrade to a unit type, provided you have the money\nUpgrading while you are dead respawns you\n/uinfo - view information about units that this gamemode changes");
        });
        handler.<Player>register("uinfo", "Prints information about units changed by the gamemode.", (args, player) -> {
            player.sendMessage("Crawler - spawns 1 crawler per second, 400 hp, 10 armor\nOmura - [red]find out for yourself\n[white]Fortress - 1200 health, 20 armor");
        });
    }
}
