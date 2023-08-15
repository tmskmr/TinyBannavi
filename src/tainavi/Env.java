package tainavi;

import java.awt.Color;
import java.awt.RenderingHints;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @version 3.15.4β {@link #getAutoEventIdComplete()} 追加
 */
public class Env {
	public static Env TheEnv = null;

	/*******************************************************************************
	 * 定数とか
	 ******************************************************************************/

	// 変更できないフォルダ
	public static final String binDir = "bin";
	private static final String metainfDir = binDir+File.separator+"META-INF";
	private static final String services = metainfDir+File.separator+"services";

	public static final String envDir = "env";
	private static final String envFile = envDir+File.separator+"envs.xml";
	private static final String envText = envDir+File.separator+"envs.txt";
	private static final String lafFile = envDir+File.separator+"laf.txt";
	private static final String GTKRC = envDir+File.separator+"_gtkrc-2.0";

	public static final String skinDir = "skin";

	private static final int PAGER_DEFAULT = 7;

	private static final Color RSVDLINE_COLOR = new Color(204,153,255);
	private static final Color PICKEDLINE_COLOR = new Color(51,255,0);
	private static final Color CURRENTLINE_COLOR = new Color(240,120,120);
	private static final Color MATCHEDKEYWORD_COLOR = new Color(51,102,255);

	private static final Color TIMEBAR_COLOR_06_12 = new Color(180,180,180);
	private static final Color TIMEBAR_COLOR_12_18 = new Color(160,160,160);
	private static final Color TIMEBAR_COLOR_18_24 = new Color(140,140,140);
	private static final Color TIMEBAR_COLOR_24_06 = new Color(120,120,120);
	private static final Color EXECON_TUNE_COLOR = Color.WHITE;
	private static final Color EXECOFF_TUNE_COLOR = Color.GRAY;
	private static final Color PICKUP_BORDER_COLOR = new Color(0,255,0);
	private static final Color PICKUP_TUNER_COLOR = Color.GRAY;
	private static final Color MOUSEOVER_COLOR = new Color(180,180,255);
	private static final Color TITLE_COLOR = Color.BLUE;
	private static final Color DETAIL_COLOR = Color.DARK_GRAY;
	private static final Color MATCHBORDER_COLOR = Color.RED;
	private static final Color ITERATIONITEM_COLOR = Color.GRAY;

	private static final Color TUNERSHORT_COLOR = new Color(255,255,0);
	private static final Color RECORDED_COLOR = new Color(204,153,255);

	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win32; x86) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.87 Safari/537.36";

	private static final Color RESTIMEBAR_COLOR_1 = new Color(0,0,255);		// BLUE
	private static final Color RESTIMEBAR_COLOR_2 = new Color(0,255,0);		// GREEN;
	private static final Color RESTIMEBAR_COLOR_3 = new Color(255,0,0);		// RED;
	private static final Color RESTIMEBAR_COLOR_4 = new Color(255,0,255);	// MAGENTA;
	private static final int RESTIMEBAR_WIDTH = 4;

	private static final int MAX_SEARCH_WORD_NUM = 64;

	// この記述は…どうなの？
	private static final String TV_SITE = "Dimora";
	private static final String CS_SITE = "Gガイド.テレビ王国(スカパー!)";
	private static final String CS2_SITE = "Dimora(CSデジ)";
	private static final String RADIO_SITE = "";

	private static final String MSGID = "[環境] ";
	private static final String ERRID = "[ERROR]"+MSGID;

	public static String[] SHUTDOWN_COMMANDS = { "shutdown /s /t 0", "sudo shutdown -h now" };

	// ダブルクリック時の動作
	public static enum DblClkCmd {

		SHOWRSVDIALOG ("予約ダイアログを開く"),
		JUMPTOPAPER ("番組欄へジャンプする"),
		JUMPTOWEB ("ブラウザで番組詳細のページを開く");

		private String name;

		private DblClkCmd(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return(name);
		}

		public String getId() {
			return(super.toString());
		}

		public static DblClkCmd get(String id) {
			for ( DblClkCmd dcc : DblClkCmd.values() ) {
				if ( dcc.getId().equals(id) ) {
					return dcc;
				}
			}
			return null;
		}
	};

	//
	public static enum SnapshotFmt {
		JPG ("JPEG","jpg"),
		PNG ("PNG","png"),
		BMP ("BMP","bmp");

		private String name;
		private String ext;

		private SnapshotFmt(String name, String ext) {
			this.name = name;
			this.ext = ext;
		}

		@Override
		public String toString() {
			return(name);
		}

		public String getExtension() {
			return(ext);
		}


		public String getId() {
			return(super.toString());
		}

		public static SnapshotFmt get(String id) {
			for ( SnapshotFmt sf : SnapshotFmt.values() ) {
				if ( sf.getId().equals(id) ) {
					return sf;
				}
			}
			return null;
		}
	}

	public static enum AAMode {
		OFF		("無効","off",RenderingHints.VALUE_TEXT_ANTIALIAS_OFF),
		ON		("有効","on",RenderingHints.VALUE_TEXT_ANTIALIAS_ON),
		GASP	("小さい文字には無効", "gasp",RenderingHints.VALUE_TEXT_ANTIALIAS_GASP),
		LCD		("サブピクセルAA", "lcd",RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB),
		HRGB	("サブピク横方向RGB", "lcd_hrgb",RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB),
		HBGR	("サブピク横方向BGR", "lcd_hbgr",RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR),
		VRGB	("サブピク縦方向RGB", "lcd_vrgb",RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB),
		VBGR	("サブピク縦方向BGR", "lcd_vbgr",RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR);

		private String name;
		private String mode;
		private Object hint;

		private AAMode(String name, String mode, Object hint) {
			this.name = name;
			this.mode = mode;
			this.hint = hint;
		}

		@Override
		public String toString() {
			return name;
		}

		public String getId() {
			return(super.toString());
		}

		public String getMode() {
			return mode;
		}

		public Object getHint() {
			return hint;
		}

		public static AAMode get(String id) {
			for ( AAMode aam : AAMode.values() ) {
				if ( aam.getId().equals(id) ) {
					return aam;
				}
			}
			return null;
		}
	}

	// 自動アップデートの間隔
	public static enum UpdateOn {
		DISABLE ("無効にする", -1),
		EVERYDAY ("毎日実行する", 0),
		EVERY1DAY ("１日おきに実行する", 1),
		EVERY2DAY ("２日おきに実行する", 2),
		EVERY3DAY ("３日おきに実行する", 3),
		EVERY4DAY ("４日おきに実行する", 4),
		EVERY5DAY ("５日おきに実行する", 5),
		EVERY6DAY ("６日おきに実行する", 6);

		private String name;
		private int interval;

		private UpdateOn(String name, int interval) {
			this.name = name;
			this.interval = interval;
		}

		@Override
		public String toString() {
			return this.name;
		}

		public String getId() {
			return(super.toString());
		}

		public int getInterval() {
			return this.interval;
		}

		public static UpdateOn get(String id) {
			for ( UpdateOn uo : UpdateOn.values() ) {
				if ( uo.getId().equals(id) ) {
					return uo;
				}
			}
			return null;
		}
	}

	/*******************************************************************************
	 * 隠し
	 ******************************************************************************/

	private static boolean firstinstance = true;	// 起動後最初の呼び出しか（２インスタンス目以降は実行しない）

	// CHソートを有効にするかどうか
	public boolean getChSortEnabled() { return chSortEnabled; }
	public void setChSortEnabled(boolean b) { chSortEnabled = b; }
	private boolean chSortEnabled = true;


	/*******************************************************************************
	 * メンバ
	 ******************************************************************************/

	/*
	 * リスト形式関連の設定
	 */

	// 番組追跡であいまい検索をしない
	public boolean getDisableFazzySearch() { return disableFazzySearch; }
	public void setDisableFazzySearch(boolean b) { disableFazzySearch = b; }
	private boolean disableFazzySearch = false;
	// あいまい検索で逆引きはしない
	public boolean getDisableFazzySearchReverse() { return disableFazzySearchReverse; }
	public void setDisableFazzySearchReverse(boolean b) { disableFazzySearchReverse = b; }
	private boolean disableFazzySearchReverse = false;
	// あいまい検索の閾値のデフォルト値
	public int getDefaultFazzyThreshold() { return defaultFazzyThreshold; }
	public void setDefaultFazzyThreshold(int c) { defaultFazzyThreshold = c; }
	private int defaultFazzyThreshold = TraceKey.defaultFazzyThreshold;
	// しょぼかるの検索結果も有効な放送局のみに絞る
	public boolean getSyoboFilterByCenters() { return syoboFilterByCenters; }
	public void setSyoboFilterByCenters(boolean b) { syoboFilterByCenters = b; }
	private boolean syoboFilterByCenters = true;
	// 終了済みエントリも表示する
	public boolean getDisplayPassedEntry() { return displayPassedEntry; }
	public void setDisplayPassedEntry(boolean b) { displayPassedEntry = b; }
	private boolean displayPassedEntry = true;
	// ピックアップマークの表示
	public boolean getShowRsvPickup() { return showRsvPickup; }
	public void setShowRsvPickup(boolean b) { showRsvPickup = b; }
	private boolean showRsvPickup = true;
	// 時間重複マークの表示
	public boolean getShowRsvDup() { return showRsvDup; }
	public void setShowRsvDup(boolean b) { showRsvDup = b; }
	private boolean showRsvDup = true;
	// 裏番組予約マークの表示
	public boolean getShowRsvUra() { return showRsvUra; }
	public void setShowRsvUra(boolean b) { showRsvUra = b; }
	private boolean showRsvUra = false;
	// 予約行の背景色を変えて強調する
	public boolean getRsvdLineEnhance() { return rsvdLineEnhance; }
	public void setRsvdLineEnhance(boolean b) { rsvdLineEnhance = b; }
	private boolean rsvdLineEnhance = true;
	// 予約行の背景色
	public Color getRsvdLineColor() { return rsvdLineColor; }
	public void setRsvdLineColor(Color c) { rsvdLineColor = c; }
	private Color rsvdLineColor = RSVDLINE_COLOR;
	// ピックアップ行の背景色
	public Color getPickedLineColor() { return pickedLineColor; }
	public void setPickedLineColor(Color c) { pickedLineColor = c; }
	private Color pickedLineColor = PICKEDLINE_COLOR;
	// 現在放送中行の背景色を変えて強調する
	public boolean getCurrentLineEnhance() { return currentLineEnhance; }
	public void setCurrentLineEnhance(boolean b) { currentLineEnhance = b; }
	private boolean currentLineEnhance = true;
	// 現在放送中行の背景色
	public Color getCurrentLineColor() { return currentLineColor; }
	public void setCurrentLineColor(Color c) { currentLineColor = c; }
	private Color currentLineColor = CURRENTLINE_COLOR;
	// 現在放送中ノードに終了後何分までの番組を表示するか
	public int getCurrentAfter() { return currentAfter; }
	public void setCurrentAfter(int n) { currentAfter = n; }
	private int currentAfter = 10*60;
	// 現在放送中ノードに開始前何分までの番組を表示するか
	public int getCurrentBefore() { return currentBefore; }
	public void setCurrentBefore(int n) { currentBefore = n; }
	private int currentBefore = 60*60;
	// キーワードにマッチした箇所の強調色
	public Color getMatchedKeywordColor() { return matchedKeywordColor; }
	public void setMatchedKeywordColor(Color c) { matchedKeywordColor = c; }
	private Color matchedKeywordColor = MATCHEDKEYWORD_COLOR;
	// 削除時に確認ダイアログを表示する
	public boolean getShowWarnDialog() { return showWarnDialog; }
	public void setShowWarnDialog(boolean b) { showWarnDialog = b; }
	private boolean showWarnDialog = false;
	// マーク表示を個別欄に分離する
	public boolean getSplitMarkAndTitle() { return splitMarkAndTitle; }
	public void setSplitMarkAndTitle(boolean b) { splitMarkAndTitle = b; }
	private boolean splitMarkAndTitle = true;
	// 番組詳細も表示する
	public boolean getShowDetailOnList() { return showDetailOnList; }
	public void setShowDetailOnList(boolean b) { showDetailOnList = b; }
	private boolean showDetailOnList = true;
	// 予約待機の実予約番組自動選択数
	public int getRsvTargets() { return rsvTargets; }
	public void setRsvTargets(int c) { rsvTargets = c; }
	private int rsvTargets = 32;
	// 行ヘッダを表示する
	public boolean getRowHeaderVisible() { return rowHeaderVisible; }
	public void setRowHeaderVisible(boolean b) { rowHeaderVisible = b; }
	private boolean rowHeaderVisible = false;
	// ダブルクリック時の動作
	public DblClkCmd getDblClkCmd() { return dblClkCmd; }
	public void setDblClkCmd(DblClkCmd c) { dblClkCmd = c; }
	private DblClkCmd dblClkCmd = DblClkCmd.SHOWRSVDIALOG;

	// 過去ログ検索件数の上限
	public int getSearchResultMax() { return searchResultMax; }
	public void setSearchResultMax(int c) { searchResultMax = c; }
	private int searchResultMax = 50;

	// 過去ログ検索履歴の上限
	public int getSearchResultBufferMax() { return searchResultBufferMax; }
	public void setSearchResultBufferMax(int c) { searchResultBufferMax = c; }
	private int searchResultBufferMax = 5;

	/*
	 * 新聞形式関連の設定
	 */

	// 描画速度優先
	public boolean getDrawcacheEnable() { return drawcacheEnable; }
	public void setDrawcacheEnable(boolean b) { drawcacheEnable = b; }
	private boolean drawcacheEnable = false;
	// 新聞形式での、１ページあたりの放送局数
	public int getCenterPerPage() { return centerPerPage; }
	public void setCenterPerPage(int c) { centerPerPage = c; }
	public boolean isPagerEnabled() { return ( ! drawcacheEnable) && (centerPerPage > 0); }

	// 新聞形式での、過去日の放送局別表示の１ページあたりの日数
	public int getDatePerPassedPage(){ return datePerPassedPage; }
	public void setDatePerPassedPage(int n) { datePerPassedPage = n; }
	private int datePerPassedPage = 14;

	/**
	 * indexなので、０から始まる
	 */
	public int getPageIndex(int n) {
		if (pageBreakEnabled && pageBreaks.size() > 0){
			for (int pno=0; pno<pageBreaks.size(); pno++){
				if (n < pageBreaks.get(pno)){
					return pno;
				}
			}

			return pageBreaks.size();
		}

		int rem = n % centerPerPage;
		return (n-rem)/centerPerPage;
	}
	/*
	 * ページ内でのオフセットを取得する
	 */
	public int getOffsetInPage(int n){
		if (pageBreakEnabled && pageBreaks.size() > 0){
			int cnum = 0;
			for (int pno=0; pno<pageBreaks.size(); pno++){
				if (n < pageBreaks.get(pno)){
					return n - cnum;
				}

				cnum = pageBreaks.get(pno);
			}

			return n - cnum;
		}

		return n - getPageIndex(n)*centerPerPage;
	}
	/*
	 * ページのオフセットを取得する
	 */
	public int getPageOffset(int pnoIn){
		if (pageBreakEnabled && pageBreaks.size() > 0){
			int cnum = 0;
			for (int pno=0; pno<pageBreaks.size(); pno++){
				if (pnoIn == pno)
					return cnum;

				cnum = pageBreaks.get(pno);
			}

			return cnum;
		}

		return pnoIn*centerPerPage;
	}
	/*
	 * ページ内の番組数を取得する
	 */
	public int getCentersInPage(int pnoIn){
		if (pageBreakEnabled && pageBreaks.size() > 0){
			int cnum = 0;

			for (int pno=0; pno<pageBreaks.size(); pno++){
				if (pnoIn == pno)
					return pageBreaks.get(pno)-cnum;

				cnum = pageBreaks.get(pno);
			}

			return 99;
		}

		return centerPerPage;
	}

	private int centerPerPage = PAGER_DEFAULT;

	// 改ページ位置の配列
	public ArrayList<Integer> getPageBreaks(){ return pageBreaks; }
	public void setPageBreaks(ArrayList<Integer> breaks){ pageBreaks = breaks; }
	private ArrayList<Integer> pageBreaks = new ArrayList<Integer>();
	private boolean pageBreakEnabled = true;
	public void setPageBreakEnabled(boolean b){ pageBreakEnabled = b; }

	// 全ページを連続スナップショット
	public boolean getAllPageSnapshot() { return allPageSnapshot; }
	public void setAllPageSnapshot(boolean b) { allPageSnapshot = b; }
	private boolean allPageSnapshot = false;
	//　番組表のツールチップを表示する
	public boolean getTooltipEnable() { return tooltipEnable; }
	public void setTooltipEnable(boolean b) { tooltipEnable = b; }
	private boolean tooltipEnable = false;
	// ツールチップの表示遅延（100ミリ秒）
	public int getTooltipInitialDelay() { return tooltipInitialDelay; }
	public void setTooltipInitialDelay(int c) { tooltipInitialDelay = c; }
	private int tooltipInitialDelay = 3;
	// ツールチップの消去遅延（100ミリ秒）
	public int getTooltipDismissDelay() { return tooltipDismissDelay; }
	public void setTooltipDismissDelay(int c) { tooltipDismissDelay = c; }
	private int tooltipDismissDelay = 30*10;
	// タイムバーにあわせてスクロール
	public boolean getTimerbarScrollEnable() { return timerbarScrollEnable; }
	public void setTimerbarScrollEnable(boolean b) { timerbarScrollEnable = b; }
	private boolean timerbarScrollEnable = true;
	// 過去ログの日付ノードの表示数
	public int getPassedLogLimit() { return passedLogLimit; }
	public void setPassedLogLimit(int c) { passedLogLimit = c; }
	private int passedLogLimit = 31;
	// 局ごとの幅（->bounds)
	// 番組枠の高さ（->bounds)
	// タイムバーの色設定
	public Color getTimebarColor() { return timebarColor; }
	public void setTimebarColor(Color c) { timebarColor = c; }
	private Color timebarColor = TIMEBAR_COLOR_06_12;
	public Color getTimebarColor2() { return timebarColor2; }
	public void setTimebarColor2(Color c) { timebarColor2 = c; }
	private Color timebarColor2 = TIMEBAR_COLOR_12_18;
	public Color getTimebarColor3() { return timebarColor3; }
	public void setTimebarColor3(Color c) { timebarColor3 = c; }
	private Color timebarColor3 = TIMEBAR_COLOR_18_24;
	public Color getTimebarColor4() { return timebarColor4; }
	public void setTimebarColor4(Color c) { timebarColor4 = c; }
	private Color timebarColor4 = TIMEBAR_COLOR_24_06;

	// 予約タイムバーの色設定
	public Color getResTimebarColor1() { return restimebarColor1; }
	public void setResTimebarColor1(Color c) { restimebarColor1 = c; }
	private Color restimebarColor1 = RESTIMEBAR_COLOR_1;

	public Color getResTimebarColor2() { return restimebarColor2; }
	public void setResTimebarColor2(Color c) { restimebarColor2 = c; }
	private Color restimebarColor2 = RESTIMEBAR_COLOR_2;

	public Color getResTimebarColor3() { return restimebarColor3; }
	public void setResTimebarColor3(Color c) { restimebarColor3 = c; }
	private Color restimebarColor3 = RESTIMEBAR_COLOR_3;

	public Color getResTimebarColor4() { return restimebarColor4; }
	public void setResTimebarColor4(Color c) { restimebarColor4 = c; }
	private Color restimebarColor4 = RESTIMEBAR_COLOR_4;

	// 予約タイムバーの幅設定
	public int getResTimebarWidth() { return restimebarWidth; }
	public void setResTimebarWidth(int n) { restimebarWidth = n; }
	private int restimebarWidth = RESTIMEBAR_WIDTH;

	// 予約枠の色設定
	public Color getExecOnFontColor() { return execOnFontColor; }
	public void setExecOnFontColor(Color c) { execOnFontColor = c; }
	private Color execOnFontColor = EXECON_TUNE_COLOR;
	// 無効予約のフォント色
	public Color getExecOffFontColor() { return execOffFontColor; }
	public void setExecOffFontColor(Color c) { execOffFontColor = c; }
	private Color execOffFontColor = EXECOFF_TUNE_COLOR;
	// ピックアップ枠の色設定
	public Color getPickedColor() { return pickedColor; }
	public void setPickedColor(Color c) { pickedColor = c; }
	private Color pickedColor = PICKUP_BORDER_COLOR;
	public Color getPickedFontColor() { return pickedFontColor; }
	public void setPickedFontColor(Color c) { pickedFontColor = c; }
	private Color pickedFontColor = PICKUP_TUNER_COLOR;
	// ジャンル別色設定（ここでは設定しない。PaperColorMapクラスで）
	@Deprecated
	public String getPaperColors() { return paperColors; }
	@Deprecated
	public void setPaperColors(String s) { paperColors = s; }
	@Deprecated
	private String paperColors = "";
	// マウスオーバー時のハイライト色設定
	public boolean getEnableHighlight() { return enableHighlight; }
	public void setEnableHighlight(boolean b) { enableHighlight = b; }
	private boolean enableHighlight = true;
	public Color getHighlightColor() { return highlightColor; }
	public void setHighlightColor(Color c) { highlightColor = c; }
	private Color highlightColor = MOUSEOVER_COLOR;
	// 番組枠の設定（titleFontStyleの初期化はコンストラクタで）
	public boolean getShowStart() { return showStart; }
	public void setShowStart(boolean b) { showStart = b; }
	private boolean showStart = true;
	public String getTitleFont() { return titleFont; }
	public void setTitleFont(String s) { titleFont = s; }
	private String titleFont = "";
	public int getTitleFontSize() { return titleFontSize; }
	public void setTitleFontSize(int n) { titleFontSize = n; }
	private int titleFontSize = 13;
	public Color getTitleFontColor() { return titleFontColor; }
	public void setTitleFontColor(Color c) { titleFontColor = c; }
	private Color titleFontColor = TITLE_COLOR;
	public ArrayList<JTXTButton.FontStyle> getTitleFontStyle() { return titleFontStyle; }
	public void setTitleFontStyle(ArrayList<JTXTButton.FontStyle> a) { titleFontStyle = a; }
	private ArrayList<JTXTButton.FontStyle> titleFontStyle = new ArrayList<JTXTButton.FontStyle>();
	public boolean getShowDetail() { return showDetail; }
	public void setShowDetail(boolean b) { showDetail = b; }
	private boolean showDetail = true;
	public int getDetailRows() { return detailRows; }
	public void setDetailRows(int n) { detailRows = n; }
	private int detailRows = 50;
	public String getDetailFont() { return detailFont; }
	public void setDetailFont(String s) { detailFont = s; }
	private String detailFont = "";
	public int getDetailFontSize() { return detailFontSize; }
	public void setDetailFontSize(int n) { detailFontSize = n; }
	private int detailFontSize = 11;
	public Color getDetailFontColor() { return detailFontColor; }
	public void setDetailFontColor(Color c) { detailFontColor = c; }
	private Color detailFontColor = DETAIL_COLOR;
	public ArrayList<JTXTButton.FontStyle> getDetailFontStyle() { return detailFontStyle; }
	public void setDetailFontStyle(ArrayList<JTXTButton.FontStyle> a) { detailFontStyle = a; }
	private ArrayList<JTXTButton.FontStyle> detailFontStyle = new ArrayList<JTXTButton.FontStyle>();
	public int getDetailTab() { return detailTab; }
	public void setDetailTab(int n) { detailTab = n; }
	private int detailTab = 8;

	// 番組詳細欄にマークも表示するかどうか
	public boolean getShowMarkOnDetailArea() { return showMarkOnDetailArea; }
	public void setShowMarkOnDetailArea(boolean b) { showMarkOnDetailArea = b; }
	private boolean showMarkOnDetailArea = true;

	public Color getMatchedBorderColor() { return matchedBorderColor; }
	public void setMatchedBorderColor(Color c) { matchedBorderColor = c; }
	private Color matchedBorderColor = MATCHBORDER_COLOR;
	public Color getMatchedKeywordBorderColor() { return matchedKeywordBorderColor; }
	public void setMatchedKeywordBorderColor(Color c) { matchedKeywordBorderColor = c; }
	private Color matchedKeywordBorderColor = MATCHBORDER_COLOR;
	public int getMatchedBorderThickness() { return matchedBorderThickness; }
	public void setMatchedBorderThickness(int n) { matchedBorderThickness = n; }
	private int matchedBorderThickness = 6;
	// AAモード
	public AAMode getPaperAAMode() { return paperAAMode; }
	public void setPaperAAMode(AAMode a) { paperAAMode = a; }
	private AAMode paperAAMode = AAMode.ON;
	// 新聞形式でもレコーダーコンボボックスを有効にする
	public boolean getEffectComboToPaper() { return effectComboToPaper; }
	public void setEffectComboToPaper(boolean b) { effectComboToPaper = b; }
	private boolean effectComboToPaper = true;
	// スナップショットのフォーマット
	public SnapshotFmt getSnapshotFmt() { return snapshotFmt; }
	public void setSnapshotFmt(SnapshotFmt s) { snapshotFmt = s; }
	private SnapshotFmt snapshotFmt = SnapshotFmt.JPG;
	// スナップショットを印刷する
	public boolean getPrintSnapshot() { return printSnapshot; }
	public void setPrintSnapshot(boolean b) { printSnapshot = b; }
	private boolean printSnapshot = false;
	// 土曜日、日曜日の色を変える
	public boolean getWeekEndColoring(){ return weekEndColoring; }
	public void setWeekEndColoring(boolean b){ weekEndColoring = b; }
	private boolean weekEndColoring = true;

	// タイムラインのラベル表示モード(0:非表示, 1:分のみ, 2:時分(0-23), 3:時分(5-28))
	public int getTimelineLabelDispMode(){ return timelineLabelDispMode; }
	public void setTimelineLabelDispMode(int n){ timelineLabelDispMode = n; }
	private int timelineLabelDispMode = 2;

	// マウスホイール時に同一時間帯でのページ切替を行うマウスボタン
	public int getMouseButtonForPageSwitch(){ return mouseButtonForPageSwitch; }
	public void setMouseButtonForPageSwitch(int n){ mouseButtonForPageSwitch = n; }
	private int mouseButtonForPageSwitch = 5;

	// マウスホイール時に同一時間帯での日付/放送局切替を行うマウスボタン
	public int getMouseButtonForNodeSwitch(){ return mouseButtonForNodeSwitch; }
	public void setMouseButtonForNodeSwitch(int n){ mouseButtonForNodeSwitch = n; }
	private int mouseButtonForNodeSwitch = 4;

	// マウスホイール時に番組表上端・下端での日付/放送局の切替を行うマウスボタン
	public int getMouseButtonForNodeAutoPaging(){ return mouseButtonForNodeAutoPaging; }
	public void setMouseButtonForNodeAutoPaging(int n){ mouseButtonForNodeAutoPaging = n; }
	private int mouseButtonForNodeAutoPaging = 1;


	/*
	 * リスト・新聞形式共通
	 */

	// 実行ONのエントリのみ予約マークを表示する
	public boolean getDisplayOnlyExecOnEntry() { return displayOnlyExecOnEntry; }
	public void setDisplayOnlyExecOnEntry(boolean b) { displayOnlyExecOnEntry = b; }
	private boolean displayOnlyExecOnEntry = false;
	// 終了済み予約も表示する
	public boolean getDisplayPassedReserve() { return displayPassedReserve; }
	public void setDisplayPassedReserve(boolean b) { displayPassedReserve = b; }
	private boolean displayPassedReserve = false;
	// リピート放送を表示しない
	public boolean getShowOnlyNonrepeated() { return showOnlyNonrepeated; }
	public void setShowOnlyNonrepeated(boolean b) { showOnlyNonrepeated = b; }
	private boolean showOnlyNonrepeated = true;
	// 深夜(24:00-28:59)の帯予約を一日前にずらす
	public boolean getAdjLateNight() { return adjLateNight; }
	public void setAdjLateNight(boolean b) { adjLateNight = b; }
	private boolean adjLateNight = false;
	// ツリーペーンにrootノードを表示させる
	public boolean getRootNodeVisible() { return rootNodeVisible; }
	public void setRootNodeVisible(boolean b) { rootNodeVisible = b; }
	private boolean rootNodeVisible = false;
	// ツリーペーンの幅を同期させる
	public boolean getSyncTreeWidth() { return syncTreeWidth; }
	public void setSyncTreeWidth(boolean b) { syncTreeWidth = b; }
	private boolean syncTreeWidth = true;
	// ★延長警告★を短縮表示
	public boolean getShortExtMark() { return shortExtMark; }
	public void setShortExtMark(boolean b) { shortExtMark = b; }
	private boolean shortExtMark = false;

	// 表示マークの選択
	public HashMap<TVProgram.ProgOption,Boolean> getOptMarks() { return optMarks; }
	public void setOptMarks(HashMap<TVProgram.ProgOption,Boolean> h) { optMarks = h;	}
	private HashMap<TVProgram.ProgOption,Boolean> optMarks = new HashMap<TVProgram.ProgOption, Boolean>();

	// 表示マークの選択（後付け）
	public HashMap<TVProgram.ProgOption,Boolean> getOptPostfixMarks() { return optPostfixMarks; }
	public void setOptPostfixMarks(HashMap<TVProgram.ProgOption,Boolean> h) { optPostfixMarks = h;	}
	private HashMap<TVProgram.ProgOption,Boolean> optPostfixMarks = new HashMap<TVProgram.ProgOption, Boolean>();

	// クリップボードアイテムの選択（->clipboardItem）
	// 右クリックメニューの実行アイテム
	public ArrayList<TextValueSet> getTvCommand() { return tvCommand; }
	public void setTvCommand(ArrayList<TextValueSet> al) { tvCommand = al; }
	private ArrayList<TextValueSet> tvCommand = new ArrayList<TextValueSet>();

	/*
	 * Web番組表対応
	 */

	// 29時をまたいで同タイトルが続いている場合は同一番組とみなす
	public boolean getContinueTomorrow() { return continueTomorrow; }
	public void setContinueTomorrow(boolean b) { continueTomorrow = b; }
	private boolean continueTomorrow = true;
	// Web番組表のキャッシュ保持時間
	public int getCacheTimeLimit() { return cacheTimeLimit; }
	public void setCacheTimeLimit(int w) { cacheTimeLimit = w; }

	// 高速キャッシュファイルを使用するか
	public boolean getUseProgCache(){ return useProgCache; }
	public void setUseProgCache(boolean b){ useProgCache = b;}
	private boolean useProgCache = true;

	public boolean isShutdownEnabled() { return (cacheTimeLimit == 0)&&(shutdownCmd!=null&&shutdownCmd.length()>0); }
	private int cacheTimeLimit = 12;
	// シャットダウンコマンド
	public String getShutdownCmd() { return shutdownCmd; }
	public void setShutdownCmd(String s) { shutdownCmd = s; }
	private String shutdownCmd = "";
	// 可能なら番組表８日分を取得する
	public boolean getExpandTo8() { return expandTo8; }
	public void setExpandTo8(boolean b) { expandTo8 = b; dogdays = (b)?(8):(7); }
	private boolean expandTo8 = false;
	public int getDogDays() { return dogdays; }
	private int dogdays = 7;
	// 番組詳細取得を行なうかどうか（廃止）
	@Deprecated
	public boolean getUseDetailCache() { return false; }
	@Deprecated
	public void setUseDetailCache(boolean b) { useDetailCache = false; }
	@Deprecated
	private boolean useDetailCache = false;
	/**
	 *  予約ダイアログを開いたときに自動で番組IDを取得する
	 */
	public boolean getAutoEventIdComplete() { return autoEventIdComplete; }
	public void setAutoEventIdComplete(boolean b) { autoEventIdComplete = b; }
	private boolean autoEventIdComplete = false;
	// タイトルに話数が含まれる場合に以降を分離する
	public boolean getSplitEpno() { return splitEpno; }
	public void setSplitEpno(boolean b) { splitEpno = b; }
	private boolean splitEpno = false;
	// サブタイトルを番組追跡の対象から除外する
	public boolean getTraceOnlyTitle() { return traceOnlyTitle; }
	public void setTraceOnlyTitle(boolean b) { traceOnlyTitle = b; }
	private boolean traceOnlyTitle = true;
	// タイトルを整形する
	public boolean getFixTitle() { return fixTitle; }
	public void setFixTitle(boolean b) { fixTitle = b; }
	private boolean fixTitle = true;
	// NGワード
	public ArrayList<String> getNgword() { return ngword; }
	public void setNgword(ArrayList<String> ngw) { ngword = ngw; }
	public void setNgword(String ngw) {
		ngword = new ArrayList<String>();
		for (String ngs : ngw.split(";")) {
			if (ngs.trim().length() > 0) {
				ngword.add(ngs.trim());
			}
		}
	}
	private ArrayList<String> ngword = new ArrayList<String>();
	// Web番組表をアクセスする際のUser-Agentの値
	public String getUserAgent() { return userAgent; }
	public void setUserAgent(String s) { userAgent = s; }
	private String userAgent = USER_AGENT;
	// HTTP Proxy
	public boolean getUseProxy() { return useProxy; }
	public void setUseProxy(boolean b) { useProxy = b; }
	public String getProxyAddr() { return proxyAddr; }
	public void setProxyAddr(String s) { proxyAddr = s; }
	public String getProxyPort() { return proxyPort; }
	public void setProxyPort(String s) { proxyPort = s; }
	private boolean useProxy = false;
	private String proxyAddr = "";
	private String proxyPort = "";
	// しょぼかるを利用する
	public boolean getUseSyobocal() { return useSyobocal; }
	public void setUseSyobocal(boolean b) { useSyobocal = b; }
	private boolean useSyobocal = true;
	// 日に一回しか新着履歴を更新しない
	public boolean getHistoryOnlyUpdateOnce() { return historyOnlyUpdateOnce; }
	public void setHistoryOnlyUpdateOnce(boolean b) { historyOnlyUpdateOnce = b; }
	private boolean historyOnlyUpdateOnce = true;
	// 過去ログを利用する
	public boolean getUsePassedProgram() { return usePassedProgram; }
	public void setUsePassedProgram(boolean b) { usePassedProgram = b; }
	private boolean usePassedProgram = true;
	// 何日先までのログを過去ログ用に準備しておくか
	public int getPrepPassedProgramCount() { return prepPassedProgramCount; }
	public void setPrepPassedProgramCount(int w) { prepPassedProgramCount = w; }
	private int prepPassedProgramCount = 4;

	// 決まった時間に番組表を取得する
	public boolean getDownloadProgramOnFixedTime() { return downloadProgramOnFixedTime; }
	public void setDownloadProgramOnFixedTime(boolean b) { downloadProgramOnFixedTime = b; }
	private boolean downloadProgramOnFixedTime = false;

	// 番組表を取得する時刻の一覧
	public String getDownloadProgramTimeList() { return downloadProgramTimeList; }
	public void setDownloadProgramTimeList(String s) { downloadProgramTimeList = s; }
	private String downloadProgramTimeList = "08:30";

	// 番組表をバックグランドで取得するか
	public boolean getDownloadProgramInBackground() { return downloadProgramInBackground; }
	public void setDownloadProgramInBackground(boolean b) { downloadProgramInBackground = b; }
	private boolean downloadProgramInBackground = false;

	// 最後に番組表を取得した時刻
	public String getLastDownloadTime(){ return lastDownloadTime; }
	public void setLastDownloadTime(String s){ lastDownloadTime = s; }
	private String lastDownloadTime = null;

	/*
	 * レコーダ対応
	 */

	// 常に予約詳細情報を取得する
	@Deprecated
	public boolean getForceGetRdReserveDetails() { return forceGetRdReserveDetails; }
	@Deprecated
	public void setForceGetRdReserveDetails(boolean b) { forceGetRdReserveDetails = b; }
	@Deprecated
	private boolean forceGetRdReserveDetails = false;
	// 常に予約詳細情報を取得しない
	@Deprecated
	public boolean getNeverGetRdReserveDetails() { return neverGetRdReserveDetails; }
	@Deprecated
	public void setNeverGetRdReserveDetails(boolean b) { neverGetRdReserveDetails = b; }
	@Deprecated
	private boolean neverGetRdReserveDetails = false;
	// 予約取得ボタンで録画結果も同時に取得する
	@Deprecated
	public boolean getSkipGetRdRecorded() { return skipGetRdRecorded; }
	@Deprecated
	public void setSkipGetRdRecorded(boolean b) { skipGetRdRecorded = b; }
	@Deprecated
	private boolean skipGetRdRecorded = false;

	// 予約一覧取得時に番組詳細も取得する
	public int getForceLoadReserveDetails() { return forceLoadReserveDetails; }
	public void setForceLoadReserveDetails(int n) { forceLoadReserveDetails = n; }
	private int forceLoadReserveDetails = 0;
	// 予約一覧取得時に自動予約一覧も取得する
	public int getForceLoadAutoReserves() { return forceLoadAutoReserves; }
	public void setForceLoadAutoReserves(int n) { forceLoadAutoReserves = n; }
	private int forceLoadAutoReserves = 0;
	// 予約一覧取得時に録画結果一覧も取得する
	public int getForceLoadRecorded() { return forceLoadRecorded; }
	public void setForceLoadRecorded(int n) { forceLoadRecorded = n; }
	private int forceLoadRecorded = 0;

	// 録画結果一覧の保存期間
	public void setRecordedSaveScope(int n) { recordedSaveScope = n; }
	public int getRecordedSaveScope() { return recordedSaveScope; }
	private int recordedSaveScope = 90;


	//
	@Deprecated
	public boolean getDontCalendarWorkWithNull() { return dontCalendarWorkWithNull; }
	@Deprecated
	public void setDontCalendarWorkWithNull(boolean b) { dontCalendarWorkWithNull = b; }
	@Deprecated
	boolean dontCalendarWorkWithNull = true;

	/*
	 * 予約
	 */

	// スポーツ延長の長さ（分）
	public String getSpoexLength() { return spoexLength; }
	public void setSpoexLength(String l) { spoexLength = l; }
	private String spoexLength = "90";
	// 前倒し
	public boolean getOverlapUp() { return overlapUp; }
	public void setOverlapUp(boolean b) { overlapUp = b; }
	private boolean overlapUp = false;
	// 先送り
	public boolean getOverlapDown() { return overlapDown; }
	public void setOverlapDown(boolean b) { overlapDown = b; }
	private boolean overlapDown = false;
	// ケツ短縮
	public boolean getOverlapDown2() { return overlapDown2; }
	public void setOverlapDown2(boolean b) { overlapDown2 = b; }
	private boolean overlapDown2 = false;
	// OVAとか縮めんといて
	public boolean getNoOverlapDown2Sp() { return noOverlapDown2Sp; }
	public void setNoOverlapDown2Sp(boolean b) { noOverlapDown2Sp = b; }
	private boolean noOverlapDown2Sp = true;
	// 自動フォルダ選択
	public boolean getAutoFolderSelect() { return autoFolderSelect; }
	public void setAutoFolderSelect(boolean b) { autoFolderSelect = b; }
	private boolean autoFolderSelect = true;
	// ジャンルではなく放送局でＡＶ設定をする
	public boolean getEnableCHAVsetting() { return enableCHAVsetting; }
	public void setEnableCHAVsetting(boolean b) { enableCHAVsetting = b; }
	private boolean enableCHAVsetting = false;
	// 類似予約の検索範囲
	public int getRangeLikeRsv() { return rangeLikeRsv; }
	public void setRangeLikeRsv(int c) { rangeLikeRsv = c; }
	private int rangeLikeRsv = 0;
	// 類似予約の情報を引き継ぐ
	public boolean getGivePriorityToReserved() { return givePriorityToReserved; }
	public void setGivePriorityToReserved(boolean b) { givePriorityToReserved = b; }
	private boolean givePriorityToReserved = true;
	// 類似予約の予約名を優先する
	public boolean getGivePriorityToReservedTitle() { return givePriorityToReservedTitle; }
	public void setGivePriorityToReservedTitle(boolean b) { givePriorityToReservedTitle = b; }
	private boolean givePriorityToReservedTitle = true;
	// 隣接する番組を重複扱いしない
	public boolean getAdjoiningNotRepetition() { return adjoiningNotRepetition; }
	public void setAdjoiningNotRepetition(boolean b) { adjoiningNotRepetition = b; }
	private boolean adjoiningNotRepetition = false;
	// 予約一覧で繰り返し予約を展開する
	public boolean getShowAllIterationItem() { return showAllIterationItem; }
	public void setShowAllIterationItem(boolean b) { showAllIterationItem = b; }
	private boolean showAllIterationItem = true;
	// 繰り返し予約の２つ目以降の文字色
	public Color getIterationItemForeground() { return iterationItemForeground; }
	public void setIterationItemForeground(Color c) { iterationItemForeground = c; }
	private Color iterationItemForeground = ITERATIONITEM_COLOR;
	// チューナー不足警告の強調色
	public Color getTunerShortColor() { return tunerShortColor; }
	public void setTunerShortColor(Color c) { tunerShortColor = c; }
	private Color tunerShortColor = TUNERSHORT_COLOR;
	// 正常録画済み(と思われる)と思われる背景色
	public Color getRecordedColor() { return recordedColor; }
	public void setRecordedColor(Color c) { recordedColor = c; }
	private Color recordedColor = RECORDED_COLOR;
	// タイトル自動補完
	public boolean getUseAutocomplete() { return useAutocomplete; }
	public void setUseAutocomplete(boolean b) { useAutocomplete = b; }
	private boolean useAutocomplete = false;

	// 予約番組の開始を通知するか
	public boolean getNotifyBeforeProgStart(){ return notifyBeforeProgStart; }
	public void setNotifyBeforeProgStart(boolean b){ notifyBeforeProgStart = b; }
	private boolean notifyBeforeProgStart = false;

	// ピックアップ番組の開始を通知するか
	public boolean getNotifyBeforePickProgStart(){ return notifyBeforePickProgStart; }
	public void setNotifyBeforePickProgStart(boolean b){ notifyBeforePickProgStart = b; }
	private boolean notifyBeforePickProgStart = false;

	// 開始何分前に通知するか
	public int getMinsBeforeProgStart(){ return minsBeforeProgStart; }
	public void setMinsBeforeProgStart(int n){ minsBeforeProgStart = n; }
	private int minsBeforeProgStart = 5;

	/*
	 * タイトル一覧関係
	 */
	// タイトル詳細を表示するか
	public boolean getShowTitleDetail(){ return showTitleDetail; }
	public void setShowTitleDetail(boolean b){ showTitleDetail = b; }
	private boolean showTitleDetail = true;

	/*
	 * その他の設定
	 */

	// 起動時にアップデートを確認する
	public boolean getCheckUpdate() { return checkUpdate; }
	public void setCheckUpdate(boolean b) { checkUpdate= b; }
	private boolean checkUpdate = true;
	public UpdateOn getUpdateMethod() { return updateMethod; }
	public void setUpdateMethod(UpdateOn u) { updateMethod = u; }
	private UpdateOn updateMethod = UpdateOn.EVERY2DAY;
	// beep音無効
	public boolean getDisableBeep() { return disableBeep; }
	public void setDisableBeep(boolean b) { disableBeep = b; }
	private boolean disableBeep = false;
	// システムトレイを表示する
	public boolean getShowSysTray() { return showSysTray; }
	public void setShowSysTray(boolean b) { showSysTray = b; }
	private boolean showSysTray = false;
	// [X]ボタンでシステムトレイに隠す
	public boolean getHideToTray() { return hideToTray; }
	public void setHideToTray(boolean b) { hideToTray = b; }
	private boolean hideToTray = false;
	// 多重起動禁止
	public boolean getOnlyOneInstance() { return onlyOneInstance; }
	public void setOnlyOneInstance(boolean b) { onlyOneInstance = b; }
	private boolean onlyOneInstance = false;
	// LookAndFeel
	public String getLookAndFeel() { return lookAndFeel; }
	public void setLookAndFeel(String n) { lookAndFeel = n; }
	private String lookAndFeel = "";
	// 表示フォント
	public String getFontName() { return fontName; }
	public void setFontName(String f) { fontName = f; }
	private String fontName = "";
	public int getFontSize() { return fontSize; }
	public void setFontSize(int f) { fontSize = f; }
	private int fontSize = 12;

	public static int ZMSIZE(int size){ return TheEnv != null ? size*TheEnv.fontSize/12 : size; }

	public boolean getUseGTKRC() { return useGTKRC; }
	public void setUseGTKRC(boolean b) { useGTKRC = b; }
	private boolean useGTKRC = true;

	private int fontSizeAdjustGTK = 3;

	// 【Win】ファイルオープンにrundll32を使用する
	public boolean getUseRundll32() { return useRundll32; }
	public void setUseRundll32(boolean b) { useRundll32 = b; }
	private boolean useRundll32 = false;
	// 【注意】デバッグモード
	public boolean getDebug() { return debug; }
	public void setDebug(boolean b) { debug = b; }
	private boolean debug = false;

	// 内閣府の「国民の祝日」CSVを使用する
	public boolean getUseHolidayCSV(){ return useHolidayCSV; }
	public void setUseHolidayCSV(boolean b){ useHolidayCSV = b; }
	private boolean useHolidayCSV = true;

	// 内閣府の「国民の祝日」CSVのURL
	public String getHolidayFetchURL(){ return holidayFetchURL; }
	public void setHolidayFetchURL(String s){ holidayFetchURL = s; }
	private String holidayFetchURL = "https://www8.cao.go.jp/chosei/shukujitsu/syukujitsu.csv";

	// 内閣府の「国民の祝日」CSVの取得間隔（単位：日数）
	public int getHolidayFetchInterval(){ return holidayFetchInterval; }
	public void setHolidayFetchInterval(int n){ holidayFetchInterval = n; }
	private int holidayFetchInterval = 90;

	// 検索キーワードの最大数
	public int getMaxSearchWordNuml(){ return maxSearchWordNum; }
	public void setMaxSearchWordNum(int n){ maxSearchWordNum = n; }
	private int maxSearchWordNum = MAX_SEARCH_WORD_NUM;

	// しょぼかるの過去取得日数（タイトル一覧用）
	public int getSyobocalPastDays(){ return syobocalPastDays; }
	public void setSyobocalPastDays(int n){ syobocalPastDays = n; }
	private int syobocalPastDays = 0;

	/*******************************************************************************
	 * 作ったけど使ってないもの
	 ******************************************************************************/

	/*
	 * フォルダの設定
	 */

	// キャッシュ格納場所
	public String getProgDir() { return progDir; }
	public void setProgDir(String s) { progDir = s; }
	private String progDir = "progcache";
	// 過去ログ格納場所
	public String getPassedDir() { return passedDir; }
	public void setPassedDir(String s) { passedDir = s; }
	private String passedDir = "passed";


	/*******************************************************************************
	 * 設定項目ではないもの
	 ******************************************************************************/

	/*
	 * Web番組表関連の設定
	 */

	// Web番組表サイト(TV)
	public String getTVProgramSite() { return tvProgramSite; }
	public void setTVProgramSite(String s) { tvProgramSite = s; }
	private String tvProgramSite = TV_SITE;
	// Web番組表サイト(CS)
	public String getCSProgramSite() { return csProgramSite; }
	public void setCSProgramSite(String s) { csProgramSite = s; }
	private String csProgramSite = CS_SITE;
	// Web番組表サイト(CS2)
	public String getCS2ProgramSite() { return cs2ProgramSite; }
	public void setCS2ProgramSite(String s) { cs2ProgramSite = s; }
	private String cs2ProgramSite = CS2_SITE;
	// Web番組表サイト(Radio)
	@Deprecated
	public String getRadioProgramSite() { return radioProgramSite; }
	@Deprecated
	public void setRadioProgramSite(String s) { radioProgramSite = s; }
	@Deprecated
	private String radioProgramSite = RADIO_SITE;

	/*******************************************************************************
	 * もう使われていないアイテム
	 ******************************************************************************/

	// 番組表の表示を簡素化する（現在は使用していない）
	@Deprecated
	public boolean getLightProgramView() { return lightProgramView; }
	@Deprecated
	public void setLightProgramView(boolean b) { lightProgramView = b; }
	@Deprecated
	private boolean lightProgramView = false;

	// スポーツ延長の検索範囲
	@Deprecated
	public String getSpoexSearchStart() { return spoexSearchStart; }
	@Deprecated
	public void setSpoexSearchStart(String s) { spoexSearchStart = s; }
	@Deprecated
	public String getSpoexSearchEnd() { return spoexSearchEnd; }

	@Deprecated
	public void setSpoexSearchEnd(String s) { spoexSearchEnd = s; }
	@Deprecated
	private String spoexSearchStart = "00:00";
	@Deprecated
	private String spoexSearchEnd = "00:00";

	// 「最大延長」の記載のあるもののみスポーツ延長のトリガーにする
	@Deprecated
	public boolean getSpoexLimitation() { return spoexLimitation; }
	@Deprecated
	public void setSpoexLimitation(boolean b) { spoexLimitation = b; }
	@Deprecated
	private boolean spoexLimitation = false;


	/*******************************************************************************
	 * 雑多なメソッド
	 ******************************************************************************/

	// 各種ディレクトリの作成
	public void makeEnvDir() {
		String cause = "";
		try {
			File d = null;

			// 設定ファイルを格納するディレクトリの作成
			d = new File(cause = envDir);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}

			// プラグイン一覧ファイルを格納するディレクトリの作成
			d = new File(cause = metainfDir);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}
			d = new File(cause = services);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}

			// 番組表をキャッシュするディレクトリの作成
			d = new File(cause = progDir);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}

			// 過去ログ
			d = new File(cause = passedDir);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}

			// skin
			d = new File(cause = skinDir);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}

			/*
			// debug
			d = new File(cause = debugDir);
			if (d.exists() == false) {
				if (d.mkdir() == false) {
					// 何か致命的なエラー
				}
			}
			*/
		}
		catch (Exception e) {
			System.err.println(ERRID+"フォルダの作成に失敗 "+cause);
		}
	}

	// 保存する・読みだす
	public boolean save() {

		if ( FieldUtils.save(envText,this) ) {
    		// テキスト形式へ移行済み

			if ( useGTKRC ) {
				saveGTKRC();
			}
			else {
				File f = new File(GTKRC);
				if ( f.exists() ) {
					f.delete();
				}
			}

			return true;
		}

    	System.out.println(MSGID+"保存します: "+envFile);
    	if ( ! CommonUtils.writeXML(envFile, this) ) {
        	System.err.println(ERRID+"保存に失敗しました");
        	return false;
    	}
    	return true;
	}

	private boolean saveGTKRC() {
		StringBuilder sb = new StringBuilder();
		sb.append("style \"tainavistyle\" {\n");
		sb.append("font_name = \"");
		sb.append(this.getFontName());
		sb.append(" ");
		sb.append(String.valueOf(this.getFontSize()-fontSizeAdjustGTK));
		sb.append("\"\n");
		sb.append("}\n");
		sb.append("\n");
		sb.append("class \"GtkWidget\" style \"tainavistyle\"\n");
		return CommonUtils.write2file(GTKRC, sb.toString());
	}

	public boolean load() {

    	File ft = new File(envText);
    	if ( ft.exists() ) {
    		// テキスト形式へ移行済み
			System.out.println("@Deprecated: "+envFile);
    		return true;
    	}

    	System.out.println(MSGID+"読み込みます: "+envFile);
    	File fx = new File(envFile);
    	if ( fx.exists() ) {
    		Env b = (Env) CommonUtils.readXML(envFile);
    		if ( b != null ) {
    			FieldUtils.deepCopy(this, b);

				// テキスト形式がなければ作るよ
				if ( FieldUtils.save(envText,this) ) {
					fx.renameTo(new File(envFile+".bak"));
				}

    			return true;
    		}
    	}

    	System.err.println(ERRID+"環境設定が読み込めなかったのでデフォルトの設定値を利用します");
    	return false;
	}


	/**
	 * {@link Env#save()}から呼び出されるだけで公開はしない
	 */
	@Deprecated
	private boolean saveLAF() {
    	System.out.println(MSGID+"ルックアンドフィール設定を保存します: "+lafFile);
    	if ( ! CommonUtils.write2file(lafFile, String.format("%s\t%s\t%d",this.lookAndFeel,fontName,fontSize)) ) {
        	System.err.println(ERRID+"ルックアンドフィール設定の保存に失敗しました");
        	return false;
    	}
    	return true;
	}

	/**
	 * 移行用
	 * @return
	 */
	public boolean loadText() {

		boolean b = FieldUtils.load(envText,this);

		// 廃止するよ
		File f = new File(lafFile);
    	if ( f.exists() ) {
        	System.out.println(MSGID+"ルックアンドフィール設定ファイルは廃止されます: "+lafFile);
        	String s = CommonUtils.read4file(lafFile, true);
        	if ( s != null ) {
        		Matcher ma = Pattern.compile("^(.+?)\t(.+?)\t(\\d+?)$",Pattern.DOTALL).matcher(s);
        		if ( ma.find() ) {
        			this.lookAndFeel = ma.group(1);
        			this.fontName = ma.group(2);
        			this.fontSize = Integer.valueOf(ma.group(3));
        			System.out.println("+lookandfeel="+this.lookAndFeel+", fontname="+this.fontName+", fontsize="+this.fontSize);
        		}
        	}
			f.delete();
    	}

    	// CHAN-TORUを削除する
    	if (b && tvCommand != null){
    		for (TextValueSet tv : tvCommand) {
    			if (tv == null || tv.getValue() == null)
    				continue;

    			if (tv.getValue().contains("tv.so-net.ne.jp/chan-toru")){
    				tvCommand.remove(tv);
    				break;
    			}
    		}
    	}

		return b;

	}

	/**
	 * <P>XMLDecoderに失敗すると起動できなくなる場合があるので、どうしても起動時に欲しい設定だけベタテキストで保存しおく
	 * <P>{@link #saveLAF()}は公開しない
	 */
	@Deprecated
	public boolean loadLAF() {
    	return true;
	}

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public Env() {

		// ↑の宣言時に設定できないもの

		{
			ArrayList<JTXTButton.FontStyle> fsa = new ArrayList<JTXTButton.FontStyle>();
			fsa.add(JTXTButton.FontStyle.BOLD);
			fsa.add(JTXTButton.FontStyle.UNDERLINE);
			this.setTitleFontStyle(fsa);
		}
		{
			ArrayList<JTXTButton.FontStyle> fsa = new ArrayList<JTXTButton.FontStyle>();
			this.setDetailFontStyle(fsa);
		}
		{
			HashMap<TVProgram.ProgOption, Boolean> h = new HashMap<TVProgram.ProgOption, Boolean>();
			for ( Object[] obj : TVProgram.optMarks ) {
				h.put((TVProgram.ProgOption) obj[0],true);
			}
			this.setOptMarks(h);
		}
		{
			ArrayList<TextValueSet> al = new ArrayList<TextValueSet>();

			TextValueSet tv = new TextValueSet();
			tv.setText("Googleでタイトルを検索");
			tv.setValue("http://www.google.co.jp/search?q=%ENCTITLE%");
			al.add(tv);

			tv = new TextValueSet();
			tv.setText("Bingでタイトルを検索");
			tv.setValue("http://www.bing.com/search?q=%ENCTITLE%");
			al.add(tv);

			tv = new TextValueSet();
			tv.setText("ブラウザで番組詳細のページを開く");
			tv.setValue("%DETAILURL%");
			al.add(tv);

			tv = new TextValueSet();
			tv.setText("ブラウザで放送局のページを探す");
			tv.setValue("http://www.google.co.jp/search?hl=ja&btnI=1&q=%ENCCHNAME%");
			al.add(tv);

			tv = new TextValueSet();
			tv.setText("ブラウザで番組情報のページを探す");
			tv.setValue("http://www.google.co.jp/search?hl=ja&btnI=1&q=%ENCCHNAME%+%ENCTITLE%");
			al.add(tv);

			tv = new TextValueSet();
			tv.setText("ブラウザでTweetする");
			tv.setValue("https://twitter.com/share?text=%DATE%+%START%-%END%+%ENCCHNAME%+%ENCTITLE%");
			al.add(tv);

			this.setTvCommand(al);
		}

		// おまけ
		if ( firstinstance ) {
			firstinstance = false;
			loadLAF();
		}
	}
}
