package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.table.TableCellRenderer;

public class VWColorChooserDialog extends JEscCancelDialog {

	private static final long serialVersionUID = 1L;

	//
	private Color selectedColor = null;

	// コンストラクタ

	public VWColorChooserDialog() {

		super();

		this.setModal(true);
		this.setContentPane(getJPanel());

		// タイトルバーの高さも考慮する必要がある
		Dimension d = getJPanel().getPreferredSize();
		this.pack();
/*
		this.setPreferredSize(new Dimension(
				d.width+(this.getInsets().left+this.getInsets().right),
				d.height+(this.getInsets().top+this.getInsets().bottom)));
		this.setResizable(false);
*/
		//
		this.setTitle("色見本");
	}

	// 公開メソッド

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
	}

	public Color getSelectedColor() {
		return selectedColor;
	}

	public void setColor(Color c) {
		jColorChooser.setColor(c);
    	selectedColor = null;
	}

	/*
	public void setPosition(int x, int y) {
		Rectangle r = this.getBounds();
		r.x = x;
		r.y = y;
		this.setBounds(r);
	}
	*/

	// 録画予約ウィンドウのコンポーネント

	private JPanel jPanel = null;
	private JColorChooser jColorChooser = null;
	private JPanel jPanel_buttons = null;
	private JButton jButton = null;
	private JButton jButton_cancel = null;

	// ほげほげ

	private JPanel getJPanel() {
		if (jPanel == null) {
			jPanel = new JPanel();
			jPanel.setLayout(new BorderLayout());
			jPanel.add(getJColorChooser(), BorderLayout.CENTER);
			jPanel.add(getJPanel_buttons(), BorderLayout.SOUTH);

			Dimension d = new Dimension(Env.ZMSIZE(600), Env.ZMSIZE(400));
//			if ( CommonUtils.isLinux() ) {
//				d = new Dimension(600, 400);
//			}
//			else {
//				d = new Dimension(450, 400);
//			}
			jPanel.setPreferredSize(d);
		}
		return(jPanel);
	}

	private JPanel getJPanel_buttons() {
		if (jPanel_buttons == null) {
			jPanel_buttons = new JPanel();
			jPanel_buttons.setLayout(new BorderLayout());
			jPanel_buttons.add(getJButton("決定"), BorderLayout.CENTER);
			jPanel_buttons.add(getJButton_cancel("ｷｬﾝｾﾙ"), BorderLayout.EAST);
		}
		return(jPanel_buttons);
	}

	//
	private JColorChooser getJColorChooser() {
		if (jColorChooser == null) {
			if ( System.getProperty("java.version").startsWith("1.6.") && ! CommonUtils.isLinux() ) {
				System.out.println("[色選択ダイアログ] カスタマイズダイアログを使う： "+System.getProperty("java.version"));
				jColorChooser = new XColorChooser();
			}
			else {
				System.out.println("[色選択ダイアログ] 標準ダイアログを使う： "+System.getProperty("java.version"));
				UIManager.put("ColorChooser.swatchesSwatchSize", new Dimension(Env.ZMSIZE(12), Env.ZMSIZE(12)));
				UIManager.put("ColorChooser.swatchesRecentSwatchSize",  new Dimension(Env.ZMSIZE(12), Env.ZMSIZE(12)));
				jColorChooser = new JColorChooser();
			}
		}
		return jColorChooser;
	}

	private JButton getJButton(String s) {
		if (jButton == null) {
			jButton = new JButton(s);
			final JDialog jd = this;
			jButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					selectedColor = jColorChooser.getColor();
					jd.setVisible(false);
				}
			});
		}
		return(jButton);
	}

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

	@Override
	protected void doCancel() {
		selectedColor = null;
		setVisible(false);
	}

	/*
	 * カスタムカラーチューザー
	 */

	/**
	 * LookAndFeelの変更を行うとカスタマイズした内容がデフォルトのJColorChooserに戻ってしまうことが今更ながら判明
	 * @version 3.16
	 */
	private class XColorChooser extends JColorChooser {

		private static final long serialVersionUID = 1L;

		public XColorChooser() {
			super();
		}

		@Override
		public void updateUI() {
			super.updateUI();
			customize();
		}

		@Override
		public void setPreviewPanel(JComponent preview) {
			// LookAndFeel変更でプレビューパネルが消えてしまうので
			if ( CommonUtils.isLinux() ) super.setPreviewPanel(preview);	// Linuxでは死んでしまう
		}

		private void customize() {
			if ( CommonUtils.isLinux() ) return;	// Linuxでは死んでしまう

			AbstractColorChooserPanel[] accp = this.getChooserPanels();
			for ( AbstractColorChooserPanel a : accp ) {
				this.removeChooserPanel(a);
			}

			for ( AbstractColorChooserPanel a : accp ) {
				if ( ! "RGB".equals(a.getDisplayName())) {
					this.addChooserPanel(a);
				}
			}
			this.addChooserPanel(new customColorChooserPanel2());
			this.addChooserPanel(new customColorChooserPanel());
		}

		private class customColorChooserPanel extends AbstractColorChooserPanel implements MouseListener {

			private static final long serialVersionUID = 1L;

			JTable jTable = null;

			@Override
			protected void buildChooser() {
				if (jTable == null) {
					jTable = new JTable(8,8) {

						private static final long serialVersionUID = 1L;

						@Override
						  public boolean isCellEditable(int row, int column) {
						  	return false;
						  }
					};
					jTable.setDefaultRenderer(Object.class, new labelRenderer());
					jTable.setPreferredSize(new Dimension(300,160));
					jTable.setRowHeight(20);
					jTable.addMouseListener(this);
					add(jTable);
				}
			}

			@Override
			public String getDisplayName() {
				return "鯛ナビ";
			}

			@Override
			public Icon getLargeDisplayIcon() {
				return null;
			}

			@Override
			public Icon getSmallDisplayIcon() {
				return null;
			}

			@Override
			public void updateChooser() {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				Point p = e.getPoint();
				int row = jTable.rowAtPoint(p);
				int column = jTable.columnAtPoint(p);
				Color c = getColorFromTable(row, column);
				jColorChooser.setColor(c);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}
		}

		private class customColorChooserPanel2 extends AbstractColorChooserPanel {

			private static final long serialVersionUID = 1L;

			JColorChooseSlider jCCS_Red = null;
			JColorChooseSlider jCCS_Blue = null;
			JColorChooseSlider jCCS_Green = null;
			JCheckBox jCB_Hex = null;

			@Override
			protected void buildChooser() {
				if (jCCS_Red == null) {

					jCCS_Red = new JColorChooseSlider("赤") {

						private static final long serialVersionUID = 1L;

						@Override
						void evHandle(int n) {
							myEvHandle(n);
						}
					};

					jCCS_Green = new JColorChooseSlider("緑") {

						private static final long serialVersionUID = 1L;

						@Override
						void evHandle(int n) {
							myEvHandle(n);
						}
					};

					jCCS_Blue = new JColorChooseSlider("青") {

						private static final long serialVersionUID = 1L;

						@Override
						void evHandle(int n) {
							myEvHandle(n);
						}
					};

					jCB_Hex = new JCheckBox("16進数(00-ff)で入力", false);

					jCB_Hex.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							jCCS_Red.setHex(jCB_Hex.isSelected());
							jCCS_Green.setHex(jCB_Hex.isSelected());
							jCCS_Blue.setHex(jCB_Hex.isSelected());
						}
					});

					setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
					add(jCCS_Red);
					add(Box.createRigidArea(new Dimension(10,10)));
					add(jCCS_Green);
					add(Box.createRigidArea(new Dimension(10,10)));
					add(jCCS_Blue);
					add(Box.createRigidArea(new Dimension(10,20)));
					add(jCB_Hex);
				}
			}

			@Override
			public String getDisplayName() {
				return "RGB";
			}

			@Override
			public Icon getLargeDisplayIcon() {
				return null;
			}

			@Override
			public Icon getSmallDisplayIcon() {
				return null;
			}

			@Override
			public void updateChooser() {
				if ( jColorChooser != null ) {
					Color c = jColorChooser.getColor();
					jCCS_Red.setValue(c.getRed());
					jCCS_Green.setValue(c.getGreen());
					jCCS_Blue.setValue(c.getBlue());
				}
			}

			private void myEvHandle(int n) {
				jColorChooser.setColor(new Color(jCCS_Red.getValue(),jCCS_Green.getValue(),jCCS_Blue.getValue()));
			}
		}

		private class labelRenderer extends JLabel implements TableCellRenderer {
			private static final long serialVersionUID = 1L;

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				this.setOpaque(true);

				Color c = getColorFromTable(row,column);
				this.setBackground(c);

				this.setToolTipText("("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")");

				return this;
			}
		}

		private Color getColorFromTable(int row, int column) {
			int b = ((int)(column / 4)+((int)(row / 4))*2) * 85;
			int g = (column % 4) * 85;
			int r = (row % 4) * 85;
			return new Color(r,g,b);
		}
	}
}
