package tainavi;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;



/**
 * 【結論】ちゃんと設計しないとメンバの変更で簡単に例外が起きる。できるならシリアライズは自前で組もう！
 */
public class Bounds {
	public static Bounds TheBounds = null;

	//
	private static final String BOUNDS_FILE = "env"+File.separator+"bounds.xml";
	private static final String BOUNDS_TEXT = "env"+File.separator+"bounds.txt";

	//
	private Rectangle winRectangle;
	public Rectangle getWinRectangle() { return winRectangle; }
	public void setWinRectangle(Rectangle r) { winRectangle = r; }
	//
	private int bangumiColumnWidth;
	public int getBangumiColumnWidth() { return bangumiColumnWidth; }
	public void setBangumiColumnWidth(int w) { bangumiColumnWidth = w; }
	private int bangumiColumnHeight;
	public int getBangumiColumnHeight() { return bangumiColumnHeight; }
	public void setBangumiColumnHeight(int h) { bangumiColumnHeight = h; }
	//
	private int treeWidth;
	public int getTreeWidth() { return treeWidth; }
	public void setTreeWidth(int w) { treeWidth = w; }
	private int treeWidthPaper;
	public int getTreeWidthPaper() { return treeWidthPaper; }
	public void setTreeWidthPaper(int w) { treeWidthPaper = w; }

	// サイドツリーの最小幅（固定）
	public int getMinDivLoc() { return 32; }

	//
	public int tooltipWidth;
	public int getTooltipWidth() { return tooltipWidth; }
	public void setTooltipWidth(int w) { tooltipWidth = w; }

	//
	private int timebarColumnWidth;
	public int getTimebarColumnWidth() { return timebarColumnWidth; }
	public void setTimebarColumnWidth(int w) { timebarColumnWidth = w; }
	//
	private float paperHeightMultiplier;
	public float getPaperHeightMultiplier() { return paperHeightMultiplier; }
	public void setPaperHeightMultiplier(float m) { paperHeightMultiplier = m; }
	//
	@Deprecated
	private int detailAreaHeight;
	@Deprecated
	public int getDetailAreaHeight() { return detailAreaHeight; }
	@Deprecated
	public void setDetailAreaHeight(int h) { detailAreaHeight = h; }
	//
	@Deprecated
	private int statusAreaHeight;
	@Deprecated
	public int getStatusAreaHeight() { return statusAreaHeight; }
	@Deprecated
	public void setStatusAreaHeight(int h) { statusAreaHeight = h; }

	//
	private int selectedTab;
	public int getSelectedTab() { return selectedTab; }
	public void setSelectedTab(int t) { selectedTab = t; }

	//
	@Deprecated
	private boolean showSettingTabs;
	@Deprecated
	public boolean getShowSettingTabs() { return showSettingTabs; }
	@Deprecated
	public void setShowSettingTabs(boolean b) { showSettingTabs = b; }

	// 予約済み背景色を描画する（リスト形式）
	public boolean getShowReservedBackground() { return showReservedBackground; }
	public void setShowReservedBackground(boolean b) { showReservedBackground = b; }
	private boolean showReservedBackground = true;

	// 実行OFFの予約枠を表示するかどうか
	public boolean getShowOffReserve(){ return showOffReserve; }
	public void setShowOffReserve(boolean b){ showOffReserve = b; }
	private boolean showOffReserve = true;

	// 検索マッチ枠を表示する（新聞形式）
	public boolean getShowMatchedBorder() { return showMatchedBorder; }
	public void setShowMatchedBorder(boolean b) { showMatchedBorder = b; }
	private boolean showMatchedBorder = true;

	// ステータスエリアを表示する
	private boolean showStatus;
	public boolean getShowStatus() { return showStatus; }
	public void setShowStatus(boolean b) { showStatus = b; }

	// 番組詳細エリアの高さ
	public int getDetailRows() { return detailRows; }
	public void setDetailRows(int n) { detailRows = n; }
	private int detailRows = 4;

	// ステータスエリアの高さ
	public int getStatusRows() { return statusRows; }
	public void setStatusRows(int n) { statusRows = n; }
	private int statusRows = 4;

	//
	private boolean enableTimer;
	public boolean getEnableTimer() { return enableTimer; }
	public void setEnableTimer(boolean b) { enableTimer = b; }

	//
	public int getTimelinePosition() { return timelinePosition; }
	public void setTimelinePosition(int n) { timelinePosition = n; }
	private int timelinePosition = 30;

	// リスト形式のカラム幅

	@Deprecated
	private HashMap<Viewer.ListedColumn, Integer> listedColumnWidth;
	@Deprecated
	public HashMap<Viewer.ListedColumn, Integer> getListedColumnWidth() { return listedColumnWidth; }
	@Deprecated
	public void setListedColumnWidth(HashMap<Viewer.ListedColumn, Integer> map) {
		//listedColumnWidth = map;
		if ( map.size() > 0 ) {
			for ( Viewer.ListedColumn lc : map.keySet() ) {
				// 新しいほうに入れなおす
				listedColumnSize.put(lc.toString(), map.get(lc));
			}
			listedColumnWidth.clear();
		}
	}

	private HashMap<String, Integer> listedColumnSize;
	public HashMap<String, Integer> getListedColumnSize() { return listedColumnSize; }
	public void setListedColumnSize(HashMap<String, Integer> map) { listedColumnSize = map; }

	// 本体予約一覧のカラム幅

	@Deprecated
	private HashMap<Viewer.RsvedColumn, Integer> rsvedColumnWidth;
	@Deprecated
	public HashMap<Viewer.RsvedColumn, Integer> getRsvedColumnWidth() { return rsvedColumnWidth; }
	@Deprecated
	public void setRsvedColumnWidth(HashMap<Viewer.RsvedColumn, Integer> map) {
		//rsvedColumnWidth = map;
		if ( map.size() > 0 ) {
			for ( Viewer.RsvedColumn rc : map.keySet() ) {
				// 新しいほうに入れなおす
				rsvedColumnSize.put(rc.toString(), map.get(rc));
			}
			rsvedColumnWidth.clear();
		}
	}

	// 録画結果一覧のカラム幅
	private HashMap<String, Integer> recordedColumnSize;
	public HashMap<String, Integer> getRecordedColumnSize() { return recordedColumnSize; }
	public void setRecordedColumnSize(HashMap<String, Integer> map) { recordedColumnSize = map; }

	private HashMap<String, Integer> rsvedColumnSize;
	public HashMap<String, Integer> getRsvedColumnSize() { return rsvedColumnSize; }
	public void setRsvedColumnSize(HashMap<String, Integer> map) { rsvedColumnSize = map; }

	// タイトル一覧のカラム幅
	private HashMap<String, Integer> titleColumnSize;
	public HashMap<String, Integer> getTitleColumnSize() { return titleColumnSize; }
	public void setTitleColumnSize(HashMap<String, Integer> map) { titleColumnSize = map; }


	// 選択中のレコーダ
	private String selectedRecorderId;
	public String getSelectedRecorderId() { return selectedRecorderId; }
	public void setSelectedRecorderId(String s) { selectedRecorderId = s; }

	//
	private boolean expandedSyobocalNode;
	private boolean expandedStandbyNode;
	private boolean expandedTraceNode;
	private boolean expandedKeywordNode;
	private boolean expandedKeywordGrpNode;
	private boolean expandedGenreNode;
	private boolean expandedCenterListNode;
	private boolean expandedExtensionNode;
	public boolean getExpandedSyobocalNode() { return expandedSyobocalNode; }
	public void setExpandedSyobocalNode(boolean b) { expandedSyobocalNode = b; }
	public boolean getExpandedStandbyNode() { return expandedStandbyNode; }
	public void setExpandedStandbyNode(boolean b) { expandedStandbyNode = b; }
	public boolean getExpandedTraceNode() { return expandedTraceNode; }
	public void setExpandedTraceNode(boolean b) { expandedTraceNode = b; }
	public boolean getExpandedKeywordNode() { return expandedKeywordNode; }
	public void setExpandedKeywordNode(boolean b) { expandedKeywordNode = b; }
	public boolean getExpandedKeywordGrpNode() { return expandedKeywordGrpNode; }
	public void setExpandedKeywordGrpNode(boolean b) { expandedKeywordGrpNode = b; }
	public boolean getExpandedGenreNode() { return expandedGenreNode; }
	public void setExpandedGenreNode(boolean b) { expandedGenreNode = b; }
	public boolean getExpandedCenterListNode() { return expandedCenterListNode; }
	public void setExpandedCenterListNode(boolean b) { expandedCenterListNode = b; }
	public boolean getExpandedExtensionNode() { return expandedExtensionNode; }
	public void setExpandedExtensionNode(boolean b) { expandedExtensionNode = b; }

	//
	private boolean expandedDateNode;
	private boolean expandedDgNode;
	private boolean expandedCsNode;
	private boolean expandedRadioNode;
	private boolean expandedCenterNode;
	public boolean getExpandedDateNode() { return expandedDateNode; }
	public void setExpandedDateNode(boolean b) { expandedDateNode = b; }
	public boolean getExpandedDgNode() { return expandedDgNode; }
	public void setExpandedDgNode(boolean b) { expandedDgNode = b; }
	public boolean getExpandedCsNode() { return expandedCsNode; }
	public void setExpandedCsNode(boolean b) { expandedCsNode = b; }
	public boolean getExpandedRadioNode() { return expandedRadioNode; }
	public void setExpandedRadioNode(boolean b) { expandedRadioNode = b; }
	public boolean getExpandedCenterNode() { return expandedCenterNode; }
	public void setExpandedCenterNode(boolean b) { expandedCenterNode = b; }

	//
	private int frameBufferSize;
	public int getFrameBufferSize() { return frameBufferSize; }
	public void setFrameBufferSize(int n) { frameBufferSize = n; }

	// 検索ボックス欄の横幅
	private int searchBoxAreaWidth;
	public int getSearchBoxAreaWidth(){ return searchBoxAreaWidth; }
	public void setSearchBoxAreaWidth(int n){ searchBoxAreaWidth = n; }

	// ページャコンボの横幅
	private int pagerComboWidth=16*6;
	public int getPagerComboWidth(){ return pagerComboWidth; }
	public void setPagerComboWidth(int n){ pagerComboWidth = n; }

	// レコーダーコンボの横幅
	private int recorderComboWidth=16*10;
	public int getRecorderComboWidth(){ return recorderComboWidth; }
	public void setRecorderComboWidth(int n){ recorderComboWidth = n; }

	// リスト形式の詳細欄の高さ
	private int listedDetailHeight;
	public int getListedDetailHeight(){ return listedDetailHeight; }
	public void setListedDetailHeight(int n){ listedDetailHeight = n; }

	// 新聞形式の詳細欄の高さ
	private int paperDetailHeight;
	public int getPaperDetailHeight(){ return paperDetailHeight; }
	public void setPaperDetailHeight(int n){ paperDetailHeight = n; }

	// 本体予約一覧の詳細欄の高さ
	private int reserveDetailHeight;
	public int getReserveDetailHeight(){ return reserveDetailHeight; }
	public void setReserveDetailHeight(int n){ reserveDetailHeight = n; }

	// ステータスウインドウの高さ
	private int statusWindowHeight;
	public int getStatusWindowHeight(){ return statusWindowHeight; }
	public void setStatusWindowHeight(int n){ statusWindowHeight = n; }

	//
	private boolean loaded;
	public boolean isLoaded() { return loaded; }
	public void setLoaded(boolean b) { loaded = b; }


	/***
	 *
	 */

	public boolean save() {
		System.out.println("ウィンドウサイズ・位置情報を保存します: "+BOUNDS_TEXT);
		if ( ! FieldUtils.save(BOUNDS_TEXT, this) ) {
			System.err.println("ウィンドウサイズ・位置情報の保存に失敗しました.");
			return false;
		}
		return true;
	}

	public boolean load() {

		File ft = new File(BOUNDS_TEXT);
		if ( ft.exists() ) {
			// 移行済み
			System.out.println("@Deprecated: "+BOUNDS_FILE);
			return true;
		}

		System.out.println("ウィンドウサイズ・位置情報を読み込みます: "+BOUNDS_FILE);
		File fx = new File(BOUNDS_FILE);
		if ( fx.exists() ) {
			Bounds b = (Bounds) CommonUtils.readXML(BOUNDS_FILE);
			if ( b != null ) {
				b.setListedColumnWidth(b.getListedColumnWidth());	// 旧型式→新形式に変換
				b.setRsvedColumnWidth(b.getRsvedColumnWidth());		// 旧型式→新形式に変換
				FieldUtils.deepCopy(this, b);

				// テキスト形式がなければ作るよ
				if ( FieldUtils.save(BOUNDS_TEXT,this) ) {
					fx.renameTo(new File(BOUNDS_FILE+".bak"));
				}

				return true;
			}
		}

		System.err.println("ウィンドウサイズ・位置情報を読み込めなかったのでデフォルトの設定値を利用します.");
		return false;
	}

	public boolean loadText() {
		System.out.println("ウィンドウサイズ・位置情報を読み込みます: "+BOUNDS_TEXT);
		if ( FieldUtils.load(BOUNDS_TEXT, this) ) {
			return true;
		}
		System.err.println("ウィンドウサイズ・位置情報を読み込めなかったのでデフォルトの設定値を利用します.");
		return false;
	}

	@SuppressWarnings("unchecked")
	public Bounds() {
		//
		this.setWinRectangle(new Rectangle(0,0,1134,768));
		this.setBangumiColumnWidth(125);
		this.setBangumiColumnHeight(25);
		this.setTreeWidth(180);
		this.setTreeWidthPaper(this.getTreeWidth());
		this.setTooltipWidth(20);
		this.setTimebarColumnWidth(32);
		this.setPaperHeightMultiplier(3);
		this.setDetailAreaHeight(100);
		this.setStatusAreaHeight(21*3);
		this.setSelectedTab(0);
		this.setShowSettingTabs(true);
		//this.setShowMatchedBorder(false);
		this.setShowStatus(true);
		this.setEnableTimer(false);
		this.setFrameBufferSize(900);
		this.setSelectedRecorderId(null);
		this.setSearchBoxAreaWidth(500);

		this.setLoaded(false);

		// 旧版との互換部分
		listedColumnWidth = new HashMap<Viewer.ListedColumn, Integer>();
		/*
		for ( Viewer.ListedColumn lc : Viewer.ListedColumn.values() ) {
			listedColumnWidth.put(lc,lc.getIniWidth());
		}
		*/

		//
		rsvedColumnWidth = new HashMap<Viewer.RsvedColumn, Integer>();
		/*
		for ( RsvedColumn rc : RsvedColumn.values() ) {
			rsvedColumnWidth.put(rc,rc.getIniWidth());
		}
		*/

		// さむしんぐにゅー
		listedColumnSize = (HashMap<String, Integer>) AbsListedView.getColumnIniWidthMap().clone();
		rsvedColumnSize = (HashMap<String, Integer>) AbsReserveListView.getColumnIniWidthMap().clone();
		titleColumnSize = (HashMap<String, Integer>) AbsTitleListView.getColumnIniWidthMap().clone();
		recordedColumnSize = (HashMap<String, Integer>) AbsRecordedListView.getColumnIniWidthMap().clone();
	}
}
