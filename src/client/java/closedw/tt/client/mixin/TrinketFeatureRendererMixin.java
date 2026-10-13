package closedw.tt.client.mixin;

import closedw.tt.client.theme.TrinketRenderToggle;
import dev.emi.trinkets.TrinketFeatureRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「启用饰品渲染」开关的渲染侧。
 *
 * <p>Trinkets 会给每个实体渲染器挂一个 {@link TrinketFeatureRenderer}，它把身上所有注册过
 * {@code TrinketRenderer} 的饰品都画出来（3.7.2 没有任何渲染状态）。这里在本地玩家自己的模型上
 * 接管这次调用，跳过被关掉的槽位；其他实体、以及没有关掉任何槽位的情况都不插手，
 * 完全保留 Trinkets 原本的行为。
 *
 * <p>{@code remap = false} 和本模组其它 Trinkets mixin 一致：Trinkets 是模组类，它的方法名
 * 不在映射表里，而目标方法的描述符由处理器方法自己的签名推断（编译后会被 Loom 一起重映射）。
 */
@Mixin(value = TrinketFeatureRenderer.class, remap = false, priority = 1500)
public abstract class TrinketFeatureRendererMixin extends FeatureRenderer<LivingEntity, EntityModel<LivingEntity>> {

	/**
	 * 占位构造函数：Mixin 会丢弃私有的 mixin 构造函数，它存在只是为了让 javac 能满足
	 * {@link FeatureRenderer} 的构造函数要求。
	 */
	private TrinketFeatureRendererMixin(FeatureRendererContext<LivingEntity, EntityModel<LivingEntity>> context) {
		super(context);
	}

	/**
	 * 目标方法必须连描述符一起写。{@link TrinketFeatureRenderer} 里有两个同名 {@code render}：
	 * 真正干活的那个（参数是 {@code LivingEntity}）和编译器为泛型覆写自动生成的桥接方法
	 * （参数是 {@code Entity}）。只写方法名的话，Mixin 注解处理器会报
	 * “Ambiguous reference to method 'render'”，运行时也会退化成「取类文件里第一个同名匹配」，
	 * 依赖 Trinkets 的编译顺序；写上描述符后匹配就是唯一的。
	 *
	 * <p>描述符里的 {@code net.minecraft} 类名不需要手动转换：构建时 Loom 会连同注解字符串
	 * 一起重映射（{@code remap = false} 也照样重映射，已用 tiny-remapper 的 mixin 扩展实测确认），
	 * 所以这里按源码里的 yarn 名字书写即可。
	 */
	@Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
			at = @At("HEAD"), cancellable = true)
	private void trinketsTooltip$render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			LivingEntity entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress,
			float headYaw, float headPitch, CallbackInfo ci) {
		if (!TrinketRenderToggle.shouldFilter(entity)) {
			return;
		}

		TrinketRenderToggle.renderFiltered(entity, this.getContextModel(), matrices, vertexConsumers, light,
				limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
		ci.cancel();
	}
}
