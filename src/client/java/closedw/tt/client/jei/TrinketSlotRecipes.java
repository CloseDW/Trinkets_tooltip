package closedw.tt.client.jei;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 所有关于“哪些物品能放进哪个饰品槽”的逻辑。
 * 候选物品多到一页放不下的槽位会被拆成多个配方（“分块”）。
 * JEI 的翻页箭头是在一次查询拿到的配方列表里翻页，所以把一个槽位的所有分块
 * 一起交给它，箭头就会在这个槽位内部翻页。
 */
public final class TrinketSlotRecipes {

	public static final RecipeType<TrinketSlotRecipe> RECIPE_TYPE =
			RecipeType.create("trinkets-tooltip", "trinket_slot", TrinketSlotRecipe.class);

	public static final int COLUMNS = 9;
	public static final int ROWS = 10;
	public static final int PADDING = 5;
	public static final int TITLE_HEIGHT = 11;
	/** 每个分块显示的物品数量。 */
	public static final int PER_CHUNK = COLUMNS * ROWS;

	private static final Map<String, List<TrinketSlotRecipe>> BY_SLOT = new HashMap<>();
	private static final Map<Item, List<TrinketSlotRecipe>> BY_ITEM = new HashMap<>();
	private static List<TrinketSlotRecipe> allRecipes = List.of();

	private TrinketSlotRecipes() {
	}

	/**
	 * 某个饰品槽的一部分可装备物品。
	 */
	public record TrinketSlotRecipe(String group, String name, Text title, int chunk, int chunkCount,
									List<ItemStack> items) {
	}

	/** 建立缓存时客户端已知的槽位集合，用来判断缓存是否还对得上当前的槽位。 */
	private static List<String> cachedSlotKeys = List.of();

	/** 清空所有缓存，只应由 {@link #validateCache(PlayerEntity)} 和 JEI 的启停调用。 */
	public static void invalidate() {
		BY_SLOT.clear();
		BY_ITEM.clear();
		allRecipes = List.of();
		cachedSlotKeys = List.of();
	}

	/**
	 * 槽位集合和建立缓存时不一致就整体作废。
	 */
	private static void validateCache(PlayerEntity player) {
		List<String> keys = slotTypes(player).stream()
				.map(type -> type.getGroup() + "/" + type.getName())
				.sorted()
				.toList();

		if (!keys.equals(cachedSlotKeys)) {
			invalidate();
			cachedSlotKeys = keys;
		}
	}

	/** 客户端当前为该玩家所知道的所有槽位类型。 */
	public static List<SlotType> slotTypes(PlayerEntity player) {
		return TrinketsApi.getTrinketComponent(player)
				.map(component -> component.getInventory().values().stream()
						.flatMap(byName -> byName.values().stream())
						.map(TrinketInventory::getSlotType)
						.distinct()
						.toList())
				.orElse(List.of());
	}

	/**
	 * 能装进这个槽位的所有物品，判断用的正是 Trinkets 自己接收物品时的
	 * （{@code TrinketSlot.canInsert}）。
	 */
	public static List<ItemStack> candidates(SlotType type, PlayerEntity player) {
		TrinketInventory inventory = TrinketsApi.getTrinketComponent(player)
				.map(component -> component.getInventory()
						.getOrDefault(type.getGroup(), Map.of())
						.get(type.getName()))
				.orElse(null);

		if (inventory == null) {
			return List.of();
		}

		SlotReference reference = new SlotReference(inventory, 0);
		List<ItemStack> result = new ArrayList<>();

		for (Item item : Registries.ITEM) {
			if (item == Items.AIR) {
				continue;
			}

			ItemStack stack = item.getDefaultStack();
			if (TrinketSlot.canInsert(stack, reference, player)) {
				result.add(stack);
			}
		}

		return result;
	}

	/** 某个槽位的所有分块，缓存到槽位集合发生变化为止。 */
	public static List<TrinketSlotRecipe> recipesFor(SlotType type, PlayerEntity player) {
		validateCache(player);

		String key = type.getGroup() + "/" + type.getName();
		List<TrinketSlotRecipe> cached = BY_SLOT.get(key);

		if (cached != null) {
			return cached;
		}

		List<ItemStack> candidates = candidates(type, player);
		int chunkCount = Math.max(1, (candidates.size() + PER_CHUNK - 1) / PER_CHUNK);
		List<TrinketSlotRecipe> chunks = new ArrayList<>();

		for (int chunk = 0; chunk < chunkCount; chunk++) {
			int from = chunk * PER_CHUNK;
			int to = Math.min(candidates.size(), from + PER_CHUNK);

			// 只写翻译名会有歧义（hand/ring 和 offhand/ring 都叫 “Ring”），
			// 所以始终附上原始的 group/name；分块的槽位还会显示分块计数。
			Text title = chunkCount > 1
					? Text.translatable("jei.trinkets-tooltip.trinket_slot.chunked", type.getTranslation(),
							type.getGroup() + "/" + type.getName(), chunk + 1, chunkCount)
					: Text.translatable("jei.trinkets-tooltip.trinket_slot.named", type.getTranslation(),
							type.getGroup() + "/" + type.getName());

			chunks.add(new TrinketSlotRecipe(type.getGroup(), type.getName(), title, chunk, chunkCount,
					List.copyOf(candidates.subList(from, to))));
		}

		BY_SLOT.put(key, chunks);
		return chunks;
	}

	/** 所有槽位的所有分块，带缓存。JEI 浏览时使用。 */
	public static List<TrinketSlotRecipe> all(PlayerEntity player) {
		validateCache(player);

		if (!allRecipes.isEmpty()) {
			return allRecipes;
		}

		List<TrinketSlotRecipe> result = new ArrayList<>();

		for (SlotType type : slotTypes(player)) {
			result.addAll(recipesFor(type, player));
		}

		allRecipes = List.copyOf(result);
		return allRecipes;
	}

	/** 只要有任意一个饰品槽接受这件物品就返回 true。 */
	public static boolean anySlotAccepts(PlayerEntity player, Item item) {
		return !recipesForItem(player, item).isEmpty();
	}

	/**
	 * 接受这件物品的每个槽位的全部分块，带缓存。
	 */
	public static List<TrinketSlotRecipe> recipesForItem(PlayerEntity player, Item item) {
		validateCache(player);

		List<TrinketSlotRecipe> cached = BY_ITEM.get(item);

		if (cached != null) {
			return cached;
		}

		List<TrinketSlotRecipe> result = new ArrayList<>();

		for (SlotType type : slotTypes(player)) {
			List<TrinketSlotRecipe> chunks = recipesFor(type, player);
			int containing = -1;

			for (int i = 0; i < chunks.size() && containing < 0; i++) {
				for (ItemStack stack : chunks.get(i).items()) {
					if (stack.getItem() == item) {
						containing = i;
						break;
					}
				}
			}

			if (containing < 0) {
				continue;
			}

			// 装着被查询物品的那一块排在最前面，让查询界面直接停在它上面，随后跟着同一个槽位的其余分块。
			result.add(chunks.get(containing));

			for (int i = 0; i < chunks.size(); i++) {
				if (i != containing) {
					result.add(chunks.get(i));
				}
			}
		}

		BY_ITEM.put(item, result);
		return result;
	}
}
