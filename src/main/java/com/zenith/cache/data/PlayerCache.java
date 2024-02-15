package com.zenith.cache.data;

import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.EquipmentSlot;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.GlobalPos;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.data.game.inventory.CreativeGrabAction;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundChangeDifficultyPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateEnabledFeaturesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundUpdateTagsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerAbilitiesPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetCarriedItemPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.packetlib.packet.Packet;
import com.zenith.Proxy;
import com.zenith.cache.CachedData;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityCache;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.cache.data.inventory.InventoryCache;
import com.zenith.util.math.MutableVec3i;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.zenith.Shared.CLIENT_LOG;
import static java.util.Objects.nonNull;


@Getter
@Setter
@Accessors(chain = true)
public class PlayerCache implements CachedData {
    protected boolean hardcore;
    protected boolean reducedDebugInfo;
    protected int maxPlayers;
    protected boolean enableRespawnScreen;
    protected GlobalPos lastDeathPos;
    protected int portalCooldown;
    protected GameMode gameMode;
    protected int heldItemSlot = 0;

    protected EntityPlayer thePlayer = (EntityPlayer) new EntityPlayer(true).setEntityId(-1);

    protected final InventoryCache inventoryCache = new InventoryCache();

    protected final EntityCache entityCache;
    protected String[] enabledFeatures = new String[]{"minecraft:vanilla"};
    protected Difficulty difficulty = Difficulty.NORMAL;
    protected boolean isDifficultyLocked;
    protected boolean invincible;
    protected boolean canFly;
    protected boolean flying;
    protected boolean creative;
    protected float flySpeed;
    protected float walkSpeed;
    protected boolean isSneaking = false;
    protected boolean isSprinting = false;
    protected Map<String, Map<String, int[]>> tags = new HashMap<>();
    protected EntityEvent opLevel = EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0;
    protected AtomicInteger actionId = new AtomicInteger(0);
    private static final MutableVec3i DEFAULT_SPAWN_POSITION = new MutableVec3i(0, 0, 0);
    protected MutableVec3i spawnPosition = DEFAULT_SPAWN_POSITION;

    public PlayerCache(final EntityCache entityCache) {
        this.entityCache = entityCache;
    }

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer) {
        consumer.accept(new ClientboundUpdateEnabledFeaturesPacket(this.enabledFeatures));
        // todo: may need to move this out so spectators don't get sent wrong abilities
        consumer.accept(new ClientboundPlayerAbilitiesPacket(this.invincible, this.canFly, this.flying, this.creative, this.flySpeed, this.walkSpeed));
        consumer.accept(new ClientboundChangeDifficultyPacket(this.difficulty, this.isDifficultyLocked));
        consumer.accept(new ClientboundUpdateTagsPacket(this.tags));
        consumer.accept(new ClientboundGameEventPacket(GameEvent.CHANGE_GAMEMODE, this.gameMode));
        consumer.accept(new ClientboundEntityEventPacket(this.thePlayer.getEntityId(), this.opLevel));
        var container = this.inventoryCache.getContainers().get(this.inventoryCache.getOpenContainerId());
        if (container == this.inventoryCache.getContainers().defaultReturnValue()) {
            container = this.inventoryCache.getPlayerInventory();
        }
        if (container.getContainerId() != 0) {
            consumer.accept(new ClientboundOpenScreenPacket(container.getContainerId(), container.getType(), container.getTitle()));
        }
        consumer.accept(new ClientboundContainerSetContentPacket(container.getContainerId(),
                                                                 actionId.get(),
                                                                 container.getContents().toArray(new ItemStack[0]),
                                                                 new ItemStack(0, 0)));
        consumer.accept(new ClientboundPlayerPositionPacket(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), ThreadLocalRandom.current().nextInt(16, 1024)));
        consumer.accept(new ClientboundSetDefaultSpawnPositionPacket(Vector3i.from(spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ()), 0.0f));
        consumer.accept(new ClientboundSetCarriedItemPacket(heldItemSlot));
    }

    @Override
    public void reset(boolean full) {
        if (full)   {
            this.thePlayer = (EntityPlayer) new EntityPlayer(true).setEntityId(-1);
            this.hardcore = this.reducedDebugInfo = false;
            this.maxPlayers = -1;
            this.inventoryCache.reset();
            this.heldItemSlot = 0;
            this.enabledFeatures = new String[0];
        }
        this.spawnPosition = DEFAULT_SPAWN_POSITION;
        this.gameMode = null;
        this.thePlayer.setHealth(20.0f);
        this.thePlayer.setFood(20);
        this.thePlayer.setSaturation(5);
        this.thePlayer.getPotionEffectMap().clear();
        this.isSneaking = this.isSprinting = false;
    }

    @Override
    public String getSendingMessage() {
        return String.format(
                "Sending player position: (x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f)",
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getYaw(),
                this.getPitch()
        );
    }

    public static void sync() {
        if (nonNull(Proxy.getInstance().getClient())) {
            try {
                // intentionally sends an invalid inventory packet to issue a ServerWindowItems which corrects all inventory slot contents
                // pretty sure it requires a Notchian client to be connected to send the confirmTransaction stuff, can be implemented later if nesscesary
                Proxy.getInstance().getClient().sendAsync(new ServerboundContainerClickPacket(0,
                                                                                         -1337,
                                                                                         0,
                                                                                         ContainerActionType.CREATIVE_GRAB_MAX_STACK,
                                                                                         CreativeGrabAction.GRAB,
                                                                                         new ItemStack(1, 1),
                                                                                         Int2ObjectMaps.emptyMap()));
            } catch (final Exception e) {
                CLIENT_LOG.warn("Failed Player Sync", e);
            }
        }
    }

    public void setInventory(final int containerId, final ItemStack[] inventory) {
        this.inventoryCache.setInventory(containerId, inventory);
    }

    public ItemStack getEquipment(final EquipmentSlot slot) {
        var inventory = this.inventoryCache.getPlayerInventory();
        if (inventory == null) return null;
        return switch (slot) {
            case EquipmentSlot.HELMET -> inventory.getItemStack(5);
            case EquipmentSlot.CHESTPLATE -> inventory.getItemStack(6);
            case EquipmentSlot.LEGGINGS -> inventory.getItemStack(7);
            case EquipmentSlot.BOOTS -> inventory.getItemStack(8);
            case EquipmentSlot.OFF_HAND -> inventory.getItemStack(45);
            case EquipmentSlot.MAIN_HAND -> inventory.getItemStack(heldItemSlot + 36);
        };
    }

    // prefer calling getEquipment with a slot type instead of this, creates gc spam
    public Map<EquipmentSlot, ItemStack> getEquipment() {
        final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HELMET, getEquipment(EquipmentSlot.HELMET));
        equipment.put(EquipmentSlot.CHESTPLATE, getEquipment(EquipmentSlot.CHESTPLATE));
        equipment.put(EquipmentSlot.LEGGINGS, getEquipment(EquipmentSlot.LEGGINGS));
        equipment.put(EquipmentSlot.BOOTS, getEquipment(EquipmentSlot.BOOTS));
        equipment.put(EquipmentSlot.OFF_HAND, getEquipment(EquipmentSlot.OFF_HAND));
        equipment.put(EquipmentSlot.MAIN_HAND, getEquipment(EquipmentSlot.MAIN_HAND));
        return equipment;
    }

    public void setInventorySlot(final int containerId, ItemStack newItemStack, int slot) {
        this.inventoryCache.setItemStack(containerId, slot, newItemStack);
    }

    public double getX() {
        return this.thePlayer.getX();
    }

    public PlayerCache setX(double x) {
        this.thePlayer.setX(x);
        return this;
    }

    public double getY()    {
        return this.thePlayer.getY();
    }

    public PlayerCache setY(double y)    {
        this.thePlayer.setY(y);
        return this;
    }

    public double getZ()    {
        return this.thePlayer.getZ();
    }

    public PlayerCache setZ(double z)    {
        this.thePlayer.setZ(z);
        return this;
    }

    public float getYaw()    {
        return this.thePlayer.getYaw();
    }

    public PlayerCache setYaw(float yaw)    {
        this.thePlayer.setYaw(yaw);
        return this;
    }

    public float getPitch()    {
        return this.thePlayer.getPitch();
    }

    public PlayerCache setPitch(float pitch)    {
        this.thePlayer.setPitch(pitch);
        return this;
    }

    public int getEntityId()    {
        return this.thePlayer.getEntityId();
    }

    public PlayerCache setEntityId(int id)  {
        if (this.thePlayer.getEntityId() != -1) {
            this.entityCache.remove(this.thePlayer.getEntityId());
        }
        this.thePlayer.setEntityId(id);
        this.entityCache.add(this.thePlayer);
        return this;
    }

    public PlayerCache setUuid(UUID uuid) {
        this.thePlayer.setUuid(uuid);
        return this;
    }

    public double distanceToSelf(final Entity entity) {
        return Math.sqrt(
            Math.pow(getX() - entity.getX(), 2)
                + Math.pow(getY() - entity.getY(), 2)
                + Math.pow(getZ() - entity.getZ(), 2));
    }

    public void closeContainer(final int containerId) {
        this.inventoryCache.closeContainer(containerId);
    }

    public void openContainer(final int containerId, final ContainerType type, final Component title) {
        this.inventoryCache.openContainer(containerId, type, title);
    }

    public List<ItemStack> getPlayerInventory() {
        return this.inventoryCache.getPlayerInventory().getContents();
    }
}
