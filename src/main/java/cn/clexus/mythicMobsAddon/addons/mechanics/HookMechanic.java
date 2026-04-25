package cn.clexus.mythicMobsAddon.addons.mechanics;

import cn.clexus.mythicMobsAddon.MythicMobsAddon;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderBoolean;
import io.lumine.mythic.api.skills.placeholders.PlaceholderDouble;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillCondition;
import io.lumine.mythic.core.skills.placeholders.PlaceholderContext;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import io.lumine.mythic.core.skills.variables.VariableRegistry;
import org.bukkit.*;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HookMechanic extends Aura implements ITargetedEntitySkill, ITargetedLocationSkill {
    protected Optional<Skill> onHookSkill = Optional.empty();
    protected String onHookSkillName;
    protected Optional<Skill> onFailSkill = Optional.empty();
    protected String onFailSkillName;

    private final PlaceholderDouble chainsPerTick;
    private final PlaceholderString glowColor;
    private final PlaceholderBoolean canHookEntity;
    private final PlaceholderBoolean canHookPlayer;
    private final PlaceholderBoolean canHookBlock;
    private final PlaceholderBoolean pullOnEnd;
    private final PlaceholderDouble pullSpeed;
    private final PlaceholderBoolean reverse;
    private final PlaceholderDouble pullLength;
    private final PlaceholderDouble maxDistance;
    private final PlaceholderDouble size;
    private final PlaceholderDouble timeout;
    private final PlaceholderDouble successChance;
    private final PlaceholderDouble failChance;
    private final PlaceholderBoolean failIsEnd;
    private final PlaceholderBoolean livingOnly;
    private final PlaceholderBoolean ignoreArmorstands;
    private PlaceholderBoolean tickAfterHook;
    private List<SkillCondition> targetConditions;

    public HookMechanic(SkillExecutor manager, File file, String line, MythicLineConfig mlc) {
        super(manager, file, line, mlc);
        this.chainsPerTick = PlaceholderDouble.of(mlc.getString(new String[]{"chainspertick", "speed"}, "4.0"));
        this.glowColor = PlaceholderString.of(mlc.getString(new String[]{"glowcolor", "glow", "color"}, "none"));
        this.canHookEntity = PlaceholderBoolean.of(mlc.getString(new String[]{"canhookentity", "che"}, "true"));
        this.canHookPlayer = PlaceholderBoolean.of(mlc.getString(new String[]{"canhookplayer", "chp"}, "false"));
        this.canHookBlock = PlaceholderBoolean.of(mlc.getString(new String[]{"canhookblock", "chb"}, "true"));
        this.pullOnEnd = PlaceholderBoolean.of(mlc.getString(new String[]{"pullonend", "poe"}, "true"));
        this.pullSpeed = PlaceholderDouble.of(mlc.getString(new String[]{"pullspeed", "ps"}, "1.0"));
        this.reverse = PlaceholderBoolean.of(mlc.getString(new String[]{"reverse", "r"}, "false"));
        this.pullLength = PlaceholderDouble.of(mlc.getString(new String[]{"pulllength", "pl"}, "15.0"));
        this.maxDistance = PlaceholderDouble.of(mlc.getString(new String[]{"maxdistance", "md"}, "20.0"));
        this.size = PlaceholderDouble.of(mlc.getString(new String[]{"size"}, "0.5"));
        this.timeout = PlaceholderDouble.of(mlc.getString(new String[]{"timeout"}, "100"));// 单位：Tick
        this.tickAfterHook = PlaceholderBoolean.of(mlc.getString(new String[]{"tickafterhook", "tah"}, "true"));
        this.onHookSkillName = mlc.getString(new String[]{"onhook", "oh"});
        this.onFailSkillName = mlc.getString(new String[]{"onfail", "of"});
        this.successChance = PlaceholderDouble.of(mlc.getString(new String[]{"successchance", "sc"}, "1"));
        this.failChance = PlaceholderDouble.of(mlc.getString(new String[]{"failchance", "fc"}, "0"));
        this.failIsEnd = PlaceholderBoolean.of(mlc.getString(new String[]{"failisend", "fie"}, "false"));
        this.livingOnly = PlaceholderBoolean.of(mlc.getString(new String[]{"livingonly", "lo"}, "true"));
        this.ignoreArmorstands = PlaceholderBoolean.of(mlc.getString(new String[]{"ignorearmorstands", "ia"}, "true"));

        this.forceSync = true;
        this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;

        String condStr = mlc.getString(new String[]{"condition", "c"});
        if (condStr != null) {
            this.targetConditions = this.getPlugin().getSkillManager().getConditions(condStr);
        }

        if (this.onHookSkillName != null) {
            this.onHookSkill = this.getManager().getSkill(file, this, this.onHookSkillName);
        }
        if (this.onFailSkillName != null) {
            this.onFailSkill = this.getManager().getSkill(file, this, this.onFailSkillName);
        }
        if (this.onStartSkillName != null) {
            this.onStartSkill = this.getManager().getSkill(file, this, this.onStartSkillName);
        }
        if (this.onTickSkillName != null) {
            this.onTickSkill = this.getManager().getSkill(file, this, this.onTickSkillName);
        }
        if (this.onEndSkillName != null) {
            this.onEndSkill = this.getManager().getSkill(file, this, this.onEndSkillName);
        }
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata skillMetadata, AbstractEntity target) {
        new HookTracker(skillMetadata, target.getBukkitEntity(), null);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult castAtLocation(SkillMetadata skillMetadata, AbstractLocation target) {
        new HookTracker(skillMetadata, null, BukkitAdapter.adapt(target));
        return SkillResult.SUCCESS;
    }

    public class HookTracker extends Aura.AuraTracker {
        private final Entity caster;
        private final List<BlockDisplay> links = new ArrayList<>();
        private final Orientable blockData;

        private Entity targetEntity = null;
        private Location targetLocation = null;
        private Entity hookedEntity = null;
        private Location hookedBlockLoc = null;
        private boolean isHooked = false;
        private double currentSearchDist = 0;
        private Location startLocation = null;
        private Vector startDir = null;

        public HookTracker(SkillMetadata data, Entity target, Location loc) {
            super(data);
            this.caster = data.getCaster().getEntity().getBukkitEntity();
            this.targetEntity = target;
            this.targetLocation = loc;
            Material mat = Material.getMaterial("CHAIN");
            if (mat == null) {
                mat = Material.getMaterial("IRON_CHAIN");
            }
            if (mat == null) {
                throw new IllegalArgumentException("Material not found, this should not be possible unless the server version is extremely old");
            }
            this.blockData = (Orientable) mat.createBlockData();
            this.blockData.setAxis(Axis.Z);
            this.start();
        }

        private PlaceholderContext casterContext() {
            return PlaceholderContext.builder().meta(this.skillMetadata).entity(BukkitAdapter.adapt(caster)).build();
        }

        @Override
        public void run() {
            PlaceholderContext context = casterContext();
            if (tickAfterHook.get(context)) {
                if (isHooked) {
                    this.ticksRemaining -= this.interval;
                }
            } else {
                this.ticksRemaining -= this.interval;
            }
            if (!this.hasEnded && !this.isValid()) {
                this.terminate();
            } else {
                if (!links.isEmpty()) {
                    this.skillMetadata.setOrigin(BukkitAdapter.adapt(links.getLast().getLocation()));
                }

                this.auraTick();
            }

        }

        @Override
        public void auraStart() {
            super.auraStart();
        }

        @Override
        public void auraTick() {
            if (!caster.isValid() || (hookedEntity != null && !hookedEntity.isValid())) {
                this.auraStop();
                return;
            }
            PlaceholderContext context = casterContext();
            double failRate = failChance.get(context);

            if (!isHooked) {
                extendChain();
            } else {
                updateConnectedChain();
                applyPullLogic();
                if (Math.random() < failRate) {
                    VariableRegistry vars = skillMetadata.getVariables();
                    vars.putString("fail-cause", "TICK_FAILED");
                    this.execute(onFailSkill, skillMetadata);
                    this.auraStop();
                    return;
                }
            }
            if (tickAfterHook.get(context)) {
                if (isHooked) {
                    this.execute(HookMechanic.this.onTickSkill, this.skillMetadata);
                }
            } else {
                this.execute(HookMechanic.this.onTickSkill, this.skillMetadata);
            }
        }

        private void extendChain() {
            if (startLocation == null) {
                startLocation = caster instanceof HumanEntity ? getHandLocation((HumanEntity) caster) : caster.getLocation();
                if (targetEntity != null) {
                    startDir = targetEntity.getLocation().toVector().subtract(startLocation.toVector()).normalize();
                } else if (targetLocation != null) {
                    startDir = targetLocation.toVector().subtract(startLocation.toVector()).normalize();
                } else {
                    startDir = startLocation.getDirection().normalize();
                }
            }
            PlaceholderContext context = casterContext();
            double speed = chainsPerTick.get(context);
            double maxDist = maxDistance.get(context);
            double hitBox = size.get(context);
            VariableRegistry vars = skillMetadata.getVariables();

            for (int i = 0; i < speed; i++) {
                currentSearchDist += 0.8;
                if (currentSearchDist > maxDist) {
                    vars.putString("fail-cause", "OUT_OF_RANGE");
                    this.execute(onFailSkill, skillMetadata);
                    this.auraStop();
                    return;
                }

                Location currentPos = startLocation.clone().add(startDir.clone().multiply(currentSearchDist));
                double successRate = successChance.get(context);

                if (canHookBlock.get(context)) {
                    var result = caster.getWorld().rayTraceBlocks(currentPos, startDir, 0.8, FluidCollisionMode.NEVER, true, e -> {
                        if (targetConditions != null) {
                            for (var cond : targetConditions) {
                                if (!cond.evaluateToLocation(skillMetadata.getCaster().getEntity(), BukkitAdapter.adapt(e.getLocation())))
                                    return false;
                            }
                        }
                        return true;
                    });

                    if (result != null && result.getHitBlock() != null) {
                        if (Math.random() >= successRate) {
                            vars.putString("fail-cause", "NOT_SUCCESSFUL");
                            this.execute(onFailSkill, skillMetadata);
                            this.auraStop();
                            return;
                        }
                        var pos = result.getHitPosition();
                        hookedBlockLoc = new Location(result.getHitBlock().getWorld(), pos.getX(), pos.getY(), pos.getZ());
                        successHook();
                        return;
                    }
                }

                // 碰撞检测：实体
                if (canHookEntity.get(context)) {
                    var result = caster.getWorld().rayTraceEntities(currentPos, startDir, 0.8, hitBox, e -> {
                        if (e.equals(caster)
                                || (livingOnly.get(context) && !(e instanceof LivingEntity))
                                || (ignoreArmorstands.get(context) && e instanceof ArmorStand))
                            return false;
                        if (!canHookPlayer.get(context) && e instanceof Player)
                            return false;

                        // 条件判定
                        if (targetConditions != null) {
                            for (var cond : targetConditions) {
                                if (!cond.evaluateToEntity(skillMetadata, BukkitAdapter.adapt(e))) return false;
                            }
                        }
                        return true;
                    });

                    if (result != null && result.getHitEntity() != null) {
                        if (Math.random() >= successRate) {
                            vars.putString("fail-cause", "NOT_SUCCESSFUL");
                            this.execute(onFailSkill, skillMetadata);
                            this.auraStop();
                            return;
                        }
                        hookedEntity = result.getHitEntity();
                        successHook();
                        return;
                    }
                }

                // 生成视觉链条
                spawnLink(currentPos, startDir);
            }
        }

        private void successHook() {
            isHooked = true;
            if (hookedEntity != null) skillMetadata.setEntityTarget(BukkitAdapter.adapt(hookedEntity));
            if (hookedBlockLoc != null) skillMetadata.setLocationTarget(BukkitAdapter.adapt(hookedBlockLoc));
            this.execute(onHookSkill, skillMetadata);
            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1f, 1.5f);
        }

        private void applyPullLogic() {
            PlaceholderContext context = casterContext();
            boolean reversePull = reverse.get(context);
            Location anchor;
            Entity subject;
            if (hookedBlockLoc != null) {
                anchor = hookedBlockLoc;
                subject = caster;
            } else if (hookedEntity == null) {
                return;
            } else if (reversePull) {
                anchor = hookedEntity.getLocation();
                subject = caster;
            } else {
                anchor = caster.getLocation();
                subject = hookedEntity;
            }

            double dist = subject.getLocation().distance(anchor);
            double limit = pullLength.get(context);

            if (dist > limit) {
                double speedMult = pullSpeed.get(context);
                pull(subject, anchor, (dist - limit) * speedMult * 0.1);
            }
        }

        private void updateConnectedChain() {
            Location start = caster instanceof HumanEntity ? getHandLocation((HumanEntity) caster) : caster.getLocation();
            Location end = (hookedEntity != null) ? hookedEntity.getLocation().add(0, hookedEntity.getHeight() / 2, 0) : hookedBlockLoc;

            Vector dir = end.toVector().subtract(start.toVector());
            double dist = dir.length();
            dir.normalize();

            int needed = (int) Math.ceil(dist / 0.8);

            while (links.size() < needed) {
                spawnLink(start.clone(), dir);
            }
            while (links.size() > needed && !links.isEmpty()) {
                links.removeLast().remove();
            }

            for (int i = 0; i < links.size(); i++) {
                Location pos = start.clone().add(dir.clone().multiply(i * 0.8));
                pos.setDirection(dir);
                links.get(i).teleport(pos);
            }
        }

        private void spawnLink(Location loc, Vector dir) {
            loc.getWorld().playSound(loc, Sound.BLOCK_CHAIN_PLACE, 1f, 1f);
            BlockDisplay display = (BlockDisplay) caster.getWorld().spawnEntity(loc.setDirection(dir), EntityType.BLOCK_DISPLAY);
            display.setPersistent(false);
            display.setBlock(blockData);
            display.setTeleportDuration(1);
            display.setInterpolationDelay(1);
            String hex = glowColor.get(casterContext());
            if (!hex.equals("none")) {
                display.setGlowColorOverride(Color.fromRGB(
                        Integer.valueOf(hex.substring(1, 3), 16),
                        Integer.valueOf(hex.substring(3, 5), 16),
                        Integer.valueOf(hex.substring(5, 7), 16)));
                display.setGlowing(true);
            }

            Transformation t = display.getTransformation();
            t.getScale().set(0.8f, 0.8f, 0.8f);
            t.getTranslation().set(-0.4, -0.4, -0.4);
            display.setTransformation(t);

            links.add(display);
        }

        private void pull(Entity entity, Location anchor, double strength) {
            Vector direction = anchor.toVector().subtract(entity.getLocation().toVector()).normalize();
            Vector vel = entity.getVelocity();
            double dot = vel.dot(direction);
            if (dot < 0) strength -= dot * 0.5;
            entity.setVelocity(vel.add(direction.multiply(strength)));
        }

        private Location getHandLocation(LivingEntity entity) {
            Location loc = entity.getEyeLocation();
            Vector dir = loc.getDirection().normalize();
            Vector right = dir.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            return loc.add(right.multiply(0.4)).add(dir.multiply(0.5)).add(0, -0.4, 0);
        }

        @Override
        public void auraStop() {
            PlaceholderContext context = casterContext();
            boolean fie = failIsEnd.get(context);
            var vars = skillMetadata.getVariables();
            if (!fie && vars.has("fail-cause")) {
                links.forEach(Entity::remove);
                links.clear();
                HookMechanic.this.tickAfterHook = PlaceholderBoolean.of("false");
                HookTracker.this.ticksRemaining = 0;
                return;
            }
            if (pullOnEnd.get(context) && isHooked) {
                long start = System.currentTimeMillis();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PlaceholderContext pullContext = HookTracker.this.casterContext();
                        if (System.currentTimeMillis() - start >= timeout.get(pullContext) * 50) {
                            finish();
                            return;
                        }

                        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_CHAIN_BREAK, 2, 2);
                        if (hookedBlockLoc != null) {
                            if (caster.getLocation().distance(hookedBlockLoc) >= 6) {
                                pull(caster, hookedBlockLoc, 1);
                                updateConnectedChain();
                            } else {
                                finish();
                            }
                        } else if (hookedEntity != null && hookedEntity.isValid()) {
                            if (!caster.getBoundingBox().overlaps(hookedEntity.getBoundingBox())) {
                                if (reverse.get(pullContext)) {
                                    pull(caster, hookedEntity.getLocation(), 1);
                                } else {
                                    pull(hookedEntity, caster.getLocation(), 1);
                                }
                                updateConnectedChain();
                            } else {
                                finish();
                            }
                        } else {
                            finish();
                        }
                    }

                    public void finish() {
                        links.forEach(Entity::remove);
                        links.clear();
                        HookMechanic.this.tickAfterHook = PlaceholderBoolean.of("false");
                        HookTracker.this.ticksRemaining = 0;
                        HookTracker.super.auraStop();
                        cancel();
                    }
                }.runTaskTimer(MythicMobsAddon.plugin, 0L, 1L);
            } else {
                links.forEach(Entity::remove);
                links.clear();
                HookMechanic.this.tickAfterHook = PlaceholderBoolean.of("false");
                HookTracker.this.ticksRemaining = 0;
                super.auraStop();
            }
        }
    }
}