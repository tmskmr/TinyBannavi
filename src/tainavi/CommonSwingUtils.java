package tainavi;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.SpringLayout;

import tainavi.Env.SnapshotFmt;

/**
 * Swingを使う上で利用できる共通のロジックをstaticメソッドとしてまとめたもの
 */
public class CommonSwingUtils {

	public void setDebug(boolean b) { debug = b; }
	private static boolean debug = false;
	
	private static final String MSGID = "[Swing共通] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;
	
	/**
	 *  パネルに部品を貼り付ける
	 */
	public static void putComponentOn(JPanel p, JComponent c, int width, int height, int x, int y) {
		c.setPreferredSize(new Dimension(width, height));
		((SpringLayout)p.getLayout()).putConstraint(SpringLayout.NORTH, c, y, SpringLayout.NORTH, p);
		((SpringLayout)p.getLayout()).putConstraint(SpringLayout.WEST, c, x, SpringLayout.WEST, p);
		p.add(c);
	}
	
	/**
	 *  親コンポーネントの中心に移動する
	 */
	public static void setLocationCenter(Component parent, Component c) {
		Point p2 = parent.getLocationOnScreen();
		Rectangle r2 = parent.getBounds();
		Rectangle r = c.getBounds();
		int x = p2.x+(r2.width-r.width)/2;
		int y = p2.y+(r2.height-r.height)/2;
		c.setLocation(x, y);
	}
	
	/**
	 *  親コンポーネントの下に移動する
	 */
	public static void setLocationUnder(Component parent, Component c) {
		Point p2 = parent.getLocationOnScreen();
		Rectangle r2 = parent.getBounds();
		Rectangle r = c.getBounds();
		int x = p2.x+(r2.width-r.width)/2;
		int y = p2.y+r2.height-10;
		c.setLocation(x, y);
	}
	
	/**
	 * jtableの選択行が表示可能な位置にくるようにスクロールさせる
	 */
	public static boolean setSelectedRowShown(JTable tbl) {
		
		Component parent = tbl.getParent();
		if ( ! (parent instanceof JViewport) ) {
			return false;
		}
		
		int row = tbl.getSelectedRow();
		JViewport viewport = (JViewport) parent;
		Rectangle ra = tbl.getCellRect(row, 0, true);
		Rectangle rb = viewport.getVisibleRect();
		tbl.scrollRectToVisible(new Rectangle(ra.x, ra.y, (int)rb.getWidth(), (int)rb.getHeight()));
		return true;
	}
	
	/**
	 * 謎のJTextArea
	 */
	public static JTextAreaWithPopup getJta(final Component parent, int rows, int columns) {
		JTextAreaWithPopup jta = new JTextAreaWithPopup(rows,columns);
		jta.setLineWrap(true);
		jta.setEditable(false);
		jta.setBackground(parent.getBackground());
		//jta.setBorder(new LineBorder(Color.BLACK));
		jta.setMargin(new Insets(0,0,0,0));
		jta.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		jta.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if ( "background".equals(e.getPropertyName()) ) {
					((JTextAreaWithPopup) e.getSource()).setBackground(parent.getBackground());
				}
			}
		});
		
		return jta;
	}
	
	/**
	 * 色セルの書式
	 */
	public static String getColoredString(String color, String text) {
		return text+"\0"+color;
	}
	public static String getColoredString(Color color, String text) {
		return text+"\0"+CommonUtils.color2str(color);
	}
	/**
	 * @return [0] テキスト [1] 色
	 */
	public static String[] splitColorString(String value) {
		return value.split("\0");
	}
	
	/**
	 * <P>こどもをおっきする
	 * <P><B>こどもは"jCombobox_update"のように先頭が"j"ではじまること！</B>
	 */
	@SuppressWarnings("rawtypes")
	public static void setEnabledAllComponents(Component own, Class c, boolean b) {
		Field[] fd = c.getDeclaredFields();
		for ( Field fx : fd ) {
			fx.setAccessible(true);
			if ( Modifier.isFinal(fx.getModifiers()) ) {
				continue;
			}
			try {
				Object o = fx.get(own);
				if ( o == null || ! (o instanceof Component) || ! fx.getName().startsWith("j") ) {
					continue;
				}
				if (debug) System.out.println(DBGID+"enabled class="+c.getName()+" name="+fx.getName());
				((Component)o).setEnabled(true);
			}
			catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}
	
	/**
	 * リスト形式と新聞形式のスナップショットを作成する
	 */
	public static void saveComponentAsJPEG(String label, Component header, Component sidebar, Component body, String filename, SnapshotFmt fmt, Component parent) {
		
		BufferedImage myImage = null;
		{
			if ( header != null && sidebar != null && body != null ) {
				Dimension hSize = header.getPreferredSize();
				Dimension sSize = sidebar.getPreferredSize();
				Dimension bSize = body.getPreferredSize();
				
				BufferedImage hdrImage = new BufferedImage(hSize.width, hSize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D gh = hdrImage.createGraphics();
				header.paint(gh);
				
				BufferedImage sideImage = new BufferedImage(sSize.width, sSize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D gs = sideImage.createGraphics();
				sidebar.paint(gs);
				
				Font f = parent.getFont();
				int fSize = f.getSize();
				
				myImage = new BufferedImage(sSize.width*2+bSize.width, fSize*2+hSize.height*2+bSize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = myImage.createGraphics();
				g2.setColor(new Color(0,0,0));
				g2.setBackground(new Color(255,255,255));
				
				g2.clearRect(0, 0, sSize.width*2+bSize.width, fSize*2+hSize.height*2+bSize.height);
				g2.drawString(label, 0, fSize);
				
				g2.translate(0, fSize*2);
				g2.drawImage(hdrImage, sSize.width, 0, parent);
				
				g2.translate(0, hSize.height);
				g2.drawImage(sideImage, 0, 0, parent);
				
				g2.translate(sSize.width, 0);
				body.paint(g2);
	
				g2.translate(bSize.width, 0);
				g2.drawImage(sideImage, 0, 0, parent);
	
				g2.translate(-bSize.width, bSize.height);
				g2.drawImage(hdrImage, 0, 0, parent);
			}
			else if ( header != null  && body != null ) {
				Dimension hSize = header.getPreferredSize();
				Dimension bSize = body.getPreferredSize();
				
				Font f = parent.getFont();
				int fSize = f.getSize();
				
				BufferedImage hdrImage = new BufferedImage(hSize.width, hSize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D gh = hdrImage.createGraphics();
				header.paint(gh);
				
				myImage = new BufferedImage(bSize.width, fSize*2+hSize.height+bSize.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g2 = myImage.createGraphics();
				g2.setColor(new Color(0,0,0));
				g2.setBackground(new Color(255,255,255));
				
				g2.clearRect(0, 0, bSize.width, fSize*2+hSize.height+bSize.height);
				g2.drawString(label, 0, fSize);
				
				g2.translate(0, fSize*2);
				g2.drawImage(hdrImage, 0, 0, parent);
				
				g2.translate(0, hSize.height);
				body.paint(g2);
			}
		}
		
		try {
			switch (fmt) {
			case JPG:
				JPEGImageWriteParam param = new JPEGImageWriteParam(Locale.getDefault());
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.95f);
				ImageWriter iw = ImageIO.getImageWritersByFormatName("jpg").next();
				ImageOutputStream ios = ImageIO.createImageOutputStream(new File(filename));
				iw.setOutput(ios);
				iw.write(null, new IIOImage(myImage, null, null), param);
				ios.close();
				iw.dispose();
				//ImageIO.write(myImage, "jpg", new File(filename));	// 圧縮率を変更しない場合
				break;
			case BMP:
				ImageIO.write(myImage, "bmp", new File(filename));
				break;
			case PNG:
				ImageIO.write(myImage, "png", new File(filename));
				break;
			}
			myImage = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 左端で折り返す文字列の描画
	 */
	public static int drawWrappedString(Graphics2D g, int x, int baseline, int width, int height, String message, FontMetrics fm, int fontHeight, boolean center) {
		int top = 0;
		int bottom = top;
		int length = message.length();
		int strWidth = 0;
		int cWidth = 0;
		int drawX = 0;
		while  ( bottom < length && baseline < height) {
			String str;
			if ( cWidth == 0 ) {
				char c = message.charAt(bottom);
				cWidth = fm.charWidth(c);
			}
			if ( (strWidth+cWidth) > width ) {
				// 越えちゃった
				str = message.substring(top, bottom);
				// センタリング有無
				if ( center ) {
					drawX = (width-strWidth) / 2;
				}
				// 次の頭（cWidthは次のループで利用）
				top = bottom;
				strWidth = cWidth;
			}
			else if ( bottom+1 == length ) {
				// 残り全部
				str = message.substring(top, bottom+1);
				// センタリング有無
				if ( center ) {
					drawX = (width-strWidth) / 2;
				}
				// 次はない
				bottom++;
			}
			else {
				// まだ足りない
				bottom++;
				strWidth += cWidth;
				cWidth = 0;
				continue;
			}
			g.drawString(str, x+drawX, baseline);
			baseline += fontHeight;
		}
		return baseline;
	}

}
