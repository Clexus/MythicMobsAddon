package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.adapters.AbstractVector;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.ITargetedLocationSkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.placeholders.PlaceholderFloat;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.numbers.Numbers;
import io.lumine.mythic.core.logging.MythicLogger;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.projectiles.Projectile;
import io.lumine.mythic.core.skills.projectiles.ProjectileBulletType;
import io.lumine.mythic.core.utils.BlockUtil;
import io.lumine.mythic.core.utils.MythicUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class ParabolicMechanic extends Projectile implements ITargetedEntitySkill, ITargetedLocationSkill {
    protected PlaceholderFloat projectileGravity;
    protected PlaceholderFloat topXOffset;
    protected PlaceholderFloat topYOffset;

    public ParabolicMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.projectileGravity = mlc.getPlaceholderFloat(new String[]{"gravity", "g"}, 0.1f);
        this.topXOffset = mlc.getPlaceholderFloat(new String[]{"topXOffset", "txo"}, 0.5f);
        this.topYOffset = mlc.getPlaceholderFloat(new String[]{"topYOffset", "tyo"}, 5.0f);
    }

    @Override
    public SkillResult castAtLocation(SkillMetadata data, AbstractLocation target) {
        try {
            new ParabolicMechanicTracker(data, target.clone());
            return SkillResult.SUCCESS;
        } catch (Exception ex) {
            MythicLogger.error("An error occurred executing a Parabolic Mechanic", ex);
            return SkillResult.ERROR;
        }
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        return this.castAtLocation(data, target.getLocation().add(0.0, target.getEyeHeight() / 2.0, 0.0));
    }


    public class ParabolicMechanicTracker
            extends Projectile.ProjectileTracker {
        private float gravity;
        private AbstractLocation target;

        public ParabolicMechanicTracker(SkillMetadata data, AbstractLocation target) {
            super(data, target);
            this.gravity = 0.0f;
            this.target = target;
            this.start();
        }

        @Override
        public void projectileStart() {
            if (ParabolicMechanic.this.sourceIsOrigin) {
                this.startLocation = this.data.getOrigin().clone();
            } else {
                this.startLocation = this.data.getCaster().getEntity().getLocation().clone();
            }

            double syo = ParabolicMechanic.this.startYOffset.get(this.data);
            if (syo != 0.0) this.startLocation.setY(this.startLocation.getY() + syo);

            double sfo = ParabolicMechanic.this.startForwardOffset.get(this.data);
            double sso = ParabolicMechanic.this.startSideOffset.get(this.data);
            if (sfo != 0.0 || sso != 0.0) {
                this.startLocation = MythicUtil.move(ParabolicMechanic.this.faulty, this.startLocation, -sfo, 0, sso);
            }

            this.previousLocation = this.startLocation.clone();
            this.currentLocation = this.startLocation.clone();

            double dx = this.target.getX() - this.startLocation.getX();
            double dz = this.target.getZ() - this.startLocation.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            float tyo = ParabolicMechanic.this.topYOffset.get(this.data);
            double topY = Math.max(this.startLocation.getY(), this.target.getY()) + tyo;
            double topHeight = topY - this.startLocation.getY();

            this.gravity = ParabolicMechanic.this.projectileGravity.get(this.data) / this.ticksPerSecond;
            if (ParabolicMechanic.this.tickInterpolation > 0) this.gravity /= (ParabolicMechanic.this.tickInterpolation + 1);

            double v0y = Math.sqrt(2.0 * this.gravity * topHeight);

            double timeToTop = v0y / this.gravity;
            double fallHeight = topY - this.target.getY();
            double timeFall = Math.sqrt(2.0 * fallHeight / this.gravity);
            double totalTime = timeToTop + timeFall;

            double v0xz = horizontalDistance / totalTime;
            AbstractVector horizontalDir = new AbstractVector((float) dx, 0f, (float) dz).normalize();
            this.currentVelocity = horizontalDir.multiply((float) v0xz);
            this.currentVelocity.setY((float) v0y);

            if (ParabolicMechanic.this.projectileVelocityHorizOffset != null || ParabolicMechanic.this.projectileVelocityHorizNoise > 0.0f) {
                float noise = 0.0f;
                if (ParabolicMechanic.this.projectileVelocityHorizNoise > 0.0f)
                    noise = (float) (ParabolicMechanic.this.projectileVelocityHorizNoiseBase
                            + Numbers.randomDouble() * ParabolicMechanic.this.projectileVelocityHorizNoise);
                if (ParabolicMechanic.this.projectileVelocityHorizOffset != null)
                    noise += ParabolicMechanic.this.projectileVelocityHorizOffset.get(this.data).getDegrees();
                this.currentVelocity.rotate(noise);
            }

            double eso = ParabolicMechanic.this.endSideOffset.get(this.data);
            if (eso != 0.0) {
                this.target.setDirection(this.startLocation.getDirection());
                this.target = MythicUtil.move(ParabolicMechanic.this.faulty, this.target, 0.0, 0.0, eso);
            }

            if (ParabolicMechanic.this.bullet != null) {
                this.bullet = ParabolicMechanic.this.bullet.create(this, null);
            }
        }

        @Override
        public void projectileMove() {
            this.previousLocation = this.currentLocation.clone();
            if (this.gravity != 0.0) {
                this.currentVelocity.setY(this.currentVelocity.getY() - this.gravity);
            }
            this.currentLocation.add(this.currentVelocity);

            if (ParabolicMechanic.this.stopOnHitGround && !BlockUtil.isPathable(BukkitAdapter.adapt(this.currentLocation).getBlock(), this)) {
                if (ParabolicMechanic.this.onHitBlockSkill.isPresent() && ParabolicMechanic.this.onHitBlockSkill.get().isUsable(this.data)) {
                    SkillMetadata sData = this.data.deepClone();
                    AbstractLocation location = ParabolicMechanic.this.bulletType.isPresent() &&
                            ParabolicMechanic.this.bulletType.get() == ProjectileBulletType.ARROW
                            ? this.previousLocation.clone()
                            : this.currentLocation.clone();
                    ParabolicMechanic.this.onHitBlockSkill.get().execute(sData.setOrigin(location).setLocationTarget(location));
                }
                this.currentLocation = this.previousLocation;
                this.terminate();
            }
        }


        @Override
        public void projectileTick() {
            if (ParabolicMechanic.this.onTickSkill.isPresent() && ParabolicMechanic.this.onTickSkill.get().isUsable(this.data)) {
                SkillMetadata sData = this.data.deepClone();
                AbstractLocation location = ParabolicMechanic.this.bulletType.isPresent() && ParabolicMechanic.this.bulletType.get() == ProjectileBulletType.ARROW ? this.previousLocation.clone() : this.currentLocation.clone();
                ArrayList<AbstractLocation> targets = new ArrayList<>();
                targets.add(location);
                sData.setLocationTargets(targets);
                sData.setOrigin(location);
                ParabolicMechanic.this.onTickSkill.get().execute(sData);
            }
            this.evaluateTargetsInBB();
            if (!this.targets.isEmpty()) {
                this.doHit(this.targets.clone());
                for (AbstractEntity target : this.targets) {
                    if (!ParabolicMechanic.this.canStopAtEntity(this.data, target)) continue;
                    this.terminate();
                    break;
                }
            }
            this.targets.clear();
        }

        private void doHit(Collection<AbstractEntity> targets) {
            this.hasHitEntity = true;
            if (ParabolicMechanic.this.onHitSkill.isPresent()) {
                SkillMetadata sData = this.data.deepClone();
                sData.setEntityTargets(targets);
                sData.setOrigin(this.currentLocation.clone());
                if (ParabolicMechanic.this.onHitSkill.get().isUsable(sData)) {
                    ParabolicMechanic.this.onHitSkill.get().execute(sData);
                }
            }
        }

        @Override
        public void setCancelled() {
            this.terminate();
        }

        @Override
        public boolean getCancelled() {
            return this.components.hasTerminated();
        }
    }
}
