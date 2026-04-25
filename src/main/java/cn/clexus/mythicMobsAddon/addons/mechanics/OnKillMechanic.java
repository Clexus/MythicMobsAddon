package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.Events;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

import java.io.File;
import java.util.Optional;

public class OnKillMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onKillSkill = Optional.empty();
    protected String onKillSkillName;

    public OnKillMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onKillSkillName = mlc.getString(new String[]{"onkillskill", "onkill", "ok"});
        this.getManager().queueSecondPass(() -> {
            if (this.onKillSkillName != null) {
                this.onKillSkill = this.getManager().getSkill(file, this, this.onKillSkillName);
            }
        });
    }

    @Override
    public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
        new Tracker(data, target);
        return SkillResult.SUCCESS;
    }

    private class Tracker extends Aura.AuraTracker {
        public Tracker(SkillMetadata data, AbstractEntity entity) {
            super(entity, data);
            this.start();
        }

        @Override
        public void auraStart() {
            this.registerAuraComponent(Events.subscribe(EntityDeathEvent.class, EventPriority.HIGHEST)
                    .filter(this::isKilledByAuraHolder)
                    .handler(this::handleKill));

            this.executeAuraSkill(OnKillMechanic.this.onStartSkill, this.skillMetadata);
        }

        private boolean isKilledByAuraHolder(EntityDeathEvent event) {
            if (this.entity.isEmpty()) {
                return false;
            }
            if (event.getEntity().getUniqueId().equals(this.entity.get().getUniqueId())) {
                return false;
            }

            var source = event.getDamageSource();
            Entity killer = source.getCausingEntity() == null ? source.getDirectEntity() : source.getCausingEntity();
            return killer != null && killer.getUniqueId().equals(this.entity.get().getUniqueId());
        }

        private void handleKill(EntityDeathEvent event) {
            AbstractEntity victim = BukkitAdapter.adapt(event.getEntity());

            if (this.executeTargetedAuraSkill(OnKillMechanic.this.onKillSkill, skillMetadata, victim)) {
                this.consumeCharge();
            }
        }
    }
}
