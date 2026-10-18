package closedw.tt.client.mixin;

import closedw.tt.client.theme.TrinketRenderToggle;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第一人称手臂渲染的「开窗」。
 *
 * <p>第一人称看不到自己的模型，能看到的只有手臂，而手臂是
 * {@code PlayerEntityRenderer#renderRightArm / renderLeftArm} 画出来的。饰品模组会把「手」组的饰品
 * 叠在这条手臂上：Artifacts 9.5.x 就挂在 {@code artifacts.fabric.mixin.client.PlayerRendererMixin}
 * 里，用 {@code TrinketsApi.getTrinketComponent} 取组件、读 {@code TrinketComponent#getAllEquipped()}
 * 再画手套，整条路径不碰 {@link TrinketFeatureRendererMixin} 拦的那个 {@code TrinketFeatureRenderer}，
 * 所以关掉渲染之后第一人称手上还看得见。
 *
 * <p>这里不直接拦饰品模组（那是别家的内部代码，名字一变就废），而是在手臂渲染期间开一个窗口：
 * {@link LivingEntityTrinketComponentMixin} 会让窗口里的饰品遍历把关掉的槽位当成空槽，
 * 于是任何「在手臂渲染时查饰品再画上去」的模组都会自然画不出被关掉的槽位，且其它渲染路径
 * 一点不受影响。开关状态的判断在 {@link TrinketRenderToggle#shouldFilterFirstPerson}。
 *
 * <p>两个方法的签名完全一样（{@code MatrixStack, VertexConsumerProvider, int, AbstractClientPlayerEntity}），
 * 所以一次 {@code @Inject} 写两个方法名就够了。手臂渲染是同步的，HEAD 开窗、RETURN 关窗；
 * 万一渲染中途抛异常，游戏本身也就崩了，没有窗口残留的问题。
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

	@Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("HEAD"))
	private void trinketsTooltip$beginFirstPersonArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
			int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
		TrinketRenderToggle.beginFirstPersonArm();
	}

	@Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("RETURN"))
	private void trinketsTooltip$endFirstPersonArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
			int light, AbstractClientPlayerEntity player, CallbackInfo ci) {
		TrinketRenderToggle.endFirstPersonArm();
	}
}
