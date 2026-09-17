package closedw.tt.client.theme;

import closedw.tt.TrinketsTooltip;
import closedw.tt.client.theme.config.ModConfig;
import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import closedw.tt.client.theme.interfaces.TCTSurvivalTrinketSlot;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.SlotType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Curios 风格饰品面板的渲染与布局计算。
 */
public final class ThemeClient {

	public static final Identifier INVENTORY_REVAMP = new Identifier(TrinketsTooltip.MOD_ID, "textures/gui/inventory_revamp.png");

	public static final int REVAMP_WIDTH = 54;
	public static final int REVAMP_HEIGHT = 150;
	private static final int REVAMP_FRAME_U = 0;
	private static final int REVAMP_FRAME_U_REST = 7;
	private static final int REVAMP_CAP_V = 141;
	private static final int REVAMP_SLOT_U = 32;
	private static final int REVAMP_ARROW_U_PREVIOUS = 32;
	private static final int REVAMP_ARROW_U_NEXT = 43;
	private static final int REVAMP_ARROW_V_ENABLED = 126;
	private static final int REVAMP_ARROW_V_DISABLED = 138;

	public static final Identifier CURIOS_BUTTON_TEXTURE = new Identifier(TrinketsTooltip.MOD_ID, "textures/gui/inventory.png");

	private ThemeClient() {
	}

	public static void init() {
		if (!isModLoaded("trinkets")) {
			TrinketsTooltip.LOGGER.error("Trinkets isn't loaded. Without it this mod can't work properly.");
		}

		// 读取 config/trinkets-tooltip.properties。
		ModConfig.load();
	}

	/**
	 * 在原版样式的背包和 Curios 样式的背包之间切。
	 */
	public static void toggleCuriosView() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			return;
		}

		if (client.player.playerScreenHandler instanceof TCTPlayerScreenHandlerInterface tcp) {
			tcp.setTrinketsShow(!tcp.getTrinketsShow());
			updateSlots(client.player.playerScreenHandler.slots, tcp);
		}
	}

	/**
	 * 由 JEI 那一侧实现，并通过 {@link #setSlotLookup} 安装进来。
	 * 这一层间接调用把所有 JEI 类型都挡在本类之外，所以没装 JEI 时模组照常工作。
	 */
	public interface SlotLookup {
		/** 展示所有能装备到这个槽位里的东西。 */
		void open(SlotType type);
	}

	@Nullable
	private static SlotLookup slotLookup;

	public static void setSlotLookup(@Nullable SlotLookup lookup) {
		slotLookup = lookup;
	}

	/**
	 * @return 这次点击被 JEI 查询消费掉时返回 true。
	 */
	public static boolean openSlotLookup(SlotType type) {
		if (!ModConfig.jei_slot_lookup || slotLookup == null) {
			return false;
		}

		slotLookup.open(type);
		return true;
	}

	public static boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	/**
	 * 玩家 screen handler 上的饰品槽数量，由 Trinkets 报告。
	 */
	public static int getTrinketSlotCount(TCTPlayerScreenHandlerInterface tcp) {
		if (tcp instanceof TrinketPlayerScreenHandler handler) {
			return handler.trinkets$getTrinketSlotEnd() - handler.trinkets$getTrinketSlotStart();
		}
		return 0;
	}

	/**
	 * 面板的行数。
	 */
	public static int getPanelRows(int length) {
		if (length < 1) {
			return 0;
		}
		return Math.min(MathHelper.ceil((float) length / ModConfig.min_width), ModConfig.max_height);
	}

	/**
	 * 面板的列数。
	 */
	public static int getPanelColumns(int length) {
		if (length < 1) {
			return 0;
		}
		if (ModConfig.pagination) {
			return ModConfig.min_width;
		}
		return (int) Math.ceil((float) length / ModConfig.max_height);
	}

	public static void drawBackground(DrawContext context, int x, int y, TCTPlayerScreenHandlerInterface tcp) {
		int length = getTrinketSlotCount(tcp);
		int columns = getPanelColumns(length);
		int rows = getPanelRows(length);

		if (columns < 1 || rows < 1) {
			return;
		}

		boolean paged = isPaginationVisible(length);

		int upperHeight = 7 + rows * 18;
		if (paged) {
			upperHeight += PAGE_STRIP_HEIGHT;
		}

		int xOffset = getPanelLeft(columns);
		for (int c = 0; c < columns; c++) {
			int texX = c == 0 ? REVAMP_FRAME_U : REVAMP_FRAME_U_REST;

			context.drawTexture(INVENTORY_REVAMP, x + xOffset, y, texX, 0, 25, upperHeight,
					REVAMP_WIDTH, REVAMP_HEIGHT);
			context.drawTexture(INVENTORY_REVAMP, x + xOffset, y + upperHeight, texX, REVAMP_CAP_V,
					25, 7, REVAMP_WIDTH, REVAMP_HEIGHT);

			if (columns == 1) {
				context.drawTexture(INVENTORY_REVAMP, x + xOffset + 7, y, REVAMP_FRAME_U_REST, 0,
						25, upperHeight, REVAMP_WIDTH, REVAMP_HEIGHT);
				context.drawTexture(INVENTORY_REVAMP, x + xOffset + 7, y + upperHeight,
						REVAMP_FRAME_U_REST, REVAMP_CAP_V, 25, 7, REVAMP_WIDTH, REVAMP_HEIGHT);
			}

			xOffset += c == 0 ? 25 : 18;
		}

		int slotStripY = (paged ? PAGE_STRIP_HEIGHT : 0) + 7;
		for (int c = 0; c < columns; c++) {
			context.drawTexture(INVENTORY_REVAMP, x + getSlotStripX(c, columns), y + slotStripY,
					REVAMP_SLOT_U, 0, 18, rows * 18, REVAMP_WIDTH, REVAMP_HEIGHT);
		}

		// 网格中这一页没有真实饰品槽的空格，看起来仍然会像一个空槽位，所以用面板底色把它们盖掉，连同凹槽的边框和高光一起抹平。
		int onPage = Math.min(getSlotsPerPage(), Math.max(0, length - tcp.getScrollIndex() * getSlotsPerPage()));
		int cells = columns * rows;
		for (int cell = onPage; cell < cells; cell++) {
			int cellX = x + getSlotStripX(cell % columns, columns);
			int cellY = y + slotStripY + (cell / columns) * 18;
			context.fill(cellX, cellY, cellX + 18, cellY + 18, PANEL_BACKGROUND_COLOR);
		}

		if (paged) {
			drawPageButtons(context, tcp, x, y);
		}
	}

	/** 面板自己的底色。 */
	public static final int PANEL_BACKGROUND_COLOR = 0xFFC6C6C6;

	/** 有分页。 */
	public static final int PAGE_STRIP_HEIGHT = 8;

	/** 面板左边缘（相对 GUI 坐标）。 */
	public static int getPanelLeft(int columns) {
		return -33 - (columns - 1) * 18;
	}

	/** 某一列槽位凹槽的左边缘（相对 GUI 坐标）。 */
	public static int getSlotStripX(int column, int columns) {
		return -26 - (columns - 1) * 18 + column * 18;
	}

	/** 饰品槽的位置，也就是凹槽向内缩一像素。 */
	public static int getSlotX(int column, int columns) {
		return getSlotStripX(column, columns) + 1;
	}

	public static int getSlotY(int row, boolean paged) {
		return (paged ? PAGE_STRIP_HEIGHT : 0) + 8 + row * 18;
	}

	private static void drawPageButtons(DrawContext context, TCTPlayerScreenHandlerInterface tcp, int x, int y) {
		int page = tcp.getScrollIndex();
		int pages = getPageCount(getTrinketSlotCount(tcp));
		int[] buttons = getPageButtons(tcp);

		int previousY = page > 0 ? REVAMP_ARROW_V_ENABLED : REVAMP_ARROW_V_DISABLED;
		int nextY = page + 1 < pages ? REVAMP_ARROW_V_ENABLED : REVAMP_ARROW_V_DISABLED;

		context.drawTexture(INVENTORY_REVAMP, x + buttons[0], y + buttons[2], REVAMP_ARROW_U_PREVIOUS,
				previousY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, REVAMP_WIDTH, REVAMP_HEIGHT);
		context.drawTexture(INVENTORY_REVAMP, x + buttons[1], y + buttons[2], REVAMP_ARROW_U_NEXT,
				nextY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, REVAMP_WIDTH, REVAMP_HEIGHT);
	}

	/**
	 * 每页的槽位数量，Curios 风格：一页是 {@code min_width} 列、
	 * 每列 {@code max_height} 行。
	 */
	public static int getSlotsPerPage() {
		return ModConfig.min_width * ModConfig.max_height;
	}

	public static int getPageCount(int length) {
		if (length < 1 || !ModConfig.pagination) {
			return 1;
		}
		return MathHelper.ceil((float) length / getSlotsPerPage());
	}

	/**
	 * 第 {@code i} 个饰品槽（在饰品槽区间内从 0 开始）是否位于指定页。
	 * 布局和悬停检测共用这个方法，所以两者永远不会对不上。
	 */
	public static boolean isSlotVisible(int i, int page) {
		if (!ModConfig.pagination) {
			return true;
		}

		int first = page * getSlotsPerPage();
		return i >= first && i < first + getSlotsPerPage();
	}

	/**
	 * 移动并启用/禁用饰品槽，让只有当前页会被画出来。
	 * 每帧都要调用，因为饰品数据一同步 Trinkets 就会重新添加它的槽位。
	 */
	public static void updateSlots(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp) {
		if (!(tcp instanceof TrinketPlayerScreenHandler handler)) {
			return;
		}

		int start = handler.trinkets$getTrinketSlotStart();
		int count = handler.trinkets$getTrinketSlotEnd() - start;
		int first = tcp.getScrollIndex() * getSlotsPerPage();
		int columns = getPanelColumns(count);
		boolean paged = isPaginationVisible(count);

		for (int i = 0; i < count; i++) {
			int slotIndex = start + i;
			if (slotIndex >= slots.size()) {
				break;
			}

			Slot slot = slots.get(slotIndex);
			if (!(slot instanceof TCTSurvivalTrinketSlot themed)) {
				continue;
			}

			boolean visible = isSlotVisible(i, tcp.getScrollIndex());
			themed.setEnabled(visible);

			if (!visible) {
				continue;
			}

			int column;
			int row;
			if (ModConfig.pagination) {
				int visibleIndex = i - first;
				column = visibleIndex % Math.max(1, columns);
				row = visibleIndex / Math.max(1, columns);
			} else {
				column = i / ModConfig.max_height;
				row = i % ModConfig.max_height;
			}

			slot.x = getSlotX(column, Math.max(1, columns));
			slot.y = getSlotY(row, paged);
		}
	}

	/**
	 * 饰品槽的本地化名称。
	 * Trinkets 的 {@link SlotType#getTranslation()} 在槽位完全没有翻译时会直接渲染
	 * 原始的翻译键，而第三方模组添加的槽位正是这种情况。
	 */
	public static Text getSlotName(SlotType type) {
		MutableText translation = type.getTranslation();

		if (translation.getContent() instanceof TranslatableTextContent content && !I18n.hasTranslation(content.getKey())) {
			String name = type.getName();
			if (!name.isEmpty()) {
				return Text.literal(Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase());
			}
		}

		return translation;
	}

	/**
	 * 在会话中第一次真正画出槽位名称提示时记一条日志，只记一次，
	 * 这样看 bug 报告就能判断这个功能到底有没有被触发。
	 */
	public static void reportSlotNameDrawn(SlotType type, Text name) {
		if (slotNameReported) {
			return;
		}

		slotNameReported = true;
		TrinketsTooltip.LOGGER.info("Slot name on hover is active (first hovered slot: {}.{} -> '{}')",
				type.getGroup(), type.getName(), name.getString());
	}

	private static boolean slotNameReported = false;

	/**
	 * 给定鼠标绝对坐标下命中的饰品槽，没有则返回 {@code null}。
	 * 可见性由本模组自己的布局计算决定，而不是 {@link Slot#isEnabled()}，
	 * 这样就不会被别的模组的槽位可见性规则搞坏。判定范围与原版
	 * {@code HandledScreen.isPointWithinBounds} 的额外留白保持一致。
	 */
	@Nullable
	public static Slot getHoveredTrinketSlot(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp,
			double mouseX, double mouseY, int guiX, int guiY) {
		if (!(tcp instanceof TrinketPlayerScreenHandler handler)) {
			return null;
		}

		int start = handler.trinkets$getTrinketSlotStart();
		int count = handler.trinkets$getTrinketSlotEnd() - start;

		for (int i = 0; i < count; i++) {
			if (!isSlotVisible(i, tcp.getScrollIndex())) {
				continue;
			}

			int slotIndex = start + i;
			if (slotIndex >= slots.size()) {
				break;
			}

			Slot slot = slots.get(slotIndex);
			double slotX = guiX + slot.x;
			double slotY = guiY + slot.y;
			if (mouseX >= slotX - 1 && mouseX < slotX + 17 && mouseY >= slotY - 1 && mouseY < slotY + 17) {
				return slot;
			}
		}

		return null;
	}

	/**
	 * 像 Curios 那样翻到另一页，然后重新排布可见的槽位。
	 */
	public static void changePage(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp, int delta) {
		if (ModConfig.pagination) {
			int pages = getPageCount(getTrinketSlotCount(tcp));
			int page = MathHelper.clamp(tcp.getScrollIndex() + delta, 0, Math.max(0, pages - 1));
			tcp.setScrollIndex(page);
		}

		updateSlots(slots, tcp);
	}

	public static boolean isPaginationVisible(int length) {
		return ModConfig.pagination && getPageCount(length) > 1;
	}

	public static final int PAGE_BUTTON_WIDTH = 11;
	public static final int PAGE_BUTTON_HEIGHT = 12;

	/**
	 * 翻页箭头的几何位置，使用相对 GUI 坐标：
	 * {@code [上一页 x, 下一页 x, y]}。
	 */
	public static int[] getPageButtons(TCTPlayerScreenHandlerInterface tcp) {
		return new int[] { -28, -17, 0 };
	}

	public static boolean isClickInPageButtons(TCTPlayerScreenHandlerInterface tcp, double mouseX, double mouseY, int x, int y) {
		if (!isPaginationVisible(getTrinketSlotCount(tcp))) {
			return false;
		}

		int[] buttons = getPageButtons(tcp);
		double top = y + buttons[2];
		double bottom = top + PAGE_BUTTON_HEIGHT;
		if (mouseY < top || mouseY >= bottom) {
			return false;
		}

		double left = x + buttons[0];
		double right = x + buttons[1] + PAGE_BUTTON_WIDTH;
		return mouseX >= left && mouseX < right;
	}

	/** 点击命中的是“下一页”箭头而不是“上一页”箭头时返回 true。 */
	public static boolean isClickInNextPage(TCTPlayerScreenHandlerInterface tcp, double mouseX, int x) {
		int[] buttons = getPageButtons(tcp);
		return mouseX >= x + buttons[1];
	}

	public static boolean isMouseOverPanel(TCTPlayerScreenHandlerInterface tcp, double mouseX, double mouseY, int x, int y) {
		int[] bounds = getPanelBounds(tcp);
		if (bounds[2] <= bounds[0]) {
			return false;
		}

		int localX = (int) Math.round(mouseX) - x;
		int localY = (int) Math.round(mouseY) - y;
		return localX >= bounds[0] && localX < bounds[2] && localY >= bounds[1] && localY < bounds[3];
	}

	/**
	 * 面板的几何范围，使用相对 GUI 坐标：{@code [left, top, right, bottom]}。
	 */
	public static int[] getPanelBounds(TCTPlayerScreenHandlerInterface tcp) {
		int length = getTrinketSlotCount(tcp);
		int columns = getPanelColumns(length);
		int rows = getPanelRows(length);

		if (columns < 1 || rows < 1) {
			return new int[] { 0, 0, 0, 0 };
		}

		int left = getPanelLeft(columns);
		int bottom = 7 + rows * 18 + (isPaginationVisible(length) ? PAGE_STRIP_HEIGHT : 0) + 7;
		return new int[] { left, 0, -1, bottom };
	}

	/**
	 * 光标是否在面板上方，也就是滚轮翻页生效的区域。
	 */
	public static boolean isScrolledInTrinkets(TCTPlayerScreenHandlerInterface tcp, double mouseX, double mouseY, int x, int y) {
		if (ModConfig.scrolling_outside_boundary) {
			return true;
		}

		int length = getTrinketSlotCount(tcp);
		int h = MathHelper.ceil((float) length / ModConfig.min_width) - ModConfig.max_height > 0
				? ModConfig.max_height
				: length;
		x -= 8 + 18 * ModConfig.min_width;
		y += 10;
		int xx = x + 9 + 18 * ModConfig.min_width;
		int yy = y + 14 + 18 * h;
		return mouseX >= (double) x && mouseY >= (double) y && mouseX < (double) xx && mouseY < (double) yy;
	}
}
