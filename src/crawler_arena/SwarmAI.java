package crawler_arena;

import mindustry.ai.types.FlyingAI;
import mindustry.entities.Units;
import mindustry.entities.units.UnitCommand;
import mindustry.gen.Posc;
import mindustry.gen.Unit;
import mindustry.world.meta.BlockFlag;

import static mindustry.Vars.state;

public class SwarmAI extends FlyingAI {
    public float swarmRange = 320f;
    public float innerSwarmRange2 = 120f * 120f;
    public float avoidRange = 480f;
    public float avoidRange2 = avoidRange * avoidRange;
    public float swarmCollectRange = 1200f;
    public float kiteRange = 0.8f;
    public int swarmCount = 24;

    @Override
    public void updateMovement(){
        if(target != null && unit.hasWeapons() && command() == UnitCommand.attack){
            if(!unit.type.circleTarget){
                if(Units.count(unit.x, unit.y, swarmRange, u -> u.type == unit.type && u.team == unit.team) > swarmCount){
                    moveTo(target, unit.type.range * kiteRange);
                }else if(target != null){
                    Posc targetPosc = target;
                    if(unit.dst2(targetPosc.getX(), targetPosc.getY()) > avoidRange2){
                        Unit targetSameType = Units.closest(unit.team, unit.x, unit.y, u -> u.type == unit.type && unit.dst2(u) > innerSwarmRange2);
                        moveTo(targetSameType != null ? targetSameType : target, unit.hitSize * 2f);
                    }else{
                        moveTo(target, avoidRange * 1.1f);
                    }
                }else{
                    moveTo(null, avoidRange);
                }
                unit.lookAt(target);
            }else{
                attack(120f);
            }
        }

        if(target == null && command() == UnitCommand.attack && state.rules.waves && unit.team == state.rules.defaultTeam){
            moveTo(getClosestSpawner(), state.rules.dropZoneRadius + 120f);
        }

        if(command() == UnitCommand.rally){
            moveTo(targetFlag(unit.x, unit.y, BlockFlag.rally, false), 60f);
        }
    }
}
