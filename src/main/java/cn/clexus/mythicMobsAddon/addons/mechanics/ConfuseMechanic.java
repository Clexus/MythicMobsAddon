package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.api.skills.placeholders.PlaceholderInt;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.io.File;
import java.util.*;

public class ConfuseMechanic extends Aura implements ITargetedEntitySkill {

    public ConfuseMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.overwriteAll = false;
        this.overwriteCaster = false;
        this.maxStacks = PlaceholderInt.of("1");
        this.charges = PlaceholderInt.of("0");
        this.auraName = Optional.of(PlaceholderString.of("#confuse"));
        this.forceSync = true;
        this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        List<AbstractEntity> entities = new ArrayList<>(data.getEntityTargets());
        new Tracker(data, target, entities);
        return SkillResult.SUCCESS;
    }

    private class Tracker extends Aura.AuraTracker {
        private HolderState holderState;
        private final List<AbstractEntity> targets;

        public Tracker(SkillMetadata data, AbstractEntity entity, List<AbstractEntity> targets) {
            super(entity, data);
            this.targets = targets;
            this.start();
        }

        @Override
        public void auraStart() {
            if (this.entity.isEmpty()) {
                this.executeAuraSkill(ConfuseMechanic.this.onStartSkill, this.skillMetadata);
                return;
            }

            ActiveMob mob = MythicBukkit.inst().getMobManager().getMythicMobInstance(this.entity.get());
            if (mob == null) {
                this.executeAuraSkill(ConfuseMechanic.this.onStartSkill, this.skillMetadata);
                return;
            }

            this.holderState = new HolderState(mob);

            mob.setOwnerUUID(null);
            mob.setParent(null);
            mob.setFaction(UUID.randomUUID().toString());

            AbstractEntity newTarget = selectNonSelfTarget(targets, this.entity.get().getUniqueId());
            if (newTarget != null) {
                mob.setTarget(newTarget);
            } else {
                mob.resetTarget();
            }

            this.executeAuraSkill(ConfuseMechanic.this.onStartSkill, this.skillMetadata);
        }

        @Override
        public void auraStop() {
            if (this.entity.isEmpty() || this.holderState == null) {
                this.executeAuraSkill(ConfuseMechanic.this.onEndSkill, this.skillMetadata);
                return;
            }

            ActiveMob mob = MythicBukkit.inst().getMobManager().getMythicMobInstance(this.entity.get());
            if (mob == null) {
                this.executeAuraSkill(ConfuseMechanic.this.onEndSkill, this.skillMetadata);
                return;
            }

            mob.setOwnerUUID(this.holderState.ownerUuid);
            mob.setParent(resolveParentCaster(this.holderState.parentUuid));
            if (this.holderState.faction == null || this.holderState.faction.isEmpty()) {
                mob.setFaction(null);
            } else {
                mob.setFaction(this.holderState.faction);
            }
            mob.resetTarget();

            this.executeAuraSkill(ConfuseMechanic.this.onEndSkill, this.skillMetadata);
        }

        private AbstractEntity selectNonSelfTarget(List<AbstractEntity> targets, UUID selfId) {
            if (targets == null || targets.isEmpty()) {
                return null;
            }

            for (AbstractEntity target : targets) {
                if (!target.getBukkitEntity().isValid()) {
                    targets.remove(target);
                    continue;
                }
                if (!target.getUniqueId().equals(selfId)) {
                    return target;
                }
            }

            return null;
        }

        private SkillCaster resolveParentCaster(UUID uuid) {
            if (uuid == null) {
                return null;
            }

            Entity entity = Bukkit.getEntity(uuid);
            if (entity == null) {
                return null;
            }

            return ConfuseMechanic.this.getPlugin().getSkillManager().getCaster(BukkitAdapter.adapt(entity));
        }
    }

    private static final class HolderState {
        private final UUID ownerUuid;
        private final UUID parentUuid;
        private final String faction;

        private HolderState(ActiveMob mob) {
            this.ownerUuid = mob.getOwnerUUID().orElse(null);
            this.parentUuid = mob.getParentUUID().orElse(null);
            this.faction = mob.getFaction();
        }
    }
}
