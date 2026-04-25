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
import io.lumine.mythic.core.skills.variables.types.MapVariable;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OnEffectMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onEffectSkill = Optional.empty();
    protected String onEffectSkillName;
    protected boolean cancelEvent;

    public OnEffectMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onEffectSkillName = mlc.getString(new String[]{"oneffectskill", "oneffect", "oe"});
        this.cancelEvent = mlc.getBoolean(new String[]{"cancelevent", "ce"}, false);
        this.getManager().queueSecondPass(() -> {
            if (this.onEffectSkillName != null) {
                this.onEffectSkill = this.getManager().getSkill(file, this, this.onEffectSkillName);
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
            this.registerAuraComponent(Events.subscribe(EntityPotionEffectEvent.class, EventPriority.HIGHEST)
                    .filter(event -> !event.isCancelled())
                    .filter(this::isAuraHolder)
                    .handler(this::handlePotionEffect));

            this.executeAuraSkill(OnEffectMechanic.this.onStartSkill, this.skillMetadata);
        }

        private boolean isAuraHolder(EntityPotionEffectEvent event) {
            return this.entity.isPresent() && event.getEntity().getUniqueId().equals(this.entity.get().getUniqueId());
        }

        private void handlePotionEffect(EntityPotionEffectEvent event) {
            if (this.entity.isEmpty()) {
                return;
            }

            SkillMetadata meta = this.skillMetadata.deepClone();
            meta.setTrigger(this.entity.get());
            meta.getVariables().put("action", new StringVariable(event.getAction().toString()));
            meta.getVariables().put("cause", new StringVariable(event.getCause().toString()));
            meta.getVariables().put("type", new StringVariable(event.getModifiedType().key().value()));
            meta.getVariables().put("new", new MapVariable(toEffectMap(event.getNewEffect()), 0));
            meta.getVariables().put("old", new MapVariable(toEffectMap(event.getOldEffect()), 0));

            if (this.executeTargetedAuraSkill(OnEffectMechanic.this.onEffectSkill, meta, this.entity.get())) {
                this.consumeCharge();
            }

            if (OnEffectMechanic.this.cancelEvent) {
                event.setCancelled(true);
            }
        }

        private Map<String, String> toEffectMap(org.bukkit.potion.PotionEffect effect) {
            Map<String, String> vars = new HashMap<>();
            vars.put("type", "null");
            vars.put("amplifier", "null");
            vars.put("ambient", "null");
            vars.put("duration", "null");
            vars.put("icon", "null");
            vars.put("particles", "null");

            if (effect != null) {
                vars.put("type", effect.getType().key().value());
                vars.put("amplifier", String.valueOf(effect.getAmplifier()));
                vars.put("ambient", String.valueOf(effect.isAmbient()));
                vars.put("duration", String.valueOf(effect.getDuration()));
                vars.put("icon", String.valueOf(effect.hasIcon()));
                vars.put("particles", String.valueOf(effect.hasParticles()));
            }

            return vars;
        }
    }
}

