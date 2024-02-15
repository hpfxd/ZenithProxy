package com.zenith.cache.data.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import lombok.Data;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

import static com.zenith.Shared.CACHE_LOG;

@Data
public class Container {
    private int containerId;
    private List<ItemStack> contents;
    private ContainerType type = ContainerType.GENERIC_9X4;
    private Component title = Component.empty();
    public static final ItemStack EMPTY_STACK = null;

    public Container(int containerId, List<ItemStack> contents) {
        this.containerId = containerId;
        this.contents = contents;
    }

    public Container(int containerId, int size) {
        this.containerId = containerId;
        this.contents = populateContents(size);
    }

    public Container(final int containerId, final ContainerType type, final Component title) {
        this.containerId = containerId;
        this.type = type;
        this.title = title;
        this.contents = populateContents(0);
    }

    private List<ItemStack> populateContents(int size) {
        final List<ItemStack> contents = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            contents.add(EMPTY_STACK);
        }
        return contents;
    }

    public void setContents(final ItemStack[] inventory) {
        if (inventory.length != contents.size()) {
            this.contents = populateContents(inventory.length);
        }
        for (int i = 0; i < inventory.length; i++) {
            contents.set(i, inventory[i]);
        }
    }

    public void setItemStack(final int slot, final ItemStack newItemStack) {
        if (slot < 0 || slot >= contents.size()) {
            CACHE_LOG.warn("Invalid slot {} for containerId {}", slot, containerId);
            return;
        }
        contents.set(slot, newItemStack);
    }

    public ItemStack getItemStack(final int slot) {
        if (slot < 0 || slot >= contents.size()) {
            CACHE_LOG.warn("Invalid slot {} for containerId {}", slot, containerId);
            return EMPTY_STACK;
        }
        return contents.get(slot);
    }

    public int getSize() {
        return contents.size();
    }
}
