package tainavi;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

/***
 * 
 * 番組追跡検索の設定のクラス
 * 
 */

public class VWKeywordGroupDialog extends JEscCancelDialog {

	private static final long serialVersionUID = 1L;

	private boolean reg = false;
	private String oldName = "";
	
	public String getNewName() { return jTextField_title.getText(); }
	
	// キーワード検索の設定ウィンドウのコンポーネント
	
	private JPanel jPanel = null;
	
	private JLabel jLabel_title = null;
	private JTextField jTextField_title = null;
	private JButton jButton_label = null;
	private JButton jButton_cancel = null;

	// ほげほげ

	public boolean isRegistered() { return reg; }
	
	public void reopen(String name) {
		//
		oldName = name;
		jTextField_title.setText(oldName);
		jTextField_title.setCaretPosition(0);
	}
	
	//
	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();

			jPanel.setLayout(new SpringLayout());
			
			int y = 10;
			_getJComponent(jPanel, getJLabel_title("グループ名"), 80, 25, 10, y);
			_getJComponent(jPanel, getJTextField_title(), 200, 25, 100, y);
			
			y += 50;
			_getJComponent(jPanel, getJButton_label("登録"), 75, 25, 110, y);
			_getJComponent(jPanel, getJButton_cancel("ｷｬﾝｾﾙ"), 75, 25, 195, y);
			
			y += 30;
			Dimension d = new Dimension(330,y+10);
			jPanel.setPreferredSize(d);
		}
		return jPanel;
	}
	
	private void _getJComponent(JPanel p, JComponent c, int width, int height, int x, int y) {
	    c.setPreferredSize(new Dimension(width, height));
	    ((SpringLayout)p.getLayout()).putConstraint(SpringLayout.NORTH, c, y, SpringLayout.NORTH, p);
	    ((SpringLayout)p.getLayout()).putConstraint(SpringLayout.WEST, c, x, SpringLayout.WEST, p);
	    p.add(c);
	}
	
	
	
	//
	private JLabel getJLabel_title(String s) {
		if (jLabel_title == null) {
			jLabel_title = new JLabel(s);
		}
		return(jLabel_title);
	}
	
	//
	private JTextField getJTextField_title() {
		if (jTextField_title == null) {
			jTextField_title = new JTextField();
		}
		return(jTextField_title);
	}
	
	//
	private JButton getJButton_label(String s) {
		if (jButton_label == null) {
			jButton_label = new JButton();
			jButton_label.setText(s);
			
			jButton_label.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (jTextField_title.getText().equals("")) {
						return;
					}
					if ( ! jTextField_title.getText().equals(oldName)) {
						reg = true;
					}
					
					// ウィンドウを閉じる
					dispose();
				}
			});
		}
		return(jButton_label);
	}
	
	//
	private JButton getJButton_cancel(String s) {
		if (jButton_cancel == null) {
			jButton_cancel = new JButton();
			jButton_cancel.setText(s);
			
			jButton_cancel.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					doCancel();
				}
			});
		}
		return jButton_cancel;
	}
	
	@Override
	protected void doCancel() {
		dispose();
	}
	
	
	// コンストラクタ
	public VWKeywordGroupDialog() {
		
		super();

		//
		reg = false;
		
		//
		this.setModal(true);
		this.setContentPane(getJPanel());
		// タイトルバーの高さも考慮する必要がある
		Dimension d = getJPanel().getPreferredSize();
		this.pack();
		this.setPreferredSize(new Dimension(
				d.width+(this.getInsets().left+this.getInsets().right),
				d.height+(this.getInsets().top+this.getInsets().bottom)));
		this.setResizable(false);
		//
		this.setTitle("キーワードグループの設定");
	}
}
