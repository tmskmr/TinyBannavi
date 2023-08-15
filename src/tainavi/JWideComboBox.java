package tainavi;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

/**
 * コンボボックスの幅よりポップアップの幅を広げる
 */
public class JWideComboBox extends JComboBox implements WideComponent {
	
	private static final long serialVersionUID = 1L;
	
	public JWideComboBox() {
		super();
		setMyRenderer();
	}

	public JWideComboBox(ComboBoxModel aModel) {
		super(aModel);
		setMyRenderer();
	}

	public JWideComboBox(Object[] items) {
		super(items);
		setMyRenderer();
	}

	public JWideComboBox(Vector<?> items) {
		super(items);
		setMyRenderer();
	}
	
	private void setMyRenderer() {
		this.setRenderer(new DefaultListCellRenderer() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return super.getListCellRendererComponent(list, (value==null&&list.getModel().getSize()>0)?("(選択されていません)"):(value), index, isSelected, cellHasFocus);
			}
		});
	}

	public int indexOf(Object o) {
		for ( int i=0; i<this.getItemCount(); i++ ) {
			Object obj = this.getItemAt(i);
			if ( (o != null && obj != null && o.equals(obj)) || o == obj ) {
				return i;
			}
		}
		return -1;
	}
	
	private boolean layingOut = false; 
    
	private int w = 50;
	
	@Override
	public void addPopupWidth(int w) {
		this.w = w;
	}
	
	@Override
	public void doLayout() {
		try { 
			layingOut = true; 
			super.doLayout(); 
		}
		finally { 
			layingOut = false; 
		}
	} 
 
	@Override
	public Dimension getSize() { 
		Dimension dim = super.getSize(); 
		if ( ! layingOut ) 
			dim.width += w; 
		return dim;
	}
}