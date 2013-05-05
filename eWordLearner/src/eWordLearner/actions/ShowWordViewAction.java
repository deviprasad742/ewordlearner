package eWordLearner.actions;

import org.eclipse.jface.action.IAction;

import eWordLearner.ui.WordView;

public class ShowWordViewAction extends ToolBarAction {

	public void run(IAction action) {
		WordView.showView();
	}

}
