package cn.clexus.mythicMobsAddon.addons.mechanics;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.bukkit.utils.Events;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.auras.Aura;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.io.File;
import java.util.Optional;

public class OnHealMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onHealSkill = Optional.empty();
    protected String onHealSkillName;
    protected boolean cancelEvent;

    public OnHealMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onHealSkillName = mlc.getString(new String[]{"onhealskill", "onheal", "oh"});
        this.cancelEvent = mlc.getBoolean(new String[]{"cancelevent", "ce"}, false);
        this.getManager().queueSecondPass(() -> {
            if (this.onHealSkillName != null) {
                this.onHealSkill = this.getManager().getSkill(file, this, this.onHealSkillName);
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
            this.registerAuraComponent(Events.subscribe(EntityRegainHealthEvent.class, EventPriority.HIGHEST)
                    .filter(event -> !event.isCancelled())
                    .filter(this::isAuraHolder)
                    .handler(this::handleHeal));

            this.executeAuraSkill(OnHealMechanic.this.onStartSkill, this.skillMetadata);
        }

        private boolean isAuraHolder(EntityRegainHealthEvent event) {
            return this.entity.isPresent() && event.getEntity().getUniqueId().equals(this.entity.get().getUniqueId());
        }

        private void handleHeal(EntityRegainHealthEvent event) {
            if (this.entity.isEmpty()) {
                return;
            }

            SkillMetadata meta = this.skillMetadata.deepClone();
            meta.setTrigger(this.entity.get());
            meta.getVariables().put("amount", new StringVariable(String.valueOf(event.getAmount())));
            meta.getVariables().put("reason", new StringVariable(String.valueOf(event.getRegainReason())));

            if (this.executeTargetedAuraSkill(OnHealMechanic.this.onHealSkill, meta, this.entity.get())) {
                this.consumeCharge();
            }

            if (OnHealMechanic.this.cancelEvent) {
                event.setCancelled(true);
            }
        }
    }
}

