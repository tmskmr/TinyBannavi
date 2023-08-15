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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import tainavi.HDDRecorder.RecType;

/**
 * レコーダ設定のタブ
 * @since 3.15.4β　{@link Viewer}から分離
 */
public abstract class AbsRecorderSettingView extends JScrollPane {

	private static final long serialVersionUID = 1L;

	public static String getViewName() { return "レコーダ設定"; }

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	protected abstract Env getEnv();
	protected abstract RecorderInfoList getRecInfos();
	protected abstract HDDRecorderList getRecPlugins();

	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract Component getParentComponent();
	protected abstract VWColorChooserDialog getCcWin();

	protected abstract void ringBeep();

	// クラス内のイベントから呼び出されるもの

	/**
	 * レコーダ一覧の更新を反映してもらう
	 */
	protected abstract void setRecInfos();

	/*******************************************************************************
	 * 呼び出し元から引き継いだもの
	 ******************************************************************************/

	// オブジェクト
	//private final Env env = getEnv();
	private final RecorderInfoList recInfoList = getRecInfos();
	private final HDDRecorderList recPlugins = getRecPlugins();

	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	private final Component parent = getParentComponent();	// これは起動時に作成されたまま変更されないオブジェクト
	private final VWColorChooserDialog ccwin = getCcWin();	// これは起動時に作成されたまま変更されないオブジェクト

	// メソッド
	//private void StdAppendMessage(String message) { System.out.println(message); }
	//private void StdAppendError(String message) { System.err.println(message); }
	private void StWinSetVisible(boolean b) { StWin.setVisible(b); }
	private void StWinSetLocationCenter(Component frame) { CommonSwingUtils.setLocationCenter(frame, (VWStatusWindow)StWin); }

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連

	private static final int PARTS_WIDTH = 900;
	private static final int PARTS_HEIGHT = 30;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;
	private static final int SEP_HEIGHT_NALLOW = 5;

	private static final int LABEL_WIDTH = 250;
	private static final int CCLABEL_WIDTH = 140;
	private static final int TEXT_WIDTH = CCLABEL_WIDTH*2+SEP_WIDTH;

	private static final int BUTTON_WIDTH = 75;
	private static final int BUTTON_X = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH+TEXT_WIDTH+SEP_WIDTH+TEXT_WIDTH+SEP_WIDTH+150;

	private static final int UPDATE_WIDTH = 250;
	private static final int HINT_WIDTH = 750;

	private static final int PANEL_WIDTH = LABEL_WIDTH+PARTS_WIDTH+SEP_WIDTH*3;

	//
	private static final String ITEM_SCOPEDISABLE = "チェックしない";

	private static final String VALUE_CALENDAR_ENABLED = "C";

	// テキスト

	private static final String TEXT_HINT =
			"IPアドレス欄はx.x.x.x形式だけでなくホスト名でも指定が可能です。名前解決にはhostsファイルなどを利用してください。"+
			"フォルダの追加やプロファイルの変更など、レコーダの設定を変更した際は予約一覧取得を行って設定を取得してください。";

	// ログ関連

	private static final String MSGID = "["+getViewName()+"] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	// カラム設定

	/**
	 * テーブル上に"色"ではじまる項目名がいくつあるか＝エンコーダを何個まで許容するか。
	 */
	private static final ArrayList<RecorderColumn> colorColumns = new ArrayList<RecorderColumn>();

	private static enum RecorderColumn {
		NAME		("表示名",		200),
		IP			("IP",			100),
		PORT		("PORT",		50),
		MAC			("MAC",			100),
		BCAST		("ﾌﾞﾛｰﾄﾞｷｬｽﾄ",	100),
		RECID		("機種",			150),
		USER		("ID",			80),
		PW			("PW",			80),
		CALENDAR	("連",			25),
		SCOPE		("範",			25),
		TUNUM		("数",			25),
		C1			("色1",			40),
		C2			("色2",			40),
		C3			("色3",			40),
		C4			("色4",			40),
		C5			("色5",			40),
		C6			("色6",			40),
		C7			("色7",			40),
		C8			("色8",			40),
		;

		private String name;
		private int iniWidth;

		private RecorderColumn(String name, int iniWidth) {
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

	private JPanel jPanel_recorder = null;
	private JPanel jPanel_update = null;

	private JButton jButton_update = null;

	private JLabel jLabel_recordertype = null;
	private JComboBox jComboBox_recordertype = null;

	private JLabel jLabel_recorder = null;
	private JTextFieldWithPopup jTextField_ipaddr = null;
	private JTextFieldWithPopup jTextField_port = null;

	private JLabel jLabel_name = null;
	private JTextFieldWithPopup jTextField_name = null;

	private JLabel jLabel_user = null;
	private JTextFieldWithPopup jTextField_user = null;
	private JPanel jPanel_passwd = null;
	private JPasswordField jTextField_passwd = null;

	private JLabel jLabel_wakeup = null;
	private JTextFieldWithPopup jTextField_macaddr = null;
	private JTextFieldWithPopup jTextField_broadcast = null;
	private JButton jButton_getmac = null;

	private JLabel jLabel_color = null;
	private JCCLabel jLabel_colorcells[] = null;

	private JButton jButton_recorderadd = null;
	private JButton jButton_recorderupd = null;
	private JButton jButton_recorderdel = null;
	private JButton jButton_recorderup = null;
	private JButton jButton_recorderdown = null;

	private JScrollPane jScrollPane_recorders = null;
	private RecorderTable jTable_recorders = null;

	private JTextAreaWithPopup jta_help = null;

	private JCheckBoxPanel jCBP_calendar = null;

	private JCheckBoxPanel jCBP_chchange = null;

	// 主にTvRock/EDCB用
	private JComboBoxPanel jCBX_scope = null;

	// 主にDIGA用
	private JComboBoxPanel jCBX_tunernum = null;

	// 主にTvRock用 - 使わなくなった
	private JTextFieldWithPopup jtf_opt = null;

	//private DefaultTableModel tableModel_recoders = new DefaultTableModel();

	// コンポーネント以外

	// テーブルの実態
	private final RowItemList<RecorderItem> rowData = new RowItemList<RecorderItem>();


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsRecorderSettingView() {

		super();

		// 色設定は何個あるかな？
		for ( RecorderColumn rc : RecorderColumn.values() ) {
			if ( rc.getName().startsWith("色") ) {
				colorColumns.add(rc);
			}
		}

		this.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.setColumnHeaderView(getJPanel_update());
		this.setViewportView(getJPanel_recorder());

		// テーブルを初期化
		updateTable();

		// 初期値
		setSelectedRecoderTypeDefault();

		// 更新ボタンは使えない
		jButton_recorderupd.setEnabled(false);

		setUpdateButtonEnhanced(false);
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	private void updateTable() {

		rowData.clear();

		// エンコーダ別色分けのどーたらこーたら
		for ( RecorderInfo ri : recInfoList ) {
			RecorderItem sa = new RecorderItem();
			sa.name = ri.getRecorderName();
			sa.ipaddr = ri.getRecorderIPAddr();
			sa.portno = ri.getRecorderPortNo();
			sa.macaddr = ri.getRecorderMacAddr();
			sa.broadcast = ri.getRecorderBroadcast();
			sa.recorderid = ri.getRecorderId();
			sa.user = ri.getRecorderUser();
			sa.password = ri.getRecorderPasswd();
			sa.calendar = ri.getUseCalendar();
			sa.hide_chchange = ri.getUseChChange();	// 隠し
			sa.scope = ri.getRecordedCheckScope();
			sa.tunernum = ri.getTunerNum();
			for ( int n=0; n<colorColumns.size(); n++ ) {
				sa.setCx(n, CommonSwingUtils.getColoredString(ri.getEncoderColor(n),ri.getEncoder(n)));
			}

			sa.fireChanged();

			rowData.add(sa);
		}

		((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
	}

	private void setSelectedRecoderTypeDefault() {
		jComboBox_recordertype.setSelectedItem(null);
		jComboBox_recordertype.setSelectedItem("NULL");
		if ( ! "NULL".equals((String) jComboBox_recordertype.getSelectedItem()) && jComboBox_recordertype.getItemCount() >= 1 ) {
			jComboBox_recordertype.setSelectedIndex(0);
		}
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	private void setUpdateButtonEnhanced(boolean b) {
		if (b) {
			///jButton_update.setText("更新を確定する");
			jButton_update.setForeground(Color.RED);
		}
		else {
			//jButton_update.setText("更新を確定する");
			jButton_update.setForeground(Color.BLACK);
		}
		jButton_update.setEnabled(b);
	}

	/**
	 * 更新を確定する
	 */
	private final ActionListener al_update = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			TatCount tc = new TatCount();

			MWin.appendMessage(MSGID+"設定を保存します");
			StWin.clear();

			new SwingBackgroundWorker(false) {

				@Override
				protected Object doWorks() throws Exception {


					if (debug) System.err.println(DBGID+"追加するレコーダ情報の数： "+rowData.size());

					if ( rowData.size() == 0 ) {
							// からっぽだ
							recInfoList.clear();
							setRecInfos();
					}
					else {
						try {
							RecorderInfoList newRIL = new RecorderInfoList();

							for ( RecorderItem c : rowData ) {
								RecorderInfo ri = new RecorderInfo();
								ri.setRecorderName(c.name);
								ri.setRecorderIPAddr(c.ipaddr);
								ri.setRecorderPortNo(c.portno);
								ri.setRecorderMacAddr(c.macaddr);
								ri.setRecorderBroadcast(c.broadcast);
								ri.setRecorderId(c.recorderid);
								ri.setRecorderUser(c.user);
								ri.setRecorderPasswd(c.password);
								ri.setUseCalendar(c.calendar);
								ri.setUseChChange(c.hide_chchange);
								ri.setRecordedCheckScope(c.scope);
								ri.setTunerNum(c.tunernum);
								//
								ri.clearEncoders();
								ri.clearEncoderColors();
								for (int n=0; n<colorColumns.size(); n++) {
									String[] d = CommonSwingUtils.splitColorString(c.getCx(n));
									if (d != null && d.length >= 2) {
										ri.addEncoder(d[0]);
										ri.addEncoderColor(d[1]);
									}
								}

								if (debug) System.err.println(DBGID+"レコーダ情報を追加： "+ri.getRecorderIPAddr()+":"+ri.getRecorderPortNo()+":"+ri.getRecorderId());

								newRIL.add(ri);
							}

							// 置き換え
							recInfoList.clear();
							for ( RecorderInfo ri : newRIL ) {
								recInfoList.add(ri);
							}

							// 設定保存
							setRecInfos();
						}
						catch ( Exception e ) {
							e.printStackTrace();
						}
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

			setUpdateButtonEnhanced(false);
		}
	};

	/**
	 * レコーダを選択する
	 */
	private final ItemListener il_recorderSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {

				String recId = (String) ((JComboBox)e.getSource()).getSelectedItem();

				// 値をクリアする
				clearInputs();

				// ラベルを適切な値に変更する
				jTextField_name.setEnabled(true);
				jTextField_ipaddr.setEnabled(true);
				jTextField_port.setEnabled(true);
				jTextField_user.setEnabled(true);
				jTextField_passwd.setEnabled(true);
				jTextField_macaddr.setEnabled(true);
				jTextField_broadcast.setEnabled(true);
				jButton_getmac.setEnabled(false);
				jCBP_calendar.setEnabled(true);
				jCBX_scope.setEnabled(true);
				jCBX_tunernum.setEnabled(false);

				// とりあえずIDの一致するものを全部拾う
				HDDRecorderList rl = recPlugins.findPlugin(recId);
				if ( rl.size() <= 0) {
					MWin.appendError(ERRID+"選択されたプラグインのインスタンスが存在しない： "+recId);
					return;
				}

				HDDRecorder rec = rl.get(0);
				RecType recTyp = rl.getRecId2Type(recId);
				switch (recTyp) {
				case MAIL:
					jLabel_recorder.setText("SMTPサーバ/SMTPのポート番号");
					jLabel_user.setText("Fromアドレス/SMTPのパスワード");
					jLabel_wakeup.setText("Toアドレス/RDに設定したパスワード");
					break;

				case CALENDAR:
					jLabel_recorder.setText("-");
					jLabel_user.setText("GoogleのID/パスワード");
					jLabel_wakeup.setText("リマインダ(分)");

					jTextField_ipaddr.setEnabled(false);
					jTextField_port.setEnabled(false);
					jTextField_broadcast.setEnabled(false);
					jCBP_calendar.setEnabled(false);
					jCBX_scope.setEnabled(false);

					if (rec.getUser().length() > 0)		jTextField_user.setText(rec.getUser());
					if (rec.getPasswd().length() > 0)	jTextField_passwd.setText(rec.getPasswd());
					if (rec.getMacAddr().length() > 0)	jTextField_macaddr.setText(rec.getMacAddr());
					break;

				case TUNER:
					jLabel_recorder.setText("-");
					jLabel_user.setText("インストールディレクトリ");
					jLabel_wakeup.setText("-");

					jTextField_user.setText(rl.findPlugin(recId).get(0).getUser());
					jTextField_passwd.setText("dummy");

					jTextField_ipaddr.setEnabled(false);
					jTextField_port.setEnabled(false);
					jTextField_passwd.setEnabled(false);
					jTextField_macaddr.setEnabled(false);
					jTextField_broadcast.setEnabled(false);
					break;

				case EPG:
				case NULL:
					jLabel_recorder.setText("-");
					jLabel_user.setText("-");
					jLabel_wakeup.setText("-");

					jTextField_ipaddr.setText("EPG");
					jTextField_port.setText("dummy");
					jTextField_user.setText("dummy");
					jTextField_passwd.setText("dummy");

					jTextField_name.setEnabled(false);
					jTextField_ipaddr.setEnabled(false);
					jTextField_port.setEnabled(false);
					jTextField_user.setEnabled(false);
					jTextField_passwd.setEnabled(false);
					jTextField_macaddr.setEnabled(false);
					jTextField_broadcast.setEnabled(false);
					jCBX_scope.setEnabled(false);
					break;

				case RECORDER:
				default:
					jLabel_recorder.setText("レコーダIP/PORT");
					jLabel_user.setText("ID/PASS");
					jLabel_wakeup.setText("レコーダMAC/ブロードキャスト(任意)");

					if (rec.getIPAddr().length() > 0)	jTextField_ipaddr.setText(rec.getIPAddr());
					if (rec.getPortNo().length() > 0)	jTextField_port.setText(rec.getPortNo());
					if (rec.getUser().length() > 0)		jTextField_user.setText(rec.getUser());
					if (rec.getPasswd().length() > 0)	jTextField_passwd.setText(rec.getPasswd());

					jButton_getmac.setEnabled(true);

					break;
				}

				jTextField_name.setText(rec.getName());
				jCBP_chchange.setEnabled(rec.isChangeChannelSupported());

				jCBP_calendar.setSelected(rec.getUseCalendar());

				// 主にDIGA用
				jCBX_tunernum.setLabelText("-");
				if ( rl.get(0).getTunerNum() > 0 ) {
					jCBX_tunernum.setLabelText("最大同時録画数の手動設定");
					jCBX_tunernum.setEnabled(true);
					jCBX_tunernum.setSelectedItem(String.valueOf(rl.get(0).getTunerNum()));
				}

				// 主にTvRock用
				String opt = rl.get(0).getOptString();
				if ( opt == null ) {
					jtf_opt.setEnabled(false);
					jtf_opt.setText("");
				}
				else {
					jtf_opt.setEnabled(true);
					jtf_opt.setText(opt);
				}
			}
		}
	};

	/**
	 * レコーダを追加する
	 */
	private final ActionListener al_addRecorder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (CommonUtils.isNGChar(jTextField_ipaddr.getText()) || CommonUtils.isNGChar(jTextField_port.getText())) {
				MWin.appendError(ERRID+"【警告】レコーダIPまたはPORTに次の文字は利用できません　"+CommonUtils.getNGCharList());
				ringBeep();
				return;
			}
			//
			if ( jTextField_user.getText().length() == 0 || jTextField_passwd.getPassword().length == 0 ) {
				MWin.appendError(ERRID+"【警告】IDとパスワードの入力は必須です");
				ringBeep();
				return;
			}

			// 重複チェック
			if (!validateInputData(-1))
				return;

			// 入力からテーブルへ
			RecorderItem sa = copyInputs2Table();
			rowData.add(sa);

			// 入力欄をクリアする
			clearInputs();

			((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
			//jTable_recorders.updateUI();

			setSelectedRecoderTypeDefault();

			// 必要ないはずだけど
			jButton_recorderupd.setEnabled(false);

			setUpdateButtonEnhanced(true);

			return;
		}
	};

	/*
	 * 入力内容をチェックする
	 */
	private boolean validateInputData(int rowExc){
		for ( int row=0; row<rowData.size(); row++ ) {
			if (row == rowExc)
				continue;

			RecorderItem c = rowData.get(row);
			String name = c.name;
			String ip = c.ipaddr;
			String port = c.portno;
			String recid = c.recorderid;
			if ( ip.equals(jTextField_ipaddr.getText()) &&
					port.equals(jTextField_port.getText()) &&
					recid.equals((String)jComboBox_recordertype.getSelectedItem())
					) {
				MWin.appendError(ERRID+"レコーダ情報が重複しています： "+ip+":"+port+":"+recid);
				ringBeep();
				return false;
			}
			if ( name.length() > 0 && name.equals(jTextField_name.getText())){
				MWin.appendError(ERRID+"レコーダ名が重複しています： "+name);
				ringBeep();
				return false;
			}
		}

		return true;
	}


	/**
	 * レコーダを更新する
	 */
	private final ActionListener al_updRecorder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (CommonUtils.isNGChar(jTextField_ipaddr.getText()) || CommonUtils.isNGChar(jTextField_port.getText())) {
				MWin.appendError(ERRID+"【警告】レコーダIPまたはPORTに次の文字は利用できません　"+CommonUtils.getNGCharList());
				ringBeep();
				return;
			}
			//
			if ( jTextField_user.getText().length() == 0 || jTextField_passwd.getPassword().length == 0 ) {
				MWin.appendError(ERRID+"【警告】IDとパスワードの入力は必須です");
				ringBeep();
				return;
			}

			// 入力からテーブルへ
			int row =  jTable_recorders.convertRowIndexToModel(jTable_recorders.getSelectedRow());

			// 重複チェック
			if (!validateInputData(row))
				return;

			RecorderItem sa = copyInputs2Table();
			rowData.set(row, sa);

			// 入力欄をクリアする
			clearInputs();

			((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
			//jTable_recorders.updateUI();

			setSelectedRecoderTypeDefault();

			// 更新ボタンは無効に
			jButton_recorderupd.setEnabled(false);

			setUpdateButtonEnhanced(true);

			return;
		}
	};

	/**
	 * レコーダを削除する
	 */
	private final ActionListener al_delRecorder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {

			int row = jTable_recorders.getSelectedRow();

			if ( row < rowData.size() ) {

				// テーブルの情報を入力にコピー
				copyTable2Inputs(rowData.remove(row));

			}

			((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
			//jTable_recorders.updateUI();

			// 更新ボタンは無効に
			jButton_recorderupd.setEnabled(false);

			setUpdateButtonEnhanced(true);

		}
	};

	/**
	 * レコーダを一個上に上げる
	 */
	private final MouseListener ml_upRecorder = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			int row = jTable_recorders.getSelectedRow();
			rowData.up(row, 1);

			if ( row > 0 ) {
				((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
				jTable_recorders.setRowSelectionInterval(row-1,row-1);
			}

			setUpdateButtonEnhanced(true);
		}
	};

	/**
	 * レコーダを一個下に下げる
	 */
	private final MouseListener ml_downRecorder = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			int row = jTable_recorders.getSelectedRow();
			rowData.down(row, 1);

			if ( row < (rowData.size()-1) ) {
				((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
				jTable_recorders.setRowSelectionInterval(row+1,row+1);
			}

			setUpdateButtonEnhanced(true);
		}
	};

	/**
	 * MACアドレスを取得する
	 */
	private final MouseListener ml_getmac = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e) {

			Component comp = (Component) e.getSource();

			comp.setEnabled(false);

			if ( ! _getmac() ) {
				ringBeep();
			}

			comp.setEnabled(true);

		}

	};

	private boolean _getmac() {

		String mac = null;
		String host = jTextField_ipaddr.getText();
		String ip = null;

		try {
			ip = InetAddress.getByName(host).getHostAddress();
		}
		catch (UnknownHostException e1) {
			MWin.appendError(ERRID+"レコーダの指定をIPアドレスに変換できません： "+host);
			e1.printStackTrace();
			return false;
		}

		mac = ArpCommand.getMac(ip);
		if ( mac != null && mac.length() == 12 ) {
			jTextField_macaddr.setText(mac);
			jTextField_broadcast.setText("255.255.255.255");
		}

		if ( mac == null ) {
			MWin.appendError(ERRID+"MACアドレスの取得に失敗しました： "+host+"("+ip+")");
			return false;
		}

		MWin.appendError(MSGID+"MACアドレスを取得しました： "+host+"("+ip+") -> "+mac);
		return true;

	}

	/**
	 * テーブルはコピーさせないよ！
	 */
	private final KeyListener kl_cannnotcopy = new KeyAdapter() {
		@Override
		public void keyTyped(KeyEvent e) {
			// クリップボードに残さない
			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection s = new StringSelection("きさま！　見ているなッ！");
			cb.setContents(s, null);
			//　beep!メガドライブ
			ringBeep();
		}
	};

	/**
	 * テーブルで行が選択された
	 */
	private final MouseListener ml_tableSelected = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)) {
				//
				JTable t = (JTable) e.getSource();
				Point p = e.getPoint();

				int row = t.convertRowIndexToModel(t.rowAtPoint(p));

				// 入力に戻す
				copyTable2Inputs(rowData.get(row));

				// 更新ボタンを有効にする
				jButton_recorderupd.setEnabled(true);

				//setUpdateButtonEnhanced(true);	// 新規・更新・削除・上・下ボタンを押した時だけ復活すればいい
			}
		}
	};

	private void clearInputs() {
		jTextField_name.setText("");;
		jTextField_ipaddr.setText("");
		jTextField_port.setText("");
		jTextField_user.setText("");
		jTextField_passwd.setText("");
		jTextField_macaddr.setText("");
		jTextField_broadcast.setText("");
		for (int c=0; c<colorColumns.size(); c++) {
			jLabel_colorcells[c].setChoosed(new Color(0xff,0x00,0x00));
			jLabel_colorcells[c].setText(colorColumns.get(c).getName());
		}
		jCBP_calendar.setSelected(true);
		jCBX_scope.setSelectedIndex(0);
		jCBX_tunernum.setSelectedIndex(0);
	}

	/**
	 * 入力をテーブルにコピーするためにオブジェクトを作成
	 */
	private RecorderItem copyInputs2Table() {

		RecorderItem sa = new RecorderItem();

		sa.name = jTextField_name.getText();
		sa.ipaddr = jTextField_ipaddr.getText();
		sa.portno = jTextField_port.getText();
		if ( jTextField_macaddr.getText().matches("^[ 0-9a-zA-Z:-]+$") ) {
			sa.macaddr = jTextField_macaddr.getText().replaceAll("[:-]", "").toUpperCase();
		}
		else {
			sa.macaddr = jTextField_macaddr.getText();
		}
		sa.broadcast = jTextField_broadcast.getText();
		sa.recorderid = (String)jComboBox_recordertype.getSelectedItem();
		sa.user = jTextField_user.getText();
		sa.password = new String(jTextField_passwd.getPassword());

		sa.calendar = jCBP_calendar.isSelected();
		sa.hide_chchange = jCBP_chchange.isSelected();
		if ( ITEM_SCOPEDISABLE.equals((String) jCBX_scope.getSelectedItem()) ) {
			sa.scope = 0;
		}
		else {
			sa.scope = jCBX_scope.getSelectedIndex();
		}

		// 主にDIGA用
		if ( jCBX_tunernum.isEnabled() ) {
			sa.tunernum = Integer.valueOf((String) jCBX_tunernum.getSelectedItem());
		}
		else {
			sa.tunernum = 0;
		}

		for (int n=0; n<colorColumns.size(); n++) {
			String cs = CommonUtils.color2str(jLabel_colorcells[n].getChoosed());
			String es = jLabel_colorcells[n].getText();
			if ( n == 0 ) {
				es = (es.length() != 0) ? (jLabel_colorcells[n].getText()):("■");
			}
			sa.setCx(n, CommonSwingUtils.getColoredString(cs,es));
		}

		sa.fireChanged();

		return sa;

	}

	/**
	 * テーブルの情報を入力に書き戻す
	 */
	private void copyTable2Inputs(RecorderItem c) {

		jComboBox_recordertype.setSelectedItem(c.recorderid);
		//
		jTextField_name.setText(c.name);
		jTextField_ipaddr.setText(c.ipaddr);
		jTextField_port.setText(c.portno);
		jTextField_user.setText(c.user);
		jTextField_passwd.setText(c.password);
		jTextField_macaddr.setText(c.macaddr);
		jTextField_broadcast.setText(c.broadcast);

		jCBP_calendar.setSelected(c.calendar);
		jCBP_chchange.setSelected(c.hide_chchange);
		jCBX_scope.setSelectedIndex(c.scope);

		// 主にDIGA用
		if ( c.tunernum > 0 ) {
			jCBX_tunernum.setSelectedItem(String.valueOf(c.tunernum));
		}

		// イロイッカイヅツ
		for (int n=0; n<colorColumns.size(); n++) {
			String[] d = CommonSwingUtils.splitColorString(c.getCx(n));
			jLabel_colorcells[n].setChoosed(CommonUtils.str2color(d[1]));
			jLabel_colorcells[n].setText(d[0]);
		}

	}


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

	private JPanel getJPanel_recorder() {
		if (jPanel_recorder == null)
		{
			jPanel_recorder = new JPanel();
			jPanel_recorder.setLayout(new SpringLayout());

			/*
			 * レコーダ関連
			 */
			int table_w = 25;
			for ( RecorderColumn rc : RecorderColumn.values() ) {
				table_w += (rc.getIniWidth()>0)?(rc.getIniWidth()):(0);
			}

			int y = SEP_HEIGHT;
			int x = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel_recorder, new JLabel("登録済みレコーダ一覧"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			int table_h = PARTS_HEIGHT*5;
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJScrollPane_recorders(), table_w, table_h, SEP_WIDTH, y);

			y+=(table_h+SEP_HEIGHT);
			int yz = y;
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJButton_recorderup("上へ"), BUTTON_WIDTH, PARTS_HEIGHT, BUTTON_X, yz);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJButton_recorderdown("下へ"), BUTTON_WIDTH, PARTS_HEIGHT, BUTTON_X, yz+=(PARTS_HEIGHT+SEP_HEIGHT_NALLOW));

			yz += (PARTS_HEIGHT+SEP_HEIGHT_NALLOW)*3;

			CommonSwingUtils.putComponentOn(jPanel_recorder, getJButton_recorderadd("登録"), BUTTON_WIDTH, PARTS_HEIGHT, BUTTON_X, yz);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJButton_recorderupd("置換"), BUTTON_WIDTH, PARTS_HEIGHT, BUTTON_X, yz+=(PARTS_HEIGHT+SEP_HEIGHT_NALLOW));
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJButton_recorderdel("削除"), BUTTON_WIDTH, PARTS_HEIGHT, BUTTON_X, yz+=(PARTS_HEIGHT+SEP_HEIGHT_NALLOW));

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJLabel_recordertype("レコーダ機種"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJComboBox_recordertype(), TEXT_WIDTH, PARTS_HEIGHT, x, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJLabel_name("レコーダ表示名"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_name(""), TEXT_WIDTH, PARTS_HEIGHT, x, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJLabel_recorder("レコーダIP/PORT"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_ipaddr(""), TEXT_WIDTH, PARTS_HEIGHT, x, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_port(""), TEXT_WIDTH, PARTS_HEIGHT, x+TEXT_WIDTH+SEP_WIDTH, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJLabel_user("ID/PASS"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_user(""), TEXT_WIDTH, PARTS_HEIGHT, x, y);
			if ( ! CommonUtils.isLinux() ) {
				CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_passwd(""), TEXT_WIDTH, PARTS_HEIGHT, x+TEXT_WIDTH+SEP_WIDTH, y);
			}
			else {
				if (debug) System.out.println(DBGID+"fixies JPasswordField problem");
				CommonSwingUtils.putComponentOn(jPanel_recorder, getJPanel_passwd(), TEXT_WIDTH, PARTS_HEIGHT, x+TEXT_WIDTH+SEP_WIDTH, y);
			}

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJLabel_wakeup("レコーダMAC/ブロードキャスト(任意)"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_macaddr(""), TEXT_WIDTH, PARTS_HEIGHT, x, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJTextField_broadcast(""), TEXT_WIDTH, PARTS_HEIGHT, x+TEXT_WIDTH+SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJButton_getMac("Get MAC"), BUTTON_WIDTH, PARTS_HEIGHT, x+(TEXT_WIDTH+SEP_WIDTH)*2, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJLabel_color("配色"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			int ny = 0;
			jLabel_colorcells = new JCCLabel[colorColumns.size()];
			for (int c=0; c<colorColumns.size(); c++) {
				jLabel_colorcells[c] = new JCCLabel(colorColumns.get(c).getName(), Color.RED, true, jPanel_recorder, ccwin);
				CommonSwingUtils.putComponentOn(jPanel_recorder, jLabel_colorcells[c], CCLABEL_WIDTH, PARTS_HEIGHT, x+(c%4)*(CCLABEL_WIDTH+SEP_WIDTH), (ny = y+((c-c%4)/4)*(PARTS_HEIGHT+SEP_HEIGHT)));
			}
			y = ny;

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, jCBP_calendar = new JCheckBoxPanel("カレンダー連携する",LABEL_WIDTH+SEP_WIDTH), LABEL_WIDTH+SEP_WIDTH+CCLABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJCBX_scope("録画に成功した記録をチェックする範囲(日)",LABEL_WIDTH+SEP_WIDTH,CCLABEL_WIDTH), LABEL_WIDTH+SEP_WIDTH+CCLABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJComboBox_TunerNum("最大同時録画数の手動設定",LABEL_WIDTH+SEP_WIDTH,CCLABEL_WIDTH), LABEL_WIDTH+SEP_WIDTH+CCLABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, jCBP_chchange = new JCheckBoxPanel("チャンネル操作を有効にする",LABEL_WIDTH+SEP_WIDTH), LABEL_WIDTH+SEP_WIDTH+CCLABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);
			CommonSwingUtils.putComponentOn(jPanel_recorder, new JLabel("オプション指定(形式:KEY=VAL;)"), LABEL_WIDTH, PARTS_HEIGHT, SEP_WIDTH, y);
			CommonSwingUtils.putComponentOn(jPanel_recorder, getJtf_opt(), TEXT_WIDTH*2+SEP_WIDTH, PARTS_HEIGHT, x, y);

			y+=(PARTS_HEIGHT+SEP_HEIGHT);

			// 画面の全体サイズを決める
			Dimension d = new Dimension(PANEL_WIDTH,y);
			jPanel_recorder.setPreferredSize(d);
		}

		return jPanel_recorder;
	}


	// 更新確定ボタン
	private JButton getJButton_update(String s) {
		if (jButton_update == null) {
			jButton_update = new JButton(s);

			jButton_update.addActionListener(al_update);
		}
		return(jButton_update);
	}


	/*
	 *	レコーダ関連
	 */

	// レコーダからエンコーダ情報をもらったら一覧に反映する
	public void redrawRecorderEncoderEntry() {
		int row = 0;
		for ( RecorderInfo ri : recInfoList ) {
			for (int n=0; n<colorColumns.size(); n++) {
				RecorderItem c = rowData.get(row);
				String[] d = CommonSwingUtils.splitColorString(c.getCx(n));
				String e = ri.getEncoder(n);
				c.setCx(n,CommonSwingUtils.getColoredString(d[1],e));
				c.fireChanged();
			}
			row++;
		}

		((DefaultTableModel) jTable_recorders.getModel()).fireTableDataChanged();
	}

	private JButton getJButton_recorderadd(String s) {
		if (jButton_recorderadd == null) {
			jButton_recorderadd = new JButton(s);
			jButton_recorderadd.setForeground(Color.RED);
			jButton_recorderadd.addActionListener(al_addRecorder);
		}
		return(jButton_recorderadd);
	}

	private JButton getJButton_recorderupd(String s) {
		if (jButton_recorderupd == null) {
			jButton_recorderupd = new JButton(s);
			jButton_recorderupd.setForeground(Color.RED);
			jButton_recorderupd.addActionListener(al_updRecorder);
		}
		return(jButton_recorderupd);
	}

	private JButton getJButton_recorderdel(String s) {
		if (jButton_recorderdel == null) {
			jButton_recorderdel = new JButton(s);
			jButton_recorderdel.setForeground(Color.BLUE);
			jButton_recorderdel.addActionListener(al_delRecorder);
		}
		return(jButton_recorderdel);
	}

	private JButton getJButton_recorderup(String s) {
		if (jButton_recorderup == null) {
			jButton_recorderup = new JButton(s);
			jButton_recorderup.addMouseListener(ml_upRecorder);
		}
		return(jButton_recorderup);
	}

	private JButton getJButton_recorderdown(String s) {
		if (jButton_recorderdown == null) {
			jButton_recorderdown = new JButton(s);
			jButton_recorderdown.addMouseListener(ml_downRecorder);
		}
		return(jButton_recorderdown);
	}

	/*
	 * 機種
	 */

	private JLabel getJLabel_recordertype(String s) {
		if (jLabel_recordertype == null) {
			jLabel_recordertype = new JLabel();
			jLabel_recordertype.setText(s);
		}
		return(jLabel_recordertype);
	}

	private JComboBox getJComboBox_recordertype() {
		if (jComboBox_recordertype == null) {
			jComboBox_recordertype = new JComboBox();

			DefaultComboBoxModel model = new DefaultComboBoxModel();
			jComboBox_recordertype.setModel(model);
			for ( HDDRecorder r : recPlugins ) {
				int idx = 0;
				for (int i=0; i<model.getSize(); i++) {
					if (((String)model.getElementAt(i)).compareToIgnoreCase(r.getRecorderId()) < 0) {
						// ID昇順でリストアップする
						idx = i+1;
					}
				}

				model.insertElementAt(r.getRecorderId(),idx);
			}

			jComboBox_recordertype.addItemListener(il_recorderSelected);
		}
		return(jComboBox_recordertype);
	}

	/*
	 * レコーダー名
	 */

	private JLabel getJLabel_name(String s) {
		if (jLabel_name == null) {
			jLabel_name = new JLabel();
			jLabel_name.setText(s);
		}
		return(jLabel_name);
	}

	private JTextField getJTextField_name(String s) {
		if (jTextField_name == null) {
			jTextField_name = new JTextFieldWithPopup();
			jTextField_name.setText(s);
		}
		return jTextField_name;
	}

	/*
	 * IP・PORT
	 */

	private JLabel getJLabel_recorder(String s) {
		if (jLabel_recorder == null) {
			jLabel_recorder = new JLabel();
			jLabel_recorder.setText(s);
		}
		return(jLabel_recorder);
	}

	private JTextField getJTextField_ipaddr(String s) {
		if (jTextField_ipaddr == null) {
			jTextField_ipaddr = new JTextFieldWithPopup();
			jTextField_ipaddr.setText(s);
		}
		return jTextField_ipaddr;
	}

	private JTextField getJTextField_port(String s) {
		if (jTextField_port == null) {
			jTextField_port = new JTextFieldWithPopup();
			jTextField_port.setText(s);
		}
		return jTextField_port;
	}

	/*
	 * ユーザ
	 */

	private JLabel getJLabel_user(String s) {
		if (jLabel_user == null) {
			jLabel_user = new JLabel();
			jLabel_user.setText(s);
		}
		return(jLabel_user);
	}

	private JTextField getJTextField_user(String s) {
		if (jTextField_user == null) {
			jTextField_user = new JTextFieldWithPopup();
			jTextField_user.setText(s);
		}
		return jTextField_user;
	}

	private JPanel getJPanel_passwd() {
		if (jPanel_passwd == null) {
			jPanel_passwd = new JPanel();
			jPanel_passwd.setLayout(new BorderLayout());
			jPanel_passwd.add(getJTextField_passwd(""), BorderLayout.CENTER);
		}
		return jPanel_passwd;
	}

	private JPasswordField getJTextField_passwd(String s) {
		if (jTextField_passwd == null) {
			jTextField_passwd = new JPasswordField(s);
		}
		return jTextField_passwd;
	}

	/*
	 * MACアドレス
	 */

	private JLabel getJLabel_wakeup(String s) {
		if (jLabel_wakeup == null) {
			jLabel_wakeup = new JLabel();
			jLabel_wakeup.setText(s);
		}
		return(jLabel_wakeup);
	}

	private JTextField getJTextField_macaddr(String s) {
		if (jTextField_macaddr == null) {
			jTextField_macaddr = new JTextFieldWithPopup();
			jTextField_macaddr.setText(s);
		}
		return jTextField_macaddr;
	}

	private JTextField getJTextField_broadcast(String s) {
		if (jTextField_broadcast == null) {
			jTextField_broadcast = new JTextFieldWithPopup();
			jTextField_broadcast.setText(s);
		}
		return jTextField_broadcast;
	}

	private JButton getJButton_getMac(String s) {
		if (jButton_getmac == null) {
			jButton_getmac = new JButton(s);
			jButton_getmac.addMouseListener(ml_getmac);
		}
		return(jButton_getmac);
	}

	/*
	 * チューナ・エンコーダカラー
	 */

	private JLabel getJLabel_color(String s) {
		if (jLabel_color == null) {
			jLabel_color = new JLabel();
			jLabel_color.setText(s);
		}
		return(jLabel_color);
	}

	private JComboBoxPanel getJCBX_scope(String s, int labelWidth, int comboboxWidth) {
		if ( jCBX_scope == null ) {
			jCBX_scope = new JComboBoxPanel(s, labelWidth, comboboxWidth, true);
			jCBX_scope.addItem(ITEM_SCOPEDISABLE);
			for ( int n=1; n<=HDDRecorder.SCOPEMAX; n++ ) {
				jCBX_scope.addItem(String.format("%d日 (%d週)",n,(n/7)+1));
			}
		}
		return jCBX_scope;
	}

	private JComboBoxPanel getJComboBox_TunerNum(String s, int labelWidth, int comboboxWidth) {
		if (jCBX_tunernum == null) {
			jCBX_tunernum = new JComboBoxPanel(s, labelWidth, comboboxWidth, true);
			for ( int n=1; n<=colorColumns.size(); n++ ) {
				jCBX_tunernum.addItem(String.valueOf(n));
			}
		}
		return jCBX_tunernum;
	}

	/**
	 * フリーオプション
	 */
	private JTextFieldWithPopup getJtf_opt() {
		if ( jtf_opt == null ) {
			jtf_opt = new JTextFieldWithPopup();
		}
		return jtf_opt;
	}

	/*
	 * レコーダテーブル
	 */

	private JScrollPane getJScrollPane_recorders() {
		if (jScrollPane_recorders == null) {
			jScrollPane_recorders = new JScrollPane();
			jScrollPane_recorders.setViewportView(getJTable_recorders());
			jScrollPane_recorders.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		}
		return(jScrollPane_recorders);
	}

	private RecorderTable getJTable_recorders() {
		if (jTable_recorders == null) {

			jTable_recorders = new RecorderTable(rowData);

			DefaultTableModel model = new DefaultTableModel();
			for ( RecorderColumn rc : RecorderColumn.values() ) {
				if ( rc.getIniWidth() >= 0 ) {
					model.addColumn(rc.getName());
				}
			}
			jTable_recorders.setModel(model);

			//
			jTable_recorders.setAutoResizeMode(JNETable.AUTO_RESIZE_OFF);
			jTable_recorders.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			// 各カラムの幅を設定する
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)jTable_recorders.getColumnModel();
			TableColumn column = null;
			for ( RecorderColumn rc : RecorderColumn.values() ) {
				if ( rc.getIniWidth() < 0 ) {
					continue;
				}
				column = columnModel.getColumn(rc.ordinal());
				column.setPreferredWidth(rc.getIniWidth());
			}

			//
			TableCellRenderer colorCellRenderer = new VWColorCellRenderer();

			for ( RecorderColumn rc : colorColumns ) {
				jTable_recorders.getColumn(rc.getName()).setCellRenderer(colorCellRenderer);
			}

			// 内容をコピーさせない
			jTable_recorders.addKeyListener(kl_cannnotcopy);

			// 行を選択したら入力に値を戻す
			jTable_recorders.addMouseListener(ml_tableSelected);
		}
		return jTable_recorders;
	}

	private JTextAreaWithPopup getJta_help() {
		if ( jta_help == null ) {
			jta_help = CommonSwingUtils.getJta(this,2,0);
			jta_help.append(TEXT_HINT);
		}
		return jta_help;
	}

	/*******************************************************************************
	 * 独自部品
	 ******************************************************************************/
	/**
	 *  テーブルの行データの構造
	 * @see RecorderColumn
	 */
	private class RecorderItem extends RowItem implements Cloneable {
		String name;
		String ipaddr;
		String portno;
		String macaddr;
		String broadcast;
		String recorderid;
		String user;
		String password;
		Boolean calendar;
		Integer scope;
		Integer tunernum;
		String c1;
		String c2;
		String c3;
		String c4;
		String c5;
		String c6;
		String c7;
		String c8;

		Boolean hide_chchange;

		@Override
		protected void myrefresh(RowItem o) {
			RecorderItem c = (RecorderItem) o;
			c.addData(name);
			c.addData(ipaddr);
			c.addData(portno);
			c.addData(macaddr);
			c.addData(broadcast);
			c.addData(recorderid);
			c.addData(user);
			c.addData(password);
			c.addData(calendar);
			c.addData(scope);
			c.addData(tunernum);
			c.addData(c1);
			c.addData(c2);
			c.addData(c3);
			c.addData(c4);
			c.addData(c5);
			c.addData(c6);
			c.addData(c7);
			c.addData(c8);
		}

		public String getCx(int n) {
			switch (n) {
			case 0:
				return c1;
			case 1:
				return c2;
			case 2:
				return c3;
			case 3:
				return c4;
			case 4:
				return c5;
			case 5:
				return c6;
			case 6:
				return c7;
			case 7:
				return c8;
			}
			return null;
		}

		public void setCx(int n, String c) {
			switch (n) {
			case 0:
				c1 = c;
				break;
			case 1:
				c2 = c;
				break;
			case 2:
				c3 = c;
				break;
			case 3:
				c4 = c;
				break;
			case 4:
				c5 = c;
				break;
			case 5:
				c6 = c;
				break;
			case 6:
				c7 = c;
				break;
			case 7:
				c8 = c;
				break;
			}
		}

		public RecorderItem clone() {
			return (RecorderItem) super.clone();
		}
	}

	private class RecorderTable extends JTable {

		private static final long serialVersionUID = 1L;

		private final Color evenColor = new Color(240,240,255);
		private final Color oddColor = super.getBackground();

		private RowItemList<RecorderItem> rowdata = null;

		public RecorderTable(RowItemList<RecorderItem> rowdata) {

			super();

			this.rowdata = rowdata;

			// フォントサイズ変更にあわせて行の高さを変える
			this.addPropertyChangeListener("font", new RowHeightChangeListener(8));

			// 行の高さの初期値の設定
			this.firePropertyChange("font", "old", "new");
		}

		@Override
		public Object getValueAt(int row, int column) {

			int vrow = this.convertRowIndexToModel(row);
			RecorderItem c = rowdata.get(vrow);
			if ( column == RecorderColumn.PW.getColumn() ) {
				return "ﾐﾝﾅﾆﾊﾅｲｼｮﾀﾞﾖ";
			}
			else if ( column == RecorderColumn.CALENDAR.getColumn() ) {
				return ((c.calendar)?(VALUE_CALENDAR_ENABLED):(""));
			}
			return (c.get(column) instanceof String)?(c.get(column)):(String.valueOf(c.get(column)));

		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			// 編集はないよ
		}

		@Override
		public int getRowCount() {

			return rowdata.size();

		}

		// 編集禁止
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {

			Component comp = super.prepareRenderer(tcr, row, column);

			if ( column >= RecorderColumn.C1.getColumn() ) {
				return comp;
			}

			//int vrow = this.convertRowIndexToModel(row);
			//RecorderItem c = rowdata.get(vrow);

			Color fg = null;
			Color bg = null;

			boolean evenline = (row%2 == 1);

			if ( isRowSelected(row) )
			{
				fg = this.getSelectionForeground();
				bg = this.getSelectionBackground();
			}

			if (fg==null) fg = this.getForeground();
			if (bg==null) bg = (evenline)?(evenColor):(oddColor);

			comp.setForeground(fg);
			comp.setBackground(bg);

			return comp;
		}

	}

}
