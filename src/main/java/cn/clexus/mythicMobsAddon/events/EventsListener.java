package cn.clexus.mythicMobsAddon.events;

import cn.clexus.mythicMobsAddon.addons.conditions.*;
import cn.clexus.mythicMobsAddon.addons.mechanics.*;
import cn.clexus.mythicMobsAddon.addons.targeters.SourceOwner;
import cn.clexus.mythicMobsAddon.addons.targeters.TeamTargeter;
import cn.clexus.mythicMobsAddon.addons.triggers.OnKillTrigger;
import cn.clexus.mythicMobsAddon.addons.triggers.OnRegainHealthTrigger;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import io.lumine.mythic.core.skills.EventExecutor;
import io.lumine.mythic.core.skills.TriggeredSkill;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Collection;

public class EventsListener implements Listener {
    MythicBukkit mythicBukkit = MythicBukkit.inst();
    MobExecutor mobManager = mythicBukkit.getMobManager();
    EventExecutor eventExecutor = mythicBukkit.getSkillManager().getEventBus();

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity source = event.getDamageSource().getCausingEntity() == null ? event.getDamageSource().getDirectEntity() : event.getDamageSource().getCausingEntity();
        if (source != null) {
            if (mobManager.isMythicMob(source)) {
                TriggeredSkill ts = new TriggeredSkill(OnKillTrigger.onKill,
                        mobManager.getActiveMob(source.getUniqueId()).get(),
                        new BukkitEntity(event.getEntity()));
            }
        }
    }

    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        Entity entity = event.getEntity();
        if (mobManager.isMythicMob(entity)) {
            OnRegainHealthTrigger.EntityRegainHealthMeta meta = new OnRegainHealthTrigger.EntityRegainHealthMeta(event);
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnRegainHealthTrigger.onRegainHealth, meta, mobManager.getMythicMobInstance(entity), BukkitAdapter.adapt(entity), BukkitAdapter.adapt(entity.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (mobManager.isMythicMob(entity)) {
            ActiveMob activeMob = mobManager.getActiveMob(entity.getUniqueId()).orElse(null);
            if (activeMob == null) return;
            MythicMob mythicMob = activeMob.getType();
            MythicConfig options = mythicMob.getConfig().getNestedConfig("Options");
            if (options.isSet("CancelInteract")) {
                if (options.getBoolean("CancelInteract")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        MythicMob mythicMob = event.getMobType();
        Entity entity = event.getEntity();
        MythicConfig options = mythicMob.getConfig().getNestedConfig("Options");
        if (options.isSet("RemoveVanillaAttributes") && entity instanceof Attributable attributable) {
            if (options.getBoolean("RemoveVanillaAttributes")) {
                for (Attribute attribute : RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).stream().toList()) {
                    AttributeInstance attributeInstance = attributable.getAttribute(attribute);
                    if (attributeInstance != null) {
                        if (attribute.key().value().equals("armor")) {
                            attributeInstance.setBaseValue(0);
                        }
                        Collection<AttributeModifier> modifiers = attributeInstance.getModifiers();
                        for (AttributeModifier modifier : modifiers) {
                            attributeInstance.removeModifier(modifier);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMythicMechanicLoad(MythicMechanicLoadEvent event) {
        String m = event.getMechanicName();
        if (eq(m, "setpose")) {
            event.register(new SetPoseMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "onrealblockbreak", "onrealbreakblock")) {
            event.register(new OnRealBlockBreakMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "savepdc")) {
            event.register(new SavePDCMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "removepdc")) {
            event.register(new RemovePDCMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m,"parabolic")) {
            event.register(new ParabolicMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        }
    }

    @EventHandler
    public void onMythicConditionLoad(MythicConditionLoadEvent event) {
        String name = event.getConditionName();
        if (eq(name, "sameteam")) {
            event.register(new SameTeamCondition(event.getConfig()));
        } else if (eq(name, "haspose", "pose")) {
            event.register(new PoseCondition(event.getConfig()));
        } else if (eq(name, "sitting", "issitting", "sit")) {
            event.register(new SitCondition(event.getConfig()));
        } else if (eq(name, "team")) {
            event.register(new TeamCondition(event.getConfig()));
        } else if (eq(name,"input")) {
            event.register(new InputCondition(event.getConfig()));
        }
    }

    @EventHandler
    public void onMythicTargeterLoad(MythicTargeterLoadEvent event) {
        String name = event.getTargeterName();
        if (eq(name, "team")) {
            event.register(new TeamTargeter(event.getContainer().getManager(), event.getConfig()));
        } else if (eq(name, "sourceowner", "so")) {
            event.register(new SourceOwner(event.getContainer().getManager(), event.getConfig()));
        }
    }

    private boolean eq(String a, String... strings) {
        for (String s : strings) {
            if (a.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
