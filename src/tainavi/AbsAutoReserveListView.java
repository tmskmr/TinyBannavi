package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;


/**
 * 自動予約一覧タブのクラス
 * @since 3.22.2β
 */
public abstract class AbsAutoReserveListView extends JPanel implements HDDRecorderListener {

	private static final long serialVersionUID = 1L;

	public void setDebug(boolean b) {debug = b; }
	private boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();


	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String MSGID = "[自動予約一覧] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	private static final String ICONFILE_EXEC			= "icon/media-record-3.png";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// オブジェクト
	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();

	private final ImageIcon execicon = new ImageIcon(ICONFILE_EXEC);

	/**
	 * カラム定義
	 */

	public static HashMap<String,Integer> getColumnIniWidthMap() {
		if (rcmap.size() == 0 ) {
			for ( AutoRsvColumn rc : AutoRsvColumn.values() ) {
				rcmap.put(rc.toString(),rc.getIniWidth());	// toString()!
			}
		}
		return rcmap;
	}

	private static final HashMap<String,Integer> rcmap = new HashMap<String, Integer>();

	public static enum AutoRsvColumn {
		EXEC		("実行",			50),
		TITLE		("タイトル",		500),
		CHNAME		("チャンネル名",	300),
		;

		private String name;
		private int iniWidth;

		private AutoRsvColumn(String name, int iniWidth) {
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

		public static AutoRsvColumn getColumnValue(int column) {
			if ( column >= AutoRsvColumn.values().length ) {
				return null;
			}
			return (AutoRsvColumn.values())[column];
		}
	};


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	// 表示用データの入れ物
	private class AutoRsvItem extends RowItem implements Cloneable {

		AutoReserveInfo autorsv;

		@Override
		protected void myrefresh(RowItem o) {
			AutoRsvItem c = (AutoRsvItem) o;

			c.addData(c.autorsv.getExec());
			c.addData(c.autorsv.getLabel());
			c.addData(c.autorsv.getChName());
		}

		public AutoRsvItem clone() {
			return (AutoRsvItem) super.clone();
		}

	}

	// ソートが必要な場合はTableModelを作る。ただし、その場合Viewのrowがわからないので行の入れ替えが行えない
	private class AutoRsvTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		private RowItemList<AutoRsvItem> rDat;

		@Override
		public Object getValueAt(int row, int column) {
			AutoRsvItem c = rDat.get(row);
			if ( c.getColumnCount() > column ) {
				return c.get(column);
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			// ダミー
		}

		@Override
		public int getRowCount() {
			return (rDat!=null) ? rDat.size() : 0;	// ↓ のsuper()で呼ばれるのでnullチェックが必要
		}

		public AutoRsvTableModel(String[] colname, int i, RowItemList<AutoRsvItem> rowdata) {
			super(colname,i);
			this.rDat = rowdata;
		}
	}

	// リスト部
	private JScrollPane jsc_list = null;
	private JNETable jt_list = null;
	private JTable jt_rowheader = null;
	private DefaultTableModel tableModel_list = null;
	private DefaultTableModel rowheaderModel_list = null;

	// 詳細表示部
	private JScrollPane jsc_detail = null;
	private JTextAreaWithPopup jta_detail = null;

	// テーブルの実体
	private final RowItemList<AutoRsvItem> rowData = new RowItemList<AutoRsvItem>();

	// 表示のための一時保存用
	private final RowItemList<AutoRsvItem> rowViewTemp = new RowItemList<AutoRsvItem>();


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsAutoReserveListView() {

		super();

		this.setLayout(new BorderLayout());
		this.add(getJScrollPane_list(), BorderLayout.CENTER);
		this.add(getJTextPane_detail(), BorderLayout.PAGE_END);

	}


	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	/**
	 * テーブルの行番号の表示のＯＮ／ＯＦＦ
	 */
	public void setRowHeaderVisible(boolean b) {
		jsc_list.getRowHeader().setVisible(b);
	}

	/**
	 * テーブルを書き換える
	 */
	private void redrawByRecorderSelected() {

		// テーブルデータを置き換える
		rowData.clear();
		rowViewTemp.clear();
		for ( HDDRecorder rec : getSelectedRecorderList() ) {
			for ( AutoReserveInfo info : rec.getAutoReserves() ) {
				AutoRsvItem c = new AutoRsvItem();
				c.autorsv = info;
				c.fireChanged();
				rowData.add(c);
				rowViewTemp.add(c);
			}
		}

		// fire!
		tableModel_list.fireTableDataChanged();
		rowheaderModel_list.fireTableDataChanged();
	}


	/*******************************************************************************
	 * ハンドラ―メソッド
	 ******************************************************************************/

	/**
	 * ツールバーでレコーダの選択イベントが発生
	 */
	@Override
	public void valueChanged(HDDRecorderSelectionEvent e) {
		if (debug) System.out.println(DBGID+"recorder selection rised");

		// 選択中のレコーダ情報を保存する
		src_recsel = (HDDRecorderSelectable) e.getSource();

		// テーブルを書き換える
		redrawByRecorderSelected();
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
		// テーブルをリフレッシュする処理
		redrawByRecorderSelected();
	}


	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	/**
	 * リストのペーン
	 */
	private JScrollPane getJScrollPane_list() {

		if ( jsc_list == null ) {
			jsc_list = new JScrollPane();

			jsc_list.setRowHeaderView(jt_rowheader = new JTableRowHeader(rowViewTemp));
			jsc_list.setViewportView(getNETable_list());

			Dimension d = new Dimension(jt_rowheader.getPreferredSize().width,0);
			jsc_list.getRowHeader().setPreferredSize(d);

			this.setRowHeaderVisible(env.getRowHeaderVisible());
		}

		return jsc_list;
	}

	private JNETable getNETable_list() {
		if (jt_list == null) {

			ArrayList<String> cola = new ArrayList<String>();
			for ( AutoRsvColumn rc : AutoRsvColumn.values() ) {
				if ( rc.getIniWidth() >= 0 ) {
					cola.add(rc.getName());
				}
			}
			String[] colname = cola.toArray(new String[0]);

			tableModel_list = new AutoRsvTableModel(colname, 0, rowViewTemp);
			jt_list = new JNETableAutoReserve(tableModel_list, false);
			jt_list.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);

			// ヘッダのモデル
			rowheaderModel_list = (DefaultTableModel) jt_rowheader.getModel();

			/* - ソートはいらない気がする -

			// ソータを付ける
			TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel_list);
			jt_list.setRowSorter(sorter);

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

			sorter.setComparator(jt_list.getColumn(AutoResColumn.TITLE.getName()).getModelIndex(),titlecomp);
			sorter.setComparator(jt_list.getColumn(AutoResColumn.END.getName()).getModelIndex(),noncomp);

			*/

			// 各カラムの幅
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jt_list.getColumnModel();
			TableColumn column = null;
			for ( AutoRsvColumn rc : AutoRsvColumn.values() ) {
				if ( rc.getIniWidth() < 0 ) {
					continue;
				}
				column = columnModel.getColumn(rc.ordinal());
				//column.setPreferredWidth(bounds.getRecordedColumnSize().get(rc.toString()));
				column.setPreferredWidth(rc.getIniWidth());
			}

			// 特殊なカラムの設定
			ButtonColumn buttonColumn = new ButtonColumn(execicon);
			jt_list.getColumn(AutoRsvColumn.EXEC.getName()).setCellRenderer(buttonColumn);
			jt_list.getColumn(AutoRsvColumn.EXEC.getName()).setCellEditor(buttonColumn);
			jt_list.getColumn(AutoRsvColumn.EXEC.getName()).setResizable(false);

			// 詳細表示
			//jt_list.getSelectionModel().addListSelectionListener(lsSelectListner);
		}
		return jt_list;
	}

	/**
	 * 詳細表示のペーン
	 */
	private JScrollPane getJTextPane_detail() {
		if ( jsc_detail == null ) {
			jsc_detail = new JScrollPane(jta_detail = new JTextAreaWithPopup(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			jta_detail.setRows(8);
			jta_detail.setEditable(false);
			jta_detail.setBackground(Color.LIGHT_GRAY);
		}
		return jsc_detail;
	}

	/*******************************************************************************
	 * 表表示
	 ******************************************************************************/

	private class JNETableAutoReserve extends JNETable {

		private static final long serialVersionUID = 1L;

		public void setDisabledColor(Color c) { disabledColor = c; }
		private Color disabledColor = new Color(180,180,180);

		private int prechkrow = -1;
		private boolean prechkdisabled = false;

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component c = super.prepareRenderer(tcr, row, column);
			Color bgColor = (isSepRowColor && row%2 == 1)?(evenColor):(super.getBackground());

			isRowPassed(row);

			if ( prechkdisabled ) {
				bgColor = disabledColor;
			}

			if(isRowSelected(row)) {
				bgColor = CommonUtils.getSelBgColor(bgColor);
			}

			c.setBackground(bgColor);
			return c;
		}

		// 連続して同じ行へのアクセスがあったら計算を行わず前回のままにする
		private boolean isRowPassed(int prow) {

			if(prechkrow == prow) {
				return prechkdisabled;
			}

			int row = this.convertRowIndexToModel(prow);
			AutoRsvItem c = rowViewTemp.get(row);

			{
				// 実行可能かどうか
				prechkrow = prow;
				prechkdisabled = ! c.autorsv.getExec();
			}

			return true;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if ( column == AutoRsvColumn.EXEC.getColumn() ) {
				return true;
			}
			return false;
		}

		// コンストラクタ

		public JNETableAutoReserve(boolean b) {
			super(b);
		}
		public JNETableAutoReserve(TableModel d, boolean b) {
			super(d,b);
		}

	}

	/**
	 * EXECボタン
	 */
	private class ButtonColumn extends AbstractExecButtonColumn {

		private static final long serialVersionUID = 1L;

		public ButtonColumn(ImageIcon icon) {
			super(icon);
		}

		@Override
		protected void toggleAction(ActionEvent e) {

			fireEditingStopped();

			int vrow = jt_list.getSelectedRow();
			int row = jt_list.convertRowIndexToModel(vrow);

			AutoRsvItem c = rowViewTemp.get(row);

			//if ( doExecOnOff( ! c.exec, c.title, c.chname, c.hide_rsvid, c.recorder) )
			{
				c.autorsv.setExec( ! c.autorsv.getExec());
				c.fireChanged();
			}

			jt_list.clearSelection();
			jt_list.setRowSelectionInterval(vrow, vrow);
		}
	}

}
