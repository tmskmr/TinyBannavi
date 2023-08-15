package tainavi;

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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import tainavi.TVProgram.ProgGenre;


/**
 * タイトル一覧タブのクラス
 */
public abstract class AbsTitleListView extends JPanel {

	private static final long serialVersionUID = 1L;

	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;

	private boolean listenerAdded = false;
	private boolean titleUpdating = false;
	private boolean deviceUpdating = false;

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract Bounds getBoundsEnv();
	protected abstract TitleListColumnInfoList getTlItemEnv();

	protected abstract TVProgramList getTVProgramList();
	protected abstract HDDRecorderList getRecorderList();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract Component getParentComponent();

	protected abstract void ringBeep();

	/**
	 * @see Viewer.VWToolBar#getSelectedRecorder()
	 */
	protected abstract String getSelectedRecorderOnToolbar();

	/**
	 *  タイトルの詳細情報を取得するメニューアイテム
	 */
	protected abstract JMenuItem getTitleDetailMenuItem(final String title, final String chnam,
			final String devId, final String ttlId, final String recId);

	/**
	 *  複数のタイトルの詳細情報をまとめて取得するメニューアイテム
	 */
	protected abstract JMenuItem getMultiTitleDetailMenuItem(final String title, final String chnam,
			final String devId, final String [] ttlId, final String recId);

	/*
	 * 番組欄にジャンプするメニューアイテム
	 */
	protected abstract JMenuItem getJumpMenuItem(final String title, final String chnam, final String startDT);

	/**
	 *  タイトルを編集するメニューアイテム
	 */
	protected abstract JMenuItem getEditTitleMenuItem(final String title, final String chnam,
			final String devId, final String ttlId, final String recId, String otitle);

	/**
	 *  タイトルを削除するメニューアイテム
	 */
	protected abstract JMenuItem getRemoveTitleMenuItem(final String title, final String chnam,
			final String devId, final String ttlId, final String recId);

	/**
	 *  複数のタイトルをまとめて削除するメニューアイテム
	 */
	protected abstract JMenuItem getRemoveMultiTitleMenuItem(final String title, final String chnam,
			final String devId, final String [] ttlId, final String recId);

	/**
	 *  複数のタイトルをまとめてフォルダ移動するメニューアイテム
	 */
	protected abstract JMenuItem getMoveMultiTitleMenuItem(final String title, final String chnam,
			final String devId, final String [] ttlId, final String recId);

	/**
	 *  タイトルの再生を開始、終了するメニューアイテム
	 */
	protected abstract JMenuItem getStartStopPlayTitleMenuItem(final boolean start, final String title, final String chnam,
			final String devId, final String ttlId, final String recId);

	/*
	 * プログラムのブラウザーメニューを呼び出す
	 */
	protected abstract void addBrowseMenuToPopup( JPopupMenu pop,	final ProgDetailList tvd );

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String MSGID = "[タイトル一覧] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	private static final int SEP_WIDTH = 10;

	private static final int DLABEL_WIDTH = 70;
	private static final int DCOMBO_WIDTH = 200;
	private static final int DEVICE_WIDTH = DLABEL_WIDTH+DCOMBO_WIDTH;

	private static final int FLABEL_WIDTH = 70;
	private static final int FCOMBO_WIDTH = 400;
	private static final int FCOMBO_OPEN_WIDTH = 600;
	private static final int FOLDER_WIDTH = FLABEL_WIDTH+FCOMBO_WIDTH;
	private static final int FBUTTON_WIDTH = 70;

	private static final int GLABEL_WIDTH = 70;
	private static final int GCOMBO_WIDTH = 200;
	private static final int GENRE_WIDTH = GLABEL_WIDTH+GCOMBO_WIDTH;

	private static final int RELOAD_ALL_WIDTH = 100;
	private static final int RELOAD_IND_WIDTH = 30;

	private static final int PARTS_HEIGHT = 30;
	private static final int RELOAD_HEIGHT = PARTS_HEIGHT*2;
	private static final int DETAIL_HEIGHT = 150;

	public static final String FOLDER_ID_ROOT = "0";

	private static final String CURRENT_COLOR_EVEN = "#f0b4b4";
	private static final String CURRENT_COLOR_ODD = "#f88080";

	private static final String ICONFILE_PULLDOWNMENU	= "icon/down-arrow.png";

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// オブジェクト
	private final Env env = getEnv();
	private final Bounds bounds = getBoundsEnv();
	private final HDDRecorderList recorders = getRecorderList();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト

	private TitleListColumnInfoList tlitems = null;

	/**
	 * カラム定義
	 */

	public static HashMap<String,Integer> getColumnIniWidthMap() {
		if (rcmap.size() == 0 ) {
			for ( TitleColumn rc : TitleColumn.values() ) {
				rcmap.put(rc.toString(),rc.getIniWidth());	// toString()!
			}
		}
		return rcmap;
	}

	private static final HashMap<String,Integer> rcmap = new HashMap<String, Integer>();

	public static enum TitleColumn {
		START		("開始",			200),
		END			("終了",			60),
		LENGTH		("長さ",			60),
		RECMODE		("画質",			60),
		TITLE		("番組タイトル",	300),
		CHNAME		("チャンネル名",	150),
		DEVNAME		("デバイス",		100),
		FOLDER		("フォルダ",		300),
		GENRE		("ジャンル",		100),
		RECORDER	("レコーダ",		250),
		COPYCOUNT	("コピー",			60),
		DLNAOID		("DLNA OID",		100),
		;

		private String name;
		private int iniWidth;

		private TitleColumn(String name, int iniWidth) {
			this.name = name;
			this.iniWidth = iniWidth;
		}

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

	/**
	 * リスト項目定義
	 */
	private class TitleItem extends RowItem implements Cloneable {

		String start;	// YYYY/MM/DD(WD) hh:mm
		String end;			// hh:mm
		String length;
		String recmode;
		String title;
		String chname;
		String devname;
		String folder;
		String genre;
		String recname;
		String recorder;
		String copycount;
		String dlna_oid;

		String hide_ttlid;
		String hide_detail;
		boolean hide_recording;

		@Override
		protected void myrefresh(RowItem o) {
			TitleItem c = (TitleItem) o;

			c.addData(start);
			c.addData(end);
			c.addData(length);
			c.addData(recmode);
			c.addData(title);
			c.addData(chname);
			c.addData(devname);
			c.addData(folder);
			c.addData(genre);
			c.addData(recname);
			c.addData(copycount);
			c.addData(dlna_oid);
			c.addData(recorder);

			c.addData(hide_ttlid);
			c.addData(hide_detail);
			c.addData(hide_recording);
		}

		public TitleItem clone() {
			return (TitleItem) super.clone();
		}
	}

	// ソートが必要な場合はTableModelを作る。ただし、その場合Viewのrowがわからないので行の入れ替えが行えない
	private class TitleTableModel extends DefaultTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public Object getValueAt(int row, int column) {
			TitleItem c = null;
			try{
				c = rowView.get(row);
			}
			catch(IndexOutOfBoundsException e){
				return null;
			}

			ListColumnInfo info = tlitems.getVisibleAt(column);
			if (info == null)
				return null;

			// 特殊なカラム
			int cindex = info.getId()-1;

			if ( cindex == TitleColumn.LENGTH.getColumn() ) {
				return String.valueOf(c.length)+"m";
			}

			if (cindex < c.size()){
				return c.get(cindex);
			}

			return null;
		}

		@Override
		public int getRowCount() {
			return rowView.size();
		}

		public TitleTableModel(String[] colname, int i) {
			super(colname,i);
		}

	}

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JScrollPane jsc_list = null;
	private JScrollPane jsc_detail = null;
	private JTextAreaWithPopup jta_detail = null;

	private JNETable jTable_title = null;
	private JTable jTable_rowheader = null;

	private JComboBoxPanel jCBXPanel_device = null;
	private JComboBoxPanel jCBXPanel_folder = null;
	private JComboBoxPanel jCBXPanel_genre = null;
	private JLabel jLabel_deviceInfo = null;
	private JButton jButton_newFolder = null;
	private JButton jButton_editFolder = null;
	private JButton jButton_removeFolder = null;
	private JButton jButton_reloadDefault = null;
	private JButton jButton_reloadInd = null;
	private JPopupMenu jPopupMenu_reload = null;

	private DefaultTableModel tableModel_title = null;

	private DefaultTableModel rowheaderModel_title = null;

	// 表示用のテーブル
	private final RowItemList<TitleItem> rowView = new RowItemList<TitleItem>();

	// テーブルの実体
	private final RowItemList<TitleItem> rowData = new RowItemList<TitleItem>();

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsTitleListView() {

		super();

		tlitems = (TitleListColumnInfoList) getTlItemEnv().clone();

		SpringLayout layout = new SpringLayout();
		this.setLayout(layout);

		int y1 = 0;
		int y2 = PARTS_HEIGHT;
		int x = SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, jCBXPanel_device = new JComboBoxPanel("デバイス：", DLABEL_WIDTH, DCOMBO_WIDTH, true), DEVICE_WIDTH, PARTS_HEIGHT, x, y1);
		CommonSwingUtils.putComponentOn(this, jLabel_deviceInfo = new JLabel("残量(DR):"), DEVICE_WIDTH, PARTS_HEIGHT, x, y2);
		x += DEVICE_WIDTH + SEP_WIDTH;

		CommonSwingUtils.putComponentOn(this, getFolderComboPanel(), FOLDER_WIDTH, PARTS_HEIGHT, x, y1);
		x += FOLDER_WIDTH;
		CommonSwingUtils.putComponentOn(this, jButton_newFolder = new JButton("F新規"),
				FBUTTON_WIDTH, PARTS_HEIGHT, x-FBUTTON_WIDTH*3, y2);
		CommonSwingUtils.putComponentOn(this, jButton_editFolder = new JButton("F編集"),
				FBUTTON_WIDTH, PARTS_HEIGHT, x-FBUTTON_WIDTH*2, y2);
		CommonSwingUtils.putComponentOn(this, jButton_removeFolder = new JButton("F削除"),
				FBUTTON_WIDTH, PARTS_HEIGHT, x-FBUTTON_WIDTH, y2);
		jButton_removeFolder.setForeground(Color.RED);

		x += SEP_WIDTH;
		CommonSwingUtils.putComponentOn(this, jCBXPanel_genre = new JComboBoxPanel("ジャンル：", GLABEL_WIDTH, GCOMBO_WIDTH, true), GENRE_WIDTH, PARTS_HEIGHT, x, y1);
		jCBXPanel_genre.getJComboBox().setMaximumRowCount(16);
		x += GENRE_WIDTH + SEP_WIDTH;

		CommonSwingUtils.putComponentOn(this, jButton_reloadDefault = new JButton(), RELOAD_ALL_WIDTH, RELOAD_HEIGHT, x, y1);
		jButton_reloadDefault.setText("再取得");
		x += RELOAD_ALL_WIDTH-2;
		CommonSwingUtils.putComponentOn(this, getReloadIndButton(), RELOAD_IND_WIDTH, RELOAD_HEIGHT, x, y1);

		JScrollPane detail = getJTextPane_detail();
		layout.putConstraint(SpringLayout.SOUTH, detail, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, detail, 0, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, detail, 0, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.NORTH, detail, -DETAIL_HEIGHT, SpringLayout.SOUTH, this);

		JScrollPane list = getJScrollPane_list();
		layout.putConstraint(SpringLayout.NORTH, list, 0, SpringLayout.SOUTH, jButton_reloadDefault);
		layout.putConstraint(SpringLayout.WEST, list, 0, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.EAST, list, 0, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, list, 0, SpringLayout.NORTH, detail);

		this.add(jsc_list);
		this.add(jsc_detail);

		updateGenreList();

		setDetailVisible(true);

		this.addComponentListener(cl_tabShown);

		addListeners();
	}

	/**
	 * リスナーを追加する
	 */
	protected void addListeners(){
		if (listenerAdded)
			return;

		listenerAdded = true;

		jButton_newFolder.addActionListener(al_newFolder);
		jButton_editFolder.addActionListener(al_editFolder);
		jButton_removeFolder.addActionListener(al_removeFolder);
		jButton_reloadDefault.addActionListener(al_reloadDefault);
		jCBXPanel_device.addItemListener(il_deviceChanged);
		jCBXPanel_folder.addItemListener(il_folderChanged);
		jCBXPanel_genre.addItemListener(il_genreChanged);
	}

	/**
	 * リスナーを削除する
	 */
	protected void removeListeners() {
		if (!listenerAdded)
			return;

		listenerAdded = false;

		jButton_newFolder.removeActionListener(al_newFolder);
		jButton_editFolder.removeActionListener(al_editFolder);
		jButton_removeFolder.removeActionListener(al_removeFolder);
		jButton_reloadDefault.removeActionListener(al_reloadDefault);
		jCBXPanel_device.removeItemListener(il_deviceChanged);
		jCBXPanel_folder.removeItemListener(il_folderChanged);
		jCBXPanel_genre.removeItemListener(il_genreChanged);
	}

	/**
	 * タイトル一覧を更新する
	 * @param force レコーダからタイトル一覧を取得する
	 * @param upfolder フォルダ一覧を更新する
	 */
	protected void updateTitleList(boolean force, boolean updevice, boolean upfolder,
			boolean setting, boolean titles, boolean details) {
		if (titleUpdating)
			return;

		// 選択されたレコーダ
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		// タイトルに対応していないレコーダは何もしない
		if (!rec.isTitleListSupported())
			return;

		titleUpdating = true;
		String device_id = getSelectedDeviceId();
		if (device_id == null)
			return;

		String device_name = rec.getDeviceName(device_id);
		if (device_name == null)
			return;

		String folder_name = getSelectedFolderName();

		if (force){
			// フォルダー作成実行
			StWin.clear();

			new SwingBackgroundWorker(false) {
				@Override
				protected Object doWorks() throws Exception {
					StWin.appendMessage(MSGID+"タイトル一覧を取得します："+device_name);
					removeListeners();

					boolean nodev = rec.getDeviceList().size() == 0;
					if (setting || nodev){
						StWin.appendMessage(MSGID+"レコーダから設定情報を取得します(force=" + String.valueOf(force) + ")");
						if (rec.GetRdSettings(force))
							MWin.appendMessage(MSGID+"レコーダから設定情報が正常に取得できました");
						else
							MWin.appendError(ERRID+"レコーダからの設定情報の取得に失敗しました");
					}

					if (updevice || nodev){
						updateDeviceList(device_name);
						updateDeviceInfoLabel();
					}

					String devId = getSelectedDeviceId();
					if (titles && devId != null){
						StWin.appendMessage(MSGID+"レコーダからタイトル一覧を取得します(force=" + String.valueOf(force) + ",details=" + String.valueOf(details) + ")："+devId);
						TatCount tc = new TatCount();
						if (rec.GetRdTitles(devId, force, details, devId.equals(HDDRecorder.DEVICE_ALL))){
							String time = String.format(" [%.2f秒]", tc.end());
							MWin.appendMessage(MSGID+"レコーダからタイトル一覧が正常に取得できました："+devId + time);
						}
						else{
							String time = String.format("[%.2f秒]", tc.end());
							MWin.appendError(ERRID+"レコーダからのタイトル一覧の取得に失敗しました："+devId + time);
						}

						if ( ! rec.getErrmsg().equals("")) {
							MWin.appendError(MSGID+"[追加情報] "+rec.getErrmsg());
							ringBeep();
						}
					}

					if (upfolder || nodev){
						updateDeviceInfoLabel();
						updateFolderList(folder_name);
						updateFolderButtons();
					}

					int vrow = jTable_title.getSelectedRow();
					_redrawTitleList();
					if (vrow >= 0 && vrow < jTable_title.getModel().getRowCount())
						jTable_title.setRowSelectionInterval(vrow, vrow);

					return null;
				}
				@Override
				protected void doFinally() {
					StWin.setVisible(false);
					addListeners();
					titleUpdating = false;
				}
			}.execute();

			CommonSwingUtils.setLocationCenter(parent, (Component)StWin);
			StWin.setVisible(true);
		}
		else{
			removeListeners();

			if (setting){
				if (rec.GetRdSettings(force)){
//					MWin.appendMessage(MSGID+"レコーダから設定情報が正常に取得できました");
				}
				else
					MWin.appendError(ERRID+"レコーダからの設定情報の取得に失敗しました");
			}

			if (updevice){
				updateDeviceList(device_name);
				updateDeviceInfoLabel();
			}

			String devId = getSelectedDeviceId();
			if (titles && devId != null){
				StWin.appendMessage(MSGID+"レコーダからタイトル一覧を取得します(force=" + String.valueOf(force) + ",details=" + String.valueOf(details) + ")："+devId);
				if (rec.GetRdTitles(devId, force, details, devId.equals(HDDRecorder.DEVICE_ALL))){
//					MWin.appendMessage(MSGID+"レコーダからタイトル一覧が正常に取得できました："+devId);
				}
				else
					MWin.appendError(ERRID+"レコーダからのタイトル一覧の取得に失敗しました："+devId);

				if ( ! rec.getErrmsg().equals("")) {
					MWin.appendError(MSGID+"[追加情報] "+rec.getErrmsg());
					ringBeep();
				}
			}

			if (upfolder){
				updateDeviceInfoLabel();
				updateFolderList(folder_name);
				updateFolderButtons();
			}

			int vrow = jTable_title.getSelectedRow();
			_redrawTitleList();
			if (vrow >= 0 && vrow < jTable_title.getModel().getRowCount())
				jTable_title.setRowSelectionInterval(vrow, vrow);

			addListeners();
			titleUpdating = false;
		}
	}

	/*
	 * デバイスコンボを更新する
	 * @param sel 更新後選択するデバイスの名称
	 */
	protected void updateDeviceList(String sel){
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		// タイトルに対応していないレコーダは何もしない
		if (!rec.isTitleListSupported())
			return;

		if (deviceUpdating)
			return;

		deviceUpdating = true;
		JComboBoxPanel combo = jCBXPanel_device;
		ArrayList<TextValueSet> tvs = rec.getDeviceList();

		combo.removeAllItems();
		int idx = 0;
		int no = 0;
		for ( TextValueSet t : tvs ) {
			if (sel != null && t.getText().equals(sel))
				idx = no;
			DeviceInfo di = rec.GetRDDeviceInfo(t.getValue());
			if (di != null)
				combo.addItem(di.getName());
			else
				combo.addItem(t.getText());

			no++;
		}

		if (no > 0)
			combo.setSelectedIndex(idx);
		combo.setEnabled( combo.getItemCount() > 0 );
		deviceUpdating = false;
	}

	/**
	 * デバイス情報ラベルを更新する
	 */
	protected void updateDeviceInfoLabel(){
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		String s = "残量(DR):";

		String device_id = getSelectedDeviceId();
		DeviceInfo info = device_id != null ? rec.GetRDDeviceInfo(device_id) : null;
		if (info != null){
			int allsize = info.getAllSize();
			int freesize = info.getFreeSize();
			int freePercent = allsize > 0 ? freesize*100/allsize : 0;
			int freemin = info.getFreeMin();

			s += String.format("%d時間%02d分（%d％）", freemin/60, freemin%60, freePercent);
		}

		jLabel_deviceInfo.setText(s);
	}

	/**
	 * フォルダーコンボを更新する
	 * @param sel 更新後選択するフォルダーの名称
	 */
	protected void updateFolderList(String sel){
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		// タイトルに対応していないレコーダは何もしない
		if (!rec.isTitleListSupported())
			return;

		String device_id = getSelectedDeviceId();
		String device_name = device_id != null ? rec.getDeviceName(device_id) : null;
		if (device_name != null)
			device_name = "[" + device_name + "]";

		JComboBoxPanel combo = jCBXPanel_folder;
		ArrayList<TextValueSet> tvs = rec.getFolderList();

		combo.removeAllItems();
		int idx = 0;
		int no = 0;
		for ( TextValueSet t : tvs ) {
			if (! t.getValue().equals(FOLDER_ID_ROOT) && ! t.getValue().equals("-1") && !device_id.equals(HDDRecorder.DEVICE_ALL) && !t.getText().startsWith(device_name))
				continue;

			if (sel != null && t.getText().equals(sel))
				idx = no;
			combo.addItem(t.getText() + getTotalsInFolder(t.getValue()));
			no++;
		}

		if (no > 0)
			combo.setSelectedIndex(idx);
		combo.setEnabled( combo.getItemCount() > 0 );
	}

	/**
	 * フォルダー関係のボタンを更新する
	 */
	protected void updateFolderButtons() {
		HDDRecorder rec = getSelectedRecorder();
		String device_id = getSelectedDeviceId();

		boolean b = rec != null ? rec.isFolderCreationSupported() : false;
		boolean ball = device_id != null && device_id.equals(HDDRecorder.DEVICE_ALL);

		int idx = jCBXPanel_folder.getSelectedIndex();
		jButton_newFolder.setEnabled(b && !ball);
		jButton_editFolder.setEnabled(b && idx != 0);
		jButton_removeFolder.setEnabled(b && idx != 0);
	}

	/*
	 * ジャンルコンボを更新する
	 */
	protected void updateGenreList() {
		JComboBoxPanel combo = jCBXPanel_genre;
		combo.removeAllItems();

		combo.addItem("指定なし");

		for (ProgGenre pg : ProgGenre.values()) {
			combo.addItem(pg.toIEPG() + ":" + pg.toString());
		}

		combo.setEnabled( combo.getItemCount() > 0 );
	}

	/**
	 * デバイスコンボで選択されているデバイスのIDを取得する
	 */
	protected String getSelectedDeviceId() {
		HDDRecorder recorder = getSelectedRecorder();
		if (recorder == null)
			return "";

		String device_name = (String)jCBXPanel_device.getSelectedItem();
		DeviceInfo info = recorder.GetRDDeviceInfoFromName(device_name);
		if (info != null)
			return info.getId();

		return text2value(recorder.getDeviceList(), device_name);
	}

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

	/*
	 * 指定されたフォルダに含まれるタイトルの数と録画時間を取得する
	 */
	protected String getTotalsInFolder(String folder_id) {
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null || folder_id == null)
			return "";

		int tnum = 0;
		int tmin = 0;

		for (TitleInfo t : rec.getTitles()){
			if (!folder_id.equals(FOLDER_ID_ROOT) && !t.containsFolder(folder_id))
				continue;

			tnum++;
			tmin += Integer.parseInt(t.getRec_min());
		}

		return String.format("  (%dタイトル %d時間%02d分)" , tnum, tmin/60, tmin%60);
	}

	/*
	 * フォルダコンボの名称からフォルダ名のみを取り出す
	 */
	protected String getSelectedFolderName(){
		String label = (String)jCBXPanel_folder.getSelectedItem();
		if (label == null)
			return null;

		Matcher ma = Pattern.compile("^(.*)  \\(\\d+タイトル \\d+時間\\d\\d分\\)").matcher(label);
		if (ma.find()){
			return ma.group(1);
		}

		return label;
	}

	/*
	 * 同一フォルダの次の録画タイトルを取得する
	 */
	protected String getNextTitleInSameFolder(int vrow){
		final int row = jTable_title.convertRowIndexToModel(vrow);
		TitleItem ra = rowView.get(row);
		if (ra == null || ra.devname == null || ra.folder == null)
			return null;

		for (vrow++ ; vrow < jTable_title.getRowCount(); vrow++){
			TitleItem rb = rowView.get(jTable_title.convertRowIndexToModel(vrow));
			if (rb.devname != null && rb.devname.equals(ra.devname) &&
				rb.folder != null && rb.folder.equals(ra.folder))
				return rb.title;
		}

		return null;
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 対外的な

	/**
	 * タイトル一覧を描画してほしいかなって
	 * ★synchronized(rowData)★
	 * @see #cl_tabShown
	 */
	public void redrawTitleList() {
		// ★★★　イベントにトリガーされた処理がかちあわないように synchronized()　★★★
		synchronized ( rowView ) {
			updateTitleList( false, true, true, true, true, false );
		}
	}

	/**
	 * タイトル一覧を再描画する
	 */
	private void _redrawTitleList() {
		// 選択されたレコーダ
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		String folder_name = getSelectedFolderName();
		String folder_id = folder_name != null ? text2value(rec.getFolderList(), folder_name) : "";

		// ジャンルが選択されている場合、そのジャンルに属するタイトル以外はスキップする
		String genre_name = (String)jCBXPanel_genre.getSelectedItem();
		ProgGenre genre = ProgGenre.get(genre_name.substring(2));

		//
		rowData.clear();

		// 並べ替えるために新しいリストを作成する
		for ( TitleInfo ro : rec.getTitles() ) {
			// フォルダーが選択されている場合、そのフォルダに属するタイトル以外はスキップする
			if (!folder_id.equals(FOLDER_ID_ROOT) && !ro.containsFolder(folder_id))
				continue;

			// ジャンルが選択されている場合、そのフォルダに属するタイトル以外はスキップする
			if (genre != null && !ro.containsGenre(genre.toIEPG()))
				continue;

			TitleItem sa = new TitleItem();
			setTitleItem(sa, ro, rec);

			addRow(sa);
		}

		// 表示用
		rowView.clear();
		for ( TitleItem a : rowData ) {
			rowView.add(a);
		}

		tableModel_title.fireTableDataChanged();
		((DefaultTableModel)jTable_rowheader.getModel()).fireTableDataChanged();

		jta_detail.setText(null);

		//jta_detail.setText("レコーダから予約結果の一覧を取得して表示します。現在の対応レコーダはTvRock/EpgDataCap_Bonのみです。");
	}

	/**
	 * リスト項目の属性をセットする
	 * @param sa セット対象のリスト項目
	 * @param ro セット元のタイトル情報
	 * @param rec セット元のレコーダ
	 */
	private void setTitleItem(TitleItem sa, TitleInfo ro, HDDRecorder rec) {
		sa.start = ro.getRec_date()+" "+ro.getAhh()+":"+ro.getAmm();	// YYYY/MM/DD(WD) hh:mm
		sa.end = ro.getZhh()+":"+ro.getZmm();
		sa.length = ro.getRec_min();
		sa.recmode = ro.getRec_mode();
		sa.title = ro.getTitle();
		if (ro.getRecording())
			sa.title += " (録画中)";
		sa.chname = ro.getCh_name();
		if (sa.chname == null ||sa.chname.isEmpty())
			sa.chname = ro.getChannel();
		sa.devname = ro.getRec_device();
		sa.folder = ro.getFolderNameList();
		sa.genre = ro.getGenreNameList();
		sa.recname = rec.getDispName();
		sa.recorder = rec.Myself();
		sa.copycount = ro.formatCopyCount();
		sa.dlna_oid = ro.getHidden_params().get("dlnaObjectID");

		sa.hide_ttlid = ro.getId();
		sa.hide_detail = ro.formatDetail();
		sa.hide_recording = ro.getRecording();

		sa.fireChanged();
	}


	/**
	 * 絞り込み検索の本体（現在リストアップされているものから絞り込みを行う）（親から呼ばれるよ！）
	 */
	public void redrawListByKeywordFilter(SearchKey keyword, String target, String range) {

		rowView.clear();

		// 情報を一行ずつチェックする
		if ( keyword != null ) {
			String from = null;
			String to = null;

			if (range != null && !range.isEmpty()){
				// <from>-<to> を分解して、放送日の下限、上限を取得する
				Matcher ma = Pattern.compile("^(.*)-(.*)$").matcher(range);
				if (ma.find()){
					from = ma.group(1);
					to = ma.group(2);
				}
			}

			for ( TitleItem a : rowData ) {
				// 放送日下限が指定されている場合
				if (from != null && !from.isEmpty()){
					if (a.start == null || a.start.length() < 10 || a.start.substring(0, 10).compareTo(from) < 0)
						continue;
				}
				// 放送日上限が指定されている場合
				if (to != null && !to.isEmpty()){
					if (a.start == null || a.start.length() < 10 || a.start.substring(0, 10).compareTo(to) > 0)
						continue;
				}

				ProgDetailList tvd = new ProgDetailList();

				// タイトルを整形しなおす
				tvd.title = a.title;
				tvd.titlePop = TraceProgram.replacePop(tvd.title);
				// ジャンル
				if (a.genre != null && a.genre.length() > 2)
					tvd.genre = ProgGenre.get(a.genre.substring(2));
				// 番組長
				try{
					tvd.length = Integer.parseInt(a.length);
				}
				catch(Exception e){	}

				boolean isFind = SearchProgram.isMatchKeyword(keyword, a.chname, tvd);

				if ( isFind ) {
					rowView.add(a);
				}
			}
		}
		else {
			for ( TitleItem a : rowData ) {
				rowView.add(a);
			}
		}

		// fire!
		tableModel_title.fireTableDataChanged();
		rowheaderModel_title.fireTableDataChanged();
	}

	/**
	 * カラム幅を保存する（鯛ナビ終了時に呼び出されるメソッド）
	 */
	public void copyColumnWidth() {
		for ( TitleColumn rc : TitleColumn.values() ) {
			if ( rc.getIniWidth() < 0 ) {
				continue;
			}
			TableColumn col = getColumn(rc);
			if (col == null)
				continue;

			bounds.getTitleColumnSize().put(rc.toString(), col.getPreferredWidth());
		}
	}

	/**
	 * テーブルの行番号の表示のＯＮ／ＯＦＦ
	 */
	public void setRowHeaderVisible(boolean b) {
		jsc_list.getRowHeader().setVisible(b);
	}

	/*
	 * 指定した行番号のタイトルを編集する
	 */
	public void editTitleOfRow(int vrow){
		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		final int row = jTable_title.convertRowIndexToModel(vrow);
		TitleItem ra = rowView.get(row);
		if (ra == null)
			return;

		String devId = rec.getDeviceID(ra.devname);
		String otitle = getNextTitleInSameFolder(vrow);
		JMenuItem menuItem = getEditTitleMenuItem(ra.title, ra.chname, devId, ra.hide_ttlid, ra.recorder, otitle);
		menuItem.doClick();
	}

	/**
	 * 画面下部のタイトル詳細領域の表示のＯＮ／ＯＦＦ
	 */
	public void setDetailVisible(boolean b) {
		if (!env.getShowTitleDetail())
			b = false;

		jsc_detail.setVisible(b);

		SpringLayout layout = (SpringLayout)this.getLayout();
		layout.putConstraint(SpringLayout.NORTH, jsc_detail, b ? -DETAIL_HEIGHT : 0, SpringLayout.SOUTH, this);
	}

	// 内部的な
	/**
	 * テーブル（の中の人）に追加
	 */
	private void addRow(TitleItem data) {
		// 有効データ
		int n=0;
		for ( ; n<rowData.size(); n++ ) {
			TitleItem c = rowData.get(n);
			if ( c.start.compareTo(data.start) < 0 ) {
				break;
			}
		}
		rowData.add(n,data);
	}

	/*
	 * タイトルに対する番組情報を取得する
	 */
	protected ProgDetailList getProgDetailForTitle(TitleInfo t){
		TVProgramList tpl = getTVProgramList();

		if (tpl == null || t == null)
			return null;

		// 番組表の日付に変換する
		String date = CommonUtils.getDate529(t.getStartDateTime(), true);
		if (date == null)
			return null;

		// 未来分の番組情報から該当番組の情報を取得する
		TVProgramIterator pli = tpl.getIterator().build(null, TVProgramIterator.IterationType.ALL);
		ProgDetailList pdl = getProgDetailForTitle(pli, t, date);

		// 見つからなかったら過去分の番組情報をロードして該当番組の情報を取得する
		if (pdl == null){
			PassedProgram passed = tpl.getPassed();

			if (passed.loadByCenter(date, t.getCh_name())){
				pli = tpl.getIterator().build(null, TVProgramIterator.IterationType.PASSED);
				pdl = getProgDetailForTitle(pli, t, date);
			}
		}

		return pdl;
	}

	/*
	 * 指定したタイトルと放送局が同じで時間が重なる番組情報を取得する
	 */
	protected ProgDetailList getProgDetailForTitle(TVProgramIterator pli, TitleInfo t, String date){
		String start = t.getStartDateTime();
		String end = t.getEndDateTime();
		String ch_name = t.getCh_name();

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
					if (bse * bes < 0){
						attachSyobocalToProgDetailList(t, tvd);
						return tvd;
					}
				}

				break;
			}

			break;
		}

		return null;
	}

	/**
	 * 番組情報にしょぼかるの番組詳細リンクをひもづける
	 *
	 * @param t		タイトル情報
	 * @param tvd	番組情報
	 */
	protected void attachSyobocalToProgDetailList(TitleInfo t, ProgDetailList tvd){
		// すでに番組詳細リンクがある場合は何もしない
		if ( tvd.linkSyobo != null && !tvd.linkSyobo.isEmpty())
			return;

		ProgDetailList tdl = getSyobocalProgDetailForTitle(t);
		if (tdl != null){
			// 番組詳細リンクをひもづける
			tvd.linkSyobo = tdl.link;
		}
	}

	/*
	 * 指定したタイトルと放送局が同じで時間が重なるしょぼかるの番組情報を取得する
	 *
	 * @param t		タイトル情報
	 */
	protected ProgDetailList getSyobocalProgDetailForTitle(TitleInfo t){
		// ジャンルで絞り込む
		if ( ! t.containsGenre(ProgGenre.ANIME.toIEPG()) &&
			 ! t.containsGenre(ProgGenre.MOVIE.toIEPG()) &&
			 ! t.containsGenre(ProgGenre.MUSIC.toIEPG()) )
			return null;

		TVProgramList tpl = getTVProgramList();
		TVProgram syobo = tpl.getSyobo();
		if (syobo == null)
			return null;

		// しょぼかるのチャンネル別番組表を取得する
		ProgList mSvpl = null;
		for ( ProgList svpl : syobo.getCenters() ) {
			if ( t.getCh_name().equals(svpl.Center)) {
				mSvpl = svpl;
				break;
			}
		}
		if (mSvpl == null)
			return null;

		// しょぼかるの日付別番組表を取得する
		String date = CommonUtils.getDate529(t.getStartDateTime(), true);
		ProgDateList mSvc = null;
		for ( ProgDateList svc : mSvpl.pdate ) {
			if (date.equals(svc.Date) ) {
				mSvc = svc;
				break;
			}
		}
		if (mSvc == null)
			return null;

		// しょぼかるの番組情報を取得する
		String time = t.getAhh() + ":" + t.getAmm();
		ProgDetailList mSvd = null;
		for ( ProgDetailList svd : mSvc.pdetail ) {
			if ( time.equals(svd.start) ){
				mSvd = svd;
				break;
			}
		}
		if (mSvd == null)
			return null;

		return mSvd;

	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * タブが開かれたら表を書き換える
	 * ★synchronized(rowData)★
	 * @see #redrawTitleList()
	 */
	private final ComponentAdapter cl_tabShown = new ComponentAdapter() {
		@Override
		public void componentShown(ComponentEvent e) {
			// ★★★　イベントにトリガーされた処理がかちあわないように synchronized()　★★★
			synchronized ( rowView ) {
				updateTitleList( false, true, true, true, true, false );
			}
		}
	};

	/**
	 * 「新規」ボタンの処理
	 * フォルダーを作成する
	 */
	private final ActionListener al_newFolder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			editFolderName(null, "");
		}
	};

	/**
	 * 「変更」ボタンの処理
	 * フォルダーの名称編集を行う
	 */
	private final ActionListener al_editFolder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
//			int idx = jCBXPanel_folder.getSelectedIndex();

			HDDRecorder rec = getSelectedRecorder();
			String folder_name = getSelectedFolderName();
			String folder_id = folder_name != null ? text2value(rec.getFolderList(), folder_name) : "";

			editFolderName(folder_id, folder_name);
		}
	};


	/*
	 * フォルダーの作成ないし名称編集を行う
	 */
	private void editFolderName( String folder_id, String nameOld){
		String nameSel = getSelectedFolderName();
		final boolean isSelected = nameSel != null && nameOld.equals(nameSel);

		VWFolderDialog dlg = new VWFolderDialog();
		CommonSwingUtils.setLocationCenter(parent, dlg);

		HDDRecorder rec = getSelectedRecorder();

		Matcher ma = Pattern.compile("^\\[([^\\]]*)\\] (.*)$").matcher(nameOld);
		final String device_name = ma.find() ? ma.group(1) : "";
		final String device_id = rec.getDeviceID(device_name);
		nameOld = device_name.length() > 0 ? ma.group(2) : nameOld;

		String prefix = "[" + device_name + "] ";

		dlg.open(nameOld);
		dlg.setVisible(true);

		if (!dlg.isRegistered())
			return;

		String nameNew = dlg.getFolderName();
		String action = folder_id != null ? "更新" : "作成";
		final String folderNameWorking = folder_id != null ? "[" + nameOld + "] -> [" + nameNew + "]" : nameNew;

		// フォルダー作成実行
		StWin.clear();
		new SwingBackgroundWorker(false) {
			@Override
			protected Object doWorks() throws Exception {
				StWin.appendMessage(MSGID+"フォルダーを" + action + "します："+folderNameWorking);

				boolean reg = false;
				if (folder_id != null)
					reg = rec.UpdateRdFolderName(device_id, folder_id, nameNew);
				else
					reg = rec.CreateRdFolder(device_id, nameNew);
				if (reg){
					MWin.appendMessage(MSGID+"フォルダーを正常に" + action + "できました："+folderNameWorking);
					// [<device_name>]を先頭に付ける
					removeListeners();
					updateFolderList(isSelected ? prefix + nameNew : null);
					updateFolderButtons();
					updateTitleList(false, false, false, true, false, false);
					addListeners();
				}
				else {
					MWin.appendError(ERRID+"フォルダーの" + action + "に失敗しました："+folderNameWorking);

					if ( ! rec.getErrmsg().equals("")) {
						MWin.appendError(MSGID+"[追加情報] "+rec.getErrmsg());
						ringBeep();
					}
				}

				return null;
			}
			@Override
			protected void doFinally() {
				StWin.setVisible(false);
			}
		}.execute();

		CommonSwingUtils.setLocationCenter(parent, (Component)StWin);
		StWin.setVisible(true);
	}

	/**
	 * 「削除」ボタンの処理
	 * フォルダーを削除する
	 */
	private final ActionListener al_removeFolder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			HDDRecorder rec = getSelectedRecorder();

			String folder_name = getSelectedFolderName();
			if (folder_name == null)
				return;

			Matcher ma = Pattern.compile("^\\[(.*)\\] (.*)$").matcher(folder_name);
			final String device_name = ma.find() ? ma.group(1) : "";
			final String device_id = rec.getDeviceID(device_name);
			final String folderNameWorking = device_name.length() > 0 ? ma.group(2) : folder_name;
			final String folder_id = text2value(rec.getFolderList(), folder_name);

			// フォルダー削除実行
			StWin.clear();
			new SwingBackgroundWorker(false) {
				@Override
				protected Object doWorks() throws Exception {
					StWin.appendMessage(MSGID+"フォルダーを削除します："+folderNameWorking);

					if (rec.RemoveRdFolder( device_id, folder_id )){
						MWin.appendMessage(MSGID+"フォルダーを正常に削除できました："+folderNameWorking);
						removeListeners();
						updateFolderList(null);
						updateFolderButtons();
						updateTitleList(false, false, false, true, false, false);
						addListeners();
					}
					else {
						MWin.appendError(ERRID+"フォルダーの削除に失敗しました："+folderNameWorking);
					}
					if ( ! rec.getErrmsg().equals("")) {
						MWin.appendError(MSGID+"[追加情報] "+rec.getErrmsg());
						ringBeep();
					}

					return null;
				}
				@Override
				protected void doFinally() {
					StWin.setVisible(false);
				}
			}.execute();

			CommonSwingUtils.setLocationCenter(parent, (Component)StWin);
			StWin.setVisible(true);
		}
	};

	/*
	 * 「再取得」ボタン右の矢印ボタンの処理
	 * メニューをプルダウン表示する
	 */
	private final MouseAdapter ma_reloadIndividual = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			jPopupMenu_reload.show(jButton_reloadDefault, 0, jButton_reloadInd.getHeight());
		}
	};

	/**
	 * 「再取得」ボタンの処理
	 * forceフラグを指定して録画タイトルのみ取得し、タイトル一覧を更新する
	 */
	private final ActionListener al_reloadDefault = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateTitleList(true, false, true, false, true, true);
		}
	};

	/**
	 * 「設定情報＋録画タイトルを取得」ボタンの処理
	 * forceフラグを指定して設定情報と録画タイトルの両方を取得し、デバイスコンボ、フォルダコンボ、
	 * タイトル一覧を更新する
	 */
	private final ActionListener al_reloadAll = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateTitleList(true, true, true, true, true, true);
		}
	};

	/*
	 * 「設定情報のみ取得」メニューの処理
	 * forceフラグを指定して設定情報のみ取得し、デバイスコンボ、フォルダコンボ、タイトル一覧を更新する
	 */
	private final ActionListener al_reloadSettingsOnly = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateTitleList(true, true, true, true, false, false);
		}
	};

	/*
	 * 「録画タイトルのみ取得」メニューの処理
	 * forceフラグを指定して録画タイトルのみ取得し、タイトル一覧のみを更新する
	 */
	private final ActionListener al_reloadTitlesOnly = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateTitleList(true, false, true, false, true, false);
		}
	};

	/*
	 * 「録画タイトル＋詳細情報のみ取得」メニューの処理
	 * forceフラグを指定して録画タイトルとその詳細情報のみ取得し、タイトル一覧のみを更新する
	 */
	private final ActionListener al_reloadTitleAndDetailsOnly = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateTitleList(true, false, true, false, true, true);
		}
	};

	/**
	 * デバイスコンボの選択変更時の処理
	 * デバイス情報更新後、タイトル一覧を更新する
	 */
	private final ItemListener il_deviceChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				updateDeviceInfoLabel();
				updateTitleList(false, false, true, false, true, false);
			}
		}
	};

	/**
	 * フォルダーコンボの選択変更時の処理
	 * タイトル一覧を再描画した後、フォルダー関係のボタンを更新する
	 */
	private final ItemListener il_folderChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				_redrawTitleList();
				updateFolderButtons();
			}
		}
	};

	/**
	 * ジャンルコンボの選択変更時の処理
	 * タイトル一覧を再描画する
	 */
	private final ItemListener il_genreChanged = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				_redrawTitleList();
			}
		}
	};

	/**
	 * タイトル一覧の選択変更時の処理
	 * タイトルの詳細情報を取得後、詳細情報を画面下部に表示する
	 */
	private final ListSelectionListener lsSelectListener = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if(e.getValueIsAdjusting())
				return;
			int srow = jTable_title.getSelectedRow();
			if (srow < 0)
				return;

			HDDRecorder rec = getSelectedRecorder();
			if (rec == null)
				return;

			int row = jTable_title.convertRowIndexToModel(srow);
			TitleItem c = rowView.get(row);
			TitleInfo t = rec.getTitleInfo(c.hide_ttlid);
			if (!t.getDetailLoaded() && rec.GetRdTitleDetail(t)){
				t.setDetailLoaded(true);
				redrawSelectedTitle(true);
			}
			else
				redrawSelectedTitle(false);
		}
	};

	/*
	 * 選択されているタイトルを再描画する
	 * @param b TRUEの場合リストにタイトル情報をセットし直す
	 */
	public final void redrawSelectedTitle(boolean b) {
		int srow = jTable_title.getSelectedRow();
		if (srow < 0)
			return;

		HDDRecorder rec = getSelectedRecorder();
		if (rec == null)
			return;

		int row = jTable_title.convertRowIndexToModel(srow);
		if (row < 0)
			return;

		TitleItem c = rowView.get(row);
		if (b){
			TitleInfo t = rec.getTitleInfo(c.hide_ttlid);
			setTitleItem(c, t, rec);
			_redrawTitleList();
			jTable_title.setRowSelectionInterval(srow,  srow);
		}

		jta_detail.setText(c.hide_detail);
		jta_detail.setCaretPosition(0);
	}

	/*
	 * 選択されたタイトルが複数のデバイスにまたがるか
	 */
	private boolean areMultiDeviceTitles(ArrayList<TitleItem> array){
		String devname = null;
		for (TitleItem ti : array){
			if (devname == null)
				devname = ti.devname;
			else if (!devname.equals(ti.devname))
				return true;
		}

		return false;
	}
	/**
	 * タイトル一覧でのマウスイベント処理
	 * 右クリック時にポップアップメニューを表示する
	 */
	private final MouseAdapter ma_showpopup = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			// 選択されたレコーダ
			HDDRecorder rec = getSelectedRecorder();
			if (rec == null)
				return;

			//
			Point p = e.getPoint();
			final int vrow = jTable_title.rowAtPoint(p);

			if (vrow >= jTable_title.getRowCount())
				return;
			final int row = jTable_title.convertRowIndexToModel(vrow);

			if (row >= rowView.size())
				return;
			TitleItem ra = rowView.get(row);
			final String title = ra.title;
			final String chnam = ra.chname;
			final String startDT = ra.start;
			final String recId = ra.recorder;
			final String recName = recorders.getRecorderName(recId);
			final String ttlId = ra.hide_ttlid;
			final String devId = rec.getDeviceID(ra.devname);
			int num =jTable_title.getSelectedRowCount();
			final ArrayList<TitleItem> ras = new ArrayList<TitleItem>(num);
			TitleInfo ttl = rec.getTitleInfo(ttlId);

			final String otitle = getNextTitleInSameFolder(vrow);

			//
			if (e.getButton() == MouseEvent.BUTTON3) {
				if (e.getClickCount() == 1) {
					if (!jTable_title.isRowSelected(vrow))
						jTable_title.getSelectionModel().setSelectionInterval(vrow,vrow);

					String ttlIds[] = null;
					if (num > 1){
						ttlIds = new String[num];

						int rows[] = jTable_title.getSelectedRows();
						for (int n=0; n<num; n++){
							TitleItem ti = rowView.get(jTable_title.convertRowIndexToModel(rows[n]));
							ras.add(ti);
							ttlIds[n] = ti.hide_ttlid;
						}
					}
					final boolean muldev = areMultiDeviceTitles(ras);

					// 右クリックでポップアップメニューを表示
					JPopupMenu pop = new JPopupMenu();
					pop.add(getTitleDetailMenuItem(title, chnam, devId, ttlId, recId));
					if (ttlIds != null)
						pop.add(getMultiTitleDetailMenuItem(title, chnam, devId, ttlIds, recId));

					appendSelectDeviceMenuItem(pop, ttlId);
					appendSelectFolderMenuItem(pop, ttlId);

					pop.addSeparator();
					pop.add(getStartStopPlayTitleMenuItem(true, title, chnam, devId, ttlId, recId));
					pop.add(getStartStopPlayTitleMenuItem(false, title, chnam, devId, ttlId, recId));
					pop.addSeparator();
					pop.add(getEditTitleMenuItem(title, chnam, devId, ttlId, recId, otitle));
					if (ttlIds != null){
						JMenuItem mi = getMoveMultiTitleMenuItem(title, chnam, devId, ttlIds, recId);
						if (muldev)
							mi.setEnabled(false);
						pop.addSeparator();
						pop.add(mi);
					}
					pop.addSeparator();
					pop.add(getRemoveTitleMenuItem(title, chnam, devId, ttlId, recId));
					if (ttlIds != null)
						pop.add(getRemoveMultiTitleMenuItem(title, chnam, devId, ttlIds, recId));

					pop.addSeparator();
					pop.add(getJumpMenuItem(title, chnam, startDT));
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
						JMenuItem menuItem = new JMenuItem(String.format("タイトル情報をコピー【%s (%s)/%s】", title, chnam, recName));
						menuItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								String msg = formatTitleItem(ra, false);
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
								String msg = formatTitleItems(ras, false);
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
								String msg = formatTitleHeader(true) + formatTitleItem(ra, true);
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
								String msg = formatTitleHeader(true) + formatTitleItems(ras, true);
								Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
								StringSelection s = new StringSelection(msg);
								cb.setContents(s, null);
							}
						});

						pop.add(menuItem);
					}

					ProgDetailList pdl = getProgDetailForTitle(ttl);
					if (pdl != null){
						pop.addSeparator();
						addBrowseMenuToPopup(pop, pdl);
					}

					pop.show(jTable_title, e.getX(), e.getY());
				}
			}
			else if (e.getButton() == MouseEvent.BUTTON1) {
				if (e.getClickCount() == 1) {
				}
				else if (e.getClickCount() == 2) {
					editTitleOfRow(vrow);
				}
			}
		}
	};

	/*
	 * ヘッダー情報をフォーマットする
	 */
	private String formatTitleHeader(boolean csv){
		StringBuilder sb = new StringBuilder();

		for (TitleColumn col: TitleColumn.values()){
			String value = col.toString();
			boolean last = col == TitleColumn.RECORDER;
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
	 * 複数のタイトル情報をテキストないしCSVでフォーマットする
	 */
	private String formatTitleItems(ArrayList<TitleItem>ras, boolean csv){
		StringBuilder sb = new StringBuilder();

		for (TitleItem ra : ras){
			sb.append(formatTitleItem(ra, csv));
		}

		return sb.toString();
	}

	/*
	 * タイトル情報をテキストないしCSVでフォーマットする
	 */
	private String formatTitleItem(TitleItem ra, boolean csv){
		StringBuilder sb = new StringBuilder();

		for (TitleColumn col: TitleColumn.values()){
			String value = "";
			boolean last = col == TitleColumn.RECORDER;
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
			case RECMODE:
				value = ra.recmode;
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
			case GENRE:
				value = ra.genre;
				break;
			case RECORDER:
				value = ra.recname;
				break;
			case COPYCOUNT:
				value = ra.copycount;
				break;
			case DLNAOID:
				value = ra.dlna_oid;
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

	/**
	 * タイトル一覧ペイン
	 */
	private JScrollPane getJScrollPane_list() {

		if ( jsc_list == null ) {
			jsc_list = new JScrollPane();

			jsc_list.setRowHeaderView(jTable_rowheader = new JTableRowHeader(rowView));
			jsc_list.setViewportView(getNETable_title());

			Dimension d = new Dimension(jTable_rowheader.getPreferredSize().width,0);
			jsc_list.getRowHeader().setPreferredSize(d);

			this.setRowHeaderVisible(env.getRowHeaderVisible());
		}

		return jsc_list;
	}

	/**
	 * 詳細情報ペイン
	 */
	private JScrollPane getJTextPane_detail() {
		if ( jsc_detail == null ) {
			jsc_detail = new JScrollPane(jta_detail = new JTextAreaWithPopup(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jta_detail.setRows(6);
			jta_detail.setEditable(false);
			jta_detail.setBackground(Color.LIGHT_GRAY);
		}
		return jsc_detail;
	}

	private TableColumn getColumn(TitleColumn lcol){
		TableColumn col = null;
		try{
			col = jTable_title.getColumn(lcol.toString());
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
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(tableModel_title);
		jTable_title.setRowSorter(sorter);

		// 数値でソートする項目用の計算式（番組長とか）
		final Comparator<String> lengthcomp = new Comparator<String>() {

			@Override
			public int compare(String len1, String len2) {
				return Integer.parseInt(len1.substring(0, len1.length()-1)) -
						Integer.parseInt(len2.substring(0, len2.length()-1));
			}
		};

		TableColumn col = getColumn(TitleColumn.LENGTH);
		if (col != null)
			sorter.setComparator(col.getModelIndex(),lengthcomp);

		// コピー回数でソートする項目用の計算式（番組長とか）
		final Comparator<String> copycomp = new Comparator<String>() {

			@Override
			public int compare(String str1, String str2) {
				int num1 = parseCopyCount(str1);
				int num2 = parseCopyCount(str2);
				return num1 - num2;
			}

			// 整形されたコピー回数から回数を取得する
			int parseCopyCount(String str){
				try{
					// 「移動のみ」の場合
					if (str.equals(TitleInfo.MOVEONLY))
						return 0;
					// 「録画中」の場合
					else if (str.equals(TitleInfo.RECORDING))
						return -1;
					// 「n回」の場合
					else
						return Integer.parseInt(str.substring(0, str.length()-1));
				}
				catch(NumberFormatException e){
					return -2;
				}
			}
		};

		col = getColumn(TitleColumn.COPYCOUNT);
		if (col != null)
			sorter.setComparator(col.getModelIndex(), copycomp);
	}

	/*
	 * テーブルの列幅を初期化する
	 */
	private void initTableColumnWidth(){
		for ( TitleColumn rc : TitleColumn.values() ) {
			if ( rc.getIniWidth() < 0 ) {
				continue;
			}

			TableColumn col = getColumn(rc);
			if (col == null)
				continue;

			Integer width = bounds.getTitleColumnSize().get(rc.toString());
			if (width != null)
				col.setPreferredWidth(width);
		}
	}

	/*
	 * 列表示のカスタマイズ結果を反映する
	 */
	public void reflectColumnEnv(){
		tlitems = (TitleListColumnInfoList) getTlItemEnv().clone();

		tableModel_title.setColumnIdentifiers(tlitems.getColNames());

		// ソーターをつける
		initTableSorter();

		// 表示幅を初期化する
		initTableColumnWidth();
	}

	/**
	 * タイトル一覧テーブル
	 */
	private JNETable getNETable_title() {
		if (jTable_title == null) {
			tableModel_title = new TitleTableModel(tlitems.getColNames(), 0);
			jTable_title = new JNETableTitle(tableModel_title, true);
			jTable_title.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);

			// ヘッダのモデル
			rowheaderModel_title = (DefaultTableModel) jTable_rowheader.getModel();

			// ソータを付ける
			initTableSorter();

			// 各カラムの幅
			initTableColumnWidth();

			// 詳細表示
			jTable_title.getSelectionModel().addListSelectionListener(lsSelectListener);

			// 一覧表クリックで削除メニュー出現
			jTable_title.addMouseListener(ma_showpopup);

			// ENTERキーでタイトルを編集する
			jTable_title.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
		    jTable_title.getActionMap().put("Enter", new AbstractAction() {
		        @Override
		        public void actionPerformed(ActionEvent ae) {
		        	editTitleOfRow(jTable_title.getSelectedRow());
		        }
		    });
		}

		return jTable_title;
	}

	/*
	 * 「個別再取得」ボタンを取得する
	 */
	private JButton getReloadIndButton() {
		if (jButton_reloadInd == null){
			ImageIcon arrow = new ImageIcon(ICONFILE_PULLDOWNMENU);

			jButton_reloadInd = new JButton(arrow);
			jButton_reloadInd.addMouseListener(ma_reloadIndividual);

			jPopupMenu_reload = new JPopupMenu();

			JMenuItem item = new JMenuItem("設定情報のみ取得");
			jPopupMenu_reload.add(item);
			item.addActionListener(al_reloadSettingsOnly);

			item = new JMenuItem("録画タイトルのみ取得");
			jPopupMenu_reload.add(item);
			item.addActionListener(al_reloadTitlesOnly);

			item = new JMenuItem("録画タイトル＋詳細情報のみ取得");
			jPopupMenu_reload.add(item);
			item.addActionListener(al_reloadTitleAndDetailsOnly);

			item = new JMenuItem("設定情報＋録画タイトル＋詳細情報を取得");
			jPopupMenu_reload.add(item);
			item.addActionListener(al_reloadAll);
		}

		return jButton_reloadInd;
	}

	/*
	 * フォルダ用コンボボックスを取得する
	 */
	private JComboBoxPanel getFolderComboPanel() {
		if (jCBXPanel_folder == null){
			jCBXPanel_folder = new JComboBoxPanel("フォルダ：", FLABEL_WIDTH, FCOMBO_WIDTH, true);
			jCBXPanel_folder.addPopupWidth(FCOMBO_OPEN_WIDTH-FCOMBO_WIDTH);

			JComboBox combo = jCBXPanel_folder.getJComboBox();
			combo.setMaximumRowCount(32);
		}

		return jCBXPanel_folder;
	}

	/*******************************************************************************
	 * 表表示
	 ******************************************************************************/

	private class JNETableTitle extends JNETable {

		private static final long serialVersionUID = 1L;

		// 実行中のタイトルの背景色
		private Color currentColorEven = CommonUtils.str2color(CURRENT_COLOR_EVEN);
		private Color currentColorOdd = CommonUtils.str2color(CURRENT_COLOR_ODD);

		// 実行中のタイトルの背景色をセットする
		public void setCurrentColor(Color c) {
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

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component c = super.prepareRenderer(tcr, row, column);
			Color fgColor = this.getForeground();
			Color bgColor = (isSepRowColor && row%2 == 1)?(evenColor):(super.getBackground());

			int xrow = this.convertRowIndexToModel(row);
			TitleItem item = null;
			try{
				item = rowView.get(xrow);
			}
			catch(IndexOutOfBoundsException e){
				return c;
			}

			// 実行中のタイトルの場合
			if ( item.hide_recording ) {
				bgColor = (isSepRowColor && row%2 == 1)?(currentColorEven):(currentColorOdd);
			}

			if(isRowSelected(row)) {
				fgColor = this.getSelectionForeground();
				bgColor = CommonUtils.getSelBgColor(bgColor);
			}

			c.setForeground(fgColor);
			c.setBackground(bgColor);
			return c;
		}

		//
		@Override
		public void tableChanged(TableModelEvent e) {
			reset();
			super.tableChanged(e);
		}

		private void reset() {
		}

		/*
		 * コンストラクタ
		 */
		public JNETableTitle(boolean b) {
			super(b);
		}
		public JNETableTitle(TableModel d, boolean b) {
			super(d,b);
		}
	}

	// 素直にHashMapつかっておけばよかった
	public String text2value(ArrayList<TextValueSet> tvs, String text) {
		for ( TextValueSet t : tvs ) {
			if (t.getText().equals(text)) {
				return(t.getValue());
			}
		}
		return("");
	}

	/**
	 *  デバイス絞り込みのメニューアイテムを追加する
	 */
	protected void appendSelectDeviceMenuItem(final JPopupMenu pop, final String ttlId){
		HDDRecorder rec = getSelectedRecorder();
		TitleInfo ttl = rec.getTitleInfo(ttlId);

		if (rec == null || ttl == null)
			return;

		String devNameOld = (String)jCBXPanel_device.getSelectedItem();
		String devIdOld = rec.getDeviceID(devNameOld);
		if (devIdOld == null)
			return;

		String devNameNew = ttl.getRec_device();
		String folderName = getSelectedFolderName();

		pop.addSeparator();

		if (devIdOld.equals(HDDRecorder.DEVICE_ALL)){
			JMenuItem menuItem = new JMenuItem("デバイスを選択する【"+devNameNew+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateDeviceList(devNameNew);
					updateDeviceInfoLabel();
					updateFolderList(folderName);
					updateTitleList(false, false, false	, false, false, false);
				}
			});

			pop.add(menuItem);
		}
		else{
			JMenuItem menuItem = new JMenuItem("デバイスの選択を解除する");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateDeviceList(rec.getDeviceName(HDDRecorder.DEVICE_ALL));
					updateDeviceInfoLabel();
					updateFolderList(folderName);
					updateTitleList(false, false, false, false, false, false);
				}
			});

			pop.add(menuItem);
		}
	}

	/**
	 *  フォルダー絞り込みのメニューアイテムを追加する
	 */
	protected void appendSelectFolderMenuItem(final JPopupMenu pop, final String ttlId){
		HDDRecorder rec = getSelectedRecorder();
		TitleInfo ttl = rec.getTitleInfo(ttlId);

		if (ttl == null)
			return;

		ArrayList<TextValueSet> folders = ttl.getRec_folder();
		if (folders == null || folders.isEmpty())
			return;

		String folderName = folders.get(0).getText();
		String folderId = folderName != null ? text2value(rec.getFolderList(), folderName) : "";

		int idx = jCBXPanel_folder.getSelectedIndex();
		if (idx == 0){
			JMenuItem menuItem = new JMenuItem("フォルダを選択する【"+folderName+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateFolderList(folderName);
					updateFolderButtons();
					updateTitleList(false, false, false, false, false, false);
				}
			});

			pop.add(menuItem);
		}
		else{
			JMenuItem menuItem = new JMenuItem("フォルダの選択を解除する");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					jCBXPanel_folder.setSelectedIndex(0);
					updateTitleList(false, false, false, false, false, false);
				}
			});

			pop.add(menuItem);
		}

		if (!folderId.isEmpty()){
			JMenuItem menuItem = new JMenuItem("フォルダを編集する【"+folderName+"】");
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {

					editFolderName(folderId, folderName);
				}
			});

			pop.add(menuItem);
		}
	}

}
