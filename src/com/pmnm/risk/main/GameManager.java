package com.pmnm.risk.main;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.doa.engine.DoaHandler;
import com.doa.engine.DoaObject;
import com.doa.engine.graphics.DoaGraphicsContext;
import com.doa.engine.input.DoaMouse;
import com.pmnm.risk.dice.Dice;
import com.pmnm.risk.dice.exceptions.DiceException;
import com.pmnm.risk.globals.PlayerColorBank;
import com.pmnm.risk.map.board.ProvinceConnector;
import com.pmnm.risk.map.continent.Continent;
import com.pmnm.risk.map.province.Province;
import com.pmnm.risk.map.province.ProvinceHitArea;
import com.pmnm.risk.toolkit.Utils;
import com.pmnm.risk.ui.gameui.BottomPanel;
import com.pmnm.risk.ui.gameui.DicePanel;
import com.pmnm.risk.ui.gameui.RiskGameScreenUI;

public class GameManager extends DoaObject {

	private static final long serialVersionUID = -4928417050440420799L;

	public static final List<Player> players = new ArrayList<>();

	public static int numberOfPlayers = 2;
	public static boolean manualPlacement = false;

	static GameInstance gameLoader = new GameInstance("ege", "ege");

	public static boolean isManualPlacementDone = false;
	public static final Map<Player, Integer> startingTroops = new HashMap<>();
	public static int placementCounter = 0;

	public static TurnPhase currentPhase = TurnPhase.DRAFT;
	public static int reinforcementForThisTurn = 0;
	public static Player currentPlayer;
	public static int turnCount = 0;

	public static ProvinceHitArea attackerProvinceHitArea = null;
	public static ProvinceHitArea defenderProvinceHitArea = null;
	public static DicePanel dicePanel = RiskGameScreenUI.DicePanel;

	public static ProvinceHitArea moveAfterOccupySource = null;
	public static ProvinceHitArea moveAfterOccupyDestination = null;

	public static ProvinceHitArea reinforcingProvince = null;
	public static ProvinceHitArea reinforcedProvince = null;

	public static ProvinceHitArea clickedHitArea;

	private static Province draftReinforceProvince = null;

	public GameManager() {
		super(0f, 0f);
		int startingTroopCount = Player.findStartingTroopCount(numberOfPlayers);
		for (int i = 0; i < numberOfPlayers; i++) {
			Player p = DoaHandler.instantiate(Player.class, "Player" + i, PlayerColorBank.get(i), true);
			players.add(p);
			startingTroops.put(p, startingTroopCount);
		}
		/* for (int i = 0; i < numberOfPlayers; i++) { Player p =
		 * DoaHandler.instantiate(AIPlayer.class, "AIPlayer" + i,
		 * PlayerColorBank.get(i), i); players.add(p); startingTroops.put(p,
		 * startingTroopCount); } */
		currentPlayer = players.get(0);
		currentPlayer.turn();
		if (!manualPlacement) {
			randomPlacement();
		}
	}

	public static void nextPhase() {
		if (currentPhase == TurnPhase.DRAFT) {
			currentPhase = TurnPhase.ATTACK;
			if (currentPlayer.isLocalPlayer()) {
				BottomPanel.nextPhaseButton.enable();
			}
			BottomPanel.nullSpinner();
		} else if (currentPhase == TurnPhase.ATTACK) {
			currentPhase = TurnPhase.REINFORCE;
			markAttackerProvince(null);
			markDefenderProvince(null);
		} else if (currentPhase == TurnPhase.REINFORCE) {
			currentPhase = TurnPhase.DRAFT;
			currentPlayer.endTurn();
			GameManager.turnCount++;
			currentPlayer = players.get(turnCount % players.size());
			currentPlayer.turn();
			reinforcementForThisTurn = Player.calculateReinforcementsForThisTurn(currentPlayer);
			markReinforcingProvince(null);
			markReinforcedProvince(null);
			BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
			BottomPanel.nextPhaseButton.disable();
		}
	}

	@Override
	public void tick() {
		if (DoaMouse.MB1) {
			clickedHitArea = ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.stream().filter(hitArea -> hitArea.isMouseClicked()).findFirst().orElse(null);
		}
		if (!isManualPlacementDone) {
			if (startingTroops.values().stream().allMatch(v -> v <= 0)) {
				isManualPlacementDone = true;
				reinforcementForThisTurn = Player.calculateReinforcementsForThisTurn(currentPlayer);
				BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
			}
		}
	}

	@Override
	public void render(DoaGraphicsContext g) {}

	public static void claimProvince(Province claimed) {
		claimed.getClaimedBy(currentPlayer);
		startingTroops.put(currentPlayer, startingTroops.get(currentPlayer) - 1);
		currentPlayer = players.get(++placementCounter % players.size());
		currentPlayer.turn();
	}

	public static void draftReinforce(int reinforcementCount) {
		if (draftReinforceProvince != null) {
			draftReinforceProvince.addTroops(reinforcementCount);
			if (!isManualPlacementDone) {
				startingTroops.put(currentPlayer, startingTroops.get(currentPlayer) - reinforcementCount);
				currentPlayer = players.get(++placementCounter % players.size());
				currentPlayer.turn();
			} else {
				reinforcementForThisTurn -= reinforcementCount;
				if (reinforcementForThisTurn <= 0) {
					nextPhase();
				} else {
					BottomPanel.updateSpinnerValues(1, reinforcementForThisTurn);
				}
			}
			draftReinforceProvince.getProvinceHitArea().isSelected = false;
			draftReinforceProvince = null;
		}
	}

	public static int numberOfReinforcementsForThisTurn() {
		return reinforcementForThisTurn;
	}

	public static boolean areAllProvincesClaimed() {
		return Province.ALL_PROVINCES.stream().filter(province -> province.isClaimed()).count() == Province.ALL_PROVINCES.size();
	}

	public static void markAttackerProvince(ProvinceHitArea province) {
		if (attackerProvinceHitArea != null) {
			ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.stream().filter(
			        hitArea -> attackerProvinceHitArea.getProvince().getNeighbours().contains(hitArea.getProvince()) && !hitArea.getProvince().isOwnedBy(currentPlayer))
			        .collect(Collectors.toList()).forEach(hitArea -> hitArea.deemphasizeForAttack());
			attackerProvinceHitArea.deselectAsAttacker();
		}
		attackerProvinceHitArea = province;
		if (attackerProvinceHitArea != null) {
			ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.stream().filter(
			        hitArea -> attackerProvinceHitArea.getProvince().getNeighbours().contains(hitArea.getProvince()) && !hitArea.getProvince().isOwnedBy(currentPlayer))
			        .collect(Collectors.toList()).forEach(hitArea -> hitArea.emphasizeForAttack());
			attackerProvinceHitArea.selectAsAttacker();
		}
	}

	public static void markDefenderProvince(ProvinceHitArea province) {
		if (defenderProvinceHitArea != null) {
			defenderProvinceHitArea.deselectAsDefender();
		}
		defenderProvinceHitArea = province;
		if (defenderProvinceHitArea != null) {
			defenderProvinceHitArea.selectAsDefender();
			if (currentPlayer.isLocalPlayer()) {
				dicePanel.show();
			}
		} else {
			if (currentPlayer.isLocalPlayer()) {
				dicePanel.hide();
			}
		}
	}

	public static ProvinceHitArea getAttackerProvince() {
		return attackerProvinceHitArea;
	}

	public static void toss(int diceAmount) {
		Integer[] attackerDiceValues = null;
		Integer[] defenderDiceValues = null;
		if (defenderProvinceHitArea.getProvince().getTroops() == 1 || diceAmount == 1) {
			defenderDiceValues = Arrays.stream(Dice.DEFENCE_DICE_1.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
		} else {
			defenderDiceValues = Arrays.stream(Dice.DEFENCE_DICE_2.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
		}
		switch (diceAmount) {
			case 1:
				if (attackerProvinceHitArea.getProvince().getTroops() > 1) {
					attackerDiceValues = Arrays.stream(Dice.ATTACK_DICE_1.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
				}
				break;
			case 2:
				if (attackerProvinceHitArea.getProvince().getTroops() > 2) {
					attackerDiceValues = Arrays.stream(Dice.ATTACK_DICE_2.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
				}
				break;
			case 3:
				if (attackerProvinceHitArea.getProvince().getTroops() > 3) {
					attackerDiceValues = Arrays.stream(Dice.ATTACK_DICE_3.rollAllAndGetAll()).boxed().toArray(Integer[]::new);
				}
				break;
			default:
				throw new DiceException("diceAmount not in the set (1, 2, 3)");
		}
		if (attackerDiceValues != null) {
			Arrays.sort(attackerDiceValues, Collections.reverseOrder());
			Arrays.sort(defenderDiceValues, Collections.reverseOrder());
			int attackerCasualties = 0;
			int defenderCasualties = 0;
			for (int i = 0; i < Math.min(attackerDiceValues.length, defenderDiceValues.length); i++) {
				if (attackerDiceValues[i] > defenderDiceValues[i]) {
					defenderCasualties++;
				} else {
					attackerCasualties++;
				}
			}
			defenderProvinceHitArea.getProvince().removeTroops(defenderCasualties);
			attackerProvinceHitArea.getProvince().removeTroops(attackerCasualties);
			if (attackerProvinceHitArea.getProvince().getTroops() == 1) {
				markAttackerProvince(null);
				markDefenderProvince(null);
			}
			if (defenderProvinceHitArea != null && defenderProvinceHitArea.getProvince().getTroops() <= 0) {
				// capture
				defenderProvinceHitArea.getProvince().removeTroops(defenderProvinceHitArea.getProvince().getTroops() + 1);
				BottomPanel.updateSpinnerValues(diceAmount, attackerProvinceHitArea.getProvince().getTroops() - 1);
				occupyProvince(defenderProvinceHitArea.getProvince());
			}
		}
	}

	public static void blitz() {
		if (attackerProvinceHitArea != null && defenderProvinceHitArea != null) {
			int attackerTroops = attackerProvinceHitArea.getProvince().getTroops();
			if (attackerTroops <= 1 || defenderProvinceHitArea.getProvince().getTroops() <= 0) {
				return;
			}
			switch (attackerTroops) {
				default:
					toss(3);
					break;
				case 3:
					toss(2);
					break;
				case 2:
					toss(1);
					break;
			}
			blitz();
		}
	}

	private static void occupyProvince(Province occupied) {
		ProvinceConnector.getInstance().setPath(attackerProvinceHitArea, defenderProvinceHitArea);
		attackerProvinceHitArea.isSelected = false;
		defenderProvinceHitArea.isSelected = false;
		occupied.getOccupiedBy(currentPlayer);
		defenderProvinceHitArea.deemphasizeForAttack();
		moveAfterOccupySource = attackerProvinceHitArea;
		moveAfterOccupyDestination = defenderProvinceHitArea;
		markAttackerProvince(null);
		markDefenderProvince(null);
		BottomPanel.nextPhaseButton.disable();
	}

	public static ProvinceHitArea getReinforcingProvince() {
		return reinforcingProvince;
	}

	public static void markReinforcingProvince(ProvinceHitArea province) {
		if (reinforcingProvince != null) {
			reinforcingProvince.deselectAsReinforcing();
			Utils.connectedComponents(reinforcingProvince).forEach(hitArea -> {
				hitArea.deemphasizeForReinforcement();
			});
		}
		reinforcingProvince = province;
		if (reinforcingProvince != null) {
			reinforcingProvince.selectAsReinforcing();
			Utils.connectedComponents(reinforcingProvince).forEach(hitArea -> {
				hitArea.emphasizeForReinforcement();
			});
		}
	}

	public static ProvinceHitArea getReinforcedProvince() {
		return reinforcedProvince;
	}

	public static void markReinforcedProvince(ProvinceHitArea province) {
		if (reinforcedProvince != null) {
			reinforcedProvince.deselectAsReinforced();
		}
		reinforcedProvince = province;
		if (reinforcedProvince != null) {
			reinforcedProvince.selectAsReinforced();
			ProvinceConnector.getInstance().setPath(Utils.shortestPath(reinforcingProvince, reinforcedProvince));
			if (currentPlayer.isLocalPlayer()) {
				BottomPanel.updateSpinnerValues(1, reinforcingProvince.getProvince().getTroops() - 1);
			}
		} else {
			BottomPanel.nullSpinner();
			ProvinceConnector.getInstance().setPath();
		}
	}

	public static void reinforce(int reinforcementCount) {
		if (reinforcingProvince != null && reinforcedProvince != null) {
			reinforcingProvince.getProvince().removeTroops(reinforcementCount);
			reinforcedProvince.getProvince().addTroops(reinforcementCount);
			Utils.connectedComponents(reinforcingProvince).forEach(p -> p.deemphasizeForReinforcement());
			ProvinceConnector.getInstance().setPath();
			reinforcingProvince.isSelected = false;
			reinforcedProvince.isSelected = false;
			nextPhase();
		}
	}

	private static void randomPlacement() {
		while (!Province.UNCLAIMED_PROVINCES.isEmpty()) {
			currentPlayer.endTurn();
			claimProvince(Province.getRandomUnclaimedProvince());
		}
		while (!startingTroops.values().stream().allMatch(v -> v == 0)) {
			List<Province> playerProvinces = Player.getPlayerProvinces(currentPlayer);
			currentPlayer.endTurn();
			draftReinforceProvince = playerProvinces.get(ThreadLocalRandom.current().nextInt(playerProvinces.size()));
			draftReinforce(1);
		}
	}

	public static void setDraftReinforceProvince(Province clickedProvince) {
		draftReinforceProvince = clickedProvince;
	}

	public static void moveTroopsAfterOccupying(int count) {
		// 1 is there because it was -1 before
		moveAfterOccupyDestination.getProvince().addTroops(1 + count);
		moveAfterOccupySource.getProvince().removeTroops(count);
		moveAfterOccupyDestination = null;
		moveAfterOccupySource = null;
		ProvinceConnector.getInstance().setPath();
	}

	public static boolean saveGame(String saveGameName) throws IOException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		String currentDate = dateFormat.format(date); // 2016/11/16 12:08:43
		String saveName = "firstSave";

		// This is new saveGame in our system
		GameInstance newGameInstance = new GameInstance(currentDate, saveName);

		saveGameVariables(newGameInstance);

		if (newGameInstance.saveNow(newGameInstance)) {
			System.out.println("Save is successfull");
			return true;
		} else {
			System.out.println("Save is not successful");
			return false;
		}

	}

	@SuppressWarnings("null")
	public static boolean loadGame(String loadName) {

		String filename = loadName + ".ser";

		GameInstance loadedGame = null;

		// look is file exist

		File tempFile = new File(filename);
		boolean fileExist = tempFile.exists();

		if (fileExist) {
			loadedGame = gameLoader.loadNow(loadName);
			updateGameManager(loadedGame);
			System.out.println("Load is successful");
			return true;
		}

		else {
			System.out.println("Save file is not found");
			return false;
		}

	}

	private static void updateGameManager(GameInstance loadedGame) {
		System.out.println(loadedGame.getCurrentPhase());
		System.out.println(loadedGame.getCurrentPhase());
		GameManager.currentPhase = loadedGame.getCurrentPhase();
		GameManager.currentPlayer = loadedGame.getCurrentPlayer();
		GameManager.draftReinforceProvince = loadedGame.getDraftReinforceProvince();
		GameManager.numberOfPlayers = loadedGame.getNumberOfPlayers();
		GameManager.placementCounter = loadedGame.getPlacementCounter();
		GameManager.reinforcementForThisTurn = loadedGame.getReinforcementForThisTurn();
		GameManager.turnCount = loadedGame.getTurnCount();

		System.out.println("Players are: ");
		for (int i = 0; i < GameManager.players.size(); i++) {
			System.out.println(GameManager.players.get(i).toString());
		}

		System.out.println("ALL_PROVINCES ARE old owners ");
		for (int i = 0; i < Province.ALL_PROVINCES.size(); i++) {
			System.out.println(Province.ALL_PROVINCES.get(i).getName() + "   " + Province.ALL_PROVINCES.get(i).getTroops());
		}

		// Lists
		List<Player> newPlayers = loadedGame.getPlayers();
		List<Province> newAllProvinces = loadedGame.getALL_PROVINCES();

		System.out.println("*****************");

		System.out.println("saved all provinces");
		for (int i = 0; i < newAllProvinces.size(); i++) {
			System.out.println(newAllProvinces.get(i).getName() + "    " + newAllProvinces.get(i).getTroops());
		}

		System.out.println(" ALL Provinces list now ");

		for (int i = 0; i < Province.ALL_PROVINCES.size(); i++) {
			Province.ALL_PROVINCES.set(i, newAllProvinces.get(i));
			System.out.println(Province.ALL_PROVINCES.get(i).getName() + "   " + Province.ALL_PROVINCES.get(i).getTroops());
		}

		for (ProvinceHitArea p : ProvinceHitArea.ALL_PROVINCE_HIT_AREAS) {
			DoaHandler.remove(p);
		}

		ProvinceHitArea.ALL_PROVINCE_HIT_AREAS.clear();

		// DoaHandler.remove(o);

		for (int i = 0; i < newAllProvinces.size(); i++) {
			// DoaHandler.remove(o);
			DoaHandler.instantiate(ProvinceHitArea.class, newAllProvinces.get(i), 0f, 0f, 0, 0);
		}

		System.out.println("Game is updated");
	}

	// save save game variables
	private static void saveGameVariables(GameInstance newGameInstance) {
		newGameInstance.setCurrentPhase(currentPhase);
		newGameInstance.setCurrentPlayer(currentPlayer);
		newGameInstance.setDraftReinforceProvince(draftReinforceProvince);
		newGameInstance.setManualPlacement(isManualPlacementDone);
		newGameInstance.setManualPlacementDone(isManualPlacementDone);
		newGameInstance.setNumberOfPlayers(numberOfPlayers);
		newGameInstance.setPlacementCounter(placementCounter);
		newGameInstance.setReinforcementForThisTurn(reinforcementForThisTurn);
		newGameInstance.setTurnCount(turnCount);
		newGameInstance.setPlayers(players);
		newGameInstance.setNAME_CONTINENT(Continent.NAME_CONTINENT);
		newGameInstance.setALL_PROVINCES(Province.ALL_PROVINCES);
		newGameInstance.setUNCLAIMED_PROVINCES(Province.UNCLAIMED_PROVINCES);

	}

}
