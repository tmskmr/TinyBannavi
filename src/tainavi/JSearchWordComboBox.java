package tainavi;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;

/*
 * 検索キーワード用コンボボックス
 */
public class JSearchWordComboBox extends JComboBoxWithPopup {

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	private SearchWordList swlist = null;

	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
    @SuppressWarnings("unchecked")
	public JSearchWordComboBox(SearchWordList list) {
    	super();

    	swlist = list;

		this.setMaximumRowCount(32);
		this.setRenderer(new DefaultListCellRenderer() {
			@Override public Component getListCellRendererComponent(
					JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(
						list, value, index, isSelected, cellHasFocus);
				if (index > 0){
					SearchWordItem swi = swlist.getWordList().get(index-1);
					if (c instanceof JComponent) {
						((JComponent) c).setToolTipText(swi.formatAsHTML(false));
					}
				}
				return c;
			}
		});
    }

	/*******************************************************************************
	 * static 公開メソッド
	 ******************************************************************************/
	/*******************************************************************************
	 * 公開メソッド
	 ******************************************************************************/

	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	/*******************************************************************************
	 * リスナー
	 ******************************************************************************/

    /*******************************************************************************
	 * 内部関数
	 ******************************************************************************/
}