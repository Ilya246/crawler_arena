package ReinforcementAI;

import arc.math.geom.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.ai.types.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import arc.math.Mathf;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

public class ReinforcementAI extends GroundAI{

  @Override
  public void updateUnit(){
      unit.moveAt(new Vec2().trns(Mathf.atan2(world.width() * 4 - unit.x, world.height() * 4 - unit.y), unit.speed()));
      if(Math.abs(unit.x - world.width() * 4) < 120 && unit instanceof Payloadc){
          Payloadc pay = (Payloadc)unit;
          pay.dropLastPayload();
      };
      if(unit.x > world.width() * 7){
          unit.kill();
      };
      if(unit.moving()){unit.lookAt(unit.vel().angle());};
  }
}
