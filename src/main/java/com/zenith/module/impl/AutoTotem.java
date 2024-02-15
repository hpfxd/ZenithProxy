package com.zenith.module.impl;

import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ClickItemAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.zenith.Proxy;
import com.zenith.event.module.ClientTickEvent;
import com.zenith.module.Module;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.zenith.Shared.*;
import static java.util.Objects.nonNull;

public class AutoTotem extends Module {
    private boolean swapping = false;
    private int delay = 0;
    private static final int MOVEMENT_PRIORITY = 1000;
    private int totemId = ITEMS_MANAGER.getItemId("totem_of_undying");

    public boolean isActivelySwapping() {
        return CONFIG.client.extra.autoTotem.enabled && swapping;
    }

    public AutoTotem() {
        super();
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(this, ClientTickEvent.class, this::handleClientTick);
    }

    @Override
    public boolean shouldBeEnabled() {
        return CONFIG.client.extra.autoTotem.enabled;
    }

    public void handleClientTick(final ClientTickEvent event) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && playerHealthBelowThreshold()
                && Instant.now().minus(Duration.ofSeconds(2)).isAfter(Proxy.getInstance().getConnectTime())) {
            if (delay > 0) {
                delay--;
                return;
            } else {
                if (swapping) {
                    delay = 5;
                    swapping = false;
                    return;
                }
            }
            if (!isTotemEquipped()) {
                swapToTotem();
            }
            if (swapping) {
                PATHING.stop(MOVEMENT_PRIORITY);
            }
        }
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoTotem.healthThreshold;
    }

    private boolean isTotemEquipped() {
        final ItemStack offhandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND);
        return nonNull(offhandStack) && offhandStack.getId() == totemId;
    }

    private void swapToTotem() {
        final List<ItemStack> inventory = CACHE.getPlayerCache().getPlayerInventory();
        ItemStack offhand = inventory.get(45);
        for (int i = 44; i >= 9; i--) {
            final ItemStack stack = inventory.get(i);
            if (nonNull(stack) && stack.getId() == totemId) {
                if (nonNull(offhand) && nonNull(CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND))) {
                    sendClientPacketsAsync(new ServerboundContainerClickPacket(0, CACHE.getPlayerCache().getActionId().incrementAndGet(), i, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, stack, Int2ObjectMaps.singleton(i, null)),
                                           new ServerboundContainerClickPacket(0, CACHE.getPlayerCache().getActionId().incrementAndGet(), 45, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, offhand, Int2ObjectMaps.singleton(45, stack)),
                                           new ServerboundContainerClickPacket(0, CACHE.getPlayerCache().getActionId().incrementAndGet(), i, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, null, Int2ObjectMaps.singleton(i, offhand)));
                } else {
                    sendClientPacketsAsync(new ServerboundContainerClickPacket(0, CACHE.getPlayerCache().getActionId().incrementAndGet(), i, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, stack, Int2ObjectMaps.singleton(i, null)),
                                           new ServerboundContainerClickPacket(0, CACHE.getPlayerCache().getActionId().incrementAndGet(), 45, ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, null, Int2ObjectMaps.singleton(45, stack)));
                }
                CLIENT_LOG.info("AutoTotem: Swapping to totem");
                delay = 5;
                swapping = true;
                return;
            }
        }
    }
}
