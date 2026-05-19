package cn.clexus.mythicMobsAddon.addons.mechanics;

import cn.clexus.mythicMobsAddon.events.EntityTrueDamageEvent;
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
import org.bukkit.event.EventPriority;

import java.io.File;
import java.util.Optional;

public class OnTrueAttackMechanic extends Aura implements ITargetedEntitySkill {
    protected Optional<Skill> onAttackSkill = Optional.empty();
    protected String onAttackSkillName;
    protected boolean cancelDamage;

    public OnTrueAttackMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
        super(manager, file, skill, mlc);
        this.onAttackSkillName = mlc.getString(new String[]{"onattackskill", "onattack", "oa", "onhitskill", "onhit", "oh"});
        this.cancelDamage = mlc.getBoolean(new String[]{"cancelevent", "ce", "canceldamage", "cd"}, false);
        this.getManager().queueSecondPass(() -> {
            if (this.onAttackSkillName != null) {
                this.onAttackSkill = this.getManager().getSkill(file, this, this.onAttackSkillName);
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
            this.registerAuraComponent(Events.subscribe(EntityTrueDamageEvent.class, EventPriority.LOW)
                    .filter(event -> !event.isCancelled())
                    .filter(event -> this.entity.isPresent()
                            && event.getDamager().getUniqueId().equals(this.entity.get().getUniqueId()))
                    .handler(this::handleTrueAttack));

            this.executeAuraSkill(OnTrueAttackMechanic.this.onStartSkill, this.skillMetadata);
        }

        private void handleTrueAttack(EntityTrueDamageEvent event) {
            AbstractEntity target = BukkitAdapter.adapt(event.getEntity());
            SkillMetadata meta = this.skillMetadata.deepClone();
            meta.setTrigger(target);
            meta.getVariables().putDouble("damage-amount", event.getDamage());

            if (this.executeTargetedAuraSkill(OnTrueAttackMechanic.this.onAttackSkill, meta, target)) {
                this.consumeCharge();
                if(cancelDamage) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

