package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import tainavi.TVProgram.ProgSubtype;
import tainavi.TVProgram.ProgType;
import tainavi.plugintv.Syobocal;

/**
 * ChannelConver.datを編集するView
 */
public abstract class AbsChannelConvertView extends JScrollPane {

	private static final long serialVersionUID = 1L;
	
	public static String getViewName() { return "CHコンバート設定"; } 

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/
	
	protected abstract Env getEnv();
	protected abstract TVProgramList getProgPlugins();
	protected abstract ChannelConvert getChannelConvert();

	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/
	
	private final Env env = getEnv();
	private final TVProgramList progPlugins = getProgPlugins();
	private final ChannelConvert chconv = getChannelConvert();
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連
	
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	
	private static final int UPDATE_WIDTH = 250;
	private static final int HINT_WIDTH = 750;
	
	private static final int LABEL_WIDTH = 0;

	private static final int TABLE_WIDTH = 950;
	private static final int TABLE_ORIG_WIDTH = 200;
	private static final int TABLE_RELATED_WIDTH = 250;
	private static final int TABLE_OPTION_WIDTH = 75;
	private static final int TABLE_EDIT_WIDTH = TABLE_WIDTH-TABLE_ORIG_WIDTH-TABLE_RELATED_WIDTH-TABLE_OPTION_WIDTH*2;
	private static final int TABLE_HEIGHT = 450;

	private static final int SELECTOR_WIDTH = 250;
	private static final int AREA_WIDTH = 100;
	private static final int RELATION_WIDTH = TABLE_WIDTH-SELECTOR_WIDTH-AREA_WIDTH-SEP_WIDTH*2;
	
	private static final int PANEL_WIDTH = SEP_WIDTH+UPDATE_WIDTH+SEP_HEIGHT*4+HINT_WIDTH+SEP_WIDTH;
	
	// テキスト
	
	private static final String TEXT_HINT =
			"鯛ナビで表示される放送局名を設定します。各番組表の放送局名を同じ名前に統一すると、番組表を切り替えても検索条件を変える必要がなくなります。"+
			"またしょぼかる連携が必要な場合は「Syobocal」の放送局名とも一致させてください。有効な場合は「しょぼかる」欄が「連携有効」となります。";
	private static final String TEXT_NOTE =
			"3.16以前から継続してご利用の場合は先にCH設定タブで各番組表の放送局リストの再取得を行ってください。"+
			"これは、3.16以前の版では「変換前の放送局名」の情報が正しくないためです。";

	//private static final String TEXT_UPDATE = "更新を確定する";
	private static final String TEXT_UPDATE = "（更新はまだ実装してない）";

	private static final String TEXT_CONVERTED = "あり";

	private static final String TEXT_SYOBO_EN = "連携有効";

	private static final String TEXT_AREASELECT_UNSUPPORTED = "（エリア非対応）";
	
	private static final String TEXT_RELATEDCOLUMN = "放送局名が変わらない他の番組表(→CH設定)";
	private static final String TEXT_RELATEDCOLUMN_SYOBO = "しょぼかる連携が有効な番組表";
	private static final String TEXT_MATCHEDCOLUMN = "しょぼかる";
	private static final String TEXT_MATCHEDCOLUMN_SYOBO = "-";
	
	private static final String UNAVAILABLE_CENTER = "（選択できません）";

	// ログ関連
	
	private static final String MSGID = "["+getViewName()+"] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;
	
	// カラム設定
	
	private static enum ChConvColumn {
		ORIG		("[a]番組表サイト上での放送局名",							TABLE_ORIG_WIDTH),
		EDIT		("[b]鯛ナビでの放送局名(CH設定での「Web番組表の放送局名」)",	TABLE_EDIT_WIDTH),
		CONVERTED	("a→b変換",												TABLE_OPTION_WIDTH),
		RELATED		(TEXT_RELATEDCOLUMN,									TABLE_RELATED_WIDTH),
		MATCHED		(TEXT_MATCHEDCOLUMN,									TABLE_OPTION_WIDTH),
		;
		
		private String name;
		private int width;
		
		private ChConvColumn(String name, int width) {
			this.name = name;
			this.width = width;
		}
		
		String getName() { return name; }
		
		int getIniWidth() { return width; }
		
		int getColumn() { return ordinal(); }
	}

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	// コンポーネント
	
	private JPanel jp_update = null;
	private JButton jbtn_update = null;
	private JTextAreaWithPopup jta_hint = null;

	private JPanel jp_setting = null;
	private JTextAreaWithPopup jta_note = null;
	private JComboBox jcb_progplugin = null;
	private JLabel jl_area = null;
	private JLabel jl_rel = null;
	private JScrollPane jscr_chconv = null;
	private ChConvTable jtbl_chconv = null;
	
	// コンポーネント以外
	
	// しょぼーん
	private Syobocal syoboplugin = null;
	
	// テーブルのデータの入れ物
	private final RowItemList<ChConvItem> rowData = new RowItemList<ChConvItem>();
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsChannelConvertView() {
		
		super();
		
		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		this.setColumnHeaderView(getJP_update());
		this.setViewportView(getJP_setting());
		
		// テーブルの初期化
		initialize();
		
		if (debug) System.out.println(DBGID+"構築完了");
		
	}
	
	/*******************************************************************************
	 * アクション
	 ******************************************************************************/
	
	// 外部向け
	
	/**
	 * テーブルの書き換えイベントを起こす
	 */
	public void updateChannelConvertTable() {
		int selected = jcb_progplugin.getSelectedIndex();
		jcb_progplugin.setSelectedIndex(-1);
		jcb_progplugin.setSelectedIndex(selected);
	}
	
	// 内部向け
	
	/**
	 *  初期化
	 */
	private void initialize() {
		
		// しょぼーん
		setSyobo();
		
		// コンボボックスの設定
		setPluginsForComboBox();

		jcb_progplugin.setSelectedIndex(-1);
		jcb_progplugin.setSelectedIndex(0);
		
		jbtn_update.setEnabled(false);

	}
	
	/**
	 *  しょぼーん
	 */
	private void setSyobo() {
		
		if ( env.getUseSyobocal() )
		{
			syoboplugin = new Syobocal();

			syoboplugin.loadCenter(syoboplugin.getSelectedCode(), false);
			syoboplugin.setSortedCRlist();
			
			if (debug) System.out.println(DBGID+"しょぼかるを利用しています： "+syoboplugin.getCRlist().size());
		}
				
	}
	
	/**
	 *  コンボボックスの設定
	 */
	private void setPluginsForComboBox() {
		
		ProgSubtype[] subs = { ProgSubtype.TERRA, ProgSubtype.CS, ProgSubtype.CS2 };
		
		jcb_progplugin.removeItemListener(il_pluginselected);	// リスナー停止
		
		for ( ProgSubtype sub : subs )
		{
			for ( TVProgram tp : progPlugins )
			{
				if ( tp.getType() == ProgType.PROG && tp.getSubtype() == sub )
				{
					tp.loadAreaCode();
					tp.loadCenter(tp.getSelectedCode(), false);
					tp.setSortedCRlist();

					jcb_progplugin.addItem(tp.getTVProgramId());
				}
			}
		}
		if ( env.getUseSyobocal() )
		{
			jcb_progplugin.addItem(syoboplugin.getTVProgramId());
		}
		
		jcb_progplugin.addItemListener(il_pluginselected);		// リスナー再開

	}
	
	/**
	 *  テーブルのデータを更新する
	 * @param tp
	 */
	private void setCentersForTable(TVProgram tp) {
		
		rowData.clear();
		
		// 有効局が優先
		for ( Center cr : tp.getSortedCRlist() )
		{
			ChConvItem c = getChConvItem(cr,tp);
			if ( c == null )
			{
				continue;
			}
			
			rowData.add(c);
			
			//if (debug) System.out.println(DBGID+"有効局を追加しました： "+c.centername);
		}
		
		// 無効局はおまけ
		for ( Center cr : tp.getCRlist() )
		{
			if ( cr.getOrder() > 0 )
			{
				continue;
			}
			
			ChConvItem c = getChConvItem(cr,tp);
			if ( c == null )
			{
				continue;
			}
			
			rowData.add(c);
			
			//if (debug) System.out.println(DBGID+"無効局を追加しました： "+c.centername);
		}
		
		// 更新して！
		((DefaultTableModel) jtbl_chconv.getModel()).fireTableDataChanged();
		
		if (debug) System.out.println(DBGID+"rowData count="+rowData.size());
		
	}
	
	/**
	 *  テーブルの行データの作成
	 * @param cr
	 * @return
	 */
	private ChConvItem getChConvItem(Center cr, TVProgram tp) {
		
		if ( cr.getCenterOrig().equals(UNAVAILABLE_CENTER) )
		{
			return null;
		}
		
		boolean issyobo = (tp.getType() == ProgType.SYOBO);
		
		ChConvItem c = new ChConvItem();
		
		c.original = cr.getCenterOrig();
		c.centername = chconv.get(c.original);
		
		c.hide_enabled = (cr.getOrder() > 0);
		
		c.hide_converted = ( ! c.original.equals(c.centername));
		if ( c.hide_converted )
		{
			c.converted = TEXT_CONVERTED;
		}
		
		// 他サイトとの連携が行われているか確認する
		//if ( ! issyobo )
		{
			c.related = "";
			for ( TVProgram p : progPlugins ) {
				if ( p.getTVProgramId().equals(tp.getTVProgramId()) )
				{
					continue;
				}
				if ( p.getType() == ProgType.SYOBO )
				{
					continue;
				}
				
				for ( Center crp : p.getCRlist() )
				{
					if ( crp.getCenter().equals(c.centername) )
					{
						c.related += p.getTVProgramId()+", ";
						break;
					}
				}
				
			}
			c.related = c.related.replaceFirst(", $","");
		}
	
		// しょぼかる連携が行われているか確認する
		if ( env.getUseSyobocal() && ! issyobo )
		{
			for ( Center crsy : syoboplugin.getCRlist() )
			{
				if ( c.centername.equals(chconv.get(crsy.getCenterOrig())) )
				{
					//if (debug) System.out.println(DBGID+"しょぼかる連携が有効です： "+c.centername+"<->"+chconv.get(crsy.getCenterOrig()));
					
					c.syobo = TEXT_SYOBO_EN;
					c.hide_syoborelated = true;
					break;
				}
			}
		}
		
		c.fireChanged();
		
		return c;
		
	}
	
	//

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	// 更新確定ボタンを押したイベントを拾うリスナー
	private final ActionListener al_update = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (debug) System.out.println(DBGID+"al_update "+e.toString());
			
			JButton btn = (JButton) e.getSource();
			btn.setEnabled(false);
		}
		
	};
	
	// 番組表が選択された
	private final ItemListener il_pluginselected = new ItemListener() {

		@Override
		public void itemStateChanged(ItemEvent e) {
			if (debug) System.out.println(DBGID+"il_pluginselected "+e.toString());
			if ( e.getStateChange() == ItemEvent.SELECTED )
			{
				String selected = (String) ((JComboBox) e.getSource()).getSelectedItem();
				TVProgram tp = null;
				if ( syoboplugin != null && syoboplugin.getTVProgramId().equals(selected) )
				{
					tp = syoboplugin;
					jl_area.setText(TEXT_AREASELECT_UNSUPPORTED);
				  	setCentersForTable(syoboplugin);
				}
				else {
					tp = progPlugins.getProgPlugin(selected);
					if ( tp != null )
					{
						tp.loadAreaCode();
						tp.loadCenter(tp.getSelectedCode(), false);
						tp.setSortedCRlist();
						
					  	setCentersForTable(tp);
					  	
						if ( tp.isAreaSelectSupported() )
						{
							jl_area.setText(tp.getSelectedArea());
						}
						else
						{
							jl_area.setText(TEXT_AREASELECT_UNSUPPORTED);
						}
					}
				}
				
				if ( tp != null ) {
					TableColumn colrel = jtbl_chconv.getColumnModel().getColumn(ChConvColumn.RELATED.getColumn());
					TableColumn colsyo = jtbl_chconv.getColumnModel().getColumn(ChConvColumn.MATCHED.getColumn());
					if ( tp.getType() == ProgType.SYOBO )
					{
						colrel.setHeaderValue(TEXT_RELATEDCOLUMN_SYOBO);
						colsyo.setHeaderValue(TEXT_MATCHEDCOLUMN_SYOBO);
					}
					else
					{
						colrel.setHeaderValue(TEXT_RELATEDCOLUMN);
						colsyo.setHeaderValue(TEXT_MATCHEDCOLUMN);
					}
					//jtbl_chconv.getTableHeader().resizeAndRepaint();
				}
				
				if (debug) System.out.println(DBGID+"il_pluginselected selected="+selected);
			}
		}
		
	};
	
	// 行が選択されて
	private final ListSelectionListener lsl_selected = new ListSelectionListener() {
		
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (debug) System.out.println(DBGID+"lsl_selected "+e.toString());
			if ( ! e.getValueIsAdjusting() )
			{
				ListSelectionModel model = (ListSelectionModel) e.getSource();
				if ( ! model.isSelectionEmpty() )
				{
					int row = model.getMinSelectionIndex();
					ChConvItem c = rowData.get(row);
					jl_rel.setText(c.related);
				}
			}
		}
		
	};
	
	// セルが編集された
	private final CellEditorListener cel_edited = new CellEditorListener() {
		
		@Override
		public void editingStopped(ChangeEvent e) {
			if (debug) System.out.println(DBGID+"cel_edited "+e.toString());
			jbtn_update.setEnabled(true);
		}
		
		@Override
		public void editingCanceled(ChangeEvent e) {
			if (debug) System.out.println(DBGID+"cel_edited "+e.toString());
		}
		
	};
	
	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
	
	// 更新確定ボタンの部分
	
	public JPanel getJP_update() {
		
		if (jp_update == null)
		{
			jp_update = new JPanel();
			jp_update.setLayout(new SpringLayout());
			
			jp_update.setBorder(new LineBorder(Color.GRAY));
			
			int y = SEP_HEIGHT;
			int x = SEP_WIDTH;
			
			CommonSwingUtils.putComponentOn(jp_update, getJBtn_update(TEXT_UPDATE), UPDATE_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			
			int y2 = SEP_HEIGHT/2;
			int x2 = x + UPDATE_WIDTH+SEP_WIDTH*4;
			CommonSwingUtils.putComponentOn(jp_update, getJTa_hint(), HINT_WIDTH, PARTS_HEIGHT+SEP_HEIGHT, x2, y2);
			
			y += (PARTS_HEIGHT + SEP_HEIGHT);
			
			jp_update.setPreferredSize(new Dimension(PANEL_WIDTH,y));
		}
		return jp_update;
		
	}
	
	// 「更新を確定する」ボタン
	private JButton getJBtn_update(String s) {
		
		if ( jbtn_update == null )
		{
			jbtn_update = new JButton(s);
			jbtn_update.addActionListener(al_update);
		}
		return jbtn_update;
		
	}
	
	// ヒント表示部分
	private JTextAreaWithPopup getJTa_hint() {
		
		if ( jta_hint == null )
		{
			jta_hint = CommonSwingUtils.getJta(this,2,0);
			jta_hint.append(TEXT_HINT);
		}
		return jta_hint;
		
	}
	
	// 設定画面の部分
	
	// パネル
	public JPanel getJP_setting() {
		
		if (jp_setting == null)
		{
			jp_setting = new JPanel();
			jp_setting.setLayout(new SpringLayout());
			
			int y = SEP_HEIGHT;
			int x = SEP_WIDTH;
			int x2 = x+LABEL_WIDTH+SEP_WIDTH;
			
			CommonSwingUtils.putComponentOn(jp_setting, getJTa_note(), TABLE_WIDTH, PARTS_HEIGHT, x2, y);
			y += PARTS_HEIGHT/*+SEP_HEIGHT*/;
			
			CommonSwingUtils.putComponentOn(jp_setting, getJCb_progplugin(), SELECTOR_WIDTH, PARTS_HEIGHT, x2, y);
			CommonSwingUtils.putComponentOn(jp_setting, getJL_area(), AREA_WIDTH, PARTS_HEIGHT, x2+SELECTOR_WIDTH+SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jp_setting, getJL_rel(), RELATION_WIDTH, PARTS_HEIGHT, x2+SELECTOR_WIDTH+AREA_WIDTH+SEP_WIDTH*2, y);
			y += PARTS_HEIGHT+SEP_HEIGHT;
			
			getJScr_chconv();
			int sb_w = jscr_chconv.getVerticalScrollBar().getPreferredSize().width;
			CommonSwingUtils.putComponentOn(jp_setting, jscr_chconv, TABLE_WIDTH+sb_w+5, TABLE_HEIGHT, x2, y);
			y += TABLE_HEIGHT+SEP_HEIGHT;
			
			jp_setting.setPreferredSize(new Dimension(PANEL_WIDTH,y));
		}
		return jp_setting;
		
	}
	
	// 通知
	private JTextAreaWithPopup getJTa_note() {
		
		if ( jta_note == null )
		{
			jta_note = CommonSwingUtils.getJta(this,1,0);
			jta_note.append(TEXT_NOTE);
		}
		return jta_note;
		
	}
	
	// 番組表プラグインの選択
	private JComboBox getJCb_progplugin() {
		
		if ( jcb_progplugin == null )
		{
			jcb_progplugin = new JComboBox();
			
			jcb_progplugin.addItemListener(il_pluginselected);
		}
		return jcb_progplugin;
		
	}
	
	// 番組表のエリア
	private JLabel getJL_area() {
		
		if ( jl_area == null )
		{
			jl_area = new JLabel();
			jl_area.setBorder(new LineBorder(Color.BLACK));
		}
		return jl_area;
		
	}
	
	// 他サイト連携
	private JLabel getJL_rel() {
		
		if ( jl_rel == null )
		{
			jl_rel = new JLabel();
			jl_rel.setBorder(new LineBorder(Color.BLACK));
		}
		return jl_rel;
		
	}
	
	// テーブルの入れ物
	private JScrollPane getJScr_chconv() {
		
		if (jscr_chconv == null)
		{
			jscr_chconv = new JScrollPane();
			jscr_chconv.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			jscr_chconv.getVerticalScrollBar().setUnitIncrement(25);
			jscr_chconv.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			jscr_chconv.setViewportView(getJTbl_chconv());
		}
		return jscr_chconv;
		
	}
	
	// テーブル
	private ChConvTable getJTbl_chconv() {
		
		if ( jtbl_chconv == null )
		{
			jtbl_chconv = new ChConvTable(rowData);
			
			DefaultTableModel model = new DefaultTableModel();
			for ( ChConvColumn ccc : ChConvColumn.values() )
			{
				model.addColumn(ccc.getName());	// カラム名
			}
			jtbl_chconv.setModel(model);
			
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel) jtbl_chconv.getColumnModel();
			for ( ChConvColumn rc : ChConvColumn.values() )
			{
				if ( rc.getIniWidth() >= 0 )
				{
					columnModel.getColumn(rc.ordinal()).setPreferredWidth(rc.getIniWidth());;	// カラムの幅
				}
			}
			
			// 属性
			jtbl_chconv.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
			jtbl_chconv.getTableHeader().setReorderingAllowed(false);
			jtbl_chconv.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jtbl_chconv.putClientProperty("terminateEditOnFocusLost", true);	// これやらないと、編集が確定したように見えて確定しない。ヤバイ。
			jtbl_chconv.setRowHeight(jtbl_chconv.getRowHeight()+4);
			
			// 行を選択したら
			jtbl_chconv.getSelectionModel().addListSelectionListener(lsl_selected);
			
			// 編集セルにリスナーを付ける
			TableColumn tc = jtbl_chconv.getColumn(ChConvColumn.EDIT.getName());
			EditorColumn ec = new EditorColumn(); 
			ec.addCellEditorListener(cel_edited);
			tc.setCellEditor(ec);
		}
		return jtbl_chconv;
		
	}
	
	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/
	
	// テーブルの行データの構造
	private class ChConvItem extends RowItem implements Cloneable {
		
		// 表示メンバ
		String original;
		String centername;
		String related;
		String converted;
		String syobo;
		
		// 非表示メンバ
		boolean hide_enabled;
		boolean hide_converted;
		boolean hide_syoborelated;
		
		@Override
		protected void myrefresh(RowItem o) {
			ChConvItem c = (ChConvItem) o;
			c.addData(original);
			c.addData(centername);
			c.addData(converted);
			c.addData(related);
			c.addData(syobo);
		}
		
		@Override
		public ChConvItem clone() {
			return (ChConvItem) super.clone();
		}
		
	}
	
	// ChConvItemを使ったJTable拡張
	private class ChConvTable extends JTable {

		private static final long serialVersionUID = 1L;
		
		private final Color evenColor = new Color(240,240,255);
		private final Color oddColor = super.getBackground();
		
		private final Color unmappedOddColor = new Color(153,153,153);
		private final Color unmappedEvenColor = new Color(133,133,133);
		
		private final Color syoboOddColor = new Color(255,204,153);
		private final Color syoboEvenColor = new Color(235,184,133);
		
		private final Color disabledOddColor = new Color(200,200,200);
		private final Color disabledEvenColor = new Color(180,180,180);
		
		private RowItemList<ChConvItem> rowdata = null;
		
		public ChConvTable(RowItemList<ChConvItem> rowdata) {
			this.rowdata = rowdata;
			
			// フォントサイズ変更にあわせて行の高さを変える
			this.addPropertyChangeListener("font", new RowHeightChangeListener(8));

			// 行の高さの初期値の設定
			this.firePropertyChange("font", "old", "new");
		}
		
		@Override
		public Object getValueAt(int row, int column) {
			
			int vrow = this.convertRowIndexToModel(row);
			ChConvItem c = rowdata.get(vrow);
			return c.get(column);
			
		}
		
		@Override
		public void setValueAt(Object aValue, int row, int column) {
			
			int vrow = this.convertRowIndexToModel(row);
			ChConvItem c = rowdata.get(vrow); 
			if ( column == ChConvColumn.EDIT.getColumn() )
			{
				c.centername = (String) aValue;
				c.fireChanged();
			}
			
		}
		
		@Override
		public int getRowCount() {
			
			return rowdata.size();
			
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			
			if ( column != ChConvColumn.EDIT.getColumn() )
			{
				return false;	// 編集欄以外は編集できない
		  	}
			return true;
			
		}
		
		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			
			Component comp = super.prepareRenderer(tcr, row, column);
			
			int vrow = this.convertRowIndexToModel(row);
			ChConvItem c = rowdata.get(vrow);
			
			Color fg = null;
			Color bg = null;
			
			boolean evenline = (row%2 == 1);
			
			if ( c.hide_converted && column == ChConvColumn.EDIT.getColumn() )
			{
				fg = Color.RED;
			}
			else if ( column == ChConvColumn.ORIG.getColumn() )
			{
				fg = Color.BLUE;
			}
			else
			{
				if ( isRowSelected(row) )
				{
					fg = this.getSelectionForeground();
				}
			}
			
			if ( isRowSelected(row) )
			{
				bg = this.getSelectionBackground();
			}
			else {
				if ( column == ChConvColumn.EDIT.getColumn() ) {
					if ( ! c.hide_enabled )
					{
						bg = (evenline)?(unmappedEvenColor):(unmappedOddColor);
					}
					else if ( c.hide_syoborelated )
					{
						bg = (evenline)?(syoboEvenColor):(syoboOddColor);
					}
				}
				else {
					bg = (evenline)?(disabledEvenColor):(disabledOddColor);
				}
			}

			if (fg==null) fg = this.getForeground();
			if (bg==null) bg = (evenline)?(evenColor):(oddColor);
			
			comp.setForeground(fg);
			comp.setBackground(bg);
			
			return comp;
			
		}
		
	}

}
