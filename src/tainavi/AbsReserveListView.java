package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterEvent.Type;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import tainavi.VWMainWindow.MWinTab;


/**
 * 本体予約一覧タブのクラス
 * @since 3.15.4β　{@link Viewer}から分離
 */
public abstract class AbsReserveListView extends JPanel implements TickTimerListener {

	private static final long serialVersionUID = 1L;

	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();
	protected abstract ReserveListColumnInfoList getRlItemEnv();
	protected abstract ChannelSort getChannelSort();

	protected abstract TVProgramList getTVProgramList();
	protected abstract HDDRecorderList getRecorderList();

	//protected abstract StatusWindow getStWin();
	//protected abstract StatusTextArea getMWin();
	protected abstract AbsReserveDialog getReserveDialog();

	protected abstract Component getParentComponent();

	protected abstract void ringBeep();

	/**
	 * 予約マーク・予約枠を更新してほしい
	 */
	protected abstract void updateReserveDisplay(String chname);

	/**
	 * 予約実行を更新してほしい
	 */
	protected abstract boolean doExecOnOff(boolean fexec, String title, String chnam, String rsvId, String recId);

	/**
	 *  予約実行をONOFFするメニューアイテム
	 */
	protected abstract JMenuItem getExecOnOffMenuItem(final boolean fexec, final String start, final String title, final String chnam, final String rsvId, final String recId);

	/**
	 *  予約を削除するメニューアイテム
	 */
	protected abstract JMenuItem getRemoveRsvMenuItem(final String start, final String title, final String chnam, final String rsvId, final String recId);

	/**
	 *  新聞形式へジャンプするメニューアイテム
	 */
	protected abstract JMenuItem getJumpMenuItem(final String title, final String chnam, final String startDT);
	protected abstract JMenuItem getJumpToLastWeekMenuItem(final String title, final String chnam, final String startDT);

	/*
	 * プログラムのブラウザーメニューを呼び出す
	 */
	protected abstract void addBrowseMenuToPopup( JPopupMenu pop,	final ProgDetailList tvd );

	/**
	 * @see Viewer.VWToolBar#getSelectedRecorder()
	 */
	protected abstract String getSelectedRecorderOnToolbar();

	protected abstract boolean isTabSelected(MWinTab tab);

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String MSGID = "[本体予約一覧] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	private static final String DUPMARK_NORMAL = "■";
	private static final String DUPMARK_REP = "□";
	private static final String DUPMARK_EXCEED = "★";
	private static final String DUPMARK_COLOR = "#FFB6C1";
	private static final String DUPMARK_EXCOLOR = "#FF0000";

	private static final String VALID_TIME_COLOR = "#000000";
	private static final String INVALID_TIME_COLOR = "#ff0000";

	private static final String VALID_CHNAME_COLOR = "#0000ff";
	private static final String INVALID_CHNAME_COLOR = "#ff0000";

	private static final String VALID_DEVNAME_COLOR = "#000000";
	private static final String INVALID_DEVNAME_COLOR = "#ff0000";

	private static final String PASSED_COLOR = "#b4b4b4";
	private static final String CURRENT_COLOR_EVEN = "#f0b4b4";
	private static final String CURRENT_COLOR_ODD = "#f88080";

	private static final String ICONFILE_EXEC			= "icon/media-record-3.png";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// オブジェクト
	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();
	private final ChannelSort chsort = getChannelSort();
	private final HDDRecorderList recorders = getRecorderList();
	private final TVProgramList tvprograms = getTVProgramList();

	//private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	//private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final AbsReserveDialog rD = getReserveDialog();	// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	// メソッド
	//private void StdAppendMessage(String message) { System.out.println(message); }
	//private void StdAppendError(String message) { System.err.println(message); }
	//private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	//private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }

	private final ImageIcon execicon = new ImageIcon(ICONFILE_EXEC);

	private ReserveListColumnInfoList rlitems = null;

	private int dividerLocationOnShown = 0;

	/**
	 * カラム定義
	 */

	public static HashMap<String,Integer> getColumnIniWidthMap() {
		if (rcmap.size() == 0 ) {
			for ( RsvedColumn rc : RsvedColumn.values() ) {
				rcmap.put(rc.toString(),rc.getIniWidth());	// toString()!
			}
		}
		return rcmap;
	}

	private static final HashMap<String,Integer> rcmap = new HashMap<String, Integer>();

	public static enum RsvedColumn {
		PATTERN		("パタン",			110),
		DUPMARK		("重複",			35),
		EXEC		("実行",			35),
		TRACE		("追跡",			35),
		AUTO		("自動",			35),
		OPTIONS		("オプション",		100),
		NEXTSTART	("次回実行予定",	150),
		END			("終了",			50),
		LENGTH		("長さ",			50),
		ENCODER		("ｴﾝｺｰﾀﾞ",		50),
		VRATE		("画質",			100),
		ARATE		("音質",			50),
		TITLE		("番組タイトル",	300),
		CHNAME		("チャンネル名",	150),
		DEVNAME		("デバイス",		100),
		FOLDER		("フォルダ",		300),
		RECORDER	("レコーダ",		200),

		/*
		HID_INDEX	("INDEX",		-1),
		HID_RSVID	("RSVID",		-1),
		*/
		;

		private String name;
		private int iniWidth;

		private RsvedColumn(String name, int iniWidth) {
			this.name = name;
			this.iniWidth = iniWidth;
		}

		@Override
		public String toString() {
			return name;
		}

		public int getIniWidth() {
			return iniWidth;
		}

		public int getColumn() {
			return ordinal();
		}
	};


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private class ReservedItem extends RowItem implements Cloneable {

		String pattern;
		String dupmark;
		Boolean exec;
		String trace;
		String auto;
		String options;
		String nextstart;	// YYYY/MM/DD(WD) hh:mm
		String end;			// hh:mm
		String length;
		String encoder;
		String vrate;
		String arate;
		String title;
		String chname;
		String devname;
		String folder;
		String recname;
		String recorder;

		String hide_chname;
		String hide_rsvid;
		String hide_nextstartcolor;
		String hide_endcolor;
		String hide_lengthcolor;
		String hide_centercolor;
		String hide_encodercolor;
		String hide_itecolor;
		String hide_devicecolor;
		Boolean hide_tunershort;
		Boolean hide_recorded;
		String hide_nextstart;
		String hide_nextend;

		@Override
		protected void myrefresh(RowItem o) {
			ReservedItem c = (ReservedItem) o;

			c.addData(pattern);
			c.addData(dupmark);
			c.addData(exec);
			c.addData(trace);
			c.addData(auto);
			c.addData(options);
			c.addData(nextstart);
			c.addData(end);
			c.addData(length);
			c.addData(encoder);
			c.addData(vrate);
			c.addData(arate);
			c.addData(title);
			c.addData(chname);
			c.addData(devname);
			c.addData(folder);
			c.addData(recname);
			c.addData(recorder);
		}

		public ReservedItem clone() {
			return (ReservedItem) super.clone();
		}
	}

	// ソートが必要な場合はTableModelを作る。ただし、その場合Viewのrowがわからないので行の入れ替えが行えない
	private class ReservedTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		private RowItemList<ReservedItem> rDat;

		@Override
		public Object getValueAt(int row, int column) {
			ReservedItem c = rDat.get(row);
			ListColumnInfo info = rlitems.getVisibleAt(column);
			if (info == null)
				return null;

			int cindex = info.getId()-1;
			if ( cindex == RsvedColumn.DUPMARK.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.dupmark.equals(DUPMARK_EXCEED) ? DUPMARK_EXCOLOR : DUPMARK_COLOR,c.dupmark);
			}
			else if ( cindex == RsvedColumn.ENCODER.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.hide_encodercolor,c.encoder);
			}
			else if ( cindex == RsvedColumn.TITLE.getColumn() ) {
				if ( c.hide_itecolor!=null ) {
					return CommonSwingUtils.getColoredString(c.hide_itecolor,c.title);
				}
				else {
					return c.title;
				}
			}
			else if ( cindex == RsvedColumn.NEXTSTART.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.hide_nextstartcolor,c.nextstart);
			}
			else if ( cindex == RsvedColumn.END.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.hide_endcolor,c.end);
			}
			else if ( cindex == RsvedColumn.LENGTH.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.hide_lengthcolor,c.length+"m");
			}
			else if ( cindex == RsvedColumn.CHNAME.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.hide_centercolor,c.chname);
			}
			else if ( cindex == RsvedColumn.LENGTH.getColumn() ) {
				return c.length+"m";
			}
			else if ( cindex == RsvedColumn.DEVNAME.getColumn() ) {
				return CommonSwingUtils.getColoredString(c.hide_devicecolor,c.devname);
			}

			if (cindex < c.size()){
				return c.get(cindex);
			}

			return null;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			/*
			ReservedItem c = rowView.get(row);
			if ( column == RsvedColumn.EXEC.getColumn() ) {
				//c.exec = (Boolean) aValue;
				//c.fireChanged();
			}
			*/
		}

		@Override
		public int getRowCount() {
			return (rDat!=null) ? rDat.size() : 0;	// ↓ のsuper()で呼ばれるのでnullチェックが必要
		}

		public ReservedItem getRowItem(int row) { return rDat.get(row); }

		public ReservedTableModel(String[] colname, int i, RowItemList<ReservedItem> rowdata) {
			super(colname,i);
			this.rDat = rowdata;
		}

		private boolean filtered = false;

		public boolean getFiltered() { return filtered; }

		@Deprecated
		public void fireTableDataChanged() {
			throw new NullPointerException();
		}

		public void fireTableDataChanged(boolean filtered) {
			super.fireTableDataChanged();
			this.filtered = filtered;
		}
	}

	//private final ReservedItem sa = new ReservedItem();

	private JSplitPane jSplitPane_main = null;
	private JDetailPanel jTextPane_detail = null;
	private JScrollPane jScrollPane_view = null;

	private JNETableReserved jTable_rsved = null;
	private JTable jTable_rowheader = null;

	private ReservedTableModel tableModel_rsved = null;

	private DefaultTableModel rowheaderModel_rsved = null;

	// 表示用のテーブル
	private final RowItemList<ReservedItem> rowViewTemp = new RowItemList<ReservedItem>();

	// テーブルの実体
	private final RowItemList<ReservedItem> rowData = new RowItemList<ReservedItem>();

	// 現在放送中のタイマー
	private boolean timer_now_enabled = false;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsReserveListView() {

		super();

		this.rlitems = getRlItemEnv();

		// コンポーネントを追加
		this.setLayout(new BorderLayout());
		this.add(getJSplitPane_main(), BorderLayout.CENTER);

		// バグ対応
		if ( bounds.getRsvedColumnSize() == null ) {
			System.err.println(ERRID+"なんらかの不具合によりテーブルのカラム幅設定が取得できませんでした。設定はリセットされました。申し訳ありません。");
			bounds.setRsvedColumnSize(rcmap);
		}
		else {
			for ( Entry<String, Integer> en : rcmap.entrySet() ) {
				try {
					bounds.getRsvedColumnSize().get(en.getKey());
				}
				catch (NullPointerException e) {
					System.err.println(ERRID+en.getKey()+", "+e.toString());
					bounds.getRsvedColumnSize().put(en.getKey(),en.getValue());
				}
			}
		}

		//
		this.addComponentListener(cl_tabshown);
	}


	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 対外的な

	/**
	 * 現在時刻追従を開始する
	 * @see #stopTimer
	 * @see #pauseTimer
	 */
	private void startTimer() {
		timer_now_enabled = true;
	}

	/**
	 * 現在時刻追従を停止する
	 */
	private boolean stopTimer(boolean showmsg) {
		return (timer_now_enabled = false);
	}

	/**
	 * 予約一覧を描画してほしいかなって
	 * ★synchronized(rowData)★
	 * @see #cl_tabshown
	 */
	public void redrawReservedList() {
		// ★★★　イベントにトリガーされた処理がかちあわないように synchronized()　★★★
		synchronized ( rowViewTemp ) {
			_redrawReservedList();
		}
	}

	private void _redrawReservedList() {
		stopTimer(true);

		//
		rowData.clear();

		// 選択されたレコーダ
		String myself = getSelectedRecorderOnToolbar();
		HDDRecorderList recs = recorders.findInstance(myself);

		// 現在日時
		String curDateTime = CommonUtils.getDateTime(0);
		String critDateTime = CommonUtils.getCritDateTime();

		// 繰り返し予約関連
		String itecolor = CommonUtils.color2str(env.getIterationItemForeground());
		int maxn = ( env.getShowAllIterationItem() ) ? (env.getDogDays()) : (1);

		// 背景色
		jTable_rsved.setTunerShortColor(env.getTunerShortColor());
		jTable_rsved.setRecordedColor(env.getRecordedColor());

		TVProgramIterator pli = tvprograms.getIterator().build(chsort.getClst(), TVProgramIterator.IterationType.ALL);

		for ( HDDRecorder recorder : recs )
		{
			if ( recorder.isBackgroundOnly() ) {
				continue;
			}

			// 終了した番組があれば整理
			recorder.removePassedReserves();

			// 並べ替えるために新しいリストを作成する
			for ( ReserveList ro : recorder.getReserves() ) {
				ArrayList<String> starts = new ArrayList<String>();
				ArrayList<String> ends = new ArrayList<String>();
				CommonUtils.getStartEndList(starts, ends, ro);
				for ( int n=0; n<starts.size() && n<maxn; n++ ) {
					if ( ! env.getDisplayPassedReserve() ) {
						if ( ends.get(n).compareTo(curDateTime) < 0 ) {
							continue;
						}
					}

					// 現在時刻と開始時刻、終了時刻の前後関係をチェックする
					long cur_remain = -1;
					long cur_wait = 0;

					String cridt = CommonUtils.getDateTime(-env.getCurrentAfter());
					String curdt = CommonUtils.getDateTime(0);
					String nextdt = CommonUtils.getDateTime(env.getCurrentBefore());

					// 開始時刻、終了時刻
					String nextstart = CommonUtils.getDate(CommonUtils.getCalendar(starts.get(n)), false)+" "+ro.getAhh()+":"+ro.getAmm();
					String nextend =  CommonUtils.getDate(CommonUtils.getCalendar(ends.get(n)), false)+" "+ro.getZhh()+":"+ro.getZmm();

					// オプションに文字列としてセットする
					String options = "";

					if (ro.getExec()){
						// 予約実行中の場合
						if ( nextstart.compareTo(cridt) <= 0 && cridt.compareTo(nextend) <= 0 ||
							 nextstart.compareTo(curdt) <= 0 && curdt.compareTo(nextend) <= 0) {
							cur_remain = CommonUtils.getCompareDateTime(nextend, curdt);
						}
						// 予約開始まで規定時間以内の場合
						else if ( nextstart.compareTo(cridt) > 0 && nextstart.compareTo(nextdt) <= 0 ) {
							cur_wait = CommonUtils.getCompareDateTime(nextstart, curdt);
						}

						if ( cur_remain > 0 ) {
							options = String.format("終了まで%3d分",cur_remain/60000);
						}
						else if ( cur_wait > 0 ){
							options = String.format("開始まで%3d分",cur_wait/60000);
						}
					}

					ReservedItem sa = new ReservedItem();

					sa.pattern = ro.getRec_pattern();
					sa.dupmark = "";
					sa.exec = ro.getExec();
					sa.trace = ((ro.getPursues())?("追"):(""));
					sa.auto = ((ro.getAutoreserved())?("○"):(""));
					sa.options = options;
					sa.nextstart = CommonUtils.getDate(CommonUtils.getCalendar(starts.get(n)))+" "+ro.getAhh()+":"+ro.getAmm();	// YYYY/MM/DD(WD) hh:mm
					sa.end = ro.getZhh()+":"+ro.getZmm();			// hh:mm
					sa.length = ro.getRec_min();
					sa.encoder = (ro.getTuner() != null)?(ro.getTuner()):("★エンコーダ不正");
					sa.vrate = getRec_mode(ro);
					sa.arate = ro.getRec_audio();
					sa.title = ro.getTitle();
					sa.chname = (ro.getCh_name()!=null && ro.getCh_name().length()>0)?(ro.getCh_name()):("★放送局名不正("+ro.getChannel()+")");
					sa.devname = ro.getRec_device();
					sa.folder = ro.getRec_folder();
					sa.recname = recorder.getDispName();
					sa.recorder = recorder.Myself();

					sa.hide_chname = (ro.getCh_name()!=null)?(ro.getCh_name()):("");
					sa.hide_rsvid = ro.getId();
					sa.hide_centercolor = (ro.getCh_name()!=null && ro.getCh_name().length()>0)?(VALID_CHNAME_COLOR):(INVALID_CHNAME_COLOR);
					sa.hide_encodercolor = recorder.getColor(ro.getTuner());
					sa.hide_itecolor = (n>0)?(itecolor):(null);
					sa.hide_devicecolor = VALID_DEVNAME_COLOR;
					sa.hide_tunershort = ro.getTunershort();
					sa.hide_recorded = ro.getRecorded();
					sa.hide_nextstart = nextstart;
					sa.hide_nextend = nextend;

					int crc = checkReserveList(pli, ro,
							CommonUtils.getDate529(CommonUtils.getCalendar(starts.get(n)), true), nextstart, nextend);
					sa.hide_nextstartcolor = (crc & 0x01) != 0 ? INVALID_TIME_COLOR : VALID_TIME_COLOR;
					sa.hide_endcolor = (crc & 0x02) != 0 ? INVALID_TIME_COLOR : VALID_TIME_COLOR;
					sa.hide_lengthcolor = (crc & 0x04) != 0 ? INVALID_TIME_COLOR : VALID_TIME_COLOR;

					sa.fireChanged();

					addRow(sa);
				}
			}
		}

		// 表示用
		rowViewTemp.clear();
		for ( ReservedItem a : rowData ) {
			rowViewTemp.add(a);
		}

		tableModel_rsved.fireTableDataChanged(false);
		((DefaultTableModel)jTable_rowheader.getModel()).fireTableDataChanged();

		setOverlapMark();

		startTimer();
	}

	/*
	 * 予約のインスタンスと時間が重なる番組情報を取得する
	 */
	private ProgDetailList getProgDetailForReserve(TVProgramIterator pli, ReserveList r, String date, String start, String end){
		pli.rewind();

		// 番組情報についてループする
		for ( ProgList pl : pli ) {
			// チャンネルが異なる場合はスキップする
			if (! pl.Center.equals(r.getCh_name()))
				continue;

			// 日付についてループする
			for (ProgDateList pdl : pl.pdate){
				// 日付が異なる場合はスキップする
				if (! pdl.Date.equals(date))
					continue;

				long msecsLike = 0;
				ProgDetailList tvdLike = null;

				// 日付内の番組についてループする
				for (ProgDetailList tvd : pdl.pdetail){
					int bse = tvd.startDateTime.compareTo(end);
					int bes = tvd.endDateTime.compareTo(start);

					// 予約情報と時間が重なる場合はその番組情報を返す
					if (bse * bes < 0){
						long msecs = calcIntersectMillisecs(tvd.startDateTime, tvd.endDateTime, start, end);
						if (msecs > msecsLike ){
							tvdLike = tvd;
							msecsLike = msecs;
						}
					}
				}

				return tvdLike;
			}

			break;
		}

		return null;
	}

	/*
	 * ２つの時間枠の重なり部分をミリ秒単位で取得する
	 */
	static private long calcIntersectMillisecs(String start1, String end1, String start2, String end2){
		if (start1 == null || end1 == null || start2 == null || end2 == null)
			return -1;

		GregorianCalendar cs1 = CommonUtils.getCalendar(start1);
		GregorianCalendar ce1 = CommonUtils.getCalendar(end1);
		GregorianCalendar cs2 = CommonUtils.getCalendar(start2);
		GregorianCalendar ce2 = CommonUtils.getCalendar(end2);
		if (cs1 == null || ce1 == null || cs2 == null || ce2 == null)
			return -1;

		return Long.min(ce1.getTimeInMillis(),  ce2.getTimeInMillis()) - Long.max(cs1.getTimeInMillis(),  cs2.getTimeInMillis());
	}

	/*
	 * 予約の開始日時、終了時刻、長さをチェックする
	 *
	 * @return 0x01=開始日時が異なる、0x02=終了時刻が異なる、0x04=長さが異なる
	 */
	private int checkReserveList(TVProgramIterator pli, ReserveList r, String date, String start, String end){
		// 予約を実行しない場合はチェックしない
		if (!r.getExec())
			return 0x00;

		int flags = 0x07;

		ProgDetailList tvd = getProgDetailForReserve(pli, r, date, start, end);
		if (tvd != null){
			flags = 0;
			int bss = tvd.startDateTime.compareTo(start);
			int bee = tvd.endDateTime.compareTo(end);

			// 開始時刻、終了時刻、長さをチェックする
			if (bss != 0){
				flags |= 0x01;
			}
			if (bee != 0){
				flags |= 0x02;
			}
			if (bee != bss){
				flags |= 0x04;
			}
		}

		return flags;
	}

	private String getRec_mode(ReserveList reserve) { String s = ((reserve.getAppsRsv())?(reserve.getRec_mvchapter()):(reserve.getRec_mode())); return (s==null)?(""):(s); }

	/**
	 * 絞り込み検索の本体（現在リストアップされているものから絞り込みを行う）（親から呼ばれるよ！）
	 */
	public void redrawListByKeywordFilter(SearchKey keyword, String target) {

		// 情報を一行ずつチェックする
		if ( keyword != null ) {

			rowViewTemp.clear();

			for ( ReservedItem a : rowData ) {

				ProgDetailList tvd = new ProgDetailList();

				// タイトルを整形しなおす
				tvd.title = a.title;
				tvd.titlePop = TraceProgram.replacePop(tvd.title);

				// 放送局
				tvd.center = a.chname != null ? a.chname : "";

				// 番組長
				try{
					tvd.length = Integer.parseInt(a.length);
				}
				catch(Exception e){}

				boolean isFind = SearchProgram.isMatchKeyword(keyword, tvd.center, tvd);

				if ( isFind ) {
					rowViewTemp.add(a);
				}
			}

			// fire!
			tableModel_rsved.fireTableDataChanged(true);
			rowheaderModel_rsved.fireTableDataChanged();
		}
		else {
			if ( ! tableModel_rsved.getFiltered() ) {
				return;
			}

			rowViewTemp.clear();

			for ( ReservedItem a : rowData ) {
				rowViewTemp.add(a);
			}

			// fire!
			tableModel_rsved.fireTableDataChanged(false);
			rowheaderModel_rsved.fireTableDataChanged();
		}
	}

	/**
	 * カラム幅を保存する（鯛ナビ終了時に呼び出されるメソッド）
	 */
	public void copyColumnWidth() {
		for ( RsvedColumn rc : RsvedColumn.values() ) {
			if ( rc.getIniWidth() < 0 ) {
				continue;
			}
			TableColumn column = getColumn(rc);
			if (column == null)
				continue;

			bounds.getRsvedColumnSize().put(rc.toString(), column.getPreferredWidth());
		}
	}

	/**
	 * テーブルの行番号の表示のＯＮ／ＯＦＦ
	 */
	public void setRowHeaderVisible(boolean b) {
		jScrollPane_view.getRowHeader().setVisible(b);
	}

	/**
	 * 画面上部の番組詳細領域の表示のＯＮ／ＯＦＦ
	 */
	public void setDetailVisible(boolean aFlag) {
		jTextPane_detail.setVisible(aFlag);

		if (aFlag){
			int height = bounds.getReserveDetailHeight();

			if (! isTabSelected(MWinTab.RSVED))
				dividerLocationOnShown = height;
			else
				jSplitPane_main.setDividerLocation(height);

			bounds.setReserveDetailHeight(height);
		}
	}

	/**
	 * 通知すべき予約情報を取得する
	 */
	public boolean getReservesToNotify(ArrayList<String>list) {
		list.clear();

		String os = System.getProperty("os.name");
		boolean isWin10 = os != null && os.contains("Windows 10");

		// 現在日時
		int min = env.getMinsBeforeProgStart();
		String dtmin = CommonUtils.getDateTime(min*60);
		String dtmax = CommonUtils.getDateTime((min+1)*60);

		// 選択されたレコーダ
		HDDRecorderList recs = recorders.findInstance(getSelectedRecorderOnToolbar());

		String bodyAll = "";
		String head = "" + min + "分後に予約した番組が始まります\n";

		for ( HDDRecorder recorder : recs )	{
			if ( recorder.isBackgroundOnly() ) {
				continue;
			}

			// 並べ替えるために新しいリストを作成する
			for ( ReserveList ro : recorder.getReserves() ) {
				if (!ro.getExec())
					continue;

				ArrayList<String> starts = new ArrayList<String>();
				ArrayList<String> ends = new ArrayList<String>();
				CommonUtils.getStartEndList(starts, ends, ro);
				if (starts.size() == 0)
					continue;

				// 開始時刻、終了時刻
				String nextstart = CommonUtils.getDate(CommonUtils.getCalendar(starts.get(0)), false)+" "+ro.getAhh()+":"+ro.getAmm();

				// 予約開始まで規定時間以内の場合
				if ( nextstart.compareTo(dtmin) >= 0 && nextstart.compareTo(dtmax) < 0 ) {
//					String chname = (ro.getCh_name()!=null && ro.getCh_name().length()>0)?(ro.getCh_name()):("★放送局名不正("+ro.getChannel()+")");
//					String length = ro.getRec_min();
					String title = ro.getTitle();
					String stime = ro.getAhh()+":"+ro.getAmm();
					String etime = ro.getZhh()+":"+ro.getZmm();
					String body =
							"「" + title + "」\n" +
							"(" + stime + "～" + etime + ")\n";

					if (isWin10){
						list.add(head + body);
					}
					else{
						bodyAll += body;
					}
				}
			}
		}

		if (!isWin10 && !bodyAll.isEmpty())
			list.add(head + bodyAll);

		return list.size() > 0;
	}

	// 内部的な

	/**
	 * テーブル（の中の人）に追加
	 */
	private void addRow(ReservedItem data) {
		// 開始日時＋放送局でソート
		int i=0;
		for (; i<rowData.size(); i++) {
			ReservedItem ra = rowData.get(i);
			int x = ra.nextstart.compareTo(data.nextstart);
			int y = ra.hide_chname.compareTo(data.hide_chname);
			int z = ra.encoder.compareTo(data.encoder);
			if (x == 0 && y == 0 && z > 0) {
				break;	// 挿入位置確定
			}
			if (x == 0 && y > 0) {
				break;	// 挿入位置確定
			}
			else if (x > 0) {
				break;	// 挿入位置確定
			}
		}

		// 有効データ
		rowData.add(i, data);
	}

	/**
	 * 重複マークつけてください
	 */
	private void setOverlapMark() {

		if ( rowViewTemp.size() < 2 ) {
			return;
		}

		// 時間重複の計算
		ResTimeList list = new ResTimeList();
		for (int na=0; na<jTable_rsved.getRowCount()-1; na++) {
			int vrow = jTable_rsved.convertRowIndexToModel(na);
			ReservedItem ra = rowViewTemp.get(vrow);
			if (!ra.exec)
				continue;

			GregorianCalendar ca = CommonUtils.getCalendar(ra.nextstart);
			if ( ca != null ) {
				String sDTa = CommonUtils.getDateTime(ca);

				int len = Integer.valueOf(ra.length);
				ca.add(Calendar.MINUTE, len);
				String eDTa = CommonUtils.getDateTime(ca);

				list.mergeResTimeItem(ra.recorder, sDTa, eDTa, null, null);
			}
		}


		// 最初の一行はリセットしておかないとなんの処理も行われない場合がある
		ReservedItem fr = rowViewTemp.get(jTable_rsved.convertRowIndexToModel(0));
		fr.dupmark = "";
		fr.hide_devicecolor = VALID_DEVNAME_COLOR;
		fr.fireChanged();

		// 時間重複のマーキング
		for (int na=0; na<jTable_rsved.getRowCount()-1; na++) {
			int vrow = jTable_rsved.convertRowIndexToModel(na);
			ReservedItem ra = rowViewTemp.get(vrow);

			HDDRecorderList recs = recorders.findInstance(ra.recorder);
			int tnum = (recs != null && recs.size() > 0) ? recs.get(0).getTunerNum() : 0;

			String sDTa = "";
			String eDTa = "";
			GregorianCalendar ca = CommonUtils.getCalendar(ra.nextstart);
			if ( ca != null ) {
				sDTa = CommonUtils.getDateTime(ca);

				int len = Integer.valueOf(ra.length);
				ca.add(Calendar.MINUTE, len);
				eDTa = CommonUtils.getDateTime(ca);
			}

			boolean dup_rep = false;

			for (int nb=0; nb<jTable_rsved.getRowCount()-1; nb++) {
				if (nb == na)
					continue;

				int vrow2 = jTable_rsved.convertRowIndexToModel(nb);
				ReservedItem rb = rowViewTemp.get(vrow2);
				if (!rb.recorder.equals(ra.recorder))
					continue;

				String sDTb = "";
				String eDTb = "";
				GregorianCalendar cb = CommonUtils.getCalendar(rb.nextstart);
				if ( cb != null ) {
					sDTb = CommonUtils.getDateTime(cb);

					int len = Integer.valueOf(rb.length);
					cb.add(Calendar.MINUTE, len);
					eDTb = CommonUtils.getDateTime(cb);
				}

				if (ra.devname != null && rb.devname != null){
					boolean usb_check = !ra.devname.equals(rb.devname) && ra.devname.startsWith("USB") &&
							rb.devname.startsWith("USB");

					if ( sDTa.compareTo(eDTb) <= 0 && eDTa.compareTo(sDTb) >= 0 && usb_check){
						ra.hide_devicecolor = INVALID_DEVNAME_COLOR;
					}
				}

				if ( eDTa.equals(sDTb) || sDTa.equals(eDTb)) {
					dup_rep = true;
				}
			}

			int dup_count = ra.exec ? list.getMaxResCount(ra.recorder, sDTa, eDTa) : 0;
			if (dup_count > tnum){
				ra.dupmark = DUPMARK_EXCEED;
			}
			else if (dup_count > 1){
				ra.dupmark = DUPMARK_NORMAL;
			}
			else if (dup_rep){
				ra.dupmark = DUPMARK_REP;
			}
			else {
				ra.dupmark = "";
			}

			ra.fireChanged();
		}
	}

	/**
	 * 予約を編集したい
	 */
	private void editReserve(String recId,String rsvId,String chnam,int vrow) {

		//VWReserveDialog rD = new VWReserveDialog(0, 0, env, tvprograms, recorders, avs, chavs, stwin);
		//rD.clear();
		CommonSwingUtils.setLocationCenter(parent,rD);

		if (rD.open(recId,rsvId)) {
			rD.setVisible(true);
		}
		else {
			rD.setVisible(false);
		}

		if (rD.isSucceededReserve()) {
			// よそさま
			updateReserveDisplay(chnam);
			// じぶん
			_redrawReservedList();
			// フォーカスを戻す
			jTable_rsved.getSelectionModel().setSelectionInterval(vrow,vrow);
		}
	}


	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * 現在時刻追従処理
	 */
	@Override
	public void timerRised(TickTimerRiseEvent e) {
		if ( ! timer_now_enabled ) {
			return;
		}

		stopTimer(false);

		// 更新前に選択していた行を確認する
		String rsvid = null;
		{
			int row = jTable_rsved.getSelectedRow();
			if ( row >= 0 ) {
				int vrow = jTable_rsved.convertRowIndexToModel(row);
				rsvid = rowData.get(vrow).hide_rsvid;
			}
		}

		// タイマーはこの中で再開される
		_redrawReservedList();

		// 更新前に選択していた行を再度選択する
		if ( rsvid != null ) {
			int vrow = -1;
			for ( ReservedItem c : rowData ) {
				vrow++;
				if ( c.hide_rsvid.equals(rsvid) ) {
					int row = jTable_rsved.convertRowIndexToView(vrow);
					jTable_rsved.setRowSelectionInterval(row,row);
					break;
				}
			}
		}
	}

	/**
	 * タブが開かれたら表を書き換える
	 * ★synchronized(rowData)★
	 * @see #redrawReservedList()
	 */
	private final ComponentAdapter cl_tabshown = new ComponentAdapter() {
		@Override
		public void componentShown(ComponentEvent e) {
			// ★★★　イベントにトリガーされた処理がかちあわないように synchronized()　★★★
			synchronized ( rowViewTemp ) {
				// 終了した予約を整理する
				for (HDDRecorder recorder : recorders) {
					recorder.removePassedReserves();
				}

				if (dividerLocationOnShown != 0){
					jSplitPane_main.setDividerLocation(dividerLocationOnShown);
					dividerLocationOnShown = 0;
				}

				// 予約一覧を再構築する
				_redrawReservedList();
			}
		}
	};

	private void showPopupMenu(final int vrow, final ReservedItem ra, final int x, final int y){
		final boolean fexec = ra.exec;
		final String start = ra.nextstart;
		final String title = ra.title;
		final String chnam = ra.hide_chname;
		final String recId = ra.recorder;
		final String rsvId = ra.hide_rsvid;
		int num =jTable_rsved.getSelectedRowCount();
		final ArrayList<ReservedItem> ras = new ArrayList<ReservedItem>(num);

		if (!jTable_rsved.isRowSelected(vrow)){
			jTable_rsved.getSelectionModel().setSelectionInterval(vrow,vrow);
			num = 1;
		}

		if (num > 1){
			int rows[] = jTable_rsved.getSelectedRows();
			for (int n=0; n<num; n++){
				ras.add(rowViewTemp.get(jTable_rsved.convertRowIndexToModel(rows[n])));
			}
		}

		// 右クリックで予約削除メニュー表示
		JPopupMenu pop = new JPopupMenu();
		//
		{
			JMenuItem menuItem = new JMenuItem(String.format("予約を編集する【%s - %s(%s)】",start,title,chnam));
			menuItem.setForeground(new Color(0,127,0));
			Font f = menuItem.getFont();
			menuItem.setFont(f.deriveFont(f.getStyle()|Font.BOLD));

			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					editReserve(recId,rsvId,chnam,vrow);
				}
			});
			pop.add(menuItem);
		}

		pop.addSeparator();

		// 予約実行ON・OFF
		{
			pop.add(getExecOnOffMenuItem(fexec,start,title,chnam,rsvId,recId));
		}

		pop.addSeparator();

		{
			pop.add(getRemoveRsvMenuItem(start,title,chnam,rsvId,recId));
		}

		pop.addSeparator();

		{
			pop.add(getJumpMenuItem(title,chnam,start));
			pop.add(getJumpToLastWeekMenuItem(title,chnam,start));
		}

		pop.addSeparator();

		// クリップボードへコピーする
		{
			JMenuItem menuItem = new JMenuItem("番組名をコピー【"+title+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = title;
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});

			pop.add(menuItem);
		}
		{
			JMenuItem menuItem = new JMenuItem(String.format("予約情報をコピー【%s - %s(%s)】",start,title,chnam));
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = formatReservedItem(ra, false);
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});

			pop.add(menuItem);
		}
		if (num > 1){
			JMenuItem menuItem = new JMenuItem(String.format("選択中の%d個の予約情報をコピー【%s - %s(%s)】",
					num,start,title,chnam));
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = formatReservedItems(ras, false);
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});

			pop.add(menuItem);
		}

		pop.addSeparator();

		// CSV形式でクリップボードへコピーする
		{
			JMenuItem menuItem = new JMenuItem(String.format("予約情報をCSVでコピー【%s - %s(%s)】",start,title,chnam));
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = formatReservedHeader(true) + formatReservedItem(ra, true);
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});

			pop.add(menuItem);
		}

		if (num > 1){
			JMenuItem menuItem = new JMenuItem(String.format("選択中の%d個の予約情報をCSVでコピー【%s - %s(%s)】",
					num,start,title,chnam));
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String msg = formatReservedHeader(true) + formatReservedItems(ras, true);
					Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					StringSelection s = new StringSelection(msg);
					cb.setContents(s, null);
				}
			});

			pop.add(menuItem);
		}

		ProgDetailList pdl = getProgDetailList(ra);
		if (pdl != null){
			pop.addSeparator();
			addBrowseMenuToPopup(pop, pdl);
		}

		pop.show(jTable_rsved, x, y);

	}

	private final MouseAdapter ma_showpopup = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			//
			Point p = e.getPoint();
			final int vrow = jTable_rsved.rowAtPoint(p);
			final int row = jTable_rsved.convertRowIndexToModel(vrow);

			ReservedItem ra = rowViewTemp.get(row);
			final String chnam = ra.hide_chname;
			final String recId = ra.recorder;
			final String rsvId = ra.hide_rsvid;

			//
			if (e.getButton() == MouseEvent.BUTTON3) {
				if (e.getClickCount() == 1) {
					showPopupMenu(vrow, ra, e.getX(), e.getY());
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getClickCount() == 1) {

				}
				else if (e.getClickCount() == 2) {
					// 左ダブルクリックで予約ウィンドウを開く
					editReserve(recId,rsvId,chnam,vrow);
				}
			}
		}
	};

	/*
	 * ヘッダー情報をフォーマットする
	 */
	private String formatReservedHeader(boolean csv){
		StringBuilder sb = new StringBuilder();

		for (RsvedColumn col: RsvedColumn.values()){
			String value = col.toString();
			boolean last = col == RsvedColumn.RECORDER;
			if (csv){
				sb.append(CommonUtils.toQuoted(value));
				if (!last)
					sb.append(",");
			}
			else{
				sb.append(value);
				if (!last)
					sb.append("\t");
			}
		}
		sb.append("\n");

		return sb.toString();
	}
	/*
	 * 複数の予約情報をテキストないしCSVでフォーマットする
	 */
	private String formatReservedItems(ArrayList<ReservedItem>ras, boolean csv){
		StringBuilder sb = new StringBuilder();

		for (ReservedItem ra : ras){
			sb.append(formatReservedItem(ra, csv));
		}

		return sb.toString();
	}

	/*
	 * 予約情報をテキストないしCSVでフォーマットする
	 */
	private String formatReservedItem(ReservedItem ra, boolean csv){
		StringBuilder sb = new StringBuilder();

		for (RsvedColumn col: RsvedColumn.values()){
			String value = "";
			boolean last = col == RsvedColumn.RECORDER;
			switch(col){
			case PATTERN:
				value = ra.pattern;
				break;
			case DUPMARK:
				value =ra.dupmark;
				break;
			case EXEC:
				value = ra.exec ? "○" : "";
				break;
			case TRACE:
				value = ra.trace;
				break;
			case AUTO:
				value = ra.auto;
				break;
			case OPTIONS:
				value = ra.options;
				break;
			case NEXTSTART:
				value = ra.nextstart;
				break;
			case END:
				value = ra.end;
				break;
			case LENGTH:
				value = ra.length;
				break;
			case ENCODER:
				value = ra.encoder;
				break;
			case VRATE:
				value = ra.vrate;
				break;
			case ARATE:
				value = ra.arate;
				break;
			case TITLE:
				value = ra.title;
				break;
			case CHNAME:
				value = ra.chname;
				break;
			case DEVNAME:
				value = ra.devname;
				break;
			case FOLDER:
				value = ra.folder;
				break;
			case RECORDER:
				value = ra.recname;
				break;
			}

			if (value == null)
				value = "";

			if (csv){
				sb.append(CommonUtils.toQuoted(value));
				if (!last)
					sb.append(",");
			}
			else{
				sb.append(value);
				if (!last)
					sb.append("\t");
			}
		}

		sb.append("\n");

		return sb.toString();
	}

	private final RowSorterListener rsl_sorterchanged = new RowSorterListener() {
		@Override
		public void sorterChanged(RowSorterEvent e) {
			if ( e.getType() == Type.SORTED ) {
				if (rowViewTemp.size()>=2) setOverlapMark();
			}
		}
	};

	/**
	 *  行を選択すると詳細が表示されるようにする
	 */
	private ListSelectionListener lsSelectListner = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			if(e.getValueIsAdjusting()) return;
			if (jTable_rsved.getSelectedRow() >= 0) {
				int row = jTable_rsved.convertRowIndexToModel(jTable_rsved.getSelectedRow());
				ReservedItem c = rowData.get(row);
				jTextPane_detail.setLabel(
						c.nextstart,
						c.end,
						c.title,
						c.encoder + "　\0" + c.hide_encodercolor);

				String detail = getReserveDetail(c);
				String pdetail = getProgramDetail(c);

				if (detail == null)
					detail = "";
				else if (!detail.isEmpty())
					detail += "\r\n";
				if (pdetail != null)
					detail += pdetail;

				jTextPane_detail.setText(detail);
			}
			else {
				jTextPane_detail.setLabel("","","","");
				jTextPane_detail.setText("");
			}
		}
	};

	/*
	 * 予約詳細情報を取得する
	 */
	private String getReserveDetail(ReservedItem c){
		HDDRecorderList recs = recorders.findInstance(c.recorder);
		if (recs == null || recs.size() == 0)
			return null;

		ReserveList res = recs.get(0).getReserveList(c.hide_rsvid);

		return res != null ? res.getDetail() : null;
	}

	/*
	 * 番組情報を取得する
	 */
	private String getProgramDetail(ReservedItem c){
		ProgDetailList pdl = getProgDetailList(c);
		if (pdl == null)
			return null;

		return "【番組情報】\r\n" + pdl.accurateDate + " " + pdl.start + "～" + pdl.end + "　" + pdl.prefix_mark + pdl.newlast_mark + pdl.title + pdl.postfix_mark + "\r\n" + pdl.detail;
	}

	// 未来分の番組情報から該当番組の情報を取得する
	private ProgDetailList getProgDetailList(ReservedItem c){
		if (tvprograms == null)
			return null;

		// 番組表の日付に変換する
		String date = CommonUtils.getDate529(c.nextstart, true);
		if (date == null)
			return null;

		TVProgramIterator pli = tvprograms.getIterator().build(null, TVProgramIterator.IterationType.ALL);
		ProgDetailList pdl = getProgDetailForReserve(pli, c, date);

		// 見つからなかったら過去分の番組情報をロードして該当番組の情報を取得する
		if (pdl == null){
			PassedProgram passed = tvprograms.getPassed();

			if (passed.loadByCenter(date, c.chname)){
				pli = tvprograms.getIterator().build(null, TVProgramIterator.IterationType.PASSED);
				pdl = getProgDetailForReserve(pli, c, date);
			}
		}

		return pdl;
	}

	/*
	 * 指定したタイトルと放送局が同じで時間が重なる番組情報を取得する
	 */
	private ProgDetailList getProgDetailForReserve(TVProgramIterator pli, ReservedItem c, String date){
		String start = c.hide_nextstart;
		String end = c.hide_nextend;
		String ch_name = c.chname;

		pli.rewind();

		// 番組情報についてループする
		for ( ProgList pl : pli ) {
			// チャンネルが異なる場合はスキップする
			if (! pl.Center.equals(ch_name))
				continue;

			// 日付についてループする
			for (ProgDateList pdl : pl.pdate){
				// 日付が異なる場合はスキップする
				if (! pdl.Date.equals(date))
					continue;

				// 日付内の番組についてループする
				for (ProgDetailList tvd : pdl.pdetail){
					int bse = tvd.startDateTime.compareTo(end);
					int bes = tvd.endDateTime.compareTo(start);

					// 予約情報と時間が重なる場合はその番組情報を返す
					if (bse * bes < 0)
						return tvd;
				}

				break;
			}

			break;
		}

		return null;
	}

	/*
	 * 詳細欄関係
	 */
	/*
	 * 詳細欄の高さを初期化する
	 */
	private void initDetailHeight(){
		if (bounds.getDetailRows() > 0){
			resetDetailHeight();
		}
		else{
			int dh = bounds.getReserveDetailHeight();
			if (dh > 1)
				setDetailHeight(dh);
			else
				resetDetailHeight();
		}
	}

	/*
	 * 詳細欄の高さをセットする
	 */
	private void setDetailHeight(int height){
		if (jTextPane_detail == null)
			return;

		jSplitPane_main.setDividerLocation(height);
		bounds.setReserveDetailHeight(height);
	}

	/*
	 * 詳細欄の高さをリセットする
	 */
	private void resetDetailHeight(){
		if (jTextPane_detail == null)
			return;

		int rows = bounds.getDetailRows();
		int height = jTextPane_detail.getHeightFromRows(rows > 0 ? rows : 4);

		setDetailHeight(height);
	}

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
	/*
	 * 詳細情報欄とリストを分割するペインを生成する
	 */
	private JSplitPane getJSplitPane_main() {
		if ( jSplitPane_main == null ) {

			jSplitPane_main = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

			jSplitPane_main.setTopComponent(getJTextPane_detail());
			jSplitPane_main.setBottomComponent(getJScrollPane_view());

			initDetailHeight();

			jSplitPane_main.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
					@Override
			        public void propertyChange(PropertyChangeEvent pce) {
						if (jTextPane_detail.isVisible())
							bounds.setReserveDetailHeight(jSplitPane_main.getDividerLocation());
					}
				});
		}

		return jSplitPane_main;
	}

	/*
	 * 詳細情報欄を生成する
	 */
	private JDetailPanel getJTextPane_detail() {
		if (jTextPane_detail == null) {
			jTextPane_detail = new JDetailPanel();
			jTextPane_detail.setRows(bounds.getDetailRows());
		}
		return jTextPane_detail;
	}

	/*
	 * メインのペインを生成する
	 */
	private JScrollPane getJScrollPane_view() {
		if ( jScrollPane_view == null ) {
			jScrollPane_view = new JScrollPane();
			jScrollPane_view.setRowHeaderView(jTable_rowheader = new JTableRowHeader(rowViewTemp));
			jScrollPane_view.setViewportView(getNETable_rsved());

			Dimension d = new Dimension(jTable_rowheader.getPreferredSize().width,0);
			jScrollPane_view.getRowHeader().setPreferredSize(d);

			setRowHeaderVisible(env.getRowHeaderVisible());

		}

		return jScrollPane_view;
	}

	private TableColumn getColumn(RsvedColumn rcol){
		TableColumn col = null;
		try{
			col = jTable_rsved.getColumn(rcol.toString());
		}
		catch(IllegalArgumentException e){
			return null;
		}

		return col;
	}

	/*
	 * テーブルのソーターを初期化する
	 */
 	private void initTableSorter(){
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel_rsved);
		jTable_rsved.setRowSorter(sorter);

		sorter.addRowSorterListener(rsl_sorterchanged);

		// 数値でソートする項目用の計算式（番組長とか）
		final Comparator<String> titlecomp = new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				String t1 = TraceProgram.replacePop(o1.replaceAll(TVProgram.titlePrefixRemoveExpr, "")).replaceFirst(TVProgram.epnoNormalizeExpr, "$1\\0$2");
				String t2 = TraceProgram.replacePop(o2.replaceAll(TVProgram.titlePrefixRemoveExpr, "")).replaceFirst(TVProgram.epnoNormalizeExpr, "$1\\0$2");
				return t1.compareTo(t2);
			}
		};

		// ソーターの効かない項目用の計算式（重複マーク）
		final Comparator<String> noncomp = new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return 0;
			}
		};

		TableColumn col = getColumn(RsvedColumn.TITLE);
		if (col != null)
			sorter.setComparator(col.getModelIndex(),titlecomp);
		col = getColumn(RsvedColumn.DUPMARK);
		if (col != null)
			sorter.setComparator(col.getModelIndex(),noncomp);

		final Comparator<String> lengthcomp = new Comparator<String>() {

			@Override
			public int compare(String len1, String len2) {
				String [] s1 = len1.split("m");
				String [] s2 = len2.split("m");
				return Integer.parseInt(s1[0]) - Integer.parseInt(s2[0]);
			}
		};

		col = getColumn(RsvedColumn.LENGTH);
		if (col != null)
			sorter.setComparator(col.getModelIndex(),lengthcomp);

	}

	/*
	 * テーブルのレンダラーを初期化する
	 */
	private void initTableRenderer(){
		// 重複マーク・実行マークはちょっとだけ表示の仕方が違う
		VWColorCharCellRenderer renderer = new VWColorCharCellRenderer(JLabel.CENTER);
		if ( CommonUtils.isMac() ) renderer.setMacMarkFont();

		TableColumn col = getColumn(RsvedColumn.DUPMARK);
		if (col != null)
			col.setCellRenderer(renderer);
		ButtonColumn buttonColumn = new ButtonColumn(execicon);
		col = getColumn(RsvedColumn.EXEC);
		if (col != null){
			col.setCellRenderer(buttonColumn);
			col.setCellEditor(buttonColumn);
			col.setResizable(false);
		}
		col = getColumn(RsvedColumn.TRACE);
		if (col != null)
			col.setCellRenderer(renderer);
		col = getColumn(RsvedColumn.AUTO);
		if (col != null)
			col.setCellRenderer(renderer);

		VWColorCharCellRenderer renderer2 = new VWColorCharCellRenderer(JLabel.LEFT);
		col = getColumn(RsvedColumn.TITLE);
		if (col != null)
			col.setCellRenderer(renderer2);
		col = getColumn(RsvedColumn.NEXTSTART);
		if (col != null)
			col.setCellRenderer(renderer2);
		col = getColumn(RsvedColumn.END);
		if (col != null)
			col.setCellRenderer(renderer2);
		col = getColumn(RsvedColumn.LENGTH);
		if (col != null)
			col.setCellRenderer(renderer2);
		col = getColumn(RsvedColumn.CHNAME);
		if (col != null)
			col.setCellRenderer(renderer2);
		col = getColumn(RsvedColumn.VRATE);
		if (col != null)
			col.setCellRenderer(renderer2);

		VWColorCharCellRenderer renderer3 = new VWColorCharCellRenderer(JLabel.LEFT);
		col = getColumn(RsvedColumn.ENCODER);
		if (col != null)
			col.setCellRenderer(renderer3);
		col = getColumn(RsvedColumn.DEVNAME);
		if (col != null)
			col.setCellRenderer(renderer3);
	}

	/*
	 * テーブルの列幅を初期化する
	 */
	private void initTableColumnWidth(){
		for ( RsvedColumn rc : RsvedColumn.values() ) {
			if ( rc.getIniWidth() < 0 ) {
				continue;
			}

			TableColumn col = getColumn(rc);
			if (col == null)
				continue;

			Integer w = bounds.getRsvedColumnSize().get(rc.toString());
			if (rc == RsvedColumn.EXEC)
				col.setPreferredWidth(env.ZMSIZE(rc.getIniWidth()));
			else
				col.setPreferredWidth(w != null ? w : rc.getIniWidth());
		}
	}

	/*
	 * 列表示のカスタマイズ結果を反映する
	 */
	public void reflectColumnEnv(){
		rlitems = (ReserveListColumnInfoList) getRlItemEnv().clone();

		tableModel_rsved.setColumnIdentifiers(rlitems.getColNames());

		// ソーターをつける
		initTableSorter();

		// レンダラーを初期化する
		initTableRenderer();

		// 表示幅を初期化する
		initTableColumnWidth();
	}

	/*
	 * Envの内容を反映する
	 */
	public void reflectEnv(){
		setRowHeaderVisible(env.getRowHeaderVisible());

		initDetailHeight();
	}

	private JNETableReserved getNETable_rsved() {
		if (jTable_rsved == null) {
			tableModel_rsved = new ReservedTableModel(rlitems.getColNames(), 0, rowViewTemp);
			jTable_rsved = new JNETableReserved(tableModel_rsved, true);
			jTable_rsved.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);

			// ヘッダのモデル
			rowheaderModel_rsved = (DefaultTableModel) jTable_rowheader.getModel();

			// ソータを付ける
			initTableSorter();

			// レンダラーを初期化する
			initTableRenderer();

			// 各カラムの幅
			initTableColumnWidth();

			// Envの内容を反映する
			reflectEnv();

			//　行を選択すると詳細が表示されるようにする
			jTable_rsved.getSelectionModel().addListSelectionListener(lsSelectListner);

			// 一覧表クリックで削除メニュー出現
			jTable_rsved.addMouseListener(ma_showpopup);
		}
		return jTable_rsved;
	}

	/*******************************************************************************
	 * 表表示
	 ******************************************************************************/

	private class JNETableReserved extends JNETable {

		private static final long serialVersionUID = 1L;

		// futuer use.
		public void setDisabledColor(Color c) { disabledColor = c; }
		private Color disabledColor = new Color(180,180,180);

		// 過去の予約の背景色
		public void setPassedColor(Color c) { passedColor = c; }
		private Color passedColor = CommonUtils.str2color(PASSED_COLOR);

		// 実行中の予約の背景色
		private Color currentColorEven = CommonUtils.str2color(CURRENT_COLOR_EVEN);
		private Color currentColorOdd = CommonUtils.str2color(CURRENT_COLOR_ODD);

		// 実行中の予約の背景色をセットする
		public void setCurrentColor(	Color c) {
			if ( c == null ) {
				currentColorEven = null;
				currentColorOdd = null;
			}
			else {
				currentColorOdd = c;
				currentColorEven = new Color(
						((c.getRed()>=247)?(255):(c.getRed()+8)),
						((c.getGreen()>=247)?(255):(c.getGreen()+8)),
						((c.getBlue()>=247)?(255):(c.getBlue()+8))
						);
			}
		}

		public void setTunerShortColor(Color c) { tunershortColor = c; }
		private Color tunershortColor = new Color(255,255,0);

		public void setRecordedColor(Color c) { recordedColor = c; }
		private Color recordedColor = new Color(204,153,255);

		private int prechkrow = -1;
		private boolean prechkdisabled = false;
		private boolean prechktunershort = false;
		private boolean prechkrecorded = false;
		private boolean prechkpassed = false;
		private boolean prechkcurrent = false;
		private boolean prechknextweek = false;

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component c = super.prepareRenderer(tcr, row, column);
			Color fgColor = this.getForeground();
			Color bgColor = (isSepRowColor && row%2 == 1)?(evenColor):(super.getBackground());

			isRowPassed(row);

			// 過去の予約の場合
			if( prechkpassed && passedColor != null ) {
				bgColor = passedColor;
			}
			// スキップ対象の予約の場合
			else if ( prechkdisabled ) {
				bgColor = disabledColor;
			}
			else if ( prechktunershort ) {
				bgColor = tunershortColor;
			}
			else if ( prechkrecorded ) {
				bgColor = recordedColor;
			}
			// 実行中の予約の場合
			else if( prechkcurrent && currentColorEven != null ) {
				bgColor = (isSepRowColor && row%2 == 1)?(currentColorEven):(currentColorOdd);
			}

			if(isRowSelected(row)) {
				fgColor = this.getSelectionForeground();
				bgColor = CommonUtils.getSelBgColor(bgColor);
			}

			if ( ! (tcr instanceof VWColorCharCellRenderer) && ! (tcr instanceof VWColorCharCellRenderer2) && ! (tcr instanceof VWColorCellRenderer)) {
				c.setForeground(fgColor);
			}
			if ( ! (tcr instanceof VWColorCellRenderer)) {
				c.setBackground(bgColor);
			}

			return c;
		}

	    // getToolTipText()をオーバーライド
		@Override
	    public String getToolTipText(MouseEvent e){
	    	int row = rowAtPoint(e.getPoint());
	    	int column = columnAtPoint(e.getPoint());
	    	if (row == -1 || column == -1)
	    		return null;

			// ツールチップテキストを生成する
			if (column == RsvedColumn.DUPMARK.getColumn()){
				int mr = convertRowIndexToModel(row);
				int mc = convertColumnIndexToModel(column);
				Object o = getModel().getValueAt(mr, mc);

				String text = null;
				if (o != null){
					String s = o.toString();
					if (s == null)
						;
					else if (s.startsWith("□")){
						text = "□:開始時間と終了時間が同じ";
					}
					else if (s.startsWith("■")){
						text = "■:時間が重なっている";
					}
					else if (s.startsWith("★")){
						text = "★:エンコーダの数を超える予約がある";
					}
				}

				return text;
			}
	    	else
	    		return super.getToolTipText(e);
	    }

		// 連続して同じ行へのアクセスがあったら計算を行わず前回のままにする
		private boolean isRowPassed(int prow) {

			if(prechkrow == prow) {
				return prechkdisabled;
			}

			int row = this.convertRowIndexToModel(prow);
			ReservedItem c = rowViewTemp.get(row);

			prechkpassed = false;
			prechkcurrent = false;
			prechknextweek = false;

			{
				// 実行可能かどうか
				prechkrow = prow;
				prechkdisabled = ! c.exec;
				prechktunershort = c.hide_tunershort;
				prechkrecorded = c.hide_recorded;
			}
			{
				// 終了済みの番組か否か
				String cDT = CommonUtils.getDateTime(0);

				prechkpassed = (cDT.compareTo(c.hide_nextend) >= 0);
				if ( ! prechkpassed ) {
					// 現在放送中
					prechkcurrent = (cDT.compareTo(c.hide_nextstart) >= 0);
				}
				if ( ! prechkcurrent ) {
					// 来週かな
					String critDT = CommonUtils.getCritDateTime(7);
					prechknextweek = (critDT.compareTo(c.hide_nextstart) <= 0);
				}
			}

			return true;
		}

		//
		@Override
		public void tableChanged(TableModelEvent e) {
			reset();
			super.tableChanged(e);
		}

		private void reset() {
			prechkrow = -1;
			prechkdisabled = false;
			prechktunershort = false;
			prechkrecorded = false;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			ListColumnInfo info = rlitems.getVisibleAt(column);
			if (info == null)
				return false;

			int cindex = info.getId()-1;
			if ( cindex == RsvedColumn.EXEC.getColumn() ) {
				return true;
			}
			return false;
		}

		// コンストラクタ
		public JNETableReserved(boolean b) {
			super(b);
		}
		public JNETableReserved(TableModel d, boolean b) {
			super(d,b);
		}
	}


	/**
	 * EXECボタン
	 */
	private class ButtonColumn extends AbstractExecButtonColumn {

		private static final long serialVersionUID = 1L;

		// コンストラクタ
		public ButtonColumn(ImageIcon icon) {
			super(icon);
		}

		@Override
		protected void toggleAction(ActionEvent e) {

			fireEditingStopped();

			int vrow = jTable_rsved.getSelectedRow();
			int row = jTable_rsved.convertRowIndexToModel(vrow);

			ReservedItem c = ((ReservedTableModel) jTable_rsved.getModel()).getRowItem(row);

			if ( doExecOnOff( ! c.exec, c.title, c.chname, c.hide_rsvid, c.recorder) ) {
				c.exec = ! c.exec;
				c.fireChanged();
			}

			jTable_rsved.clearSelection();
			jTable_rsved.setRowSelectionInterval(vrow, vrow);
		}
	}
}
