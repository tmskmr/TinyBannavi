package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * CH設定について、放送の種類の別に独立したパネルを用意
 * @version 3.15.4β
 */
public class ChannelSettingPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;

	/*
	 * メンバ
	 */

	private String label = "";
	private TVProgramList progPlugins = null;
	private boolean hasArea = true;
	private VWColorChooserDialog ccwin = null;
	private StatusWindow stwin = null;
	private Component parent = null;

	/**
	 * 現在使用されているプラグイン
	 */
	private TVProgram usingNow = null;

	/**
	 * <P>起動時 {@link #getUsingPluginForInit()}でclone()
	 * <P>番組表を切り替えたとき {@link #getEditingPluginForEdit()}でclone()
	 */
	private TVProgram editingNow = null;

	private static final String flTextNormal = "Webサイトから放送局リストを取得し直す";
	private static final String flTextWarn = "「更新を確定する」を実行してください";



	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	//private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;

	private static final int LABEL_WIDTH = 250;
	private static final int BUTTON_WIDTH = 50;
	private static final int BUTTON_WIDTH_LONG = 100;
	private static final int TABLE_NAME_WIDTH = 250;
	private static final int TABLE_AREA_WIDTH = 75;
	private static final int TABLE_COLOR_WIDTH = 25;
	private static final int TABLE_WIDTH = TABLE_NAME_WIDTH+TABLE_AREA_WIDTH+TABLE_COLOR_WIDTH;
	private static final int TABLE_HEIGHT = 350;
	//private static final int PANEL_WIDTH = PARTS_WIDTH+100;

	private static final String MSGID = "[CH設定パネル] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	// カラム設定

	private static enum ChSetColumn {
		CENTER		("有効無効",	TABLE_NAME_WIDTH),
		AREA		("エリア",		TABLE_AREA_WIDTH),
		COLOR		("色",		TABLE_COLOR_WIDTH),
		;

		private String name;
		private int width;

		private ChSetColumn(String name, int width) {
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

	private JComboBox jComboBox_progplugin = null;
	private JComboBox jComboBox_area = null;
	private JScrollPane jScrollPane_enable = null;
	private JScrollPane jScrollPane_disable = null;
	private ChSetTable jTable_enable = null;
	private ChSetTable jTable_disable = null;
	private JButton jButton_d2e = null;
	private JButton jButton_e2d = null;
	private JButton jButton_up = null;
	private JButton jButton_down = null;
	private JButton jButton_forceload = null;
	private JButton jButton_opt = null;
	//private JTextAreaWithPopup jTextArea_opt = null;
	private JTextFieldWithPopup jTextField_opt = null;

	// コンポーネント以外

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public ChannelSettingPanel(String label, TVProgramList progPlugins, final String selectedsite, boolean hasArea, VWColorChooserDialog ccwin, StatusWindow stwin, Component parent) {

		super();

		// 初期化
		this.label = label;
		this.progPlugins = progPlugins;
		this.hasArea = hasArea;
		this.ccwin = ccwin;
		this.stwin = stwin;
		this.parent = parent;

		// パーツの組み立て
		this.setLayout(new SpringLayout());

		//コンポーネントの生成
		getMyComponents();

		// 起動時に選択されているWeb番組表プラグインの情報を反映する
		getUsingPluginForInit(selectedsite);
	}

	private void getMyComponents() {
		int y = SEP_HEIGHT;

		int x = SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, new JLabel(label), LABEL_WIDTH, PARTS_HEIGHT, x, y);
		x += LABEL_WIDTH+SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, getJComboBox_progplugin(), TABLE_WIDTH, PARTS_HEIGHT, x, y);
		x += TABLE_WIDTH+SEP_WIDTH+BUTTON_WIDTH+SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, getJButton_forceload(flTextNormal), TABLE_WIDTH, PARTS_HEIGHT, x, y);

		if (this.hasArea) {
			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(this, new JLabel("放送エリア"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(this, new JLabel("（他県局選択は「全国」を指定）"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y+PARTS_HEIGHT/2);
			CommonSwingUtils.putComponentOn(this, getJComboBox_area(), TABLE_WIDTH, PARTS_HEIGHT, SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH, y);
		}

		y+=(PARTS_HEIGHT+SEP_HEIGHT);
		x = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, getJScrollPane_enable(), TABLE_WIDTH, TABLE_HEIGHT, x, y);

		int yz = y+TABLE_HEIGHT/2;
		int xz = x + SEP_WIDTH+TABLE_WIDTH;
		CommonSwingUtils.putComponentOn(this, getJButton_d2e("＜"),	BUTTON_WIDTH, PARTS_HEIGHT, xz, yz-PARTS_HEIGHT-SEP_HEIGHT/2);
		CommonSwingUtils.putComponentOn(this, getJButton_e2d("＞"),	BUTTON_WIDTH, PARTS_HEIGHT, xz, yz+SEP_HEIGHT/2);

		yz = y+TABLE_HEIGHT-PARTS_HEIGHT*2-SEP_HEIGHT;
		CommonSwingUtils.putComponentOn(this, getJButton_up("↑"), 	BUTTON_WIDTH, PARTS_HEIGHT, xz, yz);
		CommonSwingUtils.putComponentOn(this, getJButton_down("↓"), BUTTON_WIDTH, PARTS_HEIGHT, xz, yz+SEP_HEIGHT+PARTS_HEIGHT);

		xz += BUTTON_WIDTH+SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, getJScrollPane_disable(),	TABLE_WIDTH, TABLE_HEIGHT, xz, y);

		int panel_w = xz+TABLE_WIDTH+SEP_WIDTH;

		y+=(TABLE_HEIGHT+SEP_HEIGHT);

		int tt_l = TABLE_WIDTH*2+SEP_WIDTH*2+BUTTON_WIDTH;
		CommonSwingUtils.putComponentOn(this, new JLabel("オプション指定(形式：KEY=VAL;)"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
		CommonSwingUtils.putComponentOn(this, getJTextArea_opt(), tt_l-BUTTON_WIDTH_LONG-SEP_WIDTH, PARTS_HEIGHT, x, y);
		CommonSwingUtils.putComponentOn(this, getJButton_opt("future use."), BUTTON_WIDTH_LONG, PARTS_HEIGHT, x+tt_l-BUTTON_WIDTH_LONG, y);

		y+=(PARTS_HEIGHT+SEP_HEIGHT);

		//y += SEP_HEIGHT;

		Dimension d = new Dimension(panel_w,y);
		this.setPreferredSize(d);
		this.setBorder(new LineBorder(new Color(0,0,0)));
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	/*
	 * 公開メソッド
	 */

	/**
	 *  選択中の放送局を返却する
	 */
	public String getSelectedCenter() {
		return (String)jComboBox_progplugin.getSelectedItem();
	}

	/**
	 * <P>更新の確定時に呼び出されるメソッド
	 * <P>プラグイン内の設定はここで保存、プラグインの選択情報は親で保存
	 */
	public boolean saveChannelSetting() {
		if ( editingNow != null ) {
			// 放送局リスト再取得ボタンをもとに戻す
			setReloadButtonEnhanced(false);
			// ワークを使って保存する
			editingNow.setSelectedAreaByName(getSelectedArea());
			editingNow.saveAreaCode();
			setChOrder(editingNow);
			setChBgColor(editingNow);
			editingNow.saveCenter();
			if ( ! editingNow.setOptString((String) jTextField_opt.getText()) ) {
				// メッセージぐらいでてもいいのでは
				stwin.appendError(ERRID+"【致命的】フリーワードオプションの設定に失敗しました： "+((String) jTextField_opt.getText()));
			}
			jTextField_opt.setText(editingNow.getOptString());
			//
			int size = progPlugins.size();
			for ( int i=0; i<size; i++ ) {
				TVProgram px = progPlugins.get(i);
				if ( px.getTVProgramId().equals(editingNow.getTVProgramId()) ) {
					progPlugins.set(i,editingNow);
					usingNow = editingNow;
					return true;
				}
			}
			return false;
		}
		return false;
	}


	/*
	 * 非公開
	 */

	/**
	 * <P>インスタンス作成時に編集するプラグインを選択する
	 * <P>起動時一回のみの実行
	 * <P>コンボボックス選択によりイベントも起こす
	 * @see #il_progChanged
	 */
	private boolean getUsingPluginForInit(String selectedsite) {
		if ( progPlugins.size() == 0 ) {
			stwin.appendError(ERRID+"【致命的】プラグインが１つも登録されていません： "+selectedsite);
			return false;
		}

		// 起動時に選択されていたプラグインはusingNowである
		TVProgram p = progPlugins.getProgPlugin(selectedsite);
		if ( p == null ) {
			p = progPlugins.get(0);
			stwin.appendError(ERRID+"指定のプラグインがみつからなかったので代替のプラグインを利用します： "+selectedsite+" -> "+p.getTVProgramId());
		}
		else {
			System.out.println(MSGID+"番組表プラグインの初期値です： "+selectedsite+" -> "+p.getTVProgramId());
		}
		usingNow = p;

		// 初期化
		p.loadAreaCode();
		p.loadCenter(p.getSelectedCode(), false);
		p.setSortedCRlist();

		final String area = (p.getSelectedArea()==null)?(p.getDefaultArea()):p.getSelectedArea();

		TatCount tc = new TatCount();
		stwin.appendMessage(MSGID+"選択されている番組表： "+p.getTVProgramId()+" / "+area);

		// 編集対象editingNowは、現時点ではusingNowと同じである
		editingNow = p.clone();

		// MacOSXで起動時ハングアップが発生することがあるのでトラップコードを入れてみた
		try {
			// プラグイン選択
			jComboBox_progplugin.removeItemListener(il_progChanged);
			jComboBox_progplugin.setSelectedItem(editingNow.getTVProgramId());
			jComboBox_progplugin.addItemListener(il_progChanged);

			// エリア選択
			if (hasArea) {
				jComboBox_area.removeItemListener(il_areaChanged);
				for ( AreaCode ac : editingNow.getAClist() ) {
					jComboBox_area.addItem(ac.getArea());
				}
				jComboBox_area.setSelectedItem(editingNow.getSelectedArea());
				jComboBox_area.addItemListener(il_areaChanged);
			}

			// テーブルへ設定する
			addCentersToTable(jTable_enable, jTable_disable, editingNow);

			if (CommonUtils.isMac()) System.err.println(DBGID+"mac debug 1");

			// オプションの扱い
			String opt = editingNow.getOptString();
			setOptionFieldEnabled(opt);

			if (CommonUtils.isMac()) System.err.println(DBGID+"mac debug 2");
		}
		catch (Exception e) {
			e.printStackTrace();
			stwin.appendError(ERRID+"[致命的エラー] 異常動作です。製作元に連絡してください。");
		}

		stwin.appendMessage(String.format("%s番組表が初期化されました。所要時間： %.2f秒 %s %s",MSGID,tc.end(),p.getTVProgramId(),p.getSelectedArea()));

		return true;
	}

	/**
	 * プラグイン選択コンボボックスを操作した場合
	 */
	private boolean getEditingPluginForEdit() {

		// コンボボックスで選択されたプラグインは？
		TVProgram p = progPlugins.getProgPlugin(getSelectedPluginId());
		if ( p == null ) {
			System.err.println(ERRID+"【致命的】選択されたプラグインのインスタンスがみつかりません： "+getSelectedPluginId());
			return false;
		}

		stwin.clear();

		TatCount tc = new TatCount();
		stwin.appendMessage(MSGID+"選択された番組表： "+p.getTVProgramId()+" / "+p.getSelectedArea());

		// これだね
		editingNow = p.clone();

		new SwingBackgroundWorker(false) {
			@Override
			protected Object doWorks() throws Exception {
				// あればファイル、なければWebから
				editingNow.loadAreaCode();

				String code = editingNow.getCode(usingNow.getSelectedArea());
				if ( code != null ) {
					editingNow.setSelectedAreaByName(usingNow.getSelectedArea());
					if (debug) stwin.appendMessage(DBGID+"切り替え前のプラグインからエリアを引き継ぎました： "+usingNow.getTVProgramId()+":"+usingNow.getSelectedArea()+" -> "+editingNow.getTVProgramId()+":"+editingNow.getSelectedArea());
				}
				else {
					if (debug) stwin.appendMessage(DBGID+"プラグインがきりかわります： "+usingNow.getTVProgramId()+":"+usingNow.getSelectedArea()+" -> "+editingNow.getTVProgramId()+":"+editingNow.getSelectedArea());
				}
				editingNow.loadCenter(editingNow.getSelectedCode(), false);
				editingNow.setSortedCRlist();

				// エリア選択
				if (hasArea) {
					jComboBox_area.removeItemListener(il_areaChanged);
					// プラグイン選択
					jComboBox_area.removeAllItems();
					for ( AreaCode ac : editingNow.getAClist() ) {
						jComboBox_area.addItem(ac.getArea());
					}
					jComboBox_area.setSelectedItem(editingNow.getSelectedArea());
					jComboBox_area.addItemListener(il_areaChanged);
				}

				if ( editingNow.getSortedCRlist().size() == 0 && usingNow.getSortedCRlist().size() > 0 ) {
					stwin.appendMessage(MSGID+"有効局が未設定のエリアのため放送局情報の引き継ぎをこころみます ");
					inheritEnabledCenters(editingNow, usingNow);
				}

				// テーブルへ設定する
				addCentersToTable(jTable_enable, jTable_disable, editingNow);

				// オプションの扱い
				String opt = editingNow.getOptString();
				setOptionFieldEnabled(opt);

				return null;
			}
			@Override
			protected void doFinally() {
				stwin.setVisible(false);
			}
		}.execute();

		CommonSwingUtils.setLocationCenter(parent, (Component) stwin);
		stwin.setVisible(true);

		stwin.appendMessage(String.format("%s番組表が切り替わりました。所要時間： %.2f秒 %s %s",MSGID,tc.end(),p.getTVProgramId(),p.getSelectedArea()));

		return true;
	}

	/**
	 * エリア選択コンボボックスを操作した場合
	 */
	private boolean setAreaForEdit() {

		// コンボボックスで選択されたエリアは？
		final String code = editingNow.setSelectedAreaByName(getSelectedArea());
		if ( code == null ) {
			System.err.println(ERRID+"【致命的】選択されたエリア情報がみつかりません： "+getSelectedPluginId()+" "+getSelectedArea());
			return false;
		}

		stwin.clear();

		TatCount tc = new TatCount();
		stwin.appendMessage(MSGID+"選択されたエリア： "+editingNow.getTVProgramId()+" / "+getSelectedArea());

		new SwingBackgroundWorker(false) {
			@Override
			protected Object doWorks() throws Exception {
				// あればファイル、なければWebから
				editingNow.loadCenter(code, false);
				editingNow.setSortedCRlist();

				if ( editingNow.getSortedCRlist().size() == 0 && usingNow.getSortedCRlist().size() > 0 ) {
					System.out.println(MSGID+"有効局が未設定のエリアのため情報の引き継ぎをこころみます ");
					inheritEnabledCenters(editingNow, usingNow);
				}

				// テーブルへ設定する
				addCentersToTable(jTable_enable, jTable_disable, editingNow);

				return null;
			}
			@Override
			protected void doFinally() {
				stwin.setVisible(false);
			}
		}.execute();

		CommonSwingUtils.setLocationCenter(parent, (Component) stwin);
		stwin.setVisible(true);

		stwin.appendMessage(String.format("%sエリアが切り替わりました。所要時間： %.2f秒 %s %s",MSGID,tc.end(),editingNow.getTVProgramId(),editingNow.getSelectedArea()));

		return true;
	}

	/**
	 * 再読み込みしちゃいなちゃーいなちゃいなー
	 */
	private boolean getCenterListFromWeb() {

		setReloadButtonEnhanced(true);

		stwin.clear();

		TatCount tc = new TatCount();
		stwin.appendMessage(MSGID+"選択されたエリア： "+editingNow.getTVProgramId()+" / "+getSelectedArea());

		final TVProgram tmp = editingNow.clone();

		new SwingBackgroundWorker(false) {
			@Override
			protected Object doWorks() throws Exception {
				editingNow.setOptString(getOptionField());
				String code = editingNow.getCode(getSelectedArea());
				editingNow.loadCenter(code, true);
				editingNow.setSortedCRlist();

				if ( editingNow.getSortedCRlist().size() == 0 && tmp.getSortedCRlist().size() > 0 ) {
					//　editingNowが未選択状態であれば、以前の選択状態を引き継ぎたい
					inheritEnabledCenters(editingNow, tmp);
				}

				// テーブルへ設定する
				addCentersToTable(jTable_enable, jTable_disable, editingNow);

				// オプションの扱い
				String opt = editingNow.getOptString();
				setOptionFieldEnabled(opt);

				return null;
			}
			@Override
			protected void doFinally() {
				stwin.setVisible(false);
			}
		}.execute();

		CommonSwingUtils.setLocationCenter(parent, (Component) stwin);
		stwin.setVisible(true);

		stwin.appendMessage(String.format("%s放送局リストを再読み込みしました。所要時間： %.2f秒 %s %s",MSGID,tc.end(),editingNow.getTVProgramId(),editingNow.getSelectedArea()));

		return true;
	}

	/**
	 * 番組表再取得ボタンを強調表示したりもどしたりする
	 */
	private void setReloadButtonEnhanced(boolean b) {
		if ( b ) {
			jButton_forceload.setText(flTextWarn);
			jButton_forceload.setForeground(Color.RED);
		}
		else {
			jButton_forceload.setText(flTextNormal);
			jButton_forceload.setForeground(Color.BLACK);
		}
	}

	// 選択中のプラグインを返却する
	private String getSelectedPluginId() {
		return (String) jComboBox_progplugin.getSelectedItem();
	}

	// 選択中のエリアを返却する
	private String getSelectedArea() {
		return ((hasArea)?((String) jComboBox_area.getSelectedItem()):(null));
	}

	// おぷしょん
	private String getOptionField() {
		return jTextField_opt.getText();
	}

	// 放送局リストに並び順を設定する
	private void setChOrder(TVProgram progplugin) {

		RowItemList<ChSetItem> eRowdata = jTable_enable.getRowData();

		// 局順クリア
		for ( Center center : progplugin.getCRlist() ) {
			center.setOrder(0);
		}

		// 局順・色セット
		for ( int row=0; row<eRowdata.size(); row++ ) {
			ChSetItem c = eRowdata.get(row);
			String centerName = c.centername;
			String areaName = c.area;
			Color colorValue = CommonUtils.str2color(c.color);
			for ( Center cr : progplugin.getCRlist() ) {
				if ( cr.getCenter().equals(centerName) && cr.getAreaCode().equals(progplugin.getCode(areaName))) {
					cr.setOrder(row+1);
					cr.setBgColor(colorValue);
					break;
				}
			}
		}

		// ソート済みリスト作成
		progplugin.setSortedCRlist();
	}

	// 放送局リストに背景色を設定する
	private void setChBgColor(TVProgram progplugin) {

		RowItemList<ChSetItem> eRowdata = jTable_enable.getRowData();

		for ( int row=0; row<eRowdata.size(); row++ ) {
			progplugin.getSortedCRlist().get(row).setBgColor(CommonUtils.str2color(eRowdata.get(row).color));
		}
	}

	/**
	 *  放送局をテーブルに書き込む
	 */
	private void addCentersToTable(ChSetTable eTable, ChSetTable dTable, TVProgram program) {

		RowItemList<ChSetItem> eRowdata = eTable.getRowData();
		RowItemList<ChSetItem> dRowdata = dTable.getRowData();

		eRowdata.clear();
		dRowdata.clear();

		for ( int order=1; order <= program.getCRlist().size(); order++ ) {
			Center center = null;
			for ( Center cr : program.getCRlist() ) {
				if ( cr.getOrder() == order ) {
					center = cr;
					break;
				}
			}
			if (center != null) {
				ChSetItem c = new ChSetItem();
				c.centername = center.getCenter();
				c.area = program.getArea(center.getAreaCode());
				c.color = CommonSwingUtils.getColoredString(center.getBgColor(),"－");
				c.fireChanged();
				eRowdata.add(c);
			}
		}
		for ( Center center : program.getCRlist() ) {
			if ( center.getOrder() == 0  && ! center.getCenter().equals("（選択できません）") ) {
				ChSetItem c = new ChSetItem();
				c.centername = center.getCenter();
				c.area = program.getArea(center.getAreaCode());
				c.color = CommonSwingUtils.getColoredString(center.getBgColor(),"－");
				c.fireChanged();
				dRowdata.add(c);
			}
		}

		((DefaultTableModel) eTable.getModel()).fireTableDataChanged();
		((DefaultTableModel) dTable.getModel()).fireTableDataChanged();
	}

	/**
	 * <P>有効局の引き継ぎ
	 * <P>有効局が空の番組表を選択した場合にカレントの番組表から放送局の順序などを引き継ぐ
	 */
	private void inheritEnabledCenters(TVProgram editing, TVProgram using) {

		// 既存の設定のあるものは引き継ぎを行わない
		if ( editing.getSortedCRlist().size() > 0 ) {
			return;
		}

		int order = 1;
		for ( Center ocr : using.getSortedCRlist() ) {
			Center xcr = null;
			for ( Center ncr : editing.getCRlist() ) {
				if ( ncr.getCenter().equals(ocr.getCenter()) ) {
					xcr = ncr;
					if (editing.getArea(ncr.getAreaCode()).equals(using.getArea(ocr.getAreaCode()))) {
						// エリアが等しいものがあればそれを採用する
						break;
					}
				}
			}
			if (xcr != null) {
				xcr.setOrder(order++);
				xcr.setEnabled(true);
				xcr.setBgColor(ocr.getBgColor());
			}
		}

		// 整理して終わり
		editing.setSortedCRlist();
	}

	// 左右
	private void moveChSrcToDst(ChSetTable sTable, ChSetTable dTable)
	{
		RowItemList<ChSetItem> sRowData = sTable.getRowData();
		RowItemList<ChSetItem> dRowData = dTable.getRowData();

		if ( sRowData.size() <= 0 ) {
			return;
		}

		int top = sTable.getSelectedRow();
		int length = sTable.getSelectedRowCount();

		RowItemList<ChSetItem> tmp = new RowItemList<ChSetItem>();
		for ( int i=top; i<top+length; i++) {
			tmp.add(sRowData.get(i));
		}
		for ( int i=0; i<tmp.size(); i++) {
			dRowData.add(tmp.get(i));
			sRowData.remove(tmp.get(i));
		}

		((DefaultTableModel) sTable.getModel()).fireTableDataChanged();
		((DefaultTableModel) dTable.getModel()).fireTableDataChanged();
	}

	// DISに戻したものをもとの行にかえしてあげる
	private void refreshChDisOrder(ChSetTable table)
	{
		RowItemList<ChSetItem> rowdata = table.getRowData();
		RowItemList<ChSetItem> tmp = new RowItemList<ChSetItem>();

		for ( Center cr : editingNow.getCRlist() ) {
			for ( ChSetItem c : rowdata ) {
				if ( cr.getCenter().equals(c.centername) && cr.getAreaCode().equals(editingNow.getCode(c.area))) {
					tmp.add(c);
					break;
				}
			}
		}

		rowdata.clear();
		for ( ChSetItem c : tmp ) {
			rowdata.add(c);
		}

		((DefaultTableModel) table.getModel()).fireTableDataChanged();
	}

	// 上下
	private void chUp(ChSetTable table)
	{
		int top = table.getSelectedRow();
		int length = table.getSelectedRowCount();

		RowItemList<ChSetItem> rowdata = table.getRowData();

		if ( top <= 0 ) {
			return;
		}

		rowdata.up(top, length);

		((DefaultTableModel) table.getModel()).fireTableDataChanged();
		table.setRowSelectionInterval(top-1, top-1+(length-1));
	}
	private void chDown(ChSetTable table)
	{
		int top = table.getSelectedRow();
		int length = table.getSelectedRowCount();

		RowItemList<ChSetItem> rowdata = table.getRowData();

		if ( (top+length) >= rowdata.size() ) {
			return;
		}

		rowdata.down(top, length);

		((DefaultTableModel) table.getModel()).fireTableDataChanged();
		table.setRowSelectionInterval(top+1, top+1+(length-1));
	}


	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * Web番組表コンボボックスを操作することで起動されるリスナー
	 */
	private final ItemListener il_progChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}
			getEditingPluginForEdit();
		}
	};

	/**
	 *  放送エリア
	 */
	private final ItemListener il_areaChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() != ItemEvent.SELECTED) {
				return;
			}
			setAreaForEdit();
		}
	};

	/**
	 * 放送局一覧再取得
	 */
	private final MouseAdapter centerlistReloadingListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			getCenterListFromWeb();
		}
	};

	/**
	 * 放送局ごとの色
	 */
	private final MouseAdapter colorSelectionAdapter = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				//
				JTable t = (JTable) e.getSource();
				Point p = e.getPoint();

				int col = t.columnAtPoint(p);
				if (col != 2) {
					return;
				}

				int row = t.rowAtPoint(p);
				//
				ccwin.setColor(CommonUtils.str2color((String) t.getValueAt(row,2)));
				ccwin.setVisible(true);

				if (ccwin.getSelectedColor() != null ) {
					t.setValueAt(CommonSwingUtils.getColoredString(ccwin.getSelectedColor(),"－"), row, 2);
				}
			}
		}
	};

	/**
	 * オプションが編集されたっぽい
	 */
	private DocumentListener optionEditedListener = new DocumentListener() {
		@Override
		public void removeUpdate(DocumentEvent e) {
			setOptionButtonEnabled(true);
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			setOptionButtonEnabled(true);
		}
		@Override
		public void changedUpdate(DocumentEvent e) {
		}
	};

	private void setOptionButtonEnabled(boolean b) {
		if ( b ) {
			jButton_opt.setEnabled(true);
			jButton_opt.setForeground(Color.RED);
		}
		else {
			jButton_opt.setEnabled(false);
			jButton_opt.setForeground(Color.DARK_GRAY);
		}
	}


	/**
	 * オプション入力エリア
	 */
	private void setOptionFieldEnabled(String opt) {
		if ( opt == null ) {
			jTextField_opt.setEnabled(false);
			jTextField_opt.setBackground(this.getBackground());
			jTextField_opt.setText("");
		}
		else {
			jTextField_opt.setEnabled(true);
			jTextField_opt.setBackground(Color.WHITE);
			jTextField_opt.setText(opt);
		}
	}

	/*
	 * 部品
	 */

	/**
	 * Web番組表プラグインの切り替えコンボボックス
	 */
	private JComboBox getJComboBox_progplugin() {
		if (jComboBox_progplugin == null) {

			jComboBox_progplugin = new JComboBox();

			// 選択値の初期化（ここはやめるべき？）
			for ( TVProgram p : progPlugins ) {
				jComboBox_progplugin.addItem(p.getTVProgramId());
			}

			// Web番組表コンボボックスにリスナーを追加
			jComboBox_progplugin.addItemListener(il_progChanged);
		}
		return jComboBox_progplugin;
	}

	/**
	 * エリアの切り替えコンボボックス
	 */
	private JComboBox getJComboBox_area() {
		if (jComboBox_area == null) {

			jComboBox_area = new JComboBox();

			// エリアコンボボックスにリスナーを追加
			jComboBox_area.addItemListener(il_areaChanged);
		}
		return jComboBox_area;
	}

	// 放送局リスト（有効）
	private JScrollPane getJScrollPane_enable() {
		if (jScrollPane_enable == null) {
			jScrollPane_enable = new JScrollPane();
			jScrollPane_enable.setViewportView(jTable_enable = getJTable_tvcenter(true,TABLE_NAME_WIDTH,TABLE_AREA_WIDTH,TABLE_COLOR_WIDTH));
			jScrollPane_enable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_enable);
	}

	// 放送局リスト（無効）
	private JScrollPane getJScrollPane_disable() {
		if (jScrollPane_disable == null) {
			jScrollPane_disable = new JScrollPane();
			jScrollPane_disable.setViewportView(jTable_disable = getJTable_tvcenter(false,TABLE_NAME_WIDTH,TABLE_AREA_WIDTH,TABLE_COLOR_WIDTH));
			jScrollPane_disable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_disable);
	}

	private ChSetTable getJTable_tvcenter(boolean isEnabled, int col1_w, int col2_w, int col3_w) {

		ArrayList<String> cola = new ArrayList<String>();
		ArrayList<Integer> wida = new ArrayList<Integer>();
		for ( ChSetColumn cs : ChSetColumn.values() ) {
			if ( cs == ChSetColumn.CENTER ) {
				cola.add((isEnabled)?("有効なWeb番組表の放送局名"):("無効"));
			}
			else {
				cola.add(cs.getName());
			}
			wida.add(cs.getIniWidth());
		}
		String[] colname = cola.toArray(new String[0]);
		Integer[] colwidth = wida.toArray(new Integer[0]);

		ChSetTable jTable_tvcenter = getJTableWithFormat(colname, colwidth);

		// 重要！
		jTable_tvcenter.setRowData(new RowItemList<ChSetItem>());

		jTable_tvcenter.getTableHeader().setReorderingAllowed(false);
		jTable_tvcenter.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

		// 色選択欄のみレンダラが独自
		VWColorCellRenderer renderer = new VWColorCellRenderer();
		jTable_tvcenter.getColumn(ChSetColumn.COLOR.getName()).setCellRenderer(renderer);

		// 有効リストのみ色選択リスナーが付く
		if ( isEnabled ) {
			jTable_tvcenter.addMouseListener(colorSelectionAdapter);
		}

		return(jTable_tvcenter);
	}

	private ChSetTable getJTableWithFormat(String[] s, Integer[] w) {
		//
		DefaultTableModel model = new DefaultTableModel(s,0);
		ChSetTable jTable = new ChSetTable(model,true);
		// 各カラムの幅を設定する
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable.getColumnModel();
		TableColumn column = null;
		for (int i = 0 ; i < columnModel.getColumnCount() ; i++){
			column = columnModel.getColumn(i);
			column.setPreferredWidth(w[i]);
		}
		return(jTable);
	}

	// 左右ボタン
	private JButton getJButton_d2e(String s) {
		if (jButton_d2e == null) {
			jButton_d2e = new JButton(s);
			jButton_d2e.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					moveChSrcToDst(jTable_disable,jTable_enable);
				}
			});
		}
		return(jButton_d2e);
	}
	private JButton getJButton_e2d(String s) {
		if (jButton_e2d == null) {
			jButton_e2d = new JButton(s);
			jButton_e2d.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					moveChSrcToDst(jTable_enable,jTable_disable);
					refreshChDisOrder(jTable_disable);
				}
			});
		}
		return(jButton_e2d);
	}

	// 上下ボタン
	private JButton getJButton_up(String s) {
		if (jButton_up == null) {
			jButton_up = new JButton(s);
			jButton_up.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					chUp(jTable_enable);
				}
			});
		}
		return(jButton_up);
	}
	private JButton getJButton_down(String s) {
		if (jButton_down == null) {
			jButton_down = new JButton(s);
			jButton_down.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					chDown(jTable_enable);
				}
			});
		}
		return(jButton_down);
	}

	// 放送局リストを再構築する
	private JButton getJButton_forceload(String s) {
		if (jButton_forceload == null) {
			jButton_forceload = new JButton(s);
			jButton_forceload.addMouseListener(centerlistReloadingListener);
		}
		return(jButton_forceload);
	}

	/**
	 *  フリーオプションの保存ボタン
	 */
	@SuppressWarnings("serial")
	private JButton getJButton_opt(String s) {
		if (jButton_opt == null) {
			jButton_opt = new JButton(s) {
				@Override
				public void setEnabled(boolean b) {
					// ダミー中のため常にfalse
					super.setEnabled(false);
				}
			};
			setOptionButtonEnabled(false);
		}
		return(jButton_opt);
	}

	/**
	 * フリーオプション領域
	 */
	private JTextFieldWithPopup getJTextArea_opt() {
		if (jTextField_opt == null) {
			jTextField_opt = new JTextFieldWithPopup();
			jTextField_opt.setBorder(new LineBorder(Color.BLACK));
			jTextField_opt.getDocument().addDocumentListener(optionEditedListener);

		}
		return jTextField_opt;
	}

	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/

	// テーブルの行データの構造
	private class ChSetItem extends RowItem implements Cloneable {

		// 表示メンバ
		String centername;
		String area;
		String color;

		// 非表示メンバ

		@Override
		protected void myrefresh(RowItem o) {
			ChSetItem c = (ChSetItem) o;
			c.addData(centername);
			c.addData(area);
			c.addData(color);
		}

		public ChSetItem clone() {
			return (ChSetItem) super.clone();
		}

		public void set(int pos, String value) {
			if ( pos == ChSetColumn.CENTER.getColumn() )
			{
				centername = value;
			}
			else if ( pos == ChSetColumn.AREA.getColumn() )
			{
				area = value;
			}
			else if ( pos == ChSetColumn.COLOR.getColumn() )
			{
				color = value;
			}
		}

	}

	// ChSetItemを使ったJTable拡張
	private class ChSetTable extends JNETable {

		private static final long serialVersionUID = 1L;

		private RowItemList<ChSetItem> rowdata = null;

		public void setRowData(RowItemList<ChSetItem> rowdata) { this.rowdata = rowdata; }
		public RowItemList<ChSetItem> getRowData() { return rowdata; }

		public ChSetTable(boolean b) {
			super(b);
		}

		public ChSetTable(TableModel d, boolean b) {
			super(d, b);
		}

		@Override
		public Object getValueAt(int row, int column) {

			int vrow = this.convertRowIndexToModel(row);
			ChSetItem c = rowdata.get(vrow);
			return c.get(column);

		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {

			int vrow = this.convertRowIndexToModel(row);
			ChSetItem c = rowdata.get(vrow);
			c.set(column,(String) aValue);
			c.fireChanged();

		}

		@Override
		public int getRowCount() {

			return rowdata.size();

		}

	}

}
