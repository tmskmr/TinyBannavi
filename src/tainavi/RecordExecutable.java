package tainavi;

public interface RecordExecutable {

	/**
	 * 新規登録を行う
	 */
	public void doRecord();
	
	/**
	 * 更新を行う
	 */
	public void doUpdate();
	
	/**
	 * ダイアログを閉じる
	 */
	public void doCancel();
	
	/**
	 * 番組IDを取得する
	 */
	public String doGetEventId();
	
}
