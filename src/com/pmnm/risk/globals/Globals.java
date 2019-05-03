package com.pmnm.risk.globals;

import java.awt.Color;

import com.pmnm.risk.asset.AssetLoader;
import com.pmnm.risk.globals.localization.Translator;
import com.pmnm.risk.map.MapLoader;
import com.pmnm.risk.ui.UIInit;

public final class Globals {

	public static final Color PROVINCE_UNOCCUPIED = Color.WHITE;
	public static final Color PROVINCE_UNOCCUPIED_BORDER = Color.BLACK;
	public static final Color PROVINCE_SELECTED_BORDER = Color.CYAN;
	public static final Color PROVINCE_EMPHASIZE = Color.GREEN;
	public static final Color PROVINCE_HIGHLIGHT = Color.GRAY.darker().darker().darker().darker();

	private Globals() {}

	public static void initilaizeGlobals() {
		MapLoader.readMapData();
		AssetLoader.initializeAssets();
		Translator.getInstance();
		UIInit.initUI();
	}
}
