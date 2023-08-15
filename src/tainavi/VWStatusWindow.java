package tainavi;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;


/**
 *  <P>ステータスの小窓
 *  <P><B>【注意】インスタンスは１個のみとしているので、clone()を実行しても自分自身が返ってきます！</B>
 */
public class VWStatusWindow extends JDialog implements StatusWindow,Cloneable {

	private static final long serialVersionUID = 1L;

	private static final int WIN_COLS = 70;
	private static final int WIN_ROWS = 8;

	private final JScrollPane jsp = new JScrollPane();
	private final JTextArea jta = new JTextAreaWithPopup();

	private WindowAdapter wl_closing = null;

	private boolean window_close_requested = false;

	/**
	 * 鯛ナビでは１個インスタンスを作ったらずっと使いまわすので自身を返す。まあ、特殊。
	 */
	@Override
	public VWStatusWindow clone() {
		return this;
	}

	// 内部用メソッド
	private JScrollPane getJScrollPane() {
		jta.setRows(WIN_ROWS);
		jta.setColumns(WIN_COLS);
		jta.setLineWrap(false);
		jta.setEditable(false);

		jsp.setViewportView(jta);
		jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		return jsp;
	}

	// デフォルトコンストラクタ
	public VWStatusWindow() {
		//
		super();
		//
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setModal(true);

		setClosingEnabled(true);

		this.setContentPane(getJScrollPane());
		this.pack();

//		this.setResizable(false);

		//this.setLocationRelativeTo(owner);	// 画面の真ん中に

		//
		this.setTitle("タイニー番組ナビゲータ　ステータスウィンドウ");
	}

	private final WindowAdapter wl_closing_exitdisabled = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			window_close_requested = true;
			jta.append("処理中です。しばらくお待ちください…\n");
		}
	};

	private final WindowAdapter wl_closing_exitenabled = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			window_close_requested = true;
			System.out.println("強制終了します。");
			System.exit(1);
		}
	};

	/*
	 * StatusWindow用のメソッド
	 */

	public void setClosingEnabled(boolean b) {
		if ( wl_closing != null ) this.removeWindowListener(wl_closing);
		if ( b ) {
			this.addWindowListener(wl_closing = wl_closing_exitenabled);
		}
		else {
			this.addWindowListener(wl_closing = wl_closing_exitdisabled);
		}
	}

	@Override
	public void clear() {
		jta.setText("");
		jsp.getVerticalScrollBar().setValue(0);
	}

	@Override
	public void append(String message) {
		jta.append(message+"\n");
		jta.setCaretPosition(jta.getText().length());
	}

	@Override
	public void appendMessage(String message) {
		String msg = CommonUtils.getNow() + message;
		this.append(msg);
		System.out.println(msg);
	}

	@Override
	public void appendError(String message) {
		String msg = CommonUtils.getNow() + message;
		this.append(msg);
		System.err.println(msg);
	}

	@Override
	public void setVisible(boolean b) {
		try {
			super.setVisible(b);
		}
		catch (NullPointerException e) {
			System.err.println("HOGEHOEG");
		}
	}

	/*
	 * 画面の「×」ボタンが押されたか
	 */
	@Override
	public void setWindowCloseRequested(boolean b){ window_close_requested = b; }
	@Override
	public boolean isWindowCloseRequested(){ return window_close_requested; }
	@Override
	public void resetWindowCloseRequested(){ setWindowCloseRequested(false); }
}
