package closedw.tt.client.theme.config;

import com.mrcrayfish.configured.api.IConfigValue;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * 单个布尔选项的临时载体，玩家在 Configured 里保存之前由它保存编辑中的值。只有列在
 * {@link TrinketsTooltipConfig#TOOLTIP_KEYS} 里的键才会带上说明文字，
 * 这样缺翻译时也不会在界面上露出原始的键名。
 */
public class BooleanValue implements IConfigValue<Boolean> {

	private final String key;
	private final boolean defaultValue;
	private final boolean initialValue;
	private final boolean hasComment;
	private boolean value;

	public BooleanValue(String key, boolean defaultValue, boolean initialValue, boolean hasComment) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.initialValue = initialValue;
		this.hasComment = hasComment;
		this.value = initialValue;
	}

	@Override
	public Boolean get() {
		return this.value;
	}

	@Override
	public void set(Boolean value) {
		this.value = value;
	}

	@Override
	public Boolean getDefault() {
		return this.defaultValue;
	}

	@Override
	public boolean isValid(Boolean value) {
		return value != null;
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
		return null;
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
