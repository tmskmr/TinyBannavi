package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import tainavi.Env.DblClkCmd;
import tainavi.Env.SnapshotFmt;
import tainavi.Env.UpdateOn;
import tainavi.TVProgram.ProgOption;


/**
 * 各種設定タブのクラス
 * @since 3.15.4β　{@link Viewer}から分離
 */
public abstract class AbsSettingView extends JScrollPane {

	private static final long serialVersionUID = 1L;

	public static String getViewName() { return "各種設定"; }

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();
	protected abstract ClipboardInfoList getCbItemEnv();
	protected abstract ListedColumnInfoList getLvItemEnv();
	protected abstract ReserveListColumnInfoList getRlItemEnv();
	protected abstract TitleListColumnInfoList getTlItemEnv();
	protected abstract ListedNodeInfoList getLnItemEnv();
	protected abstract ToolBarIconInfoList getTbIconEnv();
	protected abstract TabInfoList getTabItemEnv();

	protected abstract VWLookAndFeel getLAFEnv();
	protected abstract VWFont getFontEnv();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract Component getParentComponent();
	protected abstract VWColorChooserDialog getCcWin();

	protected abstract void lafChanged(String lafname);
	protected abstract void fontChanged(String fn, int fontSize);
	protected abstract void setEnv(boolean reload_prog);

	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	// オブジェクト
	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();
	private final ClipboardInfoList cbitems = getCbItemEnv();
	private final ListedColumnInfoList lvitems = getLvItemEnv();
	private final ReserveListColumnInfoList rlitems = getRlItemEnv();
	private final TitleListColumnInfoList tlitems = getTlItemEnv();
	private final ListedNodeInfoList lnitems = getLnItemEnv();
	private final ToolBarIconInfoList tbicons = getTbIconEnv();
	private final TabInfoList tabitems = getTabItemEnv();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト
	private final VWColorChooserDialog ccwin = getCcWin();	// これは起動時に作成されたまま変更されないオブジェクト

	// 雑多なメソッド
	private void StdAppendMessage(String message) { System.out.println(CommonUtils.getNow() + message); }
	//private void StdAppendError(String message) { System.err.println(CommonUtils.getNow() + message); }
	private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連

	private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	private static final int SEP_HEIGHT_NARROW = 4;
	private static final int BLOCK_SEP_HEIGHT = 40;

	private static final int LABEL_WIDTH = 350;
	private static final int CCLABEL_WIDTH = 250;
	private static final int DESCRIPTION_WIDTH = LABEL_WIDTH+PARTS_WIDTH;
	private static final int HEADER_WIDTH = DESCRIPTION_WIDTH; static final int UPDATE_WIDTH = 250;
	private static final int HINT_WIDTH = 700;

	private static final int PANEL_WIDTH = LABEL_WIDTH+PARTS_WIDTH+SEP_WIDTH*3;

	//

	private static final Color NOTICEMSG_COLOR = new Color(0,153,153);

	private Font font_header = null;

	// テキスト
	private static final String TEXT_HINT =
			"各項目の詳細はプロジェクトWikiに説明があります（http://sourceforge.jp/projects/tainavi/wiki/）。"+
			"ツールバーのヘルプアイコンをクリックするとブラウザで開きます。";

	private static final String PARER_REDRAW_NORMAL = "通常";
	private static final String PARER_REDRAW_CACHE = "再描画時に一週間分をまとめて描画して日付切り替えを高速化する（メモリ消費大）";
	private static final String PARER_REDRAW_PAGER = "ページャーを有効にして一度に描画する放送局数を抑える（メモリ消費抑制、切替時間短縮）";

	// ログ関連

	private static final String MSGID = "["+getViewName()+"] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JPanel jPanel_setting = null;

	private JPanel jPanel_update = null;
	private JButton jButton_update = null;

	// 全体
	private JLabel jLabel_toolbar = null;
	private JScrollPane jScrollPane_toolbar = null;
	private JNETable jTable_toolbar = null;
	private JButton jButton_toolbar_up = null;
	private JButton jButton_toolbar_down = null;
	private JButton jButton_toolbar_default = null;

	private JLabel jLabel_tabs = null;
	private JScrollPane jScrollPane_tabs = null;
	private JNETable jTable_tabs = null;

	private JSliderPanel jSP_keywordWidth = null;
	private JSliderPanel jSP_pagerComboWidth = null;
	private JSliderPanel jSP_recorderComboWidth = null;
	private JSliderPanel jSP_detailRows = null;
	private JSliderPanel jSP_statusRows = null;


	// リスト形式
	private JCheckBoxPanel jCBP_disableFazzySearch = null;
	private JCheckBoxPanel jCBP_disableFazzySearchReverse = null;
	private JSliderPanel jSP_defaultFazzyThreshold = null;
	private JCheckBoxPanel jCBP_syoboFilterByCenters = null;
	private JCheckBoxPanel jCBP_displayPassedEntry = null;
	private JCheckBoxPanel jCBP_showRsvPickup = null;
	private JCheckBoxPanel jCBP_showRsvDup = null;
	private JCheckBoxPanel jCBP_showRsvUra = null;
	private JCheckBoxPanel jCBP_rsvdLineEnhance = null;
	private JLabel jLabel_rsvdLineColor = null;
	private JCCLabel jCCL_rsvdLineColor = null;
	private JLabel jLabel_pickedLineColor = null;
	private JCCLabel jCCL_pickedLineColor = null;
	private JCCLabel jCCL_matchedKeywordColor = null;
	private JCheckBoxPanel jCBP_currentLineEnhance = null;
	private JLabel jLabel_currentLineColor = null;
	private JCCLabel jCCL_currentLineColor = null;
	private JSliderPanel jSP_currentAfter = null;
	private JSliderPanel jSP_currentBefore = null;
	private JCheckBoxPanel jCBP_showWarnDialog = null;
	private JCheckBoxPanel jCBP_splitMarkAndTitle = null;
	private JCheckBoxPanel jCBP_showDetailOnList = null;
	private JSliderPanel jSP_rsvTargets = null;
	private JCheckBoxPanel jCBP_rowHeaderVisible = null;
	private JComboBoxPanel jCBX_dblClkCmd = null;
	private JSliderPanel jSP_searchResultMax = null;
	private JSliderPanel jSP_searchResultBufferMax = null;

	private JLabel jLabel_listed = null;
	private JScrollPane jScrollPane_listed = null;
	private JNETable jTable_listed = null;
	private JButton jButton_listed_up = null;
	private JButton jButton_listed_down = null;
	private JButton jButton_listed_default = null;

	private JLabel jLabel_listed_node = null;
	private JScrollPane jScrollPane_listed_node = null;
	private JNETable jTable_listed_node = null;
	private JButton jButton_listed_node_up = null;
	private JButton jButton_listed_node_down = null;
	private JButton jButton_listed_node_default = null;

	// 新聞形式
	private JRadioButtonPanel jRBP_getPaperRedrawType = null;
	private JSliderPanel jSP_centerPerPage = null;
	private JCheckBoxPanel jCBP_allPageSnapshot = null;
	private JCheckBoxPanel jCBP_tooltipEnable = null;
	private JSliderPanel jSP_tooltipInitialDelay = null;
	private JSliderPanel jSP_tooltipDismissDelay = null;
	private JCheckBoxPanel jCBP_timerbarScrollEnable = null;
	private JSliderPanel jSP_passedLogLimit = null;
	private JSliderPanel jSP_datePerPage = null;
	private JCheckBoxPanel jCBP_effectComboToPaper = null;
	private JComboBoxPanel jCBX_snapshotFmt = null;
	private JCheckBoxPanel jCBP_printSnapshot = null;
	private JComboBoxPanel jCBP_mouseButtonForNodeSwitch = null;
	private JComboBoxPanel jCBP_mouseButtonForNodeAutoPaging = null;
	private JComboBoxPanel jCBP_mouseButtonForPageSwitch = null;

	// リスト・新聞共通
	private JCheckBoxPanel jCBP_displayOnlyExecOnEntry = null;
	private JCheckBoxPanel jCBP_displayPassedReserve = null;
	private JCheckBoxPanel jCBP_showOnlyNonrepeated = null;
	private JCheckBoxPanel jCBP_adjLateNight = null;
	private JCheckBoxPanel jCBP_rootNodeVisible = null;
	private JCheckBoxPanel jCBP_syncTreeWidth = null;
	private JCheckBoxPanel jCBP_shortExtMark = null;

	private JLabel jLabel_showmarks = null;
	private JScrollPane jScrollPane_showmarks = null;
	private JNETable jTable_showmarks = null;

	private JLabel jLabel_clipboard = null;
	private JScrollPane jScrollPane_clipboard = null;
	private JNETable jTable_clipboard = null;
	private JButton jButton_clipboard_up = null;
	private JButton jButton_clipboard_down = null;

	private JLabel jLabel_menuitem = null;
	private JTextFieldWithPopup jTextField_mikey = null;
	private JTextFieldWithPopup jTextField_mival = null;
	private JScrollPane jScrollPane_mitable = null;
	private JNETable jTable_mitable = null;
	private JButton jButton_miadd = null;
	private JButton jButton_midel = null;
	private JButton jButton_miup = null;
	private JButton jButton_midown = null;

	// 本体予約一覧
	private JLabel jLabel_reserv = null;
	private JScrollPane jScrollPane_reserv = null;
	private JNETable jTable_reserv = null;
	private JButton jButton_reserv_up = null;
	private JButton jButton_reserv_down = null;
	private JButton jButton_reserv_default = null;

	// Web番組表対応
	private JCheckBoxPanel jCBP_continueTomorrow = null;
	private JSliderPanel jSP_cacheTimeLimit = null;
	private JComboBoxPanel jCBX_shutdownCmd = null;
	private JCheckBoxPanel jCBP_expandTo8 = null;
	//private JCheckBoxPanel jCBP_useDetailCache = null;
	private JCheckBoxPanel jCBP_autoEventIdComplete = null;
	private JCheckBoxPanel jCBP_splitEpno = null;
	private JCheckBoxPanel jCBP_traceOnlyTitle = null;
	private JCheckBoxPanel jCBP_fixTitle = null;
	private JLabel jLabel_ngword = null;
	private JTextFieldWithPopup jTextField_ngword = null;
	private JLabel jLabel_userAgent = null;
	private JTextFieldWithPopup jTextField_userAgent = null;
	private JCheckBoxPanel jCBP_useProxy = null;
	private JLabel jLabel_proxy = null;
	private JTextFieldWithPopup jTextField_proxyAddr = null;
	private JTextFieldWithPopup jTextField_proxyPort = null;
	private JCheckBoxPanel jCBP_useSyobocal = null;
	private JCheckBoxPanel jCBP_historyOnlyUpdateOnce = null;
	private JCheckBoxPanel jCBP_usePassedProgram = null;
	private JSliderPanel jSP_prepPassedProgramCount = null;
	private JCheckBoxPanel jCBP_downloadProgramOnFixedTime = null;
	private JCheckBoxPanel jCBP_downloadProgramInBackground = null;
	private JTextFieldWithPopup jTextField_downloadProgramTimeList = null;
	private JCheckBoxPanel jCBP_useProgCache = null;

	// レコーダ対応
	private JRadioButtonPanel jRBP_getRdReserveDetails = null;
	private JRadioButtonPanel jRBP_getRdAutoReserves = null;
	private JRadioButtonPanel jRBP_getRdRecorded = null;
	private JComboBoxPanel jCBX_recordedSaveScope = null;

	// 予約
	private JSliderPanel jSP_spoex_extend = null;
	private JRadioButtonPanel jRBP_overlapUp = null;
	private JRadioButtonPanel jRBP_overlapDown = null;
	private JLabel jLabel_autoFolderSelect = null;
	private JCheckBox jCheckBox_autoFolderSelect = null;
	private JLabel jLabel_enableCHAVsetting = null;
	private JCheckBox jCheckBox_enableCHAVsetting = null;
	private JSliderPanel jSP_rangeLikeRsv = null;
	private JCheckBoxPanel jCBP_givePriorityToReserved = null;
	private JCheckBoxPanel jCBP_givePriorityToReservedTitle = null;
	private JCheckBoxPanel jCBP_adjoiningNotRepetition = null;
	private JCheckBoxPanel jCBP_rsv_showallite = null;
	private JLabel jLabel_rsv_itecolor = null;
	private JCCLabel jCCL_rsv_itecolor = null;
	private JLabel jLabel_rsv_tunshortcolor = null;
	private JCCLabel jCCL_rsv_tunshortcolor = null;
	private JLabel jLabel_rsv_recedcolor = null;
	private JCCLabel jCCL_rsv_recedcolor = null;
	private JCheckBoxPanel jCBP_useAutocomplete = null;
	private JCheckBoxPanel jCBP_notifyBeforeProgStart = null;
	private JCheckBoxPanel jCBP_notifyBeforePickProgStart = null;
	private JSliderPanel jSP_minsBeforeProgStart = null;

	// タイトル一覧
	private JLabel jLabel_title = null;
	private JScrollPane jScrollPane_title = null;
	private JNETable jTable_title = null;
	private JButton jButton_title_up = null;
	private JButton jButton_title_down = null;
	private JButton jButton_title_default = null;
	private JCheckBoxPanel jCBP_showTitleDetail = null;

	// その他
	private JComboBoxPanel jCBX_updateMethod = null;
	private JCheckBoxPanel jCBP_useHolidayCSV = null;
	private JSliderPanel jSP_holidayCSVFetchInterval = null;
	private JCheckBoxPanel jCBP_disableBeep = null;
	private JCheckBoxPanel jCBP_showSysTray = null;
	private JCheckBoxPanel jCBP_hideToTray = null;
	private JCheckBoxPanel jCBP_onlyOneInstance = null;
	private JLabel jLabel_lookAndFeel = null;
	private JComboBox jComboBox_lookAndFeel = null;
	private JLabel jLabel_font = null;
	private JComboBox jComboBox_font = null;
	private JComboBox jComboBox_fontSize = null;
	private JLabel jLabel_fontSample = null;
	private JCheckBoxPanel jCBP_useGTKRC = null;
	private JCheckBoxPanel jCBP_useRundll32 = null;
	private JCheckBoxPanel jCBP_debug = null;

	private JTextAreaWithPopup jta_help = null;

	// コンポーネント以外

	/**
	 * 特定のコンポーネントを操作した時だけ番組表を再取得してほしい
	 */
	private boolean reload_prog_needed = false;

	// タブが表示固定かどうかの配列
	boolean tab_selected [] = {true, true, false, false, false, false, true};

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsSettingView() {

		super();

		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.setColumnHeaderView(getJPanel_update());
		this.setViewportView(getJPanel_setting());

		setUpdateButtonEnhanced(false);
	}

	private JPanel getJPanel_update() {
		if (jPanel_update == null)
		{
			jPanel_update = new JPanel();
			jPanel_update.setLayout(new SpringLayout());

			jPanel_update.setBorder(new LineBorder(Color.GRAY));

			int y = SEP_HEIGHT;
			CommonSwingUtils.putComponentOn(jPanel_update, getJButton_update("更新を確定する"), UPDATE_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			int yz = SEP_HEIGHT/2;
			int x = UPDATE_WIDTH+50;
			CommonSwingUtils.putComponentOn(jPanel_update, getJta_help(), HINT_WIDTH, PARTS_HEIGHT+SEP_HEIGHT, x, yz);

			y += (PARTS_HEIGHT + SEP_HEIGHT);

			// 画面の全体サイズを決める
			Dimension d = new Dimension(PANEL_WIDTH,y);
			jPanel_update.setPreferredSize(d);
		}
		return jPanel_update;
	}

	/**
	 * ActionListenerはGUIの操作では動くがsetSelected()での変更では動かない<BR>
	 * ChangeListenerもItemListenerも同じ値のセットしなおしだと動作しない<BR>
	 * 以下ではFire!するために涙ぐましい努力を行っている<BR>
	 * <BR>
	 * ex.<BR>
	 * jcheckbox.setSelected( ! env.isSelected())<BR>
	 * jchackbox.addItemListener()<BR>
	 * jcheckbox.setSelected( ! jcheckbox.isSelected())<BR>
	 * <BR>
	 * …あほか！
	 */

	/*
	 * 全体
	 */
	private int createBlock0(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜全体＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		int tabs_h = PARTS_HEIGHT*8;
		int tabs_w = 320;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_tabs("表示対象タブの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_tabs(), tabs_w, tabs_h, LABEL_WIDTH+SEP_WIDTH, y);
		// ★★★ RELOADリスナーは getJScrollPane_tabs()内でつける
		y+=(tabs_h+SEP_HEIGHT);

		int tbicons_w = 320;
		int tbicons_h = PARTS_HEIGHT*8;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_toolbar("ツールバー表示対象項目の選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_toolbar_up("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+tbicons_w, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_toolbar_down("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+tbicons_w, y+PARTS_HEIGHT+SEP_WIDTH);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_toolbar(), tbicons_w, tbicons_h, LABEL_WIDTH+SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_toolbar_default("初期値"), 75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+tbicons_w, y+tbicons_h-PARTS_HEIGHT);
		y+=(tbicons_h+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_keywordWidth = new JSliderPanel("┗　検索ボックスの横幅",LABEL_WIDTH,0,600,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_keywordWidth.setValue(bounds.getSearchBoxAreaWidth());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_pagerComboWidth = new JSliderPanel("┗　ページャーコンボボックスの横幅",LABEL_WIDTH,0,600,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_pagerComboWidth.setValue(bounds.getPagerComboWidth());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_recorderComboWidth = new JSliderPanel("┗　レコーダ選択コンボボックスの横幅",LABEL_WIDTH,0,600,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_recorderComboWidth.setValue(bounds.getRecorderComboWidth());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_detailRows = new JSliderPanel("詳細情報欄の行数(0:前回終了時の高さ)",LABEL_WIDTH,0,10,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_detailRows.setValue(bounds.getDetailRows());
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_statusRows = new JSliderPanel("ステータス欄の行数(0:前回終了時の高さ)",LABEL_WIDTH,0,10,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_statusRows.setValue(bounds.getStatusRows());
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		return y;
	}
	/*
	 * リスト形式
	 */
	private int createBlock1(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜リスト形式＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_disableFazzySearch = new JCheckBoxPanel("番組追跡であいまい検索をしない",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_disableFazzySearch.setSelected( ! env.getDisableFazzySearch());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_disableFazzySearchReverse = new JCheckBoxPanel("┗　あいまい検索の逆引きを省略(非推奨)",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_disableFazzySearchReverse.setSelected(env.getDisableFazzySearchReverse());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jSP_defaultFazzyThreshold = new JSliderPanel("┗　あいまい検索のデフォルト閾値",LABEL_WIDTH,1,99,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jSP_defaultFazzyThreshold.setValue(env.getDefaultFazzyThreshold());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定（Fire!がキモイ…）
			jCBP_disableFazzySearch.addItemListener(al_fazzysearch);
			jCBP_disableFazzySearch.setSelected( ! jCBP_disableFazzySearch.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※あいまい検索のアルゴリズムにはバイグラムを使用しています(TraceProgram.java参照)"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT);
		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("　正順では「検索キーワード」の成分が「番組表のタイトル」にどれくらい含まれているかを判定しています。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT);
		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("　逆順を有効にすると、正順でNG判定になった場合に前者と後者を入れ替えて再判定を行います。取りこぼしが減る場合がある反面、検索ノイズが増える場合もあります。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT);
		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("　閾値を大きくすると判定が厳しくなります。キーワードが短いためにヒットしまくりで検索ノイズが多くなった場合に、値を大きくしてみてください。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_traceOnlyTitle = new JCheckBoxPanel("タイトル中に含まれるサブタイトルは番組追跡の対象にしない",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_traceOnlyTitle.setSelected(env.getTraceOnlyTitle());
		jCBP_traceOnlyTitle.addItemListener(IL_RELOAD_PROG_NEEDED);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_syoboFilterByCenters = new JCheckBoxPanel("しょぼかるの検索結果も有効な放送局のみに絞る",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_syoboFilterByCenters.setSelected(env.getSyoboFilterByCenters());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_displayPassedEntry = new JCheckBoxPanel("当日の終了済み番組も表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_displayPassedEntry.setSelected(env.getDisplayPassedEntry());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showRsvPickup = new JCheckBoxPanel("ピックアップマーク(★)を表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showRsvPickup.setSelected(env.getShowRsvPickup());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showRsvDup = new JCheckBoxPanel("時間重複マーク(■)を表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showRsvDup.setSelected(env.getShowRsvDup());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showRsvUra = new JCheckBoxPanel("裏番組予約マーク(裏)を表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showRsvUra.setSelected(env.getShowRsvUra());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_rsvdLineEnhance = new JCheckBoxPanel("予約行の背景色を変えて強調する",LABEL_WIDTH), LABEL_WIDTH+PARTS_HEIGHT, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_rsvdLineEnhance.setSelected( ! env.getRsvdLineEnhance());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jLabel_rsvdLineColor = new JLabel("┗　予約行の背景色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_rsvdLineColor = new JCCLabel("予約行の背景色",env.getRsvdLineColor(),true,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jLabel_pickedLineColor = new JLabel("┗　ピックアップ行の背景色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_pickedLineColor = new JCCLabel("ピックアップ行の背景色",env.getPickedLineColor(),true,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定

			jCBP_rsvdLineEnhance.addItemListener(al_rsvdlineenhance);
			jCBP_rsvdLineEnhance.setSelected( ! jCBP_rsvdLineEnhance.isSelected());
		}

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_currentLineEnhance = new JCheckBoxPanel("現在放送中の行の背景色を変えて強調する",LABEL_WIDTH), LABEL_WIDTH+PARTS_HEIGHT, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_currentLineEnhance.setSelected( ! env.getCurrentLineEnhance());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jLabel_currentLineColor = new JLabel("┗　現在放送中行の背景色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_currentLineColor = new JCCLabel("現在放送中行の背景色",env.getCurrentLineColor(),true,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定

			jCBP_currentLineEnhance.addItemListener(al_currentlineenhance);

			jCBP_currentLineEnhance.setSelected( ! jCBP_currentLineEnhance.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_currentAfter = new JSliderPanel("現在放送中ノードに終了後何分までの番組を表示するか",LABEL_WIDTH,0,60,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_currentAfter.setValue(env.getCurrentAfter()/60);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_currentBefore = new JSliderPanel("現在放送中ノードに開始前何分までの番組を表示するか",LABEL_WIDTH,0,120,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_currentBefore.setValue(env.getCurrentBefore()/60);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		//
		CommonSwingUtils.putComponentOn(jPanel_setting, new JLabel("タイトル中のキーワードにマッチした箇所の強調色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_matchedKeywordColor = new JCCLabel("強調色",env.getMatchedKeywordColor(),false,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showWarnDialog = new JCheckBoxPanel("キーワード削除時に確認ダイアログを表示",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showWarnDialog.setSelected(env.getShowWarnDialog());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_splitMarkAndTitle = new JCheckBoxPanel("オプション表示を個別欄に分離表示",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_splitMarkAndTitle.setSelected(env.getSplitMarkAndTitle());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showDetailOnList = new JCheckBoxPanel("番組詳細列を表示",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showDetailOnList.setSelected(env.getShowDetailOnList());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_rowHeaderVisible = new JCheckBoxPanel("行番号を表示",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_rowHeaderVisible.setSelected(env.getRowHeaderVisible());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_rsvTargets = new JSliderPanel("予約待機の予約番組自動選択数",LABEL_WIDTH,1,99,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_rsvTargets.setValue(env.getRsvTargets());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBX_dblClkCmd = new JComboBoxPanel("ダブルクリック時の動作",LABEL_WIDTH,250,true), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		for ( DblClkCmd c : DblClkCmd.values() ) {
			jCBX_dblClkCmd.addItem(c);
		}
		jCBX_dblClkCmd.setSelectedItem(env.getDblClkCmd());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_searchResultMax = new JSliderPanel("過去ログ検索件数の上限",LABEL_WIDTH,10,500,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_searchResultMax.setValue(env.getSearchResultMax());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_searchResultBufferMax = new JSliderPanel("過去ログ検索履歴の上限",LABEL_WIDTH,1,10,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_searchResultBufferMax.setValue(env.getSearchResultBufferMax());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		int lnitems_w = 320;
		int lnitems_h = PARTS_HEIGHT*8;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_listed_node("ツリービュー表示対象ノードの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_listed_node_up("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+lnitems_w, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_listed_node_down("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+lnitems_w, y+PARTS_HEIGHT+SEP_WIDTH);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_listed_node(), lnitems_w, lnitems_h, LABEL_WIDTH+SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_listed_node_default("初期値"), 75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+lnitems_w, y+lnitems_h-PARTS_HEIGHT);
		y+=lnitems_h+SEP_HEIGHT;

		int lvitems_w = 320;
		int lvitems_h = PARTS_HEIGHT*8;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_listed("リスト表示対象アイテムの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_listed_up("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+lvitems_w, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_listed_down("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+lvitems_w, y+PARTS_HEIGHT+SEP_WIDTH);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_listed(), lvitems_w, lvitems_h, LABEL_WIDTH+SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_listed_default("初期値"), 75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+lvitems_w, y+lvitems_h-PARTS_HEIGHT);
		y+=lvitems_h+SEP_HEIGHT;

		return y;
	}

	/*
	 * 新聞形式
	 */
	private int createBlock2(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜新聞形式＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			int paperredraw_h = PARTS_HEIGHT*3+SEP_HEIGHT_NARROW*2;
			CommonSwingUtils.putComponentOn(jPanel_setting, jRBP_getPaperRedrawType = new JRadioButtonPanel("描画方式",LABEL_WIDTH), PARTS_WIDTH, paperredraw_h, SEP_WIDTH, y);
			jRBP_getPaperRedrawType.add(PARER_REDRAW_NORMAL, false);
			jRBP_getPaperRedrawType.add(PARER_REDRAW_CACHE, false);
			jRBP_getPaperRedrawType.add(PARER_REDRAW_PAGER, false);
			// RELOADリスナー不要
			y+=(paperredraw_h+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jSP_centerPerPage = new JSliderPanel("┗　ページあたりの放送局数",LABEL_WIDTH,1,99,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jSP_centerPerPage.setValue(env.getCenterPerPage());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_allPageSnapshot = new JCheckBoxPanel("┗　スナップショットを全ページ連続で実行",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_allPageSnapshot.setSelected(env.getAllPageSnapshot());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定(1)
			jRBP_getPaperRedrawType.addItemListener(il_paperredrawtype);

			if ( env.isPagerEnabled() ) {
				jRBP_getPaperRedrawType.setSelectedItem(PARER_REDRAW_PAGER);
			}
			else if ( env.getDrawcacheEnable() ) {
				jRBP_getPaperRedrawType.setSelectedItem(PARER_REDRAW_CACHE);
			}
			else {
				jRBP_getPaperRedrawType.setSelectedItem(PARER_REDRAW_NORMAL);
			}
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※切り替えがおっせーよ！忍耐の限界だよ！という場合は新聞形式の設定変更（ツールバーのパレットアイコン）で「番組詳細のフォント設定＞表示する」のチェックを外してください。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_tooltipEnable = new JCheckBoxPanel("番組表でツールチップを表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_tooltipEnable.setSelected( ! env.getTooltipEnable());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jSP_tooltipInitialDelay = new JSliderPanel("┗　表示までの遅延(ミリ秒)",LABEL_WIDTH,0,3000,100,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jSP_tooltipInitialDelay.setValue(env.getTooltipInitialDelay());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jSP_tooltipDismissDelay = new JSliderPanel("┗　消去までの遅延(ミリ秒)",LABEL_WIDTH,1000,60000,100,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jSP_tooltipDismissDelay.setValue(env.getTooltipDismissDelay());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jCBP_tooltipEnable.addItemListener(al_tooltipenable);
			jCBP_tooltipEnable.setSelected( ! jCBP_tooltipEnable.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_timerbarScrollEnable = new JCheckBoxPanel("現在時刻線を固定し番組表側をスクロール",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_timerbarScrollEnable.setSelected(env.getTimerbarScrollEnable());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_passedLogLimit = new JSliderPanel("過去ログの日付ノードの表示数",LABEL_WIDTH,7,365*5,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_passedLogLimit.setValue(env.getPassedLogLimit());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_datePerPage = new JSliderPanel("過去ログのページ当たりの日数",LABEL_WIDTH,7,28,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_datePerPage.setValue(env.getDatePerPassedPage());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_effectComboToPaper = new JCheckBoxPanel("レコーダコンボボックスを新聞形式でも有効に",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_effectComboToPaper.setSelected(env.getEffectComboToPaper());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);


		CommonSwingUtils.putComponentOn(jPanel_setting, new JLabel("マウスホイールによる日付/放送局/ページの切替"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_mouseButtonForNodeAutoPaging = createMouseButtonComboBoxPanel("┗　上下端での日付切替(Shiftキーと同等)", LABEL_WIDTH, 100), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_mouseButtonForNodeAutoPaging.setSelectedIndex(env.getMouseButtonForNodeAutoPaging());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_mouseButtonForNodeSwitch = createMouseButtonComboBoxPanel("┗　日付/放送局切替(Ctrlキーと同等)", LABEL_WIDTH, 100), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_mouseButtonForNodeSwitch.setSelectedIndex(env.getMouseButtonForNodeSwitch());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_mouseButtonForPageSwitch = createMouseButtonComboBoxPanel("┗　ページ切替", LABEL_WIDTH, 100), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_mouseButtonForPageSwitch.setSelectedIndex(env.getMouseButtonForPageSwitch());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		return y;
	}

	private JComboBoxPanel createMouseButtonComboBoxPanel(String s, int lw, int pw){
		JComboBoxPanel panel = new JComboBoxPanel(s, lw, pw, true);
		panel.addItem("使用しない");
		for (int n=0; n<6; n++){
			String label = "マウスボタン" + (n+1) + "を押しながら回す";
			panel.addItem(label);
		}

		return panel;
	}

	/*
	 * リスト・新聞形式共通
	 */
	private int createBlock3(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜リスト・新聞形式共通＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_displayOnlyExecOnEntry = new JCheckBoxPanel("実行ONの予約のみ予約マークを表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_displayOnlyExecOnEntry.setSelected(env.getDisplayOnlyExecOnEntry());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_displayPassedReserve = new JCheckBoxPanel("当日の終了済み予約も表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_displayPassedReserve.setSelected(env.getDisplayPassedReserve());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showOnlyNonrepeated = new JCheckBoxPanel("リピート放送と判定されたら番組追跡に表示しない",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showOnlyNonrepeated.setSelected(env.getShowOnlyNonrepeated());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_adjLateNight = new JCheckBoxPanel("【RD】深夜の帯予約を前にずらす(火～土→月～金)",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_adjLateNight.setSelected(env.getAdjLateNight());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※月～金26:00の帯番組は、実際には火～土AM2:00に放送されますので鯛ナビでもそのように帯予約を処理しています。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT);
		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("　しかし、RDのように月～金AM2:00で録画が実行されてしまうような場合にはこれをチェックしてください。帯予約の予約枠を月～金AM2:00で表示するようにします。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_rootNodeVisible = new JCheckBoxPanel("ツリーペーンにrootノードを表示させる",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_rootNodeVisible.setSelected(env.getRootNodeVisible());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_syncTreeWidth = new JCheckBoxPanel("リスト形式と新聞形式のツリーペーンの幅を同期する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_syncTreeWidth.setSelected(env.getSyncTreeWidth());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_shortExtMark = new JCheckBoxPanel("「★延長注意★」を「(延)」に短縮表示",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_shortExtMark.setSelected(env.getShortExtMark());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBX_snapshotFmt = new JComboBoxPanel("スナップショットの画像形式",LABEL_WIDTH,250,true), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		for ( SnapshotFmt s : SnapshotFmt.values() ) {
			jCBX_snapshotFmt.addItem(s);
		}
		jCBX_snapshotFmt.setSelectedItem(env.getSnapshotFmt());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_printSnapshot = new JCheckBoxPanel("スナップショットボタンで印刷を実行する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_printSnapshot.setSelected(env.getPrintSnapshot());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		int marks_h = PARTS_HEIGHT*12;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_showmarks("表示マークの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_showmarks(), 320, marks_h, LABEL_WIDTH+SEP_WIDTH, y);
		// ★★★ RELOADリスナーは getJScrollPane_showmarks()内でつける
		y+=(marks_h+SEP_HEIGHT);

		int cbitems_w = 320;
		int cbitems_h = PARTS_HEIGHT*8;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_clipboard("クリップボードアイテムの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_clipboard_up("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+cbitems_w, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_clipboard_down("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+cbitems_w, y+PARTS_HEIGHT+SEP_WIDTH);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_clipboard(), cbitems_w, cbitems_h, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y += (cbitems_h + SEP_HEIGHT);

		int mitable_h = PARTS_HEIGHT*8;
		{
			int col1_w = 150;
			int col2_w = 400;
			int mitable_w = col1_w+col2_w;
			CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_menuitem("右クリックメニューの実行アイテム"), LABEL_WIDTH,PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_mikey(),col1_w, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_mival(),col2_w, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+col1_w, y);
			int yz = y;
			CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_miadd("登録"),75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+mitable_w+SEP_WIDTH, yz);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_midel("削除"),75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+mitable_w+SEP_WIDTH, yz += PARTS_HEIGHT+SEP_HEIGHT_NARROW);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_miup("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+mitable_w+SEP_WIDTH, yz += PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_midown("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+mitable_w+SEP_WIDTH, yz += PARTS_HEIGHT+SEP_HEIGHT_NARROW);
			y += (PARTS_HEIGHT + SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_setting,getJScrollPane_mitable(col1_w,col2_w), mitable_w, mitable_h, LABEL_WIDTH+SEP_WIDTH, y);
			// RELOADリスナー不要
			y+=(mitable_h+SEP_HEIGHT);
		}

		return y;
	}

	/*
	 * 本体予約一覧対応
	 */
	private int createBlock4(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜本体予約一覧対応＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		int rlitems_w = 320;
		int rlitems_h = PARTS_HEIGHT*8;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_reserv("表示対象アイテムの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_reserv_up("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+rlitems_w, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_reserv_down("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+rlitems_w, y+PARTS_HEIGHT+SEP_WIDTH);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_reserv(), rlitems_w, rlitems_h, LABEL_WIDTH+SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_reserv_default("初期値"), 75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+rlitems_w, y+rlitems_h-PARTS_HEIGHT);
		y+=(rlitems_h+SEP_HEIGHT);

		return y;
	}

	/*
	 * Web番組表対応
	 */
	private int createBlock5(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜Web番組表対応＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_continueTomorrow = new JCheckBoxPanel("29時をまたぐ番組を検出し同一視する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_continueTomorrow.setSelected(env.getContinueTomorrow());
		jCBP_continueTomorrow.addItemListener(IL_RELOAD_PROG_NEEDED);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jSP_cacheTimeLimit = new JSliderPanel("番組表キャッシュの有効時間(0:無制限)",LABEL_WIDTH,0,72,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jSP_cacheTimeLimit.setValue((env.getCacheTimeLimit()+1)%73);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jCBX_shutdownCmd = new JComboBoxPanel("┗　シャットダウンコマンド",LABEL_WIDTH,250,true), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBX_shutdownCmd.setEditable(true);
			jCBX_shutdownCmd.addItem(env.getShutdownCmd());
			for ( String cmd : Env.SHUTDOWN_COMMANDS ) {
				jCBX_shutdownCmd.addItem(cmd);
			}
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jSP_cacheTimeLimit.addChangeListener(cl_cachetimelimit);
			jSP_cacheTimeLimit.setValue(env.getCacheTimeLimit());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※起動時に、Web番組表の再取得を自動で「実行させたくない」場合は０にしてください。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT);
		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※シャットダウンコマンドを設定すると、Web番組表取得メニューに「CSのみ取得(取得後シャットダウン)」が追加されます。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_expandTo8 = new JCheckBoxPanel("可能なら番組表を８日分取得する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_expandTo8.setSelected(env.getExpandTo8());
		jCBP_expandTo8.addItemListener(IL_RELOAD_PROG_NEEDED);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_autoEventIdComplete = new JCheckBoxPanel("予約ダイアログを開いたときに自動で番組IDを取得する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_autoEventIdComplete.setSelected(env.getAutoEventIdComplete());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_splitEpno = new JCheckBoxPanel("タイトルに話数が含まれる場合に以降を分離する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_splitEpno.setSelected(env.getSplitEpno());
		jCBP_splitEpno.addItemListener(IL_RELOAD_PROG_NEEDED);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_fixTitle = new JCheckBoxPanel("タイトル先頭の「アニメ 」を削除(NHKのみ)",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_fixTitle.setSelected(env.getFixTitle());
		jCBP_fixTitle.addItemListener(IL_RELOAD_PROG_NEEDED);
		y +=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_ngword("NGワード(;区切りで複数指定可)"), LABEL_WIDTH,PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_ngword(CommonUtils.joinStr(";", env.getNgword())),600, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		jTextField_ngword.getDocument().addDocumentListener(DL_RELOAD_PROG_NEEDED);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_userAgent("User-Agent"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_userAgent(env.getUserAgent()), 600, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useProxy = new JCheckBoxPanel("HTTPプロキシを有効にする",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_useProxy.setSelected( ! env.getUseProxy());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_proxy("┗　HTTPプロキシ/ポート"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_proxyAddr(env.getProxyAddr()), 200, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_proxyPort(env.getProxyPort()), 100, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+210, y);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jCBP_useProxy.addItemListener(al_useproxy);
			jCBP_useProxy.setSelected( ! jCBP_useProxy.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useSyobocal = new JCheckBoxPanel("【アニメ】しょぼいカレンダーを利用する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_useSyobocal.setSelected(env.getUseSyobocal());
		jCBP_useSyobocal.addItemListener(IL_RELOAD_PROG_NEEDED);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※アニメなんか興味ないよ！という場合はチェックを外して再起動してください。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_historyOnlyUpdateOnce = new JCheckBoxPanel("日に一回しか新着履歴を更新しない",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_historyOnlyUpdateOnce.setSelected(env.getHistoryOnlyUpdateOnce());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_usePassedProgram = new JCheckBoxPanel("過去ログを記録する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_usePassedProgram.setSelected( ! env.getUsePassedProgram());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jSP_prepPassedProgramCount = new JSliderPanel("┗　何日先のログまで過去ログ用に保存するか",LABEL_WIDTH,1,8,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jSP_prepPassedProgramCount.setValue(env.getPrepPassedProgramCount());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jCBP_usePassedProgram.addItemListener(al_usepassedprogram);
			jCBP_usePassedProgram.setSelected( ! jCBP_usePassedProgram.isSelected());

			CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※保存期間を４日先までにして１週間旅行に出かけると７日－４日＝３日分の過去ログがロストすることになります。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
			y+=(PARTS_HEIGHT+SEP_HEIGHT);
		}

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_downloadProgramOnFixedTime = new JCheckBoxPanel("決まった時刻に番組表を取得する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_downloadProgramOnFixedTime.setSelected( ! env.getDownloadProgramOnFixedTime());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, new JLabel("┗　取得時刻(HH:MM形式 ;区切りで複数指定可)"), LABEL_WIDTH,PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, getJTextField_downloadProgramTimeList(CommonUtils.joinStr(";", env.getDownloadProgramTimeList())),600, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_downloadProgramInBackground = new JCheckBoxPanel("バックグランドで取得する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_downloadProgramInBackground.setSelected(env.getDownloadProgramInBackground());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jCBP_downloadProgramOnFixedTime.addItemListener(al_downloadProgramOnFixedTime);
			jCBP_downloadProgramOnFixedTime.setSelected( ! jCBP_downloadProgramOnFixedTime.isSelected());
			// RELOADリスナー不要
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useProgCache = new JCheckBoxPanel("高速キャッシュファイルを使用する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_useProgCache.setSelected(env.getUseProgCache());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		return y;
	}

	private int createBlock6(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜レコーダ対応＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);


		int getdetail_h = PARTS_HEIGHT*3+SEP_HEIGHT_NARROW*2;
		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jRBP_getRdReserveDetails = new JRadioButtonPanel("予約一覧取得時に詳細情報も取得する",LABEL_WIDTH), PARTS_WIDTH, getdetail_h, SEP_WIDTH, y);
			jRBP_getRdReserveDetails.add("毎回確認する",true);
			jRBP_getRdReserveDetails.add("常に取得する",false);
			jRBP_getRdReserveDetails.add("常に取得しない",false);
			// RELOADリスナー不要
			y+=(getdetail_h+SEP_HEIGHT);

			CommonSwingUtils.putComponentOn(jPanel_setting, jRBP_getRdAutoReserves = new JRadioButtonPanel("予約一覧取得時に自動予約一覧も取得する",LABEL_WIDTH), PARTS_WIDTH, getdetail_h, SEP_WIDTH, y);
			jRBP_getRdAutoReserves.add("毎回確認する",true);
			jRBP_getRdAutoReserves.add("常に取得する",false);
			jRBP_getRdAutoReserves.add("常に取得しない",false);
			// RELOADリスナー不要
			y+=(getdetail_h+SEP_HEIGHT);

			CommonSwingUtils.putComponentOn(jPanel_setting, jRBP_getRdRecorded = new JRadioButtonPanel("予約一覧取得時に録画結果一覧も取得する",LABEL_WIDTH), PARTS_WIDTH, getdetail_h, SEP_WIDTH, y);
			jRBP_getRdRecorded.add("毎回確認する",true);
			jRBP_getRdRecorded.add("常に取得する",false);
			jRBP_getRdRecorded.add("常に取得しない",false);
			// RELOADリスナー不要
			y+=(getdetail_h+SEP_HEIGHT);

			// 選択肢
			updateSelections();

			CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※「常に取得しない」を選択した場合でも、ツールバーのプルダウンメニューから強制的に取得を実行できます。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
			y+=(PARTS_HEIGHT+SEP_HEIGHT);
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBX_recordedSaveScope = new JComboBoxPanel("録画結果一覧の保存期間",LABEL_WIDTH,250,true), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBX_recordedSaveScope.addItem("保存しない");
		for ( int n=1; n<=HDDRecorder.SCOPEMAX; n++ ) {
			jCBX_recordedSaveScope.addItem(String.format("%d日 (%d週)",n,(n/7)+1));
		}
		jCBX_recordedSaveScope.setSelectedIndex(env.getRecordedSaveScope());
		// RELOADリスナー不要
		y+=(getdetail_h+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, new JLabel("NULLプラグインでのカレンダ連携設定はレコーダ設定に移動しました"), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(75);

		return y;
	}

	/*
	 * 予約
	 */
	private int createBlock7(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜予約＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_spoex_extend = new JSliderPanel("延長警告の録画時間延長幅(分)",LABEL_WIDTH,0,180,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_spoex_extend.setValue(Integer.valueOf(env.getSpoexLength()));
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		int ovarlap_h = PARTS_HEIGHT*2+SEP_HEIGHT_NARROW*1;
		CommonSwingUtils.putComponentOn(jPanel_setting, jRBP_overlapUp = new JRadioButtonPanel("録画時間の前",LABEL_WIDTH), PARTS_WIDTH, ovarlap_h, SEP_WIDTH, y);
		jRBP_overlapUp.add("なにもしない",! (env.getOverlapUp()));
		jRBP_overlapUp.add("１分前倒し",(env.getOverlapUp()));
		// RELOADリスナー不要
		y+=(ovarlap_h+SEP_HEIGHT);

		int ovarlap2_h = PARTS_HEIGHT*3+SEP_HEIGHT_NARROW*2;
		CommonSwingUtils.putComponentOn(jPanel_setting, jRBP_overlapDown = new JRadioButtonPanel("録画時間の後ろ",LABEL_WIDTH), PARTS_WIDTH, ovarlap2_h, SEP_WIDTH, y);
		jRBP_overlapDown.add("なにもしない",! (env.getOverlapDown() || env.getOverlapDown2()));
		jRBP_overlapDown.add("１分延ばす",(env.getOverlapDown()));
		jRBP_overlapDown.add("１分短縮(NHK以外)",(env.getOverlapDown2()));
		// RELOADリスナー不要
		y+=(ovarlap2_h+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_autoFolderSelect("自動フォルダ選択を有効にする"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJCheckBox_autoFolderSelect(env.getAutoFolderSelect()), 100, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_enableCHAVsetting("AV自動設定キーをジャンルからCHに"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJCheckBox_enableCHAVsetting(env.getEnableCHAVsetting()), 100, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_rangeLikeRsv = new JSliderPanel("類似予約の検索時間範囲(0:無制限)",LABEL_WIDTH,0,24,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_rangeLikeRsv.setValue(env.getRangeLikeRsv());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_givePriorityToReserved = new JCheckBoxPanel("類似予約がある場合は情報を引き継ぐ",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_givePriorityToReserved.setSelected( ! env.getGivePriorityToReserved());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_givePriorityToReservedTitle = new JCheckBoxPanel("┗　類似予約のタイトルを引き継ぐ",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_givePriorityToReservedTitle.setSelected(env.getGivePriorityToReservedTitle());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jCBP_givePriorityToReserved.addItemListener(al_giveprioritytoreserved);
			jCBP_givePriorityToReserved.setSelected( ! jCBP_givePriorityToReserved.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_adjoiningNotRepetition = new JCheckBoxPanel("終了時刻と開始時刻が重なる番組でも重複扱いしない",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_adjoiningNotRepetition.setSelected(env.getAdjoiningNotRepetition());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_rsv_showallite = new JCheckBoxPanel("予約一覧で繰り返し予約を展開する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_rsv_showallite.setSelected( ! env.getShowAllIterationItem());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jLabel_rsv_itecolor = new JLabel("┗　展開した繰り返し予約の２回目以降の文字色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_rsv_itecolor = new JCCLabel("文字色",env.getIterationItemForeground(),false,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			jCBP_rsv_showallite.addItemListener(al_showallite);
			jCBP_rsv_showallite.setSelected( ! jCBP_rsv_showallite.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jLabel_rsv_tunshortcolor = new JLabel("チューナー不足警告の背景色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_rsv_tunshortcolor = new JCCLabel("チューナー不足警告の背景色",env.getTunerShortColor(),true,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※チューナー不足警告は、レコーダの予約一覧上に表示される警告情報を反映しています。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT);
		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※EDCBの場合、チューナー不足警告は鯛ナビからの予約アクションでは更新されませんので、必要に応じて予約一覧の再取得を行って更新してください。"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jLabel_rsv_recedcolor = new JLabel("正常録画済み(と思われる)予約の背景色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, jCCL_rsv_recedcolor = new JCCLabel("正常録画済み(と思われる)予約の背景色",env.getRecordedColor(),true,parent,ccwin), CCLABEL_WIDTH, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useAutocomplete = new JCheckBoxPanel("【RD】タイトル自動補完機能を使用する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_useAutocomplete.setSelected(env.getUseAutocomplete());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_notifyBeforeProgStart = new JCheckBoxPanel("予約した番組が開始する前に通知する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_notifyBeforeProgStart.setSelected(env.getNotifyBeforeProgStart());
		jCBP_notifyBeforeProgStart.addItemListener(il_notifyBeforeProgStart);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_notifyBeforePickProgStart = new JCheckBoxPanel("ピックアップした番組が開始する前に通知する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_notifyBeforePickProgStart.setSelected(env.getNotifyBeforePickProgStart());
		jCBP_notifyBeforePickProgStart.addItemListener(il_notifyBeforeProgStart);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_minsBeforeProgStart = new JSliderPanel("┗　番組開始の何分前に通知するか",LABEL_WIDTH,0,60,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_minsBeforeProgStart.setValue(env.getMinsBeforeProgStart());
		jSP_minsBeforeProgStart.setEnabled(jCBP_notifyBeforeProgStart.isSelected() || jCBP_notifyBeforePickProgStart.isSelected());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		return y;
	}

	/*
	 * タイトル一覧対応
	 */
	private int createBlock8(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜タイトル一覧＞＞＞"),HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		int tlitems_w = 320;
		int tlitems_h = PARTS_HEIGHT*8;
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_title("表示対象アイテムの選択"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_title_up("↑"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+tlitems_w, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_title_down("↓"), 50, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+tlitems_w, y+PARTS_HEIGHT+SEP_WIDTH);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJScrollPane_title(), tlitems_w, tlitems_h, LABEL_WIDTH+SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJButton_title_default("初期値"), 75, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+10+tlitems_w, y+tlitems_h-PARTS_HEIGHT);
		y+=(tlitems_h+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showTitleDetail = new JCheckBoxPanel("タイトル詳細を表示する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_showTitleDetail.setSelected(env.getShowTitleDetail());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		return y;
	}

	/*
	 * その他
	 */
	private int createBlock9(int y){
		CommonSwingUtils.putComponentOn(jPanel_setting, getHeaderLabel("＜＜＜その他＞＞＞"), HEADER_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useHolidayCSV = new JCheckBoxPanel("内閣府の祝日CSVを使用する（要再起動）",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_useHolidayCSV.setSelected(env.getUseHolidayCSV());
		jCBP_useHolidayCSV.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				jSP_holidayCSVFetchInterval.setEnabled(jCBP_useHolidayCSV.isSelected());
			}
		});
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, jSP_holidayCSVFetchInterval = new JSliderPanel("┗　取得間隔(日)",LABEL_WIDTH,0,360,200), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jSP_holidayCSVFetchInterval.setValue(env.getHolidayFetchInterval());
		jSP_holidayCSVFetchInterval.setEnabled(jCBP_useHolidayCSV.isSelected());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBX_updateMethod = new JComboBoxPanel("起動時にアップデートを確認する",LABEL_WIDTH,250,true), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		for ( UpdateOn u : UpdateOn.values() ) {
			jCBX_updateMethod.addItem(u);
		}
		jCBX_updateMethod.setSelectedItem(env.getUpdateMethod());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_disableBeep = new JCheckBoxPanel("beep禁止",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_disableBeep.setSelected(env.getDisableBeep());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		{
			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_showSysTray = new JCheckBoxPanel("システムトレイにアイコンを表示",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_showSysTray.setSelected( ! env.getShowSysTray());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

			CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_hideToTray = new JCheckBoxPanel("┗　最小化時はシステムトレイに隠す",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			jCBP_hideToTray.setSelected(env.getHideToTray());
			// RELOADリスナー不要
			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 連動設定
			jCBP_showSysTray.addItemListener(al_showsystray);
			jCBP_showSysTray.setSelected( ! jCBP_showSysTray.isSelected());
		}

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_onlyOneInstance = new JCheckBoxPanel("多重起動禁止（要再起動）",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_onlyOneInstance.setSelected(env.getOnlyOneInstance());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_lookAndFeel("ルック＆フィール"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJComboBox_lookAndFeel(), 250, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_font("表示フォント"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJComboBox_font(), 250, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJComboBox_fontSize(), 100, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH+260, y);
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT_NARROW);

		CommonSwingUtils.putComponentOn(jPanel_setting, new JLabel("┗　表示サンプル"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(jPanel_setting, getJLabel_fontSample(""), 360, PARTS_HEIGHT, LABEL_WIDTH+SEP_WIDTH, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useGTKRC = new JCheckBoxPanel("鯛ナビ専用のgtkrcを使う",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_useGTKRC.setSelected(env.getUseGTKRC());
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, getNoticeMsg("※ルック＆フィールがGTKの場合は再起動するまで表示フォントの設定は反映されません（@see env/_gtkrc-2.0）"), DESCRIPTION_WIDTH, PARTS_HEIGHT, SEP_WIDTH*2, y);
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_useRundll32 = new JCheckBoxPanel("【Win】ファイルオープンにrundll32を使用する",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_useRundll32.setSelected(env.getUseRundll32());
		// RELOADリスナー不要
		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		CommonSwingUtils.putComponentOn(jPanel_setting, jCBP_debug = new JCheckBoxPanel("【注意】デバッグログ出力を有効にする",LABEL_WIDTH), PARTS_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		jCBP_debug.setSelected(env.getDebug());
		// RELOADリスナー不要
		y += (PARTS_HEIGHT + 50);

		return y;
	}

	private JPanel getJPanel_setting() {
		if (jPanel_setting == null)
		{
			jPanel_setting = new JPanel();
			jPanel_setting.setLayout(new SpringLayout());

			int y = SEP_HEIGHT;

			// 全体
			y = createBlock0(y);

			// リスト形式
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock1(y);

			// 新聞形式
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock2(y);

			// リスト・新聞形式共通
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock3(y);

			// 本体予約一覧対応
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock4(y);

			// Web番組表対応
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock5(y);

			// レコーダ対応
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock6(y);

			// 予約
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock7(y);

			// タイトル一覧対応
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock8(y);

			// その他
			y+=(BLOCK_SEP_HEIGHT);
			y = createBlock9(y);

			// 画面の全体サイズを決める
			Dimension d = new Dimension(PANEL_WIDTH,y);
			jPanel_setting.setPreferredSize(d);
		}

		return jPanel_setting;
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 更新確定ボタン押下時の処理
	private void updateEnvs() {

		TatCount tc = new TatCount();

		int idx;

		try{
			// 全体
			// 表示対象タブ
			tabitems.clear();
			for (int row=0; row<jTable_tabs.getRowCount(); row++) {
	    		ListColumnInfo cb = new ListColumnInfo();
	    		if (tab_selected[row])
		        	cb.setVisible(true);
	    		else
	    			cb.setVisible((Boolean)jTable_tabs.getValueAt(row, 0));
	        	cb.setName((String)jTable_tabs.getValueAt(row, 1));
	        	cb.setId((Integer)jTable_tabs.getValueAt(row, 2));
	        	tabitems.add(cb);
			}

			// ツールバーの表示順
			tbicons.clear();
			for (int row=0; row<jTable_toolbar.getRowCount(); row++) {
	    		ListColumnInfo cb = new ListColumnInfo();
	        	cb.setVisible((Boolean)jTable_toolbar.getValueAt(row, 0));
	        	cb.setName((String)jTable_toolbar.getValueAt(row, 1));
	        	cb.setId((Integer)jTable_toolbar.getValueAt(row, 2));
	        	tbicons.add(cb);
			}
			bounds.setSearchBoxAreaWidth(jSP_keywordWidth.getValue());
			bounds.setPagerComboWidth(jSP_pagerComboWidth.getValue());
			bounds.setRecorderComboWidth(jSP_recorderComboWidth.getValue());
			bounds.setDetailRows(jSP_detailRows.getValue());
			bounds.setStatusRows(jSP_statusRows.getValue());

			// リスト形式
			env.setDisableFazzySearch(jCBP_disableFazzySearch.isSelected());
			env.setDisableFazzySearchReverse(jCBP_disableFazzySearchReverse.isSelected());
			env.setDefaultFazzyThreshold(jSP_defaultFazzyThreshold.getValue());
			env.setSyoboFilterByCenters(jCBP_syoboFilterByCenters.isSelected());
			env.setDisplayPassedEntry(jCBP_displayPassedEntry.isSelected());
			env.setShowRsvPickup(jCBP_showRsvPickup.isSelected());
			env.setShowRsvDup(jCBP_showRsvDup.isSelected());
			env.setShowRsvUra(jCBP_showRsvUra.isSelected());
			env.setRsvdLineEnhance(jCBP_rsvdLineEnhance.isSelected());
			env.setRsvdLineColor(jCCL_rsvdLineColor.getChoosed());
			env.setPickedLineColor(jCCL_pickedLineColor.getChoosed());
			env.setCurrentLineEnhance(jCBP_currentLineEnhance.isSelected());
			env.setCurrentLineColor(jCCL_currentLineColor.getChoosed());
			env.setCurrentAfter(jSP_currentAfter.getValue()*60);
			env.setCurrentBefore(jSP_currentBefore.getValue()*60);
			env.setMatchedKeywordColor(jCCL_matchedKeywordColor.getChoosed());
			env.setShowWarnDialog(jCBP_showWarnDialog.isSelected());
			env.setSplitMarkAndTitle(jCBP_splitMarkAndTitle.isSelected());
			env.setShowDetailOnList(jCBP_showDetailOnList.isSelected());
			env.setRsvTargets(jSP_rsvTargets.getValue());
			env.setRowHeaderVisible(jCBP_rowHeaderVisible.isSelected());
			env.setDblClkCmd((DblClkCmd) jCBX_dblClkCmd.getSelectedItem());
			env.setSearchResultMax(jSP_searchResultMax.getValue());
			env.setSearchResultBufferMax(jSP_searchResultBufferMax.getValue());

			lvitems.clear();
			for (int row=0; row<jTable_listed.getRowCount(); row++) {
	    		ListColumnInfo cb = new ListColumnInfo();
	        	cb.setVisible((Boolean)jTable_listed.getValueAt(row, 0));
	        	cb.setName((String)jTable_listed.getValueAt(row, 1));
	        	cb.setId((Integer)jTable_listed.getValueAt(row, 2));
	        	lvitems.add(cb);
			}

			lnitems.clear();
			for (int row=0; row<jTable_listed_node.getRowCount(); row++) {
	    		ListColumnInfo cb = new ListColumnInfo();
	        	cb.setVisible((Boolean)jTable_listed_node.getValueAt(row, 0));
	        	cb.setName((String)jTable_listed_node.getValueAt(row, 1));
	        	cb.setId((Integer)jTable_listed_node.getValueAt(row, 2));
	        	lnitems.add(cb);
			}

			// 新聞形式関連
			{
				String selected = jRBP_getPaperRedrawType.getSelectedItem().getText();
				if ( PARER_REDRAW_PAGER.equals(selected) ) {
					env.setDrawcacheEnable(false);
					env.setCenterPerPage(jSP_centerPerPage.getValue());
				}
				else if ( PARER_REDRAW_CACHE.equals(selected) ) {
					env.setDrawcacheEnable(true);
					env.setCenterPerPage(0);
				}
				else if ( PARER_REDRAW_NORMAL.equals(selected) ) {
					env.setDrawcacheEnable(false);
					env.setCenterPerPage(0);
				}
			}
			env.setAllPageSnapshot(jCBP_allPageSnapshot.isSelected());
			env.setTooltipEnable(jCBP_tooltipEnable.isSelected());
			env.setTooltipInitialDelay(jSP_tooltipInitialDelay.getValue());
			env.setTooltipDismissDelay(jSP_tooltipDismissDelay.getValue());
			env.setTimerbarScrollEnable(jCBP_timerbarScrollEnable.isSelected());
			env.setPassedLogLimit(jSP_passedLogLimit.getValue());
			env.setDatePerPassedPage(jSP_datePerPage.getValue());
			env.setEffectComboToPaper(jCBP_effectComboToPaper.isSelected());
			env.setSnapshotFmt((SnapshotFmt) jCBX_snapshotFmt.getSelectedItem());
			env.setPrintSnapshot(jCBP_printSnapshot.isSelected());
			env.setMouseButtonForNodeAutoPaging(jCBP_mouseButtonForNodeAutoPaging.getSelectedIndex());
			env.setMouseButtonForNodeSwitch(jCBP_mouseButtonForNodeSwitch.getSelectedIndex());
			env.setMouseButtonForPageSwitch(jCBP_mouseButtonForPageSwitch.getSelectedIndex());

			// リスト・新聞形式共通
			env.setDisplayOnlyExecOnEntry(jCBP_displayOnlyExecOnEntry.isSelected());
			env.setDisplayPassedReserve(jCBP_displayPassedReserve.isSelected());
			env.setShowOnlyNonrepeated(jCBP_showOnlyNonrepeated.isSelected());
			env.setAdjLateNight(jCBP_adjLateNight.isSelected());
			env.setRootNodeVisible(jCBP_rootNodeVisible.isSelected());
			env.setSyncTreeWidth(jCBP_syncTreeWidth.isSelected());
			env.setShortExtMark(jCBP_shortExtMark.isSelected());
			for (int row=0; row<jTable_showmarks.getRowCount(); row++) {
				env.getOptMarks().put((TVProgram.ProgOption) jTable_showmarks.getValueAt(row, 3), (Boolean) jTable_showmarks.getValueAt(row, 0));
				env.getOptPostfixMarks().put((TVProgram.ProgOption) jTable_showmarks.getValueAt(row, 3), (Boolean) jTable_showmarks.getValueAt(row, 1));
			}
			for (int row=0; row<jTable_clipboard.getRowCount(); row++) {
				cbitems.get(row).setB((Boolean)jTable_clipboard.getValueAt(row, 0));
				cbitems.get(row).setItem((String)jTable_clipboard.getValueAt(row, 1));
				cbitems.get(row).setId((Integer)jTable_clipboard.getValueAt(row, 2));
			}
			env.getTvCommand().removeAll(env.getTvCommand());
			for (int row = 0; row < jTable_mitable.getRowCount(); row++) {
				TextValueSet tv = new TextValueSet();
				tv.setText((String) jTable_mitable.getValueAt(row, 0));
				tv.setValue((String) jTable_mitable.getValueAt(row, 1));
				env.getTvCommand().add(tv);
			}

			// 本体予約一覧対応
			for (int row=0; row<jTable_reserv.getRowCount(); row++) {
				rlitems.get(row).setVisible((Boolean)jTable_reserv.getValueAt(row, 0));
				rlitems.get(row).setName((String)jTable_reserv.getValueAt(row, 1));
				rlitems.get(row).setId((Integer)jTable_reserv.getValueAt(row, 2));
			}

			// タイトル一覧対応
			for (int row=0; row<jTable_title.getRowCount(); row++) {
				tlitems.get(row).setVisible((Boolean)jTable_title.getValueAt(row, 0));
				tlitems.get(row).setName((String)jTable_title.getValueAt(row, 1));
				tlitems.get(row).setId((Integer)jTable_title.getValueAt(row, 2));
			}

			// Web番組表対応
			env.setContinueTomorrow(jCBP_continueTomorrow.isSelected());
			env.setCacheTimeLimit(jSP_cacheTimeLimit.getValue());
			if ( env.getCacheTimeLimit() == 0 ) {
				env.setShutdownCmd((String) jCBX_shutdownCmd.getSelectedItem());
			}
			else {
				env.setShutdownCmd("");
			}
			env.setExpandTo8(jCBP_expandTo8.isSelected());
			//env.setUseDetailCache(jCBP_useDetailCache.isSelected());
			env.setUseDetailCache(false);
			env.setAutoEventIdComplete(jCBP_autoEventIdComplete.isSelected());
			env.setSplitEpno(jCBP_splitEpno.isSelected());
			env.setTraceOnlyTitle(jCBP_traceOnlyTitle.isSelected());
			env.setFixTitle(jCBP_fixTitle.isSelected());
			env.setNgword(jTextField_ngword.getText());
			env.setUserAgent(jTextField_userAgent.getText());
			env.setUseProxy(jCBP_useProxy.isSelected());
			env.setProxyAddr((String)jTextField_proxyAddr.getText());
			env.setProxyPort((String)jTextField_proxyPort.getText());
			env.setUseSyobocal(jCBP_useSyobocal.isSelected());
			env.setHistoryOnlyUpdateOnce(jCBP_historyOnlyUpdateOnce.isSelected());
			env.setUsePassedProgram(jCBP_usePassedProgram.isSelected());
			env.setPrepPassedProgramCount(jSP_prepPassedProgramCount.getValue());
			env.setDownloadProgramOnFixedTime(jCBP_downloadProgramOnFixedTime.isSelected());
			env.setDownloadProgramInBackground(jCBP_downloadProgramInBackground.isSelected());
			env.setDownloadProgramTimeList(jTextField_downloadProgramTimeList.getText());
			env.setUseProgCache(jCBP_useProgCache.isSelected());

			// レコーダ対応
			env.setForceLoadReserveDetails(jRBP_getRdReserveDetails.getSelectedIndex());
			env.setForceLoadAutoReserves(jRBP_getRdAutoReserves.getSelectedIndex());
			env.setForceLoadRecorded(jRBP_getRdRecorded.getSelectedIndex());
			env.setRecordedSaveScope(jCBX_recordedSaveScope.getSelectedIndex());

			// 予約
			env.setSpoexLength(String.format("%d",jSP_spoex_extend.getValue()));
			idx = jRBP_overlapUp.getSelectedIndex();
			switch (idx) {
			case 1:
				env.setOverlapUp(true);
				break;
			default:
				env.setOverlapUp(false);
				break;
			}
			idx = jRBP_overlapDown.getSelectedIndex();
			switch (idx) {
			case 1:
				env.setOverlapDown(true);
				env.setOverlapDown2(false);
				break;
			case 2:
				env.setOverlapDown(false);
				env.setOverlapDown2(true);
				break;
			default:
				env.setOverlapDown(false);
				env.setOverlapDown2(false);
				break;
			}
			env.setAutoFolderSelect(jCheckBox_autoFolderSelect.isSelected());
			env.setEnableCHAVsetting(jCheckBox_enableCHAVsetting.isSelected());
			env.setRangeLikeRsv(jSP_rangeLikeRsv.getValue());
			env.setGivePriorityToReserved(jCBP_givePriorityToReserved.isSelected());
			env.setGivePriorityToReservedTitle(jCBP_givePriorityToReservedTitle.isSelected());
			env.setAdjoiningNotRepetition(jCBP_adjoiningNotRepetition.isSelected());
			env.setShowAllIterationItem(jCBP_rsv_showallite.isSelected());
			env.setIterationItemForeground(jCCL_rsv_itecolor.getChoosed());
			env.setTunerShortColor(jCCL_rsv_tunshortcolor.getChoosed());
			env.setRecordedColor(jCCL_rsv_recedcolor.getChoosed());
			env.setUseAutocomplete(jCBP_useAutocomplete.isSelected());
			env.setNotifyBeforeProgStart(jCBP_notifyBeforeProgStart.isSelected());
			env.setNotifyBeforePickProgStart(jCBP_notifyBeforePickProgStart.isSelected());
			env.setMinsBeforeProgStart(jSP_minsBeforeProgStart.getValue());

			// タイトル一覧関係
			env.setShowTitleDetail(jCBP_showTitleDetail.isSelected());

			// その他の設定
			env.setUseHolidayCSV(jCBP_useHolidayCSV.isSelected());
			env.setHolidayFetchInterval(jSP_holidayCSVFetchInterval.getValue());
			env.setUpdateMethod((UpdateOn) jCBX_updateMethod.getSelectedItem());
			env.setDisableBeep(jCBP_disableBeep.isSelected());
			env.setShowSysTray(jCBP_showSysTray.isSelected());
			env.setHideToTray(jCBP_hideToTray.isSelected());
			env.setOnlyOneInstance(jCBP_onlyOneInstance.isSelected());
			env.setLookAndFeel((String) jComboBox_lookAndFeel.getSelectedItem());
			env.setFontName((String) jComboBox_font.getSelectedItem());
			env.setFontSize(Integer.valueOf((String) jComboBox_fontSize.getSelectedItem()));
			env.setUseGTKRC(jCBP_useGTKRC.isSelected());
			env.setUseRundll32(jCBP_useRundll32.isSelected());
			env.setDebug(jCBP_debug.isSelected());

			// 設定保存
			setEnv(reload_prog_needed);
			setUpdateButtonEnhanced(false);
		}
		catch(Exception e){
			e.printStackTrace();
		}

		MWin.appendMessage(String.format(MSGID+"更新が完了しました。所要時間： %.2f秒",tc.end()));
	}

	/**
	 * 各種設定タブ以外で変更したenvの内容をタブに反映する
	 */
	public void updateSelections() {
		jRBP_getRdReserveDetails.setSelectedIndex(env.getForceLoadReserveDetails());
		jRBP_getRdAutoReserves.setSelectedIndex(env.getForceLoadAutoReserves());
		jRBP_getRdRecorded.setSelectedIndex(env.getForceLoadRecorded());
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/*
	 * 連動
	 */

	/**
	 * 変更があった場合に番組表のリロードを要求するコンポーネントにつけるリスナー
	 */

	private void setUpdateButtonEnhanced(boolean b) {
		if (b) {
			jButton_update.setText("更新時番組表再取得あり");
			jButton_update.setForeground(Color.RED);
		}
		else {
			jButton_update.setText("更新を確定する");
			jButton_update.setForeground(Color.BLACK);
		}
		reload_prog_needed = b;
	}

	private final ItemListener IL_RELOAD_PROG_NEEDED = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.err.println(DBGID+"MODIFIED");
			setUpdateButtonEnhanced(true);
		}
	};
	private final DocumentListener DL_RELOAD_PROG_NEEDED = new DocumentListener() {
		@Override
		public void removeUpdate(DocumentEvent e) {
			if (debug) System.err.println(DBGID+"MODIFIED");
			setUpdateButtonEnhanced(true);
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			if (debug) System.err.println(DBGID+"MODIFIED");
			setUpdateButtonEnhanced(true);
		}
		@Override
		public void changedUpdate(DocumentEvent e) {
			if (debug) System.err.println(DBGID+"MODIFIED");
			setUpdateButtonEnhanced(true);
		}
	};
	private final CellEditorListener CEL_RELOAD_PROG_NEEDED = new CellEditorListener() {
		@Override
		public void editingStopped(ChangeEvent e) {
			if (debug) System.err.println(DBGID+"MODIFIED");
			setUpdateButtonEnhanced(true);
		}

		@Override
		public void editingCanceled(ChangeEvent e) {
		}
	};

	// あいまい検索
	ItemListener al_fazzysearch = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_fazzysearch");
			if (jCBP_disableFazzySearch.isSelected()) {
				jCBP_disableFazzySearchReverse.setEnabled(false);
				jSP_defaultFazzyThreshold.setEnabled(false);
			}
			else {
				jCBP_disableFazzySearchReverse.setEnabled(true);
				jSP_defaultFazzyThreshold.setEnabled(true);
			}
		}
	};

	// 予約行の背景色
	ItemListener al_rsvdlineenhance = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_rsvdlineenhance");
			if (jCBP_rsvdLineEnhance.isSelected()) {
				jLabel_rsvdLineColor.setEnabled(true);
				jCCL_rsvdLineColor.setEnabled(true);
				jLabel_pickedLineColor.setEnabled(true);
				jCCL_pickedLineColor.setEnabled(true);

			}
			else {
				jLabel_rsvdLineColor.setEnabled(false);
				jCCL_rsvdLineColor.setEnabled(false);
				jLabel_pickedLineColor.setEnabled(false);
				jCCL_pickedLineColor.setEnabled(false);
			}
		}
	};

	// 現在放送中行の背景色
	ItemListener al_currentlineenhance = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_currentlineenhance");
			if (jCBP_currentLineEnhance.isSelected()) {
				jLabel_currentLineColor.setEnabled(true);
				jCCL_currentLineColor.setEnabled(true);

			}
			else {
				jLabel_currentLineColor.setEnabled(false);
				jCCL_currentLineColor.setEnabled(false);
			}
		}
	};

	ItemListener il_paperredrawtype = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! il_paperredrawtype "+e.toString());
			if ( e.getStateChange() == ItemEvent.SELECTED ) {
				String selected = ((JRadioButton)e.getItem()).getText();
				if ( selected.equals(PARER_REDRAW_NORMAL) ) {
					jSP_centerPerPage.setEnabled(false);
					jCBP_allPageSnapshot.setEnabled(false);
				}
				else if ( selected.equals(PARER_REDRAW_CACHE) ) {
					jSP_centerPerPage.setEnabled(false);
					jCBP_allPageSnapshot.setEnabled(false);
				}
				else if ( selected.equals(PARER_REDRAW_PAGER) ) {
					jSP_centerPerPage.setEnabled(true);
					jCBP_allPageSnapshot.setEnabled(true);
					jSP_centerPerPage.setValue(env.getCenterPerPage()>0?env.getCenterPerPage():7);
				}
			}
		}
	};

	/*
	ItemListener al_drawcacheenable = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_drawcacheenable");
			if (jCBP_drawcacheEnable.isSelected()) {
				jCBP_pagerEnable.removeItemListener(il_pagerenable);
				jCBP_pagerEnable.setEnabled(false);
				jCBP_pagerEnable.addItemListener(il_pagerenable);

				jSP_centerPerPage.setEnabled(false);
				jCBP_allPageSnapshot.setEnabled(false);
			}
			else {
				jCBP_pagerEnable.removeItemListener(il_pagerenable);
				jCBP_pagerEnable.setEnabled(true);
				jCBP_pagerEnable.addItemListener(il_pagerenable);

				if (jCBP_pagerEnable.isSelected()) {
					jSP_centerPerPage.setEnabled(true);
					jCBP_allPageSnapshot.setEnabled(true);
				}
				else {
					jSP_centerPerPage.setEnabled(false);
					jCBP_allPageSnapshot.setEnabled(false);
				}
			}
		}
	};

	ChangeListener cl_centerperpage = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (debug) System.out.println("Fire! cl_centerperpage");
			if (jSP_centerPerPage.getValue() == 0) {
				jCBP_drawcacheEnable.removeItemListener(al_drawcacheenable);
				jCBP_drawcacheEnable.setEnabled(true);
				jCBP_allPageSnapshot.setEnabled(false);
				jCBP_drawcacheEnable.addItemListener(al_drawcacheenable);
			}
			else {
				jCBP_drawcacheEnable.removeItemListener(al_drawcacheenable);
				jCBP_drawcacheEnable.setSelected(false);
				jCBP_drawcacheEnable.setEnabled(false);
				jCBP_allPageSnapshot.setEnabled(true);
				jCBP_drawcacheEnable.addItemListener(al_drawcacheenable);
			}
		}
	};
	*/

	ItemListener al_tooltipenable = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_tooltipenable");
			if (jCBP_tooltipEnable.isSelected()) {
				jSP_tooltipInitialDelay.setEnabled(true);
				jSP_tooltipDismissDelay.setEnabled(true);
			}
			else {
				jSP_tooltipInitialDelay.setEnabled(false);
				jSP_tooltipDismissDelay.setEnabled(false);
			}
		}
	};

	ChangeListener cl_cachetimelimit = new ChangeListener() {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (debug) System.out.println("Fire! cl_cachetimelimit");
			if (jSP_cacheTimeLimit.getValue() == 0) {
				jCBX_shutdownCmd.setEnabled(true);
			}
			else {
				jCBX_shutdownCmd.setEnabled(false);
			}
		}
	};

	ItemListener al_splitepno = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_splitepno");
			if (jCBP_splitEpno.isSelected()) {
				jCBP_traceOnlyTitle.setEnabled(false);
			}
			else {
				jCBP_traceOnlyTitle.setEnabled(true);
			}
		}
	};

	ItemListener al_useproxy = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_useproxy");
			if (jCBP_useProxy.isSelected()) {
				jLabel_proxy.setEnabled(true);
				jTextField_proxyAddr.setEnabled(true);
				jTextField_proxyPort.setEnabled(true);
			}
			else {
				jLabel_proxy.setEnabled(false);
				jTextField_proxyAddr.setEnabled(false);
				jTextField_proxyPort.setEnabled(false);
			}
		}
	};

	ItemListener al_usepassedprogram = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_usepassedprogram");
			if (jCBP_usePassedProgram.isSelected()) {
				jSP_prepPassedProgramCount.setEnabled(true);
			}
			else {
				jSP_prepPassedProgramCount.setEnabled(false);
			}
		}
	};

	ItemListener al_giveprioritytoreserved = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_giveprioritytoreserved");
			if (jCBP_givePriorityToReserved.isSelected()) {
				jCBP_givePriorityToReservedTitle.setEnabled(true);
				jCBP_givePriorityToReservedTitle.setSelected(true);
			}
			else {
				jCBP_givePriorityToReservedTitle.setEnabled(false);
				jCBP_givePriorityToReservedTitle.setSelected(false);
			}
		}
	};

	ItemListener al_showallite = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if ( jCBP_rsv_showallite.isSelected() ) {
				jLabel_rsv_itecolor.setEnabled(true);
				jCCL_rsv_itecolor.setEnabled(true);
			}
			else {
				jLabel_rsv_itecolor.setEnabled(false);
				jCCL_rsv_itecolor.setEnabled(false);
			}
		}
	};

	ItemListener al_showsystray = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println(DBGID+"Fire! al_showsystray");
			if (jCBP_showSysTray.isSelected()) {
				jCBP_hideToTray.setEnabled(true);
			}
			else {
				jCBP_hideToTray.setEnabled(false);
			}
		}
	};

	ActionListener al_fontChanged = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			String fn = (String) jComboBox_font.getSelectedItem();
			int fs = Integer.valueOf((String) jComboBox_fontSize.getSelectedItem());

			fontChanged(fn, fs);

			if ( jLabel_fontSample != null ) {
				Font f = jLabel_fontSample.getFont();
				jLabel_fontSample.setFont(new Font(fn,f.getStyle(),fs));
			}
		}
	};

	ItemListener al_downloadProgramOnFixedTime = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println("Fire! al_downloadProgramOnFixedTime");
			if (jCBP_downloadProgramOnFixedTime.isSelected()) {
				jTextField_downloadProgramTimeList.setEnabled(true);
//				jCBP_downloadProgramInBackground.setEnabled(true);
			}
			else {
				jTextField_downloadProgramTimeList.setEnabled(false);
//				jCBP_downloadProgramInBackground.setEnabled(false);
			}
		}
	};

	private final ItemListener il_notifyBeforeProgStart = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			jSP_minsBeforeProgStart.setEnabled(jCBP_notifyBeforeProgStart.isSelected() || jCBP_notifyBeforePickProgStart.isSelected());
		}
	};

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JLabel getHeaderLabel(String text){
		JLabel label = new JLabel(text);
		if (font_header == null){
			Font font = label.getFont();
			font_header = font.deriveFont(font.getStyle() | Font.BOLD, font.getSize());
		}

		label.setFont(font_header);

		return label;
	}

	private JLabel getNoticeMsg(String text) {
		JLabel l = new JLabel(text);
		l.setForeground(NOTICEMSG_COLOR);
		return l;
	}

	// 更新確定ボタン
	private JButton getJButton_update(String s) {
		if (jButton_update == null) {
			jButton_update = new JButton(s);

			jButton_update.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateEnvs();
				}
			});
		}
		return(jButton_update);
	}

	/*
	 *  ツールバー表示対象アイテムの選択
	 */
	private JLabel getJLabel_toolbar(String s) {
		if (jLabel_toolbar == null) {
			jLabel_toolbar = new JLabel(s);
		}
		return jLabel_toolbar;
	}

	private JButton getJButton_toolbar_up(String s) {
		if (jButton_toolbar_up == null) {
			jButton_toolbar_up = new JButton(s);
			jButton_toolbar_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_toolbar, true);
				}
			});
		}
		return jButton_toolbar_up;
	}

	private JButton getJButton_toolbar_down(String s) {
		if (jButton_toolbar_down == null) {
			jButton_toolbar_down = new JButton(s);
			jButton_toolbar_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_toolbar, false);
				}
			});
		}
		return jButton_toolbar_down;
	}

	private JButton getJButton_toolbar_default(String s) {
		if (jButton_toolbar_default == null) {
			jButton_toolbar_default = new JButton(s);
			jButton_toolbar_default.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					ToolBarIconInfoList tbicons2 = new ToolBarIconInfoList();
					tbicons2.setDefault();
					setDefaultToDispTable(jTable_toolbar, tbicons2);
				}
			});
		}
		return jButton_toolbar_default;
	}

	private JScrollPane getJScrollPane_toolbar() {
		if (jScrollPane_toolbar == null) {
			jScrollPane_toolbar = new JScrollPane();
			jScrollPane_toolbar.setViewportView(getJTable_toolbar());
			jScrollPane_toolbar.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_toolbar);
	}
	private JNETable getJTable_toolbar() {
		if (jTable_toolbar == null) {
			jTable_toolbar = getDispTable(tbicons);
		}
		return(jTable_toolbar);
	}

	/*
	 *  表示対象タブの選択
	 */
	private JLabel getJLabel_tabs(String s) {
		if (jLabel_tabs == null) {
			jLabel_tabs = new JLabel(s);
		}
		return jLabel_tabs;
	}
	private JScrollPane getJScrollPane_tabs() {
		if (jScrollPane_tabs == null) {
			jScrollPane_tabs = new JScrollPane();
			jScrollPane_tabs.setViewportView(getJTable_tabs());
			jScrollPane_tabs.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_tabs);
	}
	private JNETable getJTable_tabs() {
		if (jTable_tabs == null) {
			jTable_tabs = getDispTable(tabitems, tab_selected, tab_selected);
		}
		return(jTable_tabs);
	}

	/*
	 * User-Agent
	 */
	private JLabel getJLabel_userAgent(String s)
	{
		if (jLabel_userAgent == null) {
			jLabel_userAgent = new JLabel(s);
		}
		return(jLabel_userAgent);
	}
	private JTextField getJTextField_userAgent(String s) {
		if (jTextField_userAgent == null) {
			jTextField_userAgent = new JTextFieldWithPopup();
			jTextField_userAgent.setText(s);
			jTextField_userAgent.setCaretPosition(0);
		}
		return jTextField_userAgent;
	}

	/*
	 * ┗　HTTPプロキシ/ポート
	 */
	private JLabel getJLabel_proxy(String s)
	{
		if (jLabel_proxy == null) {
			jLabel_proxy = new JLabel(s);
		}
		return(jLabel_proxy);
	}
	private JTextField getJTextField_proxyAddr(String s) {
		if (jTextField_proxyAddr == null) {
			jTextField_proxyAddr = new JTextFieldWithPopup();
			jTextField_proxyAddr.setText(s);
			jTextField_proxyAddr.setCaretPosition(0);
		}
		return jTextField_proxyAddr;
	}
	private JTextField getJTextField_proxyPort(String s) {
		if (jTextField_proxyPort == null) {
			jTextField_proxyPort = new JTextFieldWithPopup();
			jTextField_proxyPort.setText(s);
			jTextField_proxyPort.setCaretPosition(0);
		}
		return jTextField_proxyPort;
	}

	/*
	 * リスト表示対象アイテムの選択
	 */
	private JLabel getJLabel_listed(String s) {
		if (jLabel_listed == null) {
			jLabel_listed = new JLabel(s);
		}
		return jLabel_listed;
	}
	private JButton getJButton_listed_up(String s) {
		if (jButton_listed_up == null) {
			jButton_listed_up = new JButton(s);
			jButton_listed_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_listed, true);
				}
			});
		}
		return jButton_listed_up;
	}

	private JButton getJButton_listed_down(String s) {
		if (jButton_listed_down == null) {
			jButton_listed_down = new JButton(s);
			jButton_listed_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_listed, false);
				}
			});
		}
		return jButton_listed_down;
	}

	private JButton getJButton_listed_default(String s) {
		if (jButton_listed_default == null) {
			jButton_listed_default = new JButton(s);
			jButton_listed_default.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					ListedColumnInfoList lvitems2 = new ListedColumnInfoList();
					lvitems2.setDefault();
					setDefaultToDispTable(jTable_listed, lvitems2);
				}
			});
		}
		return jButton_listed_default;
	}

	private JScrollPane getJScrollPane_listed() {
		if (jScrollPane_listed == null) {
			jScrollPane_listed = new JScrollPane();
			jScrollPane_listed.setViewportView(getJTable_listed());
			jScrollPane_listed.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_listed);
	}

	private JNETable getJTable_listed() {
		if (jTable_listed == null) {
			jTable_listed = getDispTable(lvitems);
		}
		return(jTable_listed);
	}

	/*
	 * 表示順設定の共通関数
	 */
	private void setDefaultToDispTable(JTable jTable, ArrayList<ListColumnInfo> list){
		DefaultTableModel model = (DefaultTableModel)jTable.getModel();

		while(model.getRowCount() > 0){
			model.removeRow(0);
		}

		for (ListColumnInfo cb : list ) {
			Object[] data = { cb.getVisible(), cb.getName(), cb.getId() };
			model.addRow(data);
		}
	}

	private void moveSelRowInDispTable(JTable jTable, boolean up){
		int row = jTable.getSelectedRow();
		if (up){
			if (row <= 0) {
				return;
			}
		}
		else{
			if (row == -1 || row >= jTable.getRowCount()-1) {
				return;
			}
		}
		int rowNew = up ? row-1 : row+1;

		Object b = jTable.getValueAt(row, 0);
		Object item = jTable.getValueAt(row, 1);
		Object id = jTable.getValueAt(row, 2);

		jTable.setValueAt(jTable.getValueAt(rowNew, 0), row, 0);
		jTable.setValueAt(jTable.getValueAt(rowNew, 1), row, 1);
		jTable.setValueAt(jTable.getValueAt(rowNew, 2), row, 2);

		jTable.setValueAt(b, rowNew, 0);
		jTable.setValueAt(item, rowNew, 1);
		jTable.setValueAt(id, rowNew, 2);

		jTable.setRowSelectionInterval(rowNew, rowNew);

	}

	private JNETable getDispTable(ArrayList<ListColumnInfo> list) {
		return getDispTable(list, null, null);
	}

	private JNETable getDispTable(ArrayList<ListColumnInfo> list, boolean disables[], boolean selected[]) {
		// ヘッダの設定
		String[] colname = {"ﾁｪｯｸ", "アイテム", "ID"};
		int[] colwidth = {50,250,0};

		//
		DefaultTableModel model = new DefaultTableModel(colname, 0);
		JNETable jTable = new JNETable(model, false) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return (column == 0);
			}
		};

		jTable.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable.getColumnModel();
		TableColumn column = null;
		for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
			column = columnModel.getColumn(i);
			column.setPreferredWidth(colwidth[i]);
		}

		// にゃーん
		jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// エディタに手を入れる
		DefaultCellEditor editor = new DefaultCellEditor(new JCheckBox() {

			private static final long serialVersionUID = 1L;

			@Override
			public int getHorizontalAlignment() {
				return JCheckBox.CENTER;
			}
		});
		jTable.getColumn("ﾁｪｯｸ").setCellEditor(editor);
		// レンダラに手を入れる
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				//
				JCheckBox cBox = new JCheckBox();
				cBox.setHorizontalAlignment(JCheckBox.CENTER);
				//
				Boolean b = (Boolean)value;
				cBox.setSelected(b.booleanValue());
				//
				if (isSelected) {
					cBox.setBackground(table.getSelectionBackground());
				}
				else {
					cBox.setBackground(table.getBackground());
				}

				if (disables != null && disables.length > row && disables[row]){
					cBox.setEnabled(false);
				}
				if (selected != null && selected.length > row && selected[row]){
					cBox.setSelected(true);
				}

				return cBox;
			}
		};
		jTable.getColumn("ﾁｪｯｸ").setCellRenderer(renderer);

		//
		if (list != null){
			for (ListColumnInfo cb : list ) {
				Object[] data = { cb.getVisible(), cb.getName(), cb.getId() };
				model.addRow(data);
			}
		}

		return jTable;
	}

	/*
	 *  ツリービュー表示対象ノードの選択
	 */
	private JLabel getJLabel_listed_node(String s) {
		if (jLabel_listed_node == null) {
			jLabel_listed_node = new JLabel(s);
		}
		return jLabel_listed_node;
	}
	private JButton getJButton_listed_node_up(String s) {
		if (jButton_listed_node_up == null) {
			jButton_listed_node_up = new JButton(s);
			jButton_listed_node_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_listed_node, true);
				}
			});
		}
		return jButton_listed_node_up;
	}

	private JButton getJButton_listed_node_down(String s) {
		if (jButton_listed_node_down == null) {
			jButton_listed_node_down = new JButton(s);
			jButton_listed_node_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_listed_node, false);
				}
			});
		}
		return jButton_listed_node_down;
	}

	private JButton getJButton_listed_node_default(String s) {
		if (jButton_listed_node_default == null) {
			jButton_listed_node_default = new JButton(s);
			jButton_listed_node_default.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					ListedNodeInfoList lnitems2 = new ListedNodeInfoList();
					lnitems2.setDefault();
					setDefaultToDispTable(jTable_listed_node, lnitems2);
				}
			});
		}
		return jButton_listed_node_default;
	}

	private JScrollPane getJScrollPane_listed_node() {
		if (jScrollPane_listed_node == null) {
			jScrollPane_listed_node = new JScrollPane();
			jScrollPane_listed_node.setViewportView(getJTable_listed_node());
			jScrollPane_listed_node.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_listed_node);
	}

	private JNETable getJTable_listed_node() {
		if (jTable_listed_node == null) {
			jTable_listed_node = getDispTable(lnitems);
		}
		return(jTable_listed_node);
	}

	/*
	 *  本体予約一覧の表示対象アイテムの選択
	 */
	private JLabel getJLabel_reserv(String s) {
		if (jLabel_reserv == null) {
			jLabel_reserv = new JLabel(s);
		}
		return jLabel_reserv;
	}

	private JButton getJButton_reserv_up(String s) {
		if (jButton_reserv_up == null) {
			jButton_reserv_up = new JButton(s);
			jButton_reserv_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_reserv, true);
				}
			});
		}
		return jButton_reserv_up;
	}

	private JButton getJButton_reserv_down(String s) {
		if (jButton_reserv_down == null) {
			jButton_reserv_down = new JButton(s);
			jButton_reserv_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_reserv, false);
				}
			});
		}
		return jButton_reserv_down;
	}

	private JButton getJButton_reserv_default(String s) {
		if (jButton_reserv_default == null) {
			jButton_reserv_default = new JButton(s);
			jButton_reserv_default.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					ReserveListColumnInfoList rlitems2 = new ReserveListColumnInfoList();
					rlitems2.setDefault();
					setDefaultToDispTable(jTable_reserv, rlitems2);
				}
			});
		}
		return jButton_reserv_default;
	}

	private JScrollPane getJScrollPane_reserv() {
		if (jScrollPane_reserv == null) {
			jScrollPane_reserv = new JScrollPane();
			jScrollPane_reserv.setViewportView(getJTable_reserv());
			jScrollPane_reserv.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_reserv);
	}

	private JNETable getJTable_reserv() {
		if (jTable_reserv == null) {
			jTable_reserv = getDispTable(rlitems);
		}
		return(jTable_reserv);
	}

	/*
	 *  タイトル一覧の表示対象アイテムの選択
	 */
	private JLabel getJLabel_title(String s) {
		if (jLabel_title == null) {
			jLabel_title = new JLabel(s);
		}
		return jLabel_title;
	}

	private JButton getJButton_title_up(String s) {
		if (jButton_title_up == null) {
			jButton_title_up = new JButton(s);
			jButton_title_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_title, true);
				}
			});
		}
		return jButton_title_up;
	}

	private JButton getJButton_title_down(String s) {
		if (jButton_title_down == null) {
			jButton_title_down = new JButton(s);
			jButton_title_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_title, false);
				}
			});
		}
		return jButton_title_down;
	}

	private JButton getJButton_title_default(String s) {
		if (jButton_title_default == null) {
			jButton_title_default = new JButton(s);
			jButton_title_default.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					TitleListColumnInfoList tlitems2 = new TitleListColumnInfoList();
					tlitems2.setDefault();
					setDefaultToDispTable(jTable_title, tlitems2);
				}
			});
		}
		return jButton_title_default;
	}

	private JScrollPane getJScrollPane_title() {
		if (jScrollPane_title == null) {
			jScrollPane_title = new JScrollPane();
			jScrollPane_title.setViewportView(getJTable_title());
			jScrollPane_title.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_title);
	}
	private JNETable getJTable_title() {
		if (jTable_title == null) {
			jTable_title = getDispTable(tlitems);
		}
		return(jTable_title);
	}

	/*
	 *  表示マークの選択
	 */
	private JLabel getJLabel_showmarks(String s) {
		if (jLabel_showmarks == null) {
			jLabel_showmarks = new JLabel(s);
		}
		return jLabel_showmarks;
	}
	private JScrollPane getJScrollPane_showmarks() {
		if (jScrollPane_showmarks == null) {
			jScrollPane_showmarks = new JScrollPane();
			jScrollPane_showmarks.setViewportView(getJTable_showmarks());
			jScrollPane_showmarks.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_showmarks);
	}
	private JNETable getJTable_showmarks() {
		if (jTable_showmarks == null) {
			jTable_showmarks = getDispTable_showmarks();

			DefaultTableModel model = (DefaultTableModel)jTable_showmarks.getModel();

			//
			for (Object[] obj : TVProgram.optMarks) {
				Entry<ProgOption,Boolean> entryPrefix = null;
				for (Entry<ProgOption,Boolean> e : env.getOptMarks().entrySet()) {
					if (e.getKey() == obj[0]) {
						entryPrefix = e;
						break;
					}
				}

				Entry<ProgOption,Boolean> entryPostfix = null;
				for (Entry<ProgOption,Boolean> e : env.getOptPostfixMarks().entrySet()) {
					if (e.getKey() == obj[0]) {
						entryPostfix = e;
						break;
					}
				}

				Object label = obj.length > 1 ? obj[1] : ((ProgOption)obj[0]).getProgLabel();
				Object [] data = {
						entryPrefix != null ? entryPrefix.getValue() : Boolean.FALSE,
						entryPostfix != null ? entryPostfix.getValue() : Boolean.FALSE,
						label,
						obj[0]
				};

				model.addRow(data);
			}
		}
		return(jTable_showmarks);
	}

	/**
	 * 表示マーク用のテーブルを生成する
	 *
	 * @return		生成したJNETableオブジェクト
	 */
	private JNETable getDispTable_showmarks() {
		String prefix = "前";
		String postfix = "後";

		// ヘッダの設定
		String[] colname = {prefix, postfix, "アイテム", "ID"};
		int[] colwidth = {30,30,240,0};

		//
		DefaultTableModel model = new DefaultTableModel(colname, 0);
		JNETable jTable = new JNETable(model, false) {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(int row, int column) {
				return (column == 0 || column == 1);
			}
		};

		jTable.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable.getColumnModel();
		TableColumn column = null;
		for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
			column = columnModel.getColumn(i);
			column.setPreferredWidth(colwidth[i]);
		}

		// にゃーん
		jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// エディタに手を入れる
		DefaultCellEditor editor = new DefaultCellEditor(new JCheckBox() {

			private static final long serialVersionUID = 1L;

			@Override
			public int getHorizontalAlignment() {
				return JCheckBox.CENTER;
			}
		});

		jTable.getColumn(prefix).setCellEditor(editor);
		jTable.getColumn(postfix).setCellEditor(editor);

		// レンダラに手を入れる
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				//
				JCheckBox cBox = new JCheckBox();
				cBox.setHorizontalAlignment(JCheckBox.CENTER);
				//
				Boolean b = (Boolean)value;
				cBox.setSelected(b.booleanValue());
				//
				if (isSelected) {
					cBox.setBackground(table.getSelectionBackground());
				}
				else {
					cBox.setBackground(table.getBackground());
				}

				// 【新】【終】は後ろには表示できない
				if (column == 1 && (row == 0 || row == 1)){
					cBox.setEnabled(false);
					cBox.setSelected(false);
				}

				return cBox;
			}
		};
		jTable.getColumn(prefix).setCellRenderer(renderer);
		jTable.getColumn(postfix).setCellRenderer(renderer);

		return jTable;
	}

	/*
	 *  クリップボードアイテムの選択
	 */
	private JLabel getJLabel_clipboard(String s) {
		if (jLabel_clipboard == null) {
			jLabel_clipboard = new JLabel(s);
		}
		return jLabel_clipboard;
	}
	private JButton getJButton_clipboard_up(String s) {
		if (jButton_clipboard_up == null) {
			jButton_clipboard_up = new JButton(s);
			jButton_clipboard_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_clipboard, true);
				}
			});
		}
		return jButton_clipboard_up;
	}
	private JButton getJButton_clipboard_down(String s) {
		if (jButton_clipboard_down == null) {
			jButton_clipboard_down = new JButton(s);
			jButton_clipboard_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					moveSelRowInDispTable(jTable_clipboard, false);
				}
			});
		}
		return jButton_clipboard_down;
	}
	private JScrollPane getJScrollPane_clipboard() {
		if (jScrollPane_clipboard == null) {
			jScrollPane_clipboard = new JScrollPane();
			jScrollPane_clipboard.setViewportView(getJTable_clipboard());
			jScrollPane_clipboard.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_clipboard);
	}
	private JNETable getJTable_clipboard() {
		if (jTable_clipboard == null) {
			jTable_clipboard = getDispTable(null);

			DefaultTableModel model = (DefaultTableModel)jTable_clipboard.getModel();

			for (ClipboardInfo cb : getCbItemEnv()) {
				Object[] data = { cb.getB(), cb.getItem(), cb.getId() };
				model.addRow(data);
			}
		}
		return(jTable_clipboard);
	}

	/*
	 *  右クリックメニューの実行コマンドの追加
	 */
	private JLabel getJLabel_menuitem(String s) {
		if (jLabel_menuitem == null) {
			jLabel_menuitem = new JLabel(s);
		}
		return jLabel_menuitem;
	}
	private JTextField getJTextField_mikey() {
		if (jTextField_mikey == null) {
			jTextField_mikey = new JTextFieldWithPopup();
		}
		return (jTextField_mikey);
	}
	private JTextField getJTextField_mival() {
		if (jTextField_mival == null) {
			jTextField_mival = new JTextFieldWithPopup();
		}
		return (jTextField_mival);
	}
	private JComponent getJButton_miadd(String s) {
		if (jButton_miadd == null) {
			jButton_miadd = new JButton(s);
			jButton_miadd.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (jTextField_mikey.getText().length() > 0 && jTextField_mival.getText().length() > 0) {
						DefaultTableModel model = (DefaultTableModel) jTable_mitable.getModel();
						Object[] data = { jTextField_mikey.getText(),jTextField_mival.getText() };
						model.addRow(data);
						jTextField_mikey.setText("");
						jTextField_mival.setText("");
					}
				}
			});
		}
		return (jButton_miadd);
	}
	private JComponent getJButton_midel(String s) {
		if (jButton_midel == null) {
			jButton_midel = new JButton(s);
			jButton_midel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					DefaultTableModel model = (DefaultTableModel) jTable_mitable.getModel();
					int row = 0;
					if ((row = jTable_mitable.getSelectedRow()) >= 0) {
						jTextField_mikey.setText((String) model.getValueAt(row, 0));
						jTextField_mival.setText((String) model.getValueAt(row, 1));
						model.removeRow(row);
					}
				}
			});
		}
		return (jButton_midel);
	}
	private JButton getJButton_miup(String s) {
		if (jButton_miup == null) {
			jButton_miup = new JButton(s);
			jButton_miup.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					int row = 0;
					if ((row = jTable_mitable.getSelectedRow()) <= 0) {
						return;
					}
					Object name = jTable_mitable.getValueAt(row, 0);
					Object cmd = jTable_mitable.getValueAt(row, 1);

					jTable_mitable.setValueAt(jTable_mitable.getValueAt(row-1, 0), row, 0);
					jTable_mitable.setValueAt(jTable_mitable.getValueAt(row-1, 1), row, 1);

					jTable_mitable.setValueAt(name, row-1, 0);
					jTable_mitable.setValueAt(cmd, row-1, 1);

					jTable_mitable.setRowSelectionInterval(row-1, row-1);
				}
			});
		}
		return jButton_miup;
	}
	private JButton getJButton_midown(String s) {
		if (jButton_midown == null) {
			jButton_midown = new JButton(s);
			jButton_midown.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					int row = 0;
					if ((row = jTable_mitable.getSelectedRow()) >= jTable_mitable.getRowCount()-1) {
						return;
					}
					Object name = jTable_mitable.getValueAt(row, 0);
					Object cmd = jTable_mitable.getValueAt(row, 1);

					jTable_mitable.setValueAt(jTable_mitable.getValueAt(row+1, 0), row, 0);
					jTable_mitable.setValueAt(jTable_mitable.getValueAt(row+1, 1), row, 1);

					jTable_mitable.setValueAt(name, row+1, 0);
					jTable_mitable.setValueAt(cmd, row+1, 1);

					jTable_mitable.setRowSelectionInterval(row+1, row+1);
				}
			});
		}
		return jButton_midown;
	}
	private JScrollPane getJScrollPane_mitable(int col1_w, int col2_w) {
		if (jScrollPane_mitable == null) {
			jScrollPane_mitable = new JScrollPane();
			jScrollPane_mitable.setViewportView(getJTable_mitable(col1_w, col2_w));
			jScrollPane_mitable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return (jScrollPane_mitable);
	}
	private JNETable getJTable_mitable(int col1_w, int col2_w) {
		if (jTable_mitable == null) {
			// ヘッダの設定
			String[] colname = {"コマンド名", "実行するコマンド"};
			int[] colwidth = {col1_w,col2_w};

			//
			DefaultTableModel model = new DefaultTableModel(colname, 0);
			jTable_mitable = new JNETable(model, false) {

				private static final long serialVersionUID = 1L;

				@Override
				public boolean isCellEditable(int row, int column) {
					return (column == 0);
				}
			};
			jTable_mitable.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_mitable.getColumnModel();
			TableColumn column = null;
			for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
				column = columnModel.getColumn(i);
				column.setPreferredWidth(colwidth[i]);
			}

			// にゃーん
			jTable_mitable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			//
			for (TextValueSet tv : env.getTvCommand()) {
				Object[] data = { tv.getText(), tv.getValue() };
				model.addRow(data);
			}
		}
		return(jTable_mitable);
	}

	/*
	 * NGワード(;区切りで複数指定可)
	 */
	private JLabel getJLabel_ngword(String s) {
		if (jLabel_ngword == null) {
			jLabel_ngword = new JLabel(s);
		}
		return(jLabel_ngword);
	}
	private JTextField getJTextField_ngword(String s) {
		if (jTextField_ngword == null) {
			jTextField_ngword = new JTextFieldWithPopup();
			jTextField_ngword.setText(s);
			jTextField_ngword.setCaretPosition(0);
		}
		return jTextField_ngword;
	}

	/*
	 * ルック＆フィール
	 */
	private JLabel getJLabel_lookAndFeel(String s) {
		if (jLabel_lookAndFeel == null) {
			jLabel_lookAndFeel = new JLabel();
			jLabel_lookAndFeel.setText(s);
		}
		return jLabel_lookAndFeel;
	}
	private JComboBox getJComboBox_lookAndFeel() {
		if (jComboBox_lookAndFeel == null) {
			jComboBox_lookAndFeel = new JComboBox();

			// 初期値を設定
			DefaultComboBoxModel model = new DefaultComboBoxModel();
			jComboBox_lookAndFeel.setModel(model);
			for ( String className : getLAFEnv().getNames() ) {
				Matcher ma = Pattern.compile("\\.([^\\.]+?)LookAndFeel$").matcher(className);
				if ( ma.find() ) {
					model.addElement(ma.group(1));
				}
			}
			if ( ! env.getLookAndFeel().equals("")) {
				model.setSelectedItem(env.getLookAndFeel());
				//updateFont(env.getFontName(), env.getFontSize());
				StdAppendMessage("Set lookandfeel="+env.getLookAndFeel());
			}
			else {
				model.setSelectedItem(UIManager.getLookAndFeel().getName());
			}

			//
			jComboBox_lookAndFeel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					DefaultComboBoxModel model = (DefaultComboBoxModel)((JComboBox)e.getSource()).getModel();
					lafChanged((String)model.getSelectedItem());
				}
			});

		}
		return jComboBox_lookAndFeel;
	}

	/*
	 * 表示フォント
	 */
	private JLabel getJLabel_font(String s) {
		if (jLabel_font == null) {
			jLabel_font = new JLabel();
			jLabel_font.setText(s);
		}
		return jLabel_font;
	}
	private JComboBox getJComboBox_font() {
		if (jComboBox_font == null) {
			jComboBox_font = new JComboBox();

			// 初期値を設定
			DefaultComboBoxModel model = new DefaultComboBoxModel();
			jComboBox_font.setModel(model);
			for ( String f : getFontEnv().getNames() ) {
				model.addElement(f);
			}
			if ( ! env.getFontName().equals("")) {
				model.setSelectedItem(env.getFontName());
				//updateFont(env.getFontName(), env.getFontSize());
				//StdAppendMessage("システムのフォント： "+env.getFontName());
			}
			else {
				model.setSelectedItem(jComboBox_font.getFont().getFontName());
			}

			//
			jComboBox_font.addActionListener(al_fontChanged);

		}
		return jComboBox_font;
	}
	private JComboBox getJComboBox_fontSize() {
		if (jComboBox_fontSize == null) {
			jComboBox_fontSize = new JComboBox();
			DefaultComboBoxModel model = new DefaultComboBoxModel();
			jComboBox_fontSize.setModel(model);
			for ( int i=6; i<=24; i++ ) {
				model.addElement(String.valueOf(i));
			}
			if ( env.getFontSize() > 0) {
				jComboBox_fontSize.setSelectedItem(String.valueOf(env.getFontSize()));
			}

			jComboBox_fontSize.addActionListener(al_fontChanged);
		}
		return(jComboBox_fontSize);
	}
	private JLabel getJLabel_fontSample(String s) {
		if (jLabel_fontSample == null) {
			jLabel_fontSample = new JLabel();
			jLabel_fontSample.setText("012０１２３abcＡＢＣあいうアイウ阿伊宇○×？");
			jLabel_fontSample.setBackground(Color.WHITE);
			jLabel_fontSample.setBorder(new LineBorder(Color.BLACK));
			jLabel_fontSample.setOpaque(true);
			Font f = jLabel_fontSample.getFont();
			jLabel_fontSample.setFont(new Font(env.getFontName(),f.getStyle(),env.getFontSize()));
		}
		return jLabel_fontSample;
	}

	/*
	 * AV自動設定キーをジャンルからCHに
	 */
	private JLabel getJLabel_enableCHAVsetting(String s)
	{
		if (jLabel_enableCHAVsetting == null) {
			jLabel_enableCHAVsetting = new JLabel();
			jLabel_enableCHAVsetting.setText(s);
		}
		return(jLabel_enableCHAVsetting);
	}
	private JCheckBox getJCheckBox_enableCHAVsetting(boolean b) {
		if (jCheckBox_enableCHAVsetting == null) {
			jCheckBox_enableCHAVsetting = new JCheckBox();
			jCheckBox_enableCHAVsetting.setSelected(b);
		}
		return(jCheckBox_enableCHAVsetting);
	}

	/*
	 * 自動フォルダ選択を有効にする
	 */
	private JLabel getJLabel_autoFolderSelect(String s)
	{
		if (jLabel_autoFolderSelect == null) {
			jLabel_autoFolderSelect = new JLabel();
			jLabel_autoFolderSelect.setText(s);
		}
		return(jLabel_autoFolderSelect);
	}
	private JCheckBox getJCheckBox_autoFolderSelect(boolean b) {
		if (jCheckBox_autoFolderSelect == null) {
			jCheckBox_autoFolderSelect = new JCheckBox();
			jCheckBox_autoFolderSelect.setSelected(b);
		}
		return(jCheckBox_autoFolderSelect);
	}


	//
	private JTextAreaWithPopup getJta_help() {
		if ( jta_help == null ) {
			jta_help = CommonSwingUtils.getJta(this,2,0);
			jta_help.setText(TEXT_HINT);
		}
		return jta_help;
	}

	// ┗　取得時刻(HH:MM形式 ;区切りで複数指定可)
	private JTextField getJTextField_downloadProgramTimeList(String s) {
		if (jTextField_downloadProgramTimeList == null) {
			jTextField_downloadProgramTimeList = new JTextFieldWithPopup();
			jTextField_downloadProgramTimeList.setText(s);
			jTextField_downloadProgramTimeList.setCaretPosition(0);
		}
		return jTextField_downloadProgramTimeList;
	}
}
