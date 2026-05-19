package cn.clexus.mythicMobsAddon.events;

import cn.clexus.mythicMobsAddon.addons.conditions.*;
import cn.clexus.mythicMobsAddon.addons.mechanics.*;
import cn.clexus.mythicMobsAddon.addons.targeters.RandomLocationsOfBoundingBoxTargeter;
import cn.clexus.mythicMobsAddon.addons.targeters.RandomLocationsOfTargetsBoundingBoxTargeter;
import cn.clexus.mythicMobsAddon.addons.targeters.SourceOwner;
import cn.clexus.mythicMobsAddon.addons.targeters.TeamTargeter;
import cn.clexus.mythicMobsAddon.addons.triggers.*;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.WitchConsumePotionEvent;
import com.destroystokyo.paper.event.entity.WitchThrowPotionEvent;
import io.lumine.mythic.api.config.MythicConfig;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.bukkit.adapters.BukkitPlayer;
import io.lumine.mythic.bukkit.events.MythicConditionLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMechanicLoadEvent;
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import io.lumine.mythic.bukkit.events.MythicTargeterLoadEvent;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import io.lumine.mythic.core.players.PlayerData;
import io.lumine.mythic.core.skills.EventExecutor;
import io.lumine.mythic.core.skills.TriggeredSkill;
import io.papermc.paper.event.entity.EntityEffectTickEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventsListener implements Listener {
    MythicBukkit mythicBukkit = MythicBukkit.inst();
    MobExecutor mobManager = mythicBukkit.getMobManager();
    EventExecutor eventExecutor = mythicBukkit.getSkillManager().getEventBus();
    public static final Map<DamageSource, Double> trueDmg = new HashMap<>();
    private final Map<UUID, Long> damageIntervalUntil = new HashMap<>();
    private final Map<UUID, Long> trueDamageIntervalUntil = new HashMap<>();

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event){
        UUID entityId = event.getEntity().getUniqueId();
        damageIntervalUntil.remove(entityId);
        trueDamageIntervalUntil.remove(entityId);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity source = event.getDamageSource().getCausingEntity() == null ? event.getDamageSource().getDirectEntity() : event.getDamageSource().getCausingEntity();
        if (source != null) {
            if (mobManager.isMythicMob(source) && mobManager.getActiveMob(source.getUniqueId()).isPresent()) {
                TriggeredSkill ts = new TriggeredSkill(OnKillTrigger.onKill,
                        mobManager.getActiveMob(source.getUniqueId()).get(),
                        new BukkitEntity(event.getEntity()));
                if (ts.getCancelled()) {
                    event.setCancelled(true);
                }
            } else if (source instanceof Player player) {
                PlayerData playerData = new PlayerData(player.getUniqueId(), player.getName());
                playerData.initialize(new BukkitPlayer(player));
                SkillMetadata data = eventExecutor.buildSkillMetadata(OnKillTrigger.onKill, playerData, BukkitAdapter.adapt(event.getEntity()), BukkitAdapter.adapt(player.getLocation()), true);
                TriggeredSkill ts = eventExecutor.processTriggerMechanics(data);
                if (ts.getCancelled()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getDamage() <= 0) return;

        Entity entity = event.getEntity();
        if (!mobManager.isMythicMob(entity)) return;

        ActiveMob am = mobManager.getMythicMobInstance(entity);
        if (am == null) return;

        MythicConfig options = am.getType().getConfig().getNestedConfig("Options");
        long interval = getIntervalTicks(options, "DamageInterval");
        if (interval <= 0) return;

        if (isIntervalActive(damageIntervalUntil, entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageMonitor(EntityDamageEvent event) {
        if (event.getFinalDamage() <= 0) return;

        Entity entity = event.getEntity();
        if (!mobManager.isMythicMob(entity)) return;

        ActiveMob am = mobManager.getMythicMobInstance(entity);
        if (am == null) return;

        MythicConfig options = am.getType().getConfig().getNestedConfig("Options");
        long interval = getIntervalTicks(options, "DamageInterval");
        if (interval <= 0) return;

        startInterval(damageIntervalUntil, entity.getUniqueId(), interval);
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
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        Entity entity = event.getEntity();
        OnEntityPotionEffectTrigger.EntityPotionEffectMeta meta = new OnEntityPotionEffectTrigger.EntityPotionEffectMeta(event);
        if (mobManager.isMythicMob(entity)) {
            ActiveMob am = mobManager.getMythicMobInstance(entity);
            if (am == null) return;
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnEntityPotionEffectTrigger.onPotionEffect, meta, am, BukkitAdapter.adapt(entity), BukkitAdapter.adapt(entity.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
            }
        } else if (entity instanceof Player player) {
            PlayerData playerData = new PlayerData(player.getUniqueId(), player.getName());
            playerData.initialize(new BukkitPlayer(player));
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnEntityPotionEffectTrigger.onPotionEffect, meta, playerData, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        if(player.getVehicle() == null) return;
        Entity vehicle = player.getVehicle();
        if (!mobManager.isMythicMob(vehicle)) return;
        ActiveMob am = mobManager.getMythicMobInstance(vehicle);
        OnPlayerInputTrigger.PlayerInputMeta meta = new OnPlayerInputTrigger.PlayerInputMeta(event);
        SkillMetadata data = eventExecutor.buildSkillMetadata(OnPlayerInputTrigger.onPlayerInput, meta, am, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);
        eventExecutor.processTriggerMechanics(data, meta);
    }

    @EventHandler
    public void onEntityTickEffect(EntityEffectTickEvent event) {
        Entity entity = event.getEntity();
        OnEntityEffectTickTrigger.EntityEffectTickMeta meta = new OnEntityEffectTickTrigger.EntityEffectTickMeta(event);
        if (mobManager.isMythicMob(entity)) {
            ActiveMob am = mobManager.getMythicMobInstance(entity);
            if (am == null) return;
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnEntityEffectTickTrigger.onEffectTick, meta, am, BukkitAdapter.adapt(entity), BukkitAdapter.adapt(entity.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
            }
        } else if (entity instanceof Player player) {
            PlayerData playerData = new PlayerData(player.getUniqueId(), player.getName());
            playerData.initialize(new BukkitPlayer(player));
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnEntityEffectTickTrigger.onEffectTick, meta, playerData, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWitchThrowPotion(WitchThrowPotionEvent event) {
        Entity entity = event.getEntity();
        if (!mobManager.isMythicMob(entity)) return;

        ActiveMob am = mobManager.getMythicMobInstance(entity);
        if (am == null) return;

        OnWitchThrowPotionTrigger.WitchThrowPotionMeta meta = new OnWitchThrowPotionTrigger.WitchThrowPotionMeta(event);
        SkillMetadata data = eventExecutor.buildSkillMetadata(OnWitchThrowPotionTrigger.onWitchThrowPotion, meta, am, BukkitAdapter.adapt(event.getTarget()), BukkitAdapter.adapt(event.getTarget().getLocation()), true);
        TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
        if (ts.getCancelled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWitchConsumePotion(WitchConsumePotionEvent event) {
        Entity entity = event.getEntity();
        if (!mobManager.isMythicMob(entity)) return;

        ActiveMob am = mobManager.getMythicMobInstance(entity);
        if (am == null) return;

        OnWitchConsumePotionTrigger.WitchConsumePotionMeta meta = new OnWitchConsumePotionTrigger.WitchConsumePotionMeta(event);
        SkillMetadata data = eventExecutor.buildSkillMetadata(OnWitchConsumePotionTrigger.onWitchConsumePotion, meta, am, BukkitAdapter.adapt(entity), BukkitAdapter.adapt(entity.getLocation()), true);
        TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
        if (ts.getCancelled()) {
            event.setCancelled(true);
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
        if (eq(m, "onrealblockbreak", "onrealbreakblock")) {
            event.register(new OnRealBlockBreakMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "savepdc")) {
            event.register(new SavePDCMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "removepdc")) {
            event.register(new RemovePDCMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m,"parabolic")) {
            event.register(new ParabolicMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "tempam","tempattributemodifier","tam")) {
            event.register(new TempAttributeModifierMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "settickslived", "stl")) {
            event.register(new SetTicksLivedMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "funnel")) {
            event.register(new FunnelMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "truedamage", "truedmg", "td")) {
            event.register(new TrueDamageMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "hook")) {
            event.register(new HookMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "onkill")) {
            event.register(new OnKillMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "onheal")) {
            event.register(new OnHealMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "oneffecttick")) {
            event.register(new OnEffectTickMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "oneffect")) {
            event.register(new OnEffectMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m,"ontruedamaged", "ontruedamage")) {
            event.register(new OnTrueDamagedMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "ontrueattack", "ontrueatk")) {
            event.register(new OnTrueAttackMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "confuse")) {
            event.register(new ConfuseMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "removeattributemodifier", "removemodifier", "ram")) {
            event.register(new RemoveAttributeModifierMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
        } else if (eq(m, "lookat")) {
            event.register(new LookAtMechanic(event.getContainer().getManager(), event.getContainer().getFile(), event.getConfig().getLine(), event.getConfig()));
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
        } else if (eq(name, "origindistance", "od")) {
            event.register(new OriginDistanceCondition(event.getConfig()));
        } else if (eq(name, "sblocktype", "sinblock")) {
            event.register(new SimpleBlockTypeCondition(event.getConfig(), 0));
        } else if (eq(name, "sonblock")) {
            event.register(new SimpleBlockTypeCondition(event.getConfig(), 0.6));
        }
    }

    @EventHandler
    public void onMythicTargeterLoad(MythicTargeterLoadEvent event) {
        String name = event.getTargeterName();
        if (eq(name, "team")) {
            event.register(new TeamTargeter(event.getContainer().getManager(), event.getConfig()));
        } else if (eq(name, "sourceowner", "so")) {
            event.register(new SourceOwner(event.getContainer().getManager(), event.getConfig()));
        } else if (eq(name, "randomlocationsofboundingbox","rlobb", "rlbb")) {
            event.register(new RandomLocationsOfBoundingBoxTargeter(event.getContainer().getManager(), event.getConfig()));
        } else if (eq(name, "randomlocationsoftargetsboundingbox", "rlotbb", "rltbb")) {
            event.register(new RandomLocationsOfTargetsBoundingBoxTargeter(event.getContainer().getManager(), event.getConfig()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityTrueDamageInterval(EntityTrueDamageEvent event) {
        Entity entity = event.getEntity();
        if (!mobManager.isMythicMob(entity)) return;

        ActiveMob am = mobManager.getMythicMobInstance(entity);
        if (am == null) return;

        MythicConfig options = am.getType().getConfig().getNestedConfig("Options");
        long interval = getIntervalTicks(options, "TrueDamageInterval");
        if (interval <= 0) return;

        if (isIntervalActive(trueDamageIntervalUntil, entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTrueDamaged(EntityTrueDamageEvent event) {
        if (event.getDamage() <= 0) return;

        Entity entity = event.getEntity();
        OnTrueDamagedTrigger.EntityTrueDamagedMeta meta = new OnTrueDamagedTrigger.EntityTrueDamagedMeta(event);
        if (mobManager.isMythicMob(entity)) {
            ActiveMob am = mobManager.getMythicMobInstance(entity);
            if (am == null) return;
            MythicConfig options = am.getType().getConfig().getNestedConfig("Options");
            long interval = getIntervalTicks(options, "TrueDamageInterval");
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnTrueDamagedTrigger.onTrueDamaged, meta, am, BukkitAdapter.adapt(entity), BukkitAdapter.adapt(entity.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
                return;
            }
            if (interval > 0 && event.getDamage() > 0) {
                startInterval(trueDamageIntervalUntil, entity.getUniqueId(), interval);
            }
        } else if (entity instanceof Player player) {
            PlayerData playerData = new PlayerData(player.getUniqueId(), player.getName());
            playerData.initialize(new BukkitPlayer(player));
            SkillMetadata data = eventExecutor.buildSkillMetadata(OnTrueDamagedTrigger.onTrueDamaged, meta, playerData, BukkitAdapter.adapt(player), BukkitAdapter.adapt(player.getLocation()), true);
            TriggeredSkill ts = eventExecutor.processTriggerMechanics(data, meta);
            if (ts.getCancelled()) {
                event.setCancelled(true);
            }
            // 玩家不使用 MythicMob Options，因此不记录真实伤害间隔。
        }
    }

    private boolean isIntervalActive(Map<UUID, Long> intervals, UUID entityId) {
        Long until = intervals.get(entityId);
        if (until == null) return false;

        long now = System.currentTimeMillis();
        if (until <= now) {
            intervals.remove(entityId);
            return false;
        }

        return true;
    }

    private void startInterval(Map<UUID, Long> intervals, UUID entityId, long ticks) {
        if (ticks <= 0) {
            intervals.remove(entityId);
            return;
        }

        intervals.put(entityId, System.currentTimeMillis() + (ticks * 50L));
    }

    private long getIntervalTicks(MythicConfig options, String key) {
        if (options == null || !options.isSet(key)) {
            return 0L;
        }

        return Math.max(0L, options.getInteger(key));
    }

    public static boolean eq(String a, String... strings) {
        for (String s : strings) {
            if (a.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
}
