package tainavi;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/*
 * 検索履歴名称変更画面
 *
 */
public class VWSearchWordRenameDialog extends JEscCancelDialog{

	private static final long serialVersionUID = 1L;

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連

	private static final int PARTS_HEIGHT = 25;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 5;

	private static final int LABEL_WIDTH = 100;
	private static final int TEXT_WIDTH = 450;

	private static final int BUTTON_WIDTH = 120;

	private static final int PANEL_WIDTH = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH+TEXT_WIDTH+SEP_WIDTH*2+BUTTON_WIDTH+SEP_WIDTH;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private JPanel jPanel = null;

	private JLabel jLabel_name = null;
	private JTextField jTextField_name = null;
	private JLabel jLabel_message = null;

	private JButton jButton_ok = null;
	private JButton jButton_cancel = null;

	/*******************************************************************************
	 * 部品以外のインスタンスメンバー
	 ******************************************************************************/
	private SearchWordList swlist = null;
	private String name_edited = null;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	public VWSearchWordRenameDialog() {

		super();

		this.setModal(true);
		setContentPane(getJPanel());
		pack();
		setTitle("検索条件の名称変更");
		setResizable(false);
	}

	/*******************************************************************************
	 * 公開メソッド
	 ******************************************************************************/
	/*
	 * ダイアログを表示する
	 */
	public void open(String s, SearchWordList list, Component comp){
		swlist = list;

		jTextField_name.setText(s);

		this.setLocationRelativeTo(comp);

		setVisible(true);
	}

	/*
	 * 入力した名称を取得する
	 *
	 */
	public String getEditedName(){
		return name_edited;
	}

	/*
	 * ダイアログを位置決めする
	 */
	public void setPosition(int x, int y) {
		Rectangle r = this.getBounds();
		r.x = x;
		r.y = y;
		this.setBounds(r);
	}

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
	/*
	 * パネル全体
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();

			jPanel.setLayout(new SpringLayout());

			// １行目
			int y = SEP_HEIGHT;
			int x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_name("検索条件名称"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJTextField_name(), TEXT_WIDTH, PARTS_HEIGHT, x, y);
			x += TEXT_WIDTH + SEP_WIDTH*2;
			int xb = x;
			CommonSwingUtils.putComponentOn(jPanel, getJButton_ok("OK"), BUTTON_WIDTH, PARTS_HEIGHT, xb, y);
			y += PARTS_HEIGHT + SEP_HEIGHT;

			// ２行目
			x = SEP_WIDTH + LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_message(), TEXT_WIDTH, PARTS_HEIGHT, x, y);

			CommonSwingUtils.putComponentOn(jPanel, getJButton_cancel("キャンセル"), BUTTON_WIDTH, PARTS_HEIGHT, xb, y);
			y += PARTS_HEIGHT + SEP_HEIGHT;

			y += SEP_HEIGHT;

			jPanel.setPreferredSize(new Dimension(PANEL_WIDTH, y));
			jPanel.setBorder(new LineBorder(Color.BLACK, 1));
		}

		return jPanel;
	}

	/*
	 * 「キーワード名称」ラベル
	 */
	private JLabel getJLabel_name(String s) {
		if (jLabel_name == null) {
			jLabel_name = new JLabel(s);
		}
		return(jLabel_name);
	}

	/*
	 * 「キーワード名称」テキストフィールド
	 */
	private JTextField getJTextField_name() {
		if (jTextField_name == null) {
			jTextField_name = new JTextField();
			jTextField_name.addActionListener(al_search);
			jTextField_name.getDocument().addDocumentListener(dl_documentChanged);
		}

		return(jTextField_name);
	}

	/*
	 * 「メッセージ」ラベル
	 */
	private JLabel getJLabel_message() {
		if (jLabel_message == null) {
			jLabel_message = new JLabel("すでに使用されている名称です。");
			jLabel_message.setForeground(Color.RED);
		}
		return(jLabel_message);
	}

	/*
	 * 「OK」ボタン
	 */
	private JButton getJButton_ok(String s) {
		if (jButton_ok == null) {
			jButton_ok = new JButton(s);
			jButton_ok.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (jButton_ok.isEnabled())
						doRename();
				}
			});
		}

		return(jButton_ok);
	}

	/*
	 * 「キャンセル」ボタン
	 */
	private JButton getJButton_cancel(String s) {
		if (jButton_cancel == null) {
			jButton_cancel = new JButton(s);
			jButton_cancel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					doCancel();
				}
			});
		}

		return(jButton_cancel);
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/
	/*
	 * 履歴の名称を変更する
	 */
	protected void doRename(){
		name_edited = jTextField_name.getText();
		setVisible(false);
	}

	/*
	 * キャンセルする
	 *
	 * @see tainavi.JEscCancelDialog#doCancel()
	 */
	@Override
	protected void doCancel() {
		setVisible(false);
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/
	/*
	 * テキストフィールドでのEnterキー
	 */
	private final ActionListener al_search = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (jButton_ok.isEnabled())
				doRename();
		}
	};

	/**
	 * テキストフィールドでの文書変更
	 */
	private final DocumentListener dl_documentChanged = new DocumentListener(){
		@Override
		public void insertUpdate(DocumentEvent e) {
			updateControlStatus();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			updateControlStatus();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			updateControlStatus();
		}
	};

	/*******************************************************************************
	 * 内部関数
	 ******************************************************************************/
	/*
	 * 部品のステータスを更新する
	 */
	protected void updateControlStatus(){
		String name = jTextField_name.getText();
		boolean used = swlist.getItemFromLabel(name) != null;

		jLabel_message.setVisible(used);
		jButton_ok.setEnabled(name.length() > 0 && !used);
	}
}
