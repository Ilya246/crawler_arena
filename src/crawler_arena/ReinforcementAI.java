package crawler_arena;

import arc.math.geom.Vec2;
import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.ai.types.GroundAI;
import mindustry.gen.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

public class ReinforcementAI extends GroundAI {

    Teamc target = null;
    boolean reached = false;
    Vec2 moveAt = new Vec2();

    @Override
    public void updateUnit(){
        if(target == null){
            target = Groups.player.isEmpty() ? null : Seq.with(Groups.player).min(p -> {
                return p.unit() == null ? Float.MAX_VALUE : p.unit().dst2(unit);
            }).unit();
        }else{
            if(!reached){
                moveAt = moveAt.trns(Mathf.atan2(target.getX() - unit.x, target.getY() - unit.y) * Mathf.radDeg, unit.speed());
            }
            unit.moveAt(moveAt);
            if(target.within(unit, 120f)){
                reached = true;
            }
            if(reached){
                Call.payloadDropped(unit, unit.x, unit.y);
            }
            if(unit instanceof Payloadc p && !p.hasPayload()){
                unit.vel.setLength(80f);
                unit.kill();
            }
            if(unit.moving()) unit.lookAt(unit.vel().angle());
        }
    }
}
