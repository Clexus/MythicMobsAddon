package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderBoolean;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
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
            this.activeCooldown = onActiveCooldown.get(skillMetadata, BukkitAdapter.adapt(owner));
            this.offCooldown = offActiveCooldown.get(skillMetadata, BukkitAdapter.adapt(owner));
            this.start();
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
            try {
                item = Bukkit.getItemFactory().createItemStack(itemString.get(skillMetadata, BukkitAdapter.adapt(owner)));
            } catch (Exception e) {
                var id = itemId.get(skillMetadata, BukkitAdapter.adapt(owner));
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
            double radius = searchRadius.get(skillMetadata, absOwner);
            LivingEntity newTarget;
            LivingEntity temp = null;
            Location ownerLoc = owner.getLocation();

            for (Entity e : owner.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof LivingEntity living) || e instanceof ArmorStand) continue;
                if(e.equals(owner) && !targetOwner.get(skillMetadata, absOwner)) continue;
                if (!targetNonMythics.get(skillMetadata, absOwner) && !MythicBukkit.inst().getAPIHelper().isMythicMob(e)) continue;
                if (living instanceof Player && !targetPlayers.get(skillMetadata, absOwner)) continue;

                if (temp == null ||
                        living.getLocation().distanceSquared(ownerLoc)
                                < temp.getLocation().distanceSquared(ownerLoc)) {

                    temp = living;
                }
            }

            newTarget = temp;
            if (!fixTarget.get(skillMetadata, absOwner)) {
                target = newTarget;
            }
            if (target != null && !target.isValid()) {
                target = null;
            }
            var dR = discardRadius.get(skillMetadata, absOwner);
            if (target != null && target.getLocation().distanceSquared(owner.getLocation()) > dR * dR) {
                target = null;
            }

            if (target == null && newTarget != null) {
                target = newTarget;
            }

            if (target != null) {
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
                var mtt = moveToTarget.get(skillMetadata, absOwner);
                boolean keepLoc = keepLocationAfterMoving.get(skillMetadata, absOwner);

                if(keepLocation == null){
                    keepLocation = target.getEyeLocation();
                }

                Location tLoc = keepLoc ? keepLocation : target.getEyeLocation();

                Vector lookDir = tLoc.toVector().subtract(nowLoc.toVector());
                if (lookDir.lengthSquared() == 0) {
                    return;
                }
                lookDir.normalize();

                if (mtt) {
                    double distance = distanceFromTarget.get(skillMetadata, absOwner);
                    Location desired = tLoc.clone().subtract(lookDir.clone().multiply(distance));

                    Location mid = nowLoc.clone()
                            .add(desired.toVector().subtract(nowLoc.toVector()).multiply(0.2));

                    mid.setDirection(tLoc.toVector().subtract(mid.toVector()));
                    display.teleport(mid);

                } else {
                    Location idle = getIdlePos();
                    Location mid = nowLoc.clone()
                            .add(idle.toVector().subtract(nowLoc.toVector()).multiply(0.2));

                    mid.setDirection(tLoc.toVector().subtract(mid.toVector()));
                    display.teleport(mid);
                }

            } else {
                Location idle = getIdlePos();
                Location mid = nowLoc.clone()
                        .add(idle.toVector().subtract(nowLoc.toVector()).multiply(0.2));

                mid.setDirection(owner.getLocation().getDirection().multiply(-1));
                display.teleport(mid);
            }

        }
    }
}
