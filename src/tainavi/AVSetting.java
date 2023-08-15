package tainavi;

import java.io.File;
import java.util.ArrayList;


public class AVSetting {
	
	//
	protected String avsettingFile = "";
	protected ArrayList<AVs> avs = new ArrayList<AVs>();
	
	// コンストラクタ
	public AVSetting() {
		avsettingFile = "env"+File.separator+"avsetting.xml";
	}
	
	// 公開メソッド
	public void save() {
    	System.out.println("画質・音質既定値設定を保存します: "+avsettingFile);
    	if ( ! CommonUtils.writeXML(avsettingFile, avs) ) {
        	System.err.println("画質・音質既定値設定の保存に失敗しました: "+avsettingFile);
    	}
	}
	
	public void load() {
    	if ( ! new File(avsettingFile).exists()) {
        	System.out.println("画質・音質既定値設定はありません: "+avsettingFile);
    		return;
    	}
    	
    	System.out.println("画質・音質既定値設定を読み込みます: "+avsettingFile);
    	
    	@SuppressWarnings("unchecked")
		ArrayList<AVs> tmp = (ArrayList<AVs>) CommonUtils.readXML(avsettingFile);
    	if ( tmp == null ) {
        	System.out.println("画質・音質既定値設定が読み込めなかったので登録なしで起動します.");
        	return;
    	}
    	
    	avs = tmp;
	}
	
	/**
	 * キーに紐づいた録画設定を取得する
	 */
	public AVs getSelectedAVs(String selected_key, String recId) {
		
		AVs myavs = get(recId, selected_key);
		if ( myavs != null ) {
			// キーに紐づいた設定がみつかた
			return myavs;
		}
		
		myavs = get(recId, null);
		if ( myavs != null ) {
			// デフォルトの設定がみつかた
			return myavs;
		}
	
		// みつかんねーよ
		return null;
	}
	
	private AVs get(String key_recorderId, String key_genre) {
		for ( AVs a : avs ) {
			if ( a.getRecorderId().equals(key_recorderId) ) {
				if ( (a.getGenre() != null && a.getGenre().equals(key_genre)) ||
						(a.getGenre() == null && key_genre == null)) {
					return(a);
				}
			}
		}
		return(null);
	}
	
	public void add(String key_recorderId, String key_genre, AVs c) {
		
		AVs a;
		while ( (a = this.get(key_recorderId,key_genre)) != null ) {
			avs.remove(a);
		}
		avs.add(c);
	}
}
