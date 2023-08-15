package tainavi;

import javax.swing.JLayeredPane;

/**
 * 自身が表示している日付の保持を追加した{@link JLayeredPane}
 */
public class JTaggedLayeredPane extends JLayeredPane {

	private static final long serialVersionUID = 1L;

	//
	private String tagstr = "";
	//
	public void setTagstr(String s) { tagstr = s; }
	public String getTagstr() { return tagstr; }
}
