package cn.clexus.mythicMobsAddon;

import cn.clexus.mythicMobsAddon.addons.mechanics.TempAttributeModifierMechanic;
import cn.clexus.mythicMobsAddon.addons.triggers.*;
import cn.clexus.mythicMobsAddon.events.CrucibleEventsListener;
import cn.clexus.mythicMobsAddon.events.EventsListener;
import cn.clexus.mythicMobsAddon.events.MMOItemsEventsListener;
import cn.clexus.mythicMobsAddon.support.CrucibleSupport;
import cn.clexus.mythicMobsAddon.support.MMOItemsSupport;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.CustomComponentRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class MythicMobsAddon extends JavaPlugin {

    public static MythicMobsAddon plugin;
    private CustomComponentRegistry componentRegistry;

    @Override
    public void onEnable() {
        plugin = this;
        Bukkit.getPluginManager().registerEvents(new EventsListener(), this);
        CrucibleSupport.init(this);
        MMOItemsSupport.init(this);
        if (CrucibleSupport.hasSupport()) {
            Bukkit.getPluginManager().registerEvents(new CrucibleEventsListener(), this);
        }
        if (MMOItemsSupport.hasSupport()) {
            Bukkit.getPluginManager().registerEvents(new MMOItemsEventsListener(), this);
        }
        componentRegistry = new CustomComponentRegistry(this, new ArrayList<>())
                .registerCustomComponent(
                        CustomComponentRegistry.MythicComponentType.PLACEHOLDER,
                        "cn.clexus.mythicMobsAddon.addons.placeholders"
                );
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, r -> {
            r.registrar().register(Commands.literal("mmaddon")
                    .requires(source -> source.getSender().hasPermission("mythicmobsaddon.command"))
                    .then(Commands.literal("moblist")
                            .executes(ctx -> {
                                var allMobs = MythicBukkit.inst().getMobManager().getActiveMobs();
                                LinkedHashMap<String, Integer> mobCount = new LinkedHashMap<>();
                                allMobs.forEach(mob -> {
                                    String internalName = mob.getType().getInternalName();
                                    mobCount.put(internalName, mobCount.getOrDefault(internalName, 0) + 1);
                                });
                                mobCount.entrySet().stream()
                                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                                        .forEach(entry -> {
                                            ctx.getSource().getSender().sendRichMessage(entry.getKey() + ": " + entry.getValue());
                                        });
                                return 1;
                            })
                    )
                    .then(Commands.literal("castas")
                            .then(Commands.argument("player", ArgumentTypes.player())
                                    .then(Commands.argument("skill", StringArgumentType.string())
                                            .suggests((ctx, builder) -> {
                                                String remaining = builder.getRemainingLowerCase();
                                                for (String name : MythicBukkit.inst().getSkillManager().getSkillNames()) {
                                                    if (remaining.isEmpty() || name.toLowerCase().contains(remaining)) {
                                                        builder.suggest(name);
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                Player caster = resolveSinglePlayer(ctx);
                                                if (caster == null) return 0;

                                                String skillName = StringArgumentType.getString(ctx, "skill");
                                                boolean ok = MythicBukkit.inst().getAPIHelper().castSkill(caster, skillName);
                                                sendCastResult(ctx, caster, skillName, ok, "");
                                                return ok ? 1 : 0;
                                            })
                                            .then(Commands.literal("at")
                                                    .then(Commands.argument("targets", ArgumentTypes.entities())
                                                            .executes(ctx -> {
                                                                Player caster = resolveSinglePlayer(ctx);
                                                                if (caster == null) return 0;

                                                                String skillName = StringArgumentType.getString(ctx, "skill");
                                                                List<org.bukkit.entity.Entity> targets;
                                                                try {
                                                                    EntitySelectorArgumentResolver resolver = ctx.getArgument("targets", EntitySelectorArgumentResolver.class);
                                                                    targets = resolver.resolve(ctx.getSource());
                                                                } catch (CommandSyntaxException ex) {
                                                                    ctx.getSource().getSender().sendRichMessage("目标选择器解析失败: " + ex.getMessage());
                                                                    return 0;
                                                                }
                                                                if (targets.isEmpty()) {
                                                                    ctx.getSource().getSender().sendRichMessage("未匹配到目标实体。");
                                                                    return 0;
                                                                }
                                                                boolean ok = MythicBukkit.inst().getAPIHelper().castSkill(
                                                                        caster,
                                                                        skillName,
                                                                        caster.getLocation(),
                                                                        targets,
                                                                        List.of(),
                                                                        1.0f
                                                                );
                                                                sendCastResult(ctx, caster, skillName, ok, " (at)");
                                                                return ok ? 1 : 0;
                                                            })
                                                    )
                                            )
                                            .then(Commands.literal("atloc")
                                                    .then(Commands.argument("pos", ArgumentTypes.finePosition())
                                                            .executes(ctx -> {
                                                                Player caster = resolveSinglePlayer(ctx);
                                                                if (caster == null) return 0;

                                                                String skillName = StringArgumentType.getString(ctx, "skill");
                                                                try {
                                                                    FinePositionResolver resolver = ctx.getArgument("pos", FinePositionResolver.class);
                                                                    var pos = resolver.resolve(ctx.getSource());
                                                                    var loc = pos.toLocation(caster.getWorld());
                                                                    boolean ok = MythicBukkit.inst().getAPIHelper().castSkill(caster, skillName, loc);
                                                                    sendCastResult(ctx, caster, skillName, ok, " (atloc)");
                                                                    return ok ? 1 : 0;
                                                                } catch (CommandSyntaxException ex) {
                                                                    ctx.getSource().getSender().sendRichMessage("位置解析失败: " + ex.getMessage());
                                                                    return 0;
                                                                }
                                                            })
                                                    )
                                            )
                                            .then(Commands.literal("attarget")
                                                    .executes(ctx -> castAtTarget(ctx, 32))
                                                    .then(Commands.argument("max", IntegerArgumentType.integer(1))
                                                            .executes(ctx -> {
                                                                int max = IntegerArgumentType.getInteger(ctx, "max");
                                                                return castAtTarget(ctx, max);
                                                            })
                                                    )
                                            )
                                    )
                            )
                    )
                    .build()
            );
        });
    }

    @Override
    public void onLoad() {
        OnKillTrigger.register();
        OnBlockBreakTrigger.register();
        OnRegainHealthTrigger.register();
        OnPlayerInputTrigger.register();
        OnEntityPotionEffectTrigger.register();
        OnEntityEffectTickTrigger.register();
        OnTrueDamagedTrigger.register();
        OnWitchThrowPotionTrigger.register();
        OnWitchConsumePotionTrigger.register();
        OnExpPickedUpTrigger.register();
    }

    @Override
    public void onDisable() {
        TempAttributeModifierMechanic.clearScheduledRemovals();
    }

    public CustomComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    private static Player resolveSinglePlayer(CommandContext<CommandSourceStack> ctx) {
        try {
            PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
            List<Player> players = resolver.resolve(ctx.getSource());
            if (players.isEmpty()) {
                ctx.getSource().getSender().sendRichMessage("未找到玩家。");
                return null;
            }
            if (players.size() > 1) {
                ctx.getSource().getSender().sendRichMessage("请仅指定一名玩家。");
                return null;
            }
            return players.get(0);
        } catch (CommandSyntaxException ex) {
            ctx.getSource().getSender().sendRichMessage("玩家解析失败: " + ex.getMessage());
            return null;
        }
    }

    private static int castAtTarget(CommandContext<CommandSourceStack> ctx, int maxDistance) {
        Player caster = resolveSinglePlayer(ctx);
        if (caster == null) return 0;

        String skillName = StringArgumentType.getString(ctx, "skill");
        org.bukkit.entity.Entity target = caster.getTargetEntity(maxDistance);
        if (target == null) {
            ctx.getSource().getSender().sendRichMessage("未找到可锁定的目标实体。距离: " + maxDistance);
            return 0;
        }
        boolean ok = MythicBukkit.inst().getAPIHelper().castSkill(
                caster,
                skillName,
                caster.getLocation(),
                List.of(target),
                List.of(),
                1.0f
        );
        sendCastResult(ctx, caster, skillName, ok, " (attarget=" + maxDistance + ")");
        return ok ? 1 : 0;
    }

    private static void sendCastResult(CommandContext<CommandSourceStack> ctx, Player caster, String skillName, boolean ok, String suffix) {
        if (ctx.getSource().getSender() instanceof ConsoleCommandSender) return;
        if (ok) {
            ctx.getSource().getSender().sendRichMessage("已让玩家 " + caster.getName() + " 施放技能: " + skillName + suffix);
        } else {
            ctx.getSource().getSender().sendRichMessage("技能施放失败: " + skillName + suffix);
        }
    }
}
