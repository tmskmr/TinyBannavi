package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;

public class JTimebarLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private String ts = "";
	private BufferedImage image = null;
	
	@Override
	public void repaint() {
		image = null;
		super.repaint();
	}
	
	@Override
	protected void paintComponent(Graphics g) { 
		
		super.paintComponent(g);
		
		// 初回描画時
		if (image == null) {
			//
			Dimension  d  = this.getSize();
			int w = d.width;
			int h = d.height;
			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = (Graphics2D)image.createGraphics();
			
			// フォント関連のほげほげ
			Font f = this.getFont();
			g2.setFont(f.deriveFont(f.getStyle() | Font.BOLD));
			g2.setColor(Color.BLACK);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			// 書き込む
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D r = fm.getStringBounds(ts,g);
			g2.drawString(ts, (w-(int)r.getWidth())/2, (h+(int)r.getHeight())/2);
		}
		
		// 反映
		g.drawImage(image, 0, 0, this);
	}
	
	public String getTs() { return ts; }
	
	public JTimebarLabel(String s) {
		super();
		ts = s;
	}
}
