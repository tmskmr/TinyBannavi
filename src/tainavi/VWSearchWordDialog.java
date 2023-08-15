package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import tainavi.TVProgram.ProgGenre;
import tainavi.TVProgram.ProgSubgenre;

/*
 * 検索文字列入力画面
 *
 */
public class VWSearchWordDialog extends JEscCancelDialog{

	private static final long serialVersionUID = 1L;

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	// レイアウト関連
	private static final int PARTS_HEIGHT = 25;
	private static final int SEP_WIDTH = 10;
	private static final int SEP_HEIGHT = 5;
	private static final int SEARCH_HEIGHT = 50;
	private static final int CLEAR_HEIGHT = 35;

	private static final int LABEL_WIDTH = 100;

	private static final int TEXT_WIDTH = 450;
	private static final int CHECKBOX_WIDTH = 150;
	private static final int DATE_WIDTH = 140;
	private static final int SAME_WIDTH = 60;
	private static final int AMONG_WIDTH = 30;
	private static final int CHANNEL_WIDTH = 450;
	private static final int GENRE_WIDTH = 210;
	private static final int SUBGENRE_WIDTH = 230;
	private static final int LEN_WIDTH = 90;
	private static final int FLAG_WIDTH = 60;

	private static final int BUTTON_WIDTH = 120;

	private static final int PANEL_WIDTH = SEP_WIDTH+LABEL_WIDTH+SEP_WIDTH+TEXT_WIDTH+SEP_WIDTH*2+BUTTON_WIDTH+SEP_WIDTH;

	// その他
	private static final int MAX_SEARCH_WORDS = 32;

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	private JPanel jPanel = null;

	private JLabel jLabel_swlist = null;
	private JSearchWordComboBox jComboBox_swlist = null;
//	private JTextField jTextField_swlist = null;

	private JLabel jLabel_keyword = null;
	private JTextFieldWithPopup jTextField_keyword = null;

	private JLabel jLabel_options = null;
	private JCheckBox jCheckBox_title = null;
	private JCheckBox jCheckBox_detail = null;
	private JCheckBox jCheckBox_filter = null;

	private JLabel jLabel_period = null;
	private JDateField jDateField_from = null;
	private JLabel jLabel_among = null;
	private JDateField jDateField_to = null;
	private JButton jButton_same = null;

	private JButton jButton_rename = null;
	private JButton jButton_delete = null;
	private JButton jButton_search = null;
	private JButton jButton_clear = null;

	private JLabel jLabel_channel = null;
	private JComboBoxWithPopup jComboBox_channel = null;

	private JLabel jLabel_genre = null;
	private JComboBoxWithPopup jComboBox_genre = null;
	private JComboBoxWithPopup jComboBox_subgenre = null;

	private JLabel jLabel_length = null;
	private JComboBoxWithPopup jComboBox_lenfrom = null;
	private JTextField jTextField_lenfrom = null;
	private JLabel jLabel_lenamong = null;
	private JComboBoxWithPopup jComboBox_lento = null;
	private JTextField jTextField_lento = null;

	private JLabel jLabel_flag = null;
	private JCheckBox jCheckBox_new = null;
	private JCheckBox jCheckBox_final = null;
	private JCheckBox jCheckBox_repeat = null;
	private JCheckBox jCheckBox_first = null;
	private JCheckBox jCheckBox_special = null;

	private JButton jButton_menu = null;
	private AbsToolBar jToolbar = null;

	/*******************************************************************************
	 * 部品以外のインスタンスメンバー
	 ******************************************************************************/
	private Instant last_shown = null;
	private SearchWordList swlist = null;
	private boolean autoclose_enabled = true;
	private ChannelSort chsort = null;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	public VWSearchWordDialog(ChannelSort chs, SearchWordList list) {

		super();

		chsort = chs;
		swlist = list;

//		this.setModal(true);
		addWindowListener(wl_panel);
		setContentPane(getJPanel());
		setUndecorated(true);
		pack();
		setTitle("検索条件の編集");
		setResizable(false);
	}

	/*******************************************************************************
	 * 公開メソッド
	 ******************************************************************************/
	/*
	 * ダイアログを表示する
	 */
	public void open(AbsToolBar jtoolbar, JButton jbutton, String s, SearchWordItem swi){
		jToolbar = jtoolbar;
		jButton_menu = jbutton;

		Instant now = Instant.now();
		if (last_shown != null && now.minusMillis(100).isBefore(last_shown))
			return;

		// 放送日は７日先まで入力可とする
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, 7);
		jDateField_from.setMaxDate(cal);
		jDateField_to.setMaxDate(cal);

		// 検索履歴を更新する
		updateKeywordComboBox();
		if (s != null){
			if (s.isEmpty()){
//				doClear();
			}
			else{
				jComboBox_swlist.setSelectedItem(s);
				decodeKeyword(s, swi);
			}
		}

		updateControlStatus();
		setVisible(true);
	}

	/*
	 * 表示・非表示を切り替える
	 *
	 * @see java.awt.Dialog#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean b) {
		if (b){
			setLocation();
			jTextField_keyword.requestFocusInWindow();
		}
		else{
			// 画面を閉じる時のインスタントを取得する
			last_shown = Instant.now();
		}

		super.setVisible(b);
	}

	/*
	 * ダイアログを▼ボタンの下、ツールバーの左端に位置決めする
	 */
	public void setLocation(){
		Point pf = jToolbar.getLocationOnScreen();
		Point pm = jButton_menu.getLocationOnScreen();
		Rectangle rm = jButton_menu.getBounds();

		setPosition(pf.x, pm.y + rm.height);
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
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_swlist("検索履歴"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_swlist(), TEXT_WIDTH, PARTS_HEIGHT, x, y);
			x += TEXT_WIDTH + SEP_WIDTH*2;
			int xb = x;
			CommonSwingUtils.putComponentOn(jPanel, getJButton_rename("名称変更"), BUTTON_WIDTH, PARTS_HEIGHT, xb, y);

			// ２行目
			y += PARTS_HEIGHT + SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_keyword("キーワード"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJTextField_keyword(), TEXT_WIDTH, PARTS_HEIGHT, x, y);
			CommonSwingUtils.putComponentOn(jPanel, getJButton_delete("削除"), BUTTON_WIDTH, PARTS_HEIGHT, xb, y);

			// ３行目
			y += PARTS_HEIGHT + SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_options("オプション"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_title("番組名一致"), CHECKBOX_WIDTH, PARTS_HEIGHT, x, y);
			x += CHECKBOX_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_detail("番組詳細一致"), CHECKBOX_WIDTH, PARTS_HEIGHT, x, y);
			x += CHECKBOX_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_filter("絞り込み"), CHECKBOX_WIDTH, PARTS_HEIGHT, x, y);
			x += SEP_WIDTH;

			// ４行目
			y += PARTS_HEIGHT + SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_period("放送日"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJDateField_from(), DATE_WIDTH, PARTS_HEIGHT, x, y);
			x += DATE_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_among("～"), AMONG_WIDTH, PARTS_HEIGHT, x, y);
			x += AMONG_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJDateField_to(), DATE_WIDTH, PARTS_HEIGHT, x, y);
			x += DATE_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJButton_same("同値"), SAME_WIDTH, PARTS_HEIGHT, x, y);
			x += BUTTON_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJButton_search("検索"), BUTTON_WIDTH, SEARCH_HEIGHT, xb, y);

			// ５行目
			y += PARTS_HEIGHT + SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_channel("チャンネル名"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_channel(), CHANNEL_WIDTH, PARTS_HEIGHT, x, y);

			// ６行目
			y += PARTS_HEIGHT + SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_genre("ジャンル/サブ"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_genre(), GENRE_WIDTH, PARTS_HEIGHT, x, y);
			x += GENRE_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_subgenre(), SUBGENRE_WIDTH, PARTS_HEIGHT, x, y);

			// ７行目
			y += PARTS_HEIGHT + SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_length("番組長(分)"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_lenfrom(), LEN_WIDTH, PARTS_HEIGHT, x, y);
			x += LEN_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_lenamong("～"), AMONG_WIDTH, PARTS_HEIGHT, x, y);
			x += AMONG_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJComboBox_lento(), LEN_WIDTH, PARTS_HEIGHT, x, y);
			x += LEN_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJButton_clear("クリア"), BUTTON_WIDTH, CLEAR_HEIGHT, xb, y);

			// ８行目
			y += PARTS_HEIGHT+SEP_HEIGHT;
			x = SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJLabel_flag("フラグ"), LABEL_WIDTH, PARTS_HEIGHT, x, y);
			x += LABEL_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_new("新"), FLAG_WIDTH, PARTS_HEIGHT, x, y);
			x += FLAG_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_final("終"), FLAG_WIDTH, PARTS_HEIGHT, x, y);
			x += FLAG_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_repeat("再"), FLAG_WIDTH, PARTS_HEIGHT, x, y);
			x += FLAG_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_first("初"), FLAG_WIDTH, PARTS_HEIGHT, x, y);
			x += FLAG_WIDTH + SEP_WIDTH;
			CommonSwingUtils.putComponentOn(jPanel, getJCheckBox_special("特"), FLAG_WIDTH, PARTS_HEIGHT, x, y);
			x += FLAG_WIDTH + SEP_WIDTH;

			y += PARTS_HEIGHT+SEP_HEIGHT*2;

			jPanel.setPreferredSize(new Dimension(PANEL_WIDTH, y));
			jPanel.setBorder(new LineBorder(Color.BLACK, 1));
		}

		return jPanel;
	}

	/*
	 * 「検索履歴」ラベル
	 */
	private JLabel getJLabel_swlist(String s) {
		if (jLabel_swlist == null) {
			jLabel_swlist = new JLabel(s);
		}
		return(jLabel_swlist);
	}

	/*
	 * 「検索履歴」コンボボックス
	 */
	@SuppressWarnings("unchecked")
	private JSearchWordComboBox getJComboBox_swlist() {
		if (jComboBox_swlist == null) {
			jComboBox_swlist = new JSearchWordComboBox(swlist);
			jComboBox_swlist.addItemListener(il_swlistSelected);
		}

		return(jComboBox_swlist);
	}

	/*
	 * 「名称変更」ボタン
	 */
	private JButton getJButton_rename(String s) {
		if (jButton_rename == null) {
			jButton_rename = new JButton();
			jButton_rename.setText(s);

			jButton_rename.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					doRename();
					jTextField_keyword.requestFocusInWindow();
					updateControlStatus();
				}
			});
		}
		return(jButton_rename);
	}

	/*
	 * 「削除」ボタン
	 */
	private JButton getJButton_delete(String s) {
		if (jButton_delete == null) {
			jButton_delete = new JButton();
			jButton_delete.setText(s);
			jButton_delete.setForeground(Color.RED);

			jButton_delete.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					doDelete();
					jTextField_keyword.requestFocusInWindow();
					updateControlStatus();
				}
			});
		}
		return(jButton_delete);
	}

	/*
	 * 「キーワード」ラベル
	 */
	private JLabel getJLabel_keyword(String s) {
		if (jLabel_keyword == null) {
			jLabel_keyword = new JLabel(s);
		}
		return(jLabel_keyword);
	}

	/*
	 * 「キーワード」テキストフィールド
	 */
	private JTextFieldWithPopup getJTextField_keyword() {
		if (jTextField_keyword == null) {
			jTextField_keyword = new JTextFieldWithPopup();
			jTextField_keyword.addActionListener(al_search);
			jTextField_keyword.getDocument().addDocumentListener(dl_documentChanged);
		}

		return(jTextField_keyword);
	}

	/*
	 * 「検索」ボタン
	 */
	private JButton getJButton_search(String s) {
		if (jButton_search == null) {
			jButton_search = new JButton();
			jButton_search.setText(s);

			Font f = jButton_search.getFont();
			jButton_search.setFont(f.deriveFont(f.getStyle() | Font.BOLD, f.getSize()));

			jButton_search.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (jButton_search.isEnabled())
						doSearch();
				}
			});
		}
		return(jButton_search);
	}

	/*
	 * 「オプション」ラベル
	 */
	private JLabel getJLabel_options(String s) {
		if (jLabel_options == null) {
			jLabel_options = new JLabel(s);
		}
		return(jLabel_options);
	}

	/*
	 * 「番組名一致」チェックボックス
	 */
	private JCheckBox getJCheckBox_title(String s) {
		if (jCheckBox_title == null) {
			jCheckBox_title = new JCheckBox();
			jCheckBox_title.setText(s);
			jCheckBox_title.setSelected(true);
		}
		return(jCheckBox_title);
	}

	/*
	 * 「番組詳細一致」チェックボックス
	 */
	private JCheckBox getJCheckBox_detail(String s) {
		if (jCheckBox_detail == null) {
			jCheckBox_detail = new JCheckBox();
			jCheckBox_detail.setText(s);
			jCheckBox_detail.setSelected(true);
		}
		return(jCheckBox_detail);
	}

	/*
	 * 「絞り込み」チェックボックス
	 */
	private JCheckBox getJCheckBox_filter(String s) {
		if (jCheckBox_filter == null) {
			jCheckBox_filter = new JCheckBox();
			jCheckBox_filter.setText(s);
		}
		return(jCheckBox_filter);
	}

	/*
	 * 「過去ログ期間」ラベル
	 */
	private JLabel getJLabel_period(String s) {
		if (jLabel_period == null) {
			jLabel_period = new JLabel(s);
		}
		return(jLabel_period);
	}

	/*
	 * 「過去ログ期間開始」テキストフィールド
	 */
	private JDateField getJDateField_from() {
		if (jDateField_from == null) {
			String tt = "<html>YYYY/MM/DDかMM/DDの形式で指定してください。<br>YYYY/MM/DDで開始日のみ指定するとその日の過去ログが閲覧できます。</html>";
			jDateField_from = createDateField(tt);
		}

		return(jDateField_from);
	}

	/*
	 * 「～」ラベル
	 */
	private JLabel getJLabel_among(String s) {
		if (jLabel_among == null) {
			jLabel_among = new JLabel(s);
			jLabel_among.setHorizontalAlignment(JLabel.CENTER);
		}
		return(jLabel_among);
	}

	/*
	 * 「過去ログ期間終了」テキストフィールド
	 */
	private JDateField getJDateField_to() {
		if (jDateField_to == null) {
			String tt = "YYYY/MM/DDかMM/DDの形式で指定してください。";
			jDateField_to = createDateField(tt);
		}

		return(jDateField_to);
	}

	/*
	 * 「同日」ボタン
	 */
	private JButton getJButton_same(String s) {
		if (jButton_same == null) {
			jButton_same = new JButton();
			jButton_same.setText(s);

			jButton_same.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					jDateField_to.setText(jDateField_from.getText());
				}
			});
		}
		return(jButton_same);
	}

	/*
	 * 「クリア」ボタン
	 */
	private JButton getJButton_clear(String s) {
		if (jButton_clear == null) {
			jButton_clear = new JButton();
			jButton_clear.setText(s);

			jButton_clear.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					doClear();
					jComboBox_swlist.setSelectedItem("");
					jTextField_keyword.requestFocusInWindow();
				}
			});
		}
		return(jButton_clear);
	}

	/*
	 * 「チャンネル」ラベル
	 */
	private JLabel getJLabel_channel(String s) {
		if (jLabel_channel == null) {
			jLabel_channel = new JLabel(s);
		}
		return(jLabel_channel);
	}

	/*
	 * 「チャンネル」コンボボックス
	 */
	private JComboBoxWithPopup getJComboBox_channel() {
		if (jComboBox_channel == null) {
			jComboBox_channel = new JComboBoxWithPopup();
			jComboBox_channel.setMaximumRowCount(32);

			jComboBox_channel.addItem("");

			List<Center> list = chsort.getClst();
			for (Center c : list){
				jComboBox_channel.addItem(c.getCenter());
			}
			jComboBox_channel.addItemListener(il_channelSelected);
		}

		return(jComboBox_channel);
	}

	/*
	 * 「ジャンル」ラベル
	 */
	private JLabel getJLabel_genre(String s) {
		if (jLabel_genre == null) {
			jLabel_genre = new JLabel(s);
		}
		return(jLabel_genre);
	}

	/*
	 * 「ジャンル」コンボボックス
	 */
	private JComboBoxWithPopup getJComboBox_genre() {
		if (jComboBox_genre == null) {
			jComboBox_genre = new JComboBoxWithPopup();
			jComboBox_genre.setMaximumRowCount(32);

			jComboBox_genre.addItem("");
			for ( ProgGenre genre : ProgGenre.values()) {
				jComboBox_genre.addItem(genre.toString());
			}

			jComboBox_genre.addItemListener(il_genreSelected);
		}

		return(jComboBox_genre);
	}

	/*
	 * 「サブジャンル」コンボボックス
	 */
	private JComboBoxWithPopup getJComboBox_subgenre() {
		if (jComboBox_subgenre == null) {
			jComboBox_subgenre = new JComboBoxWithPopup();

			jComboBox_subgenre.addItem("");
			jComboBox_subgenre.setMaximumRowCount(32);
			jComboBox_subgenre.addItemListener(il_subgenreSelected);
		}

		return(jComboBox_subgenre);
	}

	/*
	 * 「番組長」ラベル
	 */
	private JLabel getJLabel_length(String s) {
		if (jLabel_length == null) {
			jLabel_length = new JLabel(s);
		}
		return(jLabel_length);
	}

	/*
	 * 「番組長下限」テキストフィールド
	 */
	private JComboBoxWithPopup  getJComboBox_lenfrom() {
		if (jComboBox_lenfrom == null) {
			jComboBox_lenfrom = createLenComboBox();
			jTextField_lenfrom = (JTextField) jComboBox_lenfrom.getEditor().getEditorComponent();
			jTextField_lenfrom.getDocument().addDocumentListener(dl_documentChanged);
		}

		return(jComboBox_lenfrom);
	}

	/*
	 * 「～」ラベル
	 */
	private JLabel getJLabel_lenamong(String s) {
		if (jLabel_lenamong == null) {
			jLabel_lenamong = new JLabel(s);
			jLabel_lenamong.setHorizontalAlignment(JLabel.CENTER);
		}
		return(jLabel_lenamong);
	}

	/*
	 * 「番組長上限」テキストフィールド
	 */
	private JComboBoxWithPopup getJComboBox_lento() {
		if (jComboBox_lento == null) {
			jComboBox_lento = createLenComboBox();
			jTextField_lento = (JTextField) jComboBox_lento.getEditor().getEditorComponent();
//			jTextField_lento.setHorizontalAlignment(JTextField.RIGHT);
			jTextField_lento.getDocument().addDocumentListener(dl_documentChanged);
		}

		return(jComboBox_lento);
	}

	/*
	 * 「フラグ」ラベル
	 */
	private JLabel getJLabel_flag(String s) {
		if (jLabel_flag == null) {
			jLabel_flag = new JLabel(s);
		}
		return(jLabel_flag);
	}

	/*
	 * 「新番組」チェックボックス
	 */
	private JCheckBox getJCheckBox_new(String s) {
		if (jCheckBox_new == null) {
			jCheckBox_new = createFlagCheckBox(s);
		}
		return(jCheckBox_new);
	}

	/*
	 * 「最終回」チェックボックス
	 */
	private JCheckBox getJCheckBox_final(String s) {
		if (jCheckBox_final == null) {
			jCheckBox_final = createFlagCheckBox(s);
		}
		return(jCheckBox_final);
	}

	/*
	 * 「再放送」チェックボックス
	 */
	private JCheckBox getJCheckBox_repeat(String s) {
		if (jCheckBox_repeat == null) {
			jCheckBox_repeat = createFlagCheckBox(s);
		}
		return(jCheckBox_repeat);
	}

	/*
	 * 「初回放送」チェックボックス
	 */
	private JCheckBox getJCheckBox_first(String s) {
		if (jCheckBox_first == null) {
			jCheckBox_first = createFlagCheckBox(s);
		}
		return(jCheckBox_first);
	}

	/*
	 * 「特番」チェックボックス
	 */
	private JCheckBox getJCheckBox_special(String s) {
		if (jCheckBox_special == null) {
			jCheckBox_special = createFlagCheckBox(s);
		}
		return(jCheckBox_special);
	}

	/*
	 * 「フラグ」チェックボックスを生成する
	 */
	private JCheckBox createFlagCheckBox(String s){
		JCheckBox checkBox = new JCheckBox();
		checkBox.setText(s);
		checkBox.addActionListener(al_check);
		return(checkBox);
	}

	/*******************************************************************************
	 * アクション
	 ******************************************************************************/
	/*
	 * クリアする
	 */
	protected void doClear(){
		jTextField_keyword.setText("");
		jDateField_from.setText("");
		jDateField_to.setText("");

		jCheckBox_title.setSelected(true);
		jCheckBox_detail.setSelected(true);
		jCheckBox_filter.setSelected(false);

		jComboBox_channel.setSelectedItem("");

		jComboBox_genre.setSelectedItem("");
		jComboBox_subgenre.setSelectedItem("");

		jTextField_lenfrom.setText("");
		jTextField_lento.setText("");

		jCheckBox_new.setSelected(false);
		jCheckBox_final.setSelected(false);
		jCheckBox_repeat.setSelected(false);
		jCheckBox_first.setSelected(false);
		jCheckBox_special.setSelected(false);
	}

	/*
	 * 履歴の名称を変更する
	 */
	protected void doRename(){
		int no = jComboBox_swlist.getSelectedIndex();
		if (no < 1)
			return;

		SearchWordItem swi = swlist.getWordList().get(no-1);
		String nameOld = swi.getLabel();

		VWSearchWordRenameDialog dlg = new VWSearchWordRenameDialog();

		autoclose_enabled = false;
		dlg.open(nameOld,  swlist, this);
		autoclose_enabled = true;

		String nameNew = dlg.getEditedName();
		if (nameNew == null)
			return;

		swi.setLabel(nameNew);
		SearchKey search = createSearchKey();
		search.setLabel(nameNew);
		swi.setSearchKey(search);
		swlist.save();

		updateKeywordComboBox();

		jComboBox_swlist.setSelectedItem(nameNew);

		jToolbar.updateKeywordComboBox();
	}

	/*
	 * 履歴を削除する
	 */
	protected void doDelete(){
		int no = jComboBox_swlist.getSelectedIndex();
		if (no < 1)
			return;

		swlist.getWordList().remove(no-1);
		swlist.save();
		updateKeywordComboBox();

		if (jComboBox_swlist.getItemCount() > no){
			jComboBox_swlist.setSelectedIndex(no);
        	SearchWordItem swi = swlist.getWordList().get(no-1);
			decodeKeyword((String)jComboBox_swlist.getSelectedItem(), swi);
		}
		else
			doClear();

		jToolbar.updateKeywordComboBox();
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

	/*
	 * 検索する
	 */
	protected boolean doSearch(){
		StringBuilder sb = new StringBuilder("");

		// 履歴の名称
		String label = (String)jComboBox_swlist.getSelectedItem();
		// キーワード
		String keyword = jTextField_keyword.getText();

		// 放送日の下限
		String from = jDateField_from.getText();
		if (from != null && from.length() > 0){
			if (!checkDateFormat(from, true))
				return false;

			sb.append(from);
		}

		// 放送日の上限
		String to = jDateField_to.getText();
		if (to != null && to.length() > 0){
			if (!checkDateFormat(to, true))
				return false;

			if (sb.length() > 0)
				sb.append(" ");
			sb.append(to);
		}

		// ログ表示モードの場合
		if (isLogViewMode()){
			if ( ! jToolbar.jumpToPassed(from)) {
				JOptionPane.showConfirmDialog(null, from+"はみつからなかったでゲソ！", "警告", JOptionPane.CLOSED_OPTION);
				return false;
			}
			setVisible(false);
			return true;
		}

		// 検索文字列を生成する
		if (sb.length() == 0 && jCheckBox_filter.isSelected())
			sb.append("@filter");

		if (keyword.length() > 0){
			if (jCheckBox_title.isSelected() && jCheckBox_detail.isSelected())
				;
			else if (jCheckBox_title.isSelected()){
				if (sb.length() > 0)
					sb.append(" ");
				sb.append("#title");
			}
			else if (jCheckBox_detail.isSelected()){
				if (sb.length() > 0)
					sb.append(" ");
				sb.append("#detail");
			}

			if (sb.length() > 0)
				sb.append(" ");
			sb.append(keyword);
		}

		// 検索キーを生成する
		SearchKey search = createSearchKey();

		// 検索を実行する
		if (label.isEmpty())
			label = sb.toString();
		jToolbar.keywordSearch(label, sb.toString(), search, keyword, from, to, jCheckBox_filter.isSelected());

		setVisible(false);

		return true;
	}

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/
	/*
	 * ウインドウのオープン／クローズ等の変化
	 */
	private final WindowListener wl_panel = new WindowListener(){
		@Override
		public void windowOpened(WindowEvent e) {
		}

		@Override
		public void windowClosing(WindowEvent e) {
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
			if (autoclose_enabled)
				doCancel();
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowActivated(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			if (autoclose_enabled)
				doCancel();
		}
	};

	/*
	 * 「検索履歴」コンボボックスの選択変更
	 */
	private final ItemListener il_swlistSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                	int no = jComboBox_swlist.getSelectedIndex();
                	SearchWordItem swi = no < 1 ? null : swlist.getWordList().get(no-1);
                	decodeKeyword((String)jComboBox_swlist.getSelectedItem(), swi);
                    break;
            }
		}
	};

	/*
	 * テキストフィールドでのEnterキー
	 */
	private final ActionListener al_search = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (jButton_search.isEnabled())
				doSearch();
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

	/*
	 * カレンダーからのイベント
	 */
	private final CalendarListener cl_dateField = new CalendarListener(){
		@Override
		public void notifyDateChange(JCalendar cal, String date) {
		}

		@Override
		public boolean isHoliday(int year, int month, int day) {
			return HolidayInfo.IsHoliday(year, month, day);
		}
	};

	/*
	 * 「ジャンル」コンボボックスの選択変更
	 */
	private final ItemListener il_genreSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                	updateSubgenreComboBox();
                	updateControlStatus();
                    break;
            }
		}
	};

	/*
	 * 「サブジャンル」コンボボックスの選択変更
	 */
	private final ItemListener il_subgenreSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                	updateControlStatus();
                    break;
            }
		}
	};

	/*
	 * 「チャンネル」コンボボックスの選択変更
	 */
	private final ItemListener il_channelSelected = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent e) {
            switch (e.getStateChange()) {
                case ItemEvent.SELECTED:
                	updateControlStatus();
                    break;
            }
		}
	};

	/*
	 * チェックボックスでのクリック
	 */
	private final ActionListener al_check = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			updateControlStatus();
		}
	};


	/*******************************************************************************
	 * 内部関数
	 ******************************************************************************/
	/*
	 * 日付フィールドを生成する
	 */
	private JDateField createDateField(String tt){
		JDateField field = new JDateField();
		field.setAllowNoYear(true);
		field.addActionListener(al_search);
		field.setListener(cl_dateField);

		JTextField textField = (JTextField) field.getEditor().getEditorComponent();
		textField.getDocument().addDocumentListener(dl_documentChanged);
		textField.setToolTipText(tt);

		return field;
	}

	/*
	 * 「番組長」コンボボックスを生成する
	 */
	private JComboBoxWithPopup createLenComboBox(){
		JComboBoxWithPopup combo = new JComboBoxWithPopup();
		combo.setEditable(true);
		combo.setMaximumRowCount(16);

		combo.addItem("");

		int [] lens = {0, 5, 10, 15, 30, 45, 60, 90, 120, 180};
		for (int len : lens){
			combo.addItem(String.valueOf(len));
		}

		return combo;
	}

	/*
	 * 「検索履歴」コンボボックスを更新する
	 */
	protected void updateKeywordComboBox(){
		String str = (String)jComboBox_swlist.getSelectedItem();
		if (str == null)
			str = "";

		jComboBox_swlist.removeAllItems();

		jComboBox_swlist.addItem("");

		int num=0;
		for (SearchWordItem item : swlist.getWordList()){
			if (num >= MAX_SEARCH_WORDS)
				break;

			jComboBox_swlist.addItem(item.getLabel());
			num++;
		}

		jComboBox_swlist.setSelectedItem(str);
	}

	/*
	 * 「サブジャンル」コンボボックスを更新する
	 */
	protected void updateSubgenreComboBox(){
		String genre = (String)jComboBox_genre.getSelectedItem();

		jComboBox_subgenre.removeAllItems();
		jComboBox_subgenre.addItem("");

		for ( ProgSubgenre subgenre : ProgSubgenre.values()) {
			if (subgenre.getGenre().toString().equals(genre))
				jComboBox_subgenre.addItem(subgenre.toString());
		}
	}

	/*
	 * 部品のステータスを更新する
	 */
	protected void updateControlStatus(){
		String today = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
		String from = jDateField_from.getText();
		String to = jDateField_to.getText();
		boolean swlsel = jComboBox_swlist.getSelectedIndex() > 0;
		boolean hasrange = from.length() > 0 || to.length() > 0;
		boolean hasfrom = from.length() == 10 && to.length() == 0;
		boolean logview = isLogViewMode();
		boolean passlog = logview && hasfrom && from.compareTo(today) < 0;
		boolean passkey = hasrange && from.compareTo(today) < 0 && to.compareTo(today) < 0;

		jButton_rename.setEnabled(swlsel);
		jButton_delete.setEnabled(swlsel);
		jButton_search.setEnabled(isSearchEnabled());
		jButton_search.setText(passlog ? "過去ログ閲覧" : logview ? "番組表閲覧" : passkey ? "過去ログ検索" : "検索");
		jButton_same.setEnabled(hasrange);

		jCheckBox_filter.setEnabled(!hasrange);
		jDateField_to.setEnabled(from.length() > 0);
	}

	/*
	 * ログ閲覧モードかを返す
	 */
	private boolean isLogViewMode(){
		if (!jTextField_keyword.getText().isEmpty())
			return false;
		if (jDateField_from.getText().isEmpty()){
			return false;
		}
		if (!jDateField_to.getText().isEmpty()){
			return false;
		}
		if (!jComboBox_channel.getSelectedItem().toString().isEmpty())
			return false;
		if (!jComboBox_genre.getSelectedItem().toString().isEmpty())
			return false;

		if (!jTextField_lenfrom.getText().isEmpty())
			return false;
		if (!jTextField_lento.getText().isEmpty())
			return false;

		if (jCheckBox_new.isSelected())
			return false;
		if (jCheckBox_final.isSelected())
			return false;
		if (jCheckBox_repeat.isSelected())
			return false;
		if (jCheckBox_first.isSelected())
			return false;
		if (jCheckBox_special.isSelected())
			return false;

		return true;
	}

	/*
	 * 検索ボタンが有効かどうかを返す
	 */
	private boolean isSearchEnabled(){
		if (!jDateField_from.getText().isEmpty()){
			if (!jDateField_from.hasValidDate())
				return false;
		}

		if (!jDateField_to.getText().isEmpty()){
			if (!jDateField_to.hasValidDate())
				return false;
		}

		if (!jTextField_keyword.getText().isEmpty())
			return true;
		if (!jComboBox_channel.getSelectedItem().toString().isEmpty())
			return true;
		if (!jComboBox_genre.getSelectedItem().toString().isEmpty())
			return true;

		if (!jTextField_lenfrom.getText().isEmpty())
			return true;
		if (!jTextField_lento.getText().isEmpty())
			return true;

		if (jCheckBox_new.isSelected())
			return true;
		if (jCheckBox_final.isSelected())
			return true;
		if (jCheckBox_repeat.isSelected())
			return true;
		if (jCheckBox_first.isSelected())
			return true;
		if (jCheckBox_special.isSelected())
			return true;

		// ログ開始日のみ指定の場合
		if (!jDateField_from.getText().isEmpty() && jDateField_to.getText().isEmpty()){
			return true;
		}

		return false;
	}

	/*
	 * 過去日の年月日をチェックする
	 */
	private boolean checkPassedDate(String syear, String smonth, String sday, boolean msg){
		int month = Integer.parseInt(smonth);
		int day = Integer.parseInt(sday);

		if (month < 1 || month > 12 || day < 1 || day > 31)
			throw new NumberFormatException();

		String today = null;
		String date = null;
		GregorianCalendar c = new GregorianCalendar();
		c.add(Calendar.DATE, 8);

		if (syear == null){
			today = new SimpleDateFormat("MM/dd").format(c.getTime());
			date = String.format("%02d/%02d",  month, day);
		}
		else{
			int year = Integer.parseInt(syear);
			today = new SimpleDateFormat("yyyy/MM/dd").format(c.getTime());
			date = String.format("%04d/%02d/%02d", year, month, day);
		}

		if (date.compareTo(today) >= 0){
			if (msg)
				showWarnMessage(date + ":過去日を指定してください。");
			return false;
		}

		return true;
	}

	/*
	 * 日付のフォーマットをチェックする
	 */
	private boolean checkDateFormat(String str, boolean msg){
		if (str.length() == 0)
			return true;

		Matcher ma = Pattern.compile("^([0-9]{4})/([0-9]{2})/([0-9]{2})$").matcher(str);
		Matcher mb = Pattern.compile("^([0-9]{2})/([0-9]{2})$").matcher(str);

		try{
			if (ma.find()){
				if (!checkPassedDate(ma.group(1), ma.group(2), ma.group(3), msg))
					return false;
			}
			else if (mb.find()){
				if (!checkPassedDate(null, mb.group(1), mb.group(2), msg))
					return false;
			}
			else{
				throw new NumberFormatException();
			}
		}
		catch(NumberFormatException e){
			if (msg){
				showWarnMessage(str+":不正な日付です！「YYYY/MM/DD」ないし「MM/DD」で指定してください。");
			}
			return false;
		}

		return true;
	}

	/*
	 * 警告メッセージを表示する
	 */
	private void showWarnMessage(String msg){
		autoclose_enabled = false;
		JOptionPane.showConfirmDialog(null, msg, "警告", JOptionPane.CLOSED_OPTION);
		autoclose_enabled = true;
	}

	/*
	 * キーワードをデコードする
	 */
	private void decodeKeyword(String str, SearchWordItem swi){
		if (str == null || str.isEmpty())
			return;

		_decodeKeyword(str, swi);
		updateControlStatus();
	}

	/*
	 * キーワードのデコードを実行する
	 */
	private void _decodeKeyword(String str, SearchWordItem swi){
		doClear();

		String keyword = swi != null ? swi.getKeyword() : null;
		if (keyword != null)
			str = keyword;

		// 過去ログ閲覧
		if (str.matches("^\\d\\d\\d\\d/\\d\\d/\\d\\d$")) {
			jDateField_from.setText(str);
			return;
		}

		// 過去ログ検索
		Matcher ma = Pattern.compile("^(\\d\\d\\d\\d/)?(\\d\\d/\\d\\d)([ 　]+((\\d\\d\\d\\d/)?\\d\\d/\\d\\d))?[  　]?(.*)$").matcher(str);
		if (ma.find()) {
			String from = (ma.group(1) != null ? ma.group(1) : "") + ma.group(2);
			String to = ma.group(4);
			str = ma.group(6);

			jDateField_from.setText(from);
			jDateField_to.setText(to);
		}

		// 絞り込みあり
		Matcher mb = Pattern.compile("^@filter[ 　]+(.*)$").matcher(str);
		if ( mb.find() ) {
			str = mb.group(1);
			jCheckBox_filter.setSelected(true);
		}

		// オプションあり
		Matcher	mc = Pattern.compile("^(#title|#detail)?[  　]+(.*)$").matcher(str);
		if (mc.find()){
			jCheckBox_title.setSelected(!mc.group(1).equals("#detail"));
			jCheckBox_detail.setSelected(!mc.group(1).equals("#title"));
			jTextField_keyword.setText(mc.group(2));
		}
		// オプションなし
		else{
			jTextField_keyword.setText(str);
		}

		// 検索履歴あり
		if (swi != null){
			String from = swi.getFrom();
			String to = swi.getTo();
			SearchKey key = swi.getSearchKey();

			if (from != null)
				jDateField_from.setText(from);
			if (to != null)
				jDateField_to.setText(to);
			if (key != null)
				decodeSearchKey(key);
		}

		updateControlStatus();
		jTextField_keyword.requestFocusInWindow();
	}

	/*
	 * 検索キーを生成する
	 */
	private SearchKey createSearchKey(){
		SearchKey sk = new SearchKey();

		StringBuilder label = new StringBuilder("");
		String tStr = "";
		String rStr = "";
		String cStr = "";
		boolean hasData = false;

		// TITLEANDDETAIL	("0",	true,	true,	"番組名、内容に"),
		String keyword = jTextField_keyword.getText();
		if (!keyword.isEmpty()){
			if (jCheckBox_title.isSelected() && jCheckBox_detail.isSelected()){
				tStr += "0\t";
				rStr += keyword + "\t";
				cStr += "0\t";
				hasData = true;
			}
			// TITLE			("1",	true,	true,	"番組名に"),
			else if (jCheckBox_title.isSelected()){
				tStr += "1\t";
				rStr += keyword + "\t";
				cStr += "0\t";
				hasData = true;
			}
			// DETAIL			("2",	true,	true,	"番組内容に"),
			else if (jCheckBox_detail.isSelected()){
				tStr += "2\t";
				rStr += keyword + "\t";
				cStr += "0\t";
				hasData = true;
			}
			label.append(keyword);
		}
		else
			label.append("(キーワードなし)");

		// CHANNEL			("3",	true,	true,	"チャンネル名に"),
		String ch = (String)jComboBox_channel.getSelectedItem();
		if (!ch.isEmpty()){
			tStr += "3\t";
			rStr += ch + "\t";
			cStr += "0\t";
			label.append("+" + ch);
			hasData = true;
		}

		// GENRE			("4",	false,	true,	"ジャンルに"),
		String genre = (String)jComboBox_genre.getSelectedItem();
		if (!genre.isEmpty()){
			tStr += "4\t";
			rStr += genre + "\t";
			cStr += "0\t";
			label.append("+" + genre);
			hasData = true;
		}
		// SUBGENRE		("15",	false,	true,	"サブジャンルに"),
		String subgenre = (String)jComboBox_subgenre.getSelectedItem();
		if (!subgenre.isEmpty()){
			tStr += "15\t";
			rStr += genre + " - " + subgenre + "\t";
			cStr += "0\t";
			label.append("+" + subgenre);
			hasData = true;
		}

		// NEW				("5",	false,	false,	"新番組"),
		if (jCheckBox_new.isSelected()){
			tStr += "5\t";
			rStr += "\t";
			cStr += "0\t";
			label.append("+新");
			hasData = true;
		}
		// LAST			("6",	false,	false,	"最終回"),
		if (jCheckBox_final.isSelected()){
			tStr += "6\t";
			rStr += "\t";
			cStr += "0\t";
			label.append("+終");
			hasData = true;
		}
		// REPEAT			("7",	false,	false,	"再放送"),
		if (jCheckBox_repeat.isSelected()){
			tStr += "7\t";
			rStr += "\t";
			cStr += "0\t";
			label.append("+再");
			hasData = true;
		}
		// FIRST			("8",	false,	false,	"初回放送"),
		if (jCheckBox_first.isSelected()){
			tStr += "8\t";
			rStr += "\t";
			cStr += "0\t";
			label.append("+初");
			hasData = true;
		}
		// LENGTH			("9",	false,	true,	"番組長が"),
		String lenfrom = jTextField_lenfrom.getText();
		if (!lenfrom.isEmpty()){
			tStr += "9\t";
			rStr += lenfrom + " \t";
			cStr += "0\t";
			label.append("+" + lenfrom + "分以上");
			hasData = true;
		}

		String lento = jTextField_lento.getText();
		if (!lento.isEmpty()){
			tStr += "9\t";
			rStr += String.valueOf(Integer.parseInt(lento)+1) + " \t";
			cStr += "1\t";
			label.append("+" + lento + "分以下");
			hasData = true;
		}

		// STARTA			("10",	false,	true,	"開始時刻(上限)が"),
		// STARTZ			("11",	false,	true,	"開始時刻(下限)が"),
		// SPECIAL			("12",	false,	false,	"特番"),
		if (jCheckBox_special.isSelected()){
			tStr += "12\t";
			rStr += "\t";
			cStr += "0\t";
			label.append("+特");
			hasData = true;
		}
		// NOSCRUMBLE		("13",	false,	false,	"無料放送"),
		// STARTDATETIME	("14",	true,	true,	"開始日時に"),
		// LIVE				("16",	false,	false,	"生放送"),
		// BILINGUAL		("17",	false,	false,	"二か国語放送"),
		// STANDIN			("18",	false,	false,	"吹替放送"),
		// RATING			("19",	false,	false,	"視聴制限"),
		// MULTIVOICE		("20",	false,	false,	"副音声/コメンタリ"),

		if (!hasData)
			return null;

		sk.setLabel(label.toString());
		sk.setTarget(tStr);		// compile後は順番が変わるので残すことにする
		sk.setKeyword(rStr);	// 同上
		sk.setContain(cStr);	// 同上

		sk.setCondition("0");
		sk.setInfection("0");
		sk.setOkiniiri("0");
		sk.setCaseSensitive(false);
		sk.setShowInStandby(false);

		new SearchProgram().compile(sk);

		return sk;
	}

	/*
	 * 検索キーをデコードする
	 */
	private void decodeSearchKey(SearchKey key){
		if (key == null || key.getTarget() == null)
			return;

		String [] ts = key.getTarget().split("\t");
		String [] rs = key.getKeyword().split("\t");
		String [] cs = key.getContain().split("\t");

		jTextField_keyword.setText("");

		for (int n=0; n<ts.length; n++){
//			System.out.println("ts[" + n +"]=" + ts[n]);

			switch(ts[n]){
			case "0":			// TITLEANDDETAIL	("0",	true,	true,	"番組名、内容に"),
			case "1":			// TITLE			("1",	true,	true,	"番組名に"),
			case "2":			// DETAIL			("2",	true,	true,	"番組内容に"),
				jTextField_keyword.setText(rs[n]);
				jCheckBox_title.setSelected(!ts[n].equals("2"));
				jCheckBox_detail.setSelected(!ts[n].equals("1"));
				break;
			case "3":			// CHANNEL			("3",	true,	true,	"チャンネル名に"),
				jComboBox_channel.setSelectedItem(rs[n]);
				break;
			case "4":			// GENRE			("4",	false,	true,	"ジャンルに"),
				jComboBox_genre.setSelectedItem(rs[n]);
				break;
			case "5":			// NEW				("5",	false,	false,	"新番組"),
				jCheckBox_new.setSelected(true);
				break;
			case "6":			// LAST			("6",	false,	false,	"最終回"),
				jCheckBox_final.setSelected(true);
				break;
			case "7":			// REPEAT			("7",	false,	false,	"再放送"),
				jCheckBox_repeat.setSelected(true);
				break;
			case "8":			// FIRST			("8",	false,	false,	"初回放送"),
				jCheckBox_first.setSelected(true);
				break;
			case "9":			// LENGTH			("9",	false,	true,	"番組長が"),
				if (cs[n].equals("0"))
					jTextField_lenfrom.setText(rs[n].trim());
				else if (cs[n].equals("1")){
					try{
						jTextField_lento.setText(String.valueOf(Integer.parseInt(rs[n].trim())-1));
					}catch(NumberFormatException e){
						jTextField_lento.setText(rs[n].trim());
					}
				}
				break;
			// STARTA			("10",	false,	true,	"開始時刻(上限)が"),
			// STARTZ			("11",	false,	true,	"開始時刻(下限)が"),
			case "12":			// SPECIAL			("12",	false,	false,	"特番"),
				jCheckBox_special.setSelected(true);
				break;
			case "15":			// SUBGENRE		("15",	false,	true,	"サブジャンルに"),
				ProgSubgenre subgenre = ProgSubgenre.get(rs[n]);
				jComboBox_subgenre.setSelectedItem(subgenre != null ? subgenre.toString() : "");
				break;
			}
		}
	}
}
