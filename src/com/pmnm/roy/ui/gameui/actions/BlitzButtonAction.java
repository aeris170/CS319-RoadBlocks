package com.pmnm.roy.ui.gameui.actions;

import com.doa.ui.action.DoaUIAction;
import com.pmnm.risk.main.GameManager;

public class BlitzButtonAction implements DoaUIAction {

	@Override
	public void execute() {
		GameManager.INSTANCE.blitz();
	}
}