package navrelis.glintsmith.gui.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.function.Consumer;

import navrelis.glintsmith.config.GlintsmithConfig;
import navrelis.glintsmith.gui.Ui;
import navrelis.glintsmith.gui.widget.ColorSwatch;
import navrelis.glintsmith.gui.widget.HexBox;
import navrelis.glintsmith.gui.widget.OptionList;
import navrelis.glintsmith.style.EnchantmentColors;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Colour overrides for the "enchantment colours" mode, one row per enchantment with a search box on top.
 * With a world loaded the enchantments come from the registry (so modded ones appear), otherwise from the
 * built-in colour table.
 */
public final class EnchantmentColorsTab extends ConfigTab {
	private static final int SEARCH_HEIGHT = 28;
	private static final int SWATCH = Ui.HEIGHT;
	private static final int HEX_WIDTH = 64;
	private static final int RESET_WIDTH = 42;
	private static final int CONTROLS_WIDTH = SWATCH + HEX_WIDTH + RESET_WIDTH + 3 * Ui.GAP;

	private final EditBox search;
	private String filter = "";
	private List<String> ids = List.of();

	public EnchantmentColorsTab(GlintsmithConfig config) {
		super(config, Ui.t("glintsmith.tab.enchantments"));
		Font font = Minecraft.getInstance().font;
		this.search = new EditBox(font, 0, 0, Ui.WIDTH, Ui.HEIGHT, Ui.t("glintsmith.enchantments.search"));
		search.setHint(Ui.t("glintsmith.enchantments.search.hint"));
		search.setMaxLength(64);
		search.setTooltip(Ui.tip("glintsmith.enchantments.search"));
		search.setResponder(text -> {
			filter = text.trim().toLowerCase(Locale.ROOT);
			rebuild();
		});
	}

	@Override
	public void visitChildren(Consumer<AbstractWidget> visitor) {
		visitor.accept(search);
		super.visitChildren(visitor);
	}

	@Override
	public void doLayout(ScreenRectangle area) {
		search.setPosition(area.left() + (area.width() - Ui.WIDTH) / 2, area.top() + (SEARCH_HEIGHT - Ui.HEIGHT) / 2 + 1);
		list.updateSizeAndPosition(area.width(), area.height() - SEARCH_HEIGHT, area.top() + SEARCH_HEIGHT);
	}

	@Override
	public void rebuild() {
		ids = collectIds();
		super.rebuild();
	}

	@Override
	protected void fill() {
		list.addHelp(Ui.t("glintsmith.enchantments.help"));
		int shown = 0;
		for (String id : ids) {
			if (matches(id)) {
				list.add(new EnchantmentRow(id));
				shown++;
			}
		}
		if (shown == 0) {
			list.addHelp(Ui.t("glintsmith.enchantments.none"));
		}
	}

	private boolean matches(String id) {
		if (filter.isEmpty()) {
			return true;
		}
		return id.contains(filter) || displayName(id).toLowerCase(Locale.ROOT).contains(filter);
	}

	/** All enchantment ids to show, sorted: the registry in a world, else the built-in table; plus any override. */
	private List<String> collectIds() {
		TreeSet<String> all = new TreeSet<>();
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null) {
			for (ResourceLocation key : minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).keySet()) {
				all.add(key.toString());
			}
		} else {
			all.addAll(EnchantmentColors.defaults().keySet());
		}
		all.addAll(config.enchantmentColors.keySet());
		return new ArrayList<>(all);
	}

	/** The translated enchantment name, or a readable form of the id when the language has none. */
	private static String displayName(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		if (location == null) {
			return id;
		}
		String key = Util.makeDescriptionId("enchantment", location);
		if (I18n.exists(key)) {
			return I18n.get(key);
		}
		String path = location.getPath().replace('_', ' ').replace('/', ' ');
		return path.isEmpty() ? id : Character.toUpperCase(path.charAt(0)) + path.substring(1);
	}

	@Override
	public void reset(GlintsmithConfig defaults) {
		config.enchantmentColors = defaults.enchantmentColors;
	}

	private final class EnchantmentRow extends OptionList.Row {
		private final String id;
		private final String name;
		private final HexBox hex;
		private final Button reset;

		EnchantmentRow(String id) {
			super(Ui.GAP, List.<AbstractWidget>of());
			this.id = id;
			this.name = displayName(id);
			Font font = Minecraft.getInstance().font;
			ColorSwatch swatch = new ColorSwatch(SWATCH, Ui.t("glintsmith.enchantments.color"),
				() -> EnchantmentColors.colorFor(id, config.enchantmentColors), null);
			this.hex = new HexBox(font, HEX_WIDTH, Ui.t("glintsmith.enchantments.color"), true, text -> {
				if (text.isEmpty()) {
					config.enchantmentColors.remove(id);
				} else {
					config.enchantmentColors.put(id, text);
				}
			});
			String current = config.enchantmentColors.get(id);
			hex.setValueSilently(current == null ? "" : current);
			hex.setHint(Component.literal(defaultHex(id)).withStyle(ChatFormatting.DARK_GRAY));
			hex.setTooltip(Ui.tip("glintsmith.enchantments.color"));
			this.reset = Ui.button(RESET_WIDTH, Ui.t("glintsmith.enchantments.reset"), Ui.tip("glintsmith.enchantments.reset"),
				button -> {
					config.enchantmentColors.remove(id);
					hex.setValueSilently("");
				});
			this.widgets = List.of(swatch, hex, reset);
		}

		@Override
		protected void layout(int x, int y) {
			int controls = x + Ui.WIDTH - CONTROLS_WIDTH;
			widgets.get(0).setPosition(controls, y);
			hex.setPosition(controls + SWATCH + Ui.GAP, y);
			reset.setPosition(controls + SWATCH + HEX_WIDTH + 2 * Ui.GAP, y);
		}

		@Override
		protected void renderBefore(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
			Font font = Minecraft.getInstance().font;
			reset.active = config.enchantmentColors.containsKey(id);
			int textWidth = Ui.WIDTH - CONTROLS_WIDTH - Ui.GAP;
			graphics.drawString(font, fit(font, name, textWidth), x, y + 1, 0xFFFFFFFF);
			graphics.drawString(font, fit(font, id, textWidth), x, y + 11, 0xFF808080);
		}

		private static String fit(Font font, String text, int maxWidth) {
			if (font.width(text) <= maxWidth) {
				return text;
			}
			return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
		}
	}

	private static String defaultHex(String id) {
		String known = EnchantmentColors.defaults().get(id);
		return known != null ? known : navrelis.glintsmith.style.Colors.toHex(EnchantmentColors.hashed(id));
	}
}
