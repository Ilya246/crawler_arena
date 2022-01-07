package crawler_arena;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.types.FlyingAI;
import mindustry.content.Blocks;
import mindustry.content.StatusEffects;
import mindustry.content.UnitTypes;
import mindustry.entities.abilities.UnitSpawnAbility;
import mindustry.entities.bullet.SapBulletType;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.payloads.BuildPayload;

import static mindustry.Vars.*;
import static crawler_arena.CVars.*;

public class CrawlerArenaMod extends Plugin {
    public static boolean gameIsOver = true, waveIsOver = false, firstWaveLaunched = false;

    public static int worldWidth, worldHeight, worldCenterX, worldCenterY, wave = 1;
    public static float statScaling = 1f;

    public static ObjectIntMap<UnitType> unitCosts = new ObjectIntMap<>();
    public static ObjectIntMap<String> money = new ObjectIntMap<>();
    public static ObjectMap<String, UnitType> units = new ObjectMap<>();
    public static ObjectIntMap<String> unitIDs = new ObjectIntMap<>();
    public static ObjectIntMap<Block> aidBlockAmounts = new ObjectIntMap<>();

    public static long timer = Time.millis();

    @Override
    public void init(){
        unitCosts.putAll(UnitTypes.nova, 100,
        UnitTypes.pulsar, 300,
        UnitTypes.quasar, 2000,
        UnitTypes.vela, 15000,
        UnitTypes.corvus, 250000,

        UnitTypes.dagger, 25,
        UnitTypes.mace, 200,
        UnitTypes.fortress, 1500,
        UnitTypes.scepter, 20000,
        UnitTypes.reign, 250000,

        UnitTypes.crawler, 7500,
        UnitTypes.atrax, 250,
        UnitTypes.spiroct, 1500,
        UnitTypes.arkyid, 25000,
        UnitTypes.toxopid, 325000,

        UnitTypes.flare, 75,
        UnitTypes.horizon, 250,
        UnitTypes.zenith, 2500,
        UnitTypes.antumbra, 18000,
        UnitTypes.eclipse, 175000,

        UnitTypes.retusa, 400,
        UnitTypes.oxynoe, 850,
        UnitTypes.cyerce, 5000,
        UnitTypes.aegires, 30000,
        UnitTypes.navanax, 350000,

        UnitTypes.risso, 500,
        UnitTypes.minke, 750,
        UnitTypes.bryde, 5000,
        UnitTypes.sei, 75000,
        UnitTypes.omura, 1500000,

        UnitTypes.mono, 3750000,
        UnitTypes.poly, 100000,
        UnitTypes.mega, 2500,
        UnitTypes.quad, 25000,
        UnitTypes.oct, 250000);

        aidBlockAmounts.putAll(Blocks.liquidSource, 4,
        Blocks.powerSource, 4,
        Blocks.itemSource, 6,
        Blocks.constructor, 1,
                               
        Blocks.thoriumWallLarge, 8,
        Blocks.surgeWallLarge, 4,

        Blocks.mendProjector, 3,
        Blocks.forceProjector, 2,
        Blocks.repairPoint, 4,
        Blocks.repairTurret, 2,

        Blocks.arc, 6,
        Blocks.lancer, 4,
        Blocks.ripple, 2,
        Blocks.cyclone, 1,
        Blocks.swarmer, 2,
        Blocks.tsunami, 1,
        Blocks.spectre, 1,
        Blocks.foreshadow, 1);

        content.units().each(u -> u.defaultController = FlyingAI::new);
        UnitTypes.crawler.defaultController = ArenaAI::new;
        UnitTypes.atrax.defaultController = ArenaAI::new;
        UnitTypes.spiroct.defaultController = ArenaAI::new;
        UnitTypes.arkyid.defaultController = ArenaAI::new;
        UnitTypes.toxopid.defaultController = ArenaAI::new;

        UnitTypes.poly.defaultController = SwarmAI::new;
        UnitTypes.mega.defaultController = ReinforcementAI::new;

        UnitTypes.scepter.defaultController = ArenaAI::new;
        UnitTypes.reign.defaultController = ArenaAI::new;

        UnitTypes.risso.flying = true;
        UnitTypes.minke.flying = true;
        UnitTypes.bryde.flying = true;
        UnitTypes.sei.flying = true;
        UnitTypes.omura.flying = true;

        UnitTypes.retusa.flying = true;
        UnitTypes.oxynoe.flying = true;
        UnitTypes.cyerce.flying = true;
        UnitTypes.aegires.flying = true;
        UnitTypes.navanax.flying = true;

        UnitTypes.crawler.maxRange = 8000f;
        UnitTypes.atrax.maxRange = 8000f;
        UnitTypes.spiroct.maxRange = 8000f;
        UnitTypes.arkyid.maxRange = 8000f;
        UnitTypes.toxopid.maxRange = 8000f;
        UnitTypes.reign.maxRange = 8000f;

        UnitTypes.poly.maxRange = 2000f;

        UnitTypes.reign.speed = 2.5f;

        UnitTypes.poly.abilities.add(new UnitSpawnAbility(UnitTypes.poly, 480f, 0f, -32f));
        UnitTypes.poly.health = 125f;
        UnitTypes.poly.speed = 1.5f;

        UnitTypes.arkyid.weapons.each(w -> {
            if(w.bullet instanceof SapBulletType sap) sap.sapStrength = 0f;
        });

        Events.on(WorldLoadEvent.class, event -> {
            if(state.rules.defaultTeam.core() != null){
                Core.app.post(() -> state.rules.defaultTeam.cores().each(Building::kill));
            }

            Core.app.post(() -> {
                state.rules.canGameOver = false;
                state.rules.waveTimer = false;
                state.rules.waves = true;
                state.rules.unitCap = unitCap;
                Call.setRules(state.rules);
            });

            worldWidth = world.width() * tilesize;
            worldHeight = world.height() * tilesize;
            worldCenterX = worldWidth / 2;
            worldCenterY = worldHeight / 2;
            firstWaveLaunched = false;
            waveIsOver = true;
            timer = Time.millis();
            newGame();
        });

        Events.on(PlayerJoin.class, event -> {
            if(!money.containsKey(event.player.uuid()) || !units.containsKey(event.player.uuid())){
                Bundle.bundled(event.player, "events.join.welcome");
                money.put(event.player.uuid(), (int)(money.get(event.player.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                units.put(event.player.uuid(), UnitTypes.dagger);
                respawnPlayer(event.player);
            }else{
                Bundle.bundled(event.player, "events.join.already-played");
                if(unitIDs.containsKey(event.player.uuid())){
                    Unit swapTo = Groups.unit.getByID(unitIDs.get(event.player.uuid()));
                    if(swapTo != null){
                        if(swapTo.getPlayer() != null && unitIDs.containsKey(swapTo.getPlayer().uuid())){
                            Player intruder = swapTo.getPlayer();
                            Unit swapIntruderTo = Groups.unit.getByID(unitIDs.get(intruder.uuid()));
                            if(swapIntruderTo != null){
                                intruder.unit(swapIntruderTo);
                            }else{
                                intruder.clearUnit();
                            }
                        }
                        Timer.schedule(() -> event.player.unit(swapTo), 1f);
                    }
                }else{
                    respawnPlayer(event.player);
                }
            }
        });

        Events.run(Trigger.update, () -> {
            if(gameIsOver) return;

            if(!Groups.unit.contains(u -> u.team == state.rules.defaultTeam)){
                gameIsOver = true;
                if(wave > bossWave){
                    Bundle.sendToChat("events.gameover.win");
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.defaultTeam)), 2f);
                }else{
                    Bundle.sendToChat("events.gameover.lose");
                    Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.waveTeam)), 2f);
                }
                return;
            }

            Groups.player.each(p -> Call.setHudText(p.con, Bundle.format("labels.money", Bundle.findLocale(p), money.get(p.uuid()))));

            if(Mathf.chance(1f * tipChance * Time.delta)) Bundle.sendToChat("events.tip.info");
            if(Mathf.chance(1f * tipChance * Time.delta)) Bundle.sendToChat("events.tip.upgrades");

            if(!Groups.unit.contains(u -> u.team == state.rules.waveTeam) && !waveIsOver){
                if(wave < reinforcementMinWave || wave % reinforcementSpacing != 0){
                    Bundle.sendToChat("events.wave", (int)waveDelay);
                    Timer.schedule(this::nextWave, waveDelay);
                }else{
                    Bundle.sendToChat("events.next-wave", (int)(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp));
                    Timer.schedule(this::spawnReinforcements, 2.5f);
                    Timer.schedule(this::nextWave, reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp);
                }
                Groups.player.each(p -> {
                    respawnPlayer(p);
                    money.put(p.uuid(), (int)(money.get(p.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                });
                Groups.unit.each(u -> !u.isPlayer() && unitIDs.containsValue(u.id), Unit::kill);
                waveIsOver = true;
            }
            if(!waveIsOver){
                enemyTypes.each(type -> type.speed += enemySpeedBoost * Time.delta);
            }
        });

        netServer.admins.addActionFilter(action -> action.type != Administration.ActionType.breakBlock && action.type != Administration.ActionType.placeBlock);

        Log.info("Crawler Arena loaded.");
    }

    public void newGame(){
        if(firstWaveLaunched) return;
        if(Groups.player.size() == 0){
            Timer.schedule(this::newGame, 5f);
            gameIsOver = true;
            return;
        }

        reset();
        Timer.schedule(() -> gameIsOver = false, 5f);

        Bundle.sendToChat("events.first-wave", (int)firstWaveDelay);
        Timer.schedule(this::nextWave, firstWaveDelay);
        firstWaveLaunched = true;
        waveIsOver = true;
    }

    public void spawnReinforcements(){
        Bundle.sendToChat("events.aid");
        Seq<Unit> megas = new Seq<>();
        ObjectMap<Block, Integer> blocks = new ObjectMap<>();
        int megasFactor = (int)Math.min(wave * reinforcementScaling * statScaling, reinforcementMax);
        for (int i = 0; i < megasFactor; i += reinforcementFactor){
            Unit u = UnitTypes.mega.spawn(reinforcementTeam, 32, worldCenterY + Mathf.random(-80, 80));
            u.health = Integer.MAX_VALUE;
            megas.add(u);
        }

        for (int i = 0; i < megas.size; i++){
            Block block = Seq.with(aidBlockAmounts.keys()).get(Mathf.random(0, aidBlockAmounts.size - 1));
            blocks.put(block, aidBlockAmounts.get(block));
        }

        blocks.each((block, amount) -> {
            for (int i = 0; i < amount; i++){
                if(megas.get(megas.size - 1) instanceof Payloadc pay)
                    pay.addPayload(new BuildPayload(block, state.rules.defaultTeam));
            }
            megas.remove(megas.size - 1);
        });
    }

    public void respawnPlayer(Player p){
        if(p.unit().id != unitIDs.get(p.uuid(), -1)) p.unit().kill();
        if(p.dead() || p.unit().id != unitIDs.get(p.uuid())){
            Tile tile = world.tile(worldCenterX / 8 + Mathf.random(-3, 3), worldCenterY / 8 + Mathf.random(-3, 3));
            UnitType type = units.get(p.uuid());
            if(type == null){ // why does this happen
                type = UnitTypes.dagger;
            }
            int x = tile.x;
            if(!type.flying && tile.solid()){
                while (x > 0 && world.tile(x, tile.y).solid()){
                    x--;
                }
                if(x == 0){
                    x = tile.x;
                    tile.removeNet();
                }
            }
            Unit unit = type.spawn(x * tilesize, tile.worldy());
            setUnit(unit);
            p.unit(unit);
            unitIDs.put(p.uuid(), unit.id);
            return;
        }

        if(p.unit().health < p.unit().maxHealth){
            p.unit().heal();
            Bundle.bundled(p, "events.heal", Pal.heal);
        }
    }

    public void spawnEnemy(UnitType unit, int spX, int spY){
        int sX = 32;
        int sY = 32;
        switch (Mathf.random(0, 3)){
            case 0 -> {
                sX = worldWidth - 32;
                sY = worldCenterY + Mathf.random(-spY, spY);
            }
            case 1 -> {
                sX = worldCenterX + Mathf.random(-spX, spX);
                sY = worldHeight - 32;
            }
            case 2 -> sY = worldCenterY + Mathf.random(-spY, spY);
            case 3 -> sX = worldCenterX + Mathf.random(-spX, spX);
        }

        Unit u = unit.spawn(state.rules.waveTeam, sX, sY);
        u.armor = 0f;
        u.maxHealth *= (statScaling * healthMultiplierBase * (unit == UnitTypes.reign ? bossHealthMultiplier : 1f));
        u.health = u.maxHealth;

        if(unit == UnitTypes.reign){
            u.apply(StatusEffects.boss);
            u.abilities.add(new UnitSpawnAbility(UnitTypes.scepter, bossScepterDelayBase / Groups.player.size(), 0, -32));
        }
    }

    public void nextWave(){
        wave++;
        state.wave = wave;
        statScaling = 1f + wave * statScalingNormal;
        Timer.schedule(() -> waveIsOver = false, 1f);

        int crawlers = Mathf.ceil(Mathf.pow(crawlersExpBase, 1f + wave * crawlersRamp + Mathf.pow(wave, 2f) * extraCrawlersRamp) * Groups.player.size() * crawlersMultiplier);

        if(wave == bossWave - 5) Bundle.sendToChat("events.good-game");
        else if(wave == bossWave - 3) Bundle.sendToChat("events.what-so-long");
        else if(wave == bossWave - 1) Bundle.sendToChat("events.why-alive");
        else if(wave == bossWave){
            Bundle.sendToChat("events.boss");
            spawnEnemy(UnitTypes.reign, 32, 32);
            return;
        }
        else if(wave == bossWave + 1){
            Bundle.sendToChat("events.victory", Time.timeSinceMillis(timer));
        }

        if(crawlers > crawlersCeiling && wave > bossWave){
            crawlers = crawlersCeiling;
            statScaling = 1f + (float)(wave - bossWave) * extraScalingRamp;
        }

        UnitTypes.crawler.health += crawlerHealthRamp * wave * statScaling;
        UnitTypes.crawler.speed += crawlerSpeedRamp * wave * statScaling;

        int spreadX = Math.max(worldCenterX - 160 - wave * 10, 160);
        int spreadY = Math.max(worldCenterY - 160 - wave * 10, 160);

        ObjectIntMap<UnitType> typeCounts = new ObjectIntMap<>();
        int totalTarget = maxUnits - keepCrawlers;
        for(UnitType type : enemyTypes){
            int typeCount = Math.min(crawlers / enemyCrawlerCuts.get(type), totalTarget / 2);
            totalTarget -= typeCount;
            typeCounts.put(type, typeCount);
            crawlers -= typeCount * enemyCrawlerCuts.get(type) / 2;
            type.speed = defaultEnemySpeeds.get(type, 1f);
        }
        crawlers = Math.min(crawlers, keepCrawlers);

        typeCounts.forEach(entry -> spawnEnemies(entry.key, entry.value, spreadX, spreadY));
        spawnEnemies(UnitTypes.crawler, crawlers, spreadX, spreadY);
    }

    public void spawnEnemies(UnitType unit, int amount, int spX, int spY){
        for (int i = 0; i < amount; i++) spawnEnemy(unit, spX, spY);
    }

    public void setUnit(Unit unit){
        if(unit.type == UnitTypes.crawler){
            unit.maxHealth = playerCrawlerHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerCrawlerArmor;
            unit.abilities.add(new UnitSpawnAbility(UnitTypes.crawler, playerCrawlerCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        }else if(unit.type == UnitTypes.mono){
            unit.maxHealth = playerMonoHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerMonoArmor;
            unit.abilities.add(new UnitSpawnAbility(UnitTypes.navanax, playerMonoCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
        }else if(unit.type == UnitTypes.poly){
            unit.maxHealth = playerPolyHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerPolyArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            if(unit.abilities.find(ability -> ability instanceof UnitSpawnAbility) instanceof UnitSpawnAbility ability) ability.spawnTime = playerPolyCooldown;
        }else if(unit.type == UnitTypes.omura){
            unit.maxHealth = playerOmuraHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerOmuraArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            unit.abilities.each(ability -> ability instanceof UnitSpawnAbility, ability -> {
                if(ability instanceof UnitSpawnAbility spawnAbility) spawnAbility.spawnTime = playerOmuraCooldown;
            });
        }
        unit.controller(new FlyingAI());
    }

    public void reset(){
        wave = 1;
        state.wave = wave;
        statScaling = 1f;
        UnitTypes.crawler.speed = crawlerSpeedBase;
        UnitTypes.crawler.health = crawlerHealthBase;
        money.clear();
        units.clear();
        unitIDs.clear();
        Groups.player.each(p -> {
            money.put(p.uuid(), 0);
            units.put(p.uuid(), UnitTypes.dagger);
            Timer.schedule(() -> respawnPlayer(p), 1f);
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("upgrade", "<type>", "Upgrade to another unit", (args, player) -> {
            UnitType newUnitType = Seq.with(unitCosts.keys()).find(u -> u.name.equalsIgnoreCase(args[0]));
            if(newUnitType == null){
                Bundle.bundled(player, "commands.upgrade.unit-not-found");
                return;
            }

            if(money.get(player.uuid()) >= unitCosts.get(newUnitType)){
                if(!player.dead() && player.unit().type == newUnitType){
                    Unit newUnit = newUnitType.spawn(player.x, player.y);
                    setUnit(newUnit);
                    money.put(player.uuid(), money.get(player.uuid()) - unitCosts.get(newUnitType));
                    Bundle.bundled(player, "commands.upgrade.already");
                    return;
                }
                Unit newUnit = newUnitType.spawn(player.x, player.y);
                setUnit(newUnit);
                player.unit(newUnit);
                money.put(player.uuid(), money.get(player.uuid()) - unitCosts.get(newUnitType));
                units.put(player.uuid(), newUnitType);
                unitIDs.put(player.uuid(), newUnit.id);
                Bundle.bundled(player, "commands.upgrade.success");

            }else Bundle.bundled(player, "commands.upgrade.not-enough-money");
        });

        handler.<Player>register("give", "<amount> <name...>", "Give money to another player", (args, player) -> {
            Player giveTo = Groups.player.find(p -> Strings.stripColors(p.name).toLowerCase().contains(args[1].toLowerCase()));
            if(giveTo == null){
                Bundle.bundled(player, "commands.give.player-not-found");
                return;
            }

            int amount;
            try{
                amount = Integer.parseInt(args[0]);
            }catch(NumberFormatException e){
                Bundle.bundled(player, "commands.give.invalid-amount");
                return;
            }
            if(amount <= 0){
                Bundle.bundled(player, "commands.give.invalid-amount");
                return;
            }

            if(money.get(player.uuid()) >= amount){
                money.put(player.uuid(), money.get(player.uuid()) - amount);
                money.put(giveTo.uuid(), money.get(giveTo.uuid()) + amount);
                Bundle.bundled(player, "commands.give.success", amount, giveTo.coloredName());
                Bundle.bundled(giveTo, "commands.give.money-recieved", amount, player.coloredName());

            }else Bundle.bundled(player, "commands.give.not-enough-money");
        });

        handler.<Player>register("info", "Show info about the Crawler Arena gamemode", (args, player) -> Bundle.bundled(player, "commands.info"));

        handler.<Player>register("upgrades", "Show units you can upgrade to", (args, player) -> {
            StringBuilder upgrades = new StringBuilder(Bundle.format("commands.upgrades.header", Bundle.findLocale(player)));
            IntSeq sortedUnitCosts = unitCosts.values().toArray();
            sortedUnitCosts.sort();
            ObjectIntMap<UnitType> unitCostsCopy = new ObjectIntMap<>();
            unitCostsCopy.putAll(unitCosts);
            sortedUnitCosts.each((cost) -> {
                UnitType type = unitCostsCopy.findKey(cost);
                upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(cost).append(")\n");
                unitCostsCopy.remove(type);
            });
            player.sendMessage(upgrades.toString());
        });
    }

    public void registerServerCommands(CommandHandler handler){
        handler.register("kill", "Kill all enemies in the current wave.", args -> Groups.unit.each(u -> u.team == state.rules.waveTeam, Unitc::kill));
    }
}
