package tainavi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.border.LineBorder;


public class OverlapReserveViewPanel extends JScrollPane {

	private static final long serialVersionUID = 1L;

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	private static final int BAR_WIDTH = 20;
	private static final float BAR_NUM = 6.5F;
	
	private static final int BAR_REG = 2;
	
	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/
	
	JPanel jpanel = null;
	
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	public OverlapReserveViewPanel() {
		super();
		
		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		getHorizontalScrollBar().setUnitIncrement(BAR_WIDTH);
		
		setViewportView(getJPanel());

		Dimension d = new Dimension((int)(BAR_NUM*BAR_WIDTH+2.0),0);	// +2はBorder分
		setPreferredSize(d);
	}

	
	/*******************************************************************************
	 * コンポーネント
	 ******************************************************************************/

	public JPanel getJPanel() {
		if ( jpanel == null ) {
			jpanel = new JPanel();
		}
		return jpanel;
	}
	
	
	/*******************************************************************************
	 * メソッド
	 ******************************************************************************/

	public void putOverlap(ReserveList myrsv, ArrayList<ReserveList> overlaps) {

		jpanel.removeAll();
		jpanel.repaint();
		
		if ( overlaps == null || overlaps.size() == 0 ) {
			// 裏番組ないよ
			jpanel.setPreferredSize(getPreferredSize());
			jpanel.setLayout(new BorderLayout());
			jpanel.add(new JLabel("裏番組なし", JLabel.CENTER),BorderLayout.CENTER);
			return;
		}
		
		jpanel.setLayout(new SpringLayout());
		
		//int height = jpanel.getSize().height;
		int height = getSize().height - getHorizontalScrollBar().getSize().height;
		
		int min = CommonUtils.getMinOfDate(myrsv.getAhh(),myrsv.getAmm());
		int len = CommonUtils.getRecMinVal(myrsv.getAhh(),myrsv.getAmm(),myrsv.getZhh(),myrsv.getZmm());
		float mul = (float)height / (len*BAR_REG);
		int topmin = min-len/2;
		int bottommin = topmin+len*BAR_REG;
		
		int n = 0;
		_putOverlap(jpanel,myrsv,height,topmin,bottommin,n++,mul);
		for ( ReserveList r : overlaps ) {
			if ( r == myrsv ) {
				continue;
			}
			_putOverlap(jpanel,r,height,topmin,bottommin,n++,mul);
		}
		
		jpanel.setPreferredSize(new Dimension(BAR_WIDTH*n,height));
	}
	
	private void _putOverlap(JPanel p, ReserveList r, int height, int topmin, int bottommin, int n, float mul) {
		int amin = CommonUtils.getMinOfDate(r.getAhh(),r.getAmm());
		int len = CommonUtils.getRecMinVal(r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm());
		int zmin = amin+len;
		
		// 描画枠からはみ出ていたら
		if ( zmin < topmin ) {
			amin = amin+1440;
			zmin = zmin+1440;
		}
		else if ( bottommin < amin ) {
			amin = amin-1440;
			zmin = zmin-1440;
		}
		
		int h = (int)(mul*len);
		int x = n*BAR_WIDTH;
		int y = (int)(mul*(amin-topmin));

		//System.err.println("bbb "+topmin+", "+bottommin+" : "+amin+", "+zmin+" : "+y);
		
		JLabel label = new JLabel(CommonUtils.getVerticalSplittedHTML(r.getTuner()));
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setOpaque(true);
		label.setBackground(n==0 ? Color.WHITE : Color.LIGHT_GRAY);
		label.setBorder(new LineBorder(n==0 ? Color.RED : Color.BLACK));
		label.setToolTipText(String.format("<HTML>[%s]<BR>%s:%s - %s:%s<BR>(%s)<BR>%s</HTML>",r.getTuner(),r.getAhh(),r.getAmm(),r.getZhh(),r.getZmm(),r.getCh_name(),CommonUtils.enEscape(r.getTitle())));
		
		CommonSwingUtils.putComponentOn(p, label, BAR_WIDTH, h, x, y);
		
		JLabel bdr = new JLabel();
		bdr.setOpaque(true);
		bdr.setBackground(Color.GRAY);
		//bdr.setBorder(new DashBorder(Color.GRAY, 1, 5, 5));
		
		CommonSwingUtils.putComponentOn(p, bdr, BAR_WIDTH, height, x, 0);
	}
	
}
