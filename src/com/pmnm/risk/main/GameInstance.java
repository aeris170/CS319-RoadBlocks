package com.pmnm.risk.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.doa.engine.DoaHandler;
import com.pmnm.risk.card.Card;
import com.pmnm.risk.map.board.ProvinceConnector;
import com.pmnm.risk.map.continent.Continent;
import com.pmnm.risk.map.province.Province;
import com.pmnm.risk.map.province.ProvinceHitArea;
import com.pmnm.risk.map.province.ProvinceHitArea.ProvinceSymbol;
import com.pmnm.roy.ui.gameui.RiskGameScreenUI;

public final class GameInstance implements Serializable {

	private static final long serialVersionUID = 3374349513952165496L;

	List<Province> provinces;
	Map<String, Continent> continents;
	GameManager gm;
	ProvinceConnector pc;

	List<Card> UNDISTRIBUTED_CARDS;
	Map<Province, Card> PROVINCE_CARDS;
	Map<String, Player> NAME_PLAYER;
	List<Province> UNCLAIMED_PROVINCES;
	List<ProvinceHitArea> ALL_PROVINCE_HIT_AREAS;
	List<ProvinceSymbol> ALL_PROVINCE_SYMBOLS;

	private GameInstance() {
		this.provinces = Province.ALL_PROVINCES;
		this.continents = Continent.NAME_CONTINENT;
		this.gm = GameManager.INSTANCE;
		this.pc = ProvinceConnector.getInstance();
		UNDISTRIBUTED_CARDS = Card.UNDISTRIBUTED_CARDS;
		PROVINCE_CARDS = Card.PROVINCE_CARDS;
		NAME_PLAYER = Player.NAME_PLAYER;
		UNCLAIMED_PROVINCES = Province.UNCLAIMED_PROVINCES;
		ALL_PROVINCE_HIT_AREAS = ProvinceHitArea.ALL_PROVINCE_HIT_AREAS;
		ALL_PROVINCE_SYMBOLS = ProvinceHitArea.ALL_PROVINCE_SYMBOLS;
	}

	public static void saveGame() throws IOException {
		GameInstance gi = new GameInstance();
		String mapName = GameManager.INSTANCE.currentMapName;
		String dir = System.getProperty("user.home") + "\\Documents\\My Games\\RiskDigitalCut\\Saves\\" + mapName + "\\";
		File f = new File(dir);
		f.mkdirs();
		try (FileOutputStream file = new FileOutputStream(dir + "save_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + mapName + ".sav")) {
			try (ObjectOutputStream out = new ObjectOutputStream(file)) {
				out.writeObject(gi);
				System.out.println("Object has been serialized");
			}
		}
	}

	public static void loadGame() throws FileNotFoundException, IOException, ClassNotFoundException {
		// TODO take input from UI
		String mapName = GameManager.INSTANCE.currentMapName;
		String dir = System.getProperty("user.home") + "\\Documents\\My Games\\RiskDigitalCut\\Saves\\" + mapName + "\\save_20190514_124759_classic.sav";

		ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.forEach(pha -> DoaHandler.remove(pha));
		ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.clear();
		ProvinceHitArea.ALL_PROVINCE_SYMBOLS.forEach(ps -> DoaHandler.remove(ps));
		ProvinceHitArea.ALL_PROVINCE_SYMBOLS.clear();
		Player.NAME_PLAYER.values().forEach(p -> DoaHandler.remove(p));
		Player.NAME_PLAYER.clear();
		DoaHandler.remove(GameManager.INSTANCE);

		try (FileInputStream file = new FileInputStream(dir)) {
			try (ObjectInputStream in = new ObjectInputStream(file)) {
				GameInstance loadedGame = (GameInstance) in.readObject();
				GameManager.INSTANCE = loadedGame.gm;
				GameManager.INSTANCE.dicePanel = RiskGameScreenUI.DicePanel;
				DoaHandler.add(GameManager.INSTANCE);
				Province.ALL_PROVINCES = loadedGame.provinces;
				Continent.NAME_CONTINENT = loadedGame.continents;
				Card.UNDISTRIBUTED_CARDS = loadedGame.UNDISTRIBUTED_CARDS;
				Card.PROVINCE_CARDS = loadedGame.PROVINCE_CARDS;
				Player.NAME_PLAYER = loadedGame.NAME_PLAYER;
				Province.UNCLAIMED_PROVINCES = loadedGame.UNCLAIMED_PROVINCES;
				ProvinceHitArea.ALL_PROVINCE_HIT_AREAS = loadedGame.ALL_PROVINCE_HIT_AREAS;
				ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.forEach(pha -> {
					pha.cacheMeshAsImage();
					DoaHandler.add(pha);
				});
				ProvinceHitArea.ALL_PROVINCE_SYMBOLS = loadedGame.ALL_PROVINCE_SYMBOLS;
				ProvinceHitArea.ALL_PROVINCE_SYMBOLS.forEach(ps -> DoaHandler.add(ps));
				ProvinceConnector.deserialize(loadedGame.pc);
				Player.NAME_PLAYER.values().forEach(p -> DoaHandler.add(p));
				System.out.println("Object has been deserialized ");
			}
		}
	}

}