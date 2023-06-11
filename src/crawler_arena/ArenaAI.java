package crawler_arena;

import mindustry.Vars;
import mindustry.ai.types.GroundAI;
import mindustry.entities.Predict;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;
import mindustry.gen.Teamc;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.liquid.Conduit;

public class ArenaAI extends GroundAI {

    @Override
    public void updateUnit(){
        if(Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)){
            target = null;
        }

        if(retarget()){
            target = target(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
        }

        boolean rotate = false, shoot = false;

        if(!Units.invalidateTarget(target, unit, unit.range()) && unit.hasWeapons()){
            rotate = true;
            shoot = unit.within(target, unit.type.range + (target instanceof Building ? 1.5f * Vars.tilesize / 2f : ((Hitboxc) target).hitSize() / 2f));

            if(unit.type.hasWeapons())
                unit.aimLook(Predict.intercept(unit, target, unit.type.weapons.first().bullet.speed));
        }

        if(target != null){
            unit.moveAt(vec.set(target).sub(unit).limit(unit.speed()));
            if(unit.moving()) unit.lookAt(unit.vel().angle());
            unit.controlWeapons(rotate, shoot);
        }
    }

    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground && !(t.block instanceof Conveyor || t.block instanceof Conduit));
    }
}
