package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.charset.Charset;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;


/**
 * フォルダー作成画面クラス
 */
public class VWFolderDialog extends JDialog {

	/*******************************************************************************
	 * 抽象メソッド
	 ******************************************************************************/

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連

	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 10;

	private static final int PARTS_HEIGHT = 30;
	private static final int TEXT_HEIGHT = 30;

	private static final int LABEL_WIDTH = 500;
	private static final int TEXT_WIDTH = 500;
	private static final int BUTTON_WIDTH_S = 75;

	private static final int MAX_FOLDER_LENGTH = 80;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// コンポーネント

	private JPanel jPanel = null;
	private JLabel jLabel_folder = null;
	private JTextFieldWithPopup jTextField_folder = null;
	private JButton jButton_cancel = null;
	private JButton jButton_create = null;

	// コンポーネント以外
	private boolean reg = false;

	// 入力したフォルダ名
	public String getFolderName(){return jTextField_folder.getText(); 	}

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public VWFolderDialog() {
		super();

		reg = false;

		this.setModal(true);
		this.setContentPane(getJPanel());

		// タイトルバーの高さも考慮する必要がある
		Dimension d = getJPanel().getPreferredSize();
		this.pack();
		this.setPreferredSize(new Dimension(d.width, d.height+this.getInsets().top));
		this.setResizable(false);
		this.setTitle("フォルダ名称");
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/

	// 公開メソッド

	/**
	 *  フォルダーが登録されたかな？
	 */
	public boolean isRegistered() { return reg; }

	// オープン
	public void open(String name) {
		jTextField_folder.setText(name);
		updateFolderLabel();
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

	/**
	 * フォルダーを登録する
	 */
	private final ActionListener al_create = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			registerData();
		}
	};

	private void registerData(){
		String name = jTextField_folder.getText();
		if (name.equals("")) {
			JOptionPane.showMessageDialog(jPanel, "フォルダ名がブランクです。");
			return;
		}

		int lenrb = name.getBytes(Charset.forName("Shift_JIS")).length;
		if (lenrb > MAX_FOLDER_LENGTH){
			JOptionPane.showMessageDialog(jPanel, "フォルダ名が長すぎます。(" + String.valueOf(lenrb) + "バイト)");
			return;
		}

		reg = true;

		// ウィンドウを閉じる
		dispose();
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
	 * キー入力イベント処理
	 */
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

	/**
	 * 文書変更イベント処理
	 */
	private final DocumentListener dl_folderChanged = new DocumentListener(){
		@Override
		public void insertUpdate(DocumentEvent e) {
			updateFolderLabel();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateFolderLabel();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateFolderLabel();
		}
	};

	/*
	 * タイトルラベルを更新する
	 */
	private void updateFolderLabel(){
		String name = jTextField_folder.getText();
		int lenrb = name.getBytes(Charset.forName("Shift_JIS")).length;
		int restrb = MAX_FOLDER_LENGTH - lenrb;

		if (jLabel_folder != null){
			if (restrb >= 0){
				jLabel_folder.setText("フォルダ名(残り" + String.valueOf(restrb) + "バイト)");
				jLabel_folder.setForeground(Color.BLACK);
			}
			else{
				jLabel_folder.setText("フォルダ名(" + String.valueOf(-restrb) + "バイトオーバー)");
				jLabel_folder.setForeground(Color.RED);
			}
		}
	}

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
			JLabel label = getJLabel_folder("フォルダ名(最大80バイトまで)");
			label.setBounds(x, y, LABEL_WIDTH, PARTS_HEIGHT);
			jPanel.add(label);

			y += PARTS_HEIGHT;
			JTextField field = getJTextField_folder();
			field.setBounds(x, y, TEXT_WIDTH, TEXT_HEIGHT);
			jPanel.add(field);

			y += TEXT_HEIGHT + SEP_HEIGHT;
			x += TEXT_WIDTH - (BUTTON_WIDTH_S*2 + SEP_WIDTH);
			JButton btnCreate = getJButton_create("登録");
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
	private JLabel getJLabel_folder(String s) {
		if (jLabel_folder == null) {
			jLabel_folder = new JLabel(s);
		}
		return(jLabel_folder);
	}

	//
	private JTextField getJTextField_folder() {
		if (jTextField_folder == null) {
			jTextField_folder = new JTextFieldWithPopup();
			jTextField_folder.addActionListener(al_create);
			jTextField_folder.addKeyListener(kl_cancel);
			jTextField_folder.getDocument().addDocumentListener(dl_folderChanged);
		}
		return(jTextField_folder);
	}

	//
	private JButton getJButton_create(String s) {
		if (jButton_create == null) {
			jButton_create = new JButton();
			jButton_create.setText(s);

			jButton_create.addActionListener(al_create);
			jButton_create.addKeyListener(kl_cancel);
		}
		return(jButton_create);
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
}
