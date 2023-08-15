package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import tainavi.TVProgramIterator.IterationType;

/**
 * CHコード設定のタブ
 * @since 3.15.4β　{@link Viewer}から分離
 */
public abstract class AbsChannelDatSettingView extends JScrollPane {

	private static final long serialVersionUID = 1L;

	public static String getViewName() { return "CHコード設定"; }

	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract TVProgramList getTVProgramList();
	protected abstract ChannelSort getChannelSort();
	protected abstract HDDRecorderList getHDDRecorderList();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract Component getParentComponent();

	protected abstract void ringBeep();


	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	// オブジェクト
	//private final Env env = getEnv();
	private final TVProgramList tvprograms = getTVProgramList();
	private final ChannelSort chsort = getChannelSort();
	private final HDDRecorderList recorders = getHDDRecorderList();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	// メソッド
	//private void StdAppendMessage(String message) { System.out.println(message); }
	//private void StdAppendError(String message) { System.err.println(message); }
	private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連

	//private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	//private static final int BLOCK_SEP_HEIGHT = 75;

	private static final int LABEL_WIDTH = 200;
	private static final int BUTTON_WIDTH = 75;
	private static final int BUTTON_WIDTH_LONG = 200;

	private static final int TABLE_WIDTH = ChDatColumn.WEBCHNAME.getIniWidth()+ChDatColumn.RECCHNAME.getIniWidth()+ChDatColumn.CHCODE.getIniWidth()+ChDatColumn.BTYPE.getIniWidth()+ChDatColumn.AUTO.getIniWidth()+20;

	private static final int UPDATE_WIDTH = 250;
	private static final int HINT_WIDTH = 750;

	private static final int PANEL_WIDTH = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH+TABLE_WIDTH+SEP_WIDTH;

	// テキスト

	private static final String TEXT_HINT =
			"「Web番組表の放送局名」と「レコーダの放送局名」を関連付けて予約情報のやりとりができるようにしてください。"+
			"詳細はwikiを参照してください（http://sourceforge.jp/projects/tainavi/wiki/FAQ#CHCODE）";

	// ログ関連

	private static final String MSGID = "["+getViewName()+"] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	// カラム設定

	private static enum ChDatColumn {
		WEBCHNAME	("Web番組表の放送局名",	250),
		RECCHNAME	("レコーダの放送局名",		175),
		CHCODE		("放送局コード",			175),
		BTYPE		("放送波の種類",			100),
		AUTO		("AUTO",				75),
		;

		private String name;
		private int iniWidth;

		private ChDatColumn(String name, int iniWidth) {
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
	}

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JPanel jPanel_chDatSetting = null;
	private JPanel jPanel_update = null;

	private JButton jButton_update = null;

	private JLabel jLabel_recorderId = null;
	private JComboBox jComboBox_recorderId = null;

	private JScrollPane jScrollPane_entries = null;
	private ChDatTable jTable_entries = null;
	private DefaultCellEditor editorCombo_recchname = null;
	private DefaultCellEditor editorField_recchname = null;

	private JButton jButton_upCenter = null;
	private JButton jButton_downCenter = null;

	private JButton jButton_removeCenter = null;

	private JButton jButton_addCenter = null;
	private JTextFieldWithPopup jTextField_addCenter = null;

	private JTextAreaWithPopup jta_help = null;
	private JTextAreaWithPopup jta_chdathelp = null;

	// コンポーネント以外

	private HDDRecorder selectedRecorder = null;

	private final RowItemList<ChDatItem> rowData = new RowItemList<ChDatItem>();

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsChannelDatSettingView() {

		super();

		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.setColumnHeaderView(getJPanel_update());
		this.setViewportView(getJPanel_chDatSetting());

	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	/**
	 * 更新を確定する本体
	 */
	private void updateChDatSetting() {

		TatCount tc = new TatCount();

		StWin.clear();

		new SwingBackgroundWorker(false) {

			@Override
			protected Object doWorks() throws Exception {

				StWin.appendMessage(MSGID+"設定を保存します");

				String recId = selectedRecorder.getRecorderId();

				//DefaultTableModel model = (DefaultTableModel) jTable_entries.getModel();
				ArrayList<String> webChName = new ArrayList<String>();
				ArrayList<String> recChName = new ArrayList<String>();
				ArrayList<String> chCode = new ArrayList<String>();
				for ( ChDatItem c: rowData ) {
					if ( c.recChName.equals("") && c.chCode.equals("") ) {
						continue;	// 無効なエントリ
					}
					if ( selectedRecorder.isChCodeNeeded() && c.chCode.equals("") ) {
						MWin.appendError(ERRID+"放送局コードを指定してください： "+recId+", "+c.recChName);
						ringBeep();
						return null;
					}

					webChName.add(c.webChName);

					recChName.add(c.recChName);

					String chcode = null;
					if ( ! selectedRecorder.isRecChNameNeeded() ) {
						// NULL
						if ( c.recChName.equals("-") ) {
							chcode = c.chCode;
						}
						else {
							chcode = c.recChName;
						}
					}
					else if ( selectedRecorder.isChValueAvailable() || selectedRecorder.isChCodeNeeded() ) {
						// EDCB, TvRock, REGZA || RD
						chcode = c.chCode;
					}
					else {
						// DIGA
						chcode = c.recChName;
					}
					if ( selectedRecorder.isBroadcastTypeNeeded() ) {
						switch (c.bType) {
						case NONE:
							break;
						default:
							// TvRock対応
							chcode = c.bType.getName()+":"+chcode;
							break;
						}
					}
					chCode.add(chcode);
				}

				if (debug) {
					for ( int n=0; n<webChName.size(); n++ ) {
						System.out.println(String.format(DBGID+"[SAVE] %s,%s,%s",webChName.get(n),recChName.get(n),chCode.get(n)));
					}
				}

				ChannelCode cc = new ChannelCode(recId);
				cc.save(webChName, recChName, chCode);

				// ここがあるのでSwingBackgroundWorkerを使わざるをえない
				for ( HDDRecorder rec : recorders.findPlugin(recId) ) {
					// 関連するプラグインを全部リロードする
					rec.getChCode().load(false);	// ログ出力あり
					rec.GetRdReserve(false);
				}
				return null;
			}

			@Override
			protected void doFinally() {
				StWinSetVisible(false);
			}
		}.execute();

		StWinSetLocationCenter(parent);
		StWinSetVisible(true);

		MWin.appendMessage(String.format(MSGID+"更新が完了しました。所要時間： %.2f秒",tc.end()));
		//MWin.appendError(MSGID+"【重要】 再起動してCHコード設定の変更をレコーダプラグインに反映してください。");
		//ringBeep();
	}

	/**
	 * <P>RdChannelCode.datをテーブルに展開する（CH設定からも呼ばれる）
	 * <P>ここで取得した selectedRecorder は各所で使いまわす。レコーダ設定の変更には注意が必要
	 */
	public void updateChannelDatTable()	{
		//
		String recId = (String) jComboBox_recorderId.getSelectedItem();

		if ( recId == null ) {
			MWin.appendError(ERRID+"レコーダが選択できません");
			return;
		}

		ArrayList<HDDRecorder> ra = recorders.findPlugin(recId);
		if ( ra.size() == 0 ) {
			MWin.appendError(ERRID+"指定のレコーダのプラグインがみつかりません： "+recId);
			return;
		}

		// 選択したレコーダを保存する
		selectedRecorder = ra.get(0);

		ChannelCode cc = new ChannelCode(recId);
		cc.load(false);	// ログ出力なし

		Boolean availableauto = (selectedRecorder.getChValue().size() != 0) && selectedRecorder.isChCodeNeeded();

		jTable_entries.setChValueAvailable(selectedRecorder.isChValueAvailable());
		jTable_entries.setChCodeEnabled(selectedRecorder.isChCodeNeeded());
		jTable_entries.setBroadcastTypeEnabled(selectedRecorder.isBroadcastTypeNeeded());

		rowData.clear();

		/*
		 *  従来のフォーマットとの互換を保つため、いろいろ加工が入る。表示と保存も気を付けなければならない。
		 */

		// 定義にある放送局をリストアップする
		for ( String webChName : cc.getChNames() ) {
			ChDatItem sa = new ChDatItem();
			sa.webChName = webChName;
			sa.chCode = cc.getCH_WEB2CODE(webChName);
			sa.recChName = cc.getCH_CODE2REC(sa.chCode);

			if ( selectedRecorder.isChValueAvailable() ) {
				String s = selectedRecorder.value2text(selectedRecorder.getChValue(), sa.chCode);
				if ( s != null && s.length() > 0 ) {
					sa.recChName = s;
				}
			}

			sa.bType = null;
			if ( selectedRecorder.isBroadcastTypeNeeded() ) {
				// TvRock対応
				if ( sa.chCode != null ) {
					String[] d = sa.chCode.split(":",2);
					if ( d.length == 2 ) {
						sa.chCode = d[1];
						sa.bType = BroadcastType.get(d[0]);
					}
				}
			}
			if ( sa.bType == null ) {
				sa.bType = BroadcastType.NONE;
			}

			if ( ! selectedRecorder.isRecChNameNeeded() ) {
				//
			}
			else if ( ! selectedRecorder.isChCodeNeeded() && ! selectedRecorder.isChValueAvailable() ) {
				sa.chCode = "";
			}

			sa.availableAuto = availableauto;
			sa.fireChanged();

			rowData.add(sa);
		}

		// 定義にない放送局をリストアップする
		{
			TVProgramIterator pli = tvprograms.getIterator().build(chsort.getClst(), IterationType.ALL);
			for ( ProgList pl : pli ) {
				boolean exists = false;
				for ( ChDatItem c : rowData ) {
					if ( pl.Center.equals(c.webChName) ) {
						exists = true;
						break;
					}
				}
				if ( ! exists ) {
					ChDatItem sa = new ChDatItem();
					sa.webChName = pl.Center;
					if ( selectedRecorder.isRecChNameNeeded() ) {
						sa.recChName = "";
					}
					else {
						sa.recChName = sa.webChName;
					}
					sa.chCode = "";
					sa.bType = BroadcastType.NONE;
					sa.availableAuto = availableauto;
					sa.fireChanged();
					rowData.add(sa);
				}
			}
		}

		if ( selectedRecorder.isChValueAvailable() ) {
			JComboBox combo = ((JComboBox) editorCombo_recchname.getComponent());
			combo.removeAllItems();
			combo.addItem("");
			for ( TextValueSet t : selectedRecorder.getChValue() ) {
				combo.addItem(t.getText());
			}
			jTable_entries.getColumn(ChDatColumn.RECCHNAME.getName()).setCellEditor(editorCombo_recchname);
		}
		else {
			//popupを無効にできないかなー
			jTable_entries.getColumn(ChDatColumn.RECCHNAME.getName()).setCellEditor(editorField_recchname);
		}

		((DefaultTableModel) jTable_entries.getModel()).fireTableDataChanged();

		// ヘルプの表示
		jta_chdathelp.setText(selectedRecorder.getChDatHelp());
	}

	/**
	 * レコーダコンボボックスを設定しなおす（レコーダ設定タブからも呼ばれる）
	 */
	public void updateRecorderComboBox() {

		jComboBox_recorderId.removeItemListener(il_recorderChanged);	// 停止

		ArrayList<String> ra = new ArrayList<String>();
		for ( HDDRecorder r : recorders ) {
			if ( r.isBackgroundOnly() ) {
				continue;
			}

			if ( ! ra.contains(r.getRecorderId()) ) {
				ra.add(r.getRecorderId());
			}
		}

		// 初期値を設定
		jComboBox_recorderId.removeAllItems();;
		for ( String s : ra ) {
			jComboBox_recorderId.addItem(s);
		}

		jComboBox_recorderId.addItemListener(il_recorderChanged);	// 再開
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * 更新を確定する
	 */
	private final ActionListener al_update = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateChDatSetting();
		}
	};

	/**
	 * レコーダが選択された
	 */
	private final ItemListener il_recorderChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println(DBGID+"il_recorderChanged itemStateChanged "+e.paramString());
			if ( e.getStateChange() == ItemEvent.SELECTED ) {
				updateChannelDatTable();
			}
		}
	};

	private final ActionListener al_removeCenter = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int[] vrows = jTable_entries.getSelectedRows();
			Arrays.sort(vrows);
			for ( int i=vrows.length-1; i>=0; i-- ) {
				int vrow = vrows[i];
				if (vrow >=0 && vrow <= jTable_entries.getRowCount()-1) {
					rowData.remove(vrow);
				}
			}
			((DefaultTableModel) jTable_entries.getModel()).fireTableDataChanged();
		}
	};

	private final ActionListener al_upCenter = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int vrow = jTable_entries.getSelectedRow();
			int cnt = jTable_entries.getSelectedRowCount();
			if ( rowData.up(vrow, cnt) ) {
				((DefaultTableModel) jTable_entries.getModel()).fireTableDataChanged();
				jTable_entries.setRowSelectionInterval(vrow-1, vrow-1+(cnt-1));
			}
		}
	};

	private final ActionListener al_downCenter = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int vrow = jTable_entries.getSelectedRow();
			int cnt = jTable_entries.getSelectedRowCount();
			if ( rowData.down(vrow, cnt) ) {
				((DefaultTableModel) jTable_entries.getModel()).fireTableDataChanged();
				jTable_entries.setRowSelectionInterval(vrow+1, vrow+1+(cnt-1));
			}
		}
	};

	/**
	 * 放送局の強制追加
	 */
	private final MouseListener ml_addCenter = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			// 空文字列は対象外
			String newCenter = jTextField_addCenter.getText().trim();
			if (newCenter.length() == 0) {
				return;
			}

			// 重複チェック
			for ( ChDatItem c : rowData ) {
				if (c.webChName.equals(newCenter)) {
					JOptionPane.showConfirmDialog(null, "放送局名が重複しています。", "警告", JOptionPane.CLOSED_OPTION);
					return;
				}
			}

			// 表へ追加
			Boolean availableauto = false;
			for ( HDDRecorder recorder : recorders ) {
				if ( recorder.getRecorderId().equals(selectedRecorder.getRecorderId())) {
					availableauto = (recorder.getChValue().size() != 0) && recorder.isChCodeNeeded();
				}
			}

			ChDatItem sa = new ChDatItem();
			sa.webChName = newCenter;
			sa.chCode = "";
			sa.recChName = "";
			sa.bType = BroadcastType.NONE;
			sa.availableAuto = availableauto;
			sa.fireChanged();
			rowData.add(sa);

			jTextField_addCenter.setText("");

			((DefaultTableModel) jTable_entries.getModel()).fireTableDataChanged();
		}
	};


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

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

	private JPanel getJPanel_chDatSetting() {
		if (jPanel_chDatSetting == null)
		{
			jPanel_chDatSetting = new JPanel();
			jPanel_chDatSetting.setLayout(new SpringLayout());

			//
			int y = SEP_HEIGHT;
			int x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJLabel_recorderId("レコーダ種別"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJComboBox_recorderId(), LABEL_WIDTH, PARTS_HEIGHT, x+=LABEL_WIDTH+SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJta_chdathelp(), 550, PARTS_HEIGHT*2, x+LABEL_WIDTH+SEP_WIDTH, y);

			y+=(PARTS_HEIGHT*2+SEP_HEIGHT);
			x = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH;
			int table_h = 450;
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJScrollPane_entries(), TABLE_WIDTH, table_h, x, y);

			if ( jComboBox_recorderId.getItemCount() > 0 ) {
				updateChannelDatTable();
			}

			int yz = y+table_h/2;
			int xz = x - (BUTTON_WIDTH+SEP_WIDTH);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJButton_upCenter("上へ"), BUTTON_WIDTH, PARTS_HEIGHT, xz, yz-50);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJButton_downCenter("下へ"), BUTTON_WIDTH, PARTS_HEIGHT, xz, yz+50);

			y+=(table_h-PARTS_HEIGHT);
			xz = x - (BUTTON_WIDTH_LONG+SEP_WIDTH);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJButton_removeCenter("放送局名を削除する"), BUTTON_WIDTH_LONG, PARTS_HEIGHT, xz, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJButton_addCenter("放送局名を追加する"), BUTTON_WIDTH_LONG, PARTS_HEIGHT, xz, y);
			CommonSwingUtils.putComponentOn(jPanel_chDatSetting, getJTextField_addCenter(), ChDatColumn.WEBCHNAME.getIniWidth(), PARTS_HEIGHT, x, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			Dimension d = new Dimension(PANEL_WIDTH,y);
			jPanel_chDatSetting.setPreferredSize(d);
		}

		return jPanel_chDatSetting;
	}

	private JButton getJButton_update(String s)
	{
		if (jButton_update == null) {
			jButton_update = new JButton(s);

			jButton_update.addActionListener(al_update);
		}
		return(jButton_update);
	}

	// 放送局を上へ・下へ
	private JButton getJButton_upCenter(String s) {
		if (jButton_upCenter == null) {
			jButton_upCenter = new JButton(s);
			jButton_upCenter.addActionListener(al_upCenter);
		}
		return(jButton_upCenter);
	}

	private JButton getJButton_downCenter(String s) {
		if (jButton_downCenter == null) {
			jButton_downCenter = new JButton(s);
			jButton_downCenter.addActionListener(al_downCenter);
		}
		return(jButton_downCenter);
	}

	// 削除しちゃうよ
	private JButton getJButton_removeCenter(String s) {
		if (jButton_removeCenter == null) {
			jButton_removeCenter = new JButton(s);
			jButton_removeCenter.addActionListener(al_removeCenter);
		}
		return jButton_removeCenter;
	}

	// Web番組表に存在しない放送局の強制追加
	private JButton getJButton_addCenter(String s) {
		if (jButton_addCenter == null) {
			jButton_addCenter = new JButton(s);

			jButton_addCenter.addMouseListener(ml_addCenter);
		}
		return(jButton_addCenter);
	}
	private JTextField getJTextField_addCenter() {
		if (jTextField_addCenter == null) {
			jTextField_addCenter = new JTextFieldWithPopup();
		}
		return jTextField_addCenter;
	}

	//
	private JLabel getJLabel_recorderId(String s) {
		if (jLabel_recorderId == null) {
			jLabel_recorderId = new JLabel();
			jLabel_recorderId.setText(s);
		}
		return jLabel_recorderId;
	}

	private JComboBox getJComboBox_recorderId() {
		if (jComboBox_recorderId == null) {
			jComboBox_recorderId = new JComboBox();
			jComboBox_recorderId.setEditable(false);

			updateRecorderComboBox();	// 初期化

			jComboBox_recorderId.addItemListener(il_recorderChanged);
		}
		return jComboBox_recorderId;
	}

	//
	private JScrollPane getJScrollPane_entries() {
		if (jScrollPane_entries == null) {
			jScrollPane_entries = new JScrollPane();
			jScrollPane_entries.setViewportView(getJTable_entries());
			jScrollPane_entries.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_entries);
	}

	private JTable getJTable_entries() {
		if (jTable_entries == null) {

			jTable_entries = new ChDatTable(rowData) ;

			//　テーブルの基本的な設定
			ArrayList<String> cola = new ArrayList<String>();
			for ( ChDatColumn rc : ChDatColumn.values() ) {
				if ( rc.getIniWidth() >= 0 ) {
					cola.add(rc.getName());
				}
			}
			DefaultTableModel model = new DefaultTableModel(cola.toArray(new String[0]), 0);
			jTable_entries.setModel(model);

			jTable_entries.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
			jTable_entries.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			jTable_entries.getTableHeader().setReorderingAllowed(false);
			jTable_entries.putClientProperty("terminateEditOnFocusLost", true);	// これやらないと、編集が確定したように見えて確定しない
			//jTable_entries.setRowHeight(jTable_entries.getRowHeight()+4);

			// 各カラムの幅
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_entries.getColumnModel();
			TableColumn column = null;
			for ( ChDatColumn rc : ChDatColumn.values() ) {
				if ( rc.getIniWidth() < 0 ) {
					continue;
				}
				columnModel.getColumn(rc.ordinal()).setPreferredWidth(rc.getIniWidth());;
			}

			// レコーダの放送局名
			editorCombo_recchname = new DefaultCellEditor(new RecorderChannelNameComboBox());
			editorCombo_recchname.setClickCountToStart(0);
			editorField_recchname = new DefaultCellEditor(new RecoderCnannelNameTextField());
			editorField_recchname.setClickCountToStart(1);
			jTable_entries.getColumn(ChDatColumn.RECCHNAME.getName()).setCellEditor(editorField_recchname);

			// 放送局コード
			jTable_entries.getColumn(ChDatColumn.CHCODE.getName()).setCellEditor(new EditorColumn());

			// 放送局コードコンボボックス
			BroadcastTypeComboBox bTypeBox = new BroadcastTypeComboBox();
			jTable_entries.getColumn(ChDatColumn.BTYPE.getName()).setCellEditor(new DefaultCellEditor(bTypeBox));

			// 自動設定ボタン
			ButtonColumn buttonColumn = new ButtonColumn();
			column = jTable_entries.getColumn(ChDatColumn.AUTO.getName());
			column.setCellRenderer(buttonColumn);
			column.setCellEditor(buttonColumn);
			column.setResizable(false);
		}
		return jTable_entries;
	}

	//
	private JTextAreaWithPopup getJta_chdathelp() {
		jta_chdathelp = CommonSwingUtils.getJta(this,2,0);
		jta_chdathelp.setForeground(Color.BLUE);
		return jta_chdathelp;
	}

	//
	private JTextAreaWithPopup getJta_help() {
		if ( jta_help == null ) {
			jta_help = CommonSwingUtils.getJta(this,2,0);
			jta_help.setText(TEXT_HINT);
		}
		return jta_help;
	}



	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/

	private class ChDatItem extends RowItem implements Cloneable {
		String webChName;
		String recChName;
		String chCode;
		BroadcastType bType;
		Boolean availableAuto;

		@Override
		protected void myrefresh(RowItem o) {
			ChDatItem c = (ChDatItem) o;
			c.addData(webChName);
			c.addData(recChName);
			c.addData(chCode);
			c.addData(bType);
			c.addData(availableAuto);
		}

		@Override
		public ChDatItem clone() {
			return (ChDatItem) super.clone();
		}
	}

	private class ChDatTable extends JTable {

		private static final long serialVersionUID = 1L;

		private final Color evenColor = new Color(240,240,255);
		private final Color oddColor = super.getBackground();

		private final Color disabledOddColor = new Color(200,200,200);
		private final Color disabledEvenColor = new Color(180,180,180);

		private RowItemList<ChDatItem> rowdata = null;

		public ChDatTable(RowItemList<ChDatItem> rowdata) {
			super();
			this.rowdata = rowdata;

			// フォントサイズ変更にあわせて行の高さを変える
			this.addPropertyChangeListener("font", new RowHeightChangeListener(8));

			// 行の高さの初期値の設定
			this.firePropertyChange("font", "old", "new");
		}

		public void setChValueAvailable(boolean b) { chvalueavailable = b; }
		private boolean chvalueavailable = true;

		public void setChCodeEnabled(boolean b) { chcodeenabled = b; }
		private boolean chcodeenabled = true;

		public void setBroadcastTypeEnabled(boolean b) { broadcasttypeenabled = b; }
		private boolean broadcasttypeenabled = false;

		@Override
		public boolean isCellEditable(int row, int column) {
			if ( column == ChDatColumn.WEBCHNAME.getColumn() ) {
				return false;	// Web番組表の放送局名は編集できない
		  	}
			else if ( ! chcodeenabled && column == ChDatColumn.CHCODE.getColumn() ) {
				return false;
			}
			else if ( ! broadcasttypeenabled && column == ChDatColumn.BTYPE.getColumn() ) {
				return false;
			}
			return true;
		}

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component comp = super.prepareRenderer(tcr, row, column);

			Color fg = null;
			Color bg = null;

			boolean evenline = (row%2 == 1);

			if ( column == ChDatColumn.WEBCHNAME.getColumn() ) {
				fg = Color.BLUE;
				bg = (evenline)?(disabledEvenColor):(disabledOddColor);
			}
			else if ( column == ChDatColumn.RECCHNAME.getColumn() ) {
				//
			}
			else if ( column == ChDatColumn.CHCODE.getColumn() ) {
				if ( ! chcodeenabled ) bg = (evenline)?(disabledEvenColor):(disabledOddColor);
			}
			else if ( column == ChDatColumn.BTYPE.getColumn() ) {
				if ( ! broadcasttypeenabled ) bg = (evenline)?(disabledEvenColor):(disabledOddColor);
			}
			if(isRowSelected(row)) {
				if (fg==null) fg = this.getSelectionForeground();
				bg = this.getSelectionBackground();
			}
			else {
				if (fg==null) fg = this.getForeground();
				if (bg==null) bg = (evenline)?(evenColor):(oddColor);
			}

			comp.setForeground(fg);
			comp.setBackground(bg);

			return comp;
		}

		@Override
		public int getRowCount() { return rowdata.size(); }

		@Override
		public Object getValueAt(int row, int column) {
			ChDatItem c = rowdata.get(row);
			if ( column == ChDatColumn.CHCODE.getColumn() ) {
				String chcode = null;
				if ( chcodeenabled || chvalueavailable || ( ! chcodeenabled && c.recChName.equals("-")) ) {
					chcode = c.chCode;
				}
				else {
					chcode = c.recChName;
				}
				if ( broadcasttypeenabled ) {
					if ( c.bType != BroadcastType.NONE ) {
						chcode = c.bType.getName()+":"+chcode;
					}
				}
				return chcode;
			}
			else if ( column == ChDatColumn.BTYPE.getColumn() ) {

				// 設定値から放送波の種別を判断できるレコーダもある
				String selected = selectedRecorder.getRecorderId();
				if ( selected != null ) {
					BroadcastType bt = BroadcastType.get(selected, c.recChName, c.chCode);
					if ( bt != null ) {
						return bt.getName();
					}
				}

				return c.bType.getName();
			}
			return rowdata.get(row).get(column);
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			ChDatItem c = rowdata.get(row);
			if ( column == ChDatColumn.WEBCHNAME.getColumn() ) {
				c.webChName = (String) aValue;
			}
			else if ( column == ChDatColumn.RECCHNAME.getColumn() ) {
				c.recChName = (String) aValue;
				if ( chvalueavailable ) {
					// EDCB対応
					if ( c.recChName == null || c.recChName.length() == 0 ) {
						// 消したいらしいよ
						c.chCode = "";
					}
					else {
						String chcode = selectedRecorder.text2value(selectedRecorder.getChValue(), c.recChName);
						if ( chcode != null && chcode.length() > 0 ) {
							// chvalueに情報があったよ
							c.chCode = chcode;
						}
						else {
							if ( c.chCode == null || c.chCode.length() == 0 ) {
								// chvalueに情報がないよ
								c.chCode = c.recChName;
							}
						}
					}
				}
			}
			else if ( column == ChDatColumn.CHCODE.getColumn() ) {
				c.chCode = (String) aValue;
			}
			else if ( column == ChDatColumn.BTYPE.getColumn() ) {
				c.bType = BroadcastType.get((String) aValue);
			}
			else if ( column == ChDatColumn.AUTO.getColumn() ) {
				c.availableAuto = (Boolean) aValue;
			}
			c.fireChanged();
			return;
		}
	}

	/**
	 * レコーダの放送局名入力フィールド
	 */
	private class RecoderCnannelNameTextField extends JTextFieldWithPopup {

		private static final long serialVersionUID = 1L;

		private final AncestorListener al_recChNameChanged = new AncestorListener() {
			@Override
			public void ancestorRemoved(AncestorEvent e) {
				if (debug) System.out.println(DBGID+"al_recChNameChanged/ancestorRemoved "+e.toString());
				int row = jTable_entries.getSelectedRow();
				if ( row >= 0 ) {
					// 一行まるごと更新
					jTable_entries.clearSelection();
					jTable_entries.setRowSelectionInterval(row, row);
				}
			}
			@Override
			public void ancestorMoved(AncestorEvent e) {
				if (debug) System.out.println(DBGID+"al_recChNameChanged/ancestorMoved "+e.toString());
			}
			@Override
			public void ancestorAdded(AncestorEvent e) {
				if (debug) System.out.println(DBGID+"al_recChNameChanged/ancestorAdded "+e.toString());
			}
		};

		public RecoderCnannelNameTextField() {
			super();
			this.setEditable(true);

			this.addAncestorListener(al_recChNameChanged);
		}

	}

	/**
	 * レコーダの放送局名選択コンボボックス
	 */
	private class RecorderChannelNameComboBox extends JComboBoxWithPopup {

		private static final long serialVersionUID = 1L;

		private final ItemListener il_recChNameChanged = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (debug) System.out.println(DBGID+"il_recChNameChanged "+e.paramString());
				if ( e.getStateChange() == ItemEvent.DESELECTED ) {
					int row = jTable_entries.getSelectedRow();
					if ( row >= 0 && row < jTable_entries.getRowCount()) {
						// 一行まるごと更新
						jTable_entries.clearSelection();
						jTable_entries.setRowSelectionInterval(row, row);
					}
				}
			}
		};

		@Override
		public void setSelectedItem(Object anObject) {
			for ( int i=0; i<getItemCount(); i++ ) {
				if ( getItemAt(i).toString().equals(anObject.toString()) ) {
					super.setSelectedItem(anObject);
					return;
				}
			}
			if (anObject != null && ((String) anObject).length() > 0) {
				addItem(anObject);
				if (debug) System.out.println(DBGID+"選択肢を追加： "+anObject.toString());
			}
			super.setSelectedItem(anObject);
		}

		public RecorderChannelNameComboBox() {
			super();
			this.setEditable(false);
			this.setMaximumRowCount(15);

			this.addItemListener(il_recChNameChanged);
		}
	}

	/**
	 * 地上波/BS/CS選択コンボボックス
	 */
	private class BroadcastTypeComboBox extends JComboBox {

		private static final long serialVersionUID = 1L;

		private final ItemListener il_btypeChanged = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (debug) System.out.println(DBGID+"il_btypeChanged "+e.paramString());
				if ( e.getStateChange() == ItemEvent.SELECTED ) {
					int row = jTable_entries.getSelectedRow();
					if ( row >= 0 ) {
						// 一行まるごと更新
						jTable_entries.clearSelection();
						jTable_entries.setRowSelectionInterval(row, row);
					}
				}
			}
		};

		public BroadcastTypeComboBox() {
			super();
			this.setEditable(false);
			for ( BroadcastType b : BroadcastType.values() ) {
				this.addItem(b.getName());
			}

			this.addItemListener(il_btypeChanged);
		}
	}


	/**
	 * AUTOボタン
	 */
	private class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

		private static final long serialVersionUID = 1L;

		private static final String LABEL = "SET";

		private final JButton renderButton;
		private final JButton editorButton;
		private Boolean cellValue = Boolean.FALSE;

		public ButtonColumn() {
			super();
			renderButton = new JButton(LABEL);
			editorButton = new JButton(LABEL);
			editorButton.addActionListener(al_set);
		}

		private final ActionListener al_set = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();

				int row = jTable_entries.getSelectedRow();

				ChDatItem c = rowData.get(row);

				String chCode = c.chCode;
				if ( chCode.length() == 0 ) {

					// レコーダプラグインが持っているチャンネルコードバリューを検索する
					if ( selectedRecorder.isChValueAvailable() ) {
						// 新バージョン
						String recChName = c.recChName;
						for ( TextValueSet t : selectedRecorder.getChValue() ) {
							if ( recChName.equals(t.getText()) ) {
								c.chCode = t.getValue();
								break;
							}
						}
					}
					else if ( selectedRecorder.getChValue().size() > 0 ) {
						// 旧バージョン（置き換えていきたい）
						String recChName = c.recChName;
						if ( selectedRecorder.getRecorderId().startsWith("DIGA ") ) {
							// for DIGA ONLY
							for ( TextValueSet t : selectedRecorder.getChValue() ) {
								if ( recChName.startsWith(t.getText()+" ") ) {
									String val = t.getValue();
									if ( val != null ) {
										c.chCode = recChName.replaceFirst(t.getText()+" ","")+":"+val;
									}
									break;
								}
							}
						}
						else {
							// for RD ONLY
							for ( TextValueSet t : selectedRecorder.getChValue() ) {
								if ( t.getText().equals(recChName) ) {
									String val = t.getValue();
									if ( val != null ) {
										c.chCode = val;
									}
									break;
								}
							}
						}
					}

					c.fireChanged();
					jTable_entries.clearSelection();
					jTable_entries.setRowSelectionInterval(row, row);
				}
			}
		};

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			ChDatItem c = rowData.get(row);
			renderButton.setEnabled(c.availableAuto);
			return renderButton;
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			ChDatItem c = rowData.get(row);
			editorButton.setEnabled(c.availableAuto);
			cellValue = c.availableAuto;
			return editorButton;
		}

		public Object getCellEditorValue() {
			return cellValue;
		}
	}
}
