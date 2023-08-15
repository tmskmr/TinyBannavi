package tainavi;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class TextEditPopupMenu extends MouseAdapter {

	@Override
	public void mouseReleased(MouseEvent e) {
		mousePopup(e);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		mousePopup(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	//
	private void mousePopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			JComponent c = (JComponent)e.getSource();
			showPopup(c, e.getX(), e.getY());
			e.consume();
		}
	}

	private void showPopup(JComponent c, int x, int y) {
		JTextComponent tc = (JTextComponent)c;
		boolean bc = tc.getSelectedText() != null;
		boolean bp = tc.isEditable();

        JPopupMenu pmenu = new JPopupMenu();

		ActionMap am = c.getActionMap();

		Action cut = am.get(DefaultEditorKit.cutAction);
		addMenu(pmenu, "切り取り(X)", cut, 'X', KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), bc && bp);

		Action copy = am.get(DefaultEditorKit.copyAction);
		addMenu(pmenu, "コピー(C)", copy, 'C', KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), bc);

		Action paste = am.get(DefaultEditorKit.pasteAction);
		addMenu(pmenu, "貼り付け(V)", paste, 'V', KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), bp);

		Action all = am.get(DefaultEditorKit.selectAllAction);
		addMenu(pmenu, "すべて選択(A)", all, 'A', KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), true);

		pmenu.addSeparator();

		JMenuItem mi = new JMenuItemWithShortcut("Googleで検索(S)");
		mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String value = tc.getSelectedText();
					;
				if (value != null && !value.isEmpty()){
					if (value != null && !value.isEmpty()){
						try {
							showURL("http://www.google.co.jp/search?q=" + URLEncoder.encode(value, "UTF-8"));
						} catch (UnsupportedEncodingException e1) {
							// TODO 自動生成された catch ブロック
							e1.printStackTrace();
						}
					}
				}
			}
		});
		mi.setEnabled(bc);
		pmenu.add(mi);

		JMenuItem mi2 = new JMenuItemWithShortcut("Syobocalで検索(B)");
		mi2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK));
		mi2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String value = tc.getSelectedText();
				if (value != null && !value.isEmpty()){
					try {
						showURL("http://cal.syoboi.jp/find?kw=" + URLEncoder.encode(value, "UTF-8") + "&exec=%E6%A4%9C%E7%B4%A2");
					} catch (UnsupportedEncodingException e1) {
						// TODO 自動生成された catch ブロック
						e1.printStackTrace();
					}
				}
			}
		});
		mi2.setEnabled(bc);
		pmenu.add(mi2);

		c.requestFocusInWindow();
		pmenu.show(c, x, y);
	}

	private void showURL(String url){
		Desktop desktop = Desktop.getDesktop();
		try {
			desktop.browse(new URI(url));
		} catch (IOException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
	}

	private void addMenu(JPopupMenu pmenu, String text, Action action, int mnemonic, KeyStroke ks, boolean enable) {
		if (action != null) {
			JMenuItem mi = pmenu.add(action);
			if (text != null) {
				mi.setText(text);
			}
			if (mnemonic != 0) {
				mi.setMnemonic(mnemonic);
			}
			if (ks != null) {
				mi.setAccelerator(ks);
			}

			mi.setEnabled(enable);
		}
	}
}
