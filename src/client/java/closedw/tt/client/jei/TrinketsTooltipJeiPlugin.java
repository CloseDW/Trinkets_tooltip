package closedw.tt.client.jei;

import closedw.tt.TrinketsTooltip;
import closedw.tt.client.theme.ThemeClient;
import dev.emi.trinkets.api.SlotType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.advanced.ISimpleRecipeManagerPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * JEI 集成。只通过 {@code jei_mod_plugin} 入口在装了 JEI 时加载，
 * 所以没装 JEI 时不会碰到任何 JEI 类型。
 */
@JeiPlugin
public class TrinketsTooltipJeiPlugin implements IModPlugin {

	private static final Identifier UID = new Identifier(TrinketsTooltip.MOD_ID, "jei_plugin");

	private final SlotPlugins slotPlugins = new SlotPlugins();

	@Override
	public Identifier getPluginUid() {
		return UID;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new TrinketSlotCategory(registration.getJeiHelpers().getGuiHelper()));
	}

	@Override
	public void registerAdvanced(IAdvancedRegistration registration) {
		registration.addTypedRecipeManagerPlugin(TrinketSlotRecipes.RECIPE_TYPE, slotPlugins);
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		TrinketSlotRecipes.invalidate();
		ThemeClient.setSlotLookup(type -> open(jeiRuntime, type));
	}

	@Override
	public void onRuntimeUnavailable() {
		TrinketSlotRecipes.invalidate();
		ThemeClient.setSlotLookup(null);
	}

	/** 在 JEI 里打开这个槽位的所有分块，让它的箭头在槽位内部翻页。 */
	private void open(IJeiRuntime runtime, SlotType type) {
		PlayerEntity player = MinecraftClient.getInstance().player;

		if (player == null) {
			return;
		}

		List<TrinketSlotRecipes.TrinketSlotRecipe> chunks = TrinketSlotRecipes.recipesFor(type, player);

		if (!chunks.isEmpty()) {
			runtime.getRecipesGui().showRecipes(
					new TrinketSlotCategory(runtime.getJeiHelpers().getGuiHelper()),
					chunks,
					List.of());
		}
	}

	/**
	 * 动态提供配方。槽位由数据包驱动，只有玩家进入世界之后才会同步过来，
	 * 而那已经是 JEI 让插件注册之后很久了，所以这里必须用 manager 插件，而不是静态注册配方。
	 */
	private static class SlotPlugins implements ISimpleRecipeManagerPlugin<TrinketSlotRecipes.TrinketSlotRecipe> {

		@Override
		public boolean isHandledInput(ITypedIngredient<?> input) {
			PlayerEntity player = MinecraftClient.getInstance().player;

			if (player == null) {
				return false;
			}

			return input.getIngredient(VanillaTypes.ITEM_STACK)
					.map(ItemStack::getItem)
					.map(item -> TrinketSlotRecipes.anySlotAccepts(player, item))
					.orElse(false);
		}

		@Override
		public boolean isHandledOutput(ITypedIngredient<?> output) {
			return false;
		}

		@Override
		public List<TrinketSlotRecipes.TrinketSlotRecipe> getRecipesForInput(ITypedIngredient<?> input) {
			PlayerEntity player = MinecraftClient.getInstance().player;

			if (player == null) {
				return List.of();
			}

			Item item = input.getIngredient(VanillaTypes.ITEM_STACK).map(ItemStack::getItem).orElse(null);

			return item == null ? List.of() : TrinketSlotRecipes.recipesForItem(player, item);
		}

		@Override
		public List<TrinketSlotRecipes.TrinketSlotRecipe> getRecipesForOutput(ITypedIngredient<?> output) {
			return List.of();
		}

		@Override
		public List<TrinketSlotRecipes.TrinketSlotRecipe> getAllRecipes() {
			PlayerEntity player = MinecraftClient.getInstance().player;
			return player == null ? List.of() : TrinketSlotRecipes.all(player);
		}
	}
}
