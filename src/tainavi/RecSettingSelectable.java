package tainavi;

public interface RecSettingSelectable {

	/**
	 * レコーダを選択したのでアイテムをリセットしてほしい
	 */
	public boolean doSelectRecorder(String myself);

	/**
	 * エンコーダを選択したのでアイテムをリセットしてほしい
	 */
	public boolean doSelectEncoder(String encoder);

	/**
	 * エンコーダを選択したのでアイテムをリセットしてほしい
	 */
	public boolean doSelectVrate(String vrate);


	/**
	 * ＡＶ別録画設定を適用してほしい
	 */
	public boolean doSetAVSettings();

	/**
	 * ＡＶ別録画設定を保存してほしい
	 */
	public boolean doSaveAVSettings(boolean savedefault);

	/*
	 * 選択中のタイトルを教えてほしい
	 */
	public String doGetSelectedTitle();

}
