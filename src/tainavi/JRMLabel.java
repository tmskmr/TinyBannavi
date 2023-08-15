package tainavi;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.JLabel;

public class JRMLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	private String enc = "";
	private String date = "";
	private String center = "";
	private boolean exec = true;
	private BufferedImage image = null;
	private Color encForeground = Color.WHITE;
	private Color encBackground = Color.BLACK;
	
	// 予約枠の仮想座標
	private int vrow;
	private int vcolumn;
	private int vheight;
	private int vwidth;
	
	private static int columnWidth = 0;
	private static float heightMultiplier = 0;
	
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
			
			// 書き込む文字列のサイズを算出する
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D r = fm.getStringBounds(enc,g2);
			
			// 書き込む位置を決定する
			int x = 0;
			int y = 0;
			if (this.getHorizontalAlignment() == JLabel.RIGHT) {
				x = w - (int)r.getWidth() - 6;
			}
			else {
				x = 6;
			}
			if (this.getVerticalAlignment() == JLabel.BOTTOM) {
				y = h - (int)r.getHeight()/2 - 1;
			}
			else {
				y = (int)r.getHeight() + 1;
			}
			
			// 書き込む
			AttributedString as = new AttributedString(enc);
			as.addAttribute(TextAttribute.FOREGROUND, this.getEncForeground());
			as.addAttribute(TextAttribute.BACKGROUND, this.getEncBackground());
			AttributedCharacterIterator ac = as.getIterator();
			g2.drawString(ac, x, y);
		}
		
		// 反映
		g.drawImage(image, 0, 0, this);
	}
	
	// データ操作メソッド
	public void setDate(String s) { date = s; }
	public String getDate() { return date; }
	public void setCenter(String s) { center = s; }
	public String getCenter() { return center; }
	public void setEncoder(String s) { enc = s; }
	public String getEncoder() { return enc; }
	public void setExec(boolean b) { exec = b; }
	public boolean getExec() { return exec; }

	public void setEncForeground(Color c) { encForeground = c; }
	public Color getEncForeground() { return encForeground; }
	public void setEncBackground(Color c) { encBackground = c; }
	public Color getEncBackground() { return encBackground; }
	
	//public void setVRow(int n) { vrow = n; }
	public int getVRow() { return vrow; }
	//public void setVColumn(int n) { vcolumn = n; }
	public int getVColumn() { return vcolumn; }
	//public void setVHeight(int n) { vheight = n; }
	public int getVHeight() { return vheight; }
	
	public static void setColumnWidth(int n) { columnWidth = n; }
	public static void setHeightMultiplier(float f) { heightMultiplier = f; }
	
	public void setVBounds(int x, int y, int width, int height) {
		vrow = y;
		vcolumn = x;
		vheight = height;
		vwidth = width;
		super.setBounds(
				vcolumn*columnWidth,
				(int) Math.ceil(((float)vrow)*heightMultiplier),
				vwidth*columnWidth,
				(int) Math.ceil(((float)vheight)*heightMultiplier));
	}
	
	public void reVBounds() {
		super.setBounds(
				vcolumn*columnWidth,
				(int) Math.ceil(((float)vrow)*heightMultiplier),
				vwidth*columnWidth,
				(int) Math.ceil(((float)vheight)*heightMultiplier));
	}
}
