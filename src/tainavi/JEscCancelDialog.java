package tainavi;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

abstract class JEscCancelDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	//
	abstract protected void doCancel();

	//
	private static final String ESCKEYACTION = "escape";

	//
	public JEscCancelDialog() {
		super();

		InputMap imap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCKEYACTION);
		getRootPane().getActionMap().put(ESCKEYACTION, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				doCancel();
			}
		});
	}
}
