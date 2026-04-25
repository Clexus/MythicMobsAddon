package cn.clexus.mythicMobsAddon.addons.mechanics;

import com.destroystokyo.paper.MaterialTags;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderBoolean;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.SkillCondition;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class FunnelMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onActiveTickSkill = Optional.empty();
    protected String onActiveTickSkillName;
    protected Optional<Skill> offActiveTickSkill = Optional.empty();
    protected String offActiveTickSkillName;
    private final PlaceholderDouble searchRadius;
    private final PlaceholderString itemString;
    private final PlaceholderString itemId;
    private final PlaceholderBoolean fixTarget;
    private final PlaceholderDouble discardRadius;
    private final PlaceholderDouble distanceFromTarget;
    private final PlaceholderInt onActiveCooldown;
    private final PlaceholderInt offActiveCooldown;
    private final PlaceholderBoolean moveToTarget;
    private final PlaceholderBoolean targetPlayers;
    private final PlaceholderBoolean targetOwner;
    private final PlaceholderBoolean targetNonMythics;
    private final PlaceholderBoolean keepLocationAfterMoving;
    private List<SkillCondition> targetConditions;
    private List<SkillCondition> casterConditions;

    public FunnelMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.onActiveTickSkillName = mlc.getString(new String[]{"onactivetickskill", "onactivetick", "oat"});
        this.offActiveTickSkillName = mlc.getString(new String[]{"offactivetickskill", "offactivetick", "fat"});
        this.searchRadius = PlaceholderDouble.of(mlc.getString(new String[]{"searchradius", "sr"}, "15"));
        this.fixTarget = PlaceholderBoolean.of(mlc.getString(new String[]{"fixtarget", "ft"}, "true"));
        this.targetPlayers = PlaceholderBoolean.of(mlc.getString(new String[]{"targetplayers", "targetplayer", "tp"}, "false"));
        this.targetNonMythics = PlaceholderBoolean.of(mlc.getString(new String[]{"targetnonmythics", "tnm"}, "false"));
        this.keepLocationAfterMoving = PlaceholderBoolean.of(mlc.getString(new String[]{"keeplocationaftermoving", "klam"}, "false"));
        this.itemString = PlaceholderString.of(mlc.getString(new String[]{"itemstring", "is"}, "stone"));
        this.itemId = PlaceholderString.of(mlc.getString(new String[]{"itemId", "id"}, "stone"));
        this.discardRadius = PlaceholderDouble.of(mlc.getString(new String[]{"discardradius", "dr"}, "15"));
        this.distanceFromTarget = PlaceholderDouble.of(mlc.getString(new String[]{"distancefromtarget", "dft"}, "5"));
        this.moveToTarget = PlaceholderBoolean.of(mlc.getString(new String[]{"movetotarget", "mtt"}, "true"));
        this.targetOwner = PlaceholderBoolean.of(mlc.getString(new String[]{"targetowner", "to"}, "false"));
        this.onActiveCooldown = PlaceholderInt.of(mlc.getString(new String[]{"onactivecooldown", "oac"}, "0"));
        this.offActiveCooldown = PlaceholderInt.of(mlc.getString(new String[]{"offactivecooldown", "fac"}, "0"));
        String targetConStr = mlc.getString(new String[]{"targetconditions", "targetcondition", "targetcond", "targetcon", "tc"});
        String casterConStr = mlc.getString(new String[]{"conditions", "condition", "cond", "con", "c"});
        if (targetConStr != null) {
            this.targetConditions = this.getPlugin().getSkillManager().getConditions(targetConStr);
        }
        if (casterConStr != null) {
            this.casterConditions = this.getPlugin().getSkillManager().getConditions(casterConStr);
        }
        this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;
        this.forceSync = true;
        this.getManager().queueSecondPass(() -> {
            if (this.onActiveTickSkillName != null) {
                this.onActiveTickSkill = this.getManager().getSkill(file, this, this.onActiveTickSkillName);
            }
            if (this.offActiveTickSkillName != null) {
                this.offActiveTickSkill = this.getManager().getSkill(file, this, this.offActiveTickSkillName);
            }
        });
    }


    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity target) {
        if (!(target.getBukkitEntity() instanceof LivingEntity livingEntity)) {
            return SkillResult.INVALID_TARGET;
        }
        new FunnelTracker(skillMetadata, livingEntity);
        return SkillResult.SUCCESS;
    }

    public class FunnelTracker extends Aura.AuraTracker {

        private LivingEntity target;
        private final LivingEntity owner;
        private ItemDisplay display;
        private final double rightOffset;
        private final double upOffset;
        private int cooldownA = 0;
        private int cooldownF = 0;
        private final int activeCooldown;
        private final int offCooldown;
        private Location keepLocation = null;

        public FunnelTracker(SkillMetadata data, LivingEntity owner) {
            super(data);
            this.owner = owner;
            this.rightOffset = 2 * Math.random() - 1;
            this.upOffset = 2 * Math.random() - 1;
            PlaceholderContext context = ownerContext();
            this.activeCooldown = onActiveCooldown.get(context);
            this.offCooldown = offActiveCooldown.get(context);
            this.start();
        }

        private PlaceholderContext ownerContext() {
            return PlaceholderContext.builder().meta(this.skillMetadata).entity(BukkitAdapter.adapt(owner)).build();
        }

        private Location getIdlePos() {
            Location base = owner.getEyeLocation();
            Vector dir = base.getDirection().normalize();
            Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            return base.add(dir.multiply(-1)).add(right.multiply(rightOffset)).add(0, upOffset, 0);
        }

        @Override
        public void auraStart() {
            super.auraStart();
            Location spawn = getIdlePos();
            display = (ItemDisplay) owner.getWorld().spawnEntity(spawn, EntityType.ITEM_DISPLAY);
            ItemStack item;
            PlaceholderContext context = ownerContext();
            try {
                item = Bukkit.getItemFactory().createItemStack(itemString.get(context));
            } catch (Exception e) {
                var id = itemId.get(context);
                item = MythicBukkit.inst().getItemManager().getItemStack(id);
                if(item == null) {
                    throw new IllegalArgumentException("FunnelMechanic: Invalid itemId:" + id);
                }
            }
            display.setItemStack(item);
            display.setPersistent(false);
            display.setTeleportDuration(2);
        }

        @Override
        public void auraStop() {
            super.auraStop();
            if (display != null && display.isValid()) {
                display.remove();
            }
        }

        @Override
        public void auraTick() {
            if (!owner.isValid()) {
                display.remove();
                return;
            }
            cooldownA--;
            cooldownF--;
            skillMetadata.setOrigin(BukkitAdapter.adapt(display.getLocation()));
            Player p = owner instanceof Player player ? player : null;
            if (p != null) {
                if (!p.isOnline()) return;
            }

            AbstractEntity absOwner = BukkitAdapter.adapt(owner);
            PlaceholderContext context = ownerContext();
            double radius = searchRadius.get(context);
            LivingEntity temp = null;
            Location ownerLoc = owner.getLocation();

            out: for (Entity e : owner.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity living) || e instanceof ArmorStand) continue;
                if(e.equals(owner) && !targetOwner.get(context)) continue;
                if (!targetNonMythics.get(context) && !MythicBukkit.inst().getAPIHelper().isMythicMob(e)) continue;
                if (living instanceof Player && !targetPlayers.get(context)) continue;
                if (targetConditions != null && !targetConditions.isEmpty()) {
                    for(SkillCondition condition : targetConditions) {
                        if (!condition.evaluateToEntity(skillMetadata, BukkitAdapter.adapt(living))) {
                            continue out;
                        }
                    }
                }
                if (casterConditions != null && !casterConditions.isEmpty()) {
                    for(SkillCondition condition : casterConditions) {
                        if (!condition.evaluateToEntity(skillMetadata, BukkitAdapter.adapt(owner))) {
                            continue out;
                        }
                    }
                }

                if (temp == null ||
                        living.getLocation().distanceSquared(ownerLoc)
                                < temp.getLocation().distanceSquared(ownerLoc)) {

                    temp = living;
                }
            }

            LivingEntity candidate = temp;
            LivingEntity finalTarget = target;

            // 如果不锁定目标，优先使用新目标
            if (!fixTarget.get(context)) {
                finalTarget = candidate;
            }

            // 校验目标合法性 & 距离
            if (finalTarget != null) {
                if (!finalTarget.isValid()) {
                    finalTarget = null;
                } else {
                    double dR = discardRadius.get(context);
                    if (finalTarget.getLocation().distanceSquared(owner.getLocation()) > dR * dR) {
                        finalTarget = null;
                    }
                    if (targetConditions != null && !targetConditions.isEmpty() && candidate != null) {
                        for(SkillCondition condition : targetConditions) {
                            if (!condition.evaluateToEntity(skillMetadata, BukkitAdapter.adapt(candidate))) {
                                finalTarget = null;
                                break;
                            }
                        }
                    }
                    if (casterConditions != null && !casterConditions.isEmpty()) {
                        for(SkillCondition condition : casterConditions) {
                            if (!condition.evaluateToEntity(skillMetadata, BukkitAdapter.adapt(owner))) {
                                finalTarget = null;
                                break;
                            }
                        }
                    }
                }
            }

            // 若当前目标被清掉，允许回退到新目标
            if (finalTarget == null && candidate != null) {
                finalTarget = candidate;
            }

            // 同步状态
            target = finalTarget;

            if (target == null) {
                keepLocation = null;
            } else {
                skillMetadata.setEntityTarget(BukkitAdapter.adapt(target));
            }


            super.auraTick();
            if (offActiveTickSkill.isPresent() && (onActiveTickSkill.isEmpty() || target == null || !target.isValid())) {
                if (cooldownF <= 0) {
                    this.execute(FunnelMechanic.this.offActiveTickSkill, this.skillMetadata);
                    cooldownF = offCooldown;
                }
            } else {
                if (cooldownA <= 0) {
                    if (target != null && target.isValid()) {
                        this.execute(FunnelMechanic.this.onActiveTickSkill, this.skillMetadata);
                        cooldownA = activeCooldown;
                    }
                }
            }


            Location nowLoc = display.getLocation();

            if (target != null) {
                var mtt = moveToTarget.get(context);
                boolean keepLoc = keepLocationAfterMoving.get(context);

                if (keepLoc) {
                    if (keepLocation == null) {
                        double distance = distanceFromTarget.get(context);

                        Location targetLoc = target.getEyeLocation();
                        Vector dir = targetLoc.toVector().subtract(nowLoc.toVector());

                        if (dir.lengthSquared() > 0) {
                            dir.normalize();
                        }

                        keepLocation = targetLoc.clone().subtract(dir.multiply(distance));
                    }

                    Location fixed = keepLocation.clone();

                    Vector look = target.getEyeLocation().toVector().subtract(fixed.toVector());
                    if (look.lengthSquared() > 0) {
                        fixed.setDirection(look);
                    }
                    if(MaterialTags.SKULLS.isTagged(display.getItemStack())){
                        fixed.setDirection(fixed.getDirection().multiply(-1));
                    }

                    display.teleport(fixed);
                    return;
                }



                Location tLoc = target.getEyeLocation();

                Vector lookDir = tLoc.toVector().subtract(nowLoc.toVector());
                if (lookDir.lengthSquared() == 0) {
                    return;
                }
                lookDir.normalize();

                if (mtt) {
                    double distance = distanceFromTarget.get(context);
                    Location desired = tLoc.clone().subtract(lookDir.clone().multiply(distance));

                    Location mid = nowLoc.clone()
                            .add(desired.toVector().subtract(nowLoc.toVector()).multiply(0.2));

                    mid.setDirection(tLoc.toVector().subtract(mid.toVector()));
                    if(MaterialTags.SKULLS.isTagged(display.getItemStack())){
                        mid.setDirection(mid.getDirection().multiply(-1));
                    }
                    display.teleport(mid);

                } else {
                    Location idle = getIdlePos();
                    Location mid = nowLoc.clone()
                            .add(idle.toVector().subtract(nowLoc.toVector()).multiply(0.2));

                    mid.setDirection(tLoc.toVector().subtract(mid.toVector()));
                    if(MaterialTags.SKULLS.isTagged(display.getItemStack())){
                        mid.setDirection(mid.getDirection().multiply(-1));
                    }
                    display.teleport(mid);
                }

            } else {
                Location idle = getIdlePos();
                Location mid = nowLoc.clone()
                        .add(idle.toVector().subtract(nowLoc.toVector()).multiply(0.2));

                mid.setDirection(owner.getLocation().getDirection().multiply(-1));
                if(MaterialTags.SKULLS.isTagged(display.getItemStack())){
                    mid.setDirection(mid.getDirection().multiply(-1));
                }
                display.teleport(mid);
            }

        }
    }
}
