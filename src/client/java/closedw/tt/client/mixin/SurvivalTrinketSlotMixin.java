package closedw.tt.client.mixin;

import closedw.tt.client.theme.config.ModConfig;
import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import closedw.tt.client.theme.interfaces.TCTSurvivalTrinketSlot;
import dev.emi.trinkets.SurvivalTrinketSlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * 用主题自己的规则替换 Trinkets 那套基于焦点的槽位可见性：饰品槽现在活在 Curios 面板上，面板显示时它们就显示。
 */
@Mixin(value = SurvivalTrinketSlot.class, remap = false, priority = 1500)
public abstract class SurvivalTrinketSlotMixin extends Slot implements TCTSurvivalTrinketSlot {

	@Unique
	private boolean trinketsTooltip$enabled = true;

	private SurvivalTrinketSlotMixin(Inventory inventory, int index, int x, int y) {
		super(inventory, index, x, y);
	}

	/**
	 * @author Trinkets Tooltip
	 * @reason 饰品槽改画在 Curios 面板上，不再使用 Trinkets 自己的分组界面。
	 */
	@Overwrite
	public boolean isEnabled() {
		MinecraftClient client = MinecraftClient.getInstance();
		Screen screen = client.currentScreen;

		if (screen instanceof InventoryScreen inventoryScreen) {
			if (!(inventoryScreen.getScreenHandler() instanceof TCTPlayerScreenHandlerInterface tcp)) {
				return false;
			}
			return !inventoryScreen.getRecipeBookWidget().isOpen()
					&& tcp.getTrinketsShow()
					&& (!ModConfig.pagination || this.trinketsTooltip$enabled);
		} else if (screen instanceof CreativeInventoryScreen) {
			return false;
		}

		return ((SurvivalTrinketSlot) (Object) this).isTrinketFocused();
	}

	@Override
	public void setEnabled(boolean b) {
		this.trinketsTooltip$enabled = b;
	}
}
