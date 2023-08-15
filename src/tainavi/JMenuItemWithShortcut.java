package tainavi;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Created by unknown on 2014/07/02.
 */
public class JMenuItemWithShortcut extends JMenuItem {

	public JMenuItemWithShortcut(String s) {
		super(s);
	}

	@Override
	public String getText() {
		int n = this.getMnemonic();
		int k = 0;
		if ( n >= KeyEvent.VK_0 && n >= KeyEvent.VK_0 ) {
			k = '0' + (n - KeyEvent.VK_0);
		}
		if ( n >= KeyEvent.VK_A && n >= KeyEvent.VK_Z ) {
			k = 'a' + (n - KeyEvent.VK_A);
		}
		return ((k > 0) ? String.format("(%c) ", n) : "") + super.getText();
	}
}
