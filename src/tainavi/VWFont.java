package tainavi;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.JLabel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;


public class VWFont {

	private static boolean debug = false;
	
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	private static final String MSGID = "[フォント変更] ";
	private static final String DBGID = "[DEBUG]"+MSGID;
	private static final String ERRID = "[ERROR]"+MSGID;
	
	/*******************************************************************************
	 * メンバ変数関連
	 ******************************************************************************/
	
	public ArrayList<String> getNames() {
		return fontnames;
	}

	ArrayList<String> fontnames = null;

	/*******************************************************************************
	 * メソッド
	 ******************************************************************************/
	
	public String update(final String fn, final int fontSize) {
		
		//System.err.println("[DEBUG] VWFont#update "+fn+" "+fontSize);
		
		String font = getFontName(fn);
		
		UIDefaults defaults = UIManager.getDefaults();
		Enumeration<Object> keys = defaults.keys();
		
        while ( keys.hasMoreElements() ) {
        	Object key = keys.nextElement();
        	Object value = UIManager.get (key);
        	if ( value instanceof FontUIResource ) {
        		int fontstyle = ((FontUIResource) value).getStyle();
        		if ( font != null ) {
        			
        			FontUIResource to = new FontUIResource(font, fontstyle, fontSize);
            		if (debug) System.out.println(DBGID+"UIManagerの変更： key="+key.toString()+", from="+value.toString()+", to="+to.toString());
            		
            		defaults.put(key, to);
        		}
        		else {
        			
            		if (debug) System.out.println(DBGID+"変更されないUIManager： key="+key.toString()+", value="+value.toString());
            		
            		defaults.put(key, value);
        		}
        	}
        }
		
		return font;
	}
	
	/**
	 * fnで指定したフォントがフォントリストにあればそれを返す。なければデフォルトのフォント名を返す。
	 */
	private String getFontName(final String fn) {
		
		final String fWin7 = "Meiryo UI";
		final String fVista = "メイリオ";
		final String fWindows = "MS UI Gothic";
		final String fLinux = "Takao Pゴシック";
		//final String fMac = "Osaka";
		
		String fName = null;
		if ( ! fn.equals("")) {
			for ( String f : fontnames ) {
				if (f.equals(fn)) {
					fName = fn;
					System.out.println(MSGID+"指定のフォントは有効です="+fn);
					break;
				}
			}
		}
		if (fName == null) {
			for ( String f : fontnames ) {
				if ( CommonUtils.isWindowsXP() ) {
					if (fName == null && f.equals(fWindows)) {
						fName = fWindows;
					}
				}
				else if ( CommonUtils.isWindows() ) {
					if (f.equals(fWin7)) {
						fName = fWin7;
						break;
					}
					else if ((fName == null || ! fName.equals(fWin7)) && f.equals(fVista)) {
						fName = fVista;	// 優先度：低
					}
					else if (fName == null && f.equals(fWindows)) {
						fName = fWindows;	// 優先度：低
					}
				}
				else if ( CommonUtils.isLinux() ) {
					if (f.equals(fLinux)) {
						fName = fLinux;
						break;
					}
				}
				else if ( CommonUtils.isMac() ) {
					fName = new JLabel().getFont().getFontName();
					break;
				}
			}
			
			System.out.println(ERRID+"フォントリスト中に指定のフォントが見つからないのでデフォルトフォントを利用します：  指定="+fn+", デフォルト="+fName);
		}
		
		return fName;
	}
	
	//
	private void findFont() {
		
		fontnames = new ArrayList<String>();
		
		for ( String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames() ) {
			fontnames.add(f);
		}
		
	}
	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/
	
	public VWFont() {
		this.findFont();
	}
}
