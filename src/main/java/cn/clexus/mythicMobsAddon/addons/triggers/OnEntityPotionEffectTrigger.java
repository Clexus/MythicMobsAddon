package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.MapVariable;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import java.util.HashMap;
import java.util.Map;

public class OnEntityPotionEffectTrigger {
    public static final SkillTrigger onPotionEffect = SkillTrigger.create("PotionEffect", OnEntityPotionEffectTrigger.EntityPotionEffectMeta.class, "Effect", "EntityPotionEffect");

    public static void register() {
        onPotionEffect.register();
    }

    public static class EntityPotionEffectMeta extends SkillTriggerMetadata {

        private final EntityPotionEffectEvent event;

        public EntityPotionEffectMeta(EntityPotionEffectEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            data.getVariables().put("action", new StringVariable(event.getAction().toString()));
            data.getVariables().put("cause", new StringVariable(event.getCause().toString()));
            data.getVariables().put("type", new StringVariable(event.getModifiedType().key().value()));
            Map<String, String> empty = new HashMap<>();
            empty.put("type", "null");
            empty.put("amplifier", "null");
            empty.put("ambient", "null");
            empty.put("duration", "null");
            empty.put("icon", "null");
            empty.put("particles", "null");
            var newVars = new HashMap<>(empty);
            if(event.getNewEffect() != null) {
                newVars.put("type", event.getNewEffect().getType().key().value());
                newVars.put("amplifier", String.valueOf(event.getNewEffect().getAmplifier()));
                newVars.put("ambient", String.valueOf(event.getNewEffect().isAmbient()));
                newVars.put("duration", String.valueOf(event.getNewEffect().getDuration()));
                newVars.put("icon", String.valueOf(event.getNewEffect().hasIcon()));
                newVars.put("particles", String.valueOf(event.getNewEffect().hasParticles()));
            }
            var oldVars = new HashMap<>(empty);
            if(event.getOldEffect() != null) {
                oldVars.put("type", event.getOldEffect().getType().key().value());
                oldVars.put("amplifier", String.valueOf(event.getOldEffect().getAmplifier()));
                oldVars.put("ambient", String.valueOf(event.getOldEffect().isAmbient()));
                oldVars.put("duration", String.valueOf(event.getOldEffect().getDuration()));
                oldVars.put("icon", String.valueOf(event.getOldEffect().hasIcon()));
                oldVars.put("particles", String.valueOf(event.getOldEffect().hasParticles()));
            }
            data.getVariables().put("new", new MapVariable(newVars, 0));
            data.getVariables().put("old", new MapVariable(oldVars, 0));
        }
    }
}
