package tainavi;

import java.io.File;
import java.util.ArrayList;

/**
 * {@link ClipboardInfo} のリストを実現するクラスです. 
 * @since 3.15.4β
 */
public class ClipboardInfoList extends ArrayList<ClipboardInfo> {

	private static final long serialVersionUID = 1L;
	
	private static final String cbFileOld = "env"+File.separator+"cbitems.xml";
	private static final String cbFile = "env"+File.separator+"cbinfolist.xml";
	
	public boolean save() {
		System.out.println("クリップボード設定を保存します: "+cbFile);
		if ( ! CommonUtils.writeXML(cbFile, this) ) {
        	System.err.println("クリップボード設定の保存に失敗しました： "+cbFile);
        	return false;
		}
		
		return true;
	}
	
	@SuppressWarnings("deprecation")
	public boolean load() {
		
		System.out.println("クリップボード設定を読み込みます: "+cbFile);

		boolean isoldclass = false;
		ArrayList<ClipboardInfo> cl = null;
		
		if ( ! new File(cbFile).exists() ) {
			// ファイルがなければデフォルトで
			if ( new File(cbFileOld).exists() ) {
				// 旧clipboardItem対策
				isoldclass = true;
				cl = new ArrayList<ClipboardInfo>();
				ArrayList<clipboardItem> clx = (ArrayList<clipboardItem>) CommonUtils.readXML(cbFileOld);
				for ( clipboardItem cx : clx ) {
					ClipboardInfo c = new ClipboardInfo();
					FieldUtils.deepCopy(c, cx);
					cl.add(c);
				}
			}
		}
		else {
			// ファイルがあるならロード
			cl = (ClipboardInfoList) CommonUtils.readXML(cbFile);
		}
		if ( cl == null || cl.size() == 0 ) {
			System.err.println("クリップボード設定が読み込めなかったのでデフォルト設定で起動します.");
			
	    	// 初期化してみよう
	    	this.clear();
	    	int idx = 1;
	    	Object[][] o = {
	    			{true,	"番組名",	idx++},
	    			{true,	"放送局",	idx++},
	    			{true,	"開始日",	idx++},
	    			{true,	"開始時刻",	idx++},
	    			{false,	"終了時刻",	idx++},
	    			{false,	"ジャンル",	idx++},
	    			{true,	"番組詳細",	idx++},
	    	};
	    	for (int i=0; i<o.length; i++) {
	    		ClipboardInfo cb = new ClipboardInfo();
	        	cb.setB((Boolean) o[i][0]);
	        	cb.setItem((String) o[i][1]);
	        	cb.setId((Integer) o[i][2]);
	        	this.add(cb);
	    	}
	    	
			return false;
		}
		
		this.clear();
		for (ClipboardInfo c : cl) {
			this.add(c);
		}
		
		if ( isoldclass && this.save() ) {
			System.err.println("クリップボード設定ファイルを置き換えます： "+cbFileOld+"->"+cbFile);
			new File(cbFileOld).delete();
		}
		
		return true;
	}

}
