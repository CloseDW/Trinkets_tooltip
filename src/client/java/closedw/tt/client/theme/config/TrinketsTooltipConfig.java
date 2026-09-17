package closedw.tt.client.theme.config;

import closedw.tt.TrinketsTooltip;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 模组的配置，存成 {@code .properties} 文件，并通过 Configured 编辑。{@link ModConfig} 仍然是渲染代码读取的
 * 普通静态镜像，每次本配置被加载或保存时都会同步刷新。
 */
public class TrinketsTooltipConfig implements IModConfig {

	public static final String MOD_ID = TrinketsTooltip.MOD_ID;
	public static final String FILE_NAME = "trinkets-tooltip.properties";

	/** 界面分组：分组名 -&gt; 选项键列表，按显示顺序排列。 */
	public static final Map<String, List<String>> CATEGORIES = new LinkedHashMap<>();

	public static final Map<String, Boolean> DEFAULT_VALUES = new LinkedHashMap<>();
	public static final Map<String, Integer> INT_DEFAULTS = new LinkedHashMap<>();
	public static final Map<String, int[]> INT_RANGES = new LinkedHashMap<>();

	/** 这些键对应的 {@code config.<modid>.<key>.tooltip} 翻译是存在的。 */
	public static final Set<String> TOOLTIP_KEYS = Set.of(
			"pagination", "min_width", "max_height", "scrolling_outside_boundary", "jei_slot_lookup");

	static {
		CATEGORIES.put("panel", List.of("pagination", "min_width", "max_height"));
		CATEGORIES.put("general", List.of("scrolling_outside_boundary", "jei_slot_lookup"));

		DEFAULT_VALUES.put("pagination", true);
		DEFAULT_VALUES.put("scrolling_outside_boundary", false);
		DEFAULT_VALUES.put("jei_slot_lookup", true);

		INT_DEFAULTS.put("max_height", 7);
		INT_RANGES.put("max_height", new int[] { 2, 7 });
		INT_DEFAULTS.put("min_width", 1);
		INT_RANGES.put("min_width", new int[] { 1, 4 });
	}

	private final Map<String, BooleanValue> values = new LinkedHashMap<>();
	private final Map<String, IntegerValue> intValues = new LinkedHashMap<>();
	private IConfigEntry root;

	private TrinketsTooltipConfig() {
		this.load();
	}

	private static final TrinketsTooltipConfig INSTANCE = new TrinketsTooltipConfig();

	public static TrinketsTooltipConfig get() {
		return INSTANCE;
	}

	private void load() {
		// 文件读写都放在 ModConfig 里，这样只有装了 Configured 时才会加载这个类。
		ModConfig.load();

		DEFAULT_VALUES.forEach((key, defaultValue) -> this.values.put(key,
				new BooleanValue(key, defaultValue, ModConfig.getBoolean(key), TOOLTIP_KEYS.contains(key))));

		INT_DEFAULTS.forEach((key, defaultValue) -> {
			int[] range = INT_RANGES.getOrDefault(key, new int[] { Integer.MIN_VALUE, Integer.MAX_VALUE });
			this.intValues.put(key, new IntegerValue(key, defaultValue, ModConfig.getInt(key),
					range[0], range[1], TOOLTIP_KEYS.contains(key)));
		});
	}

	@Override
	public void update(IConfigEntry entry) {
		Set<IConfigValue<?>> changed = ConfigHelper.getChangedValues(entry);

		if (changed.isEmpty()) {
			return;
		}

		ModConfig.refresh(this.values.get("pagination").get(),
				this.values.get("scrolling_outside_boundary").get(),
				this.intValues.get("max_height").get(),
				this.intValues.get("min_width").get(),
				this.values.get("jei_slot_lookup").get());
		ModConfig.save();
	}

	@Override
	public IConfigEntry getRoot() {
		if (this.root == null) {
			List<IConfigEntry> children = new ArrayList<>();
			CATEGORIES.forEach((category, keys) ->
					children.add(new CategoryEntry(category, leafEntries(keys))));
			this.root = new RootEntry(children);
		}

		return this.root;
	}

	private List<IConfigEntry> leafEntries(List<String> keys) {
		return keys.stream()
				.map(key -> (IConfigEntry) (INT_DEFAULTS.containsKey(key)
						? new IntegerEntry(this.intValues.get(key))
						: new BooleanEntry(this.values.get(key))))
				.toList();
	}

	@Override
	public ConfigType getType() {
		return ConfigType.CLIENT;
	}

	@Override
	public String getFileName() {
		return FILE_NAME;
	}

	@Override
	public String getModId() {
		return MOD_ID;
	}

	@Override
	public void loadWorldConfig(Path path, Consumer<IModConfig> result) {
		// 客户端配置，没有需要从存档里读的东西。
	}

	public static class RootEntry implements IConfigEntry {

		private final List<IConfigEntry> children;

		public RootEntry(List<IConfigEntry> children) {
			this.children = children;
		}

		@Override
		public List<IConfigEntry> getChildren() {
			return this.children;
		}

		@Override
		public boolean isRoot() {
			return true;
		}

		@Override
		public boolean isLeaf() {
			return false;
		}

		@Override
		public IConfigValue<?> getValue() {
			return null;
		}

		@Override
		public String getEntryName() {
			return "Root";
		}

		@Override
		public Text getTooltip() {
			return null;
		}

		@Override
		public String getTranslationKey() {
			return null;
		}
	}

	public static class CategoryEntry implements IConfigEntry {

		private final String name;
		private final List<IConfigEntry> children;

		public CategoryEntry(String name, List<IConfigEntry> children) {
			this.name = name;
			this.children = children;
		}

		@Override
		public List<IConfigEntry> getChildren() {
			return this.children;
		}

		@Override
		public boolean isRoot() {
			return false;
		}

		@Override
		public boolean isLeaf() {
			return false;
		}

		@Override
		public IConfigValue<?> getValue() {
			return null;
		}

		@Override
		public String getEntryName() {
			return this.name;
		}

		@Override
		public Text getTooltip() {
			return null;
		}

		@Override
		public String getTranslationKey() {
			return "config." + MOD_ID + ".category." + this.name;
		}
	}

	public static class BooleanEntry implements IConfigEntry {

		private final BooleanValue value;

		public BooleanEntry(BooleanValue value) {
			this.value = value;
		}

		@Override
		public List<IConfigEntry> getChildren() {
			return List.of();
		}

		@Override
		public boolean isRoot() {
			return false;
		}

		@Override
		public boolean isLeaf() {
			return true;
		}

		@Override
		public IConfigValue<?> getValue() {
			return this.value;
		}

		@Override
		public String getEntryName() {
			return this.value.getName();
		}

		@Override
		public Text getTooltip() {
			return this.value.getComment();
		}

		@Override
		public String getTranslationKey() {
			return this.value.getTranslationKey();
		}
	}

	public static class IntegerEntry implements IConfigEntry {

		private final IntegerValue value;

		public IntegerEntry(IntegerValue value) {
			this.value = value;
		}

		@Override
		public List<IConfigEntry> getChildren() {
			return List.of();
		}

		@Override
		public boolean isRoot() {
			return false;
		}

		@Override
		public boolean isLeaf() {
			return true;
		}

		@Override
		public IConfigValue<?> getValue() {
			return this.value;
		}

		@Override
		public String getEntryName() {
			return this.value.getName();
		}

		@Override
		public Text getTooltip() {
			return this.value.getComment();
		}

		@Override
		public String getTranslationKey() {
			return this.value.getTranslationKey();
		}
	}
}
