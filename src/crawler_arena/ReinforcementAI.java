package crawler_arena;

import arc.math.geom.Vec2;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.*;
import mindustry.ai.types.GroundAI;
import mindustry.gen.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

public class ReinforcementAI extends GroundAI {

    Teamc target = null;
    Vec2 moveAt = new Vec2();
    boolean reached = false;
    long reachedSince = Time.millis();

    @Override
    public void updateUnit(){
        if(target == null || !target.isAdded()){
            target = Groups.player.isEmpty() ? null : Seq.with(Groups.player).min(p -> {
                return p.unit() == null ? Float.MAX_VALUE : p.unit().dst2(unit);
            }).unit();
            if(target != null){ // for the edge case where the target is instantly in range
                moveAt = moveAt.trns(Mathf.atan2(target.getX() - unit.x, target.getY() - unit.y) * Mathf.radDeg, unit.speed());
            }
        }else{
            if(!target.within(unit, 120f) || Time.millis() - reachedSince > 15 * 1000){
                if(reached){
                    target = null;
                    reached = false;
                    return;
                }
                moveAt = moveAt.trns(Mathf.atan2(target.getX() - unit.x, target.getY() - unit.y) * Mathf.radDeg, unit.speed());
                reachedSince = Time.millis();
            }else{
                Call.payloadDropped(unit, unit.x, unit.y);
                reached = true;
            }
            unit.moveAt(moveAt);
            if(unit instanceof Payloadc p && !p.hasPayload()){
                unit.vel.setLength(80f);
                unit.kill();
            }
            if(unit.moving()) unit.lookAt(unit.vel().angle());
        }
    }
}
