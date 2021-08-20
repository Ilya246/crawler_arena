package SwarmAI;

import arc.math.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.meta.*;
import mindustry.ai.types.*;
import mindustry.entities.*;

import static mindustry.Vars.*;

public class SwarmAI extends FlyingAI{
    public float swarmRange = 400f;
    public float avoidRange = 480f;
    public float swarmCollectRange = 1200f;
    public int swarmCount = 24;
    Unit[] farthestPoly = new Unit[]{null};
    float[] cdist = new float[]{0f};

    @Override
    public void updateMovement(){
        if(target != null && unit.hasWeapons() && command() == UnitCommand.attack){
            if(!unit.type.circleTarget){
                if(Units.count(unit.x, unit.y, swarmRange, u -> {
                    return u.type == unit.type && u.team == unit.team;
                }) > swarmCount){
                    moveTo(target, unit.type.range * 0.8f);
                }else if(target instanceof Posc){
                    Posc targetPosc = (Posc)target;
                    if(unit.dst2(targetPosc.getX(), targetPosc.getY()) > avoidRange * avoidRange * 0.49f){
                        farthestPoly[0] = null;
                        cdist[0] = 0;
                        Units.nearby(unit.team, unit.x - swarmCollectRange, unit.y - swarmCollectRange, swarmCollectRange * 2f, swarmCollectRange * 2f, e -> {
                            float dst2 = e.dst2(unit.x, unit.y) - (e.hitSize * e.hitSize);
                            if(dst2 < swarmCollectRange * swarmCollectRange && (farthestPoly[0] == null || dst2 > cdist[0])){
                                farthestPoly[0] = e;
                                cdist[0] = dst2;
                            }
                        });
                        if(farthestPoly[0] != null){
                            moveTo(farthestPoly[0], 8f);
                        }else{
                            moveTo(target, avoidRange);
                        };
                    }else{
                        moveTo(target, avoidRange);
                    };
                }else{
                    moveTo(target, avoidRange);
                };
                unit.lookAt(target);
            }else{
                attack(120f);
            };
        };

        if(target == null && command() == UnitCommand.attack && state.rules.waves && unit.team == state.rules.defaultTeam){
            moveTo(getClosestSpawner(), state.rules.dropZoneRadius + 120f);
        };

        if(command() == UnitCommand.rally){
            moveTo(targetFlag(unit.x, unit.y, BlockFlag.rally, false), 60f);
        }
    }
}
