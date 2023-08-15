package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * CHソート設定のタブ
 * @since 3.15.4β　VWChannelSortからクラス名変更
 * @version 3.16 全面リライト
 */
public abstract class AbsChannelSortView extends JScrollPane {

	private static final long serialVersionUID = 1L;

	public void setDebug(boolean b) { debug = b; }
	private boolean debug = false;


	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract TVProgramList getTVProgramList();
	protected abstract ChannelSort getChannelSort();

	protected abstract StatusTextArea getMWin();

	/**
	 * ソート設定の更新を反映してください
	 */
	protected abstract void updProc();



	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	private final Env env = getEnv();
	private final TVProgramList tvprograms = getTVProgramList();
	private final ChannelSort chsort = getChannelSort();

	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト



	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String MSGID = "[CHソート設定] ";
	//private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	//private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	//private static final int BLOCK_SEP_HEIGHT = 75;

	private static final int LABEL_WIDTH = 250;
	private static final int BUTTON_WIDTH = 100;
	//private static final int BUTTON_WIDTH_LONG = 200;

	private static final int TABLE_WIDTH = 450;
	private static final int TABLE_HEIGHT = 475;

	private static final int UPDATE_WIDTH = 250;
	private static final int HINT_WIDTH = 750;

	private static final int PANEL_WIDTH = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH+TABLE_WIDTH+SEP_WIDTH;

	private static final String PAGEDIS_COLOR = "#888888";
	private static final String PAGEEN_ODD_COLOR = "#FFA0A0";
	private static final String PAGEEN_EVEN_COLOR = "#FF6060";

	private static final String LABEL_UPDATE = "更新を確定する";

	private static final String LABEL_ENABLED = "新聞形式の放送局表示順並べ替えは有効です";
	private static final String LABEL_DISABLED = "新聞形式の放送局表示順並べ替えは無効です";
	private static final String LABEL_ENABLED_U = LABEL_ENABLED+"（要更新確定）";
	private static final String LABEL_DISABLED_U = LABEL_DISABLED+"（要更新確定）";

	public static enum ChSortColumn {
		PAGE		("ページ",				50),
		WEBCHNAME	("Web番組表の放送局名",	TABLE_WIDTH-50),
		;

		private String name;
		private int iniWidth;

		private ChSortColumn(String name, int iniWidth) {
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
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JPanel jpan_update = null;
	private JPanel jpan_chsort = null;

	private JButton jbtn_update = null;
	//private JButton jbtn_refresh = null;
	private JToggleButton jtgl_chsort = null;
	private JButton jbtn_up = null;
	private JButton jbtn_down = null;
	private JButton jbtn_addPageBreak = null;
	private JButton jbtn_removePageBreak = null;
	private JButton jbtn_removeAllPageBreaks = null;

	private JScrollPane jscr_entries = null;
	private JNETable jtbl_entries = null;

	private JTextAreaWithPopup jta_help = null;

	// コンポーネント以外

	RowItemList<ChSortItem> rowData = null;

	ChannelSort chsortbak = null;

	ArrayList<Integer> pageBreaks;
	int centerPerPage;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsChannelSortView() {

		super();

		rowData = new RowItemList<ChSortItem>();

		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.setViewportView(getJPan_chsort());
		this.setColumnHeaderView(getJPan_update());
		this.pageBreaks = env.getPageBreaks();
		this.centerPerPage = env.getCenterPerPage();

		// 無効だろうが読む
		//chsort.load();

		// 初期値は？
		if ( env.getChSortEnabled() ) {
			chsortbak = null;

			jtgl_chsort.setSelected(true);

			updateChannelSortList(false);	// 保存はしない
		}
		else {
			backupChSort();

			_updateChannelSortTable();	// テーブルならべる
		}

		updatePageBreakButtons();

		setUpdateButtonEnabled(false);	// 設定を変えないと押せない
	}

	private JPanel getJPan_update() {
		if (jpan_update == null)
		{
			jpan_update = new JPanel();
			jpan_update.setLayout(new SpringLayout());

			jpan_update.setBorder(new LineBorder(Color.GRAY));

			int y = SEP_HEIGHT;
			CommonSwingUtils.putComponentOn(jpan_update, getJBtn_update(LABEL_UPDATE), UPDATE_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			int yz = SEP_HEIGHT/2;
			int x = UPDATE_WIDTH+50;
			CommonSwingUtils.putComponentOn(jpan_update, getJta_help(), HINT_WIDTH, PARTS_HEIGHT+SEP_HEIGHT, x, yz);

			y += (PARTS_HEIGHT + SEP_HEIGHT);

			// 画面の全体サイズを決める
			Dimension d = new Dimension(PANEL_WIDTH,y);
			jpan_update.setPreferredSize(d);
		}
		return jpan_update;
	}

	private JPanel getJPan_chsort() {
		if (jpan_chsort == null) {
			jpan_chsort = new JPanel();
			jpan_chsort.setLayout(new SpringLayout());

			int y = SEP_HEIGHT;
			int x = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jpan_chsort, getJTgl_chsort(), TABLE_WIDTH, PARTS_HEIGHT, x, y);

			//y+=(PARTS_HEIGHT+SEP_HEIGHT);
			//CommonSwingUtils.putComponentOn(jpan_chsort, getJBtn_refresh("CH設定を変更したら放送局リストをリフレッシュしてください"), TABLE_WIDTH, PARTS_HEIGHT, x, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jpan_chsort, getJScr_entries(), TABLE_WIDTH, TABLE_HEIGHT, x, y);

			int yz = y + TABLE_HEIGHT/2;
			x -= (BUTTON_WIDTH+SEP_WIDTH);
			CommonSwingUtils.putComponentOn(jpan_chsort, getJBtn_up("上へ"), BUTTON_WIDTH, PARTS_HEIGHT, x, yz-PARTS_HEIGHT-10);
			CommonSwingUtils.putComponentOn(jpan_chsort, getJBtn_down("下へ"), BUTTON_WIDTH, PARTS_HEIGHT, x, yz+10);

			yz+=10+(PARTS_HEIGHT+SEP_HEIGHT)*3;
			CommonSwingUtils.putComponentOn(jpan_chsort, getJBtn_addPageBreak("改頁追加"), BUTTON_WIDTH, PARTS_HEIGHT, x, yz);

			yz+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jpan_chsort, getJBtn_removePageBreak("改頁削除"), BUTTON_WIDTH, PARTS_HEIGHT, x, yz);

			yz+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jpan_chsort, getJBtn_removeAllPageBreaks("改頁なし"), BUTTON_WIDTH, PARTS_HEIGHT, x, yz);

			y+=(TABLE_HEIGHT+SEP_HEIGHT+SEP_HEIGHT);

			Dimension d = new Dimension(PANEL_WIDTH,y);
			jpan_chsort.setPreferredSize(d);
		}
		return jpan_chsort;
	}



	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	/**
	 *  既存にマッチしたものは順番を維持する・しないものは末尾に追加。こちらは外部（CH設定タブ）からの呼び出し用
	 */
	public void updateChannelSortTable() {

		if (debug) System.out.println(DBGID+"REFRESH KICKED BY CHSETTING");

		// 順番を引き継ぐ
		_updateChannelSortTable();
		// 結果を保存して
		updateChannelSortList(env.getChSortEnabled());	// 状態によって保存したりしなかったり
	}

	/**
	 * 既存にマッチしたものは順番を維持する・しないものは末尾に追加。こちらは内部からの呼び出し用
	 */
	private void _updateChannelSortTable() {

		if (debug) System.out.println(DBGID+"REFRESH");

		// 番組表プラグインの放送局リストをなめて、現在のリストとつきあわせて引き継げるものは引き継げるようによりわける
		ArrayList<ChSortItem> en = new ArrayList<ChSortItem>();
		ArrayList<ChSortItem> dis = new ArrayList<ChSortItem>();
		for (TVProgram p : tvprograms.getProgPlugins()) {
			for (Center cr : p.getSortedCRlist()) {
				ChSortItem si = new ChSortItem();
				si.page = "";
				si.webChName = cr.getCenter();
				si.areaCode = cr.getAreaCode();
				si.fireChanged();
				for (Center cs : chsort.getClst()) {
					if (cs.getCenter().equals(si.webChName) && cs.getAreaCode().equals(si.areaCode)) {
						en.add(si);
						si = null;
						break;
					}
				}
				if (si != null) {
					dis.add(si);
				}
			}
		}

		// よりわけたものをもとの順序にならべかえる
		RowItemList<ChSortItem> newRowData = new RowItemList<ChSortItem>();
		for (Center cs : chsort.getClst()) {
			// chsortの順序を引き継ぐ
			for (ChSortItem a : en) {
				if (cs.getCenter().equals(a.webChName) && cs.getAreaCode().equals(a.areaCode)) {
					newRowData.add(a);
				}
			}
		}

		// 新規追加分は入ってきた順番に追加
		for (ChSortItem a : dis) {
			newRowData.add(a);
		}

		// テーブル入れ替え
		rowData = newRowData;

		// Fire!
		((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();

	}

	private void updateChannelSortList(boolean save) {
		chsort.clear();
		for ( RowItem ra : rowData ) {
			ChSortItem a = (ChSortItem) ra;
			Center cr = new Center();
			cr.setCenter(a.webChName);
			cr.setAreaCode(a.areaCode);
			chsort.add(cr);
		}
		if (save) chsort.save();
	}

	private void setUpdateButtonEnabled(boolean b) {
		if ( b ) {
			jbtn_update.setForeground(Color.RED);
			jbtn_update.setEnabled(true);

			if ( jtgl_chsort.isSelected() ) {
				jtgl_chsort.setText(LABEL_ENABLED_U);
				jtgl_chsort.setForeground(Color.RED);
			}
			else {
				jtgl_chsort.setText(LABEL_DISABLED_U);
				jtgl_chsort.setForeground(Color.RED);
			}
		}
		else {
			jbtn_update.setForeground(Color.BLACK);
			jbtn_update.setEnabled(false);

			if ( jtgl_chsort.isSelected() ) {
				jtgl_chsort.setText(LABEL_ENABLED);
				jtgl_chsort.setForeground(Color.BLUE);
			}
			else {
				jtgl_chsort.setText(LABEL_DISABLED);
				jtgl_chsort.setForeground(Color.BLACK);
			}
		}
	}

	private void backupChSort() {
		chsortbak = new ChannelSort();
		copyChSort(chsortbak, chsort);
		chsort.clear();
	}
	private void undoChSort() {
		if ( chsortbak != null ) {
			chsort.clear();
			copyChSort(chsort, chsortbak);
			chsortbak = null;
		}
	}

	private void copyChSort(ChannelSort to, ChannelSort from) {
		to.clear();
		for ( Center cr : from.getClst() ) {
			to.add(cr);
		}

	}

	/*
	 * 改ページ関係のボタンの状態を更新する
	 */
	private void updatePageBreakButtons(){
		int vrow = jtbl_entries.getSelectedRow();
		int rows = jtbl_entries.getRowCount();

		jbtn_addPageBreak.setEnabled(vrow!=-1 && !pageBreaks.contains(vrow+1) && vrow < rows-1);
		jbtn_removePageBreak.setEnabled(vrow!=-1 && pageBreaks.contains(vrow+1));
		jbtn_removeAllPageBreaks.setEnabled(pageBreaks.size() > 0);
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * 更新を確定したいなあ
	 */
	private final ActionListener al_update = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			env.setChSortEnabled(jtgl_chsort.isSelected());
			env.setPageBreaks(pageBreaks);

			if ( env.getChSortEnabled() ) {
				// 放送局リストを作成する
				updateChannelSortList(true);
			}
			else {
				// 放送局リストをCH設定通りの並べに戻す
				_updateChannelSortTable();
			}

			// 更新確定ボタンは無効に戻す
			setUpdateButtonEnabled(false);

			// 本体側でなにか処理があるかなー？
			updProc();

			MWin.appendMessage(MSGID+"設定を保存しました");
		}
	};

	/*
	 * 改ページを追加する
	 */
	private final ActionListener al_addPageBreak = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			int vrow = jtbl_entries.getSelectedRow();
			if (vrow == -1 || pageBreaks.contains(vrow+1))
				return;

			pageBreaks.add(vrow+1);
			pageBreaks.sort(new IndexComparator());
			// Fire!
			((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
			if ( ! jbtn_update.isSelected() ) setUpdateButtonEnabled(true);	// 変更されちゃった
		}
	};

	public class IndexComparator implements Comparator<Integer>{
		@Override
		public int compare(Integer p1, Integer p2) {
			return p1 - p2;
		}
	}

	/*
	 * 改ページを削除する
	 */
	private final ActionListener al_removePageBreak = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			int vrow = jtbl_entries.getSelectedRow();
			if (vrow == -1 || !pageBreaks.contains(vrow+1))
				return;

			int idx = pageBreaks.indexOf(vrow+1);
			pageBreaks.remove(idx);
			pageBreaks.sort(new IndexComparator());
			// Fire!
			((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
			if ( ! jbtn_update.isSelected() ) setUpdateButtonEnabled(true);	// 変更されちゃった
		}
	};

	/*
	 * 改ページをすべて削除する
	 */
	private final ActionListener al_removeAllPageBreaks = new ActionListener(){
		public void actionPerformed(ActionEvent e){
			pageBreaks.clear();
			// Fire!
			((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
			if ( ! jbtn_update.isSelected() ) setUpdateButtonEnabled(true);	// 変更されちゃった
		}
	};

	/**
	 * トグルしたいなー
	 */
	private final ItemListener il_chsort = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {

			if (debug) System.out.println(DBGID+"chsort "+e.paramString());

			JToggleButton source = (JToggleButton) e.getSource();

			// コンポーネントの状態を変える
			{
//				jtbl_entries.setEnabled(source.isSelected());
				jbtn_up.setEnabled(source.isSelected());
				jbtn_down.setEnabled(source.isSelected());
			}

			if (source.isSelected()) {
				// 有効にした時
				undoChSort();

				_updateChannelSortTable();
			}
			else {
				// 無効にした時
				backupChSort();

				_updateChannelSortTable();
			}

			updatePageBreakButtons();

			if ( ! jbtn_update.isSelected() ) setUpdateButtonEnabled(true);	// 変更されちゃった
		}
	};

	/**
	 * 放送局一覧の選択変更時の処理
	 */
	private final ListSelectionListener lsl_entries = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			updatePageBreakButtons();
		}
	};

	private final PropertyChangeListener pcl_up = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			if ( "BUTTON.BP_PUSHBUTTON".equals(e.getPropertyName()) && e.getNewValue() != null && "PRESSED".equals(e.getNewValue().toString()) ) {
				if (debug) System.out.println(DBGID+"up "+e.getPropertyName()+" "+e.getNewValue());

				int vrow = jtbl_entries.getSelectedRow();
				int cnt = jtbl_entries.getSelectedRowCount();
				if ( rowData.up(vrow, cnt) ) {
					((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
					jtbl_entries.setRowSelectionInterval(vrow-1, vrow-1+(cnt-1));
				}

				updatePageBreakButtons();

				if ( ! jbtn_update.isSelected() ) setUpdateButtonEnabled(true);	// 変更されちゃった
			}
		}
	};

	private final PropertyChangeListener pcl_down = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			if ( "BUTTON.BP_PUSHBUTTON".equals(e.getPropertyName()) && e.getNewValue() != null && "PRESSED".equals(e.getNewValue().toString()) ) {

				int vrow = jtbl_entries.getSelectedRow();
				int cnt = jtbl_entries.getSelectedRowCount();
				if ( rowData.down(vrow, cnt) ) {
					((DefaultTableModel) jtbl_entries.getModel()).fireTableDataChanged();
					jtbl_entries.setRowSelectionInterval(vrow+1, vrow+1+(cnt-1));
				}

				updatePageBreakButtons();

				if ( ! jbtn_update.isSelected() ) setUpdateButtonEnabled(true);	// 変更されちゃった
			}
		}
	};



	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JButton getJBtn_update(String s) {
		if (jbtn_update == null) {
			jbtn_update = new JButton(s);

			jbtn_update.addActionListener(al_update);
		}
		return jbtn_update;
	}

	private JToggleButton getJTgl_chsort() {
		if (jtgl_chsort == null) {
			jtgl_chsort = new JToggleButton();
			jtgl_chsort.setSelected(false);
			jtgl_chsort.addItemListener(il_chsort);
		}
		return jtgl_chsort;
	}

	private JScrollPane getJScr_entries() {
		if (jscr_entries == null) {
			jscr_entries = new JScrollPane();
			jscr_entries.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			jscr_entries.getVerticalScrollBar().setUnitIncrement(25);
			jscr_entries.setViewportView(getJTbl_entries());
		}
		return jscr_entries;
	}

	private JNETable getJTbl_entries() {
		if (jtbl_entries == null) {

			// カラム名の初期化
			ArrayList<String> cola = new ArrayList<String>();
			for ( ChSortColumn lc : ChSortColumn.values() ) {
				if ( lc.getIniWidth() >= 0 ) {
					cola.add(lc.getName());
				}
			}
			final String[] colname = cola.toArray(new String[0]);

			DefaultTableModel model = new DefaultTableModel(colname, 0);
			jtbl_entries = new JChSortTable(model,false);
			jtbl_entries.getTableHeader().setReorderingAllowed(false);

			jtbl_entries.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

			// 各カラムの幅を設定する
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel) jtbl_entries.getColumnModel();
			TableColumn column = null;
			for ( ChSortColumn lc : ChSortColumn.values() ) {
				if ( lc.getIniWidth() < 0 ) {
					continue;
				}
				column = columnModel.getColumn(lc.ordinal());
				column.setPreferredWidth(lc.getIniWidth());
			}

			// ページ欄は色つき＆センタリング
			column = columnModel.getColumn(ChSortColumn.PAGE.ordinal());
			TableCellRenderer renderer = new VWColorCellRenderer();
			column.setCellRenderer(renderer);

			jtbl_entries.getSelectionModel().addListSelectionListener(lsl_entries);
		}
		return jtbl_entries;
	}

	// 放送局を上へ・下へ
	private JButton getJBtn_up(String s) {
		if (jbtn_up == null) {
			jbtn_up = new JButton(s);
			jbtn_up.addPropertyChangeListener(pcl_up);
		}
		return jbtn_up;
	}
	private JButton getJBtn_down(String s) {
		if (jbtn_down == null) {
			jbtn_down = new JButton(s);
			jbtn_down.addPropertyChangeListener(pcl_down);
		}
		return jbtn_down;
	}

	/*
	 *  改頁追加ボタン
	 */
	private JButton getJBtn_addPageBreak(String s) {
		if (jbtn_addPageBreak == null) {
			jbtn_addPageBreak = new JButton(s);
			jbtn_addPageBreak.addActionListener(al_addPageBreak);
		}
		return jbtn_addPageBreak;
	}

	/*
	 *  改頁削除ボタン
	 */
	private JButton getJBtn_removePageBreak(String s) {
		if (jbtn_removePageBreak == null) {
			jbtn_removePageBreak = new JButton(s);
			jbtn_removePageBreak.addActionListener(al_removePageBreak);
		}
		return jbtn_removePageBreak;
	}

	/*
	 *  改頁なしボタン
	 */
	private JButton getJBtn_removeAllPageBreaks(String s) {
		if (jbtn_removeAllPageBreaks == null) {
			jbtn_removeAllPageBreaks = new JButton(s);
			jbtn_removeAllPageBreaks.addActionListener(al_removeAllPageBreaks);
		}
		return jbtn_removeAllPageBreaks;
	}

	//
	private JTextAreaWithPopup getJta_help() {
		if ( jta_help == null ) {
			jta_help = CommonSwingUtils.getJta(this,2,0);
			jta_help.append(
					"Web番組表をまたいで放送局を並べ替えるための機能です。先にCH設定を完了させる必要があります。\n"+
					"（この機能の有効無効は処理速度に影響しません）");
		}
		return jta_help;
	}


	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/

	// テーブルの行データの構造
	private class ChSortItem extends RowItem implements Cloneable {
		String page;
		String webChName;
		String areaCode;

		@Override
		protected void myrefresh(RowItem o) {
			ChSortItem c = (ChSortItem) o;
			c.addData(page);
			c.addData(webChName);
			c.addData(areaCode);
		}

		public ChSortItem clone() {
			return (ChSortItem) super.clone();
		}
	}

	// ChSortItemを使ったJTable拡張
	private class JChSortTable extends JNETable {

		private static final long serialVersionUID = 1L;

		@Override
		public Object getValueAt(int row, int column) {

			if ( column != 0 ) {
				// １カラム目以外はそのまま
				return rowData.get(row).get(column);
			}

			if ( ! env.isPagerEnabled() ) {
				// ページャーが無効ならグレーアウト
				return CommonSwingUtils.getColoredString(PAGEDIS_COLOR,"-");
			}

			// １行ごとに背景色を互い違いにする。またソートが無効な場合はグレーアウト
			int page = 1 + getPageIndex(this.convertRowIndexToView(row));
			int pagemax = 1 + getPageIndex(rowData.size()-1);
			String color = ( ! jtgl_chsort.isSelected())?(PAGEDIS_COLOR):((page % 2 == 0)?(PAGEEN_ODD_COLOR):(PAGEEN_EVEN_COLOR));
			String text = String.format("%d / %d", page, pagemax);
			return CommonSwingUtils.getColoredString(color, text);

		}

		@Override
		public int getRowCount() {
			return rowData.size();
		}

		public JChSortTable(TableModel d, boolean b) {
			super(d,b);
		}
	}

	/*
	 * チャンネルの連番からページ番号を取得する
	 */
	public int getPageIndex(int n) {
		if (pageBreaks.size() > 0){
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
}
