package tainavi;

public interface StatusWindow {

	public void clear();

	/**
	 * 基本形
	 * @see #appendMessage(String)
	 * @see #appendError(String)
	 */
	public void append(String message);
	public void appendMessage(String message);
	public void appendError(String message);

	public void setVisible(boolean b);

	public void setWindowCloseRequested(boolean b);
	public boolean isWindowCloseRequested();
	public void resetWindowCloseRequested();
}
