package tainavi;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;


/**
 * JTableのセルに張り付けるON・OFFトグルボタン
 */
public abstract class AbstractExecButtonColumn  extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {

	private static final long serialVersionUID = 1L;

	/**
	 * トグル時に処理を実行するメソッド
	 */
	protected abstract void toggleAction(ActionEvent e);
	
	// 定数
	private static final String LABEL = "";	// ""にしないとボタンに文字が
	
	// 部品
	private final ImageIcon icon;
	private final JButton renderButton;
	private final JButton editorButton;
	
	/**
	 * コンストラクタ
	 */
	public AbstractExecButtonColumn(ImageIcon icon) {
		super();

		this.icon = icon;
		
		renderButton = new JButton(LABEL);
		renderButton.setMargin(new Insets(1, 0, 0, 0));
		renderButton.setHorizontalAlignment(JLabel.CENTER);
		renderButton.setVerticalAlignment(JLabel.CENTER);
		
		editorButton = new JButton(LABEL);
		editorButton.addActionListener(al_toggle);
	}
	
	private final ActionListener al_toggle = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			toggleAction(e);
		}
	};

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		renderButton.setIcon((Boolean) value ? icon : null);
		renderButton.setPressedIcon((Boolean) value ? icon : null);
		return renderButton;
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { 
		editorButton.setIcon((Boolean) value ? icon : null);
		editorButton.setPressedIcon((Boolean) value ? icon : null);
		return editorButton;
	}
	
	@Override
	public Object getCellEditorValue() {
		return renderButton.getIcon() != null; 
	}
}
