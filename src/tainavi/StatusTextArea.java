package tainavi;

public interface StatusTextArea {

	public void clear();
	
	/**
	 * 基本形
	 * @see #appendMessage(String)
	 * @see #appendError(String)
	 */
	//public void append(String message);
	public void appendMessage(String message);
	public void appendError(String message);

	public void setVisible(boolean b);
	public int getRows();
	public void setRows(int rows);
}
