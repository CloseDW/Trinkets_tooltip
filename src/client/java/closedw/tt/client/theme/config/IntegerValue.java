package closedw.tt.client.theme.config;

import com.mrcrayfish.configured.api.IConfigValue;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * 单个有范围限制的整数选项的临时载体，玩家在 Configured 里保存之前由它保存编辑中的值。
 */
public class IntegerValue implements IConfigValue<Integer> {

	private final String key;
	private final int defaultValue;
	private final int initialValue;
	private final int min;
	private final int max;
	private final boolean hasComment;
	private int value;

	public IntegerValue(String key, int defaultValue, int initialValue, int min, int max, boolean hasComment) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.initialValue = initialValue;
		this.min = min;
		this.max = max;
		this.hasComment = hasComment;
		this.value = initialValue;
	}

	@Override
	public Integer get() {
		return this.value;
	}

	@Override
	public void set(Integer value) {
		this.value = value;
	}

	@Override
	public Integer getDefault() {
		return this.defaultValue;
	}

	@Override
	public boolean isValid(Integer value) {
		return value != null && value >= this.min && value <= this.max;
	}

	@Override
	public boolean isDefault() {
		return this.value == this.defaultValue;
	}

	@Override
	public boolean isChanged() {
		return this.value != this.initialValue;
	}

	@Override
	public void restore() {
		this.value = this.defaultValue;
	}

	@Nullable
	@Override
	public Text getComment() {
		return this.hasComment ? Text.translatable(this.getTranslationKey() + ".tooltip") : null;
	}

	@Nullable
	@Override
	public String getTranslationKey() {
		return "config." + TrinketsTooltipConfig.MOD_ID + "." + this.key;
	}

	@Nullable
	@Override
	public Text getValidationHint() {
		return Text.translatable("config." + TrinketsTooltipConfig.MOD_ID + ".range",
				this.min, this.max);
	}

	@Override
	public String getName() {
		return this.key;
	}

	@Override
	public void cleanCache() {
	}

	@Override
	public boolean requiresWorldRestart() {
		return false;
	}

	@Override
	public boolean requiresGameRestart() {
		return false;
	}
}
