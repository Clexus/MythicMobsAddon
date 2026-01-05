package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import io.papermc.paper.event.entity.EntityEffectTickEvent;

public class OnEntityEffectTickTrigger {
    public static final SkillTrigger onEffectTick = SkillTrigger.create("EffectTick", OnEntityEffectTickTrigger.EntityEffectTickMeta.class, "EntityEffectTick");

    public static void register() {
        onEffectTick.register();
    }

    public static class EntityEffectTickMeta extends SkillTriggerMetadata {

        private final EntityEffectTickEvent event;

        public EntityEffectTickMeta(EntityEffectTickEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            data.getVariables().put("amplifier", new StringVariable(String.valueOf(event.getAmplifier())));
            data.getVariables().put("type", new StringVariable(event.getType().key().value()));
        }
    }
}
