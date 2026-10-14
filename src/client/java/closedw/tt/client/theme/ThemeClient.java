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
 *
 * <p>面板有两种互斥的排版，由 {@code ModConfig.scrollbar} 决定：
 * <ul>
 *     <li>分页（默认）：面板顶端一条翻页箭头，一页 {@code min_width} 列 × {@code max_height} 行；</li>
 *     <li>滚动条：面板左侧一条 Baubles 风格的滚动条，面板固定 {@code max_height} 行，按行滚动。</li>
 * </ul>
 * 两者都是「把第 N 行到第 N + {@code max_height} 行画出来」，只是 N 一个由页码、一个由滚动量算出来，
 * 所以槽位摆放、空白格抹平、悬停判定全都共用同一套按行开窗的逻辑。
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

	/** 14x28 的双帧贴图：上半张是"面板关着"，下半张是"鼠标悬停"。 */
	public static final Identifier CURIOS_BUTTON_TEXTURE = new Identifier(TrinketsTooltip.MOD_ID, "textures/gui/inventory.png");

	/**
	 * "面板开着"那一张。1.20 的按钮原本只用一张 14x28 的双帧图（普通 + 悬停），
	 * 装不下第三种状态，所以打开态另给一张 14x14 的图，绘制方式见
	 * {@link CuriosPanelButton}。
	 */
	public static final Identifier CURIOS_BUTTON_OPEN = new Identifier(TrinketsTooltip.MOD_ID, "textures/gui/curios_button_open.png");

	/* ------------------------------------------------------------------ 左侧滚动条（Baubles 风格） */

	/**
	 * 滚动条贴图。原图是 Baubles 的 {@code baubles:textures/gui/baubles_container.png}：
	 * 轨道在 {@code (0,0)} 处 18x166，滑块在 {@code (0,167)} 处 6x89，同一张图里还画着
	 * Baubles 自己的槽位、按钮和遮挡块。这里只把那两块拼进一张 24x166 的新图
	 * （轨道仍在 {@code (0,0)}，滑块挪到 {@code (18,0)}），省得把无关素材一起带上。
	 *
	 * <p>轨道比本模组的面板高（166 对最多 148），所以按九宫格用：顶部 16 行、底部 8 行原样画，
	 * 中间 142 行压缩或拉伸 —— 这一段正好是 Baubles 里滑块能走完的凹槽区间。
	 */
	public static final Identifier BAUBLES_SCROLLER = new Identifier(TrinketsTooltip.MOD_ID, "textures/gui/baubles_scroller.png");

	private static final int SCROLLER_TEXTURE_WIDTH = 24;
	private static final int SCROLLER_TEXTURE_HEIGHT = 166;
	private static final int SCROLLER_WIDTH = 18;
	private static final int SCROLLER_TRACK_TOP_V = 0;
	private static final int SCROLLER_TRACK_TOP_HEIGHT = 16;
	private static final int SCROLLER_TRACK_MIDDLE_V = 16;
	private static final int SCROLLER_TRACK_MIDDLE_HEIGHT = 142;
	private static final int SCROLLER_TRACK_BOTTOM_V = 158;
	private static final int SCROLLER_TRACK_BOTTOM_HEIGHT = 8;
	private static final int SCROLLER_THUMB_U = 18;
	private static final int SCROLLER_THUMB_V = 0;
	private static final int SCROLLER_THUMB_WIDTH = 6;
	private static final int SCROLLER_THUMB_TEXTURE_HEIGHT = 89;
	/** 滑块在轨道里的横向位置：Baubles 也是从轨道左边 6 像素处开始画。 */
	private static final int SCROLLER_THUMB_INSET_X = 6;
	/** 滑块最短高度，免得行数一多滑块就细得看不出来。 */
	private static final int SCROLLER_THUMB_MIN_HEIGHT = 18;
	/** 滚动条与面板之间的间隙。Baubles 自己留 1～2 像素，这里取 1，让两者贴得更紧。 */
	private static final int SCROLLER_GAP = 1;

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
	 * 面板的列数。两种模式都用 {@code min_width} 列，所以列数直接等于 {@code min_width}
	 * （从前可以用配置关掉分页把槽位一次铺开，那个开关已经取消）。
	 */
	public static int getPanelColumns(int length) {
		if (length < 1) {
			return 0;
		}

		return ModConfig.min_width;
	}

	/** 面板高度：上下各 7 像素边框加中间的行，分页条另算。 */
	public static int getPanelHeight(int rows, boolean paged) {
		return 7 + rows * 18 + (paged ? PAGE_STRIP_HEIGHT : 0) + 7;
	}

	/** 内容一共有多少行。 */
	public static int getTotalRows(int length) {
		int columns = getPanelColumns(length);

		if (columns < 1 || length < 1) {
			return 0;
		}

		return MathHelper.ceil((float) length / columns);
	}

	/** 滚动条模式下能滚到的最大行偏移；内容不足一屏时是 0。 */
	public static int getMaxScrollRow(int length) {
		return Math.max(0, getTotalRows(length) - ModConfig.max_height);
	}

	/** 用的是滚动条而不是翻页箭头。 */
	public static boolean isScrollbarMode() {
		return ModConfig.scrollbar;
	}

	/** 滚动条是否要画：开了这个模式，而且内容确实超出一屏。 */
	public static boolean isScrollbarVisible(int length) {
		return isScrollbarMode() && getMaxScrollRow(length) > 0;
	}

	/**
	 * 当前窗口的第一行。分页模式下行偏移是「页码 × {@code max_height}」，滚动条模式下就是滚动量本身。
	 * 两种模式共用同一套「按行开窗」的布局，槽位摆放和命中判定因此不会出现两份算法。
	 */
	public static int getFirstVisibleRow(TCTPlayerScreenHandlerInterface tcp) {
		if (isScrollbarMode()) {
			return tcp.getScrollIndex();
		}

		return tcp.getScrollIndex() * ModConfig.max_height;
	}

	/** 当前窗口第一个饰品槽的序号。 */
	public static int getFirstVisibleSlot(int length, TCTPlayerScreenHandlerInterface tcp) {
		return getFirstVisibleRow(tcp) * Math.max(1, getPanelColumns(length));
	}

	/**
	 * 把存下来的滚动量收进合法范围。配置里行数上限改了、或者刚在两种模式之间切换时，
	 * 原来的页码/行偏移可能已经越界，所以每帧排布之前都先收一次。
	 */
	public static void clampScrollIndex(TCTPlayerScreenHandlerInterface tcp, int length) {
		int max = isScrollbarMode() ? getMaxScrollRow(length) : Math.max(0, getPageCount(length) - 1);
		int clamped = MathHelper.clamp(tcp.getScrollIndex(), 0, max);

		if (clamped != tcp.getScrollIndex()) {
			tcp.setScrollIndex(clamped);
		}
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

		// 网格里这一屏没有真实饰品槽的空格，看起来仍然会像一个空槽位，所以用面板底色把它们盖掉，连同凹槽的边框和高光一起抹平。
		// 分页模式下「这一屏」是一页，滚动条模式下是当前那一窗。
		int onPage = Math.min(getSlotsPerPage(), Math.max(0, length - getFirstVisibleSlot(length, tcp)));
		int cells = columns * rows;
		for (int cell = onPage; cell < cells; cell++) {
			int cellX = x + getSlotStripX(cell % columns, columns);
			int cellY = y + slotStripY + (cell / columns) * 18;
			context.fill(cellX, cellY, cellX + 18, cellY + 18, PANEL_BACKGROUND_COLOR);
		}

		if (paged) {
			drawPageButtons(context, tcp, x, y);
		}

		if (isScrollbarVisible(length)) {
			drawScrollbar(context, x, y, columns, rows, length, tcp);
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
		int[] buttons = getPageButtons();

		int previousY = page > 0 ? REVAMP_ARROW_V_ENABLED : REVAMP_ARROW_V_DISABLED;
		int nextY = page + 1 < pages ? REVAMP_ARROW_V_ENABLED : REVAMP_ARROW_V_DISABLED;

		context.drawTexture(INVENTORY_REVAMP, x + buttons[0], y + buttons[2], REVAMP_ARROW_U_PREVIOUS,
				previousY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, REVAMP_WIDTH, REVAMP_HEIGHT);
		context.drawTexture(INVENTORY_REVAMP, x + buttons[1], y + buttons[2], REVAMP_ARROW_U_NEXT,
				nextY, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT, REVAMP_WIDTH, REVAMP_HEIGHT);
	}

	/* ------------------------------------------------------------------ 滚动条几何 */

	/** 滚动条最左边（相对 GUI 坐标）：贴在面板左侧，和面板之间留 {@value #SCROLLER_GAP} 像素。 */
	public static int getScrollbarLeft(int columns) {
		return getPanelLeft(columns) - SCROLLER_GAP - SCROLLER_WIDTH;
	}

	/** 滚动条高度 = 面板高度（滚动条模式下没有分页条）。 */
	public static int getScrollbarHeight(int rows) {
		return getPanelHeight(rows, false);
	}

	/**
	 * 滚动条整体范围，相对 GUI 坐标：{@code [left, top, right, bottom]}。
	 * 没开这个模式、或者内容一屏放得下时返回空范围。
	 */
	public static int[] getScrollbarBounds(TCTPlayerScreenHandlerInterface tcp) {
		int length = getTrinketSlotCount(tcp);
		int columns = getPanelColumns(length);
		int rows = getPanelRows(length);

		if (columns < 1 || rows < 1 || !isScrollbarVisible(length)) {
			return new int[] { 0, 0, 0, 0 };
		}

		int left = getScrollbarLeft(columns);
		return new int[] { left, 0, left + SCROLLER_WIDTH, getScrollbarHeight(rows) };
	}

	/**
	 * 鼠标是否落在滚动条上。判定就是滚动条本身那一块，不像面板那样往外留白 ——
	 * 左边是屏幕边缘方向，多留白反而会抢走本不属于它的点击。
	 *
	 * @param mouseX 绝对屏幕坐标；{@code guiX}/{@code guiY} 是界面左上角。
	 */
	public static boolean isMouseOverScrollbar(TCTPlayerScreenHandlerInterface tcp, double mouseX, double mouseY,
			int guiX, int guiY) {
		int[] bounds = getScrollbarBounds(tcp);

		if (bounds[2] <= bounds[0]) {
			return false;
		}

		double localX = mouseX - guiX;
		double localY = mouseY - guiY;
		return localX >= bounds[0] && localX < bounds[2] && localY >= bounds[1] && localY < bounds[3];
	}

	/** 凹槽高度：轨道上下那两条固定边不参与缩放。 */
	private static int getGrooveHeight(int rows) {
		return getScrollbarHeight(rows) - SCROLLER_TRACK_TOP_HEIGHT - SCROLLER_TRACK_BOTTOM_HEIGHT;
	}

	/**
	 * 滑块高度：按「看得见的行 / 总行数」缩放，并夹在 {@value #SCROLLER_THUMB_MIN_HEIGHT} 像素和凹槽高度之间。
	 * Baubles 那边滑块是固定 89 像素的，行数一变位置就没法还原；这里按比例算。
	 */
	public static int getThumbHeight(int rows, int length) {
		int groove = getGrooveHeight(rows);
		int total = Math.max(1, getTotalRows(length));
		int visible = Math.min(getPanelRows(length), total);
		int height = Math.round(groove * (visible / (float) total));

		return MathHelper.clamp(height, Math.min(SCROLLER_THUMB_MIN_HEIGHT, groove), groove);
	}

	/** 滑块相对滚动条顶部的偏移（已经含凹槽上边那 16 像素）。 */
	public static int getThumbOffset(int rows, int length, TCTPlayerScreenHandlerInterface tcp) {
		int travel = getGrooveHeight(rows) - getThumbHeight(rows, length);
		int maxRow = getMaxScrollRow(length);

		if (travel <= 0 || maxRow <= 0) {
			return 0;
		}

		int row = MathHelper.clamp(tcp.getScrollIndex(), 0, maxRow);
		return Math.round(travel * (row / (float) maxRow));
	}

	/**
	 * 画左侧滚动条：九宫格轨道 + 按比例缩放的滑块。
	 *
	 * <p>滑块贴图原本是 6x89，比这里需要的短/长都可能，所以用带目标尺寸的
	 * {@code drawTexture} 重载把它压/拉到 {@link #getThumbHeight} 算出来的高度；
	 * 纵向的斜纹是重复图案，拉伸看不出来。
	 */
	private static void drawScrollbar(DrawContext context, int x, int y, int columns, int rows, int length,
			TCTPlayerScreenHandlerInterface tcp) {
		int left = x + getScrollbarLeft(columns);
		int top = y;
		int height = getScrollbarHeight(rows);
		int middleHeight = height - SCROLLER_TRACK_TOP_HEIGHT - SCROLLER_TRACK_BOTTOM_HEIGHT;

		context.drawTexture(BAUBLES_SCROLLER, left, top, 0, SCROLLER_TRACK_TOP_V,
				SCROLLER_WIDTH, SCROLLER_TRACK_TOP_HEIGHT, SCROLLER_TEXTURE_WIDTH, SCROLLER_TEXTURE_HEIGHT);
		context.drawTexture(BAUBLES_SCROLLER, left, top + SCROLLER_TRACK_TOP_HEIGHT, SCROLLER_WIDTH, middleHeight,
				0, SCROLLER_TRACK_MIDDLE_V, SCROLLER_WIDTH, SCROLLER_TRACK_MIDDLE_HEIGHT,
				SCROLLER_TEXTURE_WIDTH, SCROLLER_TEXTURE_HEIGHT);
		context.drawTexture(BAUBLES_SCROLLER, left, top + height - SCROLLER_TRACK_BOTTOM_HEIGHT,
				0, SCROLLER_TRACK_BOTTOM_V, SCROLLER_WIDTH, SCROLLER_TRACK_BOTTOM_HEIGHT,
				SCROLLER_TEXTURE_WIDTH, SCROLLER_TEXTURE_HEIGHT);

		int thumb = getThumbHeight(rows, length);
		int thumbTop = top + SCROLLER_TRACK_TOP_HEIGHT + getThumbOffset(rows, length, tcp);
		context.drawTexture(BAUBLES_SCROLLER, left + SCROLLER_THUMB_INSET_X, thumbTop, SCROLLER_THUMB_WIDTH, thumb,
				SCROLLER_THUMB_U, SCROLLER_THUMB_V, SCROLLER_THUMB_WIDTH, SCROLLER_THUMB_TEXTURE_HEIGHT,
				SCROLLER_TEXTURE_WIDTH, SCROLLER_TEXTURE_HEIGHT);
	}

	/**
	 * 每页的槽位数量，Curios 风格：一页是 {@code min_width} 列、
	 * 每列 {@code max_height} 行。
	 */
	public static int getSlotsPerPage() {
		return ModConfig.min_width * ModConfig.max_height;
	}

	/**
	 * 总页数。分页模式下超出这一页的槽位靠箭头/滚轮翻；开了滚动条就不用它了。
	 */
	public static int getPageCount(int length) {
		if (length < 1) {
			return 1;
		}

		return MathHelper.ceil((float) length / getSlotsPerPage());
	}

	/**
	 * 第 {@code i} 个饰品槽（在饰品槽区间内从 0 开始）是否落在以 {@code firstRow} 开头的那一屏里。
	 * 分页模式的「一屏」是一页、滚动条模式是一窗，因为都换算成行，这个方法两边共用，
	 * 布局和悬停检测因此不会对不上。
	 */
	public static boolean isSlotVisible(int i, int firstRow, int columns) {
		int row = i / Math.max(1, columns);
		return row >= firstRow && row < firstRow + ModConfig.max_height;
	}

	/**
	 * 面板隐藏时饰品槽被挪到的屏幕外坐标。
	 */
	private static final int HIDDEN_SLOT_COORD = -10000;

	/**
	 * 移动并启用/禁用饰品槽，让只有当前这一屏会被画出来。
	 * 每帧都要调用，因为饰品数据一同步 Trinkets 就会重新添加它的槽位。
	 */
	public static void updateSlots(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp) {
		updateSlots(slots, tcp, true);
	}

	/**
	 * @param panelVisible 面板（在翻页/显示意义上）当前是否可见。面板或整个饰品界面隐藏时传 {@code false}，饰品槽就会被移出屏幕。
	 */
	public static void updateSlots(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp, boolean panelVisible) {
		if (!(tcp instanceof TrinketPlayerScreenHandler handler)) {
			return;
		}

		int start = handler.trinkets$getTrinketSlotStart();
		int count = handler.trinkets$getTrinketSlotEnd() - start;
		int columns = Math.max(1, getPanelColumns(count));
		boolean paged = isPaginationVisible(count);

		// 配置改了行数上限、或者刚切换过模式时，存下来的页码/滚动量可能已经越界。
		clampScrollIndex(tcp, count);
		int firstRow = getFirstVisibleRow(tcp);

		for (int i = 0; i < count; i++) {
			int slotIndex = start + i;
			if (slotIndex >= slots.size()) {
				break;
			}

			Slot slot = slots.get(slotIndex);
			if (!(slot instanceof TCTSurvivalTrinketSlot themed)) {
				continue;
			}

			boolean visible = panelVisible && tcp.getTrinketsShow()
					&& isSlotVisible(i, firstRow, columns);
			themed.setEnabled(visible);

			if (!visible) {
				slot.x = HIDDEN_SLOT_COORD;
				slot.y = HIDDEN_SLOT_COORD;
				continue;
			}

			// 行序就是槽位序号除以列数的商，所以列直接取余数、行减去窗口第一行即可；
			// 这样即使滚动量暂时越界也不会算出负的行号。
			int column = i % columns;
			int row = i / columns - firstRow;

			slot.x = getSlotX(column, columns);
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
		int columns = Math.max(1, getPanelColumns(count));
		int firstRow = getFirstVisibleRow(tcp);

		for (int i = 0; i < count; i++) {
			if (!isSlotVisible(i, firstRow, columns)) {
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
	 * 像 Curios 那样翻到另一页，然后重新排布可见的槽位。分页模式的滚轮/箭头走这里。
	 */
	public static void changePage(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp, int delta) {
		int pages = getPageCount(getTrinketSlotCount(tcp));
		int page = MathHelper.clamp(tcp.getScrollIndex() + delta, 0, Math.max(0, pages - 1));
		tcp.setScrollIndex(page);

		updateSlots(slots, tcp);
	}

	/**
	 * 滚动条模式下按行滚动，{@code rows} 为正表示露出更靠后的行。
	 * 分页模式请用 {@link #changePage}，那时 {@code scrollIndex} 存的是页码而不是行偏移。
	 */
	public static void scrollRows(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp, int rows) {
		int maxRow = getMaxScrollRow(getTrinketSlotCount(tcp));
		tcp.setScrollIndex(MathHelper.clamp(tcp.getScrollIndex() + rows, 0, maxRow));

		updateSlots(slots, tcp);
	}

	/**
	 * 拖动滑块：把鼠标的绝对 Y 换算成行偏移。
	 * 换算时让滑块中心跟着光标走，所以按住滑块本体拖动不会先跳一下，在轨道别处按下则会跳到指的位置
	 * （1.19 原版就是这么算的）。
	 */
	public static void dragScrollbarTo(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp, double mouseY,
			int guiY) {
		int length = getTrinketSlotCount(tcp);
		int maxRow = getMaxScrollRow(length);

		if (maxRow <= 0) {
			return;
		}

		int rows = getPanelRows(length);
		int thumb = getThumbHeight(rows, length);
		int travel = getGrooveHeight(rows) - thumb;

		if (travel <= 0) {
			tcp.setScrollIndex(0);
			updateSlots(slots, tcp);
			return;
		}

		double grooveTop = guiY + SCROLLER_TRACK_TOP_HEIGHT;
		double progress = (mouseY - grooveTop - thumb / 2.0D) / travel;
		tcp.setScrollIndex(MathHelper.clamp((int) Math.round(progress * maxRow), 0, maxRow));

		updateSlots(slots, tcp);
	}

	/**
	 * 面板是否显示页码条/翻页箭头：用的是分页模式，而且确实不止一页。
	 * 开了滚动条之后这一条恒为 false，两种排版因此不会同时出现。
	 */
	public static boolean isPaginationVisible(int length) {
		return !isScrollbarMode() && getPageCount(length) > 1;
	}

	public static final int PAGE_BUTTON_WIDTH = 11;
	public static final int PAGE_BUTTON_HEIGHT = 12;

	/**
	 * 翻页箭头的几何位置，使用相对 GUI 坐标：
	 * {@code [上一页 x, 下一页 x, y]}。
	 *
	 * <p>纵向留 2 像素：原来是贴着面板顶边画的 0，箭头最上面一两行会顶在面板顶框上，看着像被压住。
	 * Curios 自己的 PageButton 也是放在 GUI 顶部往下 2 像素（{@code getGuiTop() + 2}），正好对上。
	 * 绘制和点击判定共用本方法，所以改这里两边一起动，不会错位。
	 */
	public static int[] getPageButtons() {
		return new int[] { -28, -17, 2 };
	}

	public static boolean isClickInPageButtons(TCTPlayerScreenHandlerInterface tcp, double mouseX, double mouseY, int x, int y) {
		if (!isPaginationVisible(getTrinketSlotCount(tcp))) {
			return false;
		}

		int[] buttons = getPageButtons();
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
	public static boolean isClickInNextPage(double mouseX, int x) {
		int[] buttons = getPageButtons();
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
		int bottom = getPanelHeight(rows, isPaginationVisible(length));
		return new int[] { left, 0, -1, bottom };
	}

	/**
	 * 光标是否在面板（或它左侧的滚动条）上方，也就是滚轮生效的区域。
	 * 滚轮只在面板自己这一块管用：面板之外一律交回原版（从前有个
	 * {@code scrolling_outside_boundary} 选项可以让它在整个背包里生效，那个功能已经删除）。
	 */
	public static boolean isScrolledInTrinkets(TCTPlayerScreenHandlerInterface tcp, double mouseX, double mouseY, int x, int y) {
		// 滚动条就贴在面板左边，滚轮落在它上面也应该管用，否则会看着像坏了。
		if (isMouseOverScrollbar(tcp, mouseX, mouseY, x, y)) {
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
