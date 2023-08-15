package tainavi;

import java.io.File;
import java.util.ArrayList;

public class ChannelSort {

	/*
	 * 定数
	 */
	
	private final String filename = "env"+File.separator+"chsort.xml";
	
	private final String MSGID = "[CHソート設定] ";
	private final String ERRID = "[ERROR]"+MSGID;
	
	/*
	 * 部品
	 */
	private ArrayList<Center> clst = new ArrayList<Center>();
	
	//public boolean isEnabled() { return (clst != null); }
	public int size() { return clst.size(); }
	
	public boolean add(Center cr) { return clst.add(cr); }
	
	/**
	 * ソート済みの放送局一覧を返す
	 */
	public ArrayList<Center> getClst() {
		return clst;
	}
	
	public void clear() {
		clst.clear();
	}
	
	public boolean load() {
		
		//System.out.println(MSGID+"設定を読み込みます: "+filename);
		
		if ( ! new File(filename).exists() ) {
			System.err.println(ERRID+"設定が読み込めませんでした、CHソートは無効です: "+filename);
			return false;
		}
		
		@SuppressWarnings("unchecked")
		ArrayList<Center> tClst = (ArrayList<Center>) CommonUtils.readXML(filename);
		if ( tClst == null ) {
			System.err.println(ERRID+"設定の読み込みに失敗しました、CHソートは無効です: "+filename);
			return false; 
		}
		
		System.out.println(MSGID+"設定を読み込みました: "+filename);
		
		clst = tClst;
		return true;
	}

	public boolean save() {
		if ( ! CommonUtils.writeXML(filename, clst) ) {
			System.err.println(ERRID+"設定の保存に失敗しました： "+filename);
			return false;
		}
		return true;
	}
	
	/*
	public boolean delete() {
		clst = new ArrayList<Center>();
		return new File(filename).delete();
	}
	*/
	
}
