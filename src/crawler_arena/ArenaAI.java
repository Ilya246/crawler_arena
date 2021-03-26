package ArenaAI;

import arc.math.geom.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;
import mindustry.ai.types.*;
import mindustry.ai.Pathfinder;
import arc.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.async.*;
import arc.math.Mathf;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class ArenaAI extends GroundAI{
  static boolean blockedByBlock;

  @Override
  public void updateUnit(){

      if(Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)){
          target = null;
      }

      if(retarget()){
          target = target(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
      }

      Building core = unit.closestEnemyCore();

      boolean rotate = false, shoot = false, moveToTarget = false;

      if(!Units.invalidateTarget(target, unit, unit.range()) && unit.hasWeapons()){
          rotate = true;
          shoot = unit.within(target, unit.type.weapons.first().bullet.range() +
              (target instanceof Building ? 1.5f * Vars.tilesize / 2f : ((Hitboxc)target).hitSize() / 2f));

          if(unit.type.hasWeapons()){
              unit.aimLook(Predict.intercept(unit, target, unit.type.weapons.first().bullet.speed));
          };
      };
      moveToTarget = true;
      if(target != null){
          unit.moveAt(vec.set(target).sub(unit).limit(unit.speed()));
          if(unit.moving()) unit.lookAt(unit.vel().angle());
          unit.controlWeapons(rotate, shoot);
      };
  }

  @Override
  public Teamc target(float x, float y, float range, boolean air, boolean ground){
      return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground &&
          !(t.block instanceof Conveyor || t.block instanceof Conduit)); //do not target conveyors/conduits
  }
}
