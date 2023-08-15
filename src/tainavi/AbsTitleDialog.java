package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;


/**
 * フォルダー作成画面クラス
 */
abstract class AbsTitleDialog extends JDialog {

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/
	protected abstract StatusWindow getStWin();
	protected abstract StatusTextArea getMWin();

	protected abstract HDDRecorder getSelectedRecorder();

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	private static final String MSGID = "[タイトル情報] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	// レイアウト関連

	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;

	private static final int PARTS_HEIGHT = 30;
	private static final int TEXT_HEIGHT = 30;
	private static final int RESERV_HEIGHT = 70;
	private static final int LIST_HEIGHT = 300;

	private static final int LABEL_WIDTH = 500;
	private static final int TEXT_WIDTH = 760;
	private static final int LIST_WIDTH = 300;
	private static final int CHAP_WIDTH = 450;

	private static final int BUTTON_WIDTH_S = 80;

	// その他の定数
	private static final int MAX_TITLE_LENGTH = 80;

	private String folderNameWorking = "";
	private HDDRecorder recorder = null;
	ArrayList<TextValueSet> tvsFolder = null;
	private final StatusWindow StWin = getStWin();			// これは起動時に作成されたまま変更されないオブジェクト
	private final StatusTextArea MWin = getMWin();			// これは起動時に作成されたまま変更されないオブジェクト

	/*
	 * チャプターの列定義
	 */
	public static enum ChapterColumn {
		NO			("番号",			 40),
		TITLE		("チャプター名",	300),
		DURATION	("時間",			 80),
		;

		private String name;
		private int iniWidth;

		private ChapterColumn(String name, int iniWidth) {
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

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JPanel jPanel = null;
	private JLabel jLabel_title = null;
	private JComboBoxWithPopup jComboBox_title = null;
	private JTextField jTextField_title = null;

	private JLabel jLabel_prog = null;
	private JTextAreaWithPopup jTextArea_prog = null;
	private JScrollPane jScrollPane_prog = null;

	private JLabel jLabel_folder = null;
	private JList jList_folder = null;
	private JButton jButton_selectAll = null;
	private JButton jButton_deselectAll = null;
	private JButton jButton_newFolder = null;

	private JLabel jLabel_chapter = null;
	private JScrollPane chappane = null;
	private ChapterTable chaptable = null;

	private JButton jButton_cancel = null;
	private JButton jButton_ok = null;

	// コンポーネント以外
	private boolean folderOnly = false;
	private TitleInfo info = null;
	private ProgDetailList prog = null;
	private ProgDetailList progSyobo = null;
	private TVProgramList tvprograms = null;

	private boolean reg = false;

	public TitleInfo getTitleInfo() { return info; }

	/*
	 * フォルダリストのセルレンダラー
	 */
	class FolderCellRenderer extends JCheckBox implements ListCellRenderer{
		public FolderCellRenderer() {
		}

		public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus){

			/* 項目の値を読み出して改めて表示する */
			JCheckBox checkBox = (JCheckBox)value;
			setText(checkBox.getText());
			setSelected(checkBox.isSelected());
			return this;
		}
    }

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public AbsTitleDialog(boolean b) {
		super();

		folderOnly = b;
		reg = false;

		this.setModal(true);
		this.setContentPane(getJPanel());

		// タイトルバーの高さも考慮する必要がある
		Dimension d = getJPanel().getPreferredSize();
		this.pack();
		this.setPreferredSize(new Dimension(d.width, d.height+this.getInsets().top));
		this.setResizable(false);
		this.setTitle("タイトル情報");
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 公開メソッド

	/**
	 *  フォルダーが登録されたかな？
	 */
	public boolean isRegistered() { return reg; }

	/*
	 * 画面をオープンする
	 * @param t 編集対象のタイトル情報
	 * @param tvs フォルダリスト
	 */
	public void open(TitleInfo t, ProgDetailList l, ProgDetailList ls, String otitle) {
		info = t;
		prog = l;
		progSyobo = ls;
		recorder = getSelectedRecorder();
		tvsFolder = recorder.getFolderList();

		updateTitleLabel();
		updateTitleInfo(t, prog, progSyobo, otitle);

		String device_name = "[" + t.getRec_device() + "]";

		DefaultListModel model = new DefaultListModel();

		for (TextValueSet ts : tvsFolder ){
			String folder_id = ts.getValue();
			String folder_name = ts.getText();
			if (folder_id.equals("0") || !folder_name.startsWith(device_name) )
				continue;
			JCheckBox box = new JCheckBox(ts.getText());

			if (t.containsFolder(ts.getValue()))
				box.setSelected(true);

			model.addElement(box);
		}

		jList_folder.setModel(model);

		updateFolderLabel();
	}

	/*
	 * タイトル情報を更新する
	 */
	private void updateTitleInfo(TitleInfo t, ProgDetailList pdl, ProgDetailList pdlSyobo, String titleOld){
		String title = t.getTitle();
		jTextField_title.setText(title);

		jComboBox_title.removeAllItems();
		jComboBox_title.addItem(title);
		jComboBox_title.setSelectedIndex(0);

		// <番組名> #<No>「<サブタイトル>」形式の候補を用意する
		if (pdl != null){
			String no = getProgramNo(title, pdl);
			String subtitle = getSubtitle(title, pdl);
			String ptitle = getProgramTitle(title);
			String ptitleOld = getProgramTitle(titleOld);

			String cand = formatTitleFromOld(ptitleOld, no, subtitle, titleOld);
			if (cand != null)
				jComboBox_title.addItem(cand);
			else{
				cand = formatDefaultTitle(ptitle, no, subtitle);
				if (cand != null)
					jComboBox_title.addItem(cand);
			}
		}

		if (pdlSyobo != null){
			String no = getProgramNo(title, pdlSyobo);
			String subtitle = getSubtitle(title, pdlSyobo);
			String ptitle = getProgramTitle(title);
			String ptitleOld = getProgramTitle(titleOld);

			String cand = formatTitleFromOld(ptitleOld, no, subtitle, titleOld);
			if (cand != null)
				jComboBox_title.addItem(cand);
			else{
				cand = formatDefaultTitle(ptitle, no, subtitle);
				if (cand != null)
					jComboBox_title.addItem(cand);
			}
		}

		// 番組情報が見つかったら、タイトルと詳細をセットする
		String prog = "";
		if (pdl != null){
			prog = pdl.prefix_mark + pdl.title + pdl.postfix_mark + "\r\n" + pdl.detail;
		}
		if (pdlSyobo != null){
			prog += "\r\n【しょぼかる】" + pdlSyobo.prefix_mark + pdlSyobo.title + pdlSyobo.postfix_mark + " " + pdlSyobo.detail;
		}

		jTextArea_prog.setText(prog);
		jTextArea_prog.setCaretPosition(0);
	}

	/*
	 * プログラムタイトルを取得する
	 */
	protected String getProgramTitle(String title){
		if (title == null)
			return null;

		String[] patterns = {
				"^(.*?)(#|＃|♯)",
				"^(.*?)第.*?(話|回|羽|夜)",
				"^(.*?)(\\(|（)[0-9０-９]+(\\)|）)"};

		for (String pat : patterns){
			Matcher m = Pattern.compile(pat).matcher(title);
			if (m.find()){
				return m.group(1);
			}
		}

		return null;
	}
	/*
	 * 話数を取得する
	 */
	protected String getProgramNo(String title, ProgDetailList pdl){
		String pno1 = searchProgramNo(title);
		if (pno1 != null)
			return pno1;

		String pno2 = searchProgramNo(pdl.title + " " + pdl.detail);
		if (pno2 != null)
			return pno2;

		return "";
	}

	/*
	 * 話数を抽出する
	 */
	protected String searchProgramNo(String title){
		if (title == null)
			return null;

		String[] patterns = {
				"(#|＃|♯)( |　){0,1}([0-9０-９]{1,3})",
				"(\\(|（)( |　){0,1}([0-9０-９]{1,3})(\\)|）){0,1}",
				"(第)( |　){0,1}([0-9０-９]{1,3})(話|回|羽|夜)"};
		String ntitle = Normalizer.normalize(title, Normalizer.Form.NFKC);

		for (String pat : patterns){
			Matcher m = Pattern.compile(pat).matcher(ntitle);
			if (m.find()){
				return m.group(3);
			}
		}

		return null;
	}

	/*
	 * デフォルトのタイトルを整形する
	 */
	protected String formatDefaultTitle(String ptitle, String no, String subtitle){
		if (ptitle == null || no == null || subtitle == null)
			return null;

		if (no.length() == 1){
			Matcher m = Pattern.compile("０-９").matcher(no);
			if (m.find())
				no = "０" + no;
			else
				no = "0" + no;
		}

		return ptitle + "#" + no + "「" + subtitle + "」";
	}

	/*
	 * 古いタイトルから新らしいタイトルの候補を整形する
	 */
	protected String formatTitleFromOld(String ptitle, String no, String subtitle, String otitle){
		if (ptitle == null)
			return null;

		String fno = formatProgramNo(no, otitle);
		String fst = formatSubtitle(subtitle, otitle);
		if (fno == null || fst == null)
			return null;

		return ptitle + fno + fst;
	}

	/*
	 * 古いタイトルから新しい話数を整形する
	 */
	protected String formatProgramNo(String no, String otitle){
		if (no == null || otitle == null)
			return null;

		String[] patterns = {
				"(#|＃)([0-9０-９]{1,3})",
				"(\\(|（)([0-9０-９]{1,3})(\\)|）)",
				"(第)([0-9０-９]{1,3})(話|回|羽|夜)"
			};

		for (String pat : patterns){
			Matcher m = Pattern.compile(pat).matcher(otitle);
			if (m.find()){
				String pre = m.group(1);
				String post = m.groupCount() > 2 ? m.group(3) : "";
				String pno = m.group(2);
				Matcher mw = Pattern.compile("(０-９)+").matcher(pno);
				if (mw.find()){
					no = CommonUtils.toZENNUM(no);
					if (no.length() == 1)
						no = "０" + no;
				}
				else{
					if (no.length() == 1)
						no = "0" + no;
				}

				return pre + no + post;
			}
		}

		return null;
	}

	/*
	 * 古いタイトルから新しいサブタイトルを整形する
	 */
	protected String formatSubtitle(String subtitle, String otitle){
		String[] patterns = {
				"(「)(.*?)(」)",
				"(『)(.*?)(』)",
				"(［)(.*?)(］)",
				"(【)(.*?)(】)",
				"(\\[)(.*?)(\\])"
			};

		for (String pat : patterns){
			Matcher m = Pattern.compile(pat).matcher(otitle);
			if (m.find())
				return m.group(1) + subtitle + m.group(3);
		}

		return null;
	}

	/*
	 * サブタイトルを取得する
	 */
	protected String getSubtitle(String title, ProgDetailList pdl){
		String title2 = pdl.title + " " + pdl.detail;
		String subc = searchSubtitleCombo(title2);
		if (subc != null)
			return subc;

		String sub1 = searchSubtitle(title2);
		if (sub1 != null)
			return sub1;

		return "";
	}

	/*
	 * サブタイトルを抽出する
	 */
	protected String searchSubtitleCombo(String title){
		String[] patternsCombo = {
				"(第|#|＃)([0-9０-９]{1,3})(話|回|羽|夜)?( |　)?「(.*?)」",
				"(第|#|＃)([0-9０-９]{1,3})(話|回|羽|夜)?( |　)?『(.*?)』",
				"(第|#|＃)([0-9０-９]{1,3})(話|回|羽|夜)?( |　)?【(.*?)】"};

		for (String pat : patternsCombo){
			Matcher m = Pattern.compile(pat).matcher(title);
			if (m.find())
				return m.group(5);
		}

		return null;
	}

	protected String searchSubtitle(String title){
		String[] patterns = {"「(.*?)」", "『(.*?)』", "【(.*?)】"};

		for (String pat : patterns){
			Matcher m = Pattern.compile(pat).matcher(title);
			if (m.find())
				return m.group(1);
		}

		return null;
	}

	/*
	 * フォルダ一覧のラベルを更新する。選択中のフォルダの数を表示する
	 */
	private void updateFolderLabel() {
		int count = getSelectedFolderCount();
		jLabel_folder.setText("フォルダ一覧（" + String.valueOf(count)+ "フォルダ選択中)");
	}

	/*
	 * すべてのフォルダを選択する
	 * @param b 選択するか選択解除するか
	 */
	private void selectAllFolders(boolean b) {
		ListModel model = jList_folder.getModel();

		int num = model.getSize();

		for (int n=0; n<num; n++){
			JCheckBox checkBox = (JCheckBox)model.getElementAt(n);
			checkBox.setSelected(b);
		}

		jList_folder.repaint();

		updateFolderLabel();
	}

	/*
	 * 選択されているフォルダの数を返す
	 */
	private int getSelectedFolderCount() {
		int count = 0;

		ListModel model = jList_folder.getModel();

		int num = model.getSize();

		for (int n=0; n<num; n++){
			JCheckBox checkBox = (JCheckBox)model.getElementAt(n);
			if (checkBox.isSelected()){
				count++;
			}
		}

		return count;
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * 「全選択」ボタンの処理
	 * フォルダーを全選択する
	 */
	private final ActionListener al_selectAll = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			selectAllFolders(true);
		}
	};

	/**
	 * 「選択解除」ボタンの処理
	 * フォルダーを選択解除する
	 */
	private final ActionListener al_deselectAll = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			selectAllFolders(false);
		}
	};

	/**
	 * 「新規」ボタンの処理
	 * フォルダーを新規作成する
	 */
	private final ActionListener al_newFolder = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			// 「指定なし」が選ばれている場合は「追加」とみなし、フォルダ名の初期値は番組タイトルとする
			// それ以外が選ばれている場合はそのフォルダの「変更」とみなし、フォルダ名の初期値は現在の値とする
			HDDRecorder rec = recorder;

			VWFolderDialog dlg = new VWFolderDialog();
			CommonSwingUtils.setLocationCenter(jPanel, dlg);

			String device_name = info.getRec_device();
			String device_id = text2value(rec.getDeviceList(), device_name);
			String title = jTextField_title.getText();

			String prefix = "[" + device_name + "] ";

			dlg.open(title);
			dlg.setVisible(true);

			if (!dlg.isRegistered())
				return;

			String nameNew = dlg.getFolderName();
			String action = "作成";
			folderNameWorking = nameNew;

			// フォルダー作成実行
			StWin.clear();
			new SwingBackgroundWorker(false) {
				@Override
				protected Object doWorks() throws Exception {
					StWin.appendMessage(MSGID+"フォルダーを" + action + "します："+folderNameWorking);

					boolean reg = rec.CreateRdFolder(device_id, nameNew);
					if (reg){
						MWin.appendMessage(MSGID+"フォルダーを正常に" + action + "できました："+folderNameWorking);

						String folder_name = prefix + nameNew;

						tvsFolder = rec.getFolderList();

						TextValueSet t = new TextValueSet();
						t.setText(folder_name);
						t.setValue(text2value(tvsFolder, folder_name));
						info.getRec_folder().add(t);

						DefaultListModel model = (DefaultListModel)jList_folder.getModel();
						JCheckBox box = new JCheckBox(folder_name);
						box.setSelected(true);
						model.addElement(box);

						updateFolderLabel();
					}
					else {
						MWin.appendError(ERRID+"フォルダーの" + action + "に失敗しました："+folderNameWorking);

						if ( ! rec.getErrmsg().equals("")) {
							MWin.appendMessage(MSGID+"[追加情報] "+rec.getErrmsg());
						}
					}

					return null;
				}
				@Override
				protected void doFinally() {
					StWin.setVisible(false);
				}
			}.execute();

			CommonSwingUtils.setLocationCenter(jPanel, (Component)StWin);
			StWin.setVisible(true);
		}
	};

	/*
	 * チャプター一覧の選択変更の処理
	 * 今は特に何もしない
	 */
	private final ListSelectionListener lsl_selected = new ListSelectionListener() {

		@Override
		public void valueChanged(ListSelectionEvent e) {
			System.out.println("lsl_selected "+e.toString());
			if ( ! e.getValueIsAdjusting() ){
				ListSelectionModel model = (ListSelectionModel) e.getSource();
				if ( ! model.isSelectionEmpty() ){
					int row = model.getMinSelectionIndex();
//					ChConvItem c = rowData.get(row);
//					jl_rel.setText(c.related);
				}
			}
		}

	};

	// セルが編集された
	private final CellEditorListener cel_edited = new CellEditorListener() {

		@Override
		public void editingStopped(ChangeEvent e) {
			System.out.println("cel_edited "+e.toString());
//			jbtn_update.setEnabled(true);
		}

		@Override
		public void editingCanceled(ChangeEvent e) {
			System.out.println("cel_edited "+e.toString());
		}

	};

	/**
	 * 「登録」ボタンの処理
	 * タイトルの編集結果を登録する
	 */
	private final ActionListener al_ok = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			registerData();
		}
	};

	private void registerData(){
		// タイトル
		String name = jTextField_title.getText();
		if (name.equals("")) {
			JOptionPane.showMessageDialog(jPanel, "タイトルがブランクです。");
			return;
		}

		String nameArib = AribCharMap.ConvStringToArib(name);
		int lenrb = nameArib.getBytes(Charset.forName("Shift_JIS")).length;
		if (!folderOnly && lenrb > MAX_TITLE_LENGTH){
			JOptionPane.showMessageDialog(jPanel, "タイトルが長すぎます。(" + String.valueOf(lenrb) + "バイト)");
			return;
		}

		info.setTitle(name);

		String device_name = "[" + info.getRec_device() + "]";
		ListModel model = jList_folder.getModel();

		// フォルダー
		ArrayList<TextValueSet> tvs = new ArrayList<TextValueSet>();
		int index = 0;
		ArrayList<TextValueSet> tvsFolder = recorder.getFolderList();
		for (TextValueSet ts : tvsFolder ){
			String folder_id = ts.getValue();
			String folder_name = ts.getText();
			if (folder_id.equals("0") || !folder_name.startsWith(device_name) )
				continue;

			JCheckBox checkBox = (JCheckBox)model.getElementAt(index);
			if (checkBox.isSelected()){
				TextValueSet t = new TextValueSet();
				t.setValue(ts.getValue());
				t.setText(ts.getText());
				tvs.add(t);
			}

			index++;
		}

		info.setRec_folder(tvs);

		// チャプター情報はイベント処理中に取得済
		ArrayList<ChapterInfo> chaps = info.getChapter();
		for (int nc=0; nc<chaps.size(); nc++){
			ChapterInfo ci = chaps.get(nc);

			String cname = ci.getName();
			String cnameSub = CommonUtils.substringrb(cname,80);
			if (!folderOnly && !cnameSub.equals(cname)){
				JOptionPane.showMessageDialog(jPanel, "チャプター名が長すぎます。(CHNO=" + String.valueOf(nc+1) + ")");
				return;
			}
		}

		reg = true;

		// ウィンドウを閉じる
		dispose();
	}

	/*
	 * タイトルラベルを更新する
	 */
	private void updateTitleLabel(){
		String name = jTextField_title.getText();
		String nameArib = AribCharMap.ConvStringToArib(name);
		int lenrb = nameArib.getBytes(Charset.forName("Shift_JIS")).length;
		int restrb = MAX_TITLE_LENGTH - lenrb;

		if (jLabel_title != null){
			if (restrb >= 0){
				jLabel_title.setText("タイトル名(残り" + String.valueOf(restrb) + "バイト)");
				jLabel_title.setForeground(Color.BLACK);
			}
			else{
				jLabel_title.setText("タイトル名(" + String.valueOf(-restrb) + "バイトオーバー)");
				jLabel_title.setForeground(Color.RED);
			}
		}
	}

	/**
	 * キャンセルしたい
	 */
	private final ActionListener al_cancel = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			dispose();
		}
	};

	/**
	 * 文書変更イベント処理
	 */
	private final DocumentListener dl_titleChanged = new DocumentListener(){
		@Override
		public void insertUpdate(DocumentEvent e) {
			updateTitleLabel();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateTitleLabel();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateTitleLabel();
		}
	};

	private final KeyListener kl_okcancel = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
		}
		@Override
		public void keyPressed(KeyEvent e) {
		}
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER){
				registerData();
			}
			else if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
				dispose();
			}
		}
	};

	private final KeyListener kl_cancel = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
		}
		@Override
		public void keyPressed(KeyEvent e) {
		}
		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
				dispose();
			}
		}
	};

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(null);

			int y = SEP_HEIGHT;
			int x = SEP_WIDTH;

			x = SEP_WIDTH;
			JLabel label = getJLabel_title("タイトル名(最大80バイトまで)");
			label.setBounds(x, y, LABEL_WIDTH, PARTS_HEIGHT);
			jPanel.add(label);

			y += PARTS_HEIGHT;
			JComboBoxWithPopup title = getJComboBox_title();
			title.setBounds(x, y, TEXT_WIDTH, TEXT_HEIGHT);
			jPanel.add(title);
			if (folderOnly)
				jTextField_title.disable();

			y += TEXT_HEIGHT;
			label = getJLabel_prog("番組情報");
			label.setBounds(x, y, LABEL_WIDTH, PARTS_HEIGHT);
			jPanel.add(label);

			y += PARTS_HEIGHT;
			JScrollPane area = getJScrollPane_prog();
			area.setBounds(x, y, TEXT_WIDTH, RESERV_HEIGHT);
			jPanel.add(area);
			if (folderOnly)
				area.disable();

			y += RESERV_HEIGHT;
			label = getJLabel_folder("フォルダ一覧");
			label.setBounds(x, y, LABEL_WIDTH, PARTS_HEIGHT);
			jPanel.add(label);

			int xc = x+LIST_WIDTH + SEP_WIDTH;
			label = getJLabel_chapter("チャプター一覧");
			label.setBounds(xc, y, LABEL_WIDTH, PARTS_HEIGHT);
			jPanel.add(label);

			y += PARTS_HEIGHT;
			JScrollPane list = getJList_folder();
			list.setBounds(x, y, LIST_WIDTH, LIST_HEIGHT);
			jPanel.add(list);

			JScrollPane chap = getChapterPane();
			chap.setBounds(xc, y, CHAP_WIDTH, LIST_HEIGHT);
			jPanel.add(chap);
			if (folderOnly)
				chap.disable();

			y+= LIST_HEIGHT + SEP_HEIGHT;
			JButton button = getJButton_selectAll("全選択");
			button.setBounds(x, y, BUTTON_WIDTH_S, PARTS_HEIGHT);
			jPanel.add(button);

			button = getJButton_deselectAll("選択解除");
			button.setBounds(x+BUTTON_WIDTH_S+SEP_WIDTH, y, BUTTON_WIDTH_S, PARTS_HEIGHT);
			jPanel.add(button);

			button = getJButton_newFolder("新規");
			button.setBounds(x+LIST_WIDTH-BUTTON_WIDTH_S, y, BUTTON_WIDTH_S, PARTS_HEIGHT);
			jPanel.add(button);

			x += TEXT_WIDTH - (BUTTON_WIDTH_S*2 + SEP_WIDTH);
			JButton btnCreate = getJButton_ok("登録");
			btnCreate.setBounds(x, y, BUTTON_WIDTH_S, PARTS_HEIGHT);
			jPanel.add(btnCreate);

			x += BUTTON_WIDTH_S+SEP_WIDTH;
			JButton btnCancel = getJButton_cancel("ｷｬﾝｾﾙ");
			btnCancel.setBounds(x, y, BUTTON_WIDTH_S, PARTS_HEIGHT);
			jPanel.add(btnCancel);

			x += BUTTON_WIDTH_S+SEP_WIDTH;
			y += PARTS_HEIGHT+SEP_HEIGHT;

			jPanel.setPreferredSize(new Dimension(x, y));
		}

		return jPanel;
	}

	//
	private JLabel getJLabel_title(String s) {
		if (jLabel_title == null) {
			jLabel_title = new JLabel(s);
		}
		return(jLabel_title);
	}

	//
	private JComboBoxWithPopup getJComboBox_title() {
		if (jComboBox_title == null) {
			jComboBox_title = new JComboBoxWithPopup();
			jComboBox_title.setEditable(true);

		}
		if (jTextField_title == null) {
			jTextField_title = ((JTextField)jComboBox_title.getEditor().getEditorComponent());
			jTextField_title.addActionListener(al_ok);
			jTextField_title.addKeyListener(kl_cancel);
			jTextField_title.getDocument().addDocumentListener(dl_titleChanged);
		}
		return(jComboBox_title);
	}

	//
	private JLabel getJLabel_prog(String s) {
		if (jLabel_prog == null) {
			jLabel_prog = new JLabel(s);
		}
		return(jLabel_prog);
	}

	//
	private JScrollPane getJScrollPane_prog() {
		if (jScrollPane_prog == null) {
			jScrollPane_prog = new JScrollPane(getJTextArea_prog(),JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

			jScrollPane_prog.setBorder(jComboBox_title.getBorder());
		}

		return(jScrollPane_prog);
	}

	private JTextAreaWithPopup getJTextArea_prog() {
		if (jTextArea_prog == null) {
			jTextArea_prog = new JTextAreaWithPopup();
			jTextArea_prog.setLineWrap(true);
			jTextArea_prog.addKeyListener(kl_cancel);
//			jTextArea_prog.setBorder(jComboBox_title.getBorder());
			jTextArea_prog.setEditable(false);
		}
		return(jTextArea_prog);
	}

	private JLabel getJLabel_folder(String s) {
		if (jLabel_folder == null) {
			jLabel_folder = new JLabel(s);
		}
		return(jLabel_folder);
	}

	//
	private JScrollPane getJList_folder() {
		jList_folder = new JList();

		FolderCellRenderer renderer = new FolderCellRenderer();
		jList_folder.setCellRenderer(renderer);
	    jList_folder.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                /* クリックされた座標からIndex番号を取り出す */
                Point p = e.getPoint();
                int index = jList_folder.locationToIndex(p);

                JCheckBox checkBox = (JCheckBox)jList_folder.getModel().getElementAt(index);
                if (checkBox.isSelected()){
                	checkBox.setSelected(false);
                }else{
                  checkBox.setSelected(true);
                }

				updateFolderLabel();

                /* 再描画してみる */
                jList_folder.repaint();
            }
            @Override
            public void mousePressed(MouseEvent e) {
            }
            @Override
            public void mouseReleased(MouseEvent e) {
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
	    });
		jList_folder.addKeyListener(kl_okcancel);

		JScrollPane sp = new JScrollPane();
		sp.getViewport().setView(jList_folder);

		return(sp);
	}

	//
	private JButton getJButton_selectAll(String s) {
		if (jButton_selectAll == null) {
			jButton_selectAll = new JButton();
			jButton_selectAll.setText(s);

			jButton_selectAll.addActionListener(al_selectAll);
			jButton_selectAll.addKeyListener(kl_cancel);
		}
		return(jButton_selectAll);
	}

	//
	private JButton getJButton_deselectAll(String s) {
		if (jButton_deselectAll == null) {
			jButton_deselectAll = new JButton();
			jButton_deselectAll.setText(s);

			jButton_deselectAll.addActionListener(al_deselectAll);
			jButton_deselectAll.addKeyListener(kl_cancel);
		}
		return(jButton_deselectAll);
	}

	private JButton getJButton_newFolder(String s) {
		if (jButton_newFolder == null) {
			jButton_newFolder = new JButton();
			jButton_newFolder.setText(s);

			jButton_newFolder.addActionListener(al_newFolder);
			jButton_newFolder.addKeyListener(kl_cancel);
		}
		return(jButton_newFolder);
	}

	private JLabel getJLabel_chapter(String s) {
		if (jLabel_chapter == null) {
			jLabel_chapter = new JLabel(s);
		}
		return(jLabel_chapter);
	}

	private JScrollPane getChapterPane() {
		if (chappane == null ) {
			chappane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			chappane.setViewportView(getChapterTable());
		}

		return chappane;
	}

	private ChapterTable getChapterTable() {
		if (chaptable == null) {
			// カラム名の初期化
			ArrayList<String> cola = new ArrayList<String>();
			for ( ChapterColumn lc : ChapterColumn.values() ) {
				cola.add(lc.getName());
			}
			final String[] colname = cola.toArray(new String[0]);

			//　テーブルの基本的な設定
			DefaultTableModel model = new DefaultTableModel(colname, 0);

			chaptable = new ChapterTable(model);

			// 各カラムの幅を設定する
			DefaultTableColumnModel columnModel = (DefaultTableColumnModel)chaptable.getColumnModel();
			TableColumn column = null;
			for ( ChapterColumn lc : ChapterColumn.values() ) {
				column = columnModel.getColumn(lc.ordinal());
				column.setPreferredWidth(lc.getIniWidth());
			}

			chaptable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			chaptable.getTableHeader().setReorderingAllowed(false);
			chaptable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			chaptable.putClientProperty("terminateEditOnFocusLost", true);
			chaptable.setRowHeight(chaptable.getRowHeight()+12);
			chaptable.getSelectionModel().addListSelectionListener(lsl_selected);

			// 編集セルにリスナーを付ける
			TableColumn tc = chaptable.getColumn(ChapterColumn.TITLE.getName());
			EditorColumn ec = new EditorColumn();
			ec.addCellEditorListener(cel_edited);
			tc.setCellEditor(ec);
		}

		return chaptable;
	}

	//
	private JButton getJButton_ok(String s) {
		if (jButton_ok == null) {
			jButton_ok = new JButton();
			jButton_ok.setText(s);

			jButton_ok.addActionListener(al_ok);
			jButton_ok.addKeyListener(kl_cancel);
		}
		return(jButton_ok);
	}

	//
	private JButton getJButton_cancel(String s) {
		if (jButton_cancel == null) {
			jButton_cancel = new JButton();
			jButton_cancel.setText(s);

			jButton_cancel.addActionListener(al_cancel);
			jButton_cancel.addKeyListener(kl_cancel);
		}
		return jButton_cancel;
	}

	/*******************************************************************************
	 * 独自コンポーネント
	 ******************************************************************************/
	private class ChapterTable extends JTable {

		private static final long serialVersionUID = 1L;

		public ChapterTable(DefaultTableModel model) {
			this.setModel(model);
		}

		@Override
		public Object getValueAt(int row, int column) {
			if ( column == ChapterColumn.NO.ordinal() ) {
				return String.valueOf(row+1);
			}
			else if ( column == ChapterColumn.TITLE.ordinal() ) {
				return info.getChapter().get(row).getName();
			}
			else if ( column == ChapterColumn.DURATION.ordinal() ) {
				int duration = info.getChapter().get(row).getDuration();
				return String.format("%02d:%02d:%02d", duration/3600, (duration/60)%60, duration%60);
			}
			return null;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if ( column == ChapterColumn.TITLE.ordinal() ){
				info.getChapter().get(row).setName((String)aValue);
			}
		}

		@Override
		public int getRowCount() {
			if (info == null)
				return 0;
			return info.getChapter().size();
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			if (folderOnly)
				return false;

			if (column == 1) {
				return true;
			}
			else {
				return false;
			}
		}

		@Override
		public Component prepareRenderer(TableCellRenderer tcr, int row, int column) {
			Component comp = super.prepareRenderer(tcr, row, column);
			return comp;
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
}
