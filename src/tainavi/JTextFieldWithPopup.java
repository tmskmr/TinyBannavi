package tainavi;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class JTextFieldWithPopup extends JTextField {

	private static final long serialVersionUID = 1L;

	private static final String ESCKEYACTION = "escape-cancel";
	
	public JTextFieldWithPopup() {
		super();
		this.addMouseListener(new TextEditPopupMenu());
		
		InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = this.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCKEYACTION);
		am.put(ESCKEYACTION, new CancelAction());
	}

	public JTextFieldWithPopup(int col) {
		super(col);
		this.addMouseListener(new TextEditPopupMenu());
		
		InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = this.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCKEYACTION);
		am.put(ESCKEYACTION, new CancelAction());
	}
	
	//
	
	private class CancelAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			JTextFieldWithPopup jtf = (JTextFieldWithPopup) e.getSource();
			
			CancelEvent ev = new CancelEvent(jtf, CancelEvent.Cause.TOOLBAR_SEARCH);
			for ( CancelListener l : cancel_listeners ) {
				l.cancelRised(ev);
			}
		}
	};
	
	//
	
	private ArrayList<CancelListener> cancel_listeners = new ArrayList<CancelListener>();
	
	public void addCancelListener(CancelListener l) {
		if ( ! cancel_listeners.contains(l) ) {
			cancel_listeners.add(l);
		}
	}
	
	public void removeCancelListener(CancelListener l) {
		cancel_listeners.remove(l);
	}
}
