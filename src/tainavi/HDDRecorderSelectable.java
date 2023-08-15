package tainavi;

public interface HDDRecorderSelectable {
	
	/**
	 * レコーダ選択イベントリスナー
	 */
	
	public void addHDDRecorderSelectionListener(HDDRecorderListener l);

	public void removeHDDRecorderSelectionListener(HDDRecorderListener l);

	public String getSelectedMySelf();
	
	public HDDRecorderList getSelectedList();

	
	/**
	 * レコーダ情報変更イベントリスナー
	 */
	
	public void addHDDRecorderChangeListener(HDDRecorderListener l);

	public void removeHDDRecorderChangeListener(HDDRecorderListener l);
	

}
