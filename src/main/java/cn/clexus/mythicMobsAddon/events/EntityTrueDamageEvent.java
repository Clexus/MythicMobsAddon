package cn.clexus.mythicMobsAddon.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.jetbrains.annotations.NotNull;

public class EntityTrueDamageEvent extends EntityEvent implements Cancellable {

    private double damage;
    private final LivingEntity damager;
    private boolean cancelled;
    private static final HandlerList handlers = new HandlerList();

    public EntityTrueDamageEvent(@NotNull Entity entity, @NotNull LivingEntity damager, double damage) {
        super(entity);
        this.damager = damager;
        this.damage = damage;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return damage;
    }

    public LivingEntity getDamager() {
        return damager;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}
