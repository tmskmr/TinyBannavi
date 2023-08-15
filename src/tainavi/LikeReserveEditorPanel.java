package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;


/**
 * 予約ダイアログを目的ごとに３ブロックにわけたうちの「類似予約リスト」部分のコンポーネント
 * @since 3.22.2β
 */
public class LikeReserveEditorPanel extends JScrollPane {

	private static final long serialVersionUID = 1L;

	public void setDebug(boolean b) { debug = b;}
	private static boolean debug = true;

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	public static final int LIKERSVTABLE_DEFAULT = 0;
	public static final int LIKERSVTABLE_NONE = -1;
	public static final int LIKERSVTABLE_NOTSELECTED = -2;

	private static final String LIKERSVID_NONE			= "（類似予約なし）";
	private static final String LIKERSVID_NOTSELECTED	= "類似予約を選択しない";

	// レイアウト関連
	private static final int PARTS_HEIGHT = 25;

	private static final int LIKELIST_WIDTH = 730;
	private static final int LIKELIST_ROWS = 5;

	private static final int LRT_HEADER_WIDTH = 20;
	private static final int LRT_TITLE_WIDTH = 325;
	private static final int LRT_START_WIDTH = 140;
	private static final int LRT_RECORDER_WIDTH = 200;
	private static final int LRT_ENCODER_WIDTH = 80;

	// テーブルのカラムの定義
	public static enum LikeRsvColumn {
		TITLE		("予約名",	LRT_TITLE_WIDTH),
		START		("開始日時",	LRT_START_WIDTH),
		RECORDER	("レコーダ",	LRT_RECORDER_WIDTH),
		TUNER		("ｴﾝｺｰﾀﾞ",	LRT_ENCODER_WIDTH),
		;

		private String name;
		private int iniWidth;

		private LikeRsvColumn(String name, int iniWidth) {
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

		public boolean equals(String s) {
			return name.equals(s);
		}
	};

	// ログ関連
	private static final String MSGID = "[類似予約テーブル] ";
	//private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private LikeRsvTable jtbl_likersv = null;
	private LikeRsvRowHeader jrhdr_likersv = null;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private LikeReserveSelectable likesrvsel = null;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public LikeReserveEditorPanel() {

		super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		setBorder(new LineBorder(Color.BLACK, 1));
		setRowHeaderView(getLikeRsvRowHeader());
		setViewportView(getLikeRsvTable());

		Dimension dh = new Dimension(LRT_HEADER_WIDTH,0);
		getRowHeader().setPreferredSize(dh);

		getRowHeader().setVisible(true);

		Dimension d = new Dimension(LIKELIST_WIDTH, (int)(PARTS_HEIGHT*LIKELIST_ROWS+PARTS_HEIGHT*0.5));
		setPreferredSize(d);

		jrhdr_likersv.setRowHeight(jtbl_likersv.getRowHeight());

		// 選択行が表示されるよう自動で移動するようにする
		CommonSwingUtils.setSelectedRowShown(jtbl_likersv);
	}

	public void setLikeReserveSelector(LikeReserveSelectable o) {
		likesrvsel = o;
	}

	/*******************************************************************************
	 * その他
	 ******************************************************************************/

	public void setEnabledTable(boolean enabled) {
		jtbl_likersv.setEnabled(enabled);
	}


	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/***************************************
	 * 行選択した際のイベントリスナー
	 **************************************/

	private final ListSelectionListener lsl_likersvSelected = new ListSelectionListener() {

		@Override
		public void valueChanged(ListSelectionEvent e) {
		    if ( e.getValueIsAdjusting() ) {
		    	return;
		    }

		    if ( ! jtbl_likersv.isEnabled() ) {
				//return;
			}

		    int row = jtbl_likersv.getSelectedRow();
		    if ( row == LIKERSVTABLE_NOTSELECTED ) {
		    	return;
		    }

		    if (debug) System.out.println(DBGID+"選択された類似予約： "+row);
		    likesrvsel.doSelectLikeReserve(row);
		}
	};


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private LikeRsvRowHeader getLikeRsvRowHeader() {
		if ( jrhdr_likersv == null ) {
			jrhdr_likersv = new LikeRsvRowHeader();
		}
		return jrhdr_likersv;
	}

	private LikeRsvTable getLikeRsvTable() {
		if (jtbl_likersv == null) {

			// カラム名の初期化
			ArrayList<String> cola = new ArrayList<String>();
			for ( LikeRsvColumn lc : LikeRsvColumn.values() ) {
				if ( lc.getIniWidth() >= 0 ) {
					cola.add(lc.getName());
				}
			}
			final String[] colname = cola.toArray(new String[0]);

			//　テーブルの基本的な設定
			DefaultTableModel model = new DefaultTableModel(colname, 0);

			jtbl_likersv = new LikeRsvTable(model);
			jtbl_likersv.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);

			// 各カラムの幅を設定する
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jtbl_likersv.getColumnModel();
			TableColumn column = null;
			for ( LikeRsvColumn lc : LikeRsvColumn.values() ) {
				if ( lc.getIniWidth() < 0 ) {
					continue;
				}
				column = columnModel.getColumn(lc.ordinal());
				column.setPreferredWidth(ZMSIZE(lc.getIniWidth()));
			}

			// 行選択した際のイベントリスナー
			jtbl_likersv.getSelectionModel().addListSelectionListener(lsl_likersvSelected);
		}

		return jtbl_likersv;
	}

	private int ZMSIZE(int size){ return Env.ZMSIZE(size); }


	/*******************************************************************************
	 *
	 ******************************************************************************/

	public void setListItems(LikeReserveList lrl) {

		jtbl_likersv.setDataList(lrl);
		jrhdr_likersv.setDataList(lrl);

		((DefaultTableModel)jtbl_likersv.getModel()).fireTableDataChanged();
		((DefaultTableModel)jrhdr_likersv.getModel()).fireTableDataChanged();

		setEnabledTable(lrl.size() > 0);
	}

	public void setRowSelection(int row) {
		jtbl_likersv.setRowSelectionInterval(row, row);
	}

	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/

	private class LikeRsvTable extends JNETable {

		private static final long serialVersionUID = 1L;

		private LikeReserveList likersvlist = null;

		public LikeRsvTable(DefaultTableModel model) {
			super(model,true);
		}

		public void setDataList(LikeReserveList lrl) {
			likersvlist = lrl;
		}

		private Color disabledColor = new Color(180,180,180);

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component c = super.prepareRenderer(tcr, row, column);
			Color bgColor = null;
			if(isRowSelected(row)) {
				bgColor = super.getSelectionBackground();
			}
			else {
				if ( row > 0 && ! likersvlist.get(row-1).getRsv().getExec() ) {
					bgColor = disabledColor;
				}
				else {
					bgColor = super.getBackground();
				}
			}
			c.setBackground(bgColor);
			return c;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if ( row == 0 ) {
				// "類似予約なし"
				if ( column == LikeRsvColumn.TITLE.ordinal() ) {
					if ( likersvlist.size() == 0 ) {
						return LIKERSVID_NONE;
					}
					else {
						return LIKERSVID_NOTSELECTED;
					}
				}
				else if ( column == LikeRsvColumn.START.ordinal() ) {
					return "-";
				}
				else if ( column == LikeRsvColumn.RECORDER.ordinal() ) {
					return null;
				}
				return null;
			}

			// 類似予約は表示とデータが一行ずれる
			int drow = row-1;
			ReserveList rsv = likersvlist.get(drow).getRsv();
			HDDRecorder rec = likersvlist.get(drow).getRec();

			if ( column == LikeRsvColumn.TITLE.ordinal() ) {
				return rsv.getTitle();
			}
			else if ( column == LikeRsvColumn.START.ordinal() ) {
				return rsv.getRec_pattern()+" "+rsv.getAhh()+":"+rsv.getAmm();
			}
			else if ( column == LikeRsvColumn.RECORDER.ordinal() ) {
				return rec.getDispName();
			}
			else if ( column == LikeRsvColumn.TUNER.ordinal() ) {
				return rsv.getTuner();
			}
			return null;
		}

		// この設計はよくなかったか…
		@Override
		public int getSelectedRow() {
			return super.getSelectedRow()-1;
		}

		@Override
		public void setRowSelectionInterval(int index0, int index1) {
			super.setRowSelectionInterval(index0+1, index1+1);
		}

		@Override
		public int getRowCount() {
			if ( likersvlist == null ) {
				return 1;
			}
			return likersvlist.size()+1;
		}
	}

	private class LikeRsvRowHeader extends JTable {

		private static final long serialVersionUID = 1L;

		private LikeReserveList likersvlist = null;

		public LikeRsvRowHeader() {
			super();

			String[] colname = {""};
			DefaultTableModel model = new DefaultTableModel(colname,0);
			this.setModel(model);

			//this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			//this.setRowSelectionAllowed(false);
			this.setEnabled(false);

			DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {

				private static final long serialVersionUID = 1L;

				@Override
				public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
					Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
					c.setBackground(table.getTableHeader().getBackground());
					return c;
				};
			};
			cr.setHorizontalAlignment(JLabel.CENTER);
			cr.setOpaque(true);
			this.getColumnModel().getColumn(0).setCellRenderer(cr);
		}

		public void setDataList(LikeReserveList lrl) {
			likersvlist = lrl;
		}

		@Override
		public Object getValueAt(int row, int column) {
			return (row==0) ? "-" : String.valueOf(row);
		}

		@Override
		public int getRowCount() {
			if ( likersvlist  == null ) {
				return 1;
			}
			return likersvlist.size()+1;
		}
	}

}
