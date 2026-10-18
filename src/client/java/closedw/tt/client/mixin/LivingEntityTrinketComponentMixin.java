package closedw.tt.client.mixin;

import closedw.tt.client.theme.TrinketRenderToggle;
import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.SlotReference;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.BiConsumer;

/**
 * 「启用饰品渲染」开关在第一人称里的那一半：窗口里的饰品遍历跳过被关掉的槽位。
 *
 * <p>改的是 {@code TrinketComponent#forEach} 的回调参数本身，不重写 Trinkets 的循环，
 * 所以 {@code forEach}、{@code getEquipped}、{@code getAllEquipped} 会一起被滤掉
 * （{@code getEquipped} 就是拿 {@code forEach} 实现的，{@code getAllEquipped} 又建在它上面），
 * 读这些接口画饰品的模组——比如 Artifacts 的手套——就看不见被关掉的槽位了。
 *
 * <p>窗口由 {@link PlayerEntityRendererMixin} 在本地玩家的第一人称手臂渲染期间打开，
 * 判断条件见 {@link TrinketRenderToggle#shouldFilterFirstPerson}：窗口没开、不是本地玩家、
 * 或者没关掉过任何槽位时，这里原样返回 Trinkets 自己的回调，行为与没装本模组时完全一致。
 *
 * <p>目标用的是实体身上那个 Cardinal Components 组件
 * （Trinkets 里注册的就是这个类：{@code LivingEntityTrinketComponent::new}），
 * 名字和描述符写全是为了避免歧义；{@code remap = false} 与本模组其它 Trinkets mixin 一致。
 */
@Mixin(value = LivingEntityTrinketComponent.class, remap = false)
public abstract class LivingEntityTrinketComponentMixin {

	/**
	 * {@code forEach} 只有一个参数，把它换成「先看一眼槽位开关」的包装版本即可。
	 * {@code argsOnly = true} 表示改的是方法自己的入参，而不是方法体里的局部变量。
	 */
	@ModifyVariable(method = "forEach(Ljava/util/function/BiConsumer;)V", at = @At("HEAD"), argsOnly = true)
	private BiConsumer<SlotReference, ItemStack> trinketsTooltip$hideDisabledSlots(BiConsumer<SlotReference, ItemStack> consumer) {
		LivingEntityTrinketComponent component = (LivingEntityTrinketComponent) (Object) this;

		if (!TrinketRenderToggle.shouldFilterFirstPerson(component.getEntity())) {
			return consumer;
		}

		return TrinketRenderToggle.hideDisabledSlots(consumer);
	}
}
