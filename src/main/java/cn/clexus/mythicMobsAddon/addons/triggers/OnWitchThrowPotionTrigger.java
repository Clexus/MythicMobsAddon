package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent;

public class OnWitchThrowPotionTrigger {
    public static final SkillTrigger onWitchThrowPotion = SkillTrigger.create("WitchThrowPotion", WitchThrowPotionMeta.class, "ThrowPotion");

    public static void register() {
        onWitchThrowPotion.register();
    }

    public static class WitchThrowPotionMeta extends SkillTriggerMetadata {

        private final WitchThrowPotionEvent event;

        public WitchThrowPotionMeta(WitchThrowPotionEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            String potionType = event.getPotion() == null ? "null" : event.getPotion().getType().key().value();
            data.getVariables().put("potion", new StringVariable(potionType));
        }
    }
}


