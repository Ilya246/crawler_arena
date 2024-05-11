package crawler_arena;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.Pools;
import mindustry.ai.types.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.SapBulletType;
import mindustry.entities.units.*;
import mindustry.entities.Units;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.mod.Plugin;
import mindustry.net.Administration;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.BuildPayload;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.Env;

import static mindustry.Vars.*;
import static crawler_arena.CVars.*;

public class CrawlerArenaMod extends Plugin {
    public static boolean gameIsOver = true, waveIsOver = false, firstWaveLaunched = false;

    public static int worldWidth, worldHeight, worldCenterX, worldCenterY, wave = 1;
    public static float statScaling = 1f;

    public static ObjectIntMap<String> money = new ObjectIntMap<>();
    public static ObjectMap<String, UnitType> units = new ObjectMap<>();
    public static ObjectIntMap<String> unitIDs = new ObjectIntMap<>();

    public static ObjectIntMap<UnitType> spawnsLeft = new ObjectIntMap<>();

    public static Seq<Building> toRespawn = new Seq<>();
    public static Interval respawnInterval = new Interval();

    public static Seq<Timer.Task> timers = new Seq<>(); // for cleanup purposes

    public static long timer = Time.millis();
    public static long waveStartTime = Time.millis();

    @Override
    public void init(){
        UnitTypes.crawler.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();
        UnitTypes.atrax.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();
        UnitTypes.spiroct.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();
        UnitTypes.arkyid.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();
        UnitTypes.toxopid.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();

        UnitTypes.poly.controller = u -> new SwarmAI();
        UnitTypes.mega.controller = u -> u.team == CVars.reinforcementTeam ? new ReinforcementAI() : !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? u.type.aiController.get() : new CommandAI();

        UnitTypes.scepter.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();
        UnitTypes.reign.controller = u -> !u.type.playerControllable || (u.team.isAI() && !u.team.rules().rtsAi) ? new ArenaAI() : new CommandAI();

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
                Core.app.post(() -> state.rules.defaultTeam.cores().each(c -> {
                    c.tile.setNet(Blocks.air);
                }));
            }

            Core.app.post(() -> {
                state.rules.canGameOver = false;
                state.rules.waveTimer = false;
                state.rules.waves = true;
                state.rules.unitCap = unitCap;
                state.rules.enemyCoreBuildRadius = 0f;
                state.rules.env = defaultEnv;
                state.rules.hiddenBuildItems.clear();
                state.rules.planet = Planets.sun;
                Call.setRules(state.rules);
                newGame();
            });

            worldWidth = world.width() * tilesize;
            worldHeight = world.height() * tilesize;
            worldCenterX = worldWidth / 2;
            worldCenterY = worldHeight / 2;
            firstWaveLaunched = false;
            waveIsOver = true;
            timer = Time.millis();
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
                        Timer.schedule(() -> {
                            if(!swapTo.dead && swapTo != null){
                                event.player.unit(swapTo);
                            }
                        }, 1f);
                    }
                }else{
                    respawnPlayer(event.player);
                }
            }
        });

        Events.on(BlockDestroyEvent.class, (e) -> {
            toRespawn.add(e.tile.build);
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

            if(!spawnsLeft.isEmpty()){
                float timePassed = (Time.millis() - waveStartTime) * 0.001f;
                for(ObjectIntMap.Entry<UnitType> e : spawnsLeft.entries()){
                    float maxSpawnTime = enemyMaxSpawnTimes.get(e.key, 5f) + enemySpawnTimeRamps.get(e.key, 0f) * wave;
                    float spawnRate = Mathf.sqrt(e.value) * Math.min(1f, timePassed / maxSpawnTime);
                    if(Mathf.random() < Time.delta / 60f * spawnRate){
                        spawnEnemy(e.key, worldWidth * 0.4f, worldHeight * 0.4f);
                        int val = e.value;
                        spawnsLeft.put(e.key, e.value - 1);
                        if(val - 1 <= 0){
                            spawnsLeft.remove(e.key);
                        }
                    }
                }
            }else if(!Groups.unit.contains(u -> u.team == state.rules.waveTeam) && !waveIsOver){
                if(wave < reinforcementMinWave || wave % reinforcementSpacing != 0){
                    Bundle.sendToChat("events.wave", (int)(waveDelay + wave * waveDelayRamp));
                    timers.add(Timer.schedule(this::nextWave, waveDelay + wave * waveDelayRamp));
                }else{
                    Bundle.sendToChat("events.next-wave", (int)(Math.min(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp, reinforcementWaveDelayMax)));
                    timers.add(Timer.schedule(this::spawnReinforcements, 2.5f));
                    timers.add(Timer.schedule(this::nextWave, Math.min(reinforcementWaveDelayBase + wave * reinforcementWaveDelayRamp, reinforcementWaveDelayMax)));
                }
                Groups.player.each(p -> {
                    respawnPlayer(p);
                    money.put(p.uuid(), (int)(money.get(p.uuid(), 0) + Mathf.pow(moneyExpBase, 1f + wave * moneyRamp + Mathf.pow(wave, 2) * extraMoneyRamp) * moneyMultiplier));
                });
                waveIsOver = true;
            }
            if(!waveIsOver){
                enemyTypes.each(type -> type.speed += enemySpeedBoost * Time.delta * statScaling);
            }else if(respawnInterval.get(120f)){
                for(int i = 0; i < toRespawn.size; i++){
                    Building b = toRespawn.get(i);
                    Block block = b.block;
                    boolean valid = true;
                    for(int xi = b.tileX() - (block.size - 1) / 2; xi <= b.tileX() + block.size / 2; xi++){
                        for(int yi = b.tileY() - (block.size - 1) / 2; yi <= b.tileY() + block.size / 2; yi++){
                            if(world.tile(xi, yi).build != null){
                                valid = false;
                                break;
                            }
                        }
                        if(!valid){
                            break;
                        }
                    }
                    valid = valid && !Units.anyEntities(b.x - block.size * tilesize / 2f, b.y - block.size * tilesize / 2f, block.size * tilesize, block.size * tilesize);
                    if(valid){
                        b.tile.setNet(block, b.team, b.rotation);
                        toRespawn.remove(b);
                        i--;
                    }else{
                        Call.effect(Fx.unitCapKill, b.x, b.y, 1, Color.white);
                    }
                    Call.effect(Fx.placeBlock, b.x, b.y, (float)block.size, Color.white);
                }
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
        timers.each(t -> t.cancel());
        timers.clear();
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
        ObjectIntMap<Block> blocks = new ObjectIntMap<>();
        int megasFactor = (int)Math.min(wave * reinforcementScaling * statScaling, reinforcementMax);
        for(int i = 0; i < megasFactor; i += reinforcementFactor){
            Unit u = UnitTypes.mega.spawn(reinforcementTeam, 32, worldCenterY + Mathf.random(-80, 80));
            u.health = Integer.MAX_VALUE;
            megas.add(u);
        }

        for(int i = 0; i < megas.size; i++){
            Block block;
            boolean rare = false;
            if(Mathf.chance(rareAidChance / aidBlockAmounts.size)){
                block = Seq.with(rareAidBlockAmounts.keys()).random();
                rare = true;
            }else{
                block = Seq.with(aidBlockAmounts.keys()).random();
            }
            if(guaranteedAirdrops.contains(block) || Mathf.chance(blockDropChance)){
                int blockAmount = rare ? rareAidBlockAmounts.get(block) : aidBlockAmounts.get(block);
                int range = 10;
                int x = 0;
                int y = 0;
                IntSeq valids = new IntSeq();
                int j = 0;
                while((j < maxAirdropSearches && valids.size < blockAmount) || world.tile(x, y) == null){
                    x = Mathf.clamp(world.width() / 2 + Mathf.random(-range, range), 0, world.width());
                    y = Mathf.clamp(world.height() / 2 + Mathf.random(-range, range), 0, world.height());
                    boolean valid = true;
                    for(int xi = x - (block.size - 1) / 2; xi <= x + block.size / 2; xi++){
                        for(int yi = y - (block.size - 1) / 2; yi <= y + block.size / 2; yi++){
                            if(world.tile(xi, yi).build != null || valids.contains(Point2.pack(xi, yi))){
                                valid = false;
                                break;
                            }
                        }
                        if(!valid){
                            break;
                        }
                    }
                    valid = valid && !Units.anyEntities(x * tilesize + block.offset - block.size * tilesize / 2f, y * tilesize + block.offset - block.size * tilesize / 2f, block.size * tilesize, block.size * tilesize);
                    if(valid){
                        valids.add(Point2.pack(x, y));
                    }
                    range++;
                    j++;
                }
                valids.each(v -> {
                    Point2 unpacked = Point2.unpack(v);
                    float xf = unpacked.x * tilesize;
                    float yf = unpacked.y * tilesize;
                    Call.effect(Fx.blockCrash, xf, yf, 0, Color.white, block);
                    Time.run(100f, () -> {
                        Call.soundAt(Sounds.explosionbig, xf, yf, 1, 1);
                        Call.effect(Fx.spawnShockwave, xf, yf, block.size * 60f, Color.white);
                        world.tileWorld(xf, yf).setNet(block, state.rules.defaultTeam, 0);
                    });
                });
                blocks.put(block, 0);
            }else{
                blocks.put(block, rare ? rareAidBlockAmounts.get(block) : aidBlockAmounts.get(block));
            }
        }

        for(ObjectIntMap.Entry<Block> e : blocks){
            for(int i = 0; i < e.value; i++){
                if(megas.get(megas.size - 1) instanceof Payloadc pay)
                    pay.addPayload(new BuildPayload(e.key, state.rules.defaultTeam));
            }
            megas.remove(megas.size - 1);
        }
    }

    public void respawnPlayer(Player p){
        if(p.dead() || p.unit().id != unitIDs.get(p.uuid())){
            Unit oldUnit = Groups.unit.getByID(unitIDs.get(p.uuid()));
            if(oldUnit != null && oldUnit != p.unit()){
                oldUnit.kill();
            }
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

    public void applyStatus(Unit unit, float duration, int amount, StatusEffect... effects){
        Seq<StatusEntry> entries = new Seq<>();
        for(int i = 0; i < amount; i++){
            for(StatusEffect effect : effects){
                StatusEntry entry = Pools.obtain(StatusEntry.class, StatusEntry::new);
                entry.set(effect, duration);
                entries.add(entry);
            }
        }
        var fields = unit.getClass().getFields();
        for(var field : fields){
            if(field.getName().equals("statuses")){
                try{
                    if(field.get(unit) instanceof Seq s){
                        s.addAll(entries);
                    }
                }catch(Exception e){
                }
            }
        }
    }
    public void applyStatus(Unit unit, float duration, StatusEffect... effects){
        applyStatus(unit, duration, 1, effects);
    }

    public void spawnEnemy(UnitType unit, float spX, float spY){
        float sX = 32;
        float sY = 32;
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
        u.maxHealth *= statScaling * healthMultiplierBase;
        u.health = u.maxHealth;

        if(unit == UnitTypes.reign){
            u.apply(StatusEffects.boss);
            if(Groups.player.size() > bossT1Cap){
                u.apply(StatusEffects.overclock);
            }
            if(Groups.player.size() > bossT2Cap){
                u.apply(StatusEffects.overdrive);
            }
            if(Groups.player.size() > bossT3Cap){
                applyStatus(u, Float.MAX_VALUE, StatusEffects.overdrive, StatusEffects.overclock);
            }
            float totalHealth = 0f;
            for(Unit un : Groups.unit){
                totalHealth += un.maxHealth * (un.team == state.rules.defaultTeam ? 1 : 0);
            }
            if(totalHealth >= bossBuffThreshold){
                applyStatus(u, Float.MAX_VALUE, (int)totalHealth / bossBuffThreshold, StatusEffects.overdrive, StatusEffects.overclock);
            }
            u.maxHealth *= bossHealthMultiplier * Mathf.sqrt(Groups.player.size());
            u.health = u.maxHealth;
            addUnitAbility(u, new UnitSpawnAbility(UnitTypes.scepter, bossScepterDelayBase / Groups.player.size(), 0, -32));
        }
    }

    public void nextWave(){
        wave++;
        state.wave = wave;
        waveStartTime = Time.millis();
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
        else if(wave > bossWave + 1){
            gameIsOver = true;
            Bundle.sendToChat("events.gameover.win");
            Timer.schedule(() -> Events.fire(new GameOverEvent(state.rules.defaultTeam)), 2f);
            crawlers = 0;
            return;
        }

        if(crawlers > crawlersCeiling){
            crawlers = crawlersCeiling;
        }

        UnitTypes.crawler.health += crawlerHealthRamp * wave * statScaling;
        UnitTypes.crawler.speed += crawlerSpeedRamp * wave * statScaling;

        int totalTarget = maxUnits - keepCrawlers;
        for(UnitType type : enemyTypes){
            int typeCount = Math.min(crawlers / enemyCrawlerCuts.get(type), totalTarget / 2);
            totalTarget -= typeCount;
            if(typeCount != 0){
                spawnsLeft.put(type, typeCount);
            }
            crawlers -= typeCount * enemyCrawlerCuts.get(type) / 2;
            type.speed = defaultEnemySpeeds.get(type, 1f);
        }
        crawlers = Math.min(crawlers, keepCrawlers);
        if(crawlers != 0){
            spawnsLeft.put(UnitTypes.crawler, crawlers);
        }
    }

    public void addUnitAbility(Unit unit, Ability ability){
        unit.abilities = Seq.with(unit.abilities).add(ability).toArray();
    }

    public void setUnit(Unit unit, boolean ultraEligible){
        if(unit.type == UnitTypes.crawler){
            unit.maxHealth = playerCrawlerHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerCrawlerArmor;
            addUnitAbility(unit, new UnitSpawnAbility(UnitTypes.crawler, playerCrawlerCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        }else if(unit.type == UnitTypes.mono){
            unit.maxHealth = playerMonoHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerMonoArmor;
            addUnitAbility(unit, new UnitSpawnAbility(playerMonoSpawnTypes.random(), playerMonoCooldown, 0f, -8f));
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
        }else if(unit.type == UnitTypes.poly){
            unit.maxHealth = playerPolyHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerPolyArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            boolean found = false;
            for(Ability ab : unit.abilities){
                if(ab instanceof UnitSpawnAbility s){
                    s.spawnTime = playerPolyCooldown;
                }
            }
        }else if(unit.type == UnitTypes.omura){
            unit.maxHealth = playerOmuraHealth;
            unit.health = unit.maxHealth;
            unit.armor = playerOmuraArmor;
            unit.apply(StatusEffects.boss);
            unit.apply(StatusEffects.overclock, Float.MAX_VALUE);
            unit.apply(StatusEffects.overdrive, Float.MAX_VALUE);
            for(Ability ab : unit.abilities){
                if(ab instanceof UnitSpawnAbility s){
                    s.spawnTime = playerOmuraCooldown;
                }
            }
        }else if(ultraEligible && unit.type == UnitTypes.dagger && Mathf.chance(ultraDaggerChance)){
            unit.maxHealth = ultraDaggerHealth;
            unit.health = unit.maxHealth;
            unit.armor = ultraDaggerArmor;
            addUnitAbility(unit, new UnitSpawnAbility(UnitTypes.dagger, ultraDaggerCooldown, 0f, -1f));
            applyStatus(unit, Float.MAX_VALUE, 3, StatusEffects.overclock, StatusEffects.overdrive, StatusEffects.boss);
        }
    }
    public void setUnit(Unit unit){
        setUnit(unit, false);
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
        spawnsLeft.clear();
        toRespawn.clear();
        Groups.player.each(p -> {
            money.put(p.uuid(), 0);
            units.put(p.uuid(), UnitTypes.dagger);
            Timer.schedule(() -> respawnPlayer(p), 1f);
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register("upgrade", "<type> [amount]", "Upgrade to another unit. Specifying amount instead buys units to fight alongside you", (args, player) -> {
            UnitType newUnitType = Seq.with(unitCosts.keys()).find(u -> u.name.equalsIgnoreCase(args[0]));
            if(newUnitType == null){
                Bundle.bundled(player, "commands.upgrade.unit-not-found");
                return;
            }
            int amount = 1;
            if(args.length == 2){
                try{
                    amount = Integer.parseInt(args[1]);
                }catch(NumberFormatException e){
                    Bundle.bundled(player, "exceptions.invalid-amount");
                    return;
                }
            }
            if(amount < 1){
                Bundle.bundled(player, "exceptions.invalid-amount");
                return;
            }

            if(Groups.unit.count(u -> u.type == newUnitType && u.team == state.rules.defaultTeam) > unitCap - amount){
                Bundle.bundled(player, "commands.upgrade.too-many-units");
                return;
            }

            if(money.get(player.uuid()) >= unitCosts.get(newUnitType) * amount){
                if(!player.dead() && player.unit().type == newUnitType || args.length == 2){
                    for(int i = 0; i < amount; i++){
                        Unit newUnit = newUnitType.spawn(player.x + Mathf.random(), player.y + Mathf.random());
                        setUnit(newUnit);
                    }
                    money.put(player.uuid(), money.get(player.uuid()) - unitCosts.get(newUnitType) * amount);
                    Bundle.bundled(player, "commands.upgrade.already");
                    return;
                }
                Unit newUnit = newUnitType.spawn(player.x, player.y);
                setUnit(newUnit, true);
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
            if(args[0].equalsIgnoreCase("all")){
                amount = money.get(player.uuid());
            }else{
                try{
                    amount = Integer.parseInt(args[0]);
                }catch(NumberFormatException e){
                    Bundle.bundled(player, "exceptions.invalid-amount");
                    return;
                }
            }
            if(amount < 0){
                Bundle.bundled(player, "exceptions.invalid-amount");
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

        handler.<Player>register("upgrades", "[page]", "Show units you can upgrade to", (args, player) -> {
            int page;
            if(args.length == 0){
                page = 1;
            }else{
                try{
                    page = Integer.parseInt(args[0]);
                }catch(NumberFormatException e){
                    Bundle.bundled(player, "exceptions.invalid-amount");
                    return;
                }
            }
            IntSeq sortedUnitCosts = unitCosts.values().toArray();
            int maxPage = (sortedUnitCosts.size - 1) / (unitsRows * page) + 1;
            if(1 > page || page > maxPage){
                Bundle.bundled(player, "exceptions.invalid-amount");
                return;
            }
            sortedUnitCosts.sort();
            if(page > 1){
                sortedUnitCosts.removeRange(0, unitsRows * (page - 1) - 1);
            }
            sortedUnitCosts.removeRange(unitsRows * page, sortedUnitCosts.size - 1);
            ObjectIntMap<UnitType> unitCostsCopy = new ObjectIntMap<>();
            unitCostsCopy.putAll(unitCosts);
            int i = 1;
            StringBuilder upgrades = new StringBuilder(Bundle.format("commands.upgrades.header", Bundle.findLocale(player)));
            upgrades.append(Bundle.format("commands.upgrades.header", Bundle.findLocale(player), page, maxPage)).append("/n");
            sortedUnitCosts.each((cost) -> {
                UnitType type = unitCostsCopy.findKey(cost);
                upgrades.append("[gold] - [accent]").append(type.name).append(" [lightgray](").append(cost).append("/n");
                unitCostsCopy.remove(type);
            });
            player.sendMessage(upgrades.toString());
        });

        handler.<Player>register("cost", "<type>", "Lookup the cost of a unit", (args, player) -> {
            int cost = -1;
            for(ObjectIntMap.Entry<UnitType> pair : unitCosts.entries()){
                if(pair.key.name.equalsIgnoreCase(args[0])){
                    cost = pair.value;
                }
            }
            if(cost == -1){
                Bundle.bundled(player, "commands.upgrade.unit-not-found");
                return;
            }
            player.sendMessage(Integer.toString(cost));
        });
    }

    public void registerServerCommands(CommandHandler handler){
        handler.register("kill", "Kill all enemies in the current wave.", args -> Groups.unit.each(u -> u.team == state.rules.waveTeam, Unitc::kill));
    }
}
