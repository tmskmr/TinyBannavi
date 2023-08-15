package tainavi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;


public class VWMainWindow extends JPanel {

	private static final long serialVersionUID = 1L;

	/*
	 * 定数
	 */
	public static enum MWinTab {
		LISTED	("リスト形式"),
		PAPER	("新聞形式"),
		RSVED	("本体予約一覧"),
		RECED	("録画結果一覧"),
		AUTORES	("自動予約一覧"),
		TITLED   ("タイトル一覧"),
		SETTING	("各種設定"),
		RECSET	("レコーダ設定"),
		CHSET	("CH設定"),
		CHSORT	("CHソート設定"),
		CHCONV	("CHコンバート設定"),
		CHDAT	("CHコード設定"),
		;

		String name;

		private MWinTab(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public int getIndex() {
			return ordinal();
		}

		public static MWinTab getAt(int index) {
			for ( MWinTab tab : MWinTab.values() ) {
				if ( tab.ordinal() == index ) {
					return tab;
				}
			}
			return null;
		}

		public static int size() { return MWinTab.values().length; }

	}

	private final String SETTING_LIST = "設定一覧";

	private Bounds bounds = null;
	private TabInfoList tabitems = null;

	/*
	 * 部品
	 */

	private JSplitPane jSplitPane = null;
	private JTabbedPane jTabbedPane = null;
	private JTabbedPane jTabbedPane_settings = null;
	private VWStatusTextArea statusArea = null;


	/*
	 *  コンストラクタ
	 */

	public VWMainWindow(Viewer v) {
		bounds = v.getBoundsEnv();
		tabitems = v.getTabItemEnv();

		this.setLayout(new BorderLayout());
		this.add(getJSplitPane(), BorderLayout.CENTER);

		jSplitPane.setTopComponent(getJTabbedPane());
		getJTabbedPane_settings();

		addAllTabs();
	}

	/*
	 * 公開メソッド
	 */

	// ツールバーを追加する
	public void addToolBar(Component comp){
		this.add(comp, BorderLayout.PAGE_START);
	}

	/*
	 * ステータスエリア
	 */
	public void addStatusArea(VWStatusTextArea comp) {
		statusArea = comp;
		jSplitPane.setBottomComponent(comp);
	}

	/*
	 * ステータスエリアの行数を設定する
	 */
	public void setStatusAreaRows(int rows){
		setStatusAreaHeight(statusArea.getHeightFromRows(rows));
	}

	/*
	 * ステータスエリアの高さを初期化する
	 */
	public void initStatusAreaHeight(){
		if (bounds.getStatusRows() > 0){
			resetStatusAreaHeight();
		}
		else{
			int sh = bounds.getStatusWindowHeight();
			if (sh <= 1)
				resetStatusAreaHeight();
			else
				setStatusAreaHeight(sh);
		}
	}

	/*
	 * ステータス絵アリアの高さを設定する
	 */
	public void setStatusAreaHeight(int sh){
		int h = jSplitPane.getHeight();
		jSplitPane.setDividerLocation(h-sh);
		bounds.setStatusWindowHeight(sh);
	}

	/*
	 * ステータスエリアの高さをリセットする
	 */
	public void resetStatusAreaHeight(){
		int rows = bounds.getStatusRows();
		int sh = statusArea.getHeightFromRows(rows > 0 ? rows : 5);
		bounds.setStatusWindowHeight(sh);

		setStatusAreaHeight(sh);
	}

	/*
	 * ステータスエリアの表示・非表示を設定する
	 */
	public void setStatusAreaVisible(boolean b){
		statusArea.setVisible(b);

		if (b)
			initStatusAreaHeight();
	}

	/*
	 * ジオメトリ情報を初期化する
	 */
	public void initBounds(){
		initStatusAreaHeight();

		jSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
				@Override
		        public void propertyChange(PropertyChangeEvent pce) {
					if (statusArea.isVisible()){
						int h = jSplitPane.getHeight();
						bounds.setStatusWindowHeight(h-jSplitPane.getDividerLocation());
					}
				}
			});
	}

	/*
	 * 設定情報を反映する
	 */
	public void reflectEnv(){
		addAllTabs();
	}

	/*
	 * すべてのタブを追加する
	 */
	public void addAllTabs(){
		jTabbedPane.removeAll();
		jTabbedPane_settings.removeAll();

		// タブを全部準備する
		for ( MWinTab tab : MWinTab.values() ) {
			// 設定タブでない非表示のタブはスキップする
			if (!isSettingTab(tab) && tabitems.getVisibleAt(tab.getName()) == false)
				continue;

			// 設定タブを追加する
			if ( tab == MWinTab.SETTING ) {
				jTabbedPane.add(jTabbedPane_settings, SETTING_LIST);
			}

			// タブを追加する
			addTab(null, tab);
		}
	}

	/*
	 *  タブを追加する
	 */
	public boolean addTab(Component comp, MWinTab tab) {
		int index = 0;
		JTabbedPane pane = jTabbedPane;

		// 追加先のペインとインデックスを決める
		if ( isSettingTab(tab) ) {
			pane = jTabbedPane_settings;
			index = getSettingTabIndex(tab);
		}
		else{
			pane = jTabbedPane;
			index = getTabIndex(tab);
		}

		// 表示対象でない場合はなにもしない
		if (index == -1)
			return false;

		// すでにタブがあればいったん削除する
		if ( pane.getTabCount() > index ) {
			pane.remove(index);
		}

		// ペインにタブを追加する
		pane.add(comp, tab.getName(), index);

		return true;
	}

	/*
	 *  タブを切り替える
	 */
	public void setSelectedTab(MWinTab tab) {
		if ( tab == null ) {
			jTabbedPane.setSelectedIndex(-1);
			return;
		}

		// 設定タブの場合
		if ( isSettingTab(tab) ) {
			jTabbedPane_settings.setSelectedIndex(getSettingTabIndex(tab));
			jTabbedPane.setSelectedIndex(getTabIndex(MWinTab.SETTING));
			return;
		}

		// それ以外のタブの場合
		// 非表示のタブの場合無視する
		int index = getTabIndex(tab);
		if (index == -1)
			return;

		// タブを選択する
		jTabbedPane.setSelectedIndex(index);
	}

	/*
	 *  設定タブを初期選択する
	 */
	public void initSettingTabSelect(){
		if (jTabbedPane_settings.getTabCount() > 0)
			jTabbedPane_settings.setSelectedIndex(0);
	}

	/*
	 * 指定されたタブのコンポーネントを取得する
	 */
	public Component getTab(MWinTab tab) {
		// 設定タブの場合
		if ( isSettingTab(tab) ) {
			return jTabbedPane_settings.getComponent(getSettingTabIndex(tab));
		}

		// 非表示のタブの場合無視する
		int index = getTabIndex(tab);
		if (index == -1)
			return null;

		return jTabbedPane.getComponent(index);
	}

	/*
	 *  タブが選択されているか確認する
	 */
	public boolean isTabSelected(MWinTab tab) {
		// 設定タブの場合
		if ( isSettingTab(tab) ) {
			return (jTabbedPane.getSelectedIndex() == getTabIndex(MWinTab.SETTING) &&
					jTabbedPane_settings.getSelectedIndex() == getSettingTabIndex(tab));
		}

		// 非表示のタブの場合無視する
		int index = getTabIndex(tab);
		if (index == -1)
			return false;

		return (jTabbedPane.getSelectedIndex() == index);
	}

	// どのタブが選択されているのやら
	public MWinTab getSelectedTab() {
		if ( jTabbedPane.getSelectedIndex() == getTabIndex(MWinTab.SETTING) ) {
			return getSettingTabAt(jTabbedPane_settings.getSelectedIndex());
		}
		return getTabAt(jTabbedPane.getSelectedIndex());
	}

	/*
	 *  タブの表示INDEXを取得する
	 */
	public int getTabIndex(MWinTab tab){
		int index=0;
		for ( MWinTab tab2 : MWinTab.values() ) {
			String name = tab2 == MWinTab.SETTING ? SETTING_LIST : tab2.getName();

			if (tabitems.getVisibleAt(name) == false)
				continue;

			if (tab.getIndex() == tab2.getIndex())
				return index;

			index++;
		}

		return -1;
	}

	/*
	 *  設定タブの表示INDEXを取得する
	 */
	public int getSettingTabIndex(MWinTab tab){
		return tab.getIndex() - MWinTab.SETTING.getIndex();
	}

	/*
	 *  指定されたINDEXのタブを取得する
	 */
	public MWinTab getTabAt(int index){
		int index2=0;
		for ( MWinTab tab : MWinTab.values() ) {
			if (tabitems.getVisibleAt(tab.getName()) == false)
				continue;

			if (index == index2)
				return tab;

			index2++;
		}

		return null;
	}

	/*
	 *  指定されたINDEXの設定タブを取得する
	 */
	public MWinTab getSettingTabAt(int index){
		return MWinTab.getAt(MWinTab.SETTING.getIndex()+index);
	}

	/*
	 *  指定されたタブは設定用のタブか
	 */
	public boolean isSettingTab(MWinTab tab){
		return tab.getIndex() >= MWinTab.SETTING.getIndex();
	}

	// 設定タブをトグル切り替え
	private final int firstSettingTab = MWinTab.SETTING.ordinal();
	private final int countSettingTab = MWinTab.size()-firstSettingTab;
	private Component[] st_comp = new Component[countSettingTab];
	private String[] st_title = new String[countSettingTab];
	public boolean toggleShowSettingTabs() {
		return true;
	}

	public boolean getShowSettingTabs() {
		return true;
	}
	public void setShowSettingTabs(boolean b) {
	}


	/*
	 *
	 */

	private JSplitPane getJSplitPane() {
		if (jSplitPane == null){
			jSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			jSplitPane.setResizeWeight(1.0);
		}

		return jSplitPane;
	}

	private JTabbedPane getJTabbedPane() {
		if (jTabbedPane == null) {
			jTabbedPane = new JTabbedPane();
		}
		return jTabbedPane;
	}

	private JTabbedPane getJTabbedPane_settings() {
		if (jTabbedPane_settings == null) {
			jTabbedPane_settings = new JTabbedPane();
		}
		return jTabbedPane_settings;
	}

	/**
	 * @deprecated
	 */
	public void appendStatusMessage(String s) {
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated
	 * @see Viewer#setStatusVisible(boolean)
	 */
	public void setStatusVisible(boolean b) {
		throw new UnsupportedOperationException();
	}

}
