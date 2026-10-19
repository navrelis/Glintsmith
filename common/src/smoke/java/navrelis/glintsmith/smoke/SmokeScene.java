package navrelis.glintsmith.smoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The test scene, built on the server thread: a snow block wall (the whitest full block) as backdrop, the enchanted items the screenshots
 * show, and the camera poses. All coordinates are relative to the point where the player spawned.
 * <pre>
 *   z+  wall (snow blocks) at oz+6
 *    |   item frame (enchanted diamond pickaxe) at ox+2, oz+5
 *    |   dropped enchanted golden sword at ox-0.8, oz+3.2
 *    |   player at ox+0.5, oz+0.5, looking along +z
 * </pre>
 */
final class SmokeScene {
	/** Camera position (the feet of the player) and rotation. */
	record Pose(double x, double y, double z, float yaw, float pitch) {
		/** The pose of a player standing at (x, y, z) who looks at {@code target} with the eyes. */
		static Pose lookingAt(double x, double y, double z, Vec3 target) {
			double dx = target.x - x;
			double dy = target.y - (y + 1.62);
			double dz = target.z - z;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
			return new Pose(x, y, z, yaw, pitch);
		}
	}

	// Set on the server thread by build(), read by the client thread afterwards (the volatile flag orders them).
	volatile int ox;
	volatile int gy;
	volatile int oz;
	volatile boolean built;

	Pose firstPerson() {
		return new Pose(ox + 0.5, gy, oz + 0.5, 0.0f, 6.0f);
	}

	/** The player stands in front of the wall and faces the camera of the front third person view. */
	Pose thirdPersonFront() {
		return new Pose(ox + 0.5, gy, oz + 3.5, 180.0f, 0.0f);
	}

	Vec3 droppedPosition() {
		return new Vec3(ox - 0.8, gy + 1.1, oz + 3.2);
	}

	Pose droppedCloseUp() {
		Vec3 item = droppedPosition();
		return Pose.lookingAt(item.x, gy, item.z - 1.2, item.add(0.0, 0.3, 0.0));
	}

	Pose frameCloseUp() {
		return Pose.lookingAt(ox + 2.5, gy, oz + 4.5, new Vec3(ox + 2.5, gy + 1.5, oz + 5.9));
	}

	/** Builds everything. Must run on the server thread. */
	void build(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().get(0);
		ServerLevel level = player.serverLevel();
		ox = Mth.floor(player.getX());
		oz = Mth.floor(player.getZ());
		gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, oz);

		level.setDayTime(6000L);
		level.setWeatherParameters(6000, 0, false, false);

		// Backdrop: wide and high enough to fill the view of every camera pose
		for (int x = ox - 14; x <= ox + 14; x++) {
			for (int y = gy; y <= gy + 10; y++) {
				level.setBlock(new BlockPos(x, y, oz + 6), Blocks.SNOW_BLOCK.defaultBlockState(), 3);
			}
		}

		Registry<Enchantment> e = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

		// Hands: netherite sword (main), enchanted shield (off hand)
		var inventory = player.getInventory();
		inventory.selected = 0;
		inventory.setItem(0, enchanted(e, new ItemStack(Items.NETHERITE_SWORD), Enchantments.SHARPNESS, 5, Enchantments.FIRE_ASPECT, 2));
		player.setItemSlot(EquipmentSlot.OFFHAND, enchanted(e, new ItemStack(Items.SHIELD), Enchantments.UNBREAKING, 3, null, 0));

		// Worn armor: a full diamond set
		player.setItemSlot(EquipmentSlot.HEAD, enchanted(e, new ItemStack(Items.DIAMOND_HELMET), Enchantments.PROTECTION, 4, null, 0));
		player.setItemSlot(EquipmentSlot.CHEST, enchanted(e, new ItemStack(Items.DIAMOND_CHESTPLATE), Enchantments.PROTECTION, 4, null, 0));
		player.setItemSlot(EquipmentSlot.LEGS, enchanted(e, new ItemStack(Items.DIAMOND_LEGGINGS), Enchantments.PROTECTION, 4, null, 0));
		player.setItemSlot(EquipmentSlot.FEET, enchanted(e, new ItemStack(Items.DIAMOND_BOOTS), Enchantments.PROTECTION, 4, null, 0));

		// Inventory (shown by the inventory screenshot). Main inventory slot 13 stays empty: the mouse rests there.
		inventory.setItem(1, enchanted(e, new ItemStack(Items.BOW), Enchantments.POWER, 5, null, 0));
		inventory.setItem(2, enchanted(e, new ItemStack(Items.TRIDENT), Enchantments.LOYALTY, 3, null, 0));
		inventory.setItem(3, new ItemStack(Items.TOTEM_OF_UNDYING));
		inventory.setItem(4, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
		inventory.setItem(5, EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e.getHolderOrThrow(Enchantments.MENDING), 1)));
		inventory.setItem(6, enchanted(e, new ItemStack(Items.ELYTRA), Enchantments.UNBREAKING, 3, null, 0));
		inventory.setItem(7, enchanted(e, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.EFFICIENCY, 5, null, 0));
		inventory.setItem(8, enchanted(e, new ItemStack(Items.DIAMOND_SHOVEL), Enchantments.EFFICIENCY, 5, null, 0));
		ItemStack nearlyBroken = enchanted(e, new ItemStack(Items.IRON_AXE), Enchantments.EFFICIENCY, 3, null, 0);
		nearlyBroken.setDamageValue(nearlyBroken.getMaxDamage() - 5);
		inventory.setItem(9, nearlyBroken);
		inventory.setItem(10, enchanted(e, new ItemStack(Items.NETHERITE_AXE), Enchantments.SHARPNESS, 5, null, 0));
		inventory.setItem(11, enchanted(e, new ItemStack(Items.CROSSBOW), Enchantments.QUICK_CHARGE, 3, null, 0));
		inventory.setItem(12, enchanted(e, new ItemStack(Items.FISHING_ROD), Enchantments.LUCK_OF_THE_SEA, 3, null, 0));
		inventory.setItem(14, enchanted(e, new ItemStack(Items.SHEARS), Enchantments.EFFICIENCY, 5, null, 0));
		inventory.setItem(15, enchanted(e, new ItemStack(Items.DIAMOND_HOE), Enchantments.UNBREAKING, 3, null, 0));
		inventory.setItem(16, new ItemStack(Items.COMPASS));
		inventory.setChanged();

		spawnDropped(level, e);

		// An item frame on the wall with an enchanted diamond pickaxe
		ItemFrame frame = new ItemFrame(level, new BlockPos(ox + 2, gy + 1, oz + 5), Direction.NORTH);
		frame.setItem(enchanted(e, new ItemStack(Items.DIAMOND_PICKAXE), Enchantments.EFFICIENCY, 5, Enchantments.UNBREAKING, 3), false);
		level.addFreshEntity(frame);

		teleport(server, firstPerson());
		built = true;
	}

	/** A dropped enchanted golden sword: hovers in place, is never picked up and never despawns (but it spins). */
	private void spawnDropped(ServerLevel level, Registry<Enchantment> e) {
		Vec3 drop = droppedPosition();
		ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z,
			enchanted(e, new ItemStack(Items.GOLDEN_SWORD), Enchantments.SHARPNESS, 3, Enchantments.UNBREAKING, 3));
		entity.setDeltaMovement(Vec3.ZERO);
		entity.setNoGravity(true);
		entity.setNeverPickUp();
		entity.setUnlimitedLifetime();
		level.addFreshEntity(entity);
	}

	/** Moves the player (and with that the first person camera). Must run on the server thread. */
	static void teleport(MinecraftServer server, Pose pose) {
		ServerPlayer player = server.getPlayerList().getPlayers().get(0);
		player.connection.teleport(pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch());
	}

	/** Switches the game mode of the player. Must run on the server thread. */
	static void setGameMode(MinecraftServer server, GameType mode) {
		server.getPlayerList().getPlayers().get(0).setGameMode(mode);
	}

	private static ItemStack enchanted(Registry<Enchantment> registry, ItemStack stack, ResourceKey<Enchantment> first, int firstLevel,
		ResourceKey<Enchantment> second, int secondLevel) {
		stack.enchant(registry.getHolderOrThrow(first), firstLevel);
		if (second != null) {
			stack.enchant(registry.getHolderOrThrow(second), secondLevel);
		}
		return stack;
	}
}
