package crawler_arena;

import arc.struct.*;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;

public class CVars{
    public static float tipChance = 1f / 30000f;

    public static int bossWave = 25;
    public static int crawlersCeiling = 10000000;
    public static int maxUnits = 2000;
    public static int keepCrawlers = 800;
    public static float crawlersExpBase = 2.71f;
    public static float crawlersRamp = 1f / 3f;
    public static float extraCrawlersRamp = 1f / 150f;
    public static float crawlersMultiplier = 1f / 20f;

    public static float moneyExpBase = 2.71f;
    public static float moneyRamp = 1f / 2f;
    public static float extraMoneyRamp = 1f / 4000f;
    public static float moneyMultiplier = 6f;

    public static float crawlerHealthBase = 60f;
    public static float crawlerSpeedBase = 0.43f;
    public static float statScalingNormal = 1f / 100f;
    public static float extraScalingRamp = 1f / 2f;
    public static float healthMultiplierBase = 1f / 4f;

    public static float firstWaveDelay = 15f;
    public static float waveDelay = 10f;
    public static float reinforcementWaveDelayBase = 50f;
    public static float reinforcementWaveDelayRamp = 3f;

    public static int reinforcementMinWave = 8;
    public static int reinforcementSpacing = 2;
    public static int reinforcementFactor = 3; // amount of reinforcements is integer-divided by this number
    public static int reinforcementScaling = 2;
    public static int reinforcementMax = 60 * reinforcementFactor;

    public static float playerCrawlerHealth = 400f;
    public static float playerCrawlerArmor = 10f;
    public static float playerCrawlerCooldown = 60f;
    public static float playerMonoHealth = 100000f;
    public static float playerMonoArmor = 10f;
    public static float playerMonoCooldown = 300f;
    public static float playerPolyHealth = 500f;
    public static float playerPolyArmor = 100f;
    public static float playerPolyCooldown = 60f;

    public static Seq<UnitType> enemyTypes = Seq.with(UnitTypes.toxopid, UnitTypes.arkyid, UnitTypes.spiroct, UnitTypes.atrax); // *MUST* be ordered from most to least powerful
    public static ObjectIntMap enemyThresholds = new ObjectIntMap<>();
    public static ObjectIntMap enemyCrawlerCuts = new ObjectIntMap<>();
    static {
        enemyCrawlerCuts.putAll(UnitTypes.atrax, 10,
        UnitTypes.spiroct, 50,
        UnitTypes.arkyid, 1000,
        UnitTypes.toxopid, 20000);
        enemyThresholds.putAll(UnitTypes.atrax, 100,
        UnitTypes.spiroct, 400,
        UnitTypes.arkyid, 1000,
        UnitTypes.toxopid, 20000);
    }
    public static float crawlerHealthRamp = 1f;
    public static float crawlerSpeedRamp = 0.003f;

    public static float bossHealthMultiplier = 6f;
    public static float bossScepterDelayBase = 1200f;
}
