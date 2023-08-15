package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import tainavi.HDDRecorder.RecType;
import tainavi.TVProgramIterator.IterationType;
import tainavi.VWMainWindow.MWinTab;
import tainavi.VWUpdate.UpdateResult;
import tainavi.Viewer.LoadFor;
import tainavi.Viewer.LoadRsvedFor;;

/**
 * ツールバーのクラス
 * @version 3.16.3β Viewer.classから分離
 */
public abstract class AbsToolBar extends JToolBar implements HDDRecorderSelectable {

	private static final long serialVersionUID = 1L;

	public static String getViewName() { return "ツールバー"; }

	public void setDebug(boolean b) {debug = b; }
	private boolean debug = false;

	protected SearchWordList swlist = new SearchWordList();

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	// 共用部品群

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();
	protected abstract TVProgramList getTVPrograms();
	protected abstract ChannelSort getChannelSort();
	protected abstract HDDRecorderList getHDDRecorders();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract Component getParentComponent();

	protected abstract void ringBeep();

	protected abstract ToolBarIconInfoList getTbIconEnv();

	// 前のページに移動する
	protected abstract void moveToPrevPage();
	// 前のページに移動可能かを返す
	protected abstract boolean isPrevPageEnabled();

	// 次のページに移動する
	protected abstract void moveToNextPage();
	// 次のページに移動可能かを返す
	protected abstract boolean isNextPageEnabled();

	// 親に依頼

	// リスト形式
	protected abstract boolean doKeywordSerach(SearchKey search, String kStr, String sStr, boolean doFilter);
	protected abstract boolean doBatchReserve();
	// 新聞形式
	protected abstract boolean jumpToNow();
	protected abstract boolean jumpToPassed(String passed);
	protected abstract void redrawByPager();
	protected abstract void toggleMatchBorder(boolean b);
	// 実行OFFの予約枠を表示するかどうかを切り替える
	protected abstract void toggleOffReserve(boolean b);
	protected abstract void setPaperColorDialogVisible(boolean b);
	protected abstract void setPaperZoom(int n);
	// 共通
	protected abstract boolean recorderSelectorChanged();
	protected abstract void takeSnapShot();
	protected abstract void openSearchDialog(JButton button);
	// メインウィンドウ
	/**
	 * 親から呼ばないでくださいね！
	 */
	protected abstract void setStatusVisible(boolean b);
	/**
	 * 親から呼ばないでくださいね！
	 */
	protected abstract void setFullScreen(boolean b);
	protected abstract boolean isTabSelected(MWinTab tab);
	// 部品
	protected abstract boolean addKeywordSearch(String label, SearchKey search);
	protected abstract boolean doLoadTVProgram(String selected);

	protected abstract boolean doLoadRdRecorder(String selected);

	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();
	private final TVProgramList tvprograms = getTVPrograms();
	private final ChannelSort chsort = getChannelSort();
	private final HDDRecorderList recorders = getHDDRecorders();
	private final ToolBarIconInfoList tbicons = getTbIconEnv();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// アイコンファイル

	private static final String ICONFILE_SEARCH			= "icon/system-search-2.png";
	private static final String ICONFILE_SEARCHMENU		= "icon/system-search-3.png";
	private static final String ICONFILE_ADDKEYWORD		= "icon/bookmark-new-list-4.png";
	private static final String ICONFILE_RELOADPROG		= "icon/internet-news-reader.png";
	private static final String ICONFILE_STOPRELOADPROG	= "icon/stop-news-reader.png";
	private static final String ICONFILE_BATCHCMD		= "icon/checkbox.png";
	private static final String ICONFILE_RELOADRSV		= "icon/video-television.png";
	private static final String ICONFILE_WAKEUP			= "icon/system-shutdown-2.png";
	private static final String ICONFILE_SHUTDOWN		= "icon/user-offline.png";
	private static final String ICONFILE_STATUSHIDDEN	= "icon/view-split-top-bottom-3.png";
	private static final String ICONFILE_STATUSSHOWN	= "icon/view-close.png";
	private static final String ICONFILE_TOFULL			= "icon/view-fullscreen-5.png";
	private static final String ICONFILE_TOWIN			= "icon/view-nofullscreen-3.png";
	private static final String ICONFILE_SHOWMATCHBORDER= "icon/view-calendar-timeline.png";
	private static final String ICONFILE_JUMPTONOW		= "icon/view-calendar-time-spent.png";
	private static final String ICONFILE_PALLET			= "icon/colorize-2.png";
	private static final String ICONFILE_TIMER			= "icon/tool_timer.png";
	private static final String ICONFILE_SCREENSHOT		= "icon/camera.png";
	private static final String ICONFILE_SHOWLOG		= "icon/utilities-log_viewer.png";
	private static final String ICONFILE_UPDATE			= "icon/system-software-update-2.png";
	private static final String ICONFILE_HELP			= "icon/help-browser-2.png";
	private static final String ICONFILE_PREVPAGE		= "icon/arrow-left-2.png";
	private static final String ICONFILE_NEXTPAGE		= "icon/arrow-right-2.png";
	private static final String ICONFILE_SHOWOFFRESERVE= "icon/show-off-reserve.png";

	private static final String ICONFILE_PULLDOWNMENU	= "icon/down-arrow.png";

	// ツールチップ関連

	private static final String TIPS_KEYWORD			= "<HTML><B>検索ボックスの書式</B><BR>検索：(オプション1) (オプション2) キーワード <BR>過去ログ検索：開始日[(YYYY/)MM/DD] 終了日[(YYYY/)MM/DD] (オプション2) キーワード<BR>過去ログ閲覧：日付[YYYY/MM/DD]<BR>※オプション1：@filter..絞込検索（過去ログは対象外）<BR>※オプション2：#title..番組名一致、#detail..番組詳細一致、なし..番組名＆番組詳細一致<BR><B>Ctrl+D:キーワードをコンボボックスから削除する</B><BR></HTML>";
	private static final String TIPS_SEARCH				= "キーワード検索 or 過去ログ閲覧";
	private static final String TIPS_SEARCH_MENU		= "検索設定画面(Ctrl+Shift+K)";
	private static final String TIPS_ADDKEYWORD			= "キーワードリストに登録(Ctrl+K)";
	private static final String TIPS_PAGER				= "ページャー";
	private static final String TIPS_RELOADPROG			= "Webから番組情報を再取得(Ctrl+W)";
	private static final String TIPS_STOPRELOADPROG		= "バックグランドで実行中の番組情報の取得を中止";
	private static final String TIPS_BATCHRESERVATION	= "一括予約(Ctrl+B)";
	private static final String TIPS_RECORDERSEL		= "操作するレコーダを選択する";
	private static final String TIPS_RELOADRSVED		= "レコーダから予約情報を再取得＆レコーダの各種設定情報の収集(Ctrl+R)";
	private static final String TIPS_WAKEUP				= "レコーダの電源を入れる";
	private static final String TIPS_DOWN				= "レコーダの電源を落とす";
	private static final String TIPS_STATUSHIDDEN		= "ステータスエリアを表示する(Alt+S)";
	private static final String TIPS_STATUSSHOWN		= "ステータスエリアを隠す(Alt+S)";
	private static final String TIPS_TOFULL				= "フルスクリーンモードへ(Alt+F)";
	private static final String TIPS_TOWIN				= "ウィンドウモードへ(Alt+F)";
	private static final String TIPS_SHOWBORDER			= "予約待機一覧を重ね合わせ表示する(Ctrl+O)";
	private static final String TIPS_JUMPTO				= "新聞形式の現在日時までジャンプ(Ctrl+N)";
	private static final String TIPS_PAPERCOLOR			= "新聞形式のジャンル別背景色を設定する(Alt+C)";
	private static final String TIPS_PAPERZOOM			= "新聞形式の番組枠の高さを拡大する";
	private static final String TIPS_SNAPSHOT			= "<HTML><P>スナップショットをとる(Ctrl+S)<P><B>★メモリの使用量が大きめなので、スナップショット作成後は再起動をおすすめします</B></HTML>";
	private static final String TIPS_LOGVIEW			= "ログをビューアで開く(Ctrl+L)";
	private static final String TIPS_UPDATE				= "オンラインアップデートを行う";
	private static final String TIPS_OPENHELP			= "ブラウザでヘルプを開く(Ctrl+H)";
	private static final String TIPS_PREVPAGE			= "新聞形式で前のページへ(Alt+←)";
	private static final String TIPS_NEXTPAGE			= "新聞形式で次のページへ(Alt+→)";
	private static final String TIPS_SHOWOFFRESERVE		= "実行OFFの予約を重ね合わせ表示する";

	// その他
	private static final String ESCKEYACTION = "escape-cancel";

	private static final String HELP_URL = "http://sourceforge.jp/projects/tainavi/wiki/FrontPage";

	private static final int OPENING_WIAT = 500;				// まあ起動時しか使わないんですけども

	// ログ関連

	private static final String MSGID = "["+getViewName()+"] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// ツールバーのコンポーネント

	protected JSearchWordComboBox jComboBox_keyword = null;
	protected JTextField jTextField_keyword = null;
	private JButton jButton_search = null;
	private JButton jButton_searchmenu = null;
	private JButton jButton_addkeyword = null;
	private JWideComboBox jComboBox_select_recorder = null;
	private JWideComboBox jComboBox_pager = null;
	private JButton jButton_reloadprogs = null;
	private JButton jButton_reloadprogmenu = null;
	private JPopupMenu jPopupMenu_reloadprogmenu = null;
	private JButton jButton_reloadrsved = null;
	private JButton jButton_reloadrsvedmenu = null;
	private JPopupMenu jPopupMenu_reloadrsvedmenu = null;
	private JButton jButton_batchreservation = null;
	private JToggleButton jToggleButton_showmatchborder = null;
	private JButton jButton_moveToNow = null;
	private JButton jButton_wakeup = null;
	private JButton jButton_shutdown = null;
	private JButton jButton_snapshot = null;
	private JButton jButton_paperColors = null;
	private JSlider jSlider_paperZoom = null;
	private JToggleButton jToggleButton_timer = null;
	private JButton jButton_logviewer = null;
	private JToggleButton jToggleButton_showstatus = null;
	private JToggleButton jToggleButton_fullScreen = null;
	private JButton jButton_update = null;
	private JButton jButton_help = null;
	private JButton jButton_prevpage = null;
	private JButton jButton_nextpage = null;
	private JToggleButton jToggleButton_showoffreserve = null;

	// レコーダ選択イベント発生時にキックするリスナーのリスト
	private final ArrayList<HDDRecorderListener> lsnrs_recsel = new ArrayList<HDDRecorderListener>();

	// 各種情報の変更イベント発生時にキックするリスナーのリスト
	private final ArrayList<HDDRecorderListener> lsnrs_infochg = new ArrayList<HDDRecorderListener>();

	// その他

	private String selectedMySelf = null;
	private HDDRecorderList selectedRecorderList = null;

	private boolean statusarea_shown = bounds.getShowStatus();

	private ArrayList<CancelListener> cancel_listeners = new ArrayList<CancelListener>();

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsToolBar() {

		super();

		swlist.load();

		createComponents();
		layoutComponents();
	}

	private void createComponents(){
		getJComboBox_keyword();
		getJButton_search("キーワード検索");
		getJButton_searchmenu("キーワード検索メニュー");
		getJButton_addkeyword("キーワード一覧に登録");

		getJButton_reloadprogs("番組情報再取得");
		getJButton_reloadprogmenu("番組情報再取得メニュー");
		getJToggleButton_showmatchborder("予約待機一覧を重ね合わせ表示する");
		getJToggleButton_showoffreserve("実行OFFの予約を重ね合わせ表示する");
		getJButton_moveToNow("現在日時");

		getJButton_prevpage();
		getJComboBox_pager();
		getJButton_nextpage();

		getJButton_batchreservation("一括予約");
		getJButton_reloadrsved("レコ情報再取得");
		getJButton_reloadrsvedmenu("レコ情報再取得メニュー");

		getJButton_wakeup("入");
		getJButton_shutdown("切");
		getJComboBox_select_recorder();

		getJButton_snapshot("スナップショット");
		getJButton_paperColors("ジャンル別背景色設定");
		getJSlider_paperZoom("番組枠表示拡大");
		getJButton_logviewer("ログビューア");
		getJToggleButton_timer("タイマー");

		getJToggleButton_showstatus("ステータス領域");
		getJToggleButton_fullScreen("全");

		getJButton_update("オンラインアップデート");
		getJButton_help("ヘルプ");
	}

	private void layoutComponents(){
		if (tbicons == null || tbicons.isDefault())
			layoutComponentsDefault();
		else
			layoutComponentsCustom();
	}

	private void layoutComponentsCustom(){
		this.removeAll();

		for (ListColumnInfo lci : tbicons){
			if (!lci.getVisible())
				continue;

			ToolBarIconInfo info = ToolBarIconInfo.getByName(lci.getName());
			if (info == null)
				continue;

			switch(info){
			case SEPARATOR1:
			case SEPARATOR2:
			case SEPARATOR4:
			case SEPARATOR6:
			case SEPARATOR7:
				this.addSeparator(new Dimension(4,0));
				break;
			case SEPARATOR3:
			case SEPARATOR5:
				this.addSeparator(new Dimension(6,0));
				break;
			case KEYWORD:
				this.add(jComboBox_keyword);
				break;
			case SEARCH:
				this.add(jButton_search);
				this.add(jButton_searchmenu);
				break;
			case ADDKEYWORD:
				this.add(jButton_addkeyword);
				break;
			case RELOADPROGS:
				this.add(jButton_reloadprogs);
				this.add(jButton_reloadprogmenu);
				break;
			case SHOWMATCHBORDER:
				this.add(jToggleButton_showmatchborder);
				break;
			case SHOWOFFRESERVE:
				this.add(jToggleButton_showoffreserve);
				break;
			case MOVETONOW:
				this.add(jButton_moveToNow);
				break;
			case PREVPAGE:
				this.add(jButton_prevpage);
				break;
			case PAGER:
				this.add(jComboBox_pager);
				break;
			case NEXTPAGE:
				this.add(jButton_nextpage);
				break;
			case BATCHRESERVATION:
				this.add(jButton_batchreservation);
				break;
			case RELOADRSVED:
				this.add(jButton_reloadrsved);
				this.add(jButton_reloadrsvedmenu);
				break;
			case WAKEUP:
				this.add(jButton_wakeup);
				break;
			case SHUTDOWN:
				this.add(jButton_shutdown);
				break;
			case SELECT_RECORDER:
				this.add(jComboBox_select_recorder);
				break;
			case SNAPSHOT:
				this.add(jButton_snapshot);
				break;
			case PAGER_COLORS:
				this.add(jButton_paperColors);
				break;
			case PAGER_ZOOM:
				this.add(jSlider_paperZoom);
				break;
			case LOGVIEWER:
				this.add(jButton_logviewer);
				break;
			case TIMER:
				this.add(jToggleButton_timer);
				break;
			case SHOWSTATUS:
				this.add(jToggleButton_showstatus);
				break;
			case FULL_SCREEN:
				this.add(jToggleButton_fullScreen);
				break;
			case UPDATE:
				this.add(jButton_update);
				break;
			case HELP:
				this.add(jButton_help);
				break;
			}
		}
	}

	private void layoutComponentsDefault(){
		this.removeAll();
		this.add(jComboBox_keyword);
		this.add(jButton_search);
		this.add(jButton_searchmenu);
		this.add(jButton_addkeyword);
		this.addSeparator(new Dimension(4,0));
		this.add(jButton_reloadprogs);
		this.add(jButton_reloadprogmenu);
		this.add(jToggleButton_showmatchborder);
		this.add(jToggleButton_showoffreserve);
		this.add(jButton_moveToNow);
		this.addSeparator(new Dimension(4,0));
		this.add(jButton_prevpage);
		this.add(jComboBox_pager);
		this.add(jButton_nextpage);
		this.addSeparator(new Dimension(6,0));
		this.add(jButton_batchreservation);
		this.add(jButton_reloadrsved);
		this.add(jButton_reloadrsvedmenu);
		this.addSeparator(new Dimension(4,0));
		this.add(jButton_wakeup);
		this.add(jButton_shutdown);
		this.add(jComboBox_select_recorder);
		this.addSeparator(new Dimension(6,0));
		this.add(jButton_snapshot);
		this.add(jButton_paperColors);
		this.add(jSlider_paperZoom);
		this.add(jButton_logviewer);
		this.add(jToggleButton_timer);
		this.addSeparator(new Dimension(4,0));
		this.add(jToggleButton_showstatus);
		this.add(jToggleButton_fullScreen);
		this.addSeparator(new Dimension(4,0));
		this.add(jButton_update);
		this.add(jButton_help);
	}

	public void reflectEnv(){
		// リロードメニューの書き換え
		updateReloadReservedExtension();
		updateReloadProgramExtension();

		// ページャーコンボボックスの書き換え
		setPagerItems();

		Dimension d = jComboBox_keyword.getPreferredSize();
		d.width = bounds.getSearchBoxAreaWidth();
		jComboBox_keyword.setPreferredSize(d);
		jComboBox_keyword.setMaximumSize(d);
		jComboBox_keyword.setMinimumSize(d);

		d = jComboBox_pager.getPreferredSize();
		d.width = bounds.getPagerComboWidth();
		jComboBox_pager.setPreferredSize(d);
		jComboBox_pager.setMaximumSize(d);
		jComboBox_pager.setMinimumSize(d);

		d = jComboBox_select_recorder.getPreferredSize();
		d.width = bounds.getRecorderComboWidth();
		jComboBox_select_recorder.setPreferredSize(d);
		jComboBox_select_recorder.setMaximumSize(d);
		jComboBox_select_recorder.setMinimumSize(d);

		updateShowOffReserveToggleButton();

		layoutComponents();
		this.repaint();
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	/**
	 * キーワード検索ボックスからの検索の実行
	 */
	protected void toolbarSearch() {
		int no = jComboBox_keyword.getSelectedIndex();
		String keywordStr = jTextField_keyword.getText().trim();
		String from = null;
		String to = null;
		SearchKey search = null;

		// 検索履歴を再度実行する場合
		if (no > 0){
			SearchWordItem swi = (SearchWordItem)swlist.getWordList().get(no-1).clone();
			if (keywordStr.equals(swi.getLabel())){
				keywordStr = swi.getKeyword();
				search = swi.getSearchKey();
				from = swi.getFrom();
				to = swi.getTo();

				if (search != null)
					new SearchProgram().compile(search);

				keywordSearch(swi.getLabel(), keywordStr, search, keywordStr, from, to, false);
				return;
			}
		}

		// 入力形式による分岐
		boolean doFilter = false;
		String sStr = null;
		String kStr = null;

		// 過去ログ閲覧
		if (keywordStr.matches("^\\d\\d\\d\\d/\\d\\d/\\d\\d$")) {
			if ( ! jumpToPassed(keywordStr)) {
				JOptionPane.showConfirmDialog(null, keywordStr+"はみつからなかったでゲソ！", "警告", JOptionPane.CLOSED_OPTION);
			}
			return;
		}

		if ( keywordStr.matches(("^@d(?:rop)?$"))) {
			doKeywordSerach(null,null,null,true);
			return;
		}

		Matcher ma = Pattern.compile("^(@(.+?)[ 　]+)").matcher(keywordStr);
		if ( ma.find() ) {
			// オプション指定
			if ( ma.group(2).matches("^f(ilter)?$")) {
				// 絞込検索
				kStr = keywordStr;
				kStr = kStr.substring(ma.group(1).length()-1,kStr.length()).trim();
				doFilter = true;
			}
		}
		else {
			ma = Pattern.compile("^(\\d\\d\\d\\d/)?(\\d\\d/\\d\\d)([ 　]+((\\d\\d\\d\\d/)?\\d\\d/\\d\\d))?[  　]+").matcher(keywordStr);
			if (ma.find()) {
				// 過去ログ検索(範囲指定あり）
				if (ma.group(1) == null || ma.group(1).length() == 0) {
					GregorianCalendar c = CommonUtils.getCalendar(0);
					from = String.format("%04d/%s", c.get(Calendar.YEAR), ma.group(2));
				}
				else {
					from = ma.group(1)+ma.group(2);
				}
				String cD = CommonUtils.getDate529(0,false);
				String pD = CommonUtils.getDate529(-86400,false);

				if (ma.group(4) == null) {
					to = pD;
				}
				else {
					if (ma.group(5) == null) {
						GregorianCalendar c = CommonUtils.getCalendar(0);
						to = String.format("%04d/%s", c.get(Calendar.YEAR), ma.group(4));
					}
					else {
						to = ma.group(4);
					}
				}
				if (from.compareTo(to) > 0) {
					// 開始日と終了日が逆転していたら入れ替える
					String tD = from;
					from = to;
					to = tD;
				}

				sStr = String.format("%s-%s", from, to);
				kStr = ma.replaceFirst("");
			}
			else {
				// 通常ログ検索
				kStr = keywordStr.trim();
			}
		}

		if ( kStr == null || kStr.matches("^[ 　]*$") ) {
			// 検索キーワードがない
			doKeywordSerach(null,null,null,false);
			return;
		}

		// 検索キーワードの解析
		if (search == null)
			search = decSearchKeyText(kStr);
		if ( search == null ) {
			return;
		}

		// 検索履歴に追加する
		if (swlist.add(keywordStr, kStr, search, from, to)){
			swlist.save();
			updateKeywordComboBox();
			selectKeywordComboBox(kStr, search, from, to);
		}

		// 検索を実行する
		if (search.alTarget.size() > 0) {
			doKeywordSerach(search, kStr, sStr, doFilter);
		}
	}

	/**
	 * キーワード検索指定画面からの検索の実行
	 *
	 * @param label 検索履歴の名称
	 * @param keywordStr 検索キーワードの名称
	 * @param search 検索キー
	 * @param kStr 検索キーワード
	 * @param from 日付の下限
	 * @param to 日付の上限
	 * @param doFileter 絞り込みかどうか
	 */
	protected void keywordSearch(String label, String keywordStr, SearchKey search, String kStr, String from, String to, boolean doFilter) {
		if ( keywordStr.matches(("^@d(?:rop)?$"))) {
			doKeywordSerach(null,null,null,true);
			return;
		}

		// 検索履歴に追加する
		if (swlist.add(label, kStr, search, from, to)){
			swlist.save();
			updateKeywordComboBox();
			selectKeywordComboBox(kStr, search, from, to);
		}

		// 検索キーワードを解析する
		if (search == null)
			search = decSearchKeyText(kStr);
		if ( search == null ) {
			return;
		}

		// 日付指定を文字列に変換する
		String range = null;
		if (from != null && !from.isEmpty()){
			if (to == null || to.isEmpty())
				to = CommonUtils.getDate529(-86400,false);

			if (from.compareTo(to) > 0){
				range = to + "-" + from;
			}
			else
				range = from + "-" + to;
		}

		// 検索を実行する
		doKeywordSerach(search, kStr, range, doFilter);
	}

	/*
	 * 検索キーワードボックスから検索履歴を選択する
	 *
	 * @param s 検索キーワード
	 * @param k 検索キー
	 * @param f 日付の下限
	 * @param t 日付の上限
	 */
	protected void selectKeywordComboBox(String s, SearchKey k, String f, String t){
		for (int n=1; n<jComboBox_keyword.getItemCount(); n++){
			SearchWordItem swi = swlist.getWordList().get(n-1);
			if (swi.equals(s, k, f, t)){
				jComboBox_keyword.setSelectedIndex(n);
				break;
			}
		}
	}

	/**
	 * キーワード検索ボックスに入力された字句の解析
	 * @param kStr キーワード検索ボックスに入力された字句
	 * @return
	 */
	private SearchKey decSearchKeyText(String kStr) {
		SearchKey sk = new SearchKey();

		String tStr = "";
		String rStr = "";
		String cStr = "";

		// オプションあり
		Matcher	mc = Pattern.compile("^(#title|#detail)?[  　]+(.*)$").matcher(kStr);
		if (mc.find()){
			String keyword = mc.group(2);
			if (mc.group(1).equals("#title")){
				tStr += "1\t";
				rStr += keyword + "\t";
				cStr += "0\t";
			}
			else{
				tStr += "2\t";
				rStr += keyword + "\t";
				cStr += "0\t";
			}
		}
		else{
			tStr += "0\t";
			rStr += kStr + "\t";
			cStr += "0\t";
		}

		sk.setLabel(kStr);
		sk.setTarget(tStr);		// compile後は順番が変わるので残すことにする
		sk.setKeyword(rStr);	// 同上
		sk.setContain(cStr);	// 同上

		sk.setCondition("0");
		sk.setInfection("0");
		sk.setOkiniiri("0");
		sk.setCaseSensitive(false);
		sk.setShowInStandby(false);

		new SearchProgram().compile(sk);

		return sk;
	}

	public void setFocusInSearchBox() {
		jTextField_keyword.requestFocusInWindow();
	}

	/*
	 * 「検索設定画面」を表示する
	 */
	public void openSearchDialog() {
		openSearchDialog(jButton_searchmenu);
	}

	/**
	 *
	 */
	public void setAddkeywordEnabled(boolean b) {
		jButton_addkeyword.setEnabled(b);
	}

	/*
	 * TVプログラムの再取得
	 */
	public void setReloadTVProgramsInProgress(boolean b){
		if (jButton_reloadprogs != null){
			ImageIcon icon = new ImageIcon(b ? ICONFILE_STOPRELOADPROG : ICONFILE_RELOADPROG);
			jButton_reloadprogs.setIcon(icon);
			jButton_reloadprogs.setToolTipText(b ? TIPS_STOPRELOADPROG : TIPS_RELOADPROG);
		}

		if (jButton_reloadprogmenu != null){
			jButton_reloadprogmenu.setEnabled(!b);
			if (b)
				jButton_reloadprogmenu.removeMouseListener(ma_reloadProgramExtension);
			else
				jButton_reloadprogmenu.addMouseListener(ma_reloadProgramExtension);
		}
	}
	/**
	 * ばちー
	 */
	public void setBatchReservationEnabled(boolean b) {
		jButton_batchreservation.setEnabled(b);
	}

	/**
	 * すなー
	 */
	public void setSnapShotEnabled(boolean b) {
		jButton_snapshot.setEnabled(b);
	}

	/**
	 * ぺぱー
	 */
	public void setPaperColorDialogEnabled(boolean b) {
		jButton_paperColors.setEnabled(b);
		jSlider_paperZoom.setEnabled(b);
	}

	/**
	 * ぼだー
	 */
	public void setBorderToggleEnabled(boolean b, boolean cond) {
		jToggleButton_showmatchborder.removeActionListener(al_showborder);

		jToggleButton_showmatchborder.setEnabled(b);
		jToggleButton_showmatchborder.setSelected(cond);

		jToggleButton_showmatchborder.addActionListener(al_showborder);
	}

	/*
	 * 実行OFFの予約を表示するトグルボタンの状態を更新する
	 */
	public void updateShowOffReserveToggleButton(){
		jToggleButton_showoffreserve.setEnabled(!env.getDisplayOnlyExecOnEntry() && (isTabSelected(MWinTab.PAPER) || isTabSelected(MWinTab.LISTED)));
		jToggleButton_showoffreserve.setSelected(bounds.getShowOffReserve());
	}

	/**
	 * ページャーコンボボックスの有効無効。新聞形式を開いている時以外は有効にならないよ
	 * @param b
	 */
	public void setPagerEnabled(boolean b) {// 新聞形式を開いてないとだめだよ
		jComboBox_pager.removeItemListener(il_pagerSelected);	// そんな…

		jComboBox_pager.setEnabled(b && isTabSelected(MWinTab.PAPER) && env.isPagerEnabled());

		jComboBox_pager.addItemListener(il_pagerSelected);

		updatePagerButtons();
	}

	public void updatePagerButtons(){
		jButton_prevpage.setEnabled(isPrevPageEnabled());
		jButton_nextpage.setEnabled(isNextPageEnabled());
	}

	/**
	 * ページャーコンボボックスの書き換え（汎用版）
	 */
	public void setPagerItems() {

		if ( env.isPagerEnabled() ) {
			TVProgramIterator pli = tvprograms.getIterator().build(chsort.getClst(), IterationType.ALL);
			setPagerItems(pli,null);
		}
		else {
			jComboBox_pager.removeItemListener(il_pagerSelected);
			jComboBox_pager.removeAllItems();
			jComboBox_pager.addItemListener(il_pagerSelected);
		}

	}

	/**
	 * ページャーコンボボックスの書き換え（こちらはPaperViewからしか呼ばれないはずである）
	 */
	public void setPagerItems(TVProgramIterator pli, Integer curindex) {

		if ( ! env.isPagerEnabled() ) {
			return;
		}

		int total_page = 1+env.getPageIndex(pli.size()-1);

		// イベント停止
		jComboBox_pager.removeItemListener(il_pagerSelected);

		int index = jComboBox_pager.getSelectedIndex();

		// ページャー書き換え
		jComboBox_pager.removeAllItems();

		// これは…
		if ( total_page == 0 ) {
			// イベント再開…はしなくていいか
			return;
		}

		pli.rewind();	// 巻き戻してください
		int idx = 0;

		for (int np=0; np<total_page; np++) {
			String centers = "";
			for ( int nc=0; pli.hasNext(); nc++) {
				if (env.getPageIndex(idx) != np)
					break;

				centers += pli.next().Center+"、";
				idx++;
			}
			centers = centers.replaceFirst(".$", "");
			jComboBox_pager.addItem((np+1)+"/"+total_page+((centers.length()>0)?(" - "+centers):("")));
		}

		// 選択するページ番号を決定する
		int newindex = 0;
		if ( curindex == null ) {
			// 指定されていないなら基本は以前選択されていたもの
			newindex = (index>=0) ? (index) : (0);
		}
		else {
			// 指定されていればそれ
			newindex = curindex;
		}
		if ( newindex >= jComboBox_pager.getItemCount() ) {
			// 書き換えの結果ページの数が減ってしまう（通常ここにはこないはずなので、これは例外防止用）
			if (debug) System.out.println(DBGID+"ページ数が変更された： "+newindex+" -> "+jComboBox_pager.getItemCount());
			newindex = 0;
		}
		jComboBox_pager.setSelectedIndex(newindex);
//		jComboBox_pager.setToolTipText(jComboBox_pager.getSelectedItem().toString());

		updatePagerButtons();
		updatePagerTooltipText();

		// イベント再開
		jComboBox_pager.addItemListener(il_pagerSelected);
	}

	/**
	 * ページャーコンボボックスのアイテム数
	 */
	public int getPagerCount() {
		return jComboBox_pager.getItemCount();
	}

	/**
	 * ページャーコンボボックスの選択位置
	 */
	public int getSelectedPagerIndex() {
		return jComboBox_pager.getSelectedIndex();
	}

	/**
	 * ページャーコンボボックスの選択
	 */
	public void setSelectedPagerIndex(int idx) {
		if (jComboBox_pager.isEnabled() ) {
			jComboBox_pager.setSelectedItem(null);
			jComboBox_pager.setSelectedIndex(idx);

			updatePagerButtons();
			updatePagerTooltipText();
		}
	}

	public void updatePagerTooltipText(){
		StringJoiner sb = new StringJoiner("<BR>");
		int pno = getSelectedPagerIndex();
		if (pno >= 0 && pno < getPagerCount()){
			String text = (String)jComboBox_pager.getItemAt(pno);
			Matcher ma = Pattern.compile("([1-9]/[1-9]) - (.*)").matcher(text);
			if (ma.find()){
				String [] centers = ma.group(2).split("、");
				int no = 1;
				for (String center : centers){
					sb.add(String.valueOf(no++) + ":" + center);
				}

				String tooltip = "<HTML><B>" + TIPS_PAGER + "(" + ma.group(1) + ")</B><BR>" + sb.toString() + "</HTML>";
				jComboBox_pager.setToolTipText(tooltip);
				return;
			}
		}

		jComboBox_pager.setToolTipText(TIPS_PAGER);
	}

	/*
	 *  前の放送局ページに移動する
	 */
	public void moveToPrevCenterPage(){
		int pno = getSelectedPagerIndex();
		if (pno <= 0)
			return;

		setSelectedPagerIndex(pno-1);
		redrawByPager();
	}

	/*
	 * 前の放送局ページに移動可能かを返す
	 */
	public boolean isPrevCenterPageEnabled(){
		if (jComboBox_pager == null || jButton_prevpage == null)
			return false;
		if (!jComboBox_pager.isEnabled() || !isTabSelected(MWinTab.PAPER) || !env.isPagerEnabled())
			return false;

		int pno = getSelectedPagerIndex();
		return pno > 0;
	}

	/*
	 *  次の放送局ページに移動する
	 */
	public void moveToNextCenterPage(){
		int pno = getSelectedPagerIndex();
		int pnum = getPagerCount();
		if (pno >= pnum-1)
			return;

		setSelectedPagerIndex(pno+1);
		redrawByPager();
	}

	/*
	 * 次の放送局ページに移動可能かを返す
	 */
	public boolean isNextCenterPageEnabled(){
		if (jComboBox_pager == null || jButton_prevpage == null)
			return false;
		if (!jComboBox_pager.isEnabled() || !isTabSelected(MWinTab.PAPER) || !env.isPagerEnabled())
			return false;

		int pno = getSelectedPagerIndex();
		int pnum = getPagerCount();

		return pno < pnum-1;
	}

	/**
	 * 指定のレコーダは選択されているか
	 */
	public boolean isRecorderSelected(String myself) {
		String sid = getSelectedRecorder();
		if ( sid == null || sid.equals(myself)) {
			return true;
		}
		return false;
	}

	/**
	 * 選択されているレコーダー（のMySelf()）を返す
	 * @return 「すべて」が選択されている場合はNULL、「ピックアップ」が選択されている場合は""を返す
	 */
	public String getSelectedRecorder() {

		if ( jComboBox_select_recorder == null ) {
			return HDDRecorder.SELECTED_ALL;
		}

		int sno = jComboBox_select_recorder.getSelectedIndex();
		if (sno == 0)
			return HDDRecorder.SELECTED_ALL;

		int no=1;
		for (HDDRecorder r : recorders) {
			switch ( r.getType() ) {
			case RECORDER:
			case EPG:
			case MAIL:
			case NULL:
				if (no == sno)
					return r.Myself();
				break;
			default:
				break;
			}

			no++;
		}

		String recId = (String)jComboBox_select_recorder.getSelectedItem();

		/*
		if ( recId.equals(HDDRecorder.SELECTED_ALL) ) {
			return HDDRecorderListener.SELECTED_ALL;
		}
		else if ( recId.equals(HDDRecorder.SELECTED_PICKUP) ) {
			return HDDRecorderListener.SELECTED_PICKUP;
		}
		*/

		return recId;
	}

	/**
	 * 選択しているレコーダを変える
	 * @param myself 「すべて」を選択する場合はNULLを渡す
	 */
	public void setSelectedRecorder(String myself) {
		if ( jComboBox_select_recorder != null ) {
			jComboBox_select_recorder.setSelectedItem(null);
			if (myself == null){
				jComboBox_select_recorder.setSelectedItem(HDDRecorder.SELECTED_ALL);
				return;
			}

			int no=1;
			for (HDDRecorder r : recorders) {
				switch ( r.getType() ) {
				case RECORDER:
				case EPG:
				case MAIL:
				case NULL:
					if (myself.equals(r.Myself())){
						jComboBox_select_recorder.setSelectedIndex(no);
						return;
					}
					break;
				default:
					break;
				}
				no++;
			}

			jComboBox_select_recorder.setSelectedItem(myself);
		}
	}

	/**
	 * レコーダコンボボックスを初期化する
	 */
	public void updateRecorderComboBox() {

		jComboBox_select_recorder.removeItemListener(il_recorderSelected);

		// レコーダの選択情報をリセット
		setSelectedRecorderInfo(null);

		jComboBox_select_recorder.removeAllItems();
		jComboBox_select_recorder.addItem(HDDRecorder.SELECTED_ALL);
		for (HDDRecorder r : recorders) {
			switch ( r.getType() ) {
			case RECORDER:
			case EPG:
			case MAIL:
			case NULL:
				jComboBox_select_recorder.addItem(r.getDispName());
				break;
			default:
				break;
			}
		}
		jComboBox_select_recorder.addItem(HDDRecorder.SELECTED_PICKUP);

		jComboBox_select_recorder.addItemListener(il_recorderSelected);
	}

	/**
	 *
	 */
	public boolean updateReloadReservedExtension() {

		// 消して
		jPopupMenu_reloadrsvedmenu.removeAll();

		// 追加する（毎回無視設定のメニューは赤で強調）
		for ( LoadRsvedFor lrf : LoadRsvedFor.values() ) {
			JMenuItem menuItem = new JMenuItem(lrf.getName());
			switch ( lrf ) {
			case DETAILS:
				if ( env.getForceLoadReserveDetails() == 2 ) {
					menuItem.setForeground(Color.RED);
				}
				break;
			case AUTORESERVE:
				if ( env.getForceLoadAutoReserves() == 2 ) {
					menuItem.setForeground(Color.RED);
				}
				break;
			case RECORDED:
				if ( env.getForceLoadRecorded() == 2 ) {
					menuItem.setForeground(Color.RED);
				}
				break;
			default:
				break;
			}

			jPopupMenu_reloadrsvedmenu.add(menuItem);

			menuItem.addActionListener(al_reloadReservedIndividual);
		}

		return true;
	}

	/**
	 *
	 */
	public boolean updateReloadProgramExtension() {

		// 消して
		jPopupMenu_reloadprogmenu.removeAll();

		// 追加する
		for (LoadFor lf : reloadProgMenu ) {
			if ( lf == null ) {
				jPopupMenu_reloadprogmenu.addSeparator();
				continue;
			}

			if ( (lf == LoadFor.CSwSD && ! env.isShutdownEnabled()) ||
					(lf == LoadFor.SYOBO && ! env.getUseSyobocal()) ) {
				// 無効なメニューがある
				continue;
			}

			JMenuItem menuItem = new JMenuItem(lf.getName());
			jPopupMenu_reloadprogmenu.add(menuItem);

			menuItem.addActionListener(al_reloadProgramIndividual);
		}

		return true;
	}

	/**
	 * ステータスエリアは表示中？
	 */
	public boolean isStatusShown() {
		return jToggleButton_showstatus.isSelected();
	}

	/**
	 * フルスクリーン化しているかな？
	 */
	public boolean isFullScreen() {
		return jToggleButton_fullScreen.isSelected();
	}

	/**
	 *  キーワード検索への登録ボタンが押された
	 */
	public void doKeywordAdded(){
		jButton_addkeyword.doClick();
	}

	/**
	 *  キーワードを削除する
	 */
	public void doKeywordDeleted(){
		int no = jComboBox_keyword.getSelectedIndex();
		if (no < 1)
			return;

		swlist.getWordList().remove(no-1);
		swlist.save();
		updateKeywordComboBox();

		if (jComboBox_keyword.getItemCount() > no)
			jComboBox_keyword.setSelectedIndex(no);
		else
			jTextField_keyword.setText("");
	}

	/*
	 *  予約一覧の再取得
	 */
	public void doReloadReserved(){
		jButton_reloadrsved.doClick();
	}

	/*
	 * 「新聞形式」ビューの表示倍率を変える
	 */
	public void doChangePaperZoom(boolean up){
		JSlider sl = jSlider_paperZoom;
		if (!sl.isEnabled())
			return;

		int valueNew = sl.getValue() + (up ? 5 : -5);
		if (valueNew < sl.getMinimum() || valueNew > sl.getMaximum())
			return;

		sl.setValue(valueNew);
		setPaperZoom(sl.getValue());
	}
	/*
	 *  ステータスエリアを出したりしまったり
	 */
	public void doToggleShowStatus(){
		jToggleButton_showstatus.doClick();
	}

	/*
	 *  フルスクリーンになったりウィンドウになったり
	 */
	public void doFullScreen(){
		jToggleButton_fullScreen.doClick();;
	}

	/*
	 *  新聞形式に予約待機枠を表示させたりしなかったり
	 */
	public void doShowBorder(){
		jToggleButton_showmatchborder.doClick();
	}

	/*
	 *  ログをビューアで開く
	 */
	public void doLogView(){
		jButton_logviewer.doClick();
	}

	/*
	 *  ヘルプを開く
	 */
	public void doOpenHelp(){
		jButton_help.doClick();
	}

	/*******************************************************************************
	 * リスナー追加／削除
	 ******************************************************************************/

	/**
	 * レコーダ選択イベントリスナー
	 */
	@Override
	public void addHDDRecorderSelectionListener(HDDRecorderListener l) {
		if ( ! lsnrs_recsel.contains(l) ) {
			lsnrs_recsel.add(l);
		}
	}

	@Override
	public void removeHDDRecorderSelectionListener(HDDRecorderListener l) {
		lsnrs_recsel.remove(l);
	}

	@Override
	public String getSelectedMySelf() {
		return selectedMySelf;
	}

	@Override
	public HDDRecorderList getSelectedList() {
		return selectedRecorderList;
	}

	/**
	 * 情報変更イベントリスナー（番組表リロード、レコーダ情報リロード、etc）
	 */
	@Override
	public void addHDDRecorderChangeListener(HDDRecorderListener l) {
		if ( ! lsnrs_infochg.contains(l) ) {
			lsnrs_infochg.add(l);
		}
	}

	@Override
	public void removeHDDRecorderChangeListener(HDDRecorderListener l) {
		lsnrs_infochg.remove(l);
	}

	/**
	 *
	 */
	public void addKeywordCancelListener(CancelListener l) {
		if ( ! cancel_listeners.contains(l) ) {
			cancel_listeners.add(l);
		}
	}

	public void removeKeywordCancelChangeListener(CancelListener l) {
		cancel_listeners.remove(l);
	}

	public void updateKeywordComboBox(){
		jComboBox_keyword.removeAllItems();

		jComboBox_keyword.addItem("");

		int num=0;
		int max = env.getMaxSearchWordNuml();
		for (SearchWordItem item : swlist.getWordList()){
			if (max > 0 && num >= max)
				break;

			jComboBox_keyword.addItem(item.getLabel());
			num++;
		}
	}

	/*******************************************************************************
	 * イベントトリガー
	 ******************************************************************************/

	/**
	 * レコーダ選択イベントトリガー
	 */
	private void fireHDDRecorderSelected() {

		HDDRecorderSelectionEvent e = new HDDRecorderSelectionEvent(this);

		if (debug) System.out.println(DBGID+"recorder select rise.");

		for ( HDDRecorderListener l : lsnrs_recsel ) {
			l.valueChanged(e);
		}
	}

	/**
	 * レコーダ状態変更イベントトリガー
	 */
	private void fireHDDRecorderChanged() {

		HDDRecorderChangeEvent e = new HDDRecorderChangeEvent(this);

		if (debug) System.out.println(DBGID+"recorder change rise.");

		for ( HDDRecorderListener l : lsnrs_infochg ) {
			l.stateChanged(e);
		}
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	// キーワード検索ボックスが確定された or キーワード検索ボタンが押された
	private final ActionListener al_keywordEntered = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			toolbarSearch();
		}
	};

	/*
	 *  「キーワード検索メニュー」ボタンが押された
	 */
	private final MouseAdapter ma_searchMenu = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			openSearchDialog();
		}
	};

	// キーワード検索への登録ボタンが押された
	private final ActionListener al_keywordAdded = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			// 「キーワード検索の設定」ウィンドウを開く
			String kStr = jTextField_keyword.getText().trim();

			// 履歴から検索キーを取得する
			int no = jComboBox_keyword.getSelectedIndex();
			SearchKey search = null;
			if (no > 0){
				SearchWordItem swi = swlist.getWordList().get(no-1);

				// 検索キーワードが変更されていたら履歴は無視する
				if (kStr.equals(swi.getLabel())){
//					kStr = swi.getKeyword();
					search = swi.getSearchKey();
					if (search != null)
						new SearchProgram().compile(search);
				}
				else
					no = 0;
			}

			// なければ検索キーワードを解析する
			if (no == 0 || search == null)
				search = decSearchKeyText(kStr);

			if ( search == null )
				return;

			jTextField_keyword.setText("");

			addKeywordSearch(kStr, search);
		}
	};

	// ページャーコンボボックスが選択された
	private final ItemListener il_pagerSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				if (debug) System.out.println(DBGID+"PAGER SELECTED");
				redrawByPager();
				updatePagerTooltipText();
//				jComboBox_pager.setToolTipText(jComboBox_pager.getSelectedItem().toString());
			}
		}
	};

	// 番組表の再取得の実行
	private final MouseListener ml_reloadProgram = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			doLoadTVProgram(null);
		}
	};

	// 番組表の再取得の拡張メニュー
	private final MouseAdapter ma_reloadProgramExtension = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			jPopupMenu_reloadprogmenu.show(jButton_reloadprogs,0,jButton_reloadprogs.getHeight());
		}
	};

	// 番組表の再取得の拡張メニューの個々のアイテムにつけるリスナー
	private final ActionListener al_reloadProgramIndividual = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			doLoadTVProgram(((JMenuItem)e.getSource()).getText());

		}
	};

	// 一括登録の実行
	private final ActionListener al_batchreservation = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			doBatchReserve();
		}
	};

	/**
	 * レコーダコンボボックスが選択された
	 */
	private final ItemListener il_recorderSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {

				// 選択中のレコーダ情報を保存
				setSelectedRecorderInfo(getSelectedRecorder());

				// 各タブへの反映

				// 旧ロジック
				recorderSelectorChanged();

				// 新ロジック
				fireHDDRecorderSelected();
			}

		}
	};

	/**
	 * 選択中のレコーダ情報を保存
	 */
	private void setSelectedRecorderInfo(String myself) {
		selectedMySelf = myself;
		selectedRecorderList = recorders.findInstance(myself);
	}

	// 予約一覧の再取得
	private final ActionListener al_reloadReserved = new ActionListener(){
		public void actionPerformed(ActionEvent e){

			doLoadRdRecorder(null);

			fireHDDRecorderChanged();		// 各タブへの反映
		}
	};

	// 番組表の再取得の拡張メニューの個々のアイテムにつけるリスナー
	private final ActionListener al_reloadReservedIndividual = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			doLoadRdRecorder(((JMenuItem)e.getSource()).getText());

			fireHDDRecorderChanged();		// 各タブへの反映
		}
	};

	// 番組表の再取得の拡張メニュー
	private final MouseAdapter ma_reloadReservedExtension = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			jPopupMenu_reloadrsvedmenu.show(jButton_reloadrsved,0,jButton_reloadrsved.getHeight());
		}
	};

	// レコーダにWOL
	private final ActionListener al_wakeup = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			for (HDDRecorder r : recorders) {
				if ( ! isRecorderSelected(r.Myself())) {
					continue;
				}
				if ( r.getType() != RecType.RECORDER) {
					continue;
				}
				if ( r.getMacAddr().equals("") || r.getBroadcast().equals("")) {
					MWin.appendError(ERRID+"MACアドレスとブロードキャストアドレスを設定してください： "+r.Myself());
					ringBeep();
					continue;
				}
				r.wakeup();
				MWin.appendMessage(MSGID+"wakeupリクエストを送信しました： "+r.Myself());
			}
		}
	};

	// レコーダの電源を落とす
	private final ActionListener al_down = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			for (HDDRecorder r : recorders) {
				if ( ! isRecorderSelected(r.Myself())) {
					continue;
				}
				if ( ! r.getMacAddr().equals("") && ! r.getBroadcast().equals("")) {
					r.shutdown();
					MWin.appendMessage(MSGID+"shutdownリクエストを送信しました： "+r.Myself());
				}
			}
		}
	};

	// ステータスエリアを出したりしまったり
	private final ActionListener al_toggleShowStatus = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			JToggleButton btn = (JToggleButton) e.getSource();
			boolean b = btn.isSelected();

			if (debug) System.out.println(DBGID+"act_toggleShowStatus "+b);

			setStatusVisible(b);

			bounds.setShowStatus(b);
		}
	};

	// フルスクリーンになったりウィンドウになったり
	private final ActionListener al_toggleFullscreen = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			JToggleButton btn = (JToggleButton) e.getSource();
			boolean b = btn.isSelected();

			if (debug) System.out.println(DBGID+"al_toggleFullscreen "+b);

			// ウィンドウ化
			setFullScreen(b);

			// ステータスエリアの表示／非表示
			if ( b ) {
				// フルスクリーン化の際、ステータスエリアは隠す
				statusarea_shown = jToggleButton_showstatus.isSelected();
				if ( jToggleButton_showstatus.isSelected() == true ) jToggleButton_showstatus.doClick();
			}
			else {
				// ウィンドウに戻す際、もともとステータスエリアを開いていた場合だけ戻す
				if ( jToggleButton_showstatus.isSelected() != statusarea_shown ) jToggleButton_showstatus.doClick();
			}
		}
	};

	// 新聞形式に予約待機枠を表示させたりしなかったり
	private final ActionListener al_showborder = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent e){
			toggleMatchBorder(((JToggleButton)e.getSource()).isSelected());
		}
	};

	/*
	 * 実行OFFの予約枠を表示させるかどうか
	 */
	private final ActionListener al_showoffreserve = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent e){
			toggleOffReserve(((JToggleButton)e.getSource()).isSelected());
		}
	};

	// 新聞形式の現在日付までジャンプ
	private final ActionListener al_jumpto = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			jumpToNow();
		}
	};

	// 新聞形式の背景色選択ダイアログを表示する
	private final ActionListener al_showpapercolordialog = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			setPaperColorDialogVisible(true);
		}
	};

	private final MouseAdapter ml_paperzoom = new MouseAdapter() {

		@Override
		public void mouseReleased(MouseEvent e) {
			JSlider sl = (JSlider) e.getSource();
			setPaperZoom(sl.getValue());
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				JSlider sl = (JSlider) e.getSource();
				sl.setValue(100);
				setPaperZoom(sl.getValue());
			}
		}
	};

	// スナップショットをとる
	private final ActionListener al_snapshot = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			takeSnapShot();
		}
	};

	// ログをビューアで開く
	private final ActionListener al_logview = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			LogViewer lv = new LogViewer(Viewer.LOG_FILE);
			lv.setVisible(true);
		}
	};

	//
	private final ActionListener al_update = new ActionListener(){
		public void actionPerformed(ActionEvent e){

			StWin.clear();
			new SwingBackgroundWorker(false) {
				@Override
				protected Object doWorks() throws Exception {
					UpdateResult res = new VWUpdate(StWin).checkUpdate(VersionInfo.getVersion());
					if (res == UpdateResult.DONE) {
						LogViewer lv = new LogViewer(Viewer.HISTORY_FILE);
						lv.setCaretPosition(0);
						lv.setVisible(true);
					}
					return null;
				}
				@Override
				protected void doFinally() {
					CommonUtils.milSleep(OPENING_WIAT);
					StWin.setVisible(false);
				}
			}.execute();

			CommonSwingUtils.setLocationCenter(parent, (Component) StWin);
			StWin.setVisible(true);
		}
	};

	// ヘルプを開く
	private final ActionListener al_openhelp = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			try {
				Desktop desktop = Desktop.getDesktop();
				desktop.browse(new URI(HELP_URL));
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
		}
	};

	// 前のページへ
	private final ActionListener al_prevpage = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			moveToPrevPage();
		}
	};

	// 次のページへ
	private final ActionListener al_nextpage = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			moveToNextPage();
		}
	};

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	// キーワード検索ボックス
	private JComboBoxWithPopup getJComboBox_keyword() {
		if (jComboBox_keyword == null){
			jComboBox_keyword = new JSearchWordComboBox(swlist);
			jComboBox_keyword.setEditable(true);

			Dimension d = jComboBox_keyword.getPreferredSize();
			d.width = bounds.getSearchBoxAreaWidth();

			jComboBox_keyword.setMaximumSize(d);	// 固定しないと環境によってサイズがかわっちゃう
			jComboBox_keyword.setMinimumSize(d);
		}

		if (jTextField_keyword == null) {
			jTextField_keyword = ((JTextField)jComboBox_keyword.getEditor().getEditorComponent());

			jTextField_keyword.setToolTipText(TIPS_KEYWORD);

			jTextField_keyword.addActionListener(al_keywordEntered);

			updateKeywordComboBox();

			InputMap im = jTextField_keyword.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
			ActionMap am = jTextField_keyword.getActionMap();
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ESCKEYACTION);
			am.put(ESCKEYACTION, new AbstractAction(){
				@Override
				public void actionPerformed(ActionEvent e) {
					JTextField jtf = (JTextField) e.getSource();

					CancelEvent ev = new CancelEvent(jtf, CancelEvent.Cause.TOOLBAR_SEARCH);
					for ( CancelListener l : cancel_listeners ) {
						l.cancelRised(ev);
					}
				}
			});
		}

		return jComboBox_keyword;
	}

	// 「検索ボタン」
	private JButton getJButton_search(String s) {
		if (jButton_search == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_SEARCH);
			jButton_search = new JButton(icon);
			jButton_search.setToolTipText(TIPS_SEARCH);

			//jButton_search.addActionListener(al_searchRequested);
			jButton_search.addActionListener(al_keywordEntered);
		}
		return jButton_search;
	}

	// 「検索拡張メニュー」ボタン
	private JButton getJButton_searchmenu(String s) {
		if (jButton_searchmenu == null) {
			ImageIcon arrow = new ImageIcon(ICONFILE_SEARCHMENU);
			jButton_searchmenu = new JButton(arrow);
			jButton_searchmenu.setToolTipText(TIPS_SEARCH_MENU);

			jButton_searchmenu.addMouseListener(ma_searchMenu);
		}
		return jButton_searchmenu;
	}

	// 「キーワード検索に登録」
	private JButton getJButton_addkeyword(String s) {
		if (jButton_addkeyword == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_ADDKEYWORD);
			jButton_addkeyword = new JButton(icon);
			jButton_addkeyword.setToolTipText(TIPS_ADDKEYWORD);

			jButton_addkeyword.addActionListener(al_keywordAdded);
		}
		return jButton_addkeyword;
	}

	// 「ページャー」
	private JComboBox getJComboBox_pager() {
		if (jComboBox_pager == null) {
			jComboBox_pager = new JWideComboBox();
			jComboBox_pager.addPopupWidth(600);
			jComboBox_pager.setToolTipText(TIPS_PAGER);

			Dimension d = jComboBox_pager.getPreferredSize();
			d.width = bounds.getPagerComboWidth();

			jComboBox_pager.setPreferredSize(d);
			jComboBox_pager.setMaximumSize(d);
			jComboBox_pager.setMinimumSize(d);
			jComboBox_pager.setEnabled(false);

			// 選択されたっぽい
			jComboBox_pager.addItemListener(il_pagerSelected);
		}
		return(jComboBox_pager);
	}

	// 「番組情報を再取得」
	private JButton getJButton_reloadprogs(String s) {
		if (jButton_reloadprogs == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_RELOADPROG);
			jButton_reloadprogs = new JButton(icon);
			jButton_reloadprogs.setToolTipText(TIPS_RELOADPROG);

			jButton_reloadprogs.addMouseListener(ml_reloadProgram);
		}
		return jButton_reloadprogs;
	}

	// 「番組情報を再取得」の拡張メニューの並び順
	private final LoadFor[] reloadProgMenu =
		{
			LoadFor.TERRA,
			LoadFor.CS,
			LoadFor.CSo1,
			LoadFor.CSo2,
			null,
			LoadFor.CSwSD,
			null,
			LoadFor.SYOBO
		};

	// 「番組情報を再取得」の拡張メニュー
	private JButton getJButton_reloadprogmenu(String s) {
		if (jButton_reloadprogmenu == null) {
			// メニューの作成
			jPopupMenu_reloadprogmenu = new JPopupMenu();

			// アイテムの登録
			updateReloadProgramExtension();

			ImageIcon arrow = new ImageIcon(ICONFILE_PULLDOWNMENU);
			jButton_reloadprogmenu = new JButton(arrow);
			jButton_reloadprogmenu.addMouseListener(ma_reloadProgramExtension);
		}
		return jButton_reloadprogmenu;
	}

	// 「一括予約」
	private JButton getJButton_batchreservation(String s) {
		if (jButton_batchreservation == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_BATCHCMD);
			jButton_batchreservation = new JButton(icon);
			jButton_batchreservation.setToolTipText(TIPS_BATCHRESERVATION);

			jButton_batchreservation.addActionListener(al_batchreservation);
		}
		return jButton_batchreservation;
	}

	//
	private JComboBox getJComboBox_select_recorder() {
		if (jComboBox_select_recorder == null) {
			jComboBox_select_recorder = new JWideComboBox();
			jComboBox_select_recorder.addPopupWidth(200);
			jComboBox_select_recorder.setToolTipText(TIPS_RECORDERSEL);

			Dimension d = jComboBox_select_recorder.getPreferredSize();
			d.width = bounds.getRecorderComboWidth();
			jComboBox_select_recorder.setPreferredSize(d);
			jComboBox_select_recorder.setMaximumSize(d);
			jComboBox_select_recorder.setMinimumSize(d);
			//jComboBox_select_recorder.setEnabled(false);

			// 初期値（ItemListenerは↓の中で追加される）
			updateRecorderComboBox();
		}
		return(jComboBox_select_recorder);
	}

	// 「予約一覧の再取得」
	private JButton getJButton_reloadrsved(String s) {
		if (jButton_reloadrsved == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_RELOADRSV);
			jButton_reloadrsved = new JButton(icon);
			jButton_reloadrsved.setToolTipText(TIPS_RELOADRSVED);

			jButton_reloadrsved.addActionListener(al_reloadReserved);
		}
		return jButton_reloadrsved;
	}

	// 「番組情報を再取得」の拡張メニュー
	private JButton getJButton_reloadrsvedmenu(String s) {
		if (jButton_reloadrsvedmenu == null) {
			// メニューの作成
			jPopupMenu_reloadrsvedmenu = new JPopupMenu();

			// アイテムの登録
			updateReloadReservedExtension();

			ImageIcon arrow = new ImageIcon(ICONFILE_PULLDOWNMENU);
			jButton_reloadrsvedmenu = new JButton(arrow);
			jButton_reloadrsvedmenu.addMouseListener(ma_reloadReservedExtension);
		}
		return jButton_reloadrsvedmenu;
	}

	// 「入」
	private JButton getJButton_wakeup(String s) {
		if (jButton_wakeup == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_WAKEUP);
			jButton_wakeup = new JButton(icon);
			jButton_wakeup.setToolTipText(TIPS_WAKEUP);

			jButton_wakeup.addActionListener(al_wakeup);
		}
		return jButton_wakeup;
	}
	// 「切」
	private JButton getJButton_shutdown(String s) {
		if (jButton_shutdown == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_SHUTDOWN);
			jButton_shutdown = new JButton(icon);
			jButton_shutdown.setToolTipText(TIPS_DOWN);

			jButton_shutdown.addActionListener(al_down);
		}
		return jButton_shutdown;
	}

	// 「ステータス領域」
	private JToggleButton getJToggleButton_showstatus(String s) {
		if (jToggleButton_showstatus == null) {

			final ImageIcon IconHidden = new ImageIcon(ICONFILE_STATUSHIDDEN);
			final ImageIcon IconShown = new ImageIcon(ICONFILE_STATUSSHOWN);

			jToggleButton_showstatus = new JToggleButton(IconHidden) {

				private static final long serialVersionUID = 1L;

				@Override
				public void setSelected(boolean b) {
					super.setSelected(b);
					if ( b ) {
						this.setToolTipText(TIPS_STATUSSHOWN);
					}
					else {
						this.setToolTipText(TIPS_STATUSHIDDEN);
					}
				}
			};
			jToggleButton_showstatus.setSelectedIcon(IconShown);
			jToggleButton_showstatus.setSelected(bounds.getShowStatus());
			jToggleButton_showstatus.addActionListener(al_toggleShowStatus);
		}
		return jToggleButton_showstatus;
	}

	// 「全画面」
	private JToggleButton getJToggleButton_fullScreen(String s) {
		if (jToggleButton_fullScreen == null) {

			final ImageIcon IconToWin = new ImageIcon(ICONFILE_TOWIN);
			final ImageIcon IconToFull = new ImageIcon(ICONFILE_TOFULL);

			jToggleButton_fullScreen = new JToggleButton(IconToFull) {

				private static final long serialVersionUID = 1L;

				@Override
				public void setSelected(boolean b) {
					super.setSelected(b);
					if (b) {
						this.setToolTipText(TIPS_TOWIN);
					}
					else {
						this.setToolTipText(TIPS_TOFULL);
					}
				}
			};
			jToggleButton_fullScreen.setSelectedIcon(IconToWin);
			jToggleButton_fullScreen.setSelected(false);
			jToggleButton_fullScreen.addActionListener(al_toggleFullscreen);
		}
		return jToggleButton_fullScreen;
	}

	/**
	 * 予約背景色・検索マッチ枠の表示／非表示
 	 */
	private JToggleButton getJToggleButton_showmatchborder(String s) {
		if (jToggleButton_showmatchborder == null) {
			final ImageIcon icon = new ImageIcon(ICONFILE_SHOWMATCHBORDER);
			jToggleButton_showmatchborder = new JToggleButton(icon);
			jToggleButton_showmatchborder.setToolTipText(TIPS_SHOWBORDER);
			jToggleButton_showmatchborder.setSelected(bounds.getShowMatchedBorder());

			jToggleButton_showmatchborder.addActionListener(al_showborder);
		}
		return jToggleButton_showmatchborder;
	}

	/**
	 * 実行OFFの予約枠の表示／非表示
 	 */
	private JToggleButton getJToggleButton_showoffreserve(String s) {
		if (jToggleButton_showoffreserve == null) {
			final ImageIcon icon = new ImageIcon(ICONFILE_SHOWOFFRESERVE);
			jToggleButton_showoffreserve = new JToggleButton(icon);
			jToggleButton_showoffreserve.setToolTipText(TIPS_SHOWOFFRESERVE);
			jToggleButton_showoffreserve.setSelected(bounds.getShowOffReserve());

			jToggleButton_showoffreserve.addActionListener(al_showoffreserve);
		}
		return jToggleButton_showoffreserve;
	}

	// 「現在日時」
	private JButton getJButton_moveToNow(String s) {
		if (jButton_moveToNow == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_JUMPTONOW);
			jButton_moveToNow = new JButton(icon);
			jButton_moveToNow.setToolTipText(TIPS_JUMPTO);

			jButton_moveToNow.addActionListener(al_jumpto);
		}
		return jButton_moveToNow;
	}

	// 「現在日時」
	private JButton getJButton_paperColors(String s) {
		if (jButton_paperColors == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_PALLET);
			jButton_paperColors = new JButton(icon);
			jButton_paperColors.setToolTipText(TIPS_PAPERCOLOR);

			jButton_paperColors.addActionListener(al_showpapercolordialog);
		}
		return jButton_paperColors;
	}

	private JSlider getJSlider_paperZoom(String s) {
		if ( jSlider_paperZoom == null ) {
			jSlider_paperZoom = new JSlider(50,300,100);
			jSlider_paperZoom.setToolTipText(TIPS_PAPERZOOM);

			Dimension d = jSlider_paperZoom.getPreferredSize();
			d.width = 45;
			jSlider_paperZoom.setPreferredSize(d);
			jSlider_paperZoom.setMaximumSize(d);
			jSlider_paperZoom.setMinimumSize(d);

			jSlider_paperZoom.addMouseListener(ml_paperzoom);
		}
		return jSlider_paperZoom;
	}
	/*
	// 「タイマー」
	private String nextEventDateTime = "29991231 2359";
	private void setNextEventDateTime() {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date());
		c.add(Calendar.HOUR_OF_DAY, env.getCacheTimeLimit());
		nextEventDateTime = String.format("%04d%02d%02d %02d%02d", c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),00);
		StdAppendMessage("Next Event Time: "+nextEventDateTime);
	}
	private ActionListener alTimer = new ActionListener() {
		public void actionPerformed(ActionEvent e){
			GregorianCalendar c = new GregorianCalendar();
			c.setTime(new Date());
			String curDateTime = String.format("%04d%02d%02d %02d%02d", c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE));
			if (nextEventDateTime.compareTo(curDateTime) <= 0) {
				funcReloadProgs(LoadFor.ALL);
				setNextEventDateTime();
			}
		}
	};
	private Timer tbTimer = new Timer(60*1000, alTimer);
	private void tbTimerOn() {
		String tip = "タイマーOFF";
		jToggleButton_timer.setToolTipText(tip);
		setNextEventDateTime();
		tbTimer.start();
		bounds.setEnableTimer(true);
	}
	private void tbTimerOff() {
		String tip = "タイマーON";
		jToggleButton_timer.setToolTipText(tip);
		tbTimer.stop();
		bounds.setEnableTimer(false);
	}
	*/
	private JToggleButton getJToggleButton_timer(String s) {
		if (jToggleButton_timer == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_TIMER);
			String tip = "タイマーON";
			jToggleButton_timer = new JToggleButton(icon);
			jToggleButton_timer.setToolTipText(tip);

			jToggleButton_timer.setEnabled(false);
			jToggleButton_timer.setToolTipText("future use");

			/*
			if (bounds.getEnableTimer()) {
				jToggleButton_timer.setSelected(true);
				tbTimerOn();
			}

			jToggleButton_timer.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					ButtonModel bm = jToggleButton_timer.getModel();
					if (bm.isPressed() && bm.isSelected()) {
						tbTimerOn();
					}
					else if (bm.isPressed() && ! bm.isSelected()) {
						tbTimerOff();
					}
				}
			});
			*/
		}
		return jToggleButton_timer;
	}

	// 「スナップショット」
	private JButton getJButton_snapshot(String s) {
		if (jButton_snapshot == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_SCREENSHOT);
			jButton_snapshot = new JButton(icon);
			jButton_snapshot.setToolTipText(TIPS_SNAPSHOT);

			jButton_snapshot.addActionListener(al_snapshot);
		}
		return jButton_snapshot;
	}

	// 「ログビューア」
	private JButton getJButton_logviewer(String s) {
		if (jButton_logviewer == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_SHOWLOG);
			jButton_logviewer = new JButton(icon);
			jButton_logviewer.setToolTipText(TIPS_LOGVIEW);

			jButton_logviewer.addActionListener(al_logview);
		}
		return jButton_logviewer;
	}

	// 「オンラインアップデート」
	private JButton getJButton_update(String s) {
		if (jButton_update == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_UPDATE);
			jButton_update = new JButton(icon);
			jButton_update.setToolTipText(TIPS_UPDATE);

			jButton_update.addActionListener(al_update);
		}
		return jButton_update;
	}

	// 「ヘルプ」
	private JButton getJButton_help(String s) {
		if (jButton_help == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_HELP);
			jButton_help = new JButton(icon);
			jButton_help.setToolTipText(TIPS_OPENHELP);

			jButton_help.addActionListener(al_openhelp);
		}
		return jButton_help;
	}

	// 「前のページへ」
	private JButton getJButton_prevpage() {
		if (jButton_prevpage == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_PREVPAGE);
			jButton_prevpage = new JButton(icon);
			jButton_prevpage.setToolTipText(TIPS_PREVPAGE);
			jButton_prevpage.setEnabled(false);

			jButton_prevpage.addActionListener(al_prevpage);
		}
		return jButton_prevpage;
	}

	// 「次のページへ」
	private JButton getJButton_nextpage() {
		if (jButton_nextpage == null) {
			ImageIcon icon = new ImageIcon(ICONFILE_NEXTPAGE);
			jButton_nextpage = new JButton(icon);
			jButton_nextpage.setToolTipText(TIPS_NEXTPAGE);
			jButton_nextpage.setEnabled(false);

			jButton_nextpage.addActionListener(al_nextpage);
		}
		return jButton_nextpage;
	}

}
