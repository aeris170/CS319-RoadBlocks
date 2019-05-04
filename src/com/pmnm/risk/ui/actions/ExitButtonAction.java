package com.pmnm.risk.ui.actions;

import com.doa.ui.action.DoaUIAction;
import com.pmnm.risk.ui.ExitPopup;

public class ExitButtonAction implements DoaUIAction {

	ExitPopup ep;

	public ExitButtonAction(ExitPopup ep) {
		this.ep = ep;
	}

	@Override
	public void execute() {
		ep.show();
	}
}