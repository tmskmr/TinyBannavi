package tainavi;

import javax.swing.JLabel;

public class JTXTLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private String value = "";
	
	public void setValue(String s) { value = s; }
	public String getValue() { return value; }
}
