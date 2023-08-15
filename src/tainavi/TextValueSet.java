package tainavi;

public class TextValueSet {
	private String Text;
	private String Value;
	private boolean defval = false;
	
	public String getText() { return Text; }
	public void setText(String t) { Text = t; } 
	public String getValue() { return Value; }
	public void setValue(String v) { Value = v; }
	public boolean getDefval() { return defval; }
	public void setDefval(boolean b) { defval = b; }
}
