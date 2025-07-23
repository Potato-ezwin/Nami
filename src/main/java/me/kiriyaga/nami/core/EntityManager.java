package me.kiriyaga.nami.core;

import me.kiriyaga.nami.Nami;
import me.kiriyaga.nami.event.EventPriority;
import me.kiriyaga.nami.event.SubscribeEvent;
import me.kiriyaga.nami.event.impl.Render2DEvent;
import me.kiriyaga.nami.feature.module.impl.client.EntityManagerModule;
import me.kiriyaga.nami.util.EntityUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.*;

public class EntityManager {

    private int maxIdleTicks = 500;
    private int idleTicksCounter = 0;

    private List<Entity> allEntities = List.of();
    private List<PlayerEntity> players = List.of();
    private List<PlayerEntity> otherPlayers = List.of();
    private List<Entity> hostile = List.of();
    private List<Entity> neutral = List.of();
    private List<Entity> passive = List.of();
    private List<ItemEntity> droppedItems = List.of();
    private List<Entity> endCrystals = List.of();

    private EntityManagerModule entityManagerModule = null;

    public void init() {
        Nami.EVENT_MANAGER.register(this);
    }

    public void markRequested() {
        idleTicksCounter = 0;
    }

    public Entity getTarget() {
        if (MC.player == null || MC.world == null || entityManagerModule == null)
            return null;

        markRequested();

        List<Entity> candidates = allEntities.stream()
                .filter(e -> e != MC.player)
                .filter(e -> e instanceof LivingEntity && e.isAlive())
                .filter(e -> {
                    if (e.age < entityManagerModule.minTicksExisted.get().intValue()) return false;
                    double distSq = e.squaredDistanceTo(MC.player);
                    return distSq <= entityManagerModule.targetRange.get() * entityManagerModule.targetRange.get();
                })
                .filter(e ->
                        (entityManagerModule.targetPlayers.get() && e instanceof PlayerEntity && !FRIEND_MANAGER.isFriend(e.getName().getString()))
                                || (entityManagerModule.targetHostiles.get() && EntityUtils.isHostile(e))
                                || (entityManagerModule.targetNeutrals.get() && EntityUtils.isNeutral(e))
                                || (entityManagerModule.targetPassives.get() && EntityUtils.isPassive(e)))
                .collect(Collectors.toList());

        Comparator<Entity> comparator = switch (entityManagerModule.priority.get()) {
            case HEALTH -> Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth());
            case DISTANCE -> Comparator.comparingDouble(e -> e.squaredDistanceTo(MC.player));
        };

        Optional<Entity> result = candidates.stream().min(comparator);
        return result.orElse(null);
    }

    public List<Entity> getAllEntities() {
        markRequested();
        return allEntities;
    }

    public List<PlayerEntity> getPlayers() {
        markRequested();
        return players;
    }

    public List<PlayerEntity> getOtherPlayers() {
        markRequested();
        return otherPlayers;
    }

    public List<Entity> getHostile() {
        markRequested();
        return hostile;
    }

    public List<Entity> getNeutral() {
        markRequested();
        return neutral;
    }

    public List<Entity> getPassive() {
        markRequested();
        return passive;
    }

    public List<ItemEntity> getDroppedItems() {
        markRequested();
        return droppedItems;
    }

    public List<Entity> getEndCrystals() {
        markRequested();
        return endCrystals;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onFrame(Render2DEvent event) {
        if (MC.world == null) {
            clearData();
            idleTicksCounter = 0;
            return;
        }

        entityManagerModule = MODULE_MANAGER.getStorage().getByClass(EntityManagerModule.class);
        if (entityManagerModule == null) return;

        maxIdleTicks = entityManagerModule.maxIdleTicks.get();

        if (idleTicksCounter < maxIdleTicks) {
            updateAll();
            idleTicksCounter++;
        } else {
            clearData();
        }
    }

    private void updateAll() {
        allEntities = EntityUtils.getAllEntities();
        players = EntityUtils.getPlayers();
        otherPlayers = EntityUtils.getOtherPlayers();

        hostile = allEntities.stream().filter(EntityUtils::isHostile).toList();
        neutral = allEntities.stream().filter(EntityUtils::isNeutral).toList();
        passive = allEntities.stream().filter(EntityUtils::isPassive).toList();
        droppedItems = allEntities.stream()
                .filter(e -> e instanceof ItemEntity)
                .map(e -> (ItemEntity) e)
                .toList();
        endCrystals = allEntities.stream()
                .filter(e -> e instanceof net.minecraft.entity.decoration.EndCrystalEntity)
                .toList();
    }

    private void clearData() {
        allEntities = List.of();
        players = List.of();
        otherPlayers = List.of();
        hostile = List.of();
        neutral = List.of();
        passive = List.of();
        droppedItems = List.of();
        endCrystals = List.of();
    }
}