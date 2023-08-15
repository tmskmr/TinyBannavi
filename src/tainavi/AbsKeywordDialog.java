package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;

import tainavi.SearchKey.TargetId;
import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgSubgenre;


/**
 * キーワード検索の設定のクラス（延長警告管理でも流用する）
 */
abstract class AbsKeywordDialog extends JEscCancelDialog {

	private static final long serialVersionUID = 1L;

	public void setWindowTitle(String s) { windowTitle = s; }

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	abstract void preview(SearchKey search);
	abstract ListedColumnInfoList getLvItemEnv();

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連

	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	private static final int SEP_HEIGHT_NARROW = 3;

	private static final int LABEL_WIDTH_S = 60;
	private static final int LABEL_WIDTH_L = 120;

	private static final int TEXT_WIDTH = 300;

	private static final int BUTTON_WIDTH = 75;

	private static final int TARGET_WIDTH = 175;
	private static final int REGEX_WIDTH = 300;

	private static final int CHECKBOX_WIDTH = 200;
	private static final int CHECKLABEL_WIDTH = CHECKBOX_WIDTH-25;

	private static final int CONDITION_WIDTH = 220;

	private static final int SELECTION_WIDTH = 150;

	private static final int TABLE_WIDTH = REGEX_WIDTH+TARGET_WIDTH*2+SEP_WIDTH*2;	// スクロールバー分
	private static final int TABLEPANE_WIDTH = TABLE_WIDTH+25;	// スクロールバー分
	private static final int TABLE_HEIGHT = 160;

	private static final int PANEL_WIDTH = TABLEPANE_WIDTH+BUTTON_WIDTH+SEP_WIDTH*3;

	// カラム関連

	private static enum KDColumn {
		TARGET		("",	TARGET_WIDTH+SEP_WIDTH/2),
		REGEX		("",	REGEX_WIDTH+SEP_WIDTH),
		CONTAIN		("",	TARGET_WIDTH+SEP_WIDTH/2)
		;

		private String name;
		private int iniWidth;

		private KDColumn(String name, int iniWidth) {
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

	private JPanel jPanel = null;
	private JLabel jLabel_label = null;
	private JTextField jTextField_label = null;
	private JButton jButton_label = null;
	private JButton jButton_cancel = null;
	private JButton jButton_preview = null;
	private JComboBox jComboBox_target = null;
	private JComboBox jComboBox_regex = null;
	private JComboBox jComboBox_contain = null;
	private JButton jButton_add = null;
	private JButton jButton_replace = null;
	private JButton jButton_remove = null;
	private JComboBox jComboBox_condition = null;
	private JComboBox jComboBox_infection = null;
	private JScrollPane jScrollPane_keywords = null;
	private KDTable jTable_keywords = null;
	private JLabel jLabel_okiniiri = null;
	private JComboBox jComboBox_okiniiri = null;
	private JLabel jLabel_group = null;
	private JComboBox jComboBox_group = null;
	private JCheckBoxPanel jCheckBox_caseSensitive = null;
	private JCheckBoxPanel jCheckBox_showInStandby = null;
	private JButton jButton_up = null;
	private JButton jButton_down = null;

	private JLabel jLabel_sortBy = null;
	private JComboBox<String> jComboBox_sortBy = null;
	private JLabel jLabel_sortDir = null;
	private JComboBox<String> jComboBox_sortDir = null;

	// コンポーネント以外

	private String windowTitle = "";
	private SearchProgram xKeys = null;
	private SearchGroupList xGroups = null;
	private SearchKey xKey = null;

	private boolean reg = false;

	private int selectedRow = -1;

	private ArrayList<TargetId> target_items = new ArrayList<TargetId>();
	private ArrayList<String> contain_items = new ArrayList<String>();
	private ArrayList<String> condition_items = new ArrayList<String>();
	private ArrayList<String> infection_items = new ArrayList<String>();
	private ArrayList<String> okiniiri_items = new ArrayList<String>();
	public void clean_target_items() { target_items.clear(); }
	public void add_target_item(TargetId ti) { target_items.add(ti); }
	public void clean_contain_items() { contain_items.clear(); }
	public void add_contain_item(String s) { contain_items.add(s); }
	public void clean_condition_items() { condition_items.clear(); }
	public void add_condition_item(String s) { condition_items.add(s); }
	public void clean_infection_items() { infection_items.clear(); }
	public void add_infection_item(String s) { infection_items.add(s); }
	public void clean_okiniiri_items() { okiniiri_items.clear(); }
	public void add_okiniiri_item(String s) { okiniiri_items.add(s); }

	// 分類的にここでないような

	public String getNewLabel() { return jTextField_label.getText(); }
	public String getNewGroup() { return (String)jComboBox_group.getSelectedItem(); }

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsKeywordDialog() {

		super();

		//
		reg = false;

		//
		setItems();

		//
		this.setModal(true);
		this.setContentPane(getJPanel());
/* - JDialogでは意味がないのか？ -
		// タイトルバーの高さも考慮する必要がある
		Dimension d = getContentPane().getPreferredSize();
		this.setSize(new Dimension(
				d.width+(this.getInsets().left+this.getInsets().right),
				d.height+(this.getInsets().top+this.getInsets().bottom)));
*/
		this.pack();
		this.setResizable(false);
		//
		this.setTitle(windowTitle);
	}


	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 公開メソッド

	/**
	 *  検索条件が登録が実行されたかな？
	 */
	public boolean isRegistered() { return reg; }

	/***************************************
	 * 新規オープン２種＋１種
	 **************************************/

	// キーワード検索管理としてのオープン（右クリックメニューから新規）
	public void open(SearchProgram sKeys, SearchGroupList gList, ProgDetailList tvd) {

		SearchKey sK = new SearchKey();

		// タイトル
		{
			sK.setCondition("0");
			sK.alTarget.add(TargetId.TITLE);
			sK.alContain.add("0");
			sK.alKeyword.add(tvd.title);
		}

		// 放送局
		{
			sK.setCondition("0");
			sK.alTarget.add(TargetId.CHANNEL);
			sK.alContain.add("0");
			sK.alKeyword.add(tvd.center);
		}

		sK.setCaseSensitive(false);
		sK.setShowInStandby(true);

		open(tvd.title,sKeys,gList,sK);
	}

	// キーワード検索管理としてのオープン（ツールバーから新規）
	public void open(String label, SearchProgram sKeys, SearchGroupList gList, SearchKey sK) {

		xKeys = sKeys;
		xGroups = gList;
		xKey = null;

		// テーブルの作り直し
		jComboBox_target.setSelectedIndex(0);
		jComboBox_contain.setSelectedIndex(0);
		jComboBox_condition.setSelectedIndex(0);
		for (int i=jTable_keywords.getRowCount()-1; i>=0; i--) {
			jTable_keywords.getRowItemList().remove(i);
		}

		for (int n=0; n<sK.alTarget.size(); n++) {
			KDItem data = new KDItem();
			data.target = sK.alTarget.get(n);

			if (n < sK.alKeyword.size()){
				data.regex = sK.alKeyword.get(n);
				if (data.target == TargetId.LENGTH)
					data.regex += "分以上である";
			}

			data.contain = contain_items.get(Integer.valueOf(sK.alContain.get(n)));
			data.fireChanged();

			jTable_keywords.getRowItemList().add(data);
		}

		jTable_keywords.fireChanged();

		// ラベル
		jTextField_label.setText(label);
		jTextField_label.setCaretPosition(0);

		// グループリスト
		if ( xGroups != null && xGroups.size() != 0 ) {
			jComboBox_group.setEnabled(true);
			jComboBox_group.addItem("");
			for ( SearchGroup gr : xGroups ) {
				jComboBox_group.addItem(gr.getName());
			}
		}

		// オプション
		jComboBox_okiniiri.setEnabled(true);
		jCheckBox_caseSensitive.setSelected(sK.getCaseSensitive());
		jCheckBox_showInStandby.setSelected(sK.getShowInStandby());
		jCheckBox_showInStandby.setEnabled(true);

		// ソート順
		updateSortByComboBox();
		setSortByAndDir(sK.getSortBy());

		//
		jButton_label.setText("登録");
	}

	// 延長警告管理としてのオープン（新規）
	public void open(String title, String center, boolean isInfection, SearchProgram sKeys) {
		//
		xKeys = sKeys;
		xGroups = null;
		xKey = null;
		//
		if (isInfection) {
			jTextField_label.setText("●"+title);
		}
		else {
			jTextField_label.setText("○"+title);
		}
		jTextField_label.setCaretPosition(0);

		jComboBox_regex.addItem("");
		jComboBox_target.setSelectedIndex(0);
		jComboBox_contain.setSelectedIndex(0);
		jComboBox_condition.setSelectedIndex(0);
		for (int i=jTable_keywords.getRowCount()-1; i>=0; i--) {
			jTable_keywords.getRowItemList().remove(i);
		}
		//
		KDItem d1 = new KDItem();
		d1.target = TargetId.TITLE;
		d1.regex = title;
		d1.contain = (String) jComboBox_contain.getItemAt(0);
		d1.fireChanged();
		jTable_keywords.getRowItemList().add(d1);

		KDItem d2 = new KDItem();
		d2.target = TargetId.CHANNEL;
		d2.regex = center;
		d2.contain = (String) jComboBox_contain.getItemAt(0);
		d2.fireChanged();
		if (isInfection) {
			jComboBox_infection.setSelectedItem(infection_items.get(0));
		}
		else {
			jComboBox_infection.setSelectedItem(infection_items.get(1));
		}
		jTable_keywords.getRowItemList().add(d2);

		jTable_keywords.fireChanged();

		// オプション
		jComboBox_okiniiri.setEnabled(false);
		jCheckBox_caseSensitive.setSelected(false);
		jCheckBox_showInStandby.setSelected(false);
		jCheckBox_showInStandby.setEnabled(false);

		// ソート順
		jLabel_sortBy.setVisible(false);
		jComboBox_sortBy.setVisible(false);
		jLabel_sortDir.setVisible(false);
		jComboBox_sortDir.setVisible(false);

		//
		jButton_label.setText("登録");
	}

	/***************************************
	 * 新規オープン１種＋１種＋α
	 **************************************/

	// キーワード検索管理としてのオープン（更新）
	public void reopen(String label, SearchProgram sKeys) {

		xKeys = sKeys;
		xGroups = null;

		for (SearchKey k : xKeys.getSearchKeys()) {
			if (k.getLabel().equals(label)) {
				// 操作対象をみつけた
				_reopen(sKeys, k);
				break;
			}
		}
	}

	// 延長警告管理としてのオープン（更新）
	public void reopen(String label, ExtProgram sKeys) {

		xKeys = sKeys;
		xGroups = null;

		for (SearchKey k : sKeys.getSearchKeys()) {
			if (k.getLabel().equals(label)) {
				// 操作対象をみつけた
				int idx = Integer.valueOf(k.getInfection());
				if (jComboBox_infection.getItemCount() >= idx) {
					jComboBox_infection.setSelectedIndex(idx);
				}
				_reopen((SearchProgram)sKeys, k);
				break;
			}
		}
	}

	private void _reopen(SearchProgram sKeys, SearchKey sKey) {

		xKey = sKey;

		jComboBox_regex.addItem("");
		jComboBox_target.setSelectedIndex(0);
		jComboBox_contain.setSelectedIndex(0);
		jComboBox_condition.setSelectedIndex(0);
		for (int i=jTable_keywords.getRowCount()-1; i>=0; i--) {
			jTable_keywords.getRowItemList().remove(i);
		}

		jComboBox_condition.setSelectedIndex(Integer.valueOf(xKey.getCondition()));

		Matcher ma = Pattern.compile("(.*?)\t").matcher(xKey.getTarget());
		Matcher mb = Pattern.compile("(.*?)\t").matcher(xKey.getKeyword());
		Matcher mc = Pattern.compile("(.*?)\t").matcher(xKey.getContain());
		while (ma.find()) {
			mb.find();
			mc.find();
			KDItem data = new KDItem();
			data.target = TargetId.getTargetId(ma.group(1));
			data.regex = mb.group(1);
			data.contain = contain_items.get(Integer.valueOf(mc.group(1)));
			data.fireChanged();
			jTable_keywords.getRowItemList().add(data);
		}

		jComboBox_okiniiri.setSelectedItem(xKey.getOkiniiri());

		jCheckBox_caseSensitive.setSelected(xKey.getCaseSensitive());
		if ( sKeys instanceof ExtProgram ) {
			jComboBox_okiniiri.setEnabled(false);
			jCheckBox_showInStandby.setSelected(false);
			jCheckBox_showInStandby.setEnabled(false);
		}
		else {
			jComboBox_okiniiri.setEnabled(true);
			jCheckBox_showInStandby.setSelected(xKey.getShowInStandby());
			jCheckBox_showInStandby.setEnabled(true);
		}

		jTextField_label.setText(xKey.getLabel());
		jTextField_label.setCaretPosition(0);

		// ソート順を解析する
		updateSortByComboBox();
		setSortByAndDir(xKey.getSortBy());

		//
		jTable_keywords.fireChanged();

		//
		jButton_label.setText("更新");
	}

	/**
	 *  コンパイル部分を分離してみました
	 */
	protected SearchKey skCompile() {

		SearchKey sk = new SearchKey();

		String tStr = "";
		String rStr = "";
		String cStr = "";
		for (int row=0; row<jTable_keywords.getRowCount(); row++) {

			KDItem c = jTable_keywords.getRowItemList().get(row);
			TargetId ti = c.target;
			tStr += ti.getId()+"\t";

			if ( ti.getUseRegexpr() ) {
				try {
					Pattern.compile(TraceProgram.replacePop(c.regex));
				}
				catch (PatternSyntaxException ex) {
					showExprFmtWarn(c.contain);
					return null;
				}
			}
			rStr += c.regex+"\t";

			for (int i=0; i<contain_items.size(); i++) {
				if (contain_items.get(i).equals(c.contain)) {
					cStr += i+"\t";
					break;
				}
			}
		}
		sk.setTarget(tStr);		// compile後は順番が変わるので残すことにする
		sk.setKeyword(rStr);	// 同上
		sk.setContain(cStr);	// 同上

		for (int i=0; i<condition_items.size(); i++) {
			if (condition_items.get(i).equals((String) jComboBox_condition.getSelectedItem())) {
				sk.setCondition(String.valueOf(i));
				break;
			}
		}
		for (int i=0; i<infection_items.size(); i++) {
			if (infection_items.get(i).equals((String) jComboBox_infection.getSelectedItem())) {
				sk.setInfection(String.valueOf(i));
				break;
			}
		}

		sk.setLabel(jTextField_label.getText());
		sk.setOkiniiri((String) jComboBox_okiniiri.getSelectedItem());
		sk.setCaseSensitive(jCheckBox_caseSensitive.isSelected());
		sk.setShowInStandby(jCheckBox_showInStandby.isSelected());

		// ソート順
		sk.setSortBy(getSortByAndDir());

		new SearchProgram().compile(sk);

		return sk;
	}

	/**
	 * 正規表現がおかしかったら警告したい
	 * @param expr
	 */
	private void showExprFmtWarn(String expr) {
		JOptionPane.showMessageDialog(this, "正規表現の文法に則っていません： "+expr, "エラー", JOptionPane.ERROR_MESSAGE);
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * 条件を保存する
	 */
	private final ActionListener al_save = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// ウィンドウを閉じる
			if ( addToSearchKeyList() ) {
				dispose();
			}
		}
	};

	private boolean addToSearchKeyList() {
		if (jTextField_label.getText().equals("") || jTable_keywords.getRowCount()<=0) {
			return false;
		}

		// 重複登録を許さない
		for ( SearchKey k : xKeys.getSearchKeys() ) {
			if ( k != xKey && k.getLabel().equals(getNewLabel()) ) {
				JOptionPane.showConfirmDialog(this, "既に登録されています:"+getNewLabel(), "警告", JOptionPane.CLOSED_OPTION);							// キーワード検索の追加ではダイアログで修正できるので止めない
				return false;
			}
		}

		SearchKey sk = skCompile();
		if (sk == null) {
			return false;
		}

		// 検索キーワードを保存する
		if ( xKey == null ) {
			xKeys.add(sk);
		}
		else {
			xKeys.replace(xKey, sk);
		}
		reg = xKeys.save();

		// グループに登録するかも
		String grpName = (String)jComboBox_group.getSelectedItem();
		if ( grpName != null && grpName.length() > 0 ) {
			xGroups.add(grpName,sk.getLabel());
			xGroups.save();
		}

		return reg;
	}

	/**
	 * キャンセルしたい
	 */
	private final ActionListener al_cancel = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			doCancel();
		}
	};

	@Override
	protected void doCancel() {
		dispose();
	}

	/**
	 * プレビューしたい
	 */
	private final ActionListener al_preview = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			SearchKey sk = skCompile();
			if (jTable_keywords.getRowCount() > 0 && sk != null) {
				preview(sk);
			}
		}
	};

	/**
	 * 条件選択コンボボックスが選択された
	 */
	private final ItemListener il_targetChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			//
			if ( e.getStateChange() != ItemEvent.SELECTED ) {
				return;
			}
			//
			TargetId target = (TargetId) jComboBox_target.getSelectedItem();

			jComboBox_regex.setEditable(target.getUseRegexpr());
			jComboBox_regex.setEnabled(target.getUseKeyword());
			jComboBox_regex.removeAllItems();

			if (target == TargetId.LENGTH) {
				for (int i=1; i <= 120; i++) {
					jComboBox_regex.addItem(i+" 分以上である");
				}
				jComboBox_regex.setSelectedIndex(29);
			}
			else if (target == TargetId.STARTA) {
				for (int i=0; i < 24; i++) {
					jComboBox_regex.addItem(String.format("%02d:00以降である", (i+5)%24));
				}
				jComboBox_regex.setSelectedIndex(18-5);
			}
			else if (target == TargetId.STARTZ) {
				for (int i=0; i < 24; i++) {
					jComboBox_regex.addItem(String.format("%02d:00以前である", (i+5)%24));
				}
				jComboBox_regex.setSelectedIndex(23-5);
			}
			else if (target == TargetId.GENRE) {
				for ( ProgGenre genre : ProgGenre.values()) {
					jComboBox_regex.addItem(genre.toString());
				}
			}
			else if (target == TargetId.SUBGENRE) {
				for ( ProgSubgenre subgenre : ProgSubgenre.values()) {
					jComboBox_regex.addItem(subgenre.toFullString());
				}
			}
			else {
				jComboBox_regex.addItem("");
			}
		}
	};


	/**
	 * 条件を追加する
	 */
	private final ActionListener al_addTarget = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			TargetId ti = (TargetId) jComboBox_target.getSelectedItem();
			String re = (String) jComboBox_regex.getSelectedItem();

			if ( re == null || ti.getUseKeyword() && re.length() == 0 ) {
				// キーワードが必要なのに入力されていなければＮＧ
				return;
			}

			KDItem data = new KDItem();
			data.target = ti;
			data.regex = re;
			data.contain = (String) jComboBox_contain.getSelectedItem();
			data.fireChanged();
			if ( selectedRow == -1 ) {
				jTable_keywords.getRowItemList().add(data);
			}
			else {
				jTable_keywords.getRowItemList().add(selectedRow+1, data);
			}
			jTable_keywords.fireChanged();

			if (jComboBox_regex.getItemCount() <= 1) {
				jComboBox_regex.removeAllItems();
				jComboBox_regex.addItem("");
			}

			selectedRow = -1;
			jButton_replace.setEnabled(false);
			jButton_remove.setEnabled(false);
		}
	};


	/**
	 * 条件を置換する
	 */
	private final ActionListener al_replaceTarget = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if ( selectedRow == -1 ) {
				return;
			}

			TargetId ti = (TargetId) jComboBox_target.getSelectedItem();
			String re = (String) jComboBox_regex.getSelectedItem();

			if ( ti.getUseKeyword() && re.length() == 0 ) {
				// キーワードが必要なのに入力されていなければＮＧ
				return;
			}

			KDItem data = jTable_keywords.getRowItemList().get(selectedRow);
			data.target = ti;
			data.regex = re;
			data.contain = (String) jComboBox_contain.getSelectedItem();
			data.fireChanged();
			jTable_keywords.fireChanged();

			if (jComboBox_regex.getItemCount() <= 1) {
				jComboBox_regex.removeAllItems();
				jComboBox_regex.addItem("");
			}

			selectedRow = -1;
			jButton_replace.setEnabled(false);
			jButton_remove.setEnabled(false);
		}
	};


	/**
	 * 条件を削除する（コンボボックスに戻す）
	 */
	private final ActionListener al_removeTarget = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if ( selectedRow == -1 ) {
				return;
			}

			jTable_keywords.getRowItemList().remove(selectedRow);
			jTable_keywords.fireChanged();

			selectedRow = -1;
			jButton_replace.setEnabled(false);
			jButton_remove.setEnabled(false);
		}
	};

	/**
	 *
	 */
	private final ActionListener al_upTarget = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int row = jTable_keywords.getSelectedRow();
			if ( jTable_keywords.getRowItemList().up(row, 1) ) {
				jTable_keywords.fireChanged();
				jTable_keywords.setRowSelectionInterval(row-1,row-1);
				selectedRow = row-1;
			}
		}
	};

	/**
	 *
	 */
	private final ActionListener al_downTarget = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			int row = jTable_keywords.getSelectedRow();
			if ( jTable_keywords.getRowItemList().down(row, 1) ) {
				jTable_keywords.fireChanged();
				jTable_keywords.setRowSelectionInterval(row+1,row+1);
				selectedRow = row+1;
			}
		}
	};

	/**
	 * 行を選択したら編集ブロックにコピーする
	 */
	private final MouseAdapter ml_keywordSelected = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			int row = jTable_keywords.getSelectedRow();
			if (row == -1) {
				return;
			}

			KDItem c = jTable_keywords.getRowItemList().get(row);

			jComboBox_target.setSelectedItem(c.target);

			if ( ! c.target.getUseRegexpr() && c.target.getUseKeyword() ) {
				jComboBox_regex.setSelectedItem(null);
				jComboBox_regex.setSelectedItem(c.regex);

				// 旧版互換
				String s = (String) jComboBox_regex.getSelectedItem();
				if ( c.target == TargetId.SUBGENRE ) {
					if ( s == null ) {
						ProgSubgenre subg = ProgSubgenre.get(c.regex);
						if ( subg != null ) {
							jComboBox_regex.setSelectedItem(subg.toFullString());
						}
					}
				}
				else if ( s == null && jComboBox_regex.getItemCount() > 0 ) {
					jComboBox_regex.setSelectedIndex(0);
				}
			}
			else {
				jComboBox_regex.removeAllItems();
				if ( c.target.getUseRegexpr() ) {
					jComboBox_regex.addItem(c.regex);
				}
			}

			jComboBox_contain.setSelectedItem(c.contain);

			selectedRow = row;
			jButton_replace.setEnabled(true);
			jButton_remove.setEnabled(true);
		}
	};


	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();

			jPanel.setLayout(new SpringLayout());

			int sh = ZMSIZE(SEP_HEIGHT);
			int sw = ZMSIZE(SEP_WIDTH);
			int shn = ZMSIZE(SEP_HEIGHT_NARROW);
			int lws = ZMSIZE(LABEL_WIDTH_S);
			int ph = ZMSIZE(PARTS_HEIGHT);
			int pw = ZMSIZE(PANEL_WIDTH);
			int bw = ZMSIZE(BUTTON_WIDTH);
			int tw = ZMSIZE(TEXT_WIDTH);
			int ttw = ZMSIZE(TARGET_WIDTH);
			int rw = ZMSIZE(REGEX_WIDTH);
			int clw = ZMSIZE(CHECKLABEL_WIDTH);
			int cbw = ZMSIZE(CHECKBOX_WIDTH);
			int cdw = ZMSIZE(CONDITION_WIDTH);
			int tbw = ZMSIZE(TABLE_WIDTH);
			int tpw = ZMSIZE(TABLEPANE_WIDTH);
			int th = ZMSIZE(TABLE_HEIGHT);
			int lwl = ZMSIZE(LABEL_WIDTH_L);
			int slw = ZMSIZE(SELECTION_WIDTH);

			int y = sh;
			int x = sw;

			x = sw;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_label("設定名"), lws, ph, x, y);
			CommonSwingUtils.putComponentOn(jPanel, getJTextField_label(), tw, ph, x+=lws, y);

			int xz = pw-(bw+sw);
			CommonSwingUtils.putComponentOn(jPanel, getJButton_label("登録"), bw, ph, xz, y);
			CommonSwingUtils.putComponentOn(jPanel, getJButton_cancel("ｷｬﾝｾﾙ"), bw, ph, xz, y+(shn+ph));
			CommonSwingUtils.putComponentOn(jPanel, getJButton_preview("ﾌﾟﾚﾋﾞｭｰ"), bw, ph, xz, y+(shn+ph)*2);

			y += ph+sh;
			x = sw;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_target(), ttw, ph, x, y);
			CommonSwingUtils.putComponentOn(jPanel, getJTextField_regex(), rw, ph, x+=ttw+sw, y);
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_contain(), ttw, ph, x+=rw+sw, y);

			y += ph+shn;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_caseSensitive("文字列比較は完全一致で",clw,false), cbw, ph, x, y);

			x = sh+tbw/2;
			CommonSwingUtils.putComponentOn(jPanel, getJButton_add("↓追加"), bw, ph, x-(bw+sw*2) , y);
			CommonSwingUtils.putComponentOn(jPanel, getJButton_replace("↓置換"), bw, ph, x+(sw*2), y);


			y += ph+shn;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_condition(), cdw, ph, sw, y);
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_infection(), cdw, ph, tbw-cdw+sw, y);

			y += ph+shn;
			CommonSwingUtils.putComponentOn(jPanel, getJScrollPane_keywords(), tpw, th, sw, y);

			int yz = y + (th/2-(ph+sh/2));
			CommonSwingUtils.putComponentOn(jPanel, getJButton_up("↑"), bw, ph, xz, yz);
			CommonSwingUtils.putComponentOn(jPanel, getJButton_down("↓"), bw, ph, xz, yz+=sh+ph);
			CommonSwingUtils.putComponentOn(jPanel, getJButton_remove("削除"), bw, ph, xz, yz+=sh*2+ph);

			y += th+sh;
			x = sw;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_sortBy("リストのソート列"), lwl, ph, x, y);
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_sortBy(), slw, ph, x+=lwl+sw, y);
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_okiniiri("お気に入り度"), lwl, ph, x+=slw+sw, y);
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_okiniiri(), slw, ph, x+=lwl+sw, y);

			y += ph+sh;
			x = sw;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_sortDir("同ソート方向"), lwl, ph, x, y);
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_sortDir(), slw, ph, x+=lwl+sw, y);
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_group("グループ"), lwl, ph, x+=slw+sw, y);
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_group(), slw, ph, x+=lwl+sw, y);

			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_showInStandby("予約待機に表示する",clw,true), cbw, ph, pw-cbw-sw, y);

			y += ph+sh;

			jPanel.setPreferredSize(new Dimension(pw, y));
		}
		return jPanel;
	}

	/*
	 * フォントサイズを考慮した長さを取得する
	 */
	private int ZMSIZE(int size){ return Env.ZMSIZE(size); }

	//
	private JLabel getJLabel_label(String s) {
		if (jLabel_label == null) {
			jLabel_label = new JLabel(s);
		}
		return(jLabel_label);
	}

	//
	private JTextField getJTextField_label() {
		if (jTextField_label == null) {
			jTextField_label = new JTextFieldWithPopup();
		}
		return(jTextField_label);
	}

	//
	private JButton getJButton_label(String s) {
		if (jButton_label == null) {
			jButton_label = new JButton();
			jButton_label.setText(s);

			jButton_label.addActionListener(al_save);
		}
		return(jButton_label);
	}

	//
	private JButton getJButton_cancel(String s) {
		if (jButton_cancel == null) {
			jButton_cancel = new JButton();
			jButton_cancel.setText(s);

			jButton_cancel.addActionListener(al_cancel);
		}
		return jButton_cancel;
	}

	//
	private JButton getJButton_preview(String s) {
		if (jButton_preview == null) {
			jButton_preview = new JButton();
			jButton_preview.setText(s);

			jButton_preview.addActionListener(al_preview);
		}
		return jButton_preview;
	}


	//
	private JComboBox getJComboBox_target() {
		if (jComboBox_target == null) {
			jComboBox_target = new JComboBox();

			DefaultComboBoxModel aModel = new DefaultComboBoxModel();
			jComboBox_target.setModel(aModel);
			for (TargetId k : target_items) {
				aModel.addElement(k);
			}

			jComboBox_target.addItemListener(il_targetChanged);
		}
		return(jComboBox_target);
	}

	//
	private JComboBox getJTextField_regex() {
		if (jComboBox_regex == null) {
			jComboBox_regex = new JComboBoxWithPopup();
			jComboBox_regex.setEditable(true);
		}
		return(jComboBox_regex);
	}

	//
	private JComboBox getJComboBox_contain() {
		if (jComboBox_contain == null) {
			jComboBox_contain = new JComboBox();

			DefaultComboBoxModel aModel = new DefaultComboBoxModel();
			jComboBox_contain.setModel(aModel);
			for (String k : contain_items) {
				aModel.addElement(k);
			}
		}
		return(jComboBox_contain);
	}

	//
	private JComboBox getJComboBox_condition() {
		if (jComboBox_condition == null) {
			jComboBox_condition = new JComboBox();

			DefaultComboBoxModel aModel = new DefaultComboBoxModel();
			jComboBox_condition.setModel(aModel);
			for (String k : condition_items) {
				aModel.addElement(k);
			}
		}
		return(jComboBox_condition);
	}

	//
	private JComboBox getJComboBox_infection() {
		if (jComboBox_infection == null) {
			jComboBox_infection = new JComboBox();

			if (infection_items.size() > 0) {
    			DefaultComboBoxModel aModel = new DefaultComboBoxModel();
    			jComboBox_infection.setModel(aModel);
    			for (String k : infection_items) {
    				aModel.addElement(k);
    			}
			}
			else {
				jComboBox_infection.setEnabled(false);
			}
		}
		return(jComboBox_infection);
	}

	/*
	 * 「リストのソート列」ラベル
	 */
	private JLabel getJLabel_sortBy(String s) {
		if (jLabel_sortBy == null) {
			jLabel_sortBy = new JLabel(s, SwingConstants.RIGHT);
		}
		return(jLabel_sortBy);
	}

	/*
	 * 「ソート列」コンボボックス
	 */
	private JComboBox<String> getJComboBox_sortBy() {
		if (jComboBox_sortBy == null) {
			jComboBox_sortBy = new JComboBox<String>();

			updateSortByComboBox();
		}
		return(jComboBox_sortBy);
	}

	/*
	 * 「同ソート方向」ラベル
	 *
	 */
	private JLabel getJLabel_sortDir(String s) {
		if (jLabel_sortDir == null) {
			jLabel_sortDir = new JLabel(s, SwingConstants.RIGHT);
		}
		return(jLabel_sortDir);
	}

	/*
	 * 「ソート方向」コンボボックス
	 */
	private JComboBox<String> getJComboBox_sortDir() {
		if (jComboBox_sortDir == null) {
			jComboBox_sortDir = new JComboBox<String>();

			jComboBox_sortDir.addItem("　");
			jComboBox_sortDir.addItem("昇順");
			jComboBox_sortDir.addItem("降順");
		}
		return(jComboBox_sortDir);
	}

	//
	private JLabel getJLabel_okiniiri(String s) {
		if (jLabel_okiniiri == null) {
			jLabel_okiniiri = new JLabel(s,SwingConstants.RIGHT);
		}
		return(jLabel_okiniiri);
	}
	private JComboBox getJComboBox_okiniiri() {
		if (jComboBox_okiniiri == null) {
			jComboBox_okiniiri = new JComboBox();

			DefaultComboBoxModel aModel = new DefaultComboBoxModel();
			jComboBox_okiniiri.setModel(aModel);
			for (String k : okiniiri_items) {
				aModel.addElement(k);
			}
		}
		return(jComboBox_okiniiri);
	}


	//
	private JLabel getJLabel_group(String s) {
		if (jLabel_group == null) {
			jLabel_group = new JLabel(s,SwingConstants.RIGHT);
		}
		return(jLabel_group);
	}
	private JComboBox getJComboBox_group() {
		if (jComboBox_group == null) {
			jComboBox_group = new JComboBox();

			jComboBox_group.setEnabled(false);
		}
		return(jComboBox_group);
	}

	//
	private JCheckBoxPanel getJCheckBox_caseSensitive(String s,int labelWidth, boolean b) {
		if (jCheckBox_caseSensitive == null) {
			jCheckBox_caseSensitive = new JCheckBoxPanel(s,labelWidth,true);
			jCheckBox_caseSensitive.setSelected(b);
		}
		return(jCheckBox_caseSensitive);
	}

	//
	private JCheckBoxPanel getJCheckBox_showInStandby(String s,int labelWidth, boolean b) {
		if (jCheckBox_showInStandby == null) {
			jCheckBox_showInStandby = new JCheckBoxPanel(s,labelWidth,true);
			jCheckBox_showInStandby.setSelected(b);
		}
		return(jCheckBox_showInStandby);
	}

	// 条件追加のボタン
	private JButton getJButton_add(String s) {
		if (jButton_add == null) {
			jButton_add = new JButton();
			jButton_add.setText(s);
			jButton_add.setEnabled(true);

			jButton_add.addActionListener(al_addTarget);
		}
		return(jButton_add);
	}

	// 条件置換のボタン
	private JButton getJButton_replace(String s) {
		if (jButton_replace == null) {
			jButton_replace = new JButton();
			jButton_replace.setText(s);
			jButton_replace.setEnabled(false);
			jButton_replace.setForeground(Color.BLUE);

			jButton_replace.addActionListener(al_replaceTarget);
		}
		return(jButton_replace);
	}

	// 条件削除のボタン
	private JButton getJButton_remove(String s) {
		if (jButton_remove == null) {
			jButton_remove = new JButton();
			jButton_remove.setText(s);
			jButton_remove.setEnabled(false);
			jButton_remove.setForeground(Color.RED);

			jButton_remove.addActionListener(al_removeTarget);
		}
		return(jButton_remove);
	}

	private JButton getJButton_up(String s) {
		if (jButton_up == null) {
			jButton_up = new JButton();
			jButton_up.setText(s);

			jButton_up.addActionListener(al_upTarget);
		}
		return(jButton_up);
	}

	private JButton getJButton_down(String s) {
		if (jButton_down == null) {
			jButton_down = new JButton();
			jButton_down.setText(s);

			jButton_down.addActionListener(al_downTarget);
		}
		return(jButton_down);
	}

	//
	private JScrollPane getJScrollPane_keywords() {
		if (jScrollPane_keywords == null) {
			jScrollPane_keywords = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			jScrollPane_keywords.setViewportView(getJTable_keywords());
		}
		return jScrollPane_keywords;
	}

	//
	private KDTable getJTable_keywords() {
		if (jTable_keywords == null) {

			jTable_keywords = new KDTable(false, new RowItemList<KDItem>());

			//　テーブルの基本的な設定
			ArrayList<String> cola = new ArrayList<String>();
			for ( KDColumn rc : KDColumn.values() ) {
				if ( rc.getIniWidth() >= 0 ) {
					cola.add(rc.getName());
				}
			}
			jTable_keywords.setModel(new DefaultTableModel(cola.toArray(new String[0]), 0));

			jTable_keywords.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
			jTable_keywords.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTable_keywords.getTableHeader().setReorderingAllowed(false);
			jTable_keywords.setToolTipText("dummy");

			// 各カラムの幅
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_keywords.getColumnModel();
			//TableColumn column = null;
			for ( KDColumn rc : KDColumn.values() ) {
				if ( rc.getIniWidth() < 0 ) {
					continue;
				}
				columnModel.getColumn(rc.ordinal()).setPreferredWidth(ZMSIZE(rc.getIniWidth()));
			}

			// 行を選択したら入力に値を戻す
			jTable_keywords.addMouseListener(ml_keywordSelected);
		}
		return(jTable_keywords);
	}

	/*******************************************************************************
	 * 独自コンポーネント
	 ******************************************************************************/

	private class KDItem extends RowItem implements Cloneable {
		TargetId target;
		String regex;
		String contain;

		@Override
		protected void myrefresh(RowItem o) {
			KDItem c = (KDItem) o;
			c.addData(target);
			c.addData(regex);
			c.addData(contain);
		}

		@Override
		public KDItem clone() {
			return (KDItem) super.clone();
		}
	}

	private class KDTable extends JNETable {

		private static final long serialVersionUID = 1L;

		RowItemList<KDItem> rowdata = null;

		public KDTable(boolean b, RowItemList<KDItem> rowdata) {
			super(b);
			this.rowdata = rowdata;
		}

		public RowItemList<KDItem> getRowItemList() { return rowdata; }

		public void fireChanged() { ((DefaultTableModel) this.getModel()).fireTableDataChanged(); }

		@Override
		public int getRowCount() { return rowdata.size(); }

		@Override
		public Object getValueAt(int row, int column) {
			return rowdata.get(row).get(column);
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			KDItem c = rowdata.get(row);
			if ( column == KDColumn.TARGET.getColumn() ) {
				c.target = (TargetId) aValue;
			}
			else if ( column == KDColumn.REGEX.getColumn() ) {
				c.regex = (String) aValue;
			}
			else if ( column == KDColumn.CONTAIN.getColumn() ) {
				c.contain = (String) aValue;
			}
			c.fireChanged();
		}

	}

	/*******************************************************************************
	 * 延長警告管理では以下をオーバーライドしてください
	 ******************************************************************************/

	protected void setItems() {
		setWindowTitle("キーワード検索の設定");

		clean_target_items();
		add_target_item(TargetId.TITLEANDDETAIL);
		add_target_item(TargetId.TITLE);
		add_target_item(TargetId.DETAIL);
		add_target_item(TargetId.CHANNEL);
		add_target_item(TargetId.GENRE);
		add_target_item(TargetId.SUBGENRE);
		add_target_item(TargetId.NEW);
		add_target_item(TargetId.LAST);
		add_target_item(TargetId.REPEAT);
		add_target_item(TargetId.FIRST);
		add_target_item(TargetId.SPECIAL);
		add_target_item(TargetId.RATING);
		add_target_item(TargetId.NOSCRUMBLE);
		add_target_item(TargetId.LIVE);
		add_target_item(TargetId.BILINGUAL);
		add_target_item(TargetId.STANDIN);
		add_target_item(TargetId.MULTIVOICE);
		add_target_item(TargetId.LENGTH);
		add_target_item(TargetId.STARTA);
		add_target_item(TargetId.STARTZ);
		add_target_item(TargetId.STARTDATETIME);

		clean_contain_items();
		add_contain_item("を含む番組");
		add_contain_item("を含む番組を除く");

		clean_condition_items();
		add_condition_item("次のすべての条件に一致");
		add_condition_item("次のいずれかの条件に一致");

		clean_infection_items();
    	//add_infection_item();

		clean_okiniiri_items();
		for (String okini : TVProgram.OKINIIRI) {
			add_okiniiri_item(okini);
		}
	}

	/*******************************************************************************
	 * 内部関数
	 ******************************************************************************/
	/*
	 * ソート列とソート順を更新する
	 *
	 * @param sortBy	ソート列:ソート順の文字列
	 */
	private void setSortByAndDir(String sortBy){
		jComboBox_sortBy.setSelectedIndex(0);
		jComboBox_sortDir.setSelectedIndex(0);

		if (sortBy == null)
			return;

		Matcher ma = Pattern.compile("^(.*):(.*)$").matcher(sortBy);
		if (ma.find()){
			AbsListedView.ListedColumn col = AbsListedView.ListedColumn.getColumnFromName(ma.group(1));
			if (col != null)
				jComboBox_sortBy.setSelectedItem(col.getName());

			try{
				jComboBox_sortDir.setSelectedIndex(Integer.parseInt(ma.group(2)));
			}
			catch(NumberFormatException e){
				e.printStackTrace();
			}
		}
	}

	/*
	 * ソート列コンボボックスを更新する
	 */
	private void updateSortByComboBox(){
		jComboBox_sortBy.removeAllItems();

		jComboBox_sortBy.addItem("　");
		for (ListColumnInfo lci : getLvItemEnv()){
			if (!lci.getVisible())
				continue;
			AbsListedView.ListedColumn col = AbsListedView.ListedColumn.getColumnFromLabel(lci.getName());
			if (col != null)
				jComboBox_sortBy.addItem(col.getName());
		}
	}
	/*
	 * ソート列とソート順を取得する
	 *
	 * @return ソート列:ソート順の文字列
	 */
	private String getSortByAndDir(){
		String sortBy = "";
		int no = jComboBox_sortBy.getSelectedIndex();
		AbsListedView.ListedColumn [] cols = AbsListedView.ListedColumn.values();

		int n=1;
		for (ListColumnInfo lci : getLvItemEnv()){
			if (!lci.getVisible())
				continue;
			AbsListedView.ListedColumn col = AbsListedView.ListedColumn.getColumnFromLabel(lci.getName());
			if (col != null){
				if (n == no){
					sortBy = col.name();
					break;
				}

				n++;
			}
		}

		String sortDir = String.valueOf(jComboBox_sortDir.getSelectedIndex());

		return sortBy + ":" + sortDir;
	}
}
