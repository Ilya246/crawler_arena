package crawler_arena;

import arc.struct.*;
import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class CVars{
    public static int unitCap = 96;
    public static float tipChance = 1f / 30000f;

    public static int unitsRows = 10;

    public static int bossWave = 25;
    public static int maxWave = 27;
    public static int crawlersCeiling = 10000000;
    public static int maxUnits = 4000;
    public static int keepCrawlers = 1500;
    public static float crawlersExpBase = 2.2f;
    public static float crawlersRamp = 1f / 1.5f;
    public static float extraCrawlersRamp = 1f / 40f;
    public static float crawlersMultiplier = 1f / 5f;

    public static float moneyExpBase = 2.2f;
    public static float moneyRamp = 1f / 1.5f;
    public static float extraMoneyRamp = 1f / 40f;
    public static float moneyMultiplier = 4f;

    public static float enemySpeedBoost = 0.00002f;
    public static float crawlerHealthBase = 60f;
    public static float crawlerSpeedBase = 0.43f;
    public static float statScalingNormal = 1f / 100f;
    public static float extraScalingRamp = 1f / 2f;
    public static float healthMultiplierBase = 1f / 7f;

    public static float firstWaveDelay = 15f;
    public static float waveDelay = 10f;
    public static float waveDelayRamp = 1f;
    public static float reinforcementWaveDelayBase = 20f;
    public static float reinforcementWaveDelayRamp = 4f;
    public static float reinforcementWaveDelayMax = 80f;

    public static Team reinforcementTeam = Team.derelict;
    public static int reinforcementMinWave = 2;
    public static int reinforcementSpacing = 1;
    public static float reinforcementScaling = 1f / 4f;
    public static float reinforcementRamp = 1f / 20f;
    public static int maxAirdropSearches = 100;
    public static float blockDropChance = 1f / 25f;

    public static float retargetChance = 30f;
    public static float retargetDelay = 180f;

    public static class WeightedEntry<T> {
        public T value;
        public float weight;

        public WeightedEntry(float w, T val){
            value = val;
            weight = w;
        }
    }

    public static class DropSpecifier {
        public Seq<Block> blocks = new Seq<>();
        public IntSeq amounts = new IntSeq();

        public DropSpecifier(Block block, int amount){
            blocks.add(block);
            amounts.add(amount);
        }

        public DropSpecifier(Block[] blocks, int[] amounts){
            this.blocks.addAll(blocks);
            this.amounts.addAll(amounts);
        }

        public int size(){
            return blocks.size;
        }
    }

    public static Seq<WeightedEntry<DropSpecifier>> aidDrops = new Seq<>();
    public static float aidBlocksTotal = 0f;
    public static IntMap<DropSpecifier> guaranteedDrops = new IntMap<>();
    public static Seq<Block> guaranteedAirdrops = Seq.with(Blocks.coreNucleus, Blocks.coreAcropolis, Blocks.boulder);
    public static ObjectIntMap<UnitType> unitCosts = new ObjectIntMap<>();

    public static DropSpecifier randomDrop(){
        float at = Mathf.random(aidBlocksTotal);
        int ind = 0;
        int moveBy = Integer.highestOneBit(aidDrops.size);
        while(moveBy > 0){
            if(ind + moveBy < aidDrops.size && aidDrops.get(ind + moveBy).weight < at){
                ind += moveBy;
            }
            moveBy = moveBy >> 1;
            Log.info("index @ moveBy @", ind, moveBy);
        }
        return aidDrops.get(ind).value;
    }

    public static float playerCrawlerHealth = 400f;
    public static float playerCrawlerArmor = 10f;
    public static float playerCrawlerCooldown = 60f;
    public static float playerMonoHealth = 100000f;
    public static float playerMonoArmor = 20f;
    public static float playerMonoCooldown = 300f;
    public static Seq<UnitType> playerMonoSpawnTypes = Seq.with(UnitTypes.navanax, UnitTypes.toxopid, UnitTypes.corvus);
    public static float playerPolyHealth = 500f;
    public static float playerPolyArmor = 100f;
    public static float playerPolyCooldown = 60f;
    public static float playerOmuraHealth = 100000f;
    public static float playerOmuraArmor = 20f;
    public static float playerOmuraCooldown = 30f;
    public static float ultraDaggerChance = 1f / 150f;
    public static float ultraDaggerHealth = 1000f;
    public static float ultraDaggerArmor = 100f;
    public static float ultraDaggerCooldown = 30f;

    public static Seq<UnitType> enemyTypes = Seq.with(UnitTypes.toxopid, UnitTypes.arkyid, UnitTypes.spiroct, UnitTypes.atrax); // *MUST* be ordered from most to least powerful
    public static ObjectIntMap<UnitType> enemyThresholds = new ObjectIntMap<>();
    public static ObjectIntMap<UnitType> enemyCrawlerCuts = new ObjectIntMap<>();
    public static ObjectFloatMap<UnitType> enemyMaxSpawnTimes = new ObjectFloatMap<>();
    public static ObjectFloatMap<UnitType> enemySpawnTimeRamps = new ObjectFloatMap<>();
    public static ObjectFloatMap<UnitType> defaultEnemySpeeds = new ObjectFloatMap<>();
    static {
        enemyTypes.each(type -> defaultEnemySpeeds.put(type, type.speed));

        enemyCrawlerCuts.putAll(UnitTypes.atrax, 10,
        UnitTypes.spiroct, 50,
        UnitTypes.arkyid, 1000,
        UnitTypes.toxopid, 20000);

        enemyThresholds.putAll(UnitTypes.atrax, 100,
        UnitTypes.spiroct, 400,
        UnitTypes.arkyid, 1000,
        UnitTypes.toxopid, 20000);

        enemyMaxSpawnTimes.put(UnitTypes.crawler, 5f);
        enemyMaxSpawnTimes.put(UnitTypes.atrax, 20f);
        enemyMaxSpawnTimes.put(UnitTypes.spiroct, 40f);
        enemyMaxSpawnTimes.put(UnitTypes.arkyid, 70f);
        enemyMaxSpawnTimes.put(UnitTypes.toxopid, 100f);

        enemySpawnTimeRamps.put(UnitTypes.crawler, 1f);
        enemySpawnTimeRamps.put(UnitTypes.atrax, 1f);
        enemySpawnTimeRamps.put(UnitTypes.spiroct, 1f);
        enemySpawnTimeRamps.put(UnitTypes.arkyid, 1f);
        enemySpawnTimeRamps.put(UnitTypes.toxopid, 2f);

        unitCosts.putAll(UnitTypes.nova, 100,
        UnitTypes.pulsar, 300,
        UnitTypes.quasar, 2000,
        UnitTypes.vela, 18000,
        UnitTypes.corvus, 250000,

        UnitTypes.dagger, 25,
        UnitTypes.mace, 200,
        UnitTypes.fortress, 1500,
        UnitTypes.scepter, 20000,
        UnitTypes.reign, 250000,

        UnitTypes.merui, 450,
        UnitTypes.cleroi, 1800,
        UnitTypes.anthicus, 12000,
        UnitTypes.tecta, 22000,
        UnitTypes.collaris, 325000,

        UnitTypes.elude, 500,
        UnitTypes.avert, 1000,
        UnitTypes.obviate, 11000,
        UnitTypes.quell, 25000,
        UnitTypes.disrupt, 250000,

        UnitTypes.stell, 400,
        UnitTypes.locus, 2500,
        UnitTypes.precept, 10000,
        UnitTypes.vanquish, 22000,
        UnitTypes.conquer, 250000,

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
        UnitTypes.aegires, 50000,
        UnitTypes.navanax, 400000,

        UnitTypes.risso, 500,
        UnitTypes.minke, 750,
        UnitTypes.bryde, 5000,
        UnitTypes.sei, 75000,
        UnitTypes.omura, 1500000,

        UnitTypes.mono, 10000000,
        UnitTypes.poly, 100000,
        UnitTypes.mega, 2500,
        UnitTypes.quad, 25000,
        UnitTypes.oct, 250000);

        FloatSeq weights = new FloatSeq();
        Seq<Block> drops = new Seq<>();
        IntSeq dropAmounts = new IntSeq();
        weights.addAll(    7f,                    7f,                        5f,                      10f,
                            5f,                    5f,                        1f,                      5f,
                             5f,                    10f,                       10f,                     5f,
                              5f,                    5f,                        5f,                      5f,
                           2f,                    3f,                        3f,                      3f,
                            4f,                    3f,                        4f,                      3f,
                             4f,                    4f,                        4f,                      3f,
                              3f,                    3f,                        3f,                      3f,
                           3f,                    1f,                        1f,                      1f,
                            1f,                    1f,                        1f);
        drops.addAll(      Blocks.liquidSource,   Blocks.powerSource,        Blocks.powerNodeLarge,   Blocks.itemSource,
                            Blocks.heatSource,     Blocks.constructor,        Blocks.largeConstructor, Blocks.unloader,
                             Blocks.container,      Blocks.thoriumWallLarge,   Blocks.surgeWallLarge,   Blocks.mendProjector,
                              Blocks.forceProjector, Blocks.repairPoint,        Blocks.repairTurret,     Blocks.overdriveProjector,
                           Blocks.overdriveDome,  Blocks.hyperProcessor,     Blocks.arc,              Blocks.scorch,
                            Blocks.lancer,         Blocks.sublimate,          Blocks.ripple,           Blocks.titan,
                             Blocks.cyclone,        Blocks.fuse,               Blocks.lustre,           Blocks.swarmer,
                              Blocks.tsunami,        Blocks.spectre,            Blocks.foreshadow,       Blocks.scathe,
                           Blocks.malign,         Blocks.coreNucleus,        Blocks.coreAcropolis,    Blocks.groundFactory,
                            Blocks.airFactory,     Blocks.navalFactory,       Blocks.boulder);
        dropAmounts.addAll(4,                     4,                         4,                       6,
                            4,                     1,                         1,                       4,
                             2,                     8,                         4,                       3,
                              2,                     4,                         2,                       1,
                           1,                     2,                         6,                       6,
                            4,                     2,                         2,                       2,
                             2,                     2,                         2,                       2,
                              1,                     1,                         1,                       1,
                           1,                     1,                         1,                       1,
                            1,                     1,                         100);
        for(int i = 0; i < drops.size; i++){
            float weight = weights.get(i);
            aidDrops.add(new WeightedEntry<>(weight + aidBlocksTotal, new DropSpecifier(drops.get(i), dropAmounts.get(i))));
            aidBlocksTotal += weight;
        }

        guaranteedDrops.put(20, new DropSpecifier(Blocks.largeConstructor, 1));
        guaranteedDrops.put(2,  new DropSpecifier(new Block[]{Blocks.itemSource, Blocks.liquidSource, Blocks.heatSource, Blocks.powerSource},
                                                  new int[]  {4,                 4,                   4,                 4}));
    }
    public static float crawlerHealthRamp = 1f;
    public static float crawlerSpeedRamp = 0.003f;

    public static int bossT1Cap = 2;
    public static int bossT2Cap = 5;
    public static int bossT3Cap = 8;
    public static int bossBuffThreshold = 150000;
    public static float bossHealthMultiplier = 6f;
    public static float bossScepterDelayBase = 600f;
}
