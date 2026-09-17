package closedw.tt.client.jei;

import dev.emi.trinkets.api.SlotType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 列出某个饰品槽可装备物品的 JEI 页面。
 * 物品注册为 {@link RecipeIngredientRole#INPUT}
 */
public class TrinketSlotCategory implements IRecipeCategory<TrinketSlotRecipes.TrinketSlotRecipe> {

	public static final Text TITLE = Text.translatable("jei.trinkets-tooltip.trinket_slot");

	private final IGuiHelper guiHelper;

	@Nullable
	private IDrawable icon;

	public TrinketSlotCategory(IGuiHelper guiHelper) {
		this.guiHelper = guiHelper;
	}

	@Override
	public RecipeType<TrinketSlotRecipes.TrinketSlotRecipe> getRecipeType() {
		return TrinketSlotRecipes.RECIPE_TYPE;
	}

	@Override
	public Text getTitle() {
		return TITLE;
	}

	@Override
	public int getWidth() {
		return TrinketSlotRecipes.COLUMNS * 18 + TrinketSlotRecipes.PADDING * 2;
	}

	@Override
	public int getHeight() {
		return TrinketSlotRecipes.TITLE_HEIGHT + TrinketSlotRecipes.ROWS * 18
				+ TrinketSlotRecipes.PADDING * 2;
	}

	/**
	 * 标签页图标延迟到第一次需要时才解析，取自第一件能放进任意饰品槽的物品，
	 */
	@Override
	@Nullable
	public IDrawable getIcon() {
		if (icon == null) {
			MinecraftClient client = MinecraftClient.getInstance();

			if (client.player != null) {
				for (SlotType type : TrinketSlotRecipes.slotTypes(client.player)) {
					List<ItemStack> candidates = TrinketSlotRecipes.candidates(type, client.player);

					if (!candidates.isEmpty()) {
						icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, candidates.get(0));
						break;
					}
				}
			}
		}

		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, TrinketSlotRecipes.TrinketSlotRecipe recipe,
			IFocusGroup focuses) {
		List<ItemStack> items = recipe.items();
		int cells = Math.min(items.size(), TrinketSlotRecipes.PER_CHUNK);

		for (int i = 0; i < cells; i++) {
			int x = TrinketSlotRecipes.PADDING + (i % TrinketSlotRecipes.COLUMNS) * 18;
			int y = TrinketSlotRecipes.TITLE_HEIGHT + TrinketSlotRecipes.PADDING
					+ (i / TrinketSlotRecipes.COLUMNS) * 18;

			builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStacks(List.of(items.get(i)));
		}
	}

	/** 在网格上方画出该槽位自己的本地化名称。 */
	@Override
	public void draw(TrinketSlotRecipes.TrinketSlotRecipe recipe, IRecipeSlotsView slots,
			DrawContext context, double mouseX, double mouseY) {
		context.drawText(MinecraftClient.getInstance().textRenderer, recipe.title(),
				TrinketSlotRecipes.PADDING, TrinketSlotRecipes.PADDING, 0x404040, false);
	}
}
