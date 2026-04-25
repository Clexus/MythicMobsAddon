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
import io.papermc.paper.event.entity.EntityEffectTickEvent;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.Optional;

public class OnEffectTickMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onEffectTickSkill = Optional.empty();
    protected String onEffectTickSkillName;
    protected boolean cancelEvent;

    public OnEffectTickMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onEffectTickSkillName = mlc.getString(new String[]{"oneffecttickskill", "oneffecttick", "oet"});
        this.cancelEvent = mlc.getBoolean(new String[]{"cancelevent", "ce"}, false);
        this.getManager().queueSecondPass(() -> {
            if (this.onEffectTickSkillName != null) {
                this.onEffectTickSkill = this.getManager().getSkill(file, this, this.onEffectTickSkillName);
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
            this.registerAuraComponent(Events.subscribe(EntityEffectTickEvent.class, EventPriority.HIGHEST)
                    .filter(event -> !event.isCancelled())
                    .filter(this::isAuraHolder)
                    .handler(this::handleEffectTick));

            this.executeAuraSkill(OnEffectTickMechanic.this.onStartSkill, this.skillMetadata);
        }

        private boolean isAuraHolder(EntityEffectTickEvent event) {
            return this.entity.isPresent() && event.getEntity().getUniqueId().equals(this.entity.get().getUniqueId());
        }

        private void handleEffectTick(EntityEffectTickEvent event) {
            if (this.entity.isEmpty()) {
                return;
            }

            SkillMetadata meta = this.skillMetadata.deepClone();
            meta.setTrigger(this.entity.get());
            meta.getVariables().put("amplifier", new StringVariable(String.valueOf(event.getAmplifier())));
            meta.getVariables().put("type", new StringVariable(event.getType().key().value()));

            if (this.executeTargetedAuraSkill(OnEffectTickMechanic.this.onEffectTickSkill, meta, this.entity.get())) {
                this.consumeCharge();
            }

            if (OnEffectTickMechanic.this.cancelEvent) {
                event.setCancelled(true);
            }
        }
    }
}

