package tainavi;

/*
 * ツールバーに表示するアイコンの情報
 */
public enum ToolBarIconInfo{
	KEYWORD				("キーワード",		true),
	SEARCH				("キーワード検索",	true),
	ADDKEYWORD			("キーワード追加",	true),
	SEPARATOR1			("セパレータ１",	true),
	RELOADPROGS		("番組情報再取得",	true),
	SHOWMATCHBORDER	("予約待機表示",	true),
	SHOWOFFRESERVE		("実行OFF予約表示",	true),
	MOVETONOW			("現在日時表示",	true),
	SEPARATOR2			("セパレータ２",	true),
	PREVPAGE			("ページャー前ページ",	true),
	PAGER				("ページャー",		true),
	NEXTPAGE			("ページャー次ページ",	true),
	SEPARATOR3			("セパレータ３",	true),
	BATCHRESERVATION	("一括登録",			true),
	RELOADRSVED		("レコーダー情報再取得",	true),
	SEPARATOR4			("セパレータ４",	true),
	WAKEUP				("レコーダー電源入",	true),
	SHUTDOWN			("レコーダー電源切",	true),
	SELECT_RECORDER	("レコーダー選択",		true),
	SEPARATOR5			("セパレータ５",	true),
	SNAPSHOT			("スナップショット",	true),
	PAGER_COLORS		("ジャンル別背景色設定",	true),
	PAGER_ZOOM			("番組表示枠拡大",		true),
	LOGVIEWER			("ログビューア表示",	true),
	TIMER				("タイマー",			true),
	SEPARATOR6			("セパレータ６",	true),
	SHOWSTATUS			("ステータス表示",		true),
	FULL_SCREEN		("フルスクリーンモード",	true),
	SEPARATOR7			("セパレータ７",	true),
	UPDATE				("オンラインアップデート",	true),
	HELP				("ヘルプ",				true),
	;

	private String		name;
	private boolean	visible;

	private ToolBarIconInfo(String name, boolean visible){
		this.name = name;
		this.visible = visible;
	}

	public String getName(){ return name; }
	public boolean getVisible(){ return visible; }

	public static ToolBarIconInfo getAt(int idx) {
		if ( idx >= ToolBarIconInfo.values().length ) {
			return null;
		}
		return (ToolBarIconInfo.values())[idx];
	}

	public static ToolBarIconInfo getByName(String name){
		if (name == null)
			return null;

		for (ToolBarIconInfo info : values()){
			if (name.equals(info.getName()))
				return info;
		}

		return null;
	}
}