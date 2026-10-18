package closedw.tt.client.theme;

import closedw.tt.TrinketsTooltip;
import closedw.tt.client.theme.config.ModConfig;
import closedw.tt.client.theme.interfaces.TCTPlayerScreenHandlerInterface;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Curios 的「启用饰品渲染」开关，客户端版本。
 *
 * <p>Curios 每个饰品槽右上角都有一枚 8x8 的开关（{@code top.theillusivec4.curios.client.gui.RenderButton}），
 * 按下后把 {@code renderStates[slot]} 发给服务端、由服务端同步给所有人，因此关掉之后别人也看不见你的饰品。
 * 本模组是纯客户端模组，改不了服务端状态，所以这里只做「本机可见性」：开关状态存在
 * {@code config/trinkets-tooltip.properties} 里，只拦下渲染本地玩家自己模型的那些饰品
 * （第三人称视角里的自己、以及背包界面上那个玩家预览），别的玩家和生物的饰品照常渲染。
 *
 * <p>Trinkets 3.7.2 没有任何渲染状态：{@code TrinketFeatureRenderer} 会把每个装了
 * {@code TrinketRenderer} 的物品都画出来。开关的「关」分支因此只能由本模组自己在渲染路径上实现，
 * 见 {@link closedw.tt.client.mixin.TrinketFeatureRendererMixin}。
 * <p>槽位标识用 Trinkets 自己的「组名/槽位名」加槽位序号拼出来（例如 {@code chest/necklace#0}），
 * 这是 {@link SlotType#getGroup()} / {@link SlotType#getName()} 加上
 * {@link SlotReference#index()}，面板侧和渲染侧都能拼出同一个键。
 */
public final class TrinketRenderToggle {

	/**
	 * 开关图标贴图：16x8 两个 8x8 帧，左边是「已关闭」、右边是「渲染中」。
	 * 图像取自 Curios 的 {@code curios/textures/gui/curios/inventory.png} 里同一对图标
	 * （u=83 和 u=75、v=0），两个模组同为 LGPL-3.0。
	 */
	public static final Identifier TEXTURE = new Identifier(TrinketsTooltip.MOD_ID, "textures/gui/render_toggle.png");

	/** 图标边长。 */
	public static final int SIZE = 8;

	private static final int TEXTURE_WIDTH = 16;
	private static final int TEXTURE_HEIGHT = 8;
	private static final int ENABLED_U = 8;
	private static final int DISABLED_U = 0;

	/**
	 * 图标相对饰品槽左上角的偏移，也就是槽位右上角。
	 *
	 * <p>Curios 的 RenderButton 用的是 {@code slot.x + 12, slot.y - 1}。槽位只有 18 像素宽，
	 * 按那个位置图标会向右挤出 2 像素、落到右边相邻槽位的地盘上，多列布局时右侧会被旁边的槽位盖住。
	 * 这里改成 {@code + 10}：图标正好收在本槽位自己的 18 像素里（10..17），
	 * 纵向仍与 Curios 一致。
	 */
	private static final int ICON_OFFSET_X = 10;
	private static final int ICON_OFFSET_Y = -1;

	/** 鼠标停在开关上时显示的提示文字。 */
	public static final Text TOOLTIP = Text.translatable("gui." + TrinketsTooltip.MOD_ID + ".toggle_render");

	private TrinketRenderToggle() {
	}

	/* ------------------------------------------------------------------ 槽位标识 */

	/**
	 * 槽位标识。和 Trinkets 自己用的 {@code group + "/" + slot} 记法保持一致，
	 * 再加上 {@link SlotReference#index()}，所以同一个槽位类型里的多个槽位不会混在一起。
	 */
	public static String key(SlotType type, int index) {
		return type.getGroup() + "/" + type.getName() + "#" + index;
	}

	public static String key(SlotReference reference) {
		return key(reference.inventory().getSlotType(), reference.index());
	}

	/**
	 * 面板上的饰品槽对应的标识。{@link Slot#getIndex()} 就是 Trinkets 建槽位时传的
	 * {@code slotOffset}（{@code PlayerScreenHandlerMixin} 里两个参数都是同一个 i，
	 * Trinkets 自己也用 {@code new SlotReference((TrinketInventory) slot.inventory, slot.getIndex())}），
	 * 所以这里拼出来的键和渲染侧的 {@link SlotReference} 一定能对上。
	 *
	 * @return 不是饰品槽时返回 {@code null}。
	 */
	@Nullable
	public static String key(Slot slot) {
		if (!(slot.inventory instanceof TrinketInventory inventory)) {
			return null;
		}

		return key(inventory.getSlotType(), slot.getIndex());
	}

	/* ------------------------------------------------------------------ 开关状态 */

	/** 该槽位当前是否渲染，也就是开关没被关掉。 */
	public static boolean isRendered(SlotReference reference) {
		return !ModConfig.render_disabled_slots.contains(key(reference));
	}

	/** 面板上的饰品槽是否渲染；不是饰品槽时按「照常渲染」处理。 */
	public static boolean isRendered(Slot slot) {
		String key = key(slot);
		return key == null || !ModConfig.render_disabled_slots.contains(key);
	}

	/** 翻转一个槽位的开关并落盘。 */
	public static void toggle(Slot slot) {
		String key = key(slot);

		if (key == null) {
			return;
		}

		ModConfig.setRenderDisabled(key, !ModConfig.render_disabled_slots.contains(key));
	}

	/* ------------------------------------------------------------------ 渲染拦截 */

	/**
	 * 这次渲染要不要由本模组接管。
	 *
	 * <p>只有「本地玩家自己的模型 + 至少关掉过一个槽位」时才接管：其余情况一律不碰，
	 * 完全走 Trinkets 原本的渲染代码。同一个实体实例既用于世界里的第三人称渲染，
	 * 也用于背包界面上的玩家预览（{@code InventoryScreen.drawBackground} 传的就是
	 * {@code client.player}），所以这一个判断覆盖了两处。
	 */
	public static boolean shouldFilter(LivingEntity entity) {
		return entity == MinecraftClient.getInstance().player && !ModConfig.render_disabled_slots.isEmpty();
	}

	/**
	 * Trinkets 那套渲染循环的等价实现，只是跳过被关掉的槽位
	 * （原逻辑见 TrinketFeatureRenderer#render）。
	 */
	public static void renderFiltered(LivingEntity entity, EntityModel<? extends LivingEntity> contextModel,
			MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float limbAngle,
			float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		TrinketsApi.getTrinketComponent(entity).ifPresent(component ->
				component.forEach((reference, stack) -> {
					if (stack.isEmpty() || !isRendered(reference)) {
						return;
					}

					TrinketRendererRegistry.getRenderer(stack.getItem()).ifPresent(renderer -> {
						matrices.push();
						renderer.render(stack, reference, contextModel, matrices, vertexConsumers, light, entity,
								limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
						matrices.pop();
					});
				})
		);
	}

	/* 第一人称手臂 */

	/**
	 * 本地玩家第一人称手臂的渲染深度，以及打开这个窗口的线程。
	 *
	 * <p>{@code PlayerEntityRenderer#renderRightArm / renderLeftArm} 就是画第一人称手臂的地方，
	 * 饰品模组（Artifacts 的手套）在这两个方法的末尾叠自己的模型。这里在那两个方法外面开一个窗口，
	 * 窗口里 {@link dev.emi.trinkets.api.TrinketComponent} 的遍历把关掉的槽位当成空槽，
	 * 于是叠在手臂上的饰品自然就画不出来，别的渲染路径完全不受影响。
	 *
	 * <p>为什么要记线程：单人游戏里集成服务端和客户端渲染是两个线程，窗口开着的时候服务端线程
	 * （以及区块构建线程）照样会查饰品组件，那些查询必须看到真实数据，否则关掉渲染会顺带关掉
	 * 饰品本身的能力，所以窗口只对开它的那个线程生效。
	 */
	private static int firstPersonArmDepth;
	private static Thread firstPersonArmThread;

	/** 开始渲染第一人称手臂。记的是最外层那个窗口的线程，嵌套时不会被里层覆盖。 */
	public static void beginFirstPersonArm() {
		if (firstPersonArmDepth == 0) {
			firstPersonArmThread = Thread.currentThread();
		}

		firstPersonArmDepth++;
	}

	/** 结束第一人称手臂渲染。 */
	public static void endFirstPersonArm() {
		if (firstPersonArmDepth > 0) {
			firstPersonArmDepth--;
		}

		if (firstPersonArmDepth == 0) {
			firstPersonArmThread = null;
		}
	}

	/**
	 * 这次饰品遍历要不要滤掉关掉的槽位。
	 *
	 * <p>只有「当前线程正处在本地玩家的第一人称手臂渲染窗口里，而且确实关掉过槽位」才接管，
	 * 其余情况一律不碰，遍历完全按 Trinkets 原本的代码走。
	 */
	public static boolean shouldFilterFirstPerson(LivingEntity entity) {
		return firstPersonArmDepth > 0 && Thread.currentThread() == firstPersonArmThread
				&& entity == MinecraftClient.getInstance().player && !ModConfig.render_disabled_slots.isEmpty();
	}

	/**
	 * 把遍历回调包一层，被关掉的槽位在读的时候直接当作不存在。
	 *
	 * <p>包住的是 Trinkets 自己的 {@code forEach} 参数，所以 {@code forEach}、{@code getEquipped}、
	 * {@code getAllEquipped} 会一起被滤掉——饰品模组读的正是最后那个。
	 */
	public static BiConsumer<SlotReference, ItemStack> hideDisabledSlots(BiConsumer<SlotReference, ItemStack> consumer) {
		return (reference, stack) -> {
			if (isRendered(reference)) {
				consumer.accept(reference, stack);
			}
		};
	}

	/* ------------------------------------------------------------------ 面板上的图标 */

	/** 图标在 GUI 里的相对坐标。 */
	public static int getIconX(Slot slot) {
		return slot.x + ICON_OFFSET_X;
	}

	public static int getIconY(Slot slot) {
		return slot.y + ICON_OFFSET_Y;
	}

	/**
	 * 鼠标是否落在该槽位的开关图标上。
	 *
	 * @param mouseX 绝对屏幕坐标；{@code guiX}/{@code guiY} 是界面的左上角。
	 */
	public static boolean isMouseOverIcon(Slot slot, double mouseX, double mouseY, int guiX, int guiY) {
		double left = guiX + getIconX(slot);
		double top = guiY + getIconY(slot);
		return mouseX >= left && mouseX < left + SIZE && mouseY >= top && mouseY < top + SIZE;
	}

	/**
	 * 当前可见的饰品槽里，开关图标被鼠标命中的那一个。
	 * 隐藏（翻页翻走、面板收起）的槽位已经被挪到屏幕外，命中判定自然不成立。
	 */
	@Nullable
	public static Slot getHoveredIcon(DefaultedList<Slot> slots, TCTPlayerScreenHandlerInterface tcp,
			double mouseX, double mouseY, int guiX, int guiY) {
		if (!(tcp instanceof TrinketPlayerScreenHandler handler)) {
			return null;
		}

		int start = handler.trinkets$getTrinketSlotStart();
		int end = handler.trinkets$getTrinketSlotEnd();

		for (int i = start; i < end && i < slots.size(); i++) {
			Slot slot = slots.get(i);

			if (slot.isEnabled() && isMouseOverIcon(slot, mouseX, mouseY, guiX, guiY)) {
				return slot;
			}
		}

		return null;
	}

	/**
	 * 画一个槽位的开关图标。这里用的是槽位的 GUI 相对坐标，不能再加上界面的 x/y：
	 * {@code HandledScreen.render} 在画槽位之前已经把矩阵平移到了界面左上角
	 * （1.21.1 的 HandledScreen 第 97-99 行），本方法就是从 drawSlot 的 TAIL 调进来的。
	 */
	public static void drawIcon(DrawContext context, Slot slot) {
		int u = isRendered(slot) ? ENABLED_U : DISABLED_U;
		context.drawTexture(TEXTURE, getIconX(slot), getIconY(slot), u, 0, SIZE, SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
	}
}
