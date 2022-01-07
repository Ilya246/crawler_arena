package crawler_arena;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import mindustry.ai.types.GroundAI;
import mindustry.gen.Call;
import mindustry.gen.Payloadc;

import static mindustry.Vars.*;

public class ReinforcementAI extends GroundAI {

    @Override
    public void updateUnit(){
        if(unit.team == CVars.reinforcementTeam){
            unit.moveAt(new Vec2().trns(Mathf.atan2(world.width() * 4 - unit.x, world.height() * 4 - unit.y), unit.speed()));
            if(unit.x - world.width() * tilesize / 2f > -120f && unit instanceof Payloadc){
                Call.payloadDropped(unit, unit.x, unit.y);
            }
            if(unit.x > world.width() * 7){
                unit.kill();
            }
            if(unit.moving()) unit.lookAt(unit.vel().angle());
        }
    }
}
