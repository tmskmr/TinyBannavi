package tainavi;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;


public class JTXTButton extends JLabel {

	private static final long serialVersionUID = 1L;

	/*******************************************************************************
	 * 定数
	 ******************************************************************************/

	/**
	 *  フォントスタイル
	 */
	public static enum FontStyle {
		BOLD		("太字"),
		ITALIC		("斜体"),
		UNDERLINE	("下線");

		private String name;

		private FontStyle(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}


		public String getId() {
			return super.toString();
		}

		public static FontStyle get(String id) {
			for ( FontStyle fs : FontStyle.values() ) {
				if ( fs.getId().equals(id) ) {
					return fs;
				}
			}
			return null;
		}
	};

	private static final float DRAWTAB = 2.0F;


	/*******************************************************************************
	 * 部品
	 ******************************************************************************/

	// 描画バッファ
	private BufferedImage image = null;		// ビットマップ

	private int vrow;						// 仮想座標縦位置
	private int vcolumn;					// 仮想座標横位置
	private int vheight;					// 仮想座標高さ
	private int vwidth;						// 仮想座標幅

	// 番組情報
	private ProgDetailList tvd = null;		// 番組情報そのまま

	// 表示設定
	private static boolean showStart = true;
	private static boolean splitEpno = false;
	private static boolean showDetail = true;
	private static float detailTab = 2.0F;

	private static Font defaultFont = new JLabel().getFont();

	private static Font titleFont = defaultFont;
	private static int titleFontSize = defaultFont.getSize();
	private static Color titleFontColor = Color.BLUE;
	private static int titleFontStyle = Font.BOLD;

	private static Font detailFont = defaultFont;
	private static int detailFontSize = defaultFont.getSize();
	private static Color detailFontColor = Color.DARK_GRAY;
	private static int detailFontStyle = defaultFont.getStyle();

	private static Font startFont = defaultFont;

	private static FontRenderContext frc = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_ON, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);

	private static int columnWidth = 0;
	private static float heightMultiplier = 0;


	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/

	// ないよ


	/*******************************************************************************
	 * メソッド
	 ******************************************************************************/

	// 内容をリセットする
	// setVisible(false)するとリソースが解放されてしまうのか再描画に時間がかかるようになるので表示範囲外に出して隠してしまう
	public void clean() {
		tvd = null;
		image = null;
		setBounds(-1,-1,0,0);
	}

	// フラグを変えた後に再描画させる
	public void forceRepaint() {
		image = null;
		super.repaint();
	}

	// 仮想位置の変更
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

	// 番組情報のやりとり
	public void setInfo(ProgDetailList tvd) {
		this.tvd = tvd;
		this.setText(null);	// 簡易表示時代の名残

		this.setVerticalAlignment(JButton.TOP);
		this.setHorizontalAlignment(JButton.LEFT);
		//this.setBorder(new LineBorder(Color.BLACK,1));
		this.setOpaque(true);
	}
	public ProgDetailList getInfo() {
		return tvd;
	}

	// 予約待機枠を表示するかどうかの確認
	public boolean isStandby() { return tvd.marked && tvd.showinstandby; }
	public boolean isStandbyByTrace() { return tvd.markedByTrace; }

	// 表示スタイル
	public static void setShowStart(boolean b) {
		showStart = b;
	}
	public static void setSplitEpno(boolean b) {
		splitEpno = b;
	}
	public static void setShowDetail(boolean b) {
		showDetail = b;
	}
	public static void setDetailTab(float n) {
		detailTab = n;
	}

	// フォントスタイル
	public static void setTitleFont(String fn) {
		if ( fn != null && ! fn.equals("") ) {
			Font f = new Font(fn,titleFontStyle,titleFontSize);
			if ( f != null ) {
				titleFont = f;
				return;
			}
		}
		//titleFont = this.getFont();
	}
	public static void setTitleFontSize(int n) {
		titleFontSize = n;
		titleFont = titleFont.deriveFont((float)titleFontSize);
	}
	public static void setTitleFontColor(Color c) {
		titleFontColor = c;
	}
	public static void setDetailFont(String fn) {
		if ( fn != null && ! fn.equals("") ) {
			Font f = new Font(fn,detailFontStyle,detailFontSize);
			if ( f != null ) {
				detailFont = f;
				startFont = f.deriveFont(Font.BOLD);
				return;
			}
		}
		//detailFont = new JLabel().getFont();
	}
	public static void setDetailFontSize(int n) {
		detailFontSize = n;
		detailFont = detailFont.deriveFont((float)detailFontSize);
		startFont = startFont.deriveFont((float)detailFontSize);
	}
	public static void setDetailFontColor(Color c) {
		detailFontColor = c;
	}

	// フォントスタイルの変更
	public static void setTitleFontStyle(ArrayList<FontStyle> fsa) {
		titleFont = setFontStyle(titleFont, (float)titleFontSize, fsa);
	}
	public static void setDetailFontStyle(ArrayList<FontStyle> fsa) {
		detailFont = setFontStyle(detailFont, (float)detailFontSize, fsa);
	}

	private static Font setFontStyle(Font f, float size, ArrayList<FontStyle> fsa) {
		Map<TextAttribute, Object>  attributes = new HashMap<TextAttribute, Object>();
		attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
		attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
		attributes.remove(TextAttribute.UNDERLINE);
		for ( FontStyle fs : fsa ) {
			switch (fs) {
			case BOLD:
				attributes.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
				break;
			case ITALIC:
				attributes.put(TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
				break;
			case UNDERLINE:
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);//LOW_ONE_PIXEL);
				break;
			}
		}
		attributes.put(TextAttribute.SIZE, size);
		return f.deriveFont(attributes);
	}

	// フォントエイリアスの変更
	public static void setAAHint(Object o) {
		frc = new FontRenderContext(null, o, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
	}


	/*******************************************************************************
	 * メソッド
	 ******************************************************************************/

	/**
	 * ビットマップの描画処理
	 */
	@Override
	protected void paintComponent(Graphics g) {

		super.paintComponent(g);

		// 初回描画時
		if (image == null) {
			//
			Dimension  d  = getSize();
			int imgw = d.width;
			int imgh = d.height;

			float draww = (float)imgw-DRAWTAB*2.0F;
			float drawh = (float)imgh;

			image = new BufferedImage(imgw, imgh, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = (Graphics2D)image.createGraphics();

			g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_SPEED);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

			float baseline = 0.0F;

			// 開始時刻と延長警告の描画
			if (showStart && tvd.start != null && tvd.start.length() > 0) {
				FontMetrics fm = g2.getFontMetrics(startFont);
				float hi = Float.valueOf(fm.getHeight());
				float as = Float.valueOf(fm.getAscent());

				float startx = Float.valueOf(DRAWTAB);
				float startw = draww;
				float xposstartx = 0.0F;

				baseline = as;	// 初期垂直位置

				{
					WrappedGlyphVector wgv = getWrappedGlyphVector(tvd.start, startw, xposstartx, startFont, as, frc);
					GlyphVector gv = wgv.getGv();
					g2.setPaint(Color.BLACK);
					g2.drawGlyphVector(gv, startx, baseline);

					xposstartx = wgv.getLastX();	// 後続有り
					baseline += wgv.getLastY();
				}

				{
					WrappedGlyphVector wgv = getWrappedGlyphVector(" "+tvd.extension_mark, startw, xposstartx, startFont, as, frc);
					GlyphVector gv = wgv.getGv();
					g2.setPaint(Color.RED);
					g2.drawGlyphVector(gv, startx, baseline);

					baseline += wgv.getLastY();
				}

				baseline += hi;
			}

			// タイトルの描画
			String title = ( splitEpno ) ? tvd.splitted_title : tvd.title;
			if ( title.length() > 0 ) {
				//
				String aMark;
				if (showStart && tvd.start.length() > 0) {
					aMark = tvd.prefix_mark + tvd.newlast_mark;
				}
				else {
					if (tvd.start.length() > 0) {
						aMark = tvd.extension_mark + tvd.prefix_mark + tvd.newlast_mark;
					}
					else {
						aMark = tvd.prefix_mark + tvd.newlast_mark;
					}
				}

				FontMetrics fm = g2.getFontMetrics(titleFont);
				float hi = Float.valueOf(fm.getHeight());
				float as = Float.valueOf(fm.getAscent());

				float titlex = Float.valueOf(DRAWTAB);
				float titlew = draww;
				float xpos = 0.0F;

				if ( baseline == 0.0F ) {
					baseline = as;	// 初期垂直位置
				}

				if ( aMark.length() > 0 ) {
					WrappedGlyphVector wgv = getWrappedGlyphVector(aMark, titlew, xpos, titleFont, as, frc);
					GlyphVector gv = wgv.getGv();
					g2.setPaint(Color.RED);
					g2.drawGlyphVector(gv, titlex, baseline);

					xpos = wgv.getLastX();	// 後続有り
					baseline += wgv.getLastY();
				}

				{
					WrappedGlyphVector wgv = getWrappedGlyphVector(title+tvd.postfix_mark, titlew, xpos, titleFont, as, frc);
					GlyphVector gv = wgv.getGv();
					g2.setPaint(titleFontColor);

					drawString(g2, wgv, titlex, baseline);

					baseline += wgv.getLastY();
				}

				baseline += hi;
			}

			// 番組詳細の描画
			if ( showDetail ) {
				String detail;
				if ( splitEpno ) {
					detail = tvd.splitted_detail;
				}
				else {
					detail = tvd.detail;
				}

				/*
				FontMetrics fm = g2.getFontMetrics(detailFont);
				int hi = fm.getAscent() + fm.getDescent();
				int as = fm.getAscent();
				int detailx = (int) (DRAWTAB+detailTab);
				int detailw = (int) (draww-detailTab);

				if ( baseline == 0.0F ) {
					baseline = as;	// 初期垂直位置
				}

				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setColor(detailFontColor);
				CommonSwingUtils.drawWrappedString(g2, detailx, (int)baseline, detailw, (int)drawh, detail, fm, hi, false);
				*/

				FontMetrics fm = g2.getFontMetrics(detailFont);
				float as = Float.valueOf(fm.getAscent());
				float detailx = Float.valueOf(DRAWTAB+detailTab);
				float detailw = draww-detailTab;

				if ( baseline == 0.0F ) {
					baseline = as;	// 初期垂直位置
				}

				WrappedGlyphVector wgv = getWrappedGlyphVector(detail, detailw, 0.0f, detailFont, as, frc);
				g2.setPaint(detailFontColor);

				drawString(g2, wgv, detailx, baseline);
			}
		}

		// 反映
		g.drawImage(image, 0, 0, this);
	}

	/**
	 *
	 */
	private void drawString(Graphics2D g2, WrappedGlyphVector wgv, float x, float y) {
		g2.drawGlyphVector(wgv.getGv(), x, y);

		if ( wgv.getGv().getFont().getAttributes().get(TextAttribute.UNDERLINE) != null ) {
			for ( Rectangle r : wgv.getLinePositions() ) {
				g2.drawLine((int)x+r.x, (int)y+r.y+1, (int)x+r.x+r.width-1, (int)y+r.y+1);
			}
		}
	}

	/**
	 * 参考：てんぷらメモ／JTableのセル幅で文字列を折り返し  ( http://terai.xrea.jp/Swing/TableCellRenderer.html )
	 * @param str			描画する文字列
	 * @param width			描画領域の幅
	 * @param xstart		１行目の描画開始位置
	 * @param font			描画フォント
	 * @param lineHeight	１行あたりの高さ
	 * @param frc			FontRenderContext
	 * @return
	 */
    private WrappedGlyphVector getWrappedGlyphVector(String str, float width, float xstart, Font font, float lineHeight, FontRenderContext frc) {
        Point2D gmPos    = new Point2D.Double(0.0d, 0.0d);
        GlyphVector gv   = font.createGlyphVector(frc, str);
        WrappedGlyphVector wgv = new WrappedGlyphVector(gv);
        float xpos       = xstart;
        float ypos       = 0.0F;
        float advance    = 0.0F;
        GlyphMetrics gm;
        for( int i=0; i <= gv.getNumGlyphs(); i++ ) {
        	if ( i == gv.getNumGlyphs() ) {
        		int x = (int) ((ypos == 0.0F) ? xstart : 0.0F);
        		int y = (int) ypos;
        		int w = (int) (xpos - x);
        		wgv.addLinePosition(new Rectangle(x, y, w, 1));
        		break;
        	}
            gm = gv.getGlyphMetrics(i);
            advance = gm.getAdvance();
            if( xpos < width && width <= xpos+advance ) {
        		int x = (int) ((ypos == 0.0F) ? xstart : 0.0F);
        		int y = (int) ypos;
        		int w = (int) (xpos - x);
        		wgv.addLinePosition(new Rectangle(x, y, w, 1));
                ypos += lineHeight;
                xpos = 0.0f;
            }
            gmPos.setLocation(xpos, ypos);
            gv.setGlyphPosition(i, gmPos);
            xpos = xpos + advance;

            wgv.setLastX(xpos);
            wgv.setLastY(ypos);
        }
        return wgv;
    }

    private class WrappedGlyphVector {

    	public WrappedGlyphVector(GlyphVector gv) {
    		super();
    		this.gv = gv;
    	}

    	private GlyphVector gv;

    	public GlyphVector getGv() { return gv; }

    	private float lastx;
    	private float lasty;

    	public void setLastX(float x) { lastx = x; }
    	public float getLastX() { return lastx; }
    	public void setLastY(float y) { lasty = y; }
    	public float getLastY() { return lasty; }

    	private ArrayList<Rectangle> linePositions = new ArrayList<Rectangle>();
    	public ArrayList<Rectangle> getLinePositions() { return linePositions; }
    	public void addLinePosition(Rectangle r) { linePositions.add(r); }
    }

    @Override
    public String getToolTipText(MouseEvent e){
		// ツールチップを付加する
		if (! Env.TheEnv.getTooltipEnable() || tvd == null || tvd.title.isEmpty() || tvd.start.isEmpty())
			return null;

		int tlen = Bounds.TheBounds.getTooltipWidth()*15;
		StringBuilder sb = new StringBuilder("");

		sb.append("<html>");
		sb.append(tvd.start + "～" + tvd.end + " (" + tvd.recmin + "分)");
		sb.append("&nbsp;<FONT COLOR=RED><EM>" + tvd.extension_mark + "</EM></FONT><BR>");

		sb.append("<DIV width=\"" + String.valueOf(tlen+10) + "\"><STRONG><U><FONT COLOR=RED>");
		sb.append(tvd.prefix_mark + tvd.newlast_mark);
		sb.append("</FONT><FONT COLOR=BLUE>");
		sb.append(tvd.title + tvd.postfix_mark);
		sb.append("</FONT></U></STRONG></DIV>");

		sb.append("<DIV style=\"margin-left: 10px;\" width=\"" + String.valueOf(tlen) + "\">");
		sb.append(tvd.detail);
		sb.append("</DIV></html>");

		return sb.toString();
    }
}
