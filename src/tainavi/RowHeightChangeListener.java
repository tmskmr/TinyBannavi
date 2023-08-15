package tainavi;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JTable;
import javax.swing.JTree;

public class RowHeightChangeListener implements PropertyChangeListener {

	public int h = 4;
	
	public RowHeightChangeListener(int h) { this.h = h; }
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if ( e.getSource() instanceof JTable ) {
			JTable comp = (JTable) e.getSource();
			comp.setRowHeight(comp.getFont().getSize()+h);
		}
		else if ( e.getSource() instanceof JTree ) {
			JTree comp = (JTree) e.getSource();
			comp.setRowHeight(comp.getFont().getSize()+h);
		}
	}

}
