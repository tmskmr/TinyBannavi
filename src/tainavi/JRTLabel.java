package tainavi;

import javax.swing.JLabel;

public class JRTLabel extends JLabel {

	private static final long serialVersionUID = 1L;

	// 予約枠の仮想座標
	private int vrow;
	private int vheight;

	private int vcol;

	private int cno;

	private static float heightMultiplier = 0;
	private static int vwidth;

	// データ操作メソッド
	public int getVRow() { return vrow; }
	public int getVHeight() { return vheight; }
	public int getColorNo(){ return cno; }
	public void setColorNo(int n){ cno = n; }

	public static void setHeightMultiplier(float f) { heightMultiplier = f; }
	public static void setColumnWidth(int n){ vwidth = n; }

	public void setVBounds(int x, int y, int height) {
		vcol = x;
		vrow = y;
		vheight = height;
		super.setBounds(
				vcol,
				(int) Math.ceil(((float)vrow)*heightMultiplier),
				vwidth,
				(int) Math.ceil(((float)vheight)*heightMultiplier));
	}

	public void reVBounds() {
		super.setBounds(
				vcol,
				(int) Math.ceil(((float)vrow)*heightMultiplier),
				vwidth,
				(int) Math.ceil(((float)vheight)*heightMultiplier));
	}

}
