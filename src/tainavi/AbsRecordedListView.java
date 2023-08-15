package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


/**
 * 録画結果一覧タブのクラス
 */
public abstract class AbsRecordedListView extends JPanel {

	private static final long serialVersionUID = 1L;

	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();

	protected abstract HDDRecorderList getRecorderList();

	//protected abstract StatusWindow getStWin();
	//protected abstract StatusTextArea getMWin();
	//protected abstract AbsReserveDialog getReserveDialog();

	protected abstract Component getParentComponent();

	protected abstract void ringBeep();

	/**
	 * @see Viewer.VWToolBar#getSelectedRecorder()
	 */
	protected abstract String getSelectedRecorderOnToolbar();


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String MSGID = "[録画結果一覧] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// オブジェクト
	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();
	private final HDDRecorderList recorders = getRecorderList();

	//private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	//private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	// メソッド
	//private void StdAppendMessage(String message) { System.out.println(message); }
	//private void StdAppendError(String message) { System.err.println(message); }
	//private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	//private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }

	/**
	 * カラム定義
	 */

	public static HashMap<String,Integer> getColumnIniWidthMap() {
		if (rcmap.size() == 0 ) {
			for ( RecedColumn rc : RecedColumn.values() ) {
				rcmap.put(rc.toString(),rc.getIniWidth());	// toString()!
			}
		}
		return rcmap;
	}

	private static final HashMap<String,Integer> rcmap = new HashMap<String, Integer>();

	public static enum RecedColumn {
		START		("開始",			150),
		END			("終了",			50),
		LENGTH		("長さ",			50),
		DROP		("ドロップ",		50),
		DROPMPEG	("MPEG",		50),
		TITLE		("番組タイトル",	300),
		CHNAME		("チャンネル名",	150),
		RESULT		("録画結果",		200),
		RECORDER	("レコーダ",		200),
		;

		private String name;
		private int iniWidth;

		private RecedColumn(String name, int iniWidth) {
			this.name = name;
			this.iniWidth = iniWidth;
		}

		public String getName() {
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

	private class RecordedItem extends RowItem implements Cloneable {

		String start;	// YYYY/MM/DD(WD) hh:mm
		String end;			// hh:mm
		Integer length;
		Integer drop;
		Integer dropmpeg;
		String title;
		String chname;
		String result;
		String recname;
		String recorder;

		String hide_detail;
		Boolean hide_succeeded;

		@Override
		protected void myrefresh(RowItem o) {
			RecordedItem c = (RecordedItem) o;

			c.addData(start);
			c.addData(end);
			c.addData(length);
			c.addData(drop);
			c.addData(dropmpeg);
			c.addData(title);
			c.addData(chname);
			c.addData(result);
			c.addData(recname);
			c.addData(recorder);
		}

		public RecordedItem clone() {
			return (RecordedItem) super.clone();
		}
	}

	// ソートが必要な場合はTableModelを作る。ただし、その場合Viewのrowがわからないので行の入れ替えが行えない
	private class ReservedTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public Object getValueAt(int row, int column) {
			RecordedItem c = rowView.get(row);
			if ( c.getColumnCount() > column ) {
				if ( column == RecedColumn.LENGTH.getColumn() ) {
					return String.valueOf(c.length)+"m";
				}
				return c.get(column);
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return rowView.size();
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

		public ReservedTableModel(String[] colname, int i) {
			super(colname,i);
		}

	}

	//private final RecordedItem sa = new RecordedItem();

	private JScrollPane jsc_list = null;
	private JScrollPane jsc_detail = null;
	private JTextAreaWithPopup jta_detail = null;
	private JNETable jTable_reced = null;
	private JTable jTable_rowheader = null;

	private ReservedTableModel tableModel_reced = null;

	private DefaultTableModel rowheaderModel_reced = null;

	// 表示用のテーブル
	private final RowItemList<RecordedItem> rowView = new RowItemList<RecordedItem>();

	// テーブルの実体
	private final RowItemList<RecordedItem> rowData = new RowItemList<RecordedItem>();

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsRecordedListView() {

		super();

		this.setLayout(new BorderLayout());
		this.add(getJScrollPane_list(), BorderLayout.CENTER);
		this.add(getJTextPane_detail(), BorderLayout.PAGE_END);

		// バグ対応
		/*
		if ( bounds.getRecordedColumnSize() == null ) {
			System.err.println(ERRID+"なんらかの不具合によりテーブルのカラム幅設定が取得できませんでした。設定はリセットされました。申し訳ありません。");
			bounds.setRecordedColumnSize(rcmap);
		}
		else {
			for ( Entry<String, Integer> en : rcmap.entrySet() ) {
				try {
					bounds.getListedColumnSize().get(en.getKey());
				}
				catch (NullPointerException e) {
					System.err.println(ERRID+en.getKey()+", "+e.toString());
					bounds.getListedColumnSize().put(en.getKey(),en.getValue());
				}
			}
		}
		*/


		//
		this.addComponentListener(cl_tabShown);
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 対外的な

	/**
	 * 予約一覧を描画してほしいかなって
	 * ★synchronized(rowData)★
	 * @see #cl_tabShown
	 */
	public void redrawRecordedList() {
		// ★★★　イベントにトリガーされた処理がかちあわないように synchronized()　★★★
		synchronized ( rowView ) {
			_redrawRecordedList();
		}
	}

	private void _redrawRecordedList() {
		//
		rowData.clear();

		// 選択されたレコーダ
		String myself = getSelectedRecorderOnToolbar();
		HDDRecorderList recs = recorders.findInstance(myself);

		for ( HDDRecorder recorder : recs )
		{
			if ( recorder.isBackgroundOnly() ) {
				continue;
			}

			// 並べ替えるために新しいリストを作成する
			for ( RecordedInfo ro : recorder.getRecorded() ) {

				RecordedItem sa = new RecordedItem();

				sa.start = ro.getDate()+" "+ro.getAhh()+":"+ro.getAmm();	// YYYY/MM/DD(WD) hh:mm
				sa.end = ro.getZhh()+":"+ro.getZmm();
				sa.length = ro.getLength();
				sa.drop = ro.getDrop();
				sa.dropmpeg = ro.getDrop_mpeg();
				sa.title = ro.getTitle();
				sa.chname = ro.getCh_name();
				sa.result = ro.getResult();
				sa.recname = recorder.getDispName();
				sa.recorder = recorder.Myself();

				sa.hide_detail = ro.getDetail();
				sa.hide_succeeded = ro.getSucceeded();

				sa.fireChanged();

				addRow(sa);
			}
		}

		// 表示用
		rowView.clear();
		for ( RecordedItem a : rowData ) {
			rowView.add(a);
		}

		tableModel_reced.fireTableDataChanged(false);
		((DefaultTableModel)jTable_rowheader.getModel()).fireTableDataChanged();

		jta_detail.setText(null);

		//jta_detail.setText("レコーダから予約結果の一覧を取得して表示します。現在の対応レコーダはTvRock/EpgDataCap_Bonのみです。");
	}


	/**
	 * 絞り込み検索の本体（現在リストアップされているものから絞り込みを行う）（親から呼ばれるよ！）
	 */
	public void redrawListByKeyword(SearchKey keyword, String target) {

		// 情報を一行ずつチェックする
		if ( keyword != null ) {

			rowView.clear();

			for ( RecordedItem a : rowData ) {

				ProgDetailList tvd = new ProgDetailList();
				tvd.title = a.title;
				tvd.titlePop = TraceProgram.replacePop(tvd.title);

				boolean isFind = SearchProgram.isMatchKeyword(keyword, "", tvd);

				if ( isFind ) {
					rowView.add(a);
				}
			}

			// fire!
			tableModel_reced.fireTableDataChanged(true);
			rowheaderModel_reced.fireTableDataChanged();
		}
		else {
			if ( ! tableModel_reced.getFiltered() ) {
				System.out.println("xxx");
				return;
			}
			System.out.println("yyy");

			rowView.clear();

			for ( RecordedItem a : rowData ) {
				rowView.add(a);
			}

			// fire!
			tableModel_reced.fireTableDataChanged(false);
			rowheaderModel_reced.fireTableDataChanged();
		}
	}

	/**
	 *
	 */
	public void redrawListByErrorFilter() {

		rowView.clear();

		for ( RecordedItem a : rowData ) {
			if ( a.drop != 0 || ! a.hide_succeeded ) {
				rowView.add(a);
			}
		}

		// fire!
		tableModel_reced.fireTableDataChanged(true);
		rowheaderModel_reced.fireTableDataChanged();
	}

	/**
	 * カラム幅を保存する（鯛ナビ終了時に呼び出されるメソッド）
	 */
	public void copyColumnWidth() {
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_reced.getColumnModel();
		TableColumn column = null;
		for ( RecedColumn rc : RecedColumn.values() ) {
			if ( rc.getIniWidth() < 0 ) {
				continue;
			}
			column = columnModel.getColumn(rc.ordinal());
			if (column == null)
				continue;

			int w = column.getWidth();
			bounds.getRecordedColumnSize().put(rc.toString(), w > 0 ? w : rc.getIniWidth());	// toString()!
		}
	}

	/**
	 * テーブルの行番号の表示のＯＮ／ＯＦＦ
	 */
	public void setRowHeaderVisible(boolean b) {
		jsc_list.getRowHeader().setVisible(b);
	}

	// 内部的な

	/**
	 * テーブル（の中の人）に追加
	 */
	private void addRow(RecordedItem data) {
		// 有効データ
		int n=0;
		for ( ; n<rowData.size(); n++ ) {
			RecordedItem c = rowData.get(n);
			if ( c.start.compareTo(data.start) < 0 ) {
				break;
			}
		}
		rowData.add(n,data);
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * タブが開かれたら表を書き換える
	 * ★synchronized(rowData)★
	 * @see #redrawRecordedList()
	 */
	private final ComponentAdapter cl_tabShown = new ComponentAdapter() {
		@Override
		public void componentShown(ComponentEvent e) {
			// ★★★　イベントにトリガーされた処理がかちあわないように synchronized()　★★★
			synchronized ( rowView ) {
				_redrawRecordedList();
			}
		}
	};

	/**
	 *  行を選択すると詳細が表示されるようにする
	 */
	private final ListSelectionListener lsSelectListner = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if(e.getValueIsAdjusting()) return;
			if (jTable_reced.getSelectedRow() >= 0) {
				int row = jTable_reced.convertRowIndexToModel(jTable_reced.getSelectedRow());
				RecordedItem c = rowView.get(row);
				jta_detail.setText(c.hide_detail);
				jta_detail.setCaretPosition(0);
			}
		}
	};

	/**
	 * ツールバーで選択されている「先頭の」レコーダを取得する
	 */
	protected HDDRecorder getSelectedRecorder() {
		String myself = getSelectedRecorderOnToolbar();
		HDDRecorderList recs = recorders.findInstance(myself);

		for ( HDDRecorder rec : recs )	{
			return rec;
		}

		return null;
	}

	/**
	 * 一覧でのマウスイベント処理
	 * 右クリック時にポップアップメニューを表示する
	 */
	private final MouseAdapter ma_showpopup = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			// 選択されたレコーダ
			HDDRecorder rec = getSelectedRecorder();
			if (rec == null)
				return;

			Point p = e.getPoint();
			final int vrow = jTable_reced.rowAtPoint(p);
			final int row = jTable_reced.convertRowIndexToModel(vrow);

			RecordedItem ra = rowView.get(row);
			final String title = ra.title;
			final String chnam = ra.chname;
//			final String recId = ra.recorder;
			final String recName = ra.recname;
			int num =jTable_reced.getSelectedRowCount();
			final ArrayList<RecordedItem> ras = new ArrayList<RecordedItem>(num);

			//
			if (e.getButton() == MouseEvent.BUTTON3) {
				if (e.getClickCount() == 1) {
					if (!jTable_reced.isRowSelected(vrow))
						jTable_reced.getSelectionModel().setSelectionInterval(vrow,vrow);

					if (num > 1){
						int rows[] = jTable_reced.getSelectedRows();
						for (int n=0; n<num; n++){
							RecordedItem ri = rowView.get(jTable_reced.convertRowIndexToModel(rows[n]));
							ras.add(ri);
						}
					}

					// 右クリックでポップアップメニューを表示
					JPopupMenu pop = new JPopupMenu();

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
						JMenuItem menuItem = new JMenuItem(String.format("タイトル情報をコピー【%s (%s)/%s】", title, chnam, recName));
						menuItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								String msg = formatRecordedItem(ra, false);
								Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
								StringSelection s = new StringSelection(msg);
								cb.setContents(s, null);
							}
						});

						pop.add(menuItem);
					}
					if (num > 1){
						JMenuItem menuItem = new JMenuItem(String.format("選択中の%d個のタイトル情報をコピー【%s (%s)/%s】",
								num, title, chnam, recName));
						menuItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								String msg = formatRecordedItems(ras, false);
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
						JMenuItem menuItem = new JMenuItem(String.format("タイトル情報をCSVでコピー【%s (%s)/%s】",title,chnam, recName));
						menuItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								String msg = formatRecordedHeader(true) + formatRecordedItem(ra, true);
								Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
								StringSelection s = new StringSelection(msg);
								cb.setContents(s, null);
							}
						});

						pop.add(menuItem);
					}
					if (num > 1){
						JMenuItem menuItem = new JMenuItem(String.format("選択中の%d個のタイトル情報をCSVでコピー【%s (%s)/%s】",
								num, title, chnam, recName));
						menuItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								String msg = formatRecordedHeader(true) + formatRecordedItems(ras, true);
								Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
								StringSelection s = new StringSelection(msg);
								cb.setContents(s, null);
							}
						});

						pop.add(menuItem);
					}

					pop.show(jTable_reced, e.getX(), e.getY());
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getClickCount() == 1) {
				}
				else if (e.getClickCount() == 2) {
//					editTitleOfRow(vrow);
				}
			}
		}
	};

	/*
	 * ヘッダー情報をフォーマットする
	 */
	private String formatRecordedHeader(boolean csv){
		StringBuilder sb = new StringBuilder();

		for (RecedColumn col: RecedColumn.values()){
			String value = col.getName();
			boolean last = col == RecedColumn.RECORDER;
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
	 * 複数の録画結果情報をテキストないしCSVでフォーマットする
	 */
	private String formatRecordedItems(ArrayList<RecordedItem>ras, boolean csv){
		StringBuilder sb = new StringBuilder();

		for (RecordedItem ra : ras){
			sb.append(formatRecordedItem(ra, csv));
		}

		return sb.toString();
	}

	/*
	 * 録画結果情報をテキストないしCSVでフォーマットする
	 */
	private String formatRecordedItem(RecordedItem ra, boolean csv){
		StringBuilder sb = new StringBuilder();

		for (RecedColumn col: RecedColumn.values()){
			String value = "";
			boolean last = col == RecedColumn.RECORDER;
			switch(col){
			case START:
				value = ra.start;
				break;
			case END:
				value = ra.end;
				break;
			case LENGTH:
				value = ra.length + "m";
				break;
			case DROP:
				value = ra.drop != null ? ra.drop.toString() : null;
				break;
			case DROPMPEG:
				value = ra.dropmpeg != null ? ra.dropmpeg.toString() : null;
				break;
			case TITLE:
				value = ra.title;
				break;
			case CHNAME:
				value = ra.chname;
				break;
			case RESULT:
				value = ra.result;
				break;
			case RECORDER:
				value = ra.recorder;
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


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JScrollPane getJScrollPane_list() {

		if ( jsc_list == null ) {
			jsc_list = new JScrollPane();

			jsc_list.setRowHeaderView(jTable_rowheader = new JTableRowHeader(rowView));
			jsc_list.setViewportView(getNETable_reced());

			Dimension d = new Dimension(jTable_rowheader.getPreferredSize().width,0);
			jsc_list.getRowHeader().setPreferredSize(d);

			this.setRowHeaderVisible(env.getRowHeaderVisible());
		}

		return jsc_list;
	}

	private JScrollPane getJTextPane_detail() {
		if ( jsc_detail == null ) {
			jsc_detail = new JScrollPane(jta_detail = new JTextAreaWithPopup(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			jta_detail.setRows(8);
			jta_detail.setEditable(false);
			jta_detail.setBackground(Color.LIGHT_GRAY);
		}
		return jsc_detail;
	}


	private JNETable getNETable_reced() {
		if (jTable_reced == null) {

			ArrayList<String> cola = new ArrayList<String>();
			for ( RecedColumn rc : RecedColumn.values() ) {
				if ( rc.getIniWidth() >= 0 ) {
					cola.add(rc.getName());
				}
			}
			String[] colname = cola.toArray(new String[0]);

			tableModel_reced = new ReservedTableModel(colname, 0);
			jTable_reced = new JNETableRecorded(tableModel_reced, true);
			jTable_reced.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);

			// ヘッダのモデル
			rowheaderModel_reced = (DefaultTableModel) jTable_rowheader.getModel();

			// ソータを付ける
			TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel_reced);
			jTable_reced.setRowSorter(sorter);

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

			sorter.setComparator(jTable_reced.getColumn(RecedColumn.TITLE.getName()).getModelIndex(),titlecomp);
			sorter.setComparator(jTable_reced.getColumn(RecedColumn.END.getName()).getModelIndex(),noncomp);

			// 各カラムの幅
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_reced.getColumnModel();
			TableColumn column = null;
			for ( RecedColumn rc : RecedColumn.values() ) {
				if ( rc.getIniWidth() < 0 ) {
					continue;
				}
				column = columnModel.getColumn(rc.ordinal());
				if (column == null)
					continue;

				Integer width = bounds.getRecordedColumnSize().get(rc.toString());
				column.setPreferredWidth(width != null ? width : rc.getIniWidth());
			}

			// 詳細表示
			jTable_reced.getSelectionModel().addListSelectionListener(lsSelectListner);

			// 一覧表クリックで削除メニュー出現
			jTable_reced.addMouseListener(ma_showpopup);

		}
		return jTable_reced;
	}




	/*******************************************************************************
	 * 表表示
	 ******************************************************************************/

	private class JNETableRecorded extends JNETable {

		private static final long serialVersionUID = 1L;

		private Color failedColor = new Color(255,255,0);
		private Color dropMpegColor = new Color(255,0,0);
		private Color dropColor = new Color(255,204,204);

		private int prechkrow = -1;
		private boolean prechksucceeded = false;

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component c = super.prepareRenderer(tcr, row, column);
			Color fgColor = this.getForeground();
			Color bgColor = (isSepRowColor && row%2 == 1)?(evenColor):(super.getBackground());

			int xrow = this.convertRowIndexToModel(row);
			RecordedItem item = rowView.get(xrow);

			if ( ! item.hide_succeeded ) {
				bgColor = failedColor;
			}
			else if ( item.dropmpeg > 0 ) {
				bgColor = dropMpegColor;
			}
			else if ( item.drop > 0 ) {
				bgColor = dropColor;
			}

			if(isRowSelected(row)) {
				fgColor = this.getSelectionForeground();
				bgColor = CommonUtils.getSelBgColor(bgColor);
			}

			c.setForeground(fgColor);
			c.setBackground(bgColor);
			return c;
		}

		// 連続して同じ行へのアクセスがあったら計算を行わず前回のままにする
		private boolean isRowPassed(int prow) {

			if(prechkrow == prow) {
				return prechksucceeded;
			}

			int row = this.convertRowIndexToModel(prow);
			RecordedItem c = rowView.get(row);

			{
				// 実行可能かどうか
				prechkrow = prow;
				prechksucceeded = c.hide_succeeded;
				return prechksucceeded;
			}
		}

		//
		@Override
		public void tableChanged(TableModelEvent e) {
			reset();
			super.tableChanged(e);
		}

		private void reset() {
			prechkrow = -1;
			prechksucceeded = true;
		}

		/*
		 * コンストラクタ
		 */
		public JNETableRecorded(boolean b) {
			super(b);
		}
		public JNETableRecorded(TableModel d, boolean b) {
			super(d,b);
		}
	}

}
