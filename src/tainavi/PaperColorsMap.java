package tainavi;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;

import tainavi.TVProgram.ProgGenre;

/**
 * 新聞形式タブのジャンル別背景色を保持する
 * @since 3.15.4β {@link paperColors}から移行
 */
public class PaperColorsMap extends HashMap<ProgGenre,Color> implements Cloneable {

	private static final long serialVersionUID = 1L;
	
	public static void setDebug(boolean b) {debug = b; }
	private static boolean debug = false;

	// 定数
	private static final String cFileOld = "env"+File.separator+"papercolors.xml";
	private static final String cFile = "env"+File.separator+"papercolorsmap.xml";
	
	private static final Color noneColor = new Color(255,255,255); 
	
	//
	//@Override
	public Color get(ProgGenre g) {
		Color c = super.get(g);
		if ( c != null ) {
			return(c);
		}
		return(noneColor);
	}
	
	/**
	 * 別にオーバーライドしなくてもいいけど
	 */
	@Override
	public Color put(ProgGenre key, Color value) {
		return super.put(key, value);
	}

	//
	public boolean load() {
		System.out.println("ジャンル別背景色設定を読み込みます: "+cFile);
		
		if ( ! new File(cFile).exists() ) {
			if ( new File(cFileOld).exists() ) {
				@SuppressWarnings("unchecked")
				HashMap<ProgGenre,Color> clx = (HashMap<ProgGenre,Color>) CommonUtils.readXML(cFileOld);
				if ( clx == null ) {
					System.err.println("ジャンル別背景色設定が読み込めなかったのでデフォルト設定で起動します.");
					if (this.size()==0) loadDefaults();
					return true;
				}
				
				//
				this.clear();
				for ( ProgGenre key : clx.keySet() ) {
					this.put(key, clx.get(key));
				}
				if ( this.save() ) {
					System.err.println("ジャンル別背景色設定ファイルを置き換えます： "+cFileOld+"->"+cFile);
					new File(cFileOld).delete();
				}
				System.out.println("ジャンル別背景色設定を読み込みました.");
				return true;
			}
		}
		else {
			PaperColorsMap cl = (PaperColorsMap) CommonUtils.readXML(cFile);
			if ( cl != null ) {
				System.out.println("ジャンル別背景色設定を読み込みました.");
				FieldUtils.deepCopy(this, cl);
				return true;
			}
		}
		
		System.err.println("ジャンル別背景色設定が読み込めなかったのでデフォルト設定で起動します.");
		loadDefaults();
		
		return false;
	}
	private void loadDefaults() {
		// デフォルト設定
		this.put(ProgGenre.NEWS, new Color(0xff,0xff,0xaa));
		this.put(ProgGenre.SPORTS, new Color(0x99,0x99,0x99));
		this.put(ProgGenre.DORAMA, new Color(0xcc,0xcc,0xcc));
		this.put(ProgGenre.MOVIE, new Color(0xcc,0xff,0xcc));
		this.put(ProgGenre.ANIME, new Color(0xff,0x99,0x00));
		this.put(ProgGenre.DOCUMENTARY, new Color(0xaa,0xff,0xff));
	}
	
	//
	public boolean save() {
		System.out.println("ジャンル別背景色設定を保存します: "+cFile);
		if ( CommonUtils.writeXML(cFile,this) ) {
			System.out.println("ジャンル別背景色設定を保存しました.");
			return true;
		}
		
		System.out.println("ジャンル別背景色設定の保存に失敗しました.");
		return false;
	}
	
	//
	@Override
	public PaperColorsMap clone() {
		PaperColorsMap map = (PaperColorsMap) super.clone();
		return map;
	}
}
