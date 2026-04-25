package cn.clexus.mythicMobsAddon.addons.triggers;

import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillTrigger;
import io.lumine.mythic.core.skills.triggers.SkillTriggerMetadata;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import com.destroystokyo.paper.event.entity.WitchConsumePotionEvent;

public class OnWitchConsumePotionTrigger {
    public static final SkillTrigger onWitchConsumePotion = SkillTrigger.create("WitchConsumePotion", WitchConsumePotionMeta.class, "ConsumePotion");

    public static void register() {
        onWitchConsumePotion.register();
    }

    public static class WitchConsumePotionMeta extends SkillTriggerMetadata {

        private final WitchConsumePotionEvent event;

        public WitchConsumePotionMeta(WitchConsumePotionEvent event) {
            this.event = event;
        }

        @Override
        public void applyToSkillMetadata(SkillMetadata data) {
            String potionType = event.getPotion() == null ? "null" : event.getPotion().getType().key().value();
            data.getVariables().put("potion", new StringVariable(potionType));
        }
    }
}


