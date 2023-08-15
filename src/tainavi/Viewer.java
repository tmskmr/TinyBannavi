package tainavi;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ServiceLoader;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tainavi.HDDRecorder.RecType;
import tainavi.TVProgram.ProgFlags;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgOption;
import tainavi.TVProgram.ProgSubgenre;
import tainavi.TVProgram.ProgSubtype;
import tainavi.TVProgram.ProgType;
import tainavi.VWMainWindow.MWinTab;
import tainavi.VWUpdate.UpdateResult;
import tainavi.plugintv.Syobocal;


/**
 * メインな感じ
 */
public class Viewer extends JFrame implements ChangeListener,TickTimerListener,HDDRecorderListener,CancelListener {

	private static final long serialVersionUID = 1L;


	/*
	 * メソッド的な
	 */

	private void StdAppendMessage(String message)	{ System.out.println(CommonUtils.getNow() + message); }
	private void StdAppendError(String message)		{ System.err.println(CommonUtils.getNow() + message); }
	//
	private void MWinSetVisible(boolean b)			{ mainWindow.setStatusAreaVisible(b); }
	//
	private void StWinClear()						{ stwin.clear(); }
	private void StWinSetVisible(boolean b)			{ stwin.setVisible(b); }
	private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)stwin); }
	private void StWinSetLocationUnder(Component frame)  { CommonSwingUtils.setLocationUnder(frame, (VWStatusWindow)stwin); }

	private void ringBeep() { if (env!=null && ! env.getDisableBeep()) { Toolkit.getDefaultToolkit().beep(); if ( env.getDebug() ) CommonUtils.printStackTrace(); } }


	/*
	 * オブジェクト的な
	 */

	// 設定値をいれるところ
	private final Env env = Env.TheEnv = new Env();							// 主要な設定
	private final Bounds bounds = Bounds.TheBounds = new Bounds();			// ウィンドウサイズとか動的に変化するもの
	private final ClipboardInfoList cbitems = new ClipboardInfoList();			// クリップボード対応機能でどの項目をコピーするかとかの設定
	private final ListedColumnInfoList lvitems = new ListedColumnInfoList();	// リスト形式ビューの表示項目
	private final ReserveListColumnInfoList rlitems = new ReserveListColumnInfoList();
																				// 本体予約一覧ビューの表示項目
	private final TitleListColumnInfoList tlitems = new TitleListColumnInfoList();
																				// タイトル一覧ビューの表示項目
	private final ListedNodeInfoList lnitems = new ListedNodeInfoList();		// リスト形式ビューのツリー表示項目
	private final ToolBarIconInfoList tbicons = new ToolBarIconInfoList();		// ツールバーの表示項目
	private final TabInfoList tabitems = new TabInfoList();						// タブの表示項目

	private final PaperColorsMap pColors = new PaperColorsMap();				// 新聞形式のジャンル別背景色の設定
	private final AVSetting avs = new AVSetting();								// ジャンル別録画画質・音質等設定
	private final CHAVSetting chavs = new CHAVSetting();						// CH別録画画質・音質等設定
	private final ChannelSort chsort = new ChannelSort();						// CHソート設定
	private final ChannelConvert chconv = new ChannelConvert();					// ChannelConvert.dat
	private final MarkChar markchar = new MarkChar(env);						// タイトルにつけるマークを操作する

	private final MarkedProgramList mpList = new MarkedProgramList();			// 検索結果のキャッシュ（表示高速化用）
	private final TraceProgram trKeys = new TraceProgram();						// 番組追跡の設定
	private final SearchProgram srKeys = new SearchProgram();					// キーワード検索の設定
	private final SearchGroupList srGrps = new SearchGroupList();				// キーワード検索グループの設定
	private final ExtProgram extKeys = new ExtProgram();						// 延長警告管理の設定

	private final RecorderInfoList recInfoList = new RecorderInfoList();		// レコーダ一覧の設定

	private final HDDRecorderList recPlugins = new HDDRecorderList();			// レコーダプラグイン（テンプレート）
	private final HDDRecorderList recorders = new HDDRecorderList();			// レコーダプラグイン（実際に利用するもの）

	private final TVProgramList progPlugins = new TVProgramList();				// Web番組表プラグイン（テンプレート）
	private final TVProgramList tvprograms = new TVProgramList();				// Web番組表プラグイン（実際に利用するもの）

	private final TickTimer timer_now = new TickTimer();							// 毎分00秒に起動して処理をキックするタイマー

	// 初期化的な
	private boolean logging = true;											// ログ出力する
	private boolean runRecWakeup = false;										// 起動時にレコーダを起こす
	private boolean runRecLoad = false;											// 起動時にレコーダから予約一覧を取得する
	private boolean enableWebAccess = true;										// 起動時のWeb番組表へのアクセスを禁止する
	private boolean onlyLoadProgram = false;
	private String pxaddr = null;												// ProxyAddress指定
	private String pxport = null;												// ProxtPort指定

	private boolean downloadInProgress = false;								// TVプログラムを取得中か
	private boolean cacheFileOnly = false;									// キャッシュファイルからのみの読み込みか

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	public static final String LOG_FILE = "log.txt";							// ログファイル名
	public static final String HISTORY_FILE = "05_history.txt";				// 更新履歴だよ

	private static final String ICONFILE_SYSTRAY = "icon"+File.separator+"tainavi16.png";
	private static final String ICONFILE_TAINAVI = "icon"+File.separator+"tainavi.png";

	public static final int TIMEBAR_START = 5;					// 新聞形式の開始時刻
	private static final int OPENING_WIAT = 500;				// まあ起動時しか使わないんですけども

	private static final String MSGID = "[鯛ナビ] ";
//	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	/*
	 * [メモ] enumのtoString()をoverrideすると、シリアライズの際とても困るのでやらないこと
	 */

	/**
	 * Web番組表のどれとどれを読めばいいのか
	 */
	public static enum LoadFor {
		TERRA	("地上波&BSのみ取得"),
		CS		("CSのみ取得"),
		CSo1	("CS[プライマリ]のみ取得"),
		CSo2	("CS[セカンダリ]のみ取得"),
		CSwSD	("CSのみ取得（取得後シャットダウン）"),
		RADIO	("ラジオのみ取得"),
		SYOBO	("しょぼかるのみ取得"),
		ALL		("すべて取得");

		private String name;

		private LoadFor(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public static LoadFor get(String s) {
			for ( LoadFor lf : LoadFor.values() ) {
				if ( lf.name.equals(s) ) {
					return lf;
				}
			}
			return null;
		}
	};

	/**
	 * レコーダ情報のどれとどれを読めばいいのか
	 */
	public static enum LoadRsvedFor {
//		SETTING		( "設定情報のみ取得(future use.)" ),
		DETAILS		( "予約一覧＋録画詳細のみ取得" ),
		RECORDED	( "録画結果一覧のみ取得" ),
		AUTORESERVE	( "自動予約一覧のみ取得" ),
		;

		private String name;

		private LoadRsvedFor(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static LoadRsvedFor get(String s) {
			for ( LoadRsvedFor lrf : LoadRsvedFor.values() ) {
				if ( lrf.name.equals(s) ) {
					return lrf;
				}
			}
			return null;
		}
	}

	/**
	 *  リスト形式のカラム定義
	 * @deprecated しっぱいした 半年くらいしたら削除する
	 */
	@Deprecated
	public static enum ListedColumn {
		RSVMARK		("予約",			35),
		DUPMARK		("重複",			35),
		CHNAME		("チャンネル名",	100),
		TITLE		("番組タイトル",	300),
		DETAIL		("番組詳細",		200),
		START		("開始時刻",		150),
		END			("終了",			50),
		LENGTH		("長さ",			50),
		GENRE		("ジャンル",		85),
		SITEM		("検索アイテム名",	100),
		STAR		("お気に入り度",	100),
		SCORE		("ｽｺｱ",			35),
		THRESHOLD	("閾値",			35),
		HID_PRGID	("PRGID",		-1),
		HID_STIME	("STIME",		-1),
		HID_ETIME	("ETIME",		-1),
		HID_EXFLG	("EXFLG",		-1),
		HID_TITLE	("TITLE",		-1),
		;

		@SuppressWarnings("unused")
		private String name;
		private int iniWidth;

		private ListedColumn(String name, int iniWidth) {
			this.name = name;
			this.iniWidth = iniWidth;
		}

		/* なんだかなー
		@Override
		public String toString() {
			return name;
		}
		*/

		public int getIniWidth() {
			return iniWidth;
		}

		public int getColumn() {
			return ordinal();
		}
	};

	/**
	 *  本体予約一覧のカラム定義
	 * @deprecated しっぱいした 半年くらいしたら削除する
	 */
	@Deprecated
	public static enum RsvedColumn {
		PATTERN		("パタン",			110),
		DUPMARK		("重複",			35),
		EXEC		("実行",			35),
		TRACE		("追跡",			35),
		NEXTSTART	("次回実行予定",	150),
		END			("終了",			50),
		LENGTH		("長さ",			50),
		ENCODER		("ｴﾝｺｰﾀﾞ",		50),
		VRATE		("画質",			100),
		ARATE		("音質",			50),
		TITLE		("番組タイトル",	300),
		CHNAME		("チャンネル名",	150),
		RECORDER	("レコーダ",		200),
		HID_INDEX	("INDEX",		-1),
		HID_RSVID	("RSVID",		-1),
		;

		@SuppressWarnings("unused")
		private String name;
		private int iniWidth;

		private RsvedColumn(String name, int iniWidth) {
			this.name = name;
			this.iniWidth = iniWidth;
		}

		/*
		@Override
		public String toString() {
			return name;
		}
		*/

		public int getIniWidth() {
			return iniWidth;
		}

		public int getColumn() {
			return ordinal();
		}
	};



	/*
	 * コンポーネント
	 */

	// 起動時に固定で用意しておくもの
	private final VWStatusWindow stwin = new VWStatusWindow();
	private final VWStatusTextArea mwin = new VWStatusTextArea();
	private VWColorChooserDialog ccwin = null;
	private VWPaperColorsDialog pcwin = null;
	private VWReserveDialog rdialog = null;

	// 初期化処理の中で生成していくもの
	private VWMainWindow mainWindow = null;
	private VWToolBar toolBar = null;
	private VWListedView listed = null;
	private VWPaperView paper = null;
	private VWReserveListView reserved = null;
	private VWRecordedListView recorded = null;
	private VWAutoReserveListView autores = null;
	private VWTitleListView titled = null;
	private VWSettingView setting = null;
	private VWRecorderSettingView recsetting = null;
	private VWChannelSettingView chsetting = null;
	private VWChannelDatSettingView chdatsetting = null;
	private VWChannelSortView chsortsetting = null;
	private VWChannelConvertView chconvsetting = null;
	private VWLookAndFeel vwlaf = null;
	private VWFont vwfont = null;
	private VWSearchWordDialog swdialog = null;

	private TrayIcon trayicon = null;

	/*******************************************************************************
	 * パブリック関数
	 ******************************************************************************/
	public Bounds getBoundsEnv(){ return bounds; }
	public TabInfoList getTabItemEnv(){ return tabitems; }

	/*******************************************************************************
	 * タブやダイアログのインスタンス作成用クラス定義
	 ******************************************************************************/

	/***
	 * リスト形式の内部クラス
	 */
	private class VWListedView extends AbsListedView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected ListedColumnInfoList getLvItemEnv() { return lvitems; }
		@Override
		protected ListedNodeInfoList getLnItemEnv() { return lnitems; }
		@Override
		protected ChannelSort getChannelSort() { return chsort; }

		@Override
		protected MarkedProgramList getMarkedProgramList() { return mpList; }
		@Override
		protected TraceProgram getTraceProgram() { return trKeys; }
		@Override
		protected SearchProgram getSearchProgram() { return srKeys; }
		@Override
		protected SearchGroupList getSearchGroupList() { return srGrps; }
		@Override
		protected ExtProgram getExtProgram() { return extKeys; }

		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }
		@Override
		protected HDDRecorderList getRecorderList() { return recorders; }

		// メッセージ出力関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected AbsReserveDialog getReserveDialog() { return rdialog; }
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * AbsListedView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected void onShown() {
			// キーワード登録ボタンはリスト形式のみ
			toolBar.setAddkeywordEnabled(true);
			// スナップショットを有効にする
			toolBar.setSnapShotEnabled(true);
			// 新聞形式以外ではマッチ枠を無効にする
			toolBar.setBorderToggleEnabled(true, bounds.getShowReservedBackground());
			updateBatchReservationEnabled();
			toolBar.updateShowOffReserveToggleButton();
		}

		@Override
		protected void onHidden() {
			// キーワード登録ボタンはリスト形式のみ
			toolBar.setAddkeywordEnabled(false);
			// スナップショットを無効にする
			toolBar.setSnapShotEnabled(false);
			// 新聞形式以外ではマッチ枠を無効にする
			toolBar.setBorderToggleEnabled(false, bounds.getShowReservedBackground());
			updateBatchReservationEnabled();
			toolBar.updateShowOffReserveToggleButton();
		}

		@Override
		protected void updateBatchReservationEnabled(){
			// 一括予約はリスト形式のみ
			toolBar.setBatchReservationEnabled(this.isVisible() && getSelectedRowCount() > 0);
		}

		@Override
		protected void showPopupMenu(
				final JComponent comp, 	final ProgDetailList tvd, final int x, final int y) {

			timer_now.pause();	// 停止

			Viewer.this.showPopupMenuForProgram(comp, tvd, x, y, null);

			timer_now.start();	// 再開
		}

		@Override
		protected void updateReserveDisplay(String chname) {
			timer_now.pause();
			paper.updateReserveBorder(chname);
			reserved.redrawReservedList();
			timer_now.start();
		}

		@Override
		protected void updateBangumiColumns() {
			timer_now.pause();
			paper.updateBangumiColumns();
			timer_now.start();
		}

		@Override
		protected void clearPaper() {
			timer_now.pause();
			paper.clearPanel();
			timer_now.start();
		}

		@Override
		protected void previewKeywordSearch(SearchKey search) {
			//timer_now.pause();
			if (search.alTarget.size() > 0) {
				mainWindow.setSelectedTab(MWinTab.LISTED);
				listed.redrawListByPreview(search);
			}
			//timer_now.start();
		}

		@Override
		protected void jumpToPaper(String Center, String StartDateTime) {
			//timer_now.pause();
			paper.jumpToBangumi(Center,StartDateTime);
			//timer_now.start();
		}

		@Override
		protected boolean addToPickup(ProgDetailList tvd) { return Viewer.this.addToPickup(tvd); }

		@Override
		protected boolean isTabSelected(MWinTab tab) { return mainWindow.isTabSelected(tab); }
		@Override
		protected void setSelectedTab(MWinTab tab) { mainWindow.setSelectedTab(tab); }

		@Override
		protected String getSelectedRecorderOnToolbar() { return toolBar.getSelectedRecorder(); }
		@Override
		protected boolean isFullScreen() { return toolBar.isFullScreen(); }
		@Override
		protected void setPagerEnabled(boolean b) { toolBar.setPagerEnabled(b); }
		@Override
		protected int getPagerCount() { return toolBar.getPagerCount(); }
		@Override
		protected int getSelectedPagerIndex() { return toolBar.getSelectedPagerIndex(); }

		@Override
		protected void setDividerEnvs(int loc) {
			if ( ! toolBar.isFullScreen() && mainWindow.isTabSelected(MWinTab.LISTED) ) {
				if (env.getSyncTreeWidth()) {
					bounds.setTreeWidth(loc);
					bounds.setTreeWidthPaper(loc);
				}
				else {
					bounds.setTreeWidth(loc);
				}
			}
		}
	}



	/**
	 * 新聞形式の内部クラス
	 */
	private class VWPaperView extends AbsPaperView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected PaperColorsMap getPaperColorMap() { return pColors; }
		@Override
		protected ChannelSort getChannelSort() { return chsort; }

		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }
		@Override
		protected HDDRecorderList getRecorderList() { return recorders; }

		// メッセージ出力関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected AbsReserveDialog getReserveDialog() { return rdialog; }
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * AbsPaperView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected void onShown() {
			// ページャーコンボボックスを有効にする（状況次第で有効にならない場合もある）（ツリーの選択次第で変わるのでもどし）
			//toolBar.setPagerEnabled(true);
			// スナップショットを有効にする
			toolBar.setSnapShotEnabled(true);
			// ジャンル別背景色を有効にする
			toolBar.setPaperColorDialogEnabled(true);
			// マッチ枠を有効にする
			toolBar.setBorderToggleEnabled(true, bounds.getShowMatchedBorder());
			toolBar.updateShowOffReserveToggleButton();
		}

		@Override
		protected void onHidden() {
			// 新聞形式以外ではページャーコンボボックスを無効にする（ツリーの選択次第で変わるのでもどし）
			//toolBar.setPagerEnabled(false);
			// 新聞形式以外ではスナップショットを無効にする
			toolBar.setSnapShotEnabled(false);
			// 新聞形式以外ではジャンル別背景色を無効にする
			toolBar.setPaperColorDialogEnabled(false);
			// 新聞形式以外ではマッチ枠を無効にする
			toolBar.setBorderToggleEnabled(false, bounds.getShowMatchedBorder());
			toolBar.updateShowOffReserveToggleButton();
		}

		@Override
		protected void showPopupMenu(
				final JComponent comp, final ProgDetailList tvd, final int x, final int y, final String clickedDateTime) {

			timer_now.pause();	// 停止

			Viewer.this.showPopupMenuForProgram(comp, tvd, x, y, clickedDateTime);

			timer_now.start();	// 再開
		}

		@Override
		protected void updateReserveDisplay() {
			timer_now.pause();
			listed.updateReserveMark();
			reserved.redrawReservedList();
			timer_now.start();
		}

		@Override
		protected void addToPickup(ProgDetailList tvd) { Viewer.this.addToPickup(tvd); }

		@Override
		protected boolean isTabSelected(MWinTab tab) { return mainWindow.isTabSelected(tab); }
		@Override
		protected void setSelectedTab(MWinTab tab) { mainWindow.setSelectedTab(tab); }

		@Override
		protected boolean isFullScreen() { return toolBar.isFullScreen(); }
		@Override
		protected void setSelectedPagerIndex(int idx) {
			toolBar.setSelectedPagerIndex(idx);
		}
		@Override
		protected void setPagerEnabled(boolean b) { toolBar.setPagerEnabled(b); }
		@Override
		protected int getPagerCount() { return toolBar.getPagerCount(); }
		@Override
		protected int getSelectedPagerIndex() { return toolBar.getSelectedPagerIndex(); }
		@Override
		protected void setPagerItems(TVProgramIterator pli, int curindex) {
			toolBar.setPagerItems(pli,curindex);
		}

		@Override
		protected String getExtensionMark(ProgDetailList tvd) { return markchar.getExtensionMark(tvd); }
		@Override
		protected String getOptionMark(ProgDetailList tvd) { return markchar.getOptionMark(tvd)+markchar.getNewLastMark(tvd); }
		@Override
		protected String getPostfixMark(ProgDetailList tvd) { return markchar.getPostfixMark(tvd); }

		@Override
		protected void setDividerEnvs(int loc) {
			if ( ! toolBar.isFullScreen() && mainWindow.isTabSelected(MWinTab.PAPER) ) {
				if (env.getSyncTreeWidth()) {
					bounds.setTreeWidth(loc);
					bounds.setTreeWidthPaper(loc);
				}
				else {
					bounds.setTreeWidthPaper(loc);
				}
			}
		}

		@Override
		protected void moveToPrevPage() {
			toolBar.moveToPrevPage();

		}

		@Override
		protected void moveToNextPage() {
			toolBar.moveToNextPage();
		}
	}



	/**
	 *
	 * 本体予約一覧の内部クラス
	 *
	 */
	private class VWReserveListView extends AbsReserveListView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected ReserveListColumnInfoList getRlItemEnv(){ return rlitems; }
		@Override
		protected ChannelSort getChannelSort() { return chsort; }

		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }
		@Override
		protected HDDRecorderList getRecorderList() { return recorders; }

		// ログ関係はないのか

		// コンポーネントを渡す
		@Override
		protected AbsReserveDialog getReserveDialog() { return rdialog; }
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * AbsReserveListView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected void updateReserveDisplay(String chname) {
			timer_now.pause();
			listed.updateReserveMark();
			paper.updateReserveBorder(chname);
			timer_now.start();
		}

		@Override
		protected boolean doExecOnOff(boolean fexec, String title, String chnam, String rsvId, String recId) {
			return Viewer.this.doExecOnOff(fexec, title, chnam, rsvId, recId);
		}

		@Override
		protected JMenuItem getExecOnOffMenuItem(boolean fexec,
				String start, String title, String chnam, String rsvId, String recId) {

			return Viewer.this.getExecOnOffMenuItem(fexec, start, title, chnam, rsvId, recId, 0);
		}

		@Override
		protected JMenuItem getRemoveRsvMenuItem(
				String start, String title, String chnam,	String rsvId, String recId) {

			return Viewer.this.getRemoveRsvMenuItem(start, title, chnam, rsvId, recId, 0);
		}

		@Override
		protected JMenuItem getJumpMenuItem(String title, String chnam,
				String startDT) {

			return Viewer.this.getJumpMenuItem(title, chnam, startDT);
		}

		@Override
		protected JMenuItem getJumpToLastWeekMenuItem(String title,
				String chnam, String startDT) {

			return Viewer.this.getJumpToLastWeekMenuItem(title, chnam, startDT);
		}

		/*
		 * プログラムのブラウザーメニューを呼び出す
		 */
		@Override
		protected void addBrowseMenuToPopup( JPopupMenu pop,	final ProgDetailList tvd ){
			Viewer.this.addBrowseMenuToPopup( pop, tvd);
		}

		@Override
		protected String getSelectedRecorderOnToolbar() { return toolBar.getSelectedRecorder(); }

		@Override
		protected boolean isTabSelected(MWinTab tab) { return mainWindow.isTabSelected(tab); }
	}


	/**
	 *
	 * 録画結果一覧の内部クラス
	 *
	 */
	private class VWRecordedListView extends AbsRecordedListView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }

		@Override
		protected HDDRecorderList getRecorderList() { return recorders; }

		// ログ関係はないのか

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * AbsReserveListView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected String getSelectedRecorderOnToolbar() { return toolBar.getSelectedRecorder(); }
	}


	/**
	 *
	 * 録画結果一覧の内部クラス
	 *
	 */
	private class VWAutoReserveListView extends AbsAutoReserveListView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }

	}

	/**
	 *
	 * タイトル一覧の内部クラス
	 *
	 */
	private class VWTitleListView extends AbsTitleListView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected TitleListColumnInfoList getTlItemEnv(){ return tlitems; }

		// メッセージ出力関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }

		@Override
		protected HDDRecorderList getRecorderList() { return recorders; }

		// ログ関係はないのか

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * AbsTitleListView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected String getSelectedRecorderOnToolbar() { return toolBar.getSelectedRecorder(); }

		/**
		 *  タイトルの詳細情報を取得するメニューアイテム
		 */
		@Override
		protected JMenuItem getTitleDetailMenuItem(final String title, final String chnam,
				final String devId, final String ttlId, final String recId){
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem("タイトルの詳細情報を取得する【"+title+"("+chnam+")/"+recName+"】");

			HDDRecorder rec = getSelectedRecorder();
			TitleInfo t = rec.getTitleInfo(ttlId);

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stwin.clear();

					// 取得本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							stwin.appendMessage("タイトルの詳細情報を取得します："+title+"("+ttlId+")");

							boolean b = rec.GetRdTitleDetail(t);
							if (b) {
								mwin.appendMessage("正常に取得できました："+t.getTitle()+"("+t.getCh_name()+")");
								t.setDetailLoaded(true);
							}
							else {
								mwin.appendError("取得に失敗しました："+t.getTitle());
							}

							if ( ! rec.getErrmsg().equals("")) {
								mwin.appendError("【追加情報】"+rec.getErrmsg());
								ringBeep();
							}

							titled.redrawSelectedTitle(true);
							return null;
						}
						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

 		/**
		 *  複数タイトルの詳細情報をまとめて取得するメニューアイテム
		 */
		@Override
		protected JMenuItem getMultiTitleDetailMenuItem(final String ttlname, final String chnam,
				final String device_id, final String [] ttlIds, final String recId){
			int ttlNum = ttlIds.length;
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem(String.valueOf(ttlNum) + "タイトルの詳細情報をまとめて取得する【"+ttlname+"("+chnam+")/"+recName+"】");

			HDDRecorder rec = getSelectedRecorder();

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stwin.clear();

					// 取得本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							for (int n=0; n<ttlNum; n++){
								String ttlId = ttlIds[n];

								TitleInfo t = rec.getTitleInfo(ttlId);
								if (t == null)
									continue;

								String ttlname = t.getTitle();

								stwin.appendMessage("タイトルの詳細情報を取得します："+ttlname+"("+ttlId+")");
								boolean b = rec.GetRdTitleDetail(t);
								if (b) {
									mwin.appendMessage("正常に取得できました："+t.getTitle()+"("+t.getCh_name()+")");
									t.setDetailLoaded(true);
								}
								else {
									mwin.appendError("取得に失敗しました："+t.getTitle());

									if ( ! rec.getErrmsg().equals("")) {
										mwin.appendError("【追加情報】"+rec.getErrmsg());
										ringBeep();
									}

									break;
								}

							}

							titled.redrawTitleList();
							return null;
						}

						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

		@Override
		protected JMenuItem getJumpMenuItem(final String title, final String chnam, final String startDT){

			return Viewer.this.getJumpMenuItem(title, chnam, startDT);
		}

		/**
		 *  タイトルを編集するメニューアイテム
		 */
		@Override
		protected JMenuItem getEditTitleMenuItem(final String title, final String chnam,
				final String devId, final String ttlId, final String recId, final String otitle){
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem("タイトルを編集する【"+title+"("+chnam+")/"+recName+"】");
			Font f = menuItem.getFont();
			menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));

			HDDRecorder rec = getSelectedRecorder();
			TitleInfo o = rec.getTitleInfo(ttlId);
			ProgDetailList prog = getProgDetailForTitle(o);
			ProgDetailList progSyobo = getSyobocalProgDetailForTitle(o);

			String ttlname = o.getTitle();

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!o.getDetailLoaded())
						rec.GetRdTitleDetail(o);

					VWTitleDialog dlg = new VWTitleDialog(false);
					dlg.open(o.clone(), prog, progSyobo, otitle);
					CommonSwingUtils.setLocationCenter(Viewer.this, dlg);
					dlg.setVisible(true);

					if (!dlg.isRegistered())
						return;

					TitleInfo t = dlg.getTitleInfo();
					stwin.clear();

					// 更新本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							stwin.appendMessage("タイトル情報を更新します："+ttlname+"("+ttlId+")");

							boolean b = rec.UpdateRdTitleInfo(devId, o, t);	// Noで検索
							if (b) {
								mwin.appendMessage("正常に更新できました："+t.getTitle()+"("+t.getCh_name()+")");
							}
							else {
								mwin.appendError("更新に失敗しました："+ttlname);
							}

							if ( ! rec.getErrmsg().equals("")) {
								mwin.appendError("【追加情報】"+rec.getErrmsg());
								ringBeep();
							}

							// フォーカスを戻す
							titled.redrawTitleList();
							return null;
						}
						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

 		/**
		 *  タイトルを削除するメニューアイテム
		 */
		@Override
		protected JMenuItem getRemoveTitleMenuItem(final String ttlname, final String chnam,
				final String devId, final String ttlId, final String recId){
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem("タイトルを削除する【"+ttlname+"("+chnam+")/"+recName+"】");
			menuItem.setForeground(Color.RED);

			HDDRecorder rec = getSelectedRecorder();

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					if (env.getShowWarnDialog()) {
						Container cp = getContentPane();
						int ret = JOptionPane.showConfirmDialog(cp, "削除しますか？【"+ttlname+"("+chnam+")】（"+recName+"）", "確認", JOptionPane.YES_NO_OPTION);
						if (ret != JOptionPane.YES_OPTION) {
							return;
						}
					}

					stwin.clear();

					// 削除本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							String ttlname = "";
							for (TitleInfo t : rec.getTitles()) {
								if (t.getId().equals(ttlId)) {
									ttlname = t.getTitle();
									break;
								}
							}

							stwin.appendMessage("タイトルを削除します："+ttlname+"("+ttlId+")");
							TitleInfo t = rec.RemoveRdTitle(devId, ttlId);	// Noで検索
							if (t != null) {
								mwin.appendMessage("正常に削除できました："+t.getTitle()+"("+t.getCh_name()+")");

								if ( ! t.getTitle().equals(ttlname) || ! t.getId().equals(ttlId)) {
									mwin.appendError("【警告】削除結果が一致しません！："+ttlname+"／"+t.getTitle());
								}
							}
							else {
								mwin.appendError("削除に失敗しました："+ttlname);
							}

							//
							if ( ! rec.getErrmsg().equals("")) {
								mwin.appendError("【追加情報】"+rec.getErrmsg());
								ringBeep();
							}

							titled.redrawTitleList();
							return null;
						}

						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

 		/**
		 *  複数タイトルをまとめてフォルダ移動するメニューアイテム
		 */
		@Override
		protected JMenuItem getMoveMultiTitleMenuItem(final String ttlname, final String chnam,
				final String devId, final String [] ttlIds, final String recId){
			int ttlNum = ttlIds.length;
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem(String.valueOf(ttlNum) + "タイトルをまとめて別のフォルダに移動する【"+ttlname+"("+chnam+")/"+recName+"】");

			HDDRecorder rec = getSelectedRecorder();
			TitleInfo o0 = rec.getTitleInfo(ttlIds[0]);

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (!o0.getDetailLoaded())
						rec.GetRdTitleDetail(o0);

					VWTitleDialog dlg = new VWTitleDialog(true);
					dlg.open(o0.clone(), null, null, null);
					CommonSwingUtils.setLocationCenter(Viewer.this, dlg);
					dlg.setVisible(true);

					if (!dlg.isRegistered())
						return;

					TitleInfo t0 = dlg.getTitleInfo();

					stwin.clear();

					// フォルダ移動本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							for (int n=0; n<ttlNum; n++){
								String ttlId = ttlIds[n];

								TitleInfo on = rec.getTitleInfo(ttlId);
								if (on == null)
									continue;

								String ttlname = on.getTitle();
								TitleInfo tn = on.clone();
								tn.setRec_folder((ArrayList<TextValueSet>) t0.getRec_folder().clone());

								stwin.appendMessage("タイトルを移動します："+ttlname+"("+ttlId+")");
								boolean b = rec.UpdateRdTitleInfo(devId, on, tn);	// Noで検索
								if (b) {
									mwin.appendMessage("正常に移動できました："+tn.getTitle()+"("+tn.getCh_name()+")");
								}
								else {
									mwin.appendError("移動に失敗しました："+ttlname);
								}

								if ( ! rec.getErrmsg().equals("")) {
									mwin.appendError("【追加情報】"+rec.getErrmsg());
									ringBeep();
									break;
								}
							}

							titled.redrawTitleList();
							return null;
						}

						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

 		/**
		 *  複数タイトルをまとめて削除するメニューアイテム
		 */
		@Override
		protected JMenuItem getRemoveMultiTitleMenuItem(final String ttlname, final String chnam,
				final String device_id, final String [] ttlIds, final String recId){
			int ttlNum = ttlIds.length;
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem(String.valueOf(ttlNum) + "タイトルをまとめて削除する【"+ttlname+"("+chnam+")/"+recName+"】");
			menuItem.setForeground(Color.RED);

			HDDRecorder rec = getSelectedRecorder();

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					if (env.getShowWarnDialog()) {
						Container cp = getContentPane();
						int ret = JOptionPane.showConfirmDialog(cp, String.valueOf(ttlNum) + "タイトルをまとめて削除しますか？【"+ttlname+"("+chnam+")】（"+recName+"）", "確認", JOptionPane.YES_NO_OPTION);
						if (ret != JOptionPane.YES_OPTION) {
							return;
						}
					}

					stwin.clear();

					// 削除本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							for (int n=0; n<ttlNum; n++){
								String ttlname = "";
								String ttlId = ttlIds[n];
								String devId = device_id;

								for (TitleInfo t : rec.getTitles()) {
									if (t.getId().equals(ttlId)) {
										ttlname = t.getTitle();
										devId = rec.getDeviceID(t.getRec_device());
										break;
									}
								}

								stwin.appendMessage("タイトルを削除します："+ttlname+"("+ttlId+")");
								TitleInfo t = rec.RemoveRdTitle(devId, ttlId);	// Noで検索
								if (t != null) {
									mwin.appendMessage("正常に削除できました："+t.getTitle()+"("+t.getCh_name()+")");

									if ( ! t.getTitle().equals(ttlname) || ! t.getId().equals(ttlId)) {
										mwin.appendError("【警告】削除結果が一致しません！："+ttlname+"／"+t.getTitle());
									}
								}
								else {
									mwin.appendError("削除に失敗しました："+ttlname);

									//
									if ( ! rec.getErrmsg().equals("")) {
										mwin.appendError("【追加情報】"+rec.getErrmsg());
										ringBeep();
									}

									break;
								}
							}

							titled.redrawTitleList();
							return null;
						}

						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

		/**
		 *  タイトルを再生・停止するメニューアイテム
		 */
		@Override
		protected JMenuItem getStartStopPlayTitleMenuItem(boolean start, String title, String chnam, String devId,
				String ttlId, String recId) {
			String pstr = start ? "開始" : "終了";
			String recName = recorders.getRecorderName(recId);
			JMenuItem menuItem = new JMenuItem("タイトルの再生を" + pstr + "する【"+title+"("+chnam+")/"+recName+"】");

			HDDRecorder rec = getSelectedRecorder();

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					stwin.clear();

					// 開始・終了本体
					new SwingBackgroundWorker(false) {
						@Override
						protected Object doWorks() throws Exception {
							boolean b = rec.StartStopPlayRdTitle(devId, ttlId, start);	// Noで検索
							if (b) {
								mwin.appendMessage("正常に" + pstr + "できました：" + title);
							}
							else {
								mwin.appendError(pstr + "に失敗しました："+title);
							}

							//
							if ( ! rec.getErrmsg().equals("")) {
								mwin.appendError("【追加情報】"+rec.getErrmsg());
								ringBeep();
							}

//							titled.redrawTitleList();
							return null;
						}

						@Override
						protected void doFinally() {
							stwin.setVisible(false);
						}
					}.execute();

					CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
					stwin.setVisible(true);
				}
			});

			return menuItem;
		}

		/*
		 * プログラムのブラウザーメニューを呼び出す
		 */
		@Override
		protected void addBrowseMenuToPopup( JPopupMenu pop,	final ProgDetailList tvd ){
			Viewer.this.addBrowseMenuToPopup( pop, tvd);
		}
	}

	/***
	 * 各種設定の内部クラス
	 */
	private class VWSettingView extends AbsSettingView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected ClipboardInfoList getCbItemEnv() { return cbitems; }
		@Override
		protected ListedColumnInfoList getLvItemEnv() { return lvitems; }
		@Override
		protected ReserveListColumnInfoList getRlItemEnv(){ return rlitems; }
		@Override
		protected TitleListColumnInfoList getTlItemEnv(){ return tlitems; }
		@Override
		protected ListedNodeInfoList getLnItemEnv() { return lnitems; }
		@Override
		protected ToolBarIconInfoList getTbIconEnv(){ return tbicons; }
		@Override
		protected TabInfoList getTabItemEnv(){ return tabitems; }
		@Override
		protected VWLookAndFeel getLAFEnv() { return vwlaf; }
		@Override
		protected VWFont getFontEnv() { return vwfont; }

		// メッセージ出力関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }
		@Override
		protected VWColorChooserDialog getCcWin() { return ccwin; }

		/*
		 * AbsSettingView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected void lafChanged(String lafname) {
			vwlaf.update(lafname);
			Viewer.this.updateComponentTreeUI();
			StdAppendMessage("Set LookAndFeel="+lafname);
		}

		@Override
		protected void fontChanged(String fn, int fontSize) {
			vwfont.update(fn, fontSize);
			Viewer.this.updateComponentTreeUI();
			StdAppendMessage("システムのフォントを変更しました： "+fn+", size="+fontSize);
		}

		@Override
		protected void setEnv(final boolean reload_prog) {

			//listed.pauseTimer();
			timer_now.pause();

			Viewer.this.setEnv(reload_prog);

			timer_now.start();
		}
	}

	/**
	 * レコーダ設定タブの内部クラス
	 * @see AbsRecorderSettingView
	 */
	private class VWRecorderSettingView extends AbsRecorderSettingView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected RecorderInfoList getRecInfos() { return recInfoList; }
		@Override
		protected HDDRecorderList getRecPlugins() { return recPlugins; }

		// ログ関連
		@Override
		protected VWStatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }
		@Override
		protected VWColorChooserDialog getCcWin() { return ccwin; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * AbsRecorderSettingView内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected void setRecInfos() {

			timer_now.pause();

			// 設定を保存
			recInfoList.save();

			// レコーダプラグインのリフレッシュ
			initRecPluginAll();

			// レコーダ一覧をツールバーに設定
			toolBar.updateRecorderComboBox();

			// 予約一覧のリフレッシュ
			loadRdReservesAll(false, null);		// toolBarの内容がリセットされているので recId = null で

			// 録画タイトルの取得
			loadRdTitlesAll(false, null);

			// レコーダのエンコーダ表示の更新
			this.redrawRecorderEncoderEntry();

			// レコーダ一覧をCHコード設定のコンボボックスに設定
			chdatsetting.updateRecorderComboBox();

			// Web番組表の再構築（予約マークのリフレッシュ）
			paper.updateReserveBorder(null);
			listed.updateReserveMark();

			timer_now.start();
		}

	}


	/***
	 * CH設定の内部クラス
	 */
	private class VWChannelSettingView extends AbsChannelSettingView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return Viewer.this.env; }
		@Override
		protected TVProgramList getProgPlugins() { return progPlugins; }

		// ログ関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }
		@Override
		protected VWColorChooserDialog getCcWin() { return ccwin; }

		@Override
		protected void ringBeep() {
			Viewer.this.ringBeep();
		}
		@Override
		protected void updateProgPlugin() {

			timer_now.pause();

			// 設定を保存（プラグイン内部の設定はChannelSettingPanel内で実施）
			env.save();

			// Web番組表プラグインのリフレッシュ
			setSelectedProgPlugin();
			initProgPluginAll();

			// CHソート設定に反映
			chsortsetting.updateChannelSortTable();

			// CHコンバート設定をリフレッシュ
			chconvsetting.updateChannelConvertTable();

			// CHコード設定にも反映
			chdatsetting.updateChannelDatTable();

			// 番組情報の再取得
			TVProgramUtils.invalidateProgCache(true);
			loadTVProgram(false,LoadFor.ALL, false);	// 部品呼び出し
			TVProgramUtils.invalidateProgCache(false);

			// ツールバーに反映
			toolBar.setPagerItems();

			// 新聞描画枠のリセット
			paper.clearPanel();
			paper.buildMainViewByDate();

			// サイドツリーの再構築
			paper.redrawTreeByCenter();

			listed.redrawTreeByCenter();

			// 再構築
			paper.reselectTree();
			listed.reselectTree();

			timer_now.start();
		}

	}

	/***
	 * CHコード設定の内部クラス
	 */
	private class VWChannelDatSettingView extends AbsChannelDatSettingView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return Viewer.this.env; }
		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }
		@Override
		protected ChannelSort getChannelSort() { return chsort; }
		@Override
		protected HDDRecorderList getHDDRecorderList() { return recorders; }

		// ログ関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() {
			Viewer.this.ringBeep();
		}

	}

	/**
	 * CHソート設定タブの内部クラス
	 */
	private class VWChannelSortView extends AbsChannelSortView {

		private static final long serialVersionUID = 1L;

		@Override
		protected Env getEnv() { return Viewer.this.env; }
		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }
		@Override
		protected ChannelSort getChannelSort() { return chsort; }

		// ログ関連
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		@Override
		protected void updProc() {

			timer_now.pause();

			env.save();

			toolBar.setPagerItems();
			toolBar.setSelectedPagerIndex(toolBar.getSelectedPagerIndex());

			// 新聞描画枠のリセット
			paper.clearPanel();
			paper.buildMainViewByDate();

			// サイドツリーの再構築
			paper.redrawTreeByCenter();

			listed.redrawTreeByCenter();

			// 再描画
			paper.reselectTree();
			listed.reselectTree();

			timer_now.start();
		}
	}

	/**
	 * CHｺﾝﾊﾞｰﾄ設定タブの内部クラス
	 */
	private class VWChannelConvertView extends AbsChannelConvertView {

		private static final long serialVersionUID = 1L;

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected TVProgramList getProgPlugins() { return progPlugins; }
		@Override
		protected ChannelConvert getChannelConvert() { return chconv; }

	}

	/***
	 * 予約ウィンドウの内部クラス
	 */
	private class VWReserveDialog extends AbsReserveDialog {

		private static final long serialVersionUID = 1L;

		// コンストラクタ
		public VWReserveDialog(int x, int y) {
			super(x, y);
		}

		// 環境設定の入れ物を渡す
		@Override
		protected Env getEnv() { return env; }
		@Override
		protected TVProgramList getTVProgramList() { return tvprograms; }
		@Override
		protected HDDRecorderList getRecorderList() { return recorders; }
		@Override
		protected AVSetting getAVSetting() { return avs; }
		@Override
		protected CHAVSetting getCHAVSetting() { return chavs; }

		// ログ関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		// コンポーネントを渡す
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * ReserveDialog内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected LikeReserveList findLikeReserves(ProgDetailList tvd, String keyword, int threshold) {
			return Viewer.this.findLikeReserves(tvd, keyword, threshold);
		}
	}

	/***
	 * タイトルウィンドウの内部クラス
	 */
	private class VWTitleDialog extends AbsTitleDialog {

		private static final long serialVersionUID = 1L;

		// コンストラクタ
		public VWTitleDialog(boolean b) {
			super(b);
		}

		// 環境設定の入れ物を渡す
		// ログ関連
		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }

		/*
		 * ReserveDialog内でのイベントから呼び出されるメソッド群
		 */

		@Override
		protected HDDRecorder getSelectedRecorder() { return titled.getSelectedRecorder(); }
	}

	/**
	 * 新聞の表示形式を操作するダイアログ
	 */
	private class VWPaperColorsDialog extends AbsPaperColorsDialog {

		private static final long serialVersionUID = 1L;

		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected PaperColorsMap getPaperColorMap() { return pColors; }

		@Override
		protected VWColorChooserDialog getCCWin() { return ccwin; }

		/*
		 * PaperColorsDialog内でのイベントから呼び出されるメソッド群
		 */

		// 背景色設定の反映
		@Override
		protected void updatePaperColors(Env ec,PaperColorsMap pc) {
			paper.updateColors(ec,pc);
		}

		// フォント設定の反映
		@Override
		protected void updatePaperFonts(Env ec) {
			paper.updateFonts(ec);
		}

		// サイズ設定の反映
		@Override
		protected void updatePaperBounds(Env ec, Bounds bc) {
			paper.updateBounds(ec,bc);
		}

		// 再描画？
		@Override
		protected void updatePaperRepaint() {
			paper.updateRepaint();
		}
	}

	/**
	 * キーワード検索ウィンドウの内部クラス
	 */
	private class VWKeywordDialog extends AbsKeywordDialog {

		private static final long serialVersionUID = 1L;

		@Override
		void preview(SearchKey search) {
			// 検索実行
			if (search.alTarget.size() > 0) {
				mainWindow.setSelectedTab(MWinTab.LISTED);
				listed.redrawListByPreview(search);
			}
		}

		@Override
		ListedColumnInfoList getLvItemEnv() {
			return lvitems;
		}
	}

	/**
	 * 延長警告管理ウィンドウの内部クラス
	 */
	private class VWExtensionDialog extends AbsExtensionDialog {

		private static final long serialVersionUID = 1L;

		@Override
		void preview(SearchKey search) {
			// 検索実行
			if (search.alTarget.size() > 0) {
				mainWindow.setSelectedTab(MWinTab.LISTED);
				listed.redrawListByPreview(search);
			}
		}

		@Override
		ListedColumnInfoList getLvItemEnv() {
			return lvitems;
		}
	}

	/***
	 *
	 * ツールバーの内部クラス
	 *
	 */
	private class VWToolBar extends AbsToolBar {

		private static final long serialVersionUID = 1L;

		@Override
		protected Env getEnv() { return env; }
		@Override
		protected Bounds getBoundsEnv() { return bounds; }
		@Override
		protected ToolBarIconInfoList getTbIconEnv(){ return tbicons; }
		@Override
		protected TVProgramList getTVPrograms() { return tvprograms; }
		@Override
		protected ChannelSort getChannelSort() { return chsort; }
		@Override
		protected HDDRecorderList getHDDRecorders() { return recorders; }

		@Override
		protected StatusWindow getStWin() { return stwin; }
		@Override
		protected StatusTextArea getMWin() { return mwin; }
		@Override
		protected Component getParentComponent() { return Viewer.this; }

		@Override
		protected void ringBeep() { Viewer.this.ringBeep(); }

		/*
		 * 前のページに移動する
		 * @see tainavi.AbsToolBar#moveToPrevPage()
		 */
		@Override
		protected void moveToPrevPage(){
			if (!isPrevPageEnabled())
				return;

			if (paper.isByCenterMode())
				paper.moveToPrevDatePage();
			else
				moveToPrevCenterPage();
		}

		/*
		 * 前のページに移動可能かを返す
		 * @see tainavi.AbsToolBar#isPrevPageEnabled()
		 */
		@Override
		protected boolean isPrevPageEnabled(){
			if (!mainWindow.isTabSelected(MWinTab.PAPER))
				return false;

			if (paper.isByCenterMode())
				return paper.isPrevDatePageEnabled();
			else
				return isPrevCenterPageEnabled();
		}

		/*
		 * 次のページに移動する
		 * @see tainavi.AbsToolBar#moveToNextPage()
		 */
		@Override
		protected void moveToNextPage(){
			if (!isNextPageEnabled())
				return;

			if (paper.isByCenterMode())
				paper.moveToNextDatePage();
			else
				moveToNextCenterPage();
		}

		/*
		 * 次のページに移動可能かを返す
		 * @see tainavi.AbsToolBar#isNextPageEnabled()
		 */
		@Override
		protected boolean isNextPageEnabled(){
			if (!mainWindow.isTabSelected(MWinTab.PAPER))
				return false;

			if (paper.isByCenterMode())
				return paper.isNextDatePageEnabled();
			else
				return isNextCenterPageEnabled();
		}

		@Override
		protected boolean doKeywordSerach(SearchKey search, String kStr, String sStr, boolean doFilter) {

			timer_now.pause();

			if ( mainWindow.getSelectedTab() == MWinTab.RSVED ) {
				reserved.redrawListByKeywordFilter(search, kStr);
			}
			else if ( mainWindow.getSelectedTab() == MWinTab.RECED ) {
				if ( ! doFilter ) {
					recorded.redrawListByKeyword(search, kStr);
				}
				else {
					recorded.redrawListByErrorFilter();
				}
			}
			else if ( mainWindow.getSelectedTab() == MWinTab.TITLED ) {
				titled.redrawListByKeywordFilter(search, kStr, sStr);
			}
			else {
				if ( search != null ) {
					mainWindow.setSelectedTab(MWinTab.LISTED);
					if ( doFilter ) {
						// 絞り込み検索
						listed.clearSelection();
						listed.redrawListByKeywordFilter(search, kStr);
					}
					else if (sStr != null) {
						// 過去ログ検索
						searchPassedProgram(search, sStr);
						listed.clearSelection();
						listed.redrawListBySearched(ProgType.PASSED, 0);

						listed.redrawTreeByHistory();
					}
					else {
						// キーワード検索
						listed.clearSelection();
						listed.redrawListByKeywordDyn(search, kStr);
					}
				}
			}

			timer_now.start();

			return true;
		}

		/*
		 * 検索ダイアログを表示する
		 *
		 * @see tainavi.AbsToolBar#openSearchDialog()
		 */
		@Override
		protected void openSearchDialog(JButton jbutton) {
			timer_now.pause();

			if (swdialog == null)
				swdialog = new VWSearchWordDialog(getChannelSort(), swlist);

			if (swdialog.isVisible()){
				swdialog.setVisible(false);
			}
			else{
				int no = jComboBox_keyword.getSelectedIndex();
				SearchWordItem swi= no < 1 ? null : swlist.getWordList().get(no-1);
				swdialog.open(this, jbutton, jTextField_keyword.getText(), swi);
			}

			timer_now.start();
		}

		@Override
		protected boolean doBatchReserve() {
			timer_now.pause();
			listed.doBatchReserve();
			timer_now.start();
			return true;
		}

		@Override
		protected boolean jumpToNow() {
			timer_now.pause();
			if ( ! mainWindow.isTabSelected(MWinTab.PAPER) ) {
				mainWindow.setSelectedTab(MWinTab.PAPER);
			}
			paper.jumpToNow();
			timer_now.start();
			return true;
		}

		@Override
		protected boolean jumpToPassed(String passed) {
			timer_now.pause();
			boolean b = paper.jumpToPassed(passed);
			timer_now.start();
			return b;
		}

		@Override
		protected void redrawByPager() {
			timer_now.pause();
			paper.redrawByPager();
			timer_now.start();
		}

		@Override
		protected void toggleMatchBorder(boolean b) {
			timer_now.pause();
			if ( mainWindow.isTabSelected(MWinTab.LISTED) ) {
				listed.toggleReservedBackground(b);
			}
			else if ( mainWindow.isTabSelected(MWinTab.PAPER) ) {
				paper.toggleMatchBorder(b);
			}
			timer_now.start();
		}

		/*
		 * 実行OFFの予約枠を表示するかどうかを切り替える
		 * @see tainavi.AbsToolBar#toggleOffReserve(boolean)
		 * @param boolean b 表示するかどうか
		 */
		@Override
		protected void toggleOffReserve(boolean b) {
			timer_now.pause();
			bounds.setShowOffReserve(b);
			listed.updateReserveMark();
			paper.redrawByCurrentSelection();
			timer_now.start();
		}

		@Override
		protected void setPaperColorDialogVisible(boolean b) {
			//paper.stopTimer(); xxxx
			timer_now.pause();
			CommonSwingUtils.setLocationCenter(Viewer.this,pcwin);
			pcwin.setVisible(true);
			timer_now.start();
		}

		@Override
		protected void setPaperZoom(int n) {
			timer_now.pause();
			paper.setZoom(n);
			timer_now.start();
		}

		@Override
		protected boolean recorderSelectorChanged() {

			timer_now.pause();

			if (mainWindow.isTabSelected(MWinTab.LISTED)) {
				listed.updateReserveMark();
				listed.selectBatchTarget();
			}
			else if (mainWindow.isTabSelected(MWinTab.RSVED)) {
				reserved.redrawReservedList();
			}
			else if (mainWindow.isTabSelected(MWinTab.RECED)) {
				recorded.redrawRecordedList();
			}
			else if (mainWindow.isTabSelected(MWinTab.TITLED)) {
				titled.redrawTitleList();
			}

			/*
			// 新聞形式の予約枠を書き換えるかもよ？
			if (env.getEffectComboToPaper()) {
				paper.updateReserveBorder(null);
			}
			*/

			timer_now.start();

			return true;
		}

		@Override
		protected void takeSnapShot() {

			timer_now.pause();

			Viewer.this.getSnapshot(getSelectedPagerIndex(),getPagerCount());

			timer_now.start();
		}

		@Override
		protected void setStatusVisible(boolean b) {
			Viewer.this.setStatusVisible(b);
		}

		@Override
		protected void setFullScreen(boolean b) {
			Viewer.this.setFullScreen(b);
		}

		@Override
		protected boolean isTabSelected(MWinTab tab) {
			return mainWindow.isTabSelected(tab);
		}

		@Override
		protected boolean addKeywordSearch(String label, SearchKey search) {

			timer_now.pause();

			AbsKeywordDialog kD = new VWKeywordDialog();
			CommonSwingUtils.setLocationCenter(Viewer.this,kD);

			kD.open(label, srKeys, srGrps, search);
			kD.setVisible(true);

			if (kD.isRegistered()) {
				// 検索結果の再構築
				mpList.clear(env.getDisableFazzySearch(), env.getDisableFazzySearchReverse());
				mpList.build(tvprograms, trKeys.getTraceKeys(), srKeys.getSearchKeys());

				// ツリーに反映する
				listed.redrawTreeByKeyword();

				mainWindow.setSelectedTab(MWinTab.LISTED);
			}

			timer_now.start();

			return true;
		}

		@Override
		protected boolean doLoadTVProgram(String selected) {
			LoadFor lf = (selected != null) ? LoadFor.get(selected) : LoadFor.ALL;
			boolean b = Viewer.this.doLoadTVProgram(true, lf, env.getDownloadProgramInBackground());

			if ( b && lf == LoadFor.CSwSD ) {
				// ロード後シャットダウン
				CommonUtils.executeCommand(env.getShutdownCmd());
			}

//			Viewer.this.doRedrawTVProgram();	// か き な お し

			return b;
		}

		@Override
		protected boolean doLoadRdRecorder(String selected) {
			timer_now.pause();

			LoadRsvedFor lrf = (selected != null) ? LoadRsvedFor.get(selected) : null;
			boolean b = Viewer.this.doLoadRdRecorder(lrf);

			timer_now.start();
			return b;
		}
	}

	/*******************************************************************************
	 * ハンドラ―メソッド
	 ******************************************************************************/

	/**
	 * なんかよくわからないもの
	 */
	@Override
	public void stateChanged(ChangeEvent e){
		StdAppendMessage("イベント発生");
	}

	/**
	 * ツールバーでレコーダの選択イベントが発生
	 */
	@Override
	public void valueChanged(HDDRecorderSelectionEvent e) {
		// 選択中のレコーダ情報を保存する
		src_recsel = (HDDRecorderSelectable) e.getSource();
	}

	private String getSelectedMySelf() {
		return ( src_recsel!=null ? src_recsel.getSelectedMySelf() : null );
	}

	private HDDRecorderList getSelectedRecorderList() {
		return ( src_recsel!=null ? src_recsel.getSelectedList() : null );
	}

	private HDDRecorderSelectable src_recsel;

	/**
	 * レコーダ情報の変更イベントが発生
	 */
	@Override
	public void stateChanged(HDDRecorderChangeEvent e) {
		// 未実装
	}

	/**
	 *
	 */
	@Override
	public void cancelRised(CancelEvent e) {
		if ( mainWindow.isTabSelected(MWinTab.RSVED) ) {
			if ( e.getCause() == CancelEvent.Cause.TOOLBAR_SEARCH ) {
				reserved.redrawListByKeywordFilter(null,null);
			}
		}
		else if ( mainWindow.isTabSelected(MWinTab.RECED) ) {
			if ( e.getCause() == CancelEvent.Cause.TOOLBAR_SEARCH ) {
				recorded.redrawListByKeyword(null,null);
			}
		}
		else if ( mainWindow.isTabSelected(MWinTab.TITLED) ) {
			if ( e.getCause() == CancelEvent.Cause.TOOLBAR_SEARCH ) {
				titled.redrawListByKeywordFilter(null,null,null);
			}
		}
	}

	/**
	 * タイマーイベントが発生
	 */
	@Override
	public void timerRised(TickTimerRiseEvent e) {
		if (env.getDebug()) System.out.println("Timer Rised: now="+CommonUtils.getDateTimeYMDx(e.getCalendar()));
		setTitleBar();
		checkReservesForNotify();
		checkPickProgsForNotify();
		checkDownloadProgramTime(e.getCalendar());
	}

	/*
	 * 開始間もなくの予約を通知する
	 */
	private void checkReservesForNotify(){
		if (!env.getNotifyBeforeProgStart())
			return;
		if (reserved == null)
			return;
		ArrayList<String> list = new ArrayList<String>();
		reserved.getReservesToNotify(list);

		for (String s : list){
			trayicon.displayMessage("TinyBannaviからのお知らせ", s, MessageType.INFO);
		}
	}

	/*
	 * 開始間もなくのピックアップ番組を通知する
	 */
	private void checkPickProgsForNotify(){
		if (!env.getNotifyBeforePickProgStart())
			return;

		if (listed == null)
			return;

		ArrayList<String> list = new ArrayList<String>();
		listed.getPickupsToNotify(list);

		for (String s : list){
			trayicon.displayMessage("TinyBannaviからのお知らせ", s, MessageType.INFO);
		}
	}

	/*******************************************************************************
	 * 共通メソッド群
	 ******************************************************************************/

	/**
	 * 類似予約をさがす
	 */
	private LikeReserveList findLikeReserves(ProgDetailList tvd, String keyword, int threshold) {

		String keywordVal = null;
		int thresholdVal = 0;

		// 曖昧検索のための初期化
		if ( ! env.getDisableFazzySearch() ) {
			if ( threshold > 0 ) {
				// キーワード指定がある場合
				keywordVal = TraceProgram.replacePop(keyword);
				thresholdVal = threshold;
			}
			else {
				// キーワード指定がない場合
				keywordVal = tvd.titlePop;
				thresholdVal = env.getDefaultFazzyThreshold();
			}
		}

		// 検索実行
		return recorders.findLikeReserves(tvd, keywordVal, thresholdVal, env.getRangeLikeRsv(), ! env.getDisableFazzySearchReverse());
	}

	/***
	 *
	 * リスト・新聞形式共通
	 *
	 */

	/**
	 *  番組追跡への追加とgoogle検索
	 */
	public void showPopupMenuForProgram(
			final JComponent comp, 	final ProgDetailList tvd, final int x, final int y, final String clickedDateTime)
	{
		JPopupMenu pop = new JPopupMenu();

		String myself = toolBar.getSelectedRecorder();

		// 予約関係のメニューを追加する
		addReserveMenuToPopup(pop, tvd, myself, clickedDateTime);

		// ジャンプする
		if (pop.getComponentCount() > 0)
			pop.addSeparator();
		addJumpMenuToPopup(pop, tvd);

		pop.addSeparator();

		// 番組追跡へ追加する
		addTraceMenuToPopup(pop, tvd);

		// キーワード検索へ追加する
		addKeywordMenuToPopup(pop, tvd);

		// ピックアップへ追加する
		addPickupMenuToPopup(pop, tvd);

		pop.addSeparator();

		// ブラウザー呼び出しメニューを追加する
		addBrowseMenuToPopup(pop, tvd);

		pop.addSeparator();

		// クリップボードへコピーする
		addClipboardMenuToPopup(pop, tvd);

		pop.addSeparator();

		// 延長感染源へ追加する
		addInfectionSourceMenuToPopup(pop, tvd);

		// 視聴する
		addChannelMenuToPopup(pop, tvd);

		pop.show(comp, x, y);
	}

	/*
	 * 予約メニューを追加する
	 */
	public void addReserveMenuToPopup( JPopupMenu pop, final ProgDetailList tvd, final String myself, final String clickedDateTime ){
		// 類似予約検索
		LikeReserveList likeRsvList;
		if ( env.getDisableFazzySearch() ) {
			likeRsvList = recorders.findLikeReserves(tvd, null, 0, env.getRangeLikeRsv(), false);
		}
		else {
			likeRsvList = recorders.findLikeReserves(tvd, tvd.titlePop, env.getDefaultFazzyThreshold(), env.getRangeLikeRsv(), ! env.getDisableFazzySearchReverse());
		}

		// 重複予約検索
		LikeReserveList overlapRsvList = recorders.findOverlapReserves(tvd, clickedDateTime, true, env.getOverlapUp());

		// 類似と重複で被るものを重複から除外
		for ( LikeReserveItem item : likeRsvList ) {
			overlapRsvList.removeDup(item);
		}

		// 予約する
		if ( tvd.type == ProgType.PASSED ||
				(tvd.type == ProgType.PROG && tvd.subtype == ProgSubtype.RADIO) ||
				recorders.size() == 0 ) {
			// 過去ログは処理対象外です
		}
		else {
			String target;
			LikeReserveItem item = likeRsvList.getClosest(myself);
			if ( env.getGivePriorityToReserved() && item != null && item.isCandidate(env.getOverlapUp()) ) {
				target = "予約を編集する";
			}
			else {
				target = "新規予約を登録する";
			}

			JMenuItem menuItem = new JMenuItem(String.format("%s【%s %s - %s(%s)】",target,tvd.accurateDate,tvd.start,tvd.title,tvd.center));
			{
				menuItem.setForeground(Color.BLUE);
				Font f = menuItem.getFont();
				menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
			}

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					CommonSwingUtils.setLocationCenter(mainWindow,rdialog);

					if ( rdialog.open(tvd) ) {
						rdialog.setVisible(true);
					}
					else {
						rdialog.setVisible(false);
					}

					//
					if (rdialog.isSucceededReserve()) {
						listed.updateReserveMark();
						paper.updateReserveBorder(tvd.center);
						reserved.redrawReservedList();
					}
				}
			});
			pop.add(menuItem);
		}

		// 隣接予約を編集する
		{
			for ( final LikeReserveItem item : overlapRsvList ) {

				if ( ! item.getRec().Myself().equals(toolBar.getSelectedRecorder()) ) {
					continue;	// 選択中のレコーダ以外はスルーで
				}

				{
					ReserveList rsv = item.getRsv();
					String start = CommonUtils.getDateTimeW(CommonUtils.getCalendar(rsv.getStartDateTime()));
					JMenuItem menuItem = new JMenuItem(String.format("隣接予約を上書する【%s - %s(%s)】",start,rsv.getTitle(),rsv.getCh_name()));

					menuItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {

							CommonSwingUtils.setLocationCenter(mainWindow,rdialog);

							if ( rdialog.open(tvd,item) ) {
								rdialog.setVisible(true);
							}
							else {
								rdialog.setVisible(false);
							}

							//
							if (rdialog.isSucceededReserve()) {
								listed.updateReserveMark();
								paper.updateReserveBorder(tvd.center);
								reserved.redrawReservedList();
							}
						}
					});
					pop.add(menuItem);
				}
			}
		}

		// 予約実行ON・OFF
		if ( tvd.type != ProgType.PASSED )
		{
			for ( int n=0; n<2; n++ ) {

				LikeReserveList rsvList = null;
				if ( n == 0 ) {
					rsvList = likeRsvList;
				}
				else {
					rsvList = overlapRsvList;
				}

				if (rsvList.size() > 0)
					pop.addSeparator();

				for ( LikeReserveItem rsvItem : rsvList ) {

					final boolean fexec = rsvItem.getRsv().getExec();
					final String start = rsvItem.getRsv().getAhh()+":"+rsvItem.getRsv().getAmm();
					final String title = rsvItem.getRsv().getTitle();
					final String chnam = rsvItem.getRsv().getCh_name();
					final String rsvId = rsvItem.getRsv().getId();
					final String recId = rsvItem.getRec().Myself();

					pop.add(getExecOnOffMenuItem(fexec,start,title,chnam,rsvId,recId,n));
				}
			}
		}

		// 削除する
		if ( tvd.type != ProgType.PASSED )	// 過去ログは処理対象外です
		{
			for ( int n=0; n<2; n++ ) {

				LikeReserveList rsvList = null;
				if ( n == 0 ) {
					rsvList = likeRsvList;
				}
				else {
					rsvList = overlapRsvList;
				}

				if (rsvList.size() > 0)
					pop.addSeparator();

				for ( LikeReserveItem rsvItem : rsvList ) {

					final String start = rsvItem.getRsv().getAhh()+":"+rsvItem.getRsv().getAmm();
					final String title = rsvItem.getRsv().getTitle();
					final String chnam = rsvItem.getRsv().getCh_name();
					final String rsvId = rsvItem.getRsv().getId();
					final String recId = rsvItem.getRec().Myself();

					pop.add(getRemoveRsvMenuItem(start, title,chnam,rsvId,recId,n));
				}
			}
		}
	}

	/*
	 *  ジャンプメニューを追加する
	 */
	public void addJumpMenuToPopup(JPopupMenu pop, final ProgDetailList tvd){
		if ( mainWindow.isTabSelected(MWinTab.LISTED) ) {
			pop.add(getJumpMenuItem(tvd.title,tvd.center,tvd.accurateDate+" "+tvd.start));
		}
		if ( mainWindow.isTabSelected(MWinTab.LISTED) || mainWindow.isTabSelected(MWinTab.PAPER) ) {
			JMenuItem mi = getJumpToLastWeekMenuItem(tvd.title,tvd.center,tvd.startDateTime);
			if ( mi != null ) {
				pop.add(mi);
			}
		}
	}

	/*
	 * 追跡メニューを追加する
	 */
	public void addTraceMenuToPopup( JPopupMenu pop, final ProgDetailList tvd ){
		final String label = TraceProgram.getNewLabel(tvd.title, tvd.center);
		JMenuItem menuItem = new JMenuItem("番組追跡への追加【"+label+"】");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//
				VWTraceKeyDialog tD = new VWTraceKeyDialog(0,0);
				CommonSwingUtils.setLocationCenter(mainWindow,tD);

				tD.open(trKeys, tvd, env.getDefaultFazzyThreshold());
				tD.setVisible(true);

				if (tD.isRegistered()) {
					//
					trKeys.save();

					// 検索結果の再構築
					mpList.clear(env.getDisableFazzySearch(), env.getDisableFazzySearchReverse());
					mpList.build(tvprograms, trKeys.getTraceKeys(), srKeys.getSearchKeys());

					// ツリーに反映する
					listed.redrawTreeByTrace();

					// 表示を更新する
					paper.updateBangumiColumns();
					listed.reselectTree();

					mwin.appendMessage("番組追跡へ追加しました【"+label+"】");
				}
				else {
					trKeys.remove(label);
				}
			}
		});
		pop.add(menuItem);
	}

	/*
	 * キーワードメニューを追加する
	 */
	public void addKeywordMenuToPopup( JPopupMenu pop, final ProgDetailList tvd ){
		final String label = tvd.title+" ("+tvd.center+")";
		JMenuItem menuItem = new JMenuItem("キーワード検索への追加【"+label+"】");
		menuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){

				// 「キーワード検索の設定」ウィンドウを開く

				AbsKeywordDialog kD = new VWKeywordDialog();
				CommonSwingUtils.setLocationCenter(mainWindow,kD);

				kD.open(srKeys, srGrps, tvd);
				kD.setVisible(true);

				if (kD.isRegistered()) {
					// 検索結果の再構築
					mpList.clear(env.getDisableFazzySearch(), env.getDisableFazzySearchReverse());
					mpList.build(tvprograms, trKeys.getTraceKeys(), srKeys.getSearchKeys());

					// ツリーに反映する
					listed.redrawTreeByKeyword();

					// 表示を更新する
					paper.updateBangumiColumns();
					listed.reselectTree();

					mwin.appendMessage("キーワード検索へ追加しました【"+label+"】");
				}
			}
		});
		pop.add(menuItem);
	}

	/*
	 * ピックアップメニューを追加する
	 */
	public void addPickupMenuToPopup( JPopupMenu pop, final ProgDetailList tvd ){
		{
			boolean isRemoveItem = false;
			if ( mainWindow.isTabSelected(MWinTab.LISTED) && tvd.type == ProgType.PICKED ) {
				isRemoveItem = true;
			}
			else {
				PickedProgram tvp = tvprograms.getPickup();
				if ( tvp != null ) {
					isRemoveItem = tvp.remove(tvd, tvd.center, tvd.accurateDate, false);
				}
			}

			if ( ! isRemoveItem )	// 過去ログは処理対象外です
			{
				final String label = String.format("%s(%s)",tvd.title,tvd.center);
				JMenuItem menuItem = new JMenuItem(String.format("ピックアップへの追加【%s %s - %s】",tvd.accurateDate,tvd.start,label));
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//
						PickedProgram tvp = tvprograms.getPickup();
						if ( tvp != null ) {
							tvp.refresh();
							tvp.add(tvd);
							tvp.save();

							if ( listed.isNodeSelected(JTreeLabel.Nodes.PICKUP) || listed.isNodeSelected(JTreeLabel.Nodes.STANDBY) ) {
								// ピックアップノードが選択されていたらリストを更新する
								listed.reselectTree();
							}

							listed.updateReserveMark();
							listed.refocus();
							paper.updateReserveBorder(tvd.center);
							mwin.appendMessage("【ピックアップ】追加しました： "+tvd.title+" ("+tvd.center+")");
							return;
						}
					}
				});
				pop.add(menuItem);
			}
			else {
				final String label = tvd.title+" ("+tvd.center+")";
				JMenuItem menuItem = new JMenuItem("ピックアップからの削除【"+label+"】");
				menuItem.setForeground(Color.RED);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						//
						PickedProgram tvp = tvprograms.getPickup();
						if ( tvp != null ) {
							tvp.refresh();
							tvp.remove(tvd, tvd.center, tvd.accurateDate, true);
							tvp.save();

							if ( listed.isNodeSelected(JTreeLabel.Nodes.PICKUP) || listed.isNodeSelected(JTreeLabel.Nodes.STANDBY) ) {
								// ピックアップノードが選択されていたらリストを更新する
								listed.reselectTree();
							}

							listed.updateReserveMark();
							paper.updateReserveBorder(tvd.center);
							mwin.appendMessage("【ピックアップ】削除しました： "+tvd.title+" ("+tvd.center+")");
							return;
						}
					}
				});
				pop.add(menuItem);
			}
		}
	}
	/*
	 *  ブラウザー呼び出しメニューを追加する
	 */
	public void addBrowseMenuToPopup( JPopupMenu pop,	final ProgDetailList tvd ){
		for (final TextValueSet tv : env.getTvCommand()) {
			JMenuItem menuItem = new JMenuItem(tv.getText());

			String body = "";
			Matcher ma = Pattern.compile("^(.*)[ 　]").matcher(tvd.title);
			if (ma.find())
				body = ma.group(1);

			String escepedTitle = "";
			String escepedChName = "";
			String escepedDetail = "";
			String escepedBody = "";
			try {
				escepedTitle = URLEncoder.encode(tvd.title,"UTF-8");
				escepedDetail = URLEncoder.encode(tvd.detail,"UTF-8");
				escepedChName = URLEncoder.encode(tvd.center,"UTF-8");
				escepedBody = URLEncoder.encode(body,"UTF-8");
			} catch (UnsupportedEncodingException e2) {
				//
			}

			String cmd = tv.getValue();
			if ( cmd.matches(".*%DETAILURL%.*") ) {
				if ( tvd.link == null || tvd.link.length() == 0 ) {
					// このメニューは利用できません！
					menuItem.setEnabled(false);
					menuItem.setForeground(Color.lightGray);
				}
			}
			cmd = cmd.replaceAll("%ENCTITLE%", escepedTitle);
			cmd = cmd.replaceAll("%ENCDETAIL%", escepedDetail);
			cmd = cmd.replaceAll("%ENCCHNAME%", escepedChName);
			cmd = cmd.replaceAll("%TITLE%", tvd.title);
			cmd = cmd.replaceAll("%DETAIL%", tvd.detail);
			cmd = cmd.replaceAll("%CHNAME%", tvd.center);
			cmd = cmd.replaceAll("%DATE%", tvd.accurateDate);
			cmd = cmd.replaceAll("%START%", tvd.start);
			cmd = cmd.replaceAll("%END%", tvd.end);
			cmd = cmd.replaceAll("%DETAILURL%", tvd.link);
			cmd = cmd.replaceAll("%SYOBODETAILURL%",  tvd.linkSyobo != null ? tvd.linkSyobo : "");
			cmd = cmd.replaceAll("%ENCBODY%", escepedBody);

			// CHAN-TORU対応
			if ( cmd.matches(".*%TVKAREACODE%.*") && cmd.matches(".*%TVKPID%.*") ) {
				Center cr = null;
				for ( TVProgram tvp : progPlugins ) {
					if ( tvp.getTVProgramId().startsWith("Gガイド.テレビ王国") ) {
						for ( Center tempcr : tvp.getCRlist() ) {
							// CH設定が完了している必要がある
							if ( tvp.getSubtype() == ProgSubtype.TERRA && tvp.getSelectedCode().equals(TVProgram.allCode) && ! tempcr.getAreaCode().equals(TVProgram.bsCode) ) {
								// 地域が全国の地デジの場合のみ、有効局かどうかを確認する必要がある
								if ( tempcr.getCenter().equals(tvd.center) && tempcr.getOrder() > 0 ) {
									// このメニューは利用できます！
									cr = tempcr;
									break;
								}
							}
							else {
								if ( tempcr.getCenter().equals(tvd.center) ) {
									// このメニューは利用できます！
									cr = tempcr;
									break;
								}
							}
						}

						if ( cr != null ) {
							break;
						}
					}
				}
				if ( cr != null ) {
					String areacode = null;
					String centercode = cr.getLink();
					String cat = cr.getLink().substring(0,1);
					if ( cat.equals("1") ) {
						areacode = cr.getAreaCode();
					}
					else {
						if ( cat.equals("4") ) {
							cat = "5";
						}
						else if ( cat.equals("5") ) {
							cat = "4";
						}
						areacode = "10";
					}

					cmd = cmd.replaceAll("%TVKAREACODE%", areacode);
					cmd = cmd.replaceAll("%TVKCAT%", cat);
					cmd = cmd.replaceAll("%TVKPID%", centercode+CommonUtils.getDateTimeYMD(CommonUtils.getCalendar(tvd.startDateTime)).replaceFirst("..$", ""));
					System.out.println("[DEBUG] "+cmd);

					menuItem.setEnabled(true);
					menuItem.setForeground(Color.BLACK);
				}
				else {
					menuItem.setEnabled(false);
					menuItem.setForeground(Color.lightGray);
				}
			}

			if (cmd.isEmpty())
				continue;

			final String run = cmd;

			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						if (run.indexOf("http") == 0) {
							Desktop desktop = Desktop.getDesktop();
							desktop.browse(new URI(run));
						}
						else {
							CommonUtils.executeCommand(run);
						}
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (URISyntaxException e1) {
						e1.printStackTrace();
					}
				}
			});

			pop.add(menuItem);
		}
	}

	/*
	 * クリップボードへコピーメニューを追加する
	 */
	public void addClipboardMenuToPopup(JPopupMenu pop, final ProgDetailList tvd){
		{
			JMenuItem menuItem = new JMenuItem("番組名をコピー【"+tvd.title+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = tvd.title;
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});
			pop.add(menuItem);
		}
		{
			JMenuItem menuItem = new JMenuItem("番組名と詳細をコピー【"+tvd.title+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = tvd.title+System.getProperty("line.separator")+tvd.detail+"\0"+tvd.getAddedDetail();
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});
			pop.add(menuItem);
		}
		{
			JMenuItem menuItem = new JMenuItem("番組情報をコピー【"+tvd.title+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = "";
					int preId = 0;
					for (ClipboardInfo cb : cbitems) {
						if (cb.getB()) {
							switch (cb.getId()) {
							case 1:
								msg += tvd.title+"\t";
								break;
							case 2:
								msg += tvd.center+"\t";
								break;
							case 3:
								msg += tvd.accurateDate+"\t";
								break;
							case 4:
								msg += tvd.start+"\t";
								break;
							case 5:
								if (preId == 4) {
									msg = msg.substring(0,msg.length()-1)+"-";
								}
								msg += tvd.end+"\t";
								break;
							case 6:
								msg += tvd.genre+"\t";
								break;
							case 7:
								msg += tvd.detail+"\0"+tvd.getAddedDetail()+"\t";
								break;
							}
						}
						preId = cb.getId();
					}
					if (msg.length() > 0) {
						msg = msg.substring(0,msg.length()-1);
					}
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});
			pop.add(menuItem);
		}


		if ( mainWindow.isTabSelected(MWinTab.LISTED) ) {
			pop.addSeparator();

			{
				JMenuItem menuItem = new JMenuItem("番組情報をCSVでコピー");
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String msg = listed.getCSV(false);
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection s = new StringSelection(msg);
						cb.setContents(s, s);
					}
				});
				pop.add(menuItem);
			}
			{
				JMenuItem menuItem = new JMenuItem("選択中の番組情報をCSVでコピー");
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						String msg = listed.getCSV(true);
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						StringSelection s = new StringSelection(msg);
						cb.setContents(s, s);
					}
				});
				pop.add(menuItem);
			}
		}
	}

	/*
	 * 感染源メニューを追加する
	 */
	public void addInfectionSourceMenuToPopup(JPopupMenu pop, final ProgDetailList tvd){
		if (
				tvd.type == ProgType.SYOBO ||
				tvd.type == ProgType.PASSED ||
				tvd.type == ProgType.PICKED ||
				(tvd.type == ProgType.PROG && tvd.subtype != ProgSubtype.RADIO)	)	// ラジオは処理対象外です
		{
			JMenuItem menuItem = new JMenuItem("延長感染源にしない【"+tvd.title+" ("+tvd.center+")】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//
					mwin.appendMessage("延長感染源を隔離します【"+tvd.title+"("+tvd.center+")】");
					//
					AbsExtensionDialog eD = new VWExtensionDialog();
					CommonSwingUtils.setLocationCenter(mainWindow,eD);

					eD.open(tvd.title,tvd.center,false,extKeys);
					eD.setVisible(true);

					if (eD.isRegistered()) {
						// 番組表の状態を更新する
						for (TVProgram tvp : tvprograms) {
							if (tvp.getType() == ProgType.PROG) {
								tvp.setExtension(null, null, false, extKeys.getSearchKeys());
							}
						}

						// ツリーに反映する
						listed.redrawTreeByExtension();

						mainWindow.setSelectedTab(MWinTab.LISTED);
					}
				}
			});
			pop.add(menuItem);
		}

		if ( tvd.type == ProgType.PASSED || (tvd.type == ProgType.PROG && tvd.subtype != ProgSubtype.RADIO) )	// ラジオは処理対象外です
		{
			JMenuItem menuItem = new JMenuItem("延長感染源にする【"+tvd.title+" ("+tvd.center+")】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//
					AbsExtensionDialog eD = new VWExtensionDialog();
					CommonSwingUtils.setLocationCenter(mainWindow,eD);

					eD.open(tvd.title,tvd.center,true,extKeys);
					eD.setVisible(true);

					if (eD.isRegistered()) {
						// 番組表の状態を更新する
						for (TVProgram tvp : tvprograms) {
							if (tvp.getType() == ProgType.PROG) {
								tvp.setExtension(null, null, false, extKeys.getSearchKeys());
							}
						}

						// ツリーに反映する
						listed.redrawTreeByExtension();

						mainWindow.setSelectedTab(MWinTab.LISTED);
					}
				}
			});
			pop.add(menuItem);
		}
	}

	/*
	 * 視聴メニューを追加する
	 */
	public void addChannelMenuToPopup( JPopupMenu pop, final ProgDetailList tvd){
		if ( tvd.type == ProgType.PROG && tvd.subtype != ProgSubtype.RADIO)	// ラジオは処理対象外です
		{
			boolean hassep = false;

			for (HDDRecorder recorder : recorders ) {

				if (recorder.ChangeChannel(null) == false) {
					continue;
				}

				final String recId = recorder.Myself();
				final String recName = recorder.getDispName();
				JMenuItem menuItem = new JMenuItem("【"+recName+"】で【"+tvd.center+"】を視聴する");

				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						for (HDDRecorder recorder : recorders ) {
							if (recorder.isMyself(recId)) {
								if (recorder.ChangeChannel(tvd.center) == false) {
									ringBeep();
									mwin.appendError("【警告】チャンネルを変更できませんでした："+recorder.getErrmsg());
								}
								else if (recorder.getErrmsg() !=null && recorder.getErrmsg().length() > 0) {
									mwin.appendError("[追加情報] "+recorder.getErrmsg());
								}
							}
						}
					}
				});

				menuItem.setEnabled(recorder.getUseChChange());

				if (!hassep){
					pop.addSeparator();
					hassep = true;
				}
				pop.add(menuItem);
			}
		}
	}

	// ピックアップへ追加する
	public boolean addToPickup(final ProgDetailList tvd) {

		if (tvd.start.equals("")) {
			// 番組情報がありません
			return false;
		}

		PickedProgram tvp = tvprograms.getPickup();
		if ( tvp == null ) {
			// ピックアップ先がありません
			return true;
		}

		// 削除かな？
		if ( tvp.remove(tvd, tvd.center, tvd.accurateDate, true) ) {
			tvp.save();
			if ( listed.isNodeSelected(JTreeLabel.Nodes.PICKUP) || listed.isNodeSelected(JTreeLabel.Nodes.STANDBY) ) {
				// ピックアップノードor予約待機ノードが選択されていたらリストを更新する
				listed.reselectTree();
				//listed.updateReserveMark();
			}
			else {
				// 予約マークだけ変えておけばいいよね
				listed.updateReserveMark();
				listed.refocus();
			}
			paper.updateReserveBorder(tvd.center);
			mwin.appendMessage("【ピックアップ】削除しました： "+tvd.title+" ("+tvd.center+")");
			return false;
		}

		// 追加です
		if ( tvd.endDateTime.compareTo(CommonUtils.getDateTime(0)) > 0 ) {
			tvp.refresh();
			tvp.add(tvd);
			tvp.save();
			if ( listed.isNodeSelected(JTreeLabel.Nodes.PICKUP) || listed.isNodeSelected(JTreeLabel.Nodes.STANDBY) ) {
				// ピックアップノードが選択されていたらリストを更新する
				listed.reselectTree();
				//listed.updateReserveMark();
			}
			else {
				listed.updateReserveMark();
				listed.refocus();
			}
			paper.updateReserveBorder(tvd.center);
			mwin.appendMessage("【ピックアップ】追加しました： "+tvd.title+" ("+tvd.center+")");
			return true;
		}

		//　過去ログは登録できないよ
		mwin.appendMessage("【ピックアップ】過去情報はピックアップできません.");
		return false;
	}

	/**
	 *  予約を削除するメニューアイテム
	 */
	private JMenuItem getRemoveRsvMenuItem(final String start, final String title, final String chnam, final String rsvId, final String recId, int n) {
		//
		JMenuItem menuItem = new JMenuItem();

		String mode = "削除";
		menuItem.setForeground(Color.RED);

		String target = ( n==0 ) ? "予約" : "隣接予約";

		String recName = recorders.getRecorderName(recId);
		menuItem.setText(String.format("%sを%sする【%s - %s(%s)/%s】",target,mode,start,title,chnam,recName));

		if ( recId.equals(toolBar.getSelectedRecorder()) ) {
			// 選択中のレコーダのものは太字に
			Font f = menuItem.getFont();
			menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
		}

		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (env.getShowWarnDialog()) {
					Container cp = getContentPane();
					int ret = JOptionPane.showConfirmDialog(cp, "削除しますか？【"+title+"("+chnam+")】（"+recName+"）", "確認", JOptionPane.YES_NO_OPTION);
					if (ret != JOptionPane.YES_OPTION) {
						return;
					}
				}

				stwin.clear();

				// 削除本体
				new SwingBackgroundWorker(false) {

					@Override
					protected Object doWorks() throws Exception {

						for (HDDRecorder recorder : recorders) {
							if (recorder.isMyself(recId)) {	// IPAddr:PortNo:RecorderIdで比較

								String title = "";
								for (ReserveList r : recorder.getReserves()) {
									if (r.getId().equals(rsvId)) {
										title = r.getTitle();
										break;
									}
								}

								stwin.appendMessage("予約を削除します："+title+"("+rsvId+")");
								//recorder.setProgressArea(stwin);
								ReserveList r = recorder.RemoveRdEntry(rsvId);	// Noで検索
								if (r != null) {
									mwin.appendMessage("正常に削除できました："+r.getTitle()+"("+r.getCh_name()+")");

									if ( ! r.getTitle().equals(title) || ! r.getId().equals(rsvId)) {
										mwin.appendError("【警告】削除結果が一致しません！："+title+"／"+r.getTitle());
									}

									if ( recorder.getUseCalendar()) {
										// カレンダーから削除する
										for ( HDDRecorder calendar : recorders ) {
											if (calendar.getType() == RecType.CALENDAR) {
												stwin.appendMessage("カレンダーから予約情報を削除します");
												//calendar.setProgressArea(stwin);
												if ( ! calendar.UpdateRdEntry(r, null)) {
													mwin.appendError("【カレンダー】"+calendar.getErrmsg());
													ringBeep();
												}
											}
										}
									}

									r = null;
								}
								else {
									mwin.appendError("削除に失敗しました："+title);
								}

								//
								if ( ! recorder.getErrmsg().equals("")) {
									mwin.appendError("【追加情報】"+recorder.getErrmsg());
									ringBeep();
								}
								break;
							}
						}
						return null;
					}

					@Override
					protected void doFinally() {
						StWinSetVisible(false);
					}
				}.execute();

				CommonSwingUtils.setLocationCenter(Viewer.this, stwin);
				StWinSetVisible(true);

				// 予約状況を更新
				listed.updateReserveMark();
				paper.updateReserveBorder(chnam);
				reserved.redrawReservedList();
			}
		});

		return menuItem;
	}




	/*
	 * 他のクラスに分離できなかったというか、しなかったというか、そんなメソッド群
	 */

	/**
	 *
	 */
	private boolean doExecOnOff(final boolean fexec, final String title, final String chnam, final String rsvId, final String recId) {

		CommonSwingUtils.setLocationCenter(mainWindow,rdialog);

		String mode = (fexec ? "ON" : "OFF");

		// 予約ON・OFFのみ
		if ( rdialog.open(recId,rsvId,fexec) ) {

			rdialog.doUpdate();

			if (rdialog.isSucceededReserve()) {
				// 予約状況を更新
				listed.updateReserveMark();
				paper.updateReserveBorder(chnam);
				reserved.redrawReservedList();

				{
					String recName = recorders.getRecorderName(recId);
					String msg = "予約を"+mode+"にしました【"+title+"("+chnam+")/"+recName+"】";
					//StdAppendMessage(msg);
					mwin.appendMessage(msg);
				}

				return true;
			}
		}

		return false;
	}

	/**
	 *  予約実行をONOFFするメニューアイテム
	 */
	private JMenuItem getExecOnOffMenuItem(final boolean fexec, final String start, final String title, final String chnam, final String rsvId, final String recId, int n) {

		JMenuItem menuItem = new JMenuItem();

		String mode;
		if ( ! fexec ) {
			mode = "ON";
			menuItem.setForeground(Color.BLUE);
		}
		else {
			mode = "OFF";
			menuItem.setForeground(Color.BLACK);
		}

		String target = ( n==0 ) ? "予約" : "隣接予約";

		String recName = recorders.getRecorderName(recId);
		menuItem.setText(String.format("%sを%sにする【%s - %s(%s)/%s】",target,mode,start,title,chnam,recName));

		if ( recId.equals(toolBar.getSelectedRecorder()) ) {
			// 選択中のレコーダのものは太字に
			Font f = menuItem.getFont();
			menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));
		}

		final String xmode = mode;
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				CommonSwingUtils.setLocationCenter(mainWindow,rdialog);

				// 予約ON・OFFのみ
				if ( rdialog.open(recId,rsvId, ! fexec) ) {

					rdialog.doUpdate();

					if (rdialog.isSucceededReserve()) {
						// 予約状況を更新
						listed.updateReserveMark();
						paper.updateReserveBorder(chnam);
						reserved.redrawReservedList();

						{
							String msg = "予約を"+xmode+"にしました【"+title+"("+chnam+")/"+recName+"】";
							StdAppendMessage(msg);
							mwin.appendMessage(msg);
						}
					}
				}
				else {
					//rdialog.setVisible(false);
				}
			}
		});

		return menuItem;
	}

	/**
	 *  新聞形式へジャンプするメニューアイテム
	 */
	private JMenuItem getJumpMenuItem(final String title, final String chnam, final String startDT) {
		JMenuItem menuItem = new JMenuItem(String.format("番組欄へジャンプする【%s - %s(%s)】",startDT,title,chnam));
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				paper.jumpToBangumi(chnam,startDT);
			}
		});
		return menuItem;
	}
	private JMenuItem getJumpToLastWeekMenuItem( final String title, final String chnam, final String startDT) {
		GregorianCalendar cal = CommonUtils.getCalendar(startDT);

		if ( cal != null ) {
			cal.add(Calendar.DATE, -7);
			final String lastdatetime = CommonUtils.getDateTimeW(cal);

			JMenuItem menuItem = new JMenuItem(String.format("先週の番組欄へジャンプする【%s - (%s)】",lastdatetime,chnam));

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					paper.jumpToBangumi(chnam,lastdatetime);
				}
			});
			return menuItem;
		}
		return null;
	}


	/*******************************************************************************
	 * レコーダの予約情報をDLする
	 ******************************************************************************/

	/***************************************
	 * ツールバートリガーによる
	 **************************************/

	/**
	 *  レコーダの予約情報をＤＬする
	 */
	private boolean doLoadRdRecorder(LoadRsvedFor lrf) {

		if ( lrf == null ) {
			return doLoadRdRecorderAll();
		}
		else {
			switch (lrf) {
			case DETAILS:
				return doLoadRdReserveDetails();
			case RECORDED:
				return doLoadRdRecorded();
			case AUTORESERVE:
				return doLoadRdAutoReserves();
			default:
				break;
			}
		}
		return false;
	}

	/**
	 * レコーダの情報を全部ＤＬする（ステータスウィンドウは自前で用意	する）
	 */
	private boolean doLoadRdRecorderAll() {

		final String myself = getSelectedMySelf();

		//
		StWinClear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				// 読み出せ！
				_loadRdRecorderAll(true,myself);

				// エンコーダ情報が更新されるかもしれないので、一覧のエンコーダ表示にも反映する
				recsetting.redrawRecorderEncoderEntry();

				// 各タブに反映する
				paper.updateReserveBorder(null);
				listed.updateReserveMark();
				reserved.redrawReservedList();
				recorded.redrawRecordedList();

				mwin.appendMessage(String.format("【予約一覧の取得処理が完了しました】 所要時間： %.2f秒",tc.end()));
				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}


	/**
	 * 設定情報をダウンロードする
	 */
	private boolean doLoadRdSettings() {

		final String myself = getSelectedMySelf();

		StWinClear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				boolean succeeded = true;

				HDDRecorderList recs;
				if ( myself != null ) {
					recs = recorders.findInstance(myself);
				}
				else {
					recs = recorders;
				}
				for ( HDDRecorder recorder : recs ) {
					switch ( recorder.getType() ) {
					case RECORDER:
						if ( ! recorder.GetRdSettings(true) ) {
							succeeded = false;
						}
						break;
					default:
						break;
					}
				}

				if ( succeeded ) {
					titled.redrawTitleList();

					mwin.appendMessage(String.format("【設定情報の取得処理が完了しました】 所要時間： %.2f秒",tc.end()));
				}
				else {
					ringBeep();
					mwin.appendError(String.format("【設定情報の取得処理に失敗しました】 所要時間： %.2f秒",tc.end()));
				}

				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}

	/**
	 * 予約一覧＋予約詳細をＤＬする
	 */
	private boolean doLoadRdReserveDetails() {

		final String myself = getSelectedMySelf();

		//
		StWinClear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				boolean succeeded = true;

				HDDRecorderList recs;
				if ( myself != null ) {
					recs = recorders.findInstance(myself);
				}
				else {
					recs = recorders;
				}
				for ( HDDRecorder recorder : recs ) {

					if ( ! recorder.isReserveListSupported() ) {
						continue;
					}

					// 各種設定の取得
					if ( ! recorder.GetRdSettings(true) ) {
						succeeded = false;
						continue;
					}

					// 予約一覧の取得
					if ( ! recorder.GetRdReserve(true) ) {
						succeeded = false;
						continue;
					}

					// レコーダから取得したエンコーダ情報で、登録済みレコーダ一覧を更新する
					setEncoderInfo2RecorderList(recorder,true);

					// 予約詳細の取得
					if ( recorder.isThereAdditionalDetails() ) {
						if ( ! recorder.GetRdReserveDetails() ) {
							succeeded = false;
							continue;
						}
					}

					// レコーダの放送局名をWeb番組表の放送局名に置き換え
					checkChNameIsRight(recorder);

					// 録画結果一覧を予約一覧に反映
					if ( recorder.isRecordedListSupported() ) {
						recorder.GetRdRecorded(false);
					}
				}

				if ( succeeded ) {
					reserved.redrawReservedList();
					recorded.redrawRecordedList();
					paper.redrawByCurrentSelection();

					mwin.appendMessage(String.format("【予約詳細の取得処理が完了しました】 所要時間： %.2f秒",tc.end()));
				}
				else {
					ringBeep();
					mwin.appendError(String.format("【予約詳細の取得処理に失敗しました】 所要時間： %.2f秒",tc.end()));
				}
				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}


	/**
	 * 録画結果一覧をＤＬする
	 */
	private boolean doLoadRdRecorded() {

		final String myself = getSelectedMySelf();

		//
		StWinClear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				boolean succeeded = true;

				HDDRecorderList recs;
				if ( myself != null ) {
					recs = recorders.findInstance(myself);
				}
				else {
					recs = recorders;
				}
				for ( HDDRecorder recorder : recs ) {
					if ( ! recorder.isRecordedListSupported() ) {
						succeeded = false;
						continue;
					}

					if ( ! recorder.GetRdRecorded(true) ) {
						succeeded = false;
					}
				}

				if ( succeeded ) {
					reserved.redrawReservedList();
					recorded.redrawRecordedList();

					mwin.appendMessage(String.format("【録画結果一覧の取得処理が完了しました】 所要時間： %.2f秒",tc.end()));
				}
				else {
					ringBeep();
					mwin.appendError(String.format("【録画結果一覧の取得処理に失敗しました】 所要時間： %.2f秒",tc.end()));
				}
				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}


	/**
	 * 録画結果一覧をＤＬする
	 */
	private boolean doLoadRdAutoReserves() {

		final String myself = getSelectedMySelf();

		//
		StWinClear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				boolean succeeded = true;

				HDDRecorderList recs;
				if ( myself != null ) {
					recs = recorders.findInstance(myself);
				}
				else {
					recs = recorders;
				}
				for ( HDDRecorder recorder : recs ) {
					if ( ! recorder.isEditAutoReserveSupported() ) {
						succeeded = false;
						continue;
					}

					if ( ! recorder.GetRdAutoReserve(true) ) {
						succeeded = false;
					}
				}

				if ( succeeded ) {
					// 再描画はここじゃないよ
					mwin.appendMessage(String.format("【自動予約一覧の取得処理が完了しました】 所要時間： %.2f秒",tc.end()));
				}
				else {
					ringBeep();
					mwin.appendError(String.format("【自動予約一覧の取得処理に失敗しました】 所要時間： %.2f秒",tc.end()));
				}

				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}

	/**
	 * 録画タイトル一覧をＤＬする
	 */
	private boolean doLoadRdTitles() {

		final String myself = getSelectedMySelf();

		//
		StWinClear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				boolean succeeded = true;

				HDDRecorderList recs;
				if ( myself != null ) {
					recs = recorders.findInstance(myself);
				}
				else {
					recs = recorders;
				}
				for ( HDDRecorder recorder : recs ) {
					switch ( recorder.getType() ) {
					case RECORDER:
						if ( ! recorder.GetRdSettings(true) ) {
							succeeded = false;
						}
						if ( ! recorder.GetRdTitles("0", true, true, true) ) {
							succeeded = false;
						}
						break;
					default:
						break;
					}
				}

				if ( succeeded ) {
					titled.redrawTitleList();

					mwin.appendMessage(String.format("【タイトル一覧の取得処理が完了しました】 所要時間： %.2f秒",tc.end()));
				}
				else {
					ringBeep();
					mwin.appendError(String.format("【タイトル一覧の取得処理に失敗しました】 所要時間： %.2f秒",tc.end()));
				}

				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}

	/***************************************
	 * 自クラス内呼び出しによる
	 **************************************/

	/**
	 * レコーダの情報を全部ＤＬする（ステータスウィンドウは呼び出し元が準備する）
	 */
	private void loadRdReservesAll(final boolean force, final String myself) {

		new SwingBackgroundWorker(true) {

			@Override
			protected Object doWorks() throws Exception {

				_loadRdRecorderAll(force,myself);

				return null;
			}

			@Override
			protected void doFinally() {
			}
		}.execute();
	}

	/***************************************
	 * レコーダの情報を取得する部品群
	 **************************************/

	private boolean _loadRdRecorderAll(final boolean force, final String myself) {

		HDDRecorderList recs;
		if ( myself != null ) {
			recs = recorders.findInstance(myself);
		}
		else {
			recs = recorders;
		}

		boolean success = true;

		for ( HDDRecorder recorder : recs ) {
			if ( recorder.isReserveListSupported() ) {
				success = success & _loadRdRecorder(recorder, force);
			}
		}

		return success;
	}

	private boolean _loadRdRecorder(HDDRecorder recorder, boolean force) {

		mwin.appendMessage("【レコーダ情報取得】情報を取得します: "+recorder.getDispName());
		if ( recorder.isThereAdditionalDetails() && env.getForceLoadReserveDetails() == 2 ) {
			mwin.appendMessage("＜＜＜注意！＞＞＞このレコーダでは予約詳細の個別取得を実行しないと正確な情報を得られない場合があります。");
		}

		try {

			// 各種設定の取得
			if ( ! _loadRdSettings(recorder,force) ) {
				return false;
			}

			// 予約一覧の取得
			if ( ! _loadRdReserves(recorder,force) ) {
				return false;
			}

			// レコーダから取得したエンコーダ情報で、登録済みレコーダ一覧を更新する
			setEncoderInfo2RecorderList(recorder,force);

			// 予約詳細の取得（強制取得じゃなければ処理不要）
			if ( force && ! _loadRdReserveDetails(recorder,force) ) {
				return false;
			}

			// レコーダの放送局名をWeb番組表の放送局名に置き換え
			checkChNameIsRight(recorder);

			// 自動予約一覧の取得
			if ( ! _loadRdAutoReserves(recorder,force) ) {
				return false;
			}

			// 録画結果一覧の取得
			if ( ! _loadRdRecorded(recorder,force) ) {
				return false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			mwin.appendError("【致命的エラー】予約一覧の取得で例外が発生 "+recorder.getIPAddr()+":"+recorder.getPortNo()+":"+recorder.getRecorderId());
			ringBeep();
			return false;
		}
		return true;
	}


	/***************************************
	 * レコーダの情報を取得する部品群
	 **************************************/

	private boolean _loadRdSettings(HDDRecorder recorder, boolean force) {
		if ( recorder.GetRdSettings(force) ) {
			return true;
		}

		mwin.appendError(recorder.getErrmsg()+" "+recorder.getDispName());	// 取得に失敗
		ringBeep();
		return false;
	}

	private boolean _loadRdReserves(HDDRecorder recorder, boolean force) {
		if ( recorder.GetRdReserve(force) ) {
			return true;
		}

		mwin.appendError(recorder.getErrmsg()+" "+recorder.getDispName());	// 取得に失敗
		ringBeep();
		return false;
	}

	private boolean _loadRdReserveDetails(HDDRecorder recorder, boolean force) {

		if ( ! recorder.isThereAdditionalDetails() ) {
			return true;	// 非対応レコーダ
		}

		boolean skip = false;
		if ( force && env.getForceLoadReserveDetails() == 2 ) {
			skip = true;
		}
		else if ( force && env.getForceLoadReserveDetails() == 0 ) {
			int ret = JOptOptionPane.showConfirmDialog(stwin, "<HTML>詳細情報を取得しますか？（時間がかかります）<BR><BR>"+recorder.getDispName()+"</HTML>", "今回の選択を既定の動作とする", "※既定動作は各種設定で変更できます", "確認", JOptionPane.YES_NO_OPTION);
			skip = (ret != JOptOptionPane.YES_OPTION);

			if ( JOptOptionPane.isSelected() ) {
				// 今回の選択を既定の動作とする
				env.setForceLoadReserveDetails(skip ? 2 : 1);
				env.save();
				if (setting!=null) setting.updateSelections();
			}
		}
		if ( skip ) {
			mwin.appendMessage("【！】予約詳細情報の取得はスキップされました");
			return true;
		}

		if ( recorder.GetRdReserveDetails()) {
			return true;	// 取得成功
		}

		mwin.appendError(recorder.getErrmsg()+" "+recorder.getDispName());
		ringBeep();
		return false;		// 取得失敗
	}

	private boolean _loadRdAutoReserves(HDDRecorder recorder, boolean force) {

		if ( ! recorder.isEditAutoReserveSupported() ) {
			return true;
		}

		boolean skip = false;
		if ( force && env.getForceLoadAutoReserves() == 2 ) {
			skip = true;
		}
		else if ( force && env.getForceLoadAutoReserves() == 0 ) {
			int ret = JOptOptionPane.showConfirmDialog(stwin, "<HTML>自動予約一覧を取得しますか？（時間がかかります）<BR><BR>"+recorder.getDispName()+"</HTML>", "今回の選択を既定の動作とする", "※既定動作は各種設定で変更できます", "確認", JOptionPane.YES_NO_OPTION);
			skip = (ret != JOptOptionPane.YES_OPTION);

			if ( JOptOptionPane.isSelected() ) {
				// 今回の選択を既定の動作とする
				env.setForceLoadAutoReserves(skip ? 2 : 1);
				env.save();
				if (setting!=null) setting.updateSelections();
			}
		}
		if ( skip ) {
			mwin.appendMessage("【！】自動予約一覧の取得はスキップされました");
			return true;
		}

		if ( recorder.GetRdAutoReserve(force) ) {
			return true;
		}

		mwin.appendError(recorder.getErrmsg()+" "+recorder.getDispName());
		ringBeep();
		return false;
	}

	private boolean _loadRdRecorded(HDDRecorder recorder, boolean force) {

		if ( ! recorder.isRecordedListSupported() ) {
			return true;
		}

		boolean skip = false;
		if ( force && env.getForceLoadRecorded() == 2 ) {
			skip = true;
		}
		if ( force && env.getForceLoadRecorded() == 0 ) {
			int ret = JOptOptionPane.showConfirmDialog(stwin, "<HTML>録画結果一覧を取得しますか？（時間がかかります）<BR><BR>"+recorder.getDispName()+"</HTML>", "今回の選択を既定の動作とする", "※既定動作は各種設定で変更できます", "確認", JOptionPane.YES_NO_OPTION);
			skip = (ret != JOptOptionPane.YES_OPTION);

			if ( JOptOptionPane.isSelected() ) {
				// 今回の選択を既定の動作とする
				env.setForceLoadRecorded(skip ? 2 : 1);
				env.save();
				if (setting!=null) setting.updateSelections();
			}
		}
		if ( skip ) {
			mwin.appendMessage("【！】録画結果一覧の取得はスキップされました");
			return true;
		}

		if ( recorder.GetRdRecorded(force) ) {
			return true;
		}

		mwin.appendError(recorder.getErrmsg()+" "+recorder.getDispName());
		ringBeep();
		return false;
	}

	/**
	 * レコーダから取得したエンコーダ情報で、登録済みレコーダ一覧を更新する
	 * @param recorder
	 */
	private void setEncoderInfo2RecorderList(HDDRecorder recorder, boolean force) {
		for (RecorderInfo ri : recInfoList ) {
			//if (rl.getRecorderEncoderList().size() == 0)
			{
				//String mySelf = ri.getRecorderIPAddr()+":"+ri.getRecorderPortNo()+":"+ri.getRecorderId();
				//String myMail = "MAIL"+":"+ri.getRecorderMacAddr()+":"+ri.getRecorderId();
				//if (recorder.isMyself(mySelf) || recorder.isMyself(myMail)) {
				if ( recorder.isMyself(ri.MySelf()) ) {
					ri.clearEncoders();
					for (TextValueSet enc : recorder.getEncoderList()) {
						ri.addEncoder(enc.getText());
					}

					if ( force ) {
						recInfoList.save();
					}
					break;
				}
			}
		}
	}

	/**
	 * 予約一覧の放送局名が正しい形式であるかどうかのチェック
	 */
	private void checkChNameIsRight(HDDRecorder recorder) {
		HashMap<String,String> misCN = new HashMap<String,String>();
		for ( ReserveList r : recorder.getReserves() ) {
			if ( r.getCh_name() == null ) {
				misCN.put(r.getChannel(),recorder.getRecorderId());
			}
		}
		if ( misCN.size() > 0 ) {
			for ( String cn : misCN.keySet() ) {
				String msg = "【警告(予約一覧)】 <"+misCN.get(cn)+"> \"レコーダの放送局名\"を\"Web番組表の放送局名\"に変換できません。CHコード設定に設定を追加してください：\"レコーダの放送局名\"="+cn;
				mwin.appendMessage(msg);
			}
			ringBeep();
		}
	}

	/**
	 * レコーダの録画タイトルをＤＬする
	 */
	private void loadRdTitlesAll(final boolean force, final String myself) {

		new SwingBackgroundWorker(true) {

			@Override
			protected Object doWorks() throws Exception {

				_loadRdTitlesAll(force,myself);

				return null;
			}

			@Override
			protected void doFinally() {
			}
		}.execute();
	}

	private boolean _loadRdTitlesAll(final boolean force, final String myself) {

		HDDRecorderList recs;
		if ( myself != null ) {
			recs = recorders.findInstance(myself);
		}
		else {
			recs = recorders;
		}

		boolean success = true;

		for ( HDDRecorder recorder : recs ) {
			if ( recorder.isReserveListSupported() ) {
				success = success & _loadRdTitles(recorder, force);
			}
		}

		return success;
	}

	private boolean _loadRdTitles(HDDRecorder recorder, boolean force) {
		if (!recorder.isTitleListSupported())
			return true;

		mwin.appendMessage("【レコーダ情報取得】録画タイトルを取得します: "+recorder.getDispName());
		try {

			// 各種設定の取得
			if ( !recorder.GetRdSettings(force) )
					return false;

			// 各種設定の取得
			if ( !recorder.GetRdTitles("0", force, false, true) )
					return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			mwin.appendError("【致命的エラー】録画タイトルの取得で例外が発生 "+recorder.getIPAddr()+":"+recorder.getPortNo()+":"+recorder.getRecorderId());
			ringBeep();
			return false;
		}
		return true;
	}

	/*******************************************************************************
	 * Web番組表をDLする
	 ******************************************************************************/

	/***************************************
	 * ツールバートリガー（と、各種設定変更トリガー）による
	 **************************************/

	/**
	 * Web番組表をＤＬ→再描画まで
	 * <P>単体実行の場合はこちらを呼び出す
	 * <P>部品実行の場合はこちらを呼び出す：{@link #loadTVProgram(boolean, LoadFor)}
	 * @see #doRedrawTVProgram()
	 */
	private boolean doLoadTVProgram(final boolean force, final LoadFor lf, boolean background) {
		if (downloadInProgress){
			mwin.appendError("[Web番組表取得]実行中の取得処理を中止します。");
			TVProgramUtils.setCancelRequested(true);
			return false;
		}

		if (force){
			GregorianCalendar c = new GregorianCalendar();
			c.setTimeZone(TimeZone.getDefault());

			if (env != null){
				env.setLastDownloadTime(CommonUtils.getCalendarAsString(c));
				env.save();
			}
		}

		//
		downloadInProgress = true;
		toolBar.setReloadTVProgramsInProgress(true);

		if (background){
			TVProgramUtils.setProgressAreaBackground(mwin);
		}
		else{
			StWinClear();
		}

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {
				TatCount tc = new TatCount();

				loadTVProgram(force, lf, background);

				mwin.appendMessage(String.format("[Web番組表取得] 【完了しました】 所要時間： %.2f秒",tc.end()));
				return null;
			}

			@Override
			protected void doFinally() {
				timer_now.pause();

				if (background){
					TVProgramUtils.setProgressAreaBackground(null);
				}
				else{
					StWinSetVisible(false);
				}
				downloadInProgress = false;
				toolBar.setReloadTVProgramsInProgress(false);
				doRedrawTVProgram();

				timer_now.start();
			}
		}.execute();

		if (!background){
			StWinSetLocationCenter(this);
			StWinSetVisible(true);
		}

		return true;
	}

	/**
	 *
	 * @see #doLoadTVProgram(boolean, LoadFor)
	 */
	private void doRedrawTVProgram() {

		// 新聞描画枠のリセット
		paper.clearPanel();
		paper.buildMainViewByDate();

		// サイドツリーの再構築
		paper.redrawTreeByDate();
		paper.redrawTreeByPassed();

		listed.redrawTreeByHistory();
		listed.redrawTreeByCenter();

		// 再描画
		paper.reselectTree();
		listed.reselectTree();
	}

	/***************************************
	 * 自クラス内呼び出しによる
	 **************************************/

	/*
	 * Web番組表のＤＬの進捗を報告する
	 */
	private void reportTVProgramProgress(String msg){
		TVProgramUtils.reportProgress(msg);
	}

	/**
	 * Web番組表をＤＬする
	 * <P>単体実行の場合はこちらを呼び出す：{@link #doLoadTVProgram(boolean, tainavi.Viewer.LoadFor)}
	 * <P>部品実行の場合はこちらを呼び出す
	 */
	private boolean loadTVProgram(final boolean force, final LoadFor lf, boolean background) {

		final String FUNCID = "[Web番組表取得] ";
		final String ERRID = "[ERROR]"+FUNCID;

		try {
			cacheFileOnly = true;
			stwin.resetWindowCloseRequested();
			TVProgramUtils.setCancelRequested(false);

			TVProgram tvp;

			if (TVProgramUtils.isCancelRequested()){
				return false;
			}

			tvp = tvprograms.getTvProgPlugin(null);
			if ( tvp != null )
			{
				String sType = "地上波＆ＢＳ番組表";
				if (lf == LoadFor.ALL || lf == LoadFor.TERRA) {
					if (!loadTVProgramOnce(tvp, sType, tvp.getSelectedArea(), false, force)){
						return false;
					}
					if (!tvp.getCacheFileOnly())
						cacheFileOnly = false;
				}
				else {
					reportTVProgramProgress(FUNCID+sType+"へのアクセスはスキップされました: "+tvp.getTVProgramId());
				}
			}

			if (TVProgramUtils.isCancelRequested()){
				loadTVProgramPostProcess(force);
				return false;
			}
			tvp = tvprograms.getCsProgPlugin(null);
			if ( tvp != null )
			{
				String sType = "ＣＳ番組表[プライマリ]";
				if (lf == LoadFor.ALL || lf == LoadFor.CS || lf == LoadFor.CSo1 || lf == LoadFor.CSwSD) {
					if (!loadTVProgramOnce(tvp, sType, tvp.getSelectedArea(), false, force)){
						loadTVProgramPostProcess(force);
						return false;
					}
					if (!tvp.getCacheFileOnly())
						cacheFileOnly = false;
				}
				else {
					reportTVProgramProgress(FUNCID+sType+"へのアクセスはスキップされました: "+tvp.getTVProgramId());
				}
			}

			if (TVProgramUtils.isCancelRequested()){
				loadTVProgramPostProcess(force);
				return false;
			}

			tvp = tvprograms.getCs2ProgPlugin(null);
			if ( tvp != null )
			{
				String sType = "ＣＳ番組表[セカンダリ]";
				if (lf == LoadFor.ALL || lf == LoadFor.CS || lf == LoadFor.CSo2 || lf == LoadFor.CSwSD) {
					if (!loadTVProgramOnce(tvp, sType, tvp.getSelectedArea(), false, force)){
						loadTVProgramPostProcess(force);
						return false;
					}
					if (!tvp.getCacheFileOnly())
						cacheFileOnly = false;
				}
				else {
					reportTVProgramProgress(FUNCID+sType+"へのアクセスはスキップされました: "+tvp.getTVProgramId());
				}
			}

			if (TVProgramUtils.isCancelRequested()){
				loadTVProgramPostProcess(force);
				return false;
			}

			tvp = tvprograms.getSyobo();
			if ( tvp != null ) {
				String sType = "しょぼかる";
				if ( (lf == LoadFor.ALL || lf == LoadFor.SYOBO) && enableWebAccess && env.getUseSyobocal()) {
					tvp.loadCenter(tvp.getSelectedCode(), force);	// しょぼかるには放送局リストを取得するイベントが他にないので
					loadTVProgramOnce(tvp, sType, null, true, force);
				}
				else {
					reportTVProgramProgress(FUNCID+sType+"へのアクセスはスキップされました.");
				}

				// しょぼかるの新番組マークを引き継ぐ
				attachSyoboNew();
			}

			if (TVProgramUtils.isCancelRequested()){
				loadTVProgramPostProcess(force);
				return false;
			}

			PickedProgram pickup = tvprograms.getPickup();
			if ( tvp != null ) {
				pickup.refresh();
				//pickup.save();
			}

			loadTVProgramPostProcess(force);
		}
		catch (Exception e) {
			e.printStackTrace();
			mwin.appendError(ERRID+"番組情報の取得で例外が発生");
			ringBeep();
			return false;
		}

		return true;
	}

	/*
	 * 後処理
	 */
	private void loadTVProgramPostProcess(boolean force){
		final String FUNCID = "[Web番組表取得] ";

		// 番組タイトルを整形する
		fixTitle();
		fixDetail();

		// 検索結果の再構築
		reportTVProgramProgress(FUNCID+"検索結果を生成します.");
		mpList.clear(env.getDisableFazzySearch(), env.getDisableFazzySearchReverse());
		mpList.build(tvprograms, trKeys.getTraceKeys(), srKeys.getSearchKeys());

		// 過去ログ
		if ( !env.getUsePassedProgram() || cacheFileOnly) {
			reportTVProgramProgress(FUNCID+"過去ログは記録されません.");
		}
		else {
			TatCount tc = new TatCount();
			reportTVProgramProgress(FUNCID+"過去ログを生成します.");
			if ( tvprograms.getPassed().save(tvprograms.getIterator(), chsort.getClst(), env.getPrepPassedProgramCount()) ) {
				reportTVProgramProgress(String.format(FUNCID+"過去ログを生成しました [%.2f秒].",tc.end()));
			}
			//PassedProgramList.getDateList(env.getPassedLogLimit());
		}
	}


	// 分割
	private boolean loadTVProgramOnce(TVProgram tvp, String sType, String aName, boolean loadonly, boolean force) {

		final String FUNCID = "[Web番組表取得] ";
//		final String ERRID = "[ERROR]"+FUNCID;

		// ログ
		String msg = FUNCID+sType+"を取得します: "+tvp.getTVProgramId();
		reportTVProgramProgress(msg);
		if (aName!=null) reportTVProgramProgress(FUNCID+"＋選択されているエリア="+aName);

		// 読み込み
		//tvp.setProgressArea(stwin);
		if (!tvp.loadProgram(tvp.getSelectedCode(), force)){
			if (mwin!=null) mwin.appendError(FUNCID+"番組情報の取得に失敗しました。");
			return false;
		}

		if (loadonly) {
			return true;
		}

		// 延長警告
		tvp.setExtension(null, null, false, extKeys.getSearchKeys());	// 最初の３引数は盲腸。ダミー
		// NGワード
		tvp.abon(env.getNgword());
		// 抜けチェック
		String errmsg = tvp.chkComplete();
		if (errmsg != null) {
			msg = FUNCID+"取得した情報が不正です："+errmsg;
			reportTVProgramProgress(msg);
			if (mainWindow!=null) mwin.appendError(msg);
			ringBeep();
//			return false;
			return true;
		}

		return true;
	}

	// しょぼかるの番組詳細を番組表に反映する
	private void attachSyoboNew() {
		TVProgram syobo = tvprograms.getSyobo();
		if (syobo == null) {
			return;
		}

		for ( TVProgram tvp : tvprograms ) {

			if ( tvp.getType() != ProgType.PROG ) {
				continue;
			}
			if ( ! (tvp.getSubtype() == ProgSubtype.TERRA || tvp.getSubtype() == ProgSubtype.CS || tvp.getSubtype() == ProgSubtype.CS2) ) {
				continue;
			}

			for ( ProgList tvpl : tvp.getCenters() ) {
				if ( ! tvpl.enabled) {
					continue;
				}
				for ( ProgList svpl : syobo.getCenters() ) {
					if ( ! tvpl.Center.equals(svpl.Center)) {
						continue;
					}
					for ( ProgDateList tvc : tvpl.pdate ) {

						ProgDateList mSvc = null;
						for ( ProgDateList svc : svpl.pdate ) {
							if (tvc.Date.equals(svc.Date) ) {
								mSvc = svc;
								break;
							}
						}
						if (mSvc == null) {
							// しょぼかる側に該当する日付自体ないので全部フラグを立てっぱなしでいい
							for ( ProgDetailList tvd : tvc.pdetail ) {
								if ( tvd.isEqualsGenre(ProgGenre.ANIME, null) ) {
									tvd.addOption(ProgOption.NOSYOBO);
								}
							}
						}
						else {
							// しょぼかる側に該当する日付があるのでマッチング。アニメと映画と音楽
							for ( ProgDetailList tvd : tvc.pdetail ) {

								// アニメはいったんフラグを立てる
								if ( tvd.isEqualsGenre(ProgGenre.ANIME, null) ) {
									tvd.addOption(ProgOption.NOSYOBO);
								}

								boolean isFind = false;
								for ( ProgDetailList svd : mSvc.pdetail ) {
									if ( tvd.start.equals(svd.start) ) {

										// 番組ID
										{
											//svd.progid = tvd.progid;
											svd.setContentIdStr();
										}

										boolean isAnime = tvd.isEqualsGenre(ProgGenre.ANIME, null);
										if ( ! isAnime && ! tvd.isEqualsGenre(ProgGenre.MOVIE, null) && ! tvd.isEqualsGenre(ProgGenre.MUSIC, null) ) {
											break;
										}

										// みつけた
										isFind = true;

										// しょぼかるとWeb番組表の両方に存在する
										svd.nosyobo = true;

										// 各種フラグ
										{
											boolean isAttached = false;

											// 新番組フラグ
											if ( svd.flag == ProgFlags.NEW && tvd.flag != ProgFlags.NEW ) {
												tvd.flag = ProgFlags.NEW;
												isAttached = true;
											}

											// 最終回フラグ
											if ( svd.flag == ProgFlags.LAST && tvd.flag != ProgFlags.LAST ) {
												tvd.flag = ProgFlags.LAST;
												isAttached = true;
											}

											// ジャンル
											if ( tvd.isEqualsGenre(ProgGenre.MOVIE, null) && ! tvd.isEqualsGenre(ProgGenre.MOVIE, ProgSubgenre.MOVIE_ANIME) ) {
												if ( tvd.genrelist == null ) {
													tvd.genrelist = new ArrayList<ProgGenre>();
													tvd.genrelist.add(tvd.genre);
													tvd.genrelist.add(ProgGenre.MOVIE);
													tvd.subgenrelist = new ArrayList<ProgSubgenre>();
													tvd.subgenrelist.add(tvd.subgenre);
													tvd.subgenrelist.add(ProgSubgenre.MOVIE_ANIME);
												}
												else {
													tvd.genrelist.add(ProgGenre.MOVIE);
													tvd.subgenrelist.add(ProgSubgenre.MOVIE_ANIME);
												}
												isAttached = true;
											}

											// その他のフラグ
											for ( ProgOption sopt : svd.getOption() ) {
												if ( tvd.addOption(sopt) && isAttached == false ) {
													isAttached = true;
												}
											}

											// ログ
											if (isAttached && env.getDebug()) {
												StdAppendMessage("しょぼかるのフラグを引き継ぎました: ("+tvpl.Center+") "+tvd.title);
											}
										}

										// 番組詳細
										if ( tvd.detail.length() < svd.detail.length() ) {
											tvd.detail = svd.detail;
										}
										else {
											int idx = svd.detail.indexOf("<!");
											if (idx != -1) {
												tvd.detail += svd.detail.substring(idx);
											}
										}

										tvd.linkSyobo = svd.link;

										// 「しょぼかるにのみ存在」フラグの上げ下げ（これはアニメ限定）
										if ( isAnime ) {
											if ( isFind ) {
												tvd.removeOption(ProgOption.NOSYOBO);	// NOSYOBOって…
											}
											else {
												//tvd.addOption(ProgOption.NOSYOBO);
											}
										}

										break;
									}
								}
							}
						}
					}
					break;
				}
			}
		}
	}

	// 番組タイトルを整形する
	private void fixTitle() {
		// 番組追跡からサブタイトルを除外するかどうかのフラグ
		ProgDetailList.tracenOnlyTitle = env.getFixTitle() && env.getTraceOnlyTitle();
		//
		if ( ! env.getFixTitle()) {
			return;
		}
		//
		for ( TVProgram tvp : tvprograms ) {
			//if ( ! (tvp.getType() == ProgType.PROG && tvp.getSubtype() == ProgSubtype.TERRA) ) {
			if ( tvp.getType() != ProgType.PROG ) {
				continue;
			}
			//
			for ( ProgList pl : tvp.getCenters() ) {
				if ( ! pl.enabled ) {
					continue;
				}

				for ( ProgDateList pcl : pl.pdate ) {
					//
					for ( ProgDetailList tvd : pcl.pdetail ) {
						if ( tvd.isEqualsGenre(ProgGenre.ANIME, null) ) {
							if ( pl.Center.startsWith("NHK") || pl.Center.startsWith("ＮＨＫ") ) {
								// NHK系で先頭が「アニメ　」ではじまるものから「アニメ　」を削除する
								tvd.title = tvd.title.replaceFirst("^(?:TV|ＴＶ)アニメ[ 　・]+","");
								tvd.titlePop = TraceProgram.replacePop(tvd.title);
							}
							if ( tvd.title.contains("コメンタリ") || tvd.detail.contains("コメンタリ") ) {
								// "コメンタリ"の記述のあるものは「副音声」扱いにする（副音声でなくても）
								tvd.option.add(ProgOption.MULTIVOICE);
							}
							if ( (tvd.title.contains("劇場版") || (tvd.detail.contains("映画") && ! tvd.detail.contains("映画館"))) && ! tvd.isEqualsGenre(ProgGenre.MOVIE, ProgSubgenre.MOVIE_ANIME) ) {
								// ジャンル＝アニメだがタイトルに「劇場版」が含まれるならジャンル＝映画（アニメ映画）を追加する
								if ( tvd.genrelist == null ) {
									tvd.genrelist = new ArrayList<ProgGenre>();
									tvd.genrelist.add(tvd.genre);
									tvd.genrelist.add(ProgGenre.MOVIE);
									tvd.subgenrelist = new ArrayList<ProgSubgenre>();
									tvd.subgenrelist.add(tvd.subgenre);
									tvd.subgenrelist.add(ProgSubgenre.MOVIE_ANIME);
								}
								else {
									tvd.genrelist.add(ProgGenre.MOVIE);
									tvd.subgenrelist.add(ProgSubgenre.MOVIE_ANIME);
								}
							}
						}
						else if ( tvd.isEqualsGenre(ProgGenre.MOVIE, ProgSubgenre.MOVIE_ANIME) && tvd.subgenre != ProgSubgenre.MOVIE_ANIME ) {
							// ジャンル＝映画でサブジャンルが複数ありアニメが優先されてないものはアニメを優先する
							tvd.subgenre = ProgSubgenre.MOVIE_ANIME;
						}
					}
				}
			}
		}
	}

	/**
	 * {@link ProgDetailList} の情報を整形する
	 */
	private void fixDetail() {
		for ( TVProgram tvp : tvprograms ) {
			for ( ProgList pl : tvp.getCenters() ) {
				if ( ! pl.enabled ) {
					continue;
				}
				for ( ProgDateList pcl : pl.pdate ) {
					for ( ProgDetailList tvd : pcl.pdetail ) {
						if ( tvd.start == null || tvd.start.length() == 0 ) {
							continue;
						}

						fixDetailSub(tvp, pl, tvd);
					}
				}
			}
		}
	}

	private void fixDetailSub(TVProgram tvp, ProgList pl, ProgDetailList tvd) {
		tvd.type = tvp.getType();
		tvd.subtype = tvp.getSubtype();
		tvd.center = pl.Center;

		tvd.recmin = CommonUtils.getRecMinVal(tvd.startDateTime, tvd.endDateTime);

		tvd.extension_mark = markchar.getExtensionMark(tvd);
		tvd.prefix_mark = markchar.getOptionMark(tvd);
		tvd.newlast_mark = markchar.getNewLastMark(tvd);
		tvd.postfix_mark = markchar.getPostfixMark(tvd);

		tvd.dontoverlapdown = (tvd.center.startsWith("ＮＨＫ") || tvd.center.startsWith("NHK"));
	}


	/*******************************************************************************
	 * 過去ログ検索
	 ******************************************************************************/

	/**
	 * <P>過去ログから検索キーワードにマッチする情報を取得する
	 * <P>全部検索がヒットした結果がかえるのだから {@link ProgDetailList} ではなく {@link MarkedProgramList} を使うべきなのだが…
	 */
	private boolean searchPassedProgram(final SearchKey sKey, final String target) {

		Matcher ma = Pattern.compile("^(\\d\\d\\d\\d/\\d\\d/\\d\\d)-(\\d\\d\\d\\d/\\d\\d/\\d\\d)$").matcher(target);
		if ( ! ma.find() ) {
			return false;
		}

		final GregorianCalendar s = CommonUtils.getCalendar(ma.group(1));
		final GregorianCalendar e = CommonUtils.getCalendar(ma.group(2));
		final long dDays = (e.getTimeInMillis() - s.getTimeInMillis())/86400000 + 1;

		final ArrayList<ProgDetailList> srchpdl = tvprograms.getSearched().getResultBuffer(sKey.getLabel()) ;

		stwin.clear();

		// 検索実行（時間がかかるので状況表示する）
		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				// 検索中
				int resultCnt = 0;
				for (int cnt=1; cnt<=dDays; cnt++) {

					String passdt = CommonUtils.getDate(e);
					stwin.appendMessage(String.format("[過去ログ検索] 検索中：(%d/%d) %s", cnt, dDays, passdt));

					PassedProgram tvp = new PassedProgram();
					if ( tvp.loadAllCenters(passdt) ) {
						for ( ProgList pl : tvp.getCenters() ) {
							if ( ! pl.enabled ) {
								continue;
							}

							for ( ProgDateList pcl : pl.pdate ) {
								for ( ProgDetailList tvd : pcl.pdetail ) {
									if ( tvd.start == null || tvd.start.length() == 0 ) {
										continue;
									}

									if ( SearchProgram.isMatchKeyword(sKey, pl.Center, tvd) ) {
										tvd.dynKey = sKey;
										tvd.dynMatched = SearchProgram.getMatchedString();
										fixDetailSub(tvp, pl, tvd);
										srchpdl.add(tvd);
										if ( ++resultCnt >= env.getSearchResultMax() ) {
											mwin.appendMessage(String.format("[過去ログ検索] 検索件数の上限に到達しました。所要時間： %.2f秒",tc.end()));
											return null;
										}
									}
								}
							}
						}
					}

					e.add(Calendar.DATE,-1);
				}

				mwin.appendMessage(String.format("[過去ログ検索] 検索完了。所要時間： %.2f秒",tc.end()));
				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(this);
		StWinSetVisible(true);

		return true;
	}


	/*******************************************************************************
	 * スナップ・ショット！
	 ******************************************************************************/

	/**
	 * 番組表のスナップショットをファイルに保存したり印刷したりする
	 */
	private boolean getSnapshot(int currentpage, int numberofpages) {

		try {
			String fname;
			if ( mainWindow.isTabSelected(MWinTab.LISTED) ) {
				// リスト形式
				fname = String.format("snapshot.%s",env.getSnapshotFmt().getExtension());
				CommonSwingUtils.saveComponentAsJPEG(listed.getCurrentView(), listed.getTableHeader(), null, listed.getTableBody(), fname, env.getSnapshotFmt(), Viewer.this);
			}
			else if ( mainWindow.isTabSelected(MWinTab.PAPER) ){
				// 新聞形式
				if ( env.getDrawcacheEnable() || ! env.isPagerEnabled() ) {
					fname = String.format("snapshot.%s",env.getSnapshotFmt().getExtension());
				}
				else {
					if ( env.getAllPageSnapshot() ) {
						for ( int i=0; i<numberofpages; i++ ) {
							if ( i != currentpage ) {
								// カレントページは最後にスナップる（再描画を１回で済ませるため）
								toolBar.setSelectedPagerIndex(i);
								fname = String.format("snapshot%02d.%s",i+1,env.getSnapshotFmt().getExtension());
								CommonSwingUtils.saveComponentAsJPEG(paper.getCurrentView(), paper.getCenterPane(), paper.getTimebarPane(), paper.getCurrentPane(), fname, env.getSnapshotFmt(), Viewer.this);
							}
						}
					}
					fname = String.format("snapshot%02d.%s",currentpage+1,env.getSnapshotFmt().getExtension());
					toolBar.setSelectedPagerIndex(currentpage);
				}
				CommonSwingUtils.saveComponentAsJPEG(paper.getCurrentView(), paper.getCenterPane(), paper.getTimebarPane(), paper.getCurrentPane(), fname, env.getSnapshotFmt(), Viewer.this);
			}
			else {
				// 他のタブ
				return true;
			}

			Desktop desktop = Desktop.getDesktop();
			if (env.getPrintSnapshot()) {
				// 印刷
				desktop.print(new File(fname));
			}
			else {
				// ファイルに保存
				String emsg = CommonUtils.openFile(fname);
				if (emsg != null) {
					mwin.appendError(emsg);
					return false;
				}
			}

			return true;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	/*******************************************************************************
	 * ここからおおむね初期化処理にかかわるメソッド群
	 ******************************************************************************/

	/**
	 * 各種設定の変更の反映
	 */
	private boolean setEnv(final boolean reload_prog) {

		bounds.save();
		cbitems.save();
		lvitems.save();
		rlitems.save();
		tlitems.save();
		lnitems.save();
		tbicons.save();
		tabitems.save();
		env.save();

		// CommonUtilsの設定変更
		CommonUtils.setAdjLateNight(env.getAdjLateNight());
		CommonUtils.setExpandTo8(env.getExpandTo8());
		CommonUtils.setUseRundll32(env.getUseRundll32());
		CommonUtils.setDisplayPassedReserve(env.getDisplayPassedReserve());
		CommonUtils.setDebug(env.getDebug());

		SwingBackgroundWorker.setDebug(env.getDebug());

		// ほにゃらら
		toolBar.setDebug(env.getDebug());
		autores.setDebug(env.getDebug());
		if (rdialog != null)
			rdialog.setDebug(env.getDebug());

		// PassedProgramListの設定変更
		tvprograms.getPassed().setPassedDir(env.getPassedDir());

		// レコーダプラグインの設定変更
		for ( HDDRecorder rec : recorders ) {
			// 拡張設定だけ
			setSettingRecPluginExt(rec, env);
		}

		// Web番組表共通設定
		setSettingProgPluginCommon(env);

		// web番組表のリフレッシュ
		setSettingProgPluginAll(env);

		toolBar.reflectEnv();
		mainWindow.reflectEnv();
		addAllTabs();

		// リスト形式の再構築
		listed.reflectNodeEnv();

		listed.copyColumnWidth();
		listed.reflectColumnEnv();
		listed.reflectEnv();

		// 番組表形式の再構築
		paper.reflectEnv();

		// 本体予約一覧の再構築
		reserved.copyColumnWidth();
		reserved.reflectColumnEnv();
		reserved.reflectEnv();

		// タイトル一覧の再構築
		titled.copyColumnWidth();
		titled.reflectColumnEnv();
		titled.setDetailVisible(mwin.isVisible());

		// 新聞形式にフォントを反映する
		paper.updateFonts(env);

		// システムトレイアイコン
		setTrayIconVisible(env.getShowSysTray());
		setXButtonAction(env.getShowSysTray() && env.getHideToTray());

		// 新聞形式のツールチップの表示時間を変更する
		setTooltipDelay();

		// 番組情報の再取得
		if ( reload_prog ) {
			loadTVProgram(false, LoadFor.ALL, false);	// 部品呼び出し
		}
		else
			loadTVProgramPostProcess(false);

		// Web番組表の再構築
		mpList.setHistoryOnlyUpdateOnce(env.getHistoryOnlyUpdateOnce());
		mpList.setShowOnlyNonrepeated(env.getShowOnlyNonrepeated());

		setStatusVisible(false);
		mainWindow.initStatusAreaHeight();
		if (bounds.getShowStatus())
			setStatusVisible(true);

		doRedrawTVProgram();	// か き な お し

		return true;
	}

	// システムトレイ関係
	private void getTrayIcon() {
		if ( trayicon != null ) {
			return;
		}

		try {
			Image image = ImageIO.read(new File(ICONFILE_SYSTRAY));
			trayicon = new TrayIcon(image,"Tainavi");

			final Viewer thisClass = this;

			// メニューの追加
			PopupMenu popup = new PopupMenu();
			{
				MenuItem item = new MenuItem("開く");
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						thisClass.setVisible(true);
						thisClass.setState(Frame.NORMAL);
					}
				});
				popup.add(item);
			}
			{
				MenuItem item = new MenuItem("終了する");
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						ExitOnClose();
						System.exit(0);
					}
				});
				popup.add(item);
			}
			trayicon.setPopupMenu(popup);

			// 左クリックで復帰
			trayicon.addMouseListener(new MouseAdapter() {
				//
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						thisClass.setVisible(true);
						thisClass.setState(Frame.NORMAL);
					}
				}
			});

		} catch (IOException e) {
			StdAppendError("アイコンファイルが読み込めませんでした: "+ICONFILE_SYSTRAY);
			e.printStackTrace();
		} catch (UnsupportedOperationException e){
			StdAppendError("システムトレイはサポートされていません ");
			e.printStackTrace();
		}
	}
	private void setTrayIconVisible(boolean b) {

		if ( ! SystemTray.isSupported() || trayicon == null ) {
			return;
		}

		try {
			if ( b ) {
				// システムトレイに追加
				SystemTray.getSystemTray().remove(trayicon);
				SystemTray.getSystemTray().add(trayicon);
			}
			else {
				// システムトレイから削除
				SystemTray.getSystemTray().remove(trayicon);
			}
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	private void HideToTray() {
		if ( SystemTray.isSupported() && trayicon != null && (env.getShowSysTray() && env.getHideToTray()) ) {
			this.setVisible(false);
		}
	}
	private void setXButtonAction(boolean b) {
		if ( b ) {
			this.setDefaultCloseOperation(JFrame.ICONIFIED);
		}
		else {
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
	}

	// コマンドライン引数の処理
	private void procArgs(String[] args) {
		int flag = 0;
		for (String arg : args) {
			switch (flag) {
			case 0:
				if (arg.compareTo("-L") == 0) {
					// -l : ロギング
					//logging = false;
				}
				else if (arg.compareTo("-L") == 0) {
					// -L : ロギング不可
					logging = false;
				}
				else if (arg.compareTo("-w") == 0) {
					// -w : レコーダ起動
					runRecWakeup = true;
				}
				else if (arg.compareTo("-nowebaccess") == 0) {
					// -nowebaccess : 起動時のWeb番組表へのアクセス無効
					enableWebAccess = false;
				}
				else if (arg.compareTo("-proxy") == 0) {
					// -proxy : Web番組表へのアクセスにProxy経由を強制する
					flag = 1;
				}
				else if (arg.compareTo("-loadrec") == 0) {
					// -loadrec : 起動時にレコーダにアクセスする
					runRecLoad = true;
				}
				else if (arg.compareTo("-onlyLoadProgram") == 0) {
					// -onlyLoadProgram : 番組表の取得だけ行う
					onlyLoadProgram = true;
				}
				break;
			case 1:
				String[] dat = arg.split(":");
				if (dat.length == 1 ) {
					pxaddr = dat[0];
					pxport = "8080";
				} if (dat.length >= 2 ) {
					pxaddr = dat[0];
					pxport = dat[1];
				}
				flag = 0;
				break;
			}
		}
	}

	// メインの環境設定ファイルを読みだす
	private void loadEnvfile() {
		StdAppendMessage("【環境設定】環境設定ファイルを読み込みます.");
		env.load();
	}

	// 引き続きその他の環境設定ファイルも読みだす
	private void procEnvs() {

		StdAppendMessage("【環境設定】環境設定ファイル類を読み込みます.");

		// 各種設定
		env.makeEnvDir();

		// レコーダ一覧
		recInfoList.load();

		// Proxyサーバ
		if (pxaddr != null) {
			env.setUseProxy(true);
			env.setProxyAddr(pxaddr);
			env.setProxyPort(pxport);
		}

		// Cookieの処理を入れようとしたけど無理だった
		/*
		{
			CookieManager manager = new CookieManager();
			manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(manager);
		}
		*/

		// ジャンル別背景色
		pColors.load();

		// 深夜の帯予約の補正（一日前にずらす）
		// 可能なら番組表を８日分取得する
		// 【WIN】ファイルオープンにrundll32を使用する
		CommonUtils.setAdjLateNight(env.getAdjLateNight());
		CommonUtils.setExpandTo8(env.getExpandTo8());
		CommonUtils.setUseRundll32(env.getUseRundll32());
		CommonUtils.setDisplayPassedReserve(env.getDisplayPassedReserve());
		CommonUtils.setDebug(env.getDebug());

		SwingBackgroundWorker.setDebug(env.getDebug());

		// クリップボードアイテム
		cbitems.load();
		lvitems.load();
		rlitems.load();
		tlitems.load();
		lnitems.load();
		tbicons.load();
		tabitems.load();

		// サイズ・位置情報取得
		bounds.setLoaded(bounds.load());

		// 番組追跡キーワード取得
		trKeys.load();

		// 検索キーワード取得
		srKeys.load();

		// 検索キーワードグループ取得
		srGrps.load();

		// 延長警告源設定取得
		extKeys.load();

		// デフォルトＡＶ設定取得
		avs.load();
		chavs.load();

		// スポーツ延長警告のデフォルト設定のコードはもういらないので削除（3.15.4β）

		// 簡易描画はもういらないので削除

		// ChannelConvert
		chconv.load();

		// 祝日情報
		if (env.getUseHolidayCSV())
			HolidayInfo.Load(env.getHolidayFetchURL(), env.getHolidayFetchInterval());

		// しょぼかるの過去日数
		Syobocal.setPastDays(env.getSyobocalPastDays());
	}

	// 二重起動チェック
	private void chkDualBoot() {
		if ( ! env.getOnlyOneInstance() ) {
			return;
		}

		if ( ! CommonUtils.getLock() ) {
			// 既にロックされている
			ringBeep();
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// 鯛ナビ終了時にロックを解除する
				CommonUtils.getUnlock();
			}
		});
	}

	// アップデートの有無チェック
	private void chkVerUp() {
		if ( ! enableWebAccess || onlyLoadProgram ) {
			stwin.appendError("【オンラインアップデート】オンラインアップデートは無効です");
			return;
		}

		VWUpdate vu = new VWUpdate(stwin);
		if ( ! vu.isExpired(env.getUpdateMethod()) ) {
			// メッセージはVWUpdate内で出力されます
			return;
		}
		if ( doVerUp(vu) ) {
			System.exit(0);
		}
	}

	private boolean doVerUp(VWUpdate vu) {
		UpdateResult res = vu.checkUpdate(VersionInfo.getVersion());
		switch ( res ) {
		case DONE:
			// 成功
			// 履歴は更新しない（連続アップデートがあるかも知れないので）
			LogViewer lv = new LogViewer(HISTORY_FILE);
			lv.setModal(true);
			lv.setCaretPosition(0);
			lv.setVisible(true);
			return true;
		case PASS:
			// キャンセル
			// 履歴は更新しない（次回に持ち越し）
			break;
		case NOUPDATE:
			// アップデートなし
			vu.updateHistory();
			break;
		default:
			// 失敗
			// 履歴は更新しない（次回再挑戦）
			break;
		}
		return false;
	}

	/**
	 *  レコーダプラグインをすべて読み込みます。
	 */
	private boolean loadRecPlugins() {

		stwin.appendMessage("【レコーダプラグイン】プラグインを読み込みます.");

		boolean isMailPluginEnabled = false;
		try {
			Class.forName("javax.mail.Session");
			isMailPluginEnabled = true;
		}
		catch ( Exception e ) {
			System.err.println("【レコーダプラグイン】メール系プラグイン用の外部ライブラリがみつかりません： "+e.toString());
		}

		boolean isCalendarPluginEnabled = false;
		try {
			Class.forName("com.google.gdata.client.calendar.CalendarService");
			isCalendarPluginEnabled = true;
		}
		catch ( Exception e ) {
			System.err.println("【レコーダプラグイン】カレンダー系プラグイン用の外部ライブラリがみつかりません： "+e.toString());
		}

		//
		ArrayList<String> recIda = new ArrayList<String>();
		for ( File f : new File(CommonUtils.joinPath(new String[]{"bin","tainavi","pluginrec"})).listFiles() ) {
			Matcher ma = Pattern.compile("^(PlugIn_Rec[^$]+)[^$]*\\.class$").matcher(f.getName());
			if ( ma.find() ) {
				if ( ! isMailPluginEnabled && f.getName().toLowerCase().contains("mail") ) {
					System.out.println("【レコーダプラグイン】メール系プラグインは無効です： "+f.getName());
					continue;
				}
				if ( ! isCalendarPluginEnabled && f.getName().toLowerCase().contains("calendar") ) {
					System.out.println("【レコーダプラグイン】カレンダー系プラグインは無効です： "+f.getName());
					continue;
				}

				recIda.add(ma.group(1));
			}
		}
		String[] recIdd = recIda.toArray(new String[0]);
		Arrays.sort(recIdd);

		// servicesに追記
		StringBuilder sb = new StringBuilder();
		for ( String recId : recIdd ) {
			sb.append("tainavi.pluginrec.");
			sb.append(recId);
			sb.append("\n");
		}
		if ( ! CommonUtils.write2file(CommonUtils.joinPath(new String[] {"bin","META-INF","services","tainavi.HDDRecorder"}), sb.toString()) ) {
			stwin.appendError("【レコーダプラグイン】プラグインの読み込みに失敗しました: ");
			return false;
		}

		// ここで例外が起きてもトラップできない、スレッドが落ちる
		ServiceLoader<HDDRecorder> r = ServiceLoader.load(HDDRecorder.class);

		recPlugins.clear();
		for ( HDDRecorder recorder : r ) {
			if (env.getDebug()) {
				StdAppendMessage("+追加します: "+recorder.getRecorderId());
			}
			recPlugins.add(recorder.clone());
			StdAppendMessage("+追加しました: "+recorder.getRecorderId());
		}

		return true;
	}

	/**
	 * レコーダ設定をもとにレコーダプラグインから実レコーダのインスタンスを生成します。
	 */
	private void initRecPluginAll() {
		//
		recorders.clear();
		for ( RecorderInfo ri : recInfoList ) {
			ArrayList<HDDRecorder> rl = recPlugins.findPlugin(ri.getRecorderId());
			if ( rl.size() == 0 ) {
				stwin.appendError("【レコーダプラグイン】プラグインがみつかりません: "+ri.getRecorderId()+"("+ri.getRecorderIPAddr()+":"+ri.getRecorderPortNo()+")");
			}
			else {
				stwin.appendMessage("【レコーダプラグイン】プラグインを初期化します: "+ri.getRecorderId()+"("+ri.getRecorderIPAddr()+":"+ri.getRecorderPortNo()+")");
				for ( HDDRecorder rPlugin : rl ) {
					initRecPlugin(rPlugin, ri);
				}
			}
		}
	}
	protected HDDRecorder initRecPlugin(HDDRecorder rPlugin, RecorderInfo ri) {
		HDDRecorder rec = rPlugin.clone();
		recorders.add(rec);

		rec.getChCode().load(env.getDebug());	// true : ログ出力あり
		setSettingRecPluginBase(rec, ri);
		setSettingRecPluginExt(rec,env);
		rec.setProgressArea(stwin);
		return rec;
	}
	protected void setSettingRecPluginBase(HDDRecorder to, RecorderInfo from) {
		to.setName(from.getRecorderName());
		to.setIPAddr(from.getRecorderIPAddr());
		to.setPortNo(from.getRecorderPortNo());
		to.setUser(from.getRecorderUser());
		to.setPasswd(from.getRecorderPasswd());
		to.setMacAddr(from.getRecorderMacAddr());
		to.setBroadcast(from.getRecorderBroadcast());
		to.setUseCalendar(from.getUseCalendar());
		to.setUseChChange(from.getUseChChange());
		to.setRecordedCheckScope(from.getRecordedCheckScope());
		to.setTunerNum(from.getTunerNum());
		to.setColor(from.getRecorderColor());
	}
	protected void setSettingRecPluginExt(HDDRecorder recorder, Env nEnv) {
		recorder.setUserAgent(nEnv.getUserAgent());
		recorder.setDebug(nEnv.getDebug());
		recorder.setAdjNotRep(nEnv.getAdjoiningNotRepetition());
		recorder.setRecordedSaveScope(nEnv.getRecordedSaveScope());
	}

	//
	protected void doRecWakeup() {
		for ( HDDRecorder rec : recorders ) {
			if ( ! rec.getMacAddr().equals("") && ! rec.getBroadcast().equals("") ) {
				rec.wakeup();
			}
		}
	}

	/**
	 * 一時間は再実行させないんだ
	 */
	private boolean isOLPExpired(int expire) {
		String fname = "env"+File.separator+"olp.history";
		if ( ! new File(fname).exists() || ! new File(fname).canWrite() ) {
			stwin.appendError("【警告】実行履歴ファイルがないから実行させないよ！");
			ringBeep();
			return false;
		}
		String dat = CommonUtils.read4file(fname, true);
		if ( dat == null ) {
			stwin.appendError("【警告】実行履歴を取得できなかったから実行させないよ！");
			ringBeep();
			return false;
		}
		GregorianCalendar ca = null;
		dat = EncryptPassword.dec(b64.dec(dat));
		if ( dat != null ) {
			ca = CommonUtils.getCalendar(dat);
		}
		if ( ca == null ) {
			stwin.appendError("【警告】実行履歴の内容が不正だったから実行させないよ！ "+dat);
			ringBeep();
			return false;
		}
		if ( CommonUtils.getCompareDateTime(ca, CommonUtils.getCalendar(-expire*3600)) >= 0 ) {
			ca.add(Calendar.HOUR,expire);
			stwin.appendError("【警告】"+expire+"時間以内の再実行は許さないよ！"+CommonUtils.getDateTime(ca)+"まで待って！");
			ringBeep();
			return false;
		}
		if ( ! CommonUtils.write2file(fname, b64.enc(EncryptPassword.enc(CommonUtils.getDateTime(0)))) ) {
			stwin.appendError("【警告】実行履歴を保存できなかったから実行させないよ！");
			ringBeep();
			return false;
		}

		return true;
	}

	/**
	 *  Web番組表プラグインをすべて読み込みます。
	 */
	private boolean loadProgPlugins() {

		final String FUNCID = "[Web番組表プラグイン組込] ";
		final String ERRID = "[ERROR]"+FUNCID;

		// Web番組表プラグインの処理
		stwin.appendMessage(FUNCID+"プラグインを読み込みます.");

		// Web番組表共通設定
		setSettingProgPluginCommon(env);

		/*
		 * 重要 - ここから
		 */

		// TVProgramListのインスタンスは別途初期化が必要
		progPlugins.clear();
		tvprograms.clear();

		/*
		 * 重要 - ここまで
		 */

		ArrayList<String> prgIda = new ArrayList<String>();
		for ( File f : new File(CommonUtils.joinPath("bin","tainavi","plugintv")).listFiles() ) {
			Matcher ma = Pattern.compile("^(PlugIn_(TV|CS|RAD)P[^$]+)\\.class$").matcher(f.getName());
			if (ma.find()) {
				prgIda.add(ma.group(1));
			}
		}
		String[] prgIdd = prgIda.toArray(new String[0]);
		Arrays.sort(prgIdd);

		// servicesに追記
		StringBuilder sb = new StringBuilder();
		for ( String prgId : prgIdd ) {
			sb.append("tainavi.plugintv.");
			sb.append(prgId);
			sb.append("\n");
		}
		if ( ! CommonUtils.write2file(CommonUtils.joinPath("bin","META-INF","services","tainavi.TVProgram"), sb.toString()) ) {
			stwin.appendError(ERRID+"プラグインの読み込みに失敗しました: ");
			return false;
		}

		ServiceLoader<TVProgram> p = ServiceLoader.load(TVProgram.class);

		// 実際必要ないのだが、プラグインのインスタンスはclone()して使う
		for ( TVProgram pg : p ) {
			TVProgram prog = pg.clone();

			stwin.appendMessage("+追加しました: "+prog.getTVProgramId());

			// CH設定タブではプラグイン側のインスタンスを使うので情報を追加してやる必要があるのであった
			setSettingProgPlugin(prog, env);

			progPlugins.add(prog);
		}

		p = null;

		return true;
	}

	/**
	 *  設定にあわせてWeb番組表プラグインを絞り込みます。
	 */
	private void setSelectedProgPlugin() {

		// この３つは保存しておく
		Syobocal syobo = tvprograms.getSyobo();
		PassedProgram passed = tvprograms.getPassed();
		PickedProgram pickup = tvprograms.getPickup();
		SearchResult searched = tvprograms.getSearched();

		tvprograms.clear();

		{
			TVProgram tvp = progPlugins.getTvProgPlugin(env.getTVProgramSite());
			if ( tvp == null ) {
				// デフォルトもなければ先頭にあるもの
				tvp = progPlugins.getTvProgPlugin(null);
			}
			if ( tvp == null ) {
				// てか一個もなくね？
				StdAppendError("【Web番組表選択】地上波＆ＢＳ番組表が選択されていません: "+env.getTVProgramSite());
			}
			else {
				StdAppendMessage("【Web番組表選択】地上波＆ＢＳ番組表が選択されました: "+tvp.getTVProgramId());
				tvprograms.add(tvp.clone());
			}
		}
		{
			TVProgram tvp = progPlugins.getCsProgPlugin(env.getCSProgramSite());
			if ( tvp == null ) {
				tvp = progPlugins.getCsProgPlugin(null);
			}
			if ( tvp == null ) {
				StdAppendError("【Web番組表選択】ＣＳ番組表[プライマリ]が選択されていません： "+env.getCSProgramSite());
			}
			else {
				StdAppendMessage("【Web番組表選択】ＣＳ番組表[プライマリ]が選択されました: "+tvp.getTVProgramId());
				tvprograms.add(tvp.clone());
			}
		}
		{
			TVProgram tvp = progPlugins.getCs2ProgPlugin(env.getCS2ProgramSite());
			if ( tvp == null ) {
				tvp = progPlugins.getCs2ProgPlugin(null);
			}
			if ( tvp == null ) {
				StdAppendError("【Web番組表選択】ＣＳ番組表[プライマリ]が選択されていません： "+env.getCS2ProgramSite());
			}
			else {
				StdAppendMessage("【Web番組表選択】ＣＳ番組表[プライマリ]が選択されました: "+tvp.getTVProgramId());
				tvprograms.add(tvp.clone());
			}
		}
		/*
		if ( progPlugins.getRadioProgPlugins().size() > 0 )
		{
			TVProgram tvp = progPlugins.getCsProgPlugin(env.getRadioProgramSite());
			if ( tvp == null ) {
				tvp = progPlugins.getCsProgPlugin(null);
			}
			if ( tvp == null ) {
				StdAppendError("【Web番組表選択】ラジオ番組表が選択されていません： "+env.getRadioProgramSite());
			}
			else {
				StdAppendMessage("【Web番組表選択】ラジオ番組表が選択されました: "+tvp.getTVProgramId());
				tvprograms.add(tvp.clone());
			}
		}
		*/

		{
			if ( syobo == null ) {
				syobo = new Syobocal();
			}
			tvprograms.add(syobo);
		}
		{
			if ( passed == null ) {
				passed = new PassedProgram();
			}
			tvprograms.add(passed);
		}
		{
			if ( pickup == null ) {
				pickup = new PickedProgram();
				pickup.loadProgram(null, false);
			}
			tvprograms.add(pickup);
		}
		{
			if ( searched == null ) {
				searched = new SearchResult();
			}
			tvprograms.add(searched);
		}
	}

	/**
	 * Web番組表設定をもとにレコーダプラグインのインスタンスを生成します。
	 */
	private void initProgPluginAll() {

		final String FUNCID = "[Web番組表プラグイン初期化] ";
		final LinkedHashMap<ArrayList<TVProgram>,String> map = new LinkedHashMap<ArrayList<TVProgram>, String>();
		map.put(tvprograms.getTvProgPlugins(), "地上波＆ＢＳ番組表");
		map.put(tvprograms.getCsProgPlugins(), "ＣＳ番組表[プライマリ]");
		map.put(tvprograms.getCs2ProgPlugins(), "ＣＳ番組表[セカンダリ]");
		//map.put(progPlugins.getRadioProgPlugins(), "ラジオ番組表");

		new SwingBackgroundWorker(true) {

			@Override
			protected Object doWorks() throws Exception {

				for ( ArrayList<TVProgram> tvpa : map.keySet() ) {
					stwin.appendMessage(FUNCID+map.get(tvpa)+"のベース情報（放送局リストなど）を取得します.");
					for ( TVProgram p : tvpa ) {
						stwin.appendMessage(FUNCID+"プラグインを初期化します： "+p.getTVProgramId());

						try {
							// 個別設定（２）　…（１）と（２）の順番が逆だったので前に移動してきました(3.17.3β）
							setSettingProgPlugin(p,env);				// 他からも呼び出される部分だけ分離

							// 個別設定（１）
							p.setOptString(null);						// フリーオプション初期化
							p.loadAreaCode();							// 放送エリア情報取得
							p.loadCenter(p.getSelectedCode(),false);	// 放送局情報取得
							p.setSortedCRlist();						// 有効放送局だけよりわける
						}
						catch (Exception e) {
							stwin.appendError(FUNCID+"ベース情報の取得に失敗しました.");
							e.printStackTrace();
						}
					}
				}

				// 共通設定部分の一斉更新
				//setSettingProgPluginAll(env);

				if ( env.getUseSyobocal() ) {
					TVProgram syobo = tvprograms.getSyobo();
					if ( syobo != null ) {
						stwin.appendMessage(FUNCID+"しょぼかるを初期化します.");
						setSettingProgPlugin(syobo,env);				// 他からも呼び出される部分だけ分離
						syobo.setUserAgent("tainavi");
						syobo.setOptString(null);						// フリーオプション初期化
						syobo.loadCenter(syobo.getSelectedCode(), false);
					}
				}

				return null;
			}

			@Override
			protected void doFinally() {
			}
		}.execute();
	}
	protected void setSettingProgPluginAll(Env nEnv) {
		// 通常
		setSettingProgPlugin(tvprograms.getTvProgPlugin(null),nEnv);
		setSettingProgPlugin(tvprograms.getCsProgPlugin(null),nEnv);
		setSettingProgPlugin(tvprograms.getCs2ProgPlugin(null),nEnv);
		//setSettingProgPlugin(tvprograms.getRadioProgPlugin(null),nEnv);
		setSettingProgPlugin(tvprograms.getSyobo(),nEnv);

		// しょぼかるは特殊
		tvprograms.getSyobo().setUserAgent("tainavi");
		// 検索結果も特殊
		tvprograms.getSearched().setResultBufferMax(nEnv.getSearchResultBufferMax());
	}
	protected void setSettingProgPlugin(TVProgram p, Env nEnv) {
		if ( p == null ) {
			return;
		}
		p.setUserAgent(nEnv.getUserAgent());
		p.setProgDir(nEnv.getProgDir());
		p.setCacheExpired((enableWebAccess)?(nEnv.getCacheTimeLimit()):(0));
		p.setContinueTomorrow(nEnv.getContinueTomorrow());
		p.setExpandTo8(nEnv.getExpandTo8());
		//p.setUseDetailCache(nEnv.getUseDetailCache());
		p.setUseDetailCache(false);
		p.setSplitEpno(nEnv.getSplitEpno());
	}

	/**
	 * staticで持っている共通設定の更新
	 */
	protected void setSettingProgPluginCommon(Env nEnv) {

		if ( nEnv.getUseProxy() && (nEnv.getProxyAddr().length() > 0 && nEnv.getProxyPort().length() > 0) ) {
			stwin.appendMessage("＋Web番組表へのアクセスにProxyが設定されています： "+nEnv.getProxyAddr()+":"+nEnv.getProxyPort());
			TVProgramUtils.setProxy(nEnv.getProxyAddr(),nEnv.getProxyPort());
		}
		else {
			TVProgramUtils.setProxy(null,null);
		}

		TVProgramUtils.setProgressArea(stwin);
		TVProgramUtils.setChConv(chconv);
		TVProgramUtils.setUseProgCache(nEnv.getUseProgCache());
	}

	//
	private void initMpList() {
		//mpList = new MarkedProgramList();			// 検索結果リスト
		mpList.setHistoryOnlyUpdateOnce(env.getHistoryOnlyUpdateOnce());
		mpList.setShowOnlyNonrepeated(env.getShowOnlyNonrepeated());
	}

	// L&FとFontを設定
	private void initLookAndFeelAndFont() {

		try {
			{
				vwlaf = new VWLookAndFeel();

				String lafname = vwlaf.update(env.getLookAndFeel());
				if ( lafname != null && ! lafname.equals(env.getLookAndFeel())) {
					env.setLookAndFeel(lafname);
				}

				if ( CommonUtils.isMac() ) {
					UIManager.getDefaults().put("Table.gridColor", new Color(128,128,128));
					//UIManager.getDefaults().put("Table.selectionBackground", new Color(182,207,229));
					//UIManager.getDefaults().put("Table.selectionForeground", new Color(0,0,0));
				}
			}

			{
				vwfont = new VWFont();

				String fname = vwfont.update(env.getFontName(),env.getFontSize());
				if ( fname != null && ! fname.equals(env.getFontName())) {
					env.setFontName(fname);
				}
			}
		}
		catch ( Exception e ) {
			// 落ちられると困るからトラップしておこうぜ
			e.printStackTrace();
		}
	}

	// L&FやFontを変えたらコンポーネントに通知が必要
	protected void updateComponentTreeUI() {
		try {
			SwingUtilities.updateComponentTreeUI(this);
			SwingUtilities.updateComponentTreeUI(stwin);
			SwingUtilities.updateComponentTreeUI(mwin);
			if (rdialog != null)
				SwingUtilities.updateComponentTreeUI(rdialog);
			if (ccwin != null)
				SwingUtilities.updateComponentTreeUI(ccwin);
		}
		catch ( Exception e ) {
			// 落ちられると困るからトラップしておこうぜ
			e.printStackTrace();
		}
	}

	// ツールチップの表示遅延時間を設定する
	private void setTooltipDelay() {
		ToolTipManager tp = ToolTipManager.sharedInstance();
		tp.setInitialDelay(env.getTooltipInitialDelay()*100);
		tp.setDismissDelay(env.getTooltipDismissDelay()*100);
	}

	/**
	 *
	 * @return true:前回終了時の設定がある場合
	 */
	private boolean buildMainWindow() {

		// コンポーネント作成
		{
			ccwin = new VWColorChooserDialog();
			pcwin = new VWPaperColorsDialog();
			rdialog = new VWReserveDialog(0, 0);

			// メインウィンドウの作成
			mainWindow = new VWMainWindow(this);

			// 内部クラスのインスタンス生成
			toolBar = new VWToolBar();
			listed = new VWListedView();
			paper = new VWPaperView();
			reserved = new VWReserveListView();
			recorded = new VWRecordedListView();
			autores = new VWAutoReserveListView();
			titled = new VWTitleListView();
			setting = new VWSettingView();
			recsetting = new VWRecorderSettingView();
			chsetting = new VWChannelSettingView();
			chdatsetting = new VWChannelDatSettingView();
			chsortsetting = new VWChannelSortView();
			chconvsetting = new VWChannelConvertView();
		}

		// 初期値
		{
			// 設定
			toolBar.setDebug(env.getDebug());
			autores.setDebug(env.getDebug());
			rdialog.setDebug(env.getDebug());

			// ページャーの設定
			toolBar.setPagerItems();
		}

		// コンポーネントの組み立て
		{
			// ツールバーなど
			mainWindow.addToolBar(toolBar);
			mainWindow.addStatusArea(mwin);

			// タブ群
//			addAllTabs();
		}

		// ステータスエリアを開く
		setStatusVisible(bounds.getShowStatus());

		//新聞描画枠のリセット
		paper.clearPanel();
		paper.buildMainViewByDate();

		return true;
	}

	/*
	 * すべてのタブを追加する
	 */
	private void addAllTabs(){
		mainWindow.addTab(listed, MWinTab.LISTED);
		mainWindow.addTab(paper, MWinTab.PAPER);
		mainWindow.addTab(reserved, MWinTab.RSVED);
		mainWindow.addTab(recorded, MWinTab.RECED);
		mainWindow.addTab(autores, MWinTab.AUTORES);
		mainWindow.addTab(titled, MWinTab.TITLED);
		mainWindow.addTab(setting, MWinTab.SETTING);
		mainWindow.addTab(recsetting, MWinTab.RECSET);
		mainWindow.addTab(chsetting, MWinTab.CHSET);
		mainWindow.addTab(chsortsetting, MWinTab.CHSORT);
		mainWindow.addTab(chconvsetting, MWinTab.CHCONV);
		mainWindow.addTab(chdatsetting, MWinTab.CHDAT);
		mainWindow.initSettingTabSelect();
	}

	private void ShowInitTab() {

		// いったん無選択状態にしてから
		mainWindow.setSelectedTab(null);

		if ( recInfoList.size() <= 0 ) {
			// 設定が存在しない場合
			mainWindow.setSelectedTab(MWinTab.RECSET);
		}
		else {
			// 設定が存在する場合
			MWinTab tab = MWinTab.getAt(bounds.getSelectedTab());
			mainWindow.setSelectedTab(tab);
		}
	}

	//
	private void setInitBounds() {
		// ウィンドウのサイズと表示位置を設定する
		Rectangle window = bounds.getWinRectangle();
		if (bounds.isLoaded()) {
			// 設定ファイルを読み込んであったらそれを設定する
			System.out.println(DBGID+"set bounds "+window);
			this.setBounds(window.x, window.y, window.width, window.height);
		}
		else {
			// 設定ファイルがなければ自動設定する
			Rectangle screen = this.getGraphicsConfiguration().getBounds();
			int x = 0;
			int w = window.width;
			if (window.width > screen.width) {
				x = 0;
				w = screen.width;
			}
			else {
				x = (screen.width - window.width)/2;
			}
			int y = 0;
			int h = window.height;
			if (window.height > screen.height) {
				y = 0;
				h = screen.height;
			}
			else {
				y = (screen.height - window.height)/2;
			}
			this.setBounds(x, y, w, h);
		}
	}

	/**
	 * <P>ステータスエリアを隠す
	 * {@link VWMainWindow#setStatusVisible(boolean)}の置き換え
	 */
	private void setStatusVisible(boolean b) {

		if (b) {
			listed.setDetailVisible(true);
			paper.setDetailVisible(true);
			reserved.setDetailVisible(true);
			titled.setDetailVisible(true);
			MWinSetVisible(true);
		}
		else {
			listed.setDetailVisible(false);
			paper.setDetailVisible(false);
			reserved.setDetailVisible(false);
			titled.setDetailVisible(false);
			MWinSetVisible(false);
		}
	}

	// フルスクリーンモードをトグル切り替え
	private Dimension f_dim;
	private Point f_pnt;
	private int divloc_l = 0;
	private int divloc_p = 0;

	private void setFullScreen(boolean b) {

		if ( b == true ) {
			// 枠の撤去
			this.dispose();
			this.setUndecorated(true);
			this.setVisible(true);

			//全画面表示へ
			Toolkit tk = getToolkit();
			Insets in = tk.getScreenInsets(getGraphicsConfiguration());
			Dimension d = tk.getScreenSize();
			f_dim = this.getSize();
			f_pnt = this.getLocation();
			this.setBounds(in.left, in.top, d.width-(in.left+in.right), d.height-(in.top+in.bottom));

			divloc_l = bounds.getTreeWidth();
			divloc_p = bounds.getTreeWidthPaper();

			// ツリーを閉じる
			paper.setCollapseTree();
			listed.setCollapseTree();
		}
		else {
			if ( f_pnt != null && f_dim != null ) {	// 起動直後などは値がないですしね

				// 枠の復帰
				this.dispose();
				this.setUndecorated(false);
				this.setVisible(true);

				//全画面表示終了
				this.setBounds(f_pnt.x, f_pnt.y, f_dim.width, f_dim.height);

				bounds.setTreeWidth(divloc_l);
				bounds.setTreeWidthPaper(divloc_p);

				// ツリーの幅を元に戻す
				paper.setExpandTree();
				listed.setExpandTree();
			}
		}
	}

	// タイトルバー
	private void setTitleBar() {
		MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapUsage = mbean.getHeapMemoryUsage();

		this.setTitle(
				String.format(
						"%s - %s - Memory Usage Max:%dM Committed:%dM Used:%dM - FrameBuffer Status:%s",
						VersionInfo.getVersion(),
						CommonUtils.getDateTime(0),
						heapUsage.getMax()/(1024*1024),
						heapUsage.getCommitted()/(1024*1024),
						heapUsage.getUsed()/(1024*1024),
						(paper!=null)?(paper.getFrameBufferStatus()):("N/A")
				)
		);
	}

	// 終了処理関連
	private void ExitOnClose() {
		// 座標・サイズ
		if ( ! this.toolBar.isFullScreen()) {
			Rectangle r = this.getBounds();
			bounds.setWinRectangle(r);
		}
		else {
			Rectangle r = new Rectangle();
			r.x = this.f_pnt.x;
			r.y = this.f_pnt.y;
			r.width = this.f_dim.width;
			r.height = this.f_dim.height;
			bounds.setWinRectangle(r);
		}
		listed.copyColumnWidth();
		reserved.copyColumnWidth();
		titled.copyColumnWidth();
		recorded.copyColumnWidth();

//		bounds.setStatusRows(mwin.getRows());

		// 動作状態
		bounds.setSelectedTab(mainWindow.getSelectedTab().getIndex());
		bounds.setShowSettingTabs(mainWindow.getShowSettingTabs());
		bounds.setSelectedRecorderId(toolBar.getSelectedRecorder());
		bounds.setShowStatus(toolBar.isStatusShown());

		// 保存する
		bounds.save();

		// ツリーの展開状態の保存
		listed.saveTreeExpansion();
		paper.saveTreeExpansion();
	}

	private void setKeyboardShortCut() {
		class ShortCut {
			final String action;
			final int key;
			final int mask;
			final Action callback;

			public ShortCut(String aAction, int aKey, int aMask, Action aCallback) {
				action = aAction;
				key = aKey;
				mask = aMask;
				callback = aCallback;
			}
		}

		final String FIND_ACTION = "find";
		final String SELECT_ACTION_LISTTAB = "listtab";
		final String SELECT_ACTION_PAPERTAB = "papertab";
		final String SELECT_ACTION_RSVEDTAB = "rsvedtab";
		final String SELECT_ACTION_RECEDTAB = "recedtab";
		final String SELECT_ACTION_AUTORESTAB = "autorestab";
		final String SELECT_ACTION_TITLETAB = "titletab";
		final String SELECT_ACTION_SETTINGTAB = "settingtab";

		final String BUTTON_ACTION_KEYWORD_ADDED = "keyword_added";
		final String BUTTON_ACTION_KEYWORD_DELETED = "keyword_deleted";
		final String BUTTON_ACTION_SEARCH_DIALOG = "search_dialog";
		final String BUTTON_ACTION_RELOAD_PROGRAM = "reload_program";
		final String BUTTON_ACTION_SHOW_BORDER = "show_border";
		final String BUTTON_ACTION_JUMP_TO_NOW = "jump_to_now";
		final String BUTTON_ACTION_BATCH_RESERVE = "batch_reserve";
		final String BUTTON_ACTION_RELOAD_RESERVED = "reload_reserved";
		final String BUTTON_ACTION_SNAPSHOT = "snapshot";
		final String BUTTON_ACTION_SHOW_PAPERCOLORDIALOG = "show_papercolordialog";
		final String SLIDER_ACTION_UP_PAPERZOOM = "up_paperzoom";
		final String SLIDER_ACTION_DOWN_PAPERZOOM = "down_paperzoom";
		final String BUTTON_ACTION_TOGGLE_SHOWSTATUS = "toggle_showstatus";
		final String BUTTON_ACTION_TOGGLE_FULLSCREEN = "toggle_fullscreen";
		final String BUTTON_ACTION_LOGVIEW = "logview";
		final String BUTTON_ACTION_OPEN_HELP = "open_help";

		final String SCROLL_ACTION_PREV_PAPERNODE = "prevpapernode";
		final String SCROLL_ACTION_NEXT_PAPERNODE = "nextpapernode";

		final String SCROLL_ACTION_PREV_PAGE = "prevpage";
		final String SCROLL_ACTION_NEXT_PAGE = "nextpage";

		final String BUTTON_ACTION_NEXT_CENTERPAGE = "nextcenterpage";
		final String BUTTON_ACTION_PREV_CENTERPAGE = "prevcenterpage";

		final String BUTTON_ACTION_NEXT_DATE_INDEX = "nextdateindex";
		final String BUTTON_ACTION_PREV_DATE_INDEX = "prevdateindex";

		final Action find_action =  new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.setFocusInSearchBox();
			}
		};
		final Action select_action_listtab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.LISTED);
			}
		};
		final Action select_action_papertab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.PAPER);
			}
		};
		final Action select_action_rsvedtab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.RSVED);
			}
		};
		final Action select_action_recedtab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.RECED);
			}
		};
		final Action select_action_autorestab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.AUTORES);
			}
		};
		final Action select_action_titletab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.TITLED);
			}
		};
		final Action select_action_settingtab = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.setSelectedTab(MWinTab.SETTING);
			}
		};

		final Action sc_keyword_added = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doKeywordAdded();
			}
		};
		final Action sc_keyword_deleted = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doKeywordDeleted();
			}
		};
		final Action sc_search_dialog = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.openSearchDialog();
			}
		};
		final Action sc_reload_program = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doLoadTVProgram(null);
			}
		};
		final Action sc_jump_to_now = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.jumpToNow();
			}
		};
		final Action sc_show_border = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doShowBorder();
			}
		};
		final Action sc_batch_reserve = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doBatchReserve();
			}
		};
		final Action sc_reload_reserved = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doReloadReserved();
			}
		};
		final Action sc_up_paperzoom = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doChangePaperZoom(true);
			}
		};
		final Action sc_down_paperzoom = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doChangePaperZoom(false);
			}
		};
		final Action sc_show_papercolordialog = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.setPaperColorDialogVisible(true);
			}
		};
		final Action sc_snapshot = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.takeSnapShot();
			}
		};
		final Action sc_toggle_showstatus = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doToggleShowStatus();
			}
		};
		final Action sc_toggle_fullscreen = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doFullScreen();
			}
		};
		final Action sc_logview = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doLogView();
			}
		};
		final Action sc_open_help = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toolBar.doOpenHelp();
			}
		};

		final Action sc_prev_papernode = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!mainWindow.isTabSelected(MWinTab.PAPER))
					return;

				paper.changeNode(false);
			}
		};
		final Action sc_next_papernode = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!mainWindow.isTabSelected(MWinTab.PAPER))
					return;

				paper.changeNode(true);
			}
		};

		final Action sc_prev_page = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(mainWindow.getSelectedTab()){
				case PAPER:
					paper.scrollPage(false, (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
					break;
				default:
					break;
				}
			}
		};
		final Action sc_next_page = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(mainWindow.getSelectedTab()){
				case PAPER:
					paper.scrollPage(true, (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
					break;
				default:
					break;
				}
			}
		};

		final Action sc_prev_centerpage = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(mainWindow.getSelectedTab()){
				case PAPER:
					toolBar.moveToPrevPage();
					break;
				default:
					break;
				}
			}
		};
		final Action sc_next_centerpage = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(mainWindow.getSelectedTab()){
				case PAPER:
					toolBar.moveToNextPage();
					break;
				default:
					break;
				}
			}
		};

		final Action sc_prev_date_index = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(mainWindow.getSelectedTab()){
				case PAPER:
					paper.moveToPrevDateIndex();
					break;
				default:
					break;
				}
			}
		};
		final Action sc_next_date_index = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				switch(mainWindow.getSelectedTab()){
				case PAPER:
					paper.moveToNextDateIndex();
					break;
				default:
					break;
				}
			}
		};

		ArrayList<ShortCut> sca = new ArrayList<ShortCut>();
		sca.add(new ShortCut(FIND_ACTION, KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK, find_action));
		sca.add(new ShortCut(FIND_ACTION, KeyEvent.VK_F1, 0, find_action));
		sca.add(new ShortCut(SELECT_ACTION_LISTTAB, KeyEvent.VK_1, KeyEvent.ALT_DOWN_MASK, select_action_listtab));
		sca.add(new ShortCut(SELECT_ACTION_PAPERTAB, KeyEvent.VK_2, KeyEvent.ALT_DOWN_MASK, select_action_papertab));
		sca.add(new ShortCut(SELECT_ACTION_RSVEDTAB, KeyEvent.VK_3, KeyEvent.ALT_DOWN_MASK, select_action_rsvedtab));
		sca.add(new ShortCut(SELECT_ACTION_RECEDTAB, KeyEvent.VK_4, KeyEvent.ALT_DOWN_MASK, select_action_recedtab));
		sca.add(new ShortCut(SELECT_ACTION_AUTORESTAB, KeyEvent.VK_5, KeyEvent.ALT_DOWN_MASK, select_action_autorestab));
		sca.add(new ShortCut(SELECT_ACTION_TITLETAB, KeyEvent.VK_6, KeyEvent.ALT_DOWN_MASK, select_action_titletab));
		sca.add(new ShortCut(SELECT_ACTION_SETTINGTAB, KeyEvent.VK_7, KeyEvent.ALT_DOWN_MASK, select_action_settingtab));

		sca.add(new ShortCut(BUTTON_ACTION_KEYWORD_ADDED, KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK, sc_keyword_added));
		sca.add(new ShortCut(BUTTON_ACTION_KEYWORD_DELETED, KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK, sc_keyword_deleted));
		sca.add(new ShortCut(BUTTON_ACTION_SEARCH_DIALOG, KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK, sc_search_dialog));

		sca.add(new ShortCut(BUTTON_ACTION_RELOAD_PROGRAM, KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK, sc_reload_program));
		sca.add(new ShortCut(BUTTON_ACTION_RELOAD_PROGRAM, KeyEvent.VK_F2, 0, sc_reload_program));

		sca.add(new ShortCut(BUTTON_ACTION_SHOW_BORDER, KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, sc_show_border));

		sca.add(new ShortCut(BUTTON_ACTION_JUMP_TO_NOW, KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, sc_jump_to_now));
		sca.add(new ShortCut(BUTTON_ACTION_JUMP_TO_NOW, KeyEvent.VK_F3, 0, sc_jump_to_now));

		sca.add(new ShortCut(BUTTON_ACTION_BATCH_RESERVE, KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK, sc_batch_reserve));

		sca.add(new ShortCut(BUTTON_ACTION_RELOAD_RESERVED, KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK, sc_reload_reserved));
		sca.add(new ShortCut(BUTTON_ACTION_RELOAD_RESERVED, KeyEvent.VK_F4, 0, sc_reload_reserved));

		sca.add(new ShortCut(BUTTON_ACTION_SHOW_PAPERCOLORDIALOG, KeyEvent.VK_C, KeyEvent.ALT_DOWN_MASK, sc_show_papercolordialog));

		sca.add(new ShortCut(BUTTON_ACTION_SNAPSHOT, KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, sc_snapshot));

		sca.add(new ShortCut(BUTTON_ACTION_TOGGLE_SHOWSTATUS, KeyEvent.VK_S, KeyEvent.ALT_DOWN_MASK, sc_toggle_showstatus));

		sca.add(new ShortCut(BUTTON_ACTION_TOGGLE_FULLSCREEN, KeyEvent.VK_F, KeyEvent.ALT_DOWN_MASK, sc_toggle_fullscreen));
		sca.add(new ShortCut(BUTTON_ACTION_TOGGLE_FULLSCREEN, KeyEvent.VK_F11, 0, sc_toggle_fullscreen));

		sca.add(new ShortCut(BUTTON_ACTION_LOGVIEW, KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK, sc_logview));

		sca.add(new ShortCut(BUTTON_ACTION_OPEN_HELP, KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK, sc_open_help));
		sca.add(new ShortCut(BUTTON_ACTION_OPEN_HELP, KeyEvent.VK_F12, 0, sc_open_help));

		sca.add(new ShortCut(SLIDER_ACTION_DOWN_PAPERZOOM, KeyEvent.VK_F9, 0, sc_down_paperzoom));
		sca.add(new ShortCut(SLIDER_ACTION_UP_PAPERZOOM, KeyEvent.VK_F10, 0, sc_up_paperzoom));

		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAPERNODE, KeyEvent.VK_F5, 0, sc_prev_papernode));
		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAPERNODE, KeyEvent.VK_F6, 0, sc_next_papernode));
		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAPERNODE, KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK, sc_prev_papernode));
		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAPERNODE, KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK, sc_next_papernode));

		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAGE, KeyEvent.VK_F7, 0, sc_prev_page));
		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAGE, KeyEvent.VK_F8, 0, sc_next_page));
		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAGE, KeyEvent.VK_F7, KeyEvent.SHIFT_DOWN_MASK, sc_prev_page));
		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAGE, KeyEvent.VK_F8, KeyEvent.SHIFT_DOWN_MASK, sc_next_page));

		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAGE, KeyEvent.VK_PAGE_UP, KeyEvent.ALT_DOWN_MASK, sc_prev_page));
		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAGE, KeyEvent.VK_PAGE_DOWN, KeyEvent.ALT_DOWN_MASK, sc_next_page));
		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAGE, KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, sc_prev_page));
		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAGE, KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, sc_next_page));

		sca.add(new ShortCut(BUTTON_ACTION_PREV_CENTERPAGE, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK, sc_prev_centerpage));
		sca.add(new ShortCut(BUTTON_ACTION_NEXT_CENTERPAGE, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, sc_next_centerpage));

		sca.add(new ShortCut(BUTTON_ACTION_PREV_DATE_INDEX, KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, sc_prev_date_index));
		sca.add(new ShortCut(BUTTON_ACTION_NEXT_DATE_INDEX, KeyEvent.VK_RIGHT,  KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, sc_next_date_index));

		//		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAGE, KeyEvent.VK_PAGE_UP, 0, sc_prev_page));
//		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAGE, KeyEvent.VK_PAGE_DOWN, 0, sc_next_page));
//
//		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAGE, KeyEvent.VK_PAGE_UP, KeyEvent.SHIFT_DOWN_MASK, sc_prev_page));
//		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAGE, KeyEvent.VK_PAGE_DOWN, KeyEvent.SHIFT_DOWN_MASK, sc_next_page));
//
//		sca.add(new ShortCut(SCROLL_ACTION_PREV_PAPERNODE, KeyEvent.VK_PAGE_UP, KeyEvent.CTRL_DOWN_MASK, sc_prev_papernode));
//		sca.add(new ShortCut(SCROLL_ACTION_NEXT_PAPERNODE, KeyEvent.VK_PAGE_DOWN, KeyEvent.CTRL_DOWN_MASK, sc_next_papernode));
//
		InputMap imap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		for ( ShortCut sc : sca ) {
			imap.put(KeyStroke.getKeyStroke(sc.key, sc.mask), sc.action);
			getRootPane().getActionMap().put(sc.action, sc.callback);
		}
	}

	private void checkDownloadProgramTime( GregorianCalendar gc){
		if (! env.getDownloadProgramOnFixedTime())
			return;

		GregorianCalendar c = (GregorianCalendar)gc.clone();
		c.setTimeZone(TimeZone.getDefault());

		String timeLast = "00:00";
		GregorianCalendar cl = CommonUtils.getCalendarFromString(env.getLastDownloadTime());

		if (cl != null){
			if (cl.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) &&
				cl.get(Calendar.HOUR_OF_DAY) == c.get(Calendar.HOUR_OF_DAY) &&
				cl.get(Calendar.MINUTE) == c.get(Calendar.MINUTE)){
				return;
			}

			if (cl.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR))
				timeLast = String.format("%02d:%02d",  cl.get(Calendar.HOUR_OF_DAY), cl.get(Calendar.MINUTE));
		}

		String timeNow = String.format("%02d:%02d",  c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));

		for (String time : env.getDownloadProgramTimeList().split(";")){
			if (time.compareTo(timeLast) <= 0)
				continue;

			if (time.compareTo(timeNow) <= 0){
				doLoadTVProgram(true, LoadFor.ALL, env.getDownloadProgramInBackground());
				return;
			}
		}
	}

	/*******************************************************************************
	 * main()
	 ******************************************************************************/

	// 初期化が完了したら立てる
	private static boolean initialized = false;
	private static Viewer myClass = null;

	/**
	 * めいーん
	 * @param args
	 * @throws NoSuchAlgorithmException
	 * @version 今まで初期化を行ってからウィンドウを作成していたが<BR>
	 * 途中で例外が起こるとダンマリの上にゾンビになってたりとヒドかったので<BR>
	 * 先にウィンドウを作成してから初期化を行うように変えました
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
	public static void main(final String[] args) throws NoSuchAlgorithmException, InvocationTargetException, InterruptedException {

		if ( myClass != null ) {
			// 既に起動していたらフォアグラウンドにする
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					// うーん、いいのかこのコード？
					myClass.setVisible(true);
					myClass.setState(Frame.NORMAL);
				}
			});
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				final Viewer thisClass = myClass = new Viewer(args);

				thisClass.addComponentListener(new ComponentAdapter() {
					@Override
					public void componentShown(ComponentEvent e) {

						// 一回実行したらもういらないよ
						thisClass.removeComponentListener(this);

						// 初期化するよ
						thisClass.initialize(args);

					}
				});

				thisClass.setVisible(true);
			}
		});
	}



	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	/**
	 * デフォルトコンストラクタ
	 */
	public Viewer(final String[] args) {

		super();

		env.loadText();
		bounds.loadText();


		// 初期化が終わるまでは閉じられないよ　→　どうせステータスウィンドウにブロックされて操作できない
		//setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//setResizable(false);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setTitleBar();

		try {
			Image image = ImageIO.read(new File(ICONFILE_TAINAVI));
			setIconImage(image);
		}
		catch (IOException e) {
			StdAppendError("[ERROR] アイコンが設定できない： "+e.toString());
		}

		JLabel jLabel_splash_img = new JLabel(new ImageIcon("splash.gif"));
		jLabel_splash_img.setPreferredSize(new Dimension(400,300));
		//getContentPane().setLayout(new BorderLayout());
		getContentPane().add(jLabel_splash_img, BorderLayout.CENTER);
		pack();

		setLocationRelativeTo(null);	// 画面の真ん中に

		// SwingLocker共有設定
		SwingLocker.setOwner(this);

		// とりあえずルックアンドフィールはリセットしておかないとだめっぽいよ
		initLookAndFeelAndFont();
		updateComponentTreeUI();
	}

	// 初期化をバックグラウンドで行う
	private void initialize(final String[] args) {

		StWinClear();

		// 初期化処理はバックグラウンドで行う
		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				TatCount tc = new TatCount();

				// 初期化処理
				_initialize(args);

				// 終わったら閉じられるようにするよ
				//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				//setResizable(true);

				stwin.append("");
				stwin.appendMessage(String.format("【タイニー番組ナビゲータが起動しました】 所要時間： %.2f秒",tc.end()));
				return null;
			}

			@Override
			protected void doFinally() {
				if ( ! initialized ) System.err.println("[ERROR][鯛ナビ] 【致命的エラー】 初期化処理を行っていたスレッドが異常終了しました。");
				stwin.setClosingEnabled(false);
				CommonUtils.milSleep(OPENING_WIAT);
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationUnder(this);
		StWinSetVisible(true);
	}

	// 初期化の本体
	private void _initialize(final String[] args) {

		// コマンドライン引数を処理する
		procArgs(args);

		// ログ出力を設定する（Windowsの場合は文字コードをMS932にする） →DOS窓を殺したので終了
		System.setOut(new DebugPrintStream(System.out,LOG_FILE,logging));
		System.setErr(new DebugPrintStream(System.err,LOG_FILE,logging));

		// 起動メッセージ
		StdAppendMessage("================================================================================");
		StdAppendMessage("以下のメッセージは無視してください（原因調査中）");
		StdAppendMessage("Exception occurred during event dispatching:");
		StdAppendMessage("	java.lang.NullPointerException");
		StdAppendMessage("		at javax.swing.plaf.basic.BasicScrollBarUI.layoutHScrollbar(Unknown Source)");
		StdAppendMessage("		（以下略）");
		StdAppendMessage("================================================================================");
		stwin.appendMessage(CommonUtils.getDateTime(0));
		stwin.appendMessage(String.format("タイニー番組ナビゲータが起動を開始しました(VersionInfo:%s on %s)",VersionInfo.getVersion(),VersionInfo.getEnvironment()));

		// 起動時にアップデートを確認する
		chkVerUp();

		try {
			// メインの環境設定ファイルを読み込む
			loadEnvfile();

			// 二重起動防止
			chkDualBoot();

			// その他の環境設定ファイルを読み込む
			procEnvs();

			if ( onlyLoadProgram ) {
				if ( ! isOLPExpired(4) ) {
					CommonUtils.milSleep(3000);
					System.exit(1);
				}
				// プラグインのロード
				loadProgPlugins();
				// プラグインの初期化
				setSelectedProgPlugin();
				initProgPluginAll();
				// 検索結果リストの初期化（loadTVProgram()中で使うので）
				initMpList();
				// データのロード
				loadTVProgram(true,LoadFor.ALL, false);
				stwin.appendMessage("番組表を取得したので終了します");
				CommonUtils.milSleep(3000);
				System.exit(1);
			}

			// プラグインのロード
			loadProgPlugins();
			loadRecPlugins();

			// プラグインの初期化
			setSelectedProgPlugin();
			initProgPluginAll();

			initRecPluginAll();

			// WOL指定があったなら
			if ( runRecWakeup ) {
				doRecWakeup();
			}

			// 検索結果リストの初期化（loadTVProgram()中で使うので）
			initMpList();

			// データのロード
			loadTVProgram(false,LoadFor.ALL, false);

			// 放送局の並び順もロード
			chsort.load();

			_loadRdRecorderAll(runRecLoad, null);
//			loadRdReservesAll(runRecLoad, null);

			// 録画タイトルを読み込む
//			loadRdTitlesAll(false, null);
		}
		catch ( Exception e ) {
			System.err.println("【致命的エラー】設定の初期化に失敗しました");
			e.printStackTrace();
			System.exit(1);
		}

		// （新聞形式の）ツールチップの表示時間を変更する
		setTooltipDelay();

		// ウィンドウを構築
		try {
			buildMainWindow();
		}
		catch ( Exception e ) {
			System.err.println("【致命的エラー】ウィンドウの構築に失敗しました");
			e.printStackTrace();
			System.exit(1);
		}

		// 背景色設定ダイアログにフォント名の一覧を設定する
		pcwin.setFontList(vwfont);

		// ★★★★★★★★★★
		//int x = 2/0;	// サブスレッドの突然死のトラップを確認するためのコード
		// ★★★★★★★★★★

		// トレイアイコンを作る
		getTrayIcon();
		setTrayIconVisible(env.getShowSysTray());

		// ウィンドウを閉じたときの処理
		setXButtonAction(env.getShowSysTray() && env.getHideToTray());

		// ウィンドウ操作のリスナー登録
		this.addWindowListener(new WindowAdapter() {
			// ウィンドウを最小化したときの処理
			@Override
			public void windowIconified(WindowEvent e) {
				HideToTray();
			}

			// ウィンドウを閉じたときの処理
			@Override
			public void windowClosing(WindowEvent e) {
				ExitOnClose();
			}
		});

		// 初回起動時はレコーダの登録を促す
		if ( recorders.size() == 0 ) {
			Container cp = getContentPane();
			JOptionPane.showMessageDialog(cp, "レコーダが登録されていません。\n最初に登録を行ってください。\n番組表だけを使いたい場合は、\nNULLプラグインを登録してください。");
		}

		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		// イベントリスナーの登録
		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★

		// [ツールバー/共通] レコーダ情報変更
		toolBar.addHDDRecorderChangeListener(autores);

		// [ツールバー/レコーダ選択]
		toolBar.addHDDRecorderSelectionListener(this);		// 新聞形式
		toolBar.addHDDRecorderSelectionListener(paper);		// 新聞形式
		toolBar.addHDDRecorderSelectionListener(autores);	// 自動予約一覧
		toolBar.addHDDRecorderSelectionListener(rdialog);	// 予約ダイアログ

		// [ツールバー/キーワード入力] キャンセル動作
		toolBar.addKeywordCancelListener(this);

		// [タイマー] タイトルバー更新／リスト形式の現在時刻ノード／新聞形式の現在時刻ノード
		timer_now.addTickTimerRiseListener(this);
		timer_now.addTickTimerRiseListener(listed);
		timer_now.addTickTimerRiseListener(paper);
		timer_now.addTickTimerRiseListener(reserved);

		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		// [Fire!] レコーダ選択
		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		toolBar.setSelectedRecorder(bounds.getSelectedRecorderId());

		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		// [Fire!] サイドツリーのデフォルトを選択することで番組情報の描画を開始する
		// ※ここ以前だとぬぽとかOOBとか出るかもよ！
		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		paper.selectTreeDefault();
		listed.selectTreeDefault();

		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		// メインウィンドウをスプラッシュからコンポーネントに入れ替える
		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		this.setVisible(false);
		this.setContentPane(mainWindow);
		addAllTabs();
		setInitBounds();
		this.setVisible(true);

		mainWindow.initBounds();

		setTitleBar();	// タイトルバー更新

		ShowInitTab();	// 前回開いていたタブを開く

		setKeyboardShortCut();

		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		// タイマーを起動する
		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		timer_now.start();

		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		// 初期化終了
		// ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
		mwin.appendMessage(String.format("タイニー番組ナビゲータが起動しました (VersionInfo:%s on %s)",VersionInfo.getVersion(),VersionInfo.getEnvironment()));
		initialized = true;
	}
}
