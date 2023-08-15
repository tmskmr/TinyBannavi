package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <P>レコーダで利用するCHコード設定を保持するクラス。
 * <P>NAME : Web番組表の放送局名
 * <P>NO : レコーダの放送局名
 * <P>CODE : 放送局コード
 * @version 3.15.4β EDCBの旧型式→新形式自動変換追加、DIGAの旧型式→新形式自動変換廃止ほか
 */
public class ChannelCode extends ArrayList<String> implements Cloneable {
	
	private static final long serialVersionUID = 1L;
	
	private final String datfile = "env"+File.separator+"RdChannelCode.dat";
	private String recId = null;
	
	private static final String MSGID = "[CHコード設定] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	private static final String DBGID = "[DEBUG]"+MSGID;

	private final HashMap<String,String> CH_REC2WEB = new HashMap<String,String>();
	private final HashMap<String,String> CH_CODE2REC = new HashMap<String,String>();
	private final HashMap<String,String> CH_WEB2CODE = new HashMap<String,String>();

	public void setCH_REC2WEB(String key, String val) { CH_REC2WEB.put(key, val); }
	public void setCH_CODE2REC(String key, String val) { CH_CODE2REC.put(key, val); }
	public void setCH_WEB2CODE(String key, String val) { CH_WEB2CODE.put(key, val); }
	
	/**
	 * レコーダの放送局名　を　Web番組表の放送局名　に変換する。
	 */
	public String getCH_REC2WEB(String recChName) { return (String)CH_REC2WEB.get(recChName); }
	
	/**
	 * Web番組表の放送局名　を　レコーダの放送局名　に変換する。
	 */
	public String getCH_WEB2REC(String webChName) {
		for ( Entry<String,String> e : CH_REC2WEB.entrySet() ) {
			if ( e.getValue().equals(webChName) ) {
				return e.getKey();
			}
		}
		return null;
	}
	
	/**
	 * 放送局コード　を　レコーダの放送局名　に変換する。
	 */
	public String getCH_CODE2REC(String chCode) { return (String)CH_CODE2REC.get(chCode); }
	
	/**
	 * レコーダの放送局名　を　放送局コード　に変換する。
	 */
	public String getCH_REC2CODE(String recChName) {
		for ( Entry<String,String> e : CH_CODE2REC.entrySet() ) {
			if ( e.getValue().equals(recChName) ) {
				return e.getKey();
			}
		}
		return null;
	}
	
	/**
	 *　Web番組表の放送局名　を　 放送局コード　に変換する。
	 */
	public String getCH_WEB2CODE(String webChName) { return (String)CH_WEB2CODE.get(webChName); }
	
	/**
	 *　 放送局コード　を　Web番組表の放送局名　に変換する。
	 */
	public String getCH_CODE2WEB(String chCode) {
		for ( Entry<String,String> e : CH_WEB2CODE.entrySet() ) {
			if ( e.getValue().equals(chCode) ) {
				return e.getKey();
			}
		}
		return null;
	}
	
	// コンストラクタ
	public ChannelCode(String recId) {
		this.recId = recId; 
	}
	
	/**
	 * Web番組表の放送局名のリストを返す 
	 */
	public ArrayList<String> getChNames() {
		return this;
	}
	
	/**
	 * 読み出します。
	 */
	public boolean load(boolean logging)
	{
		// 領域初期化
		CH_REC2WEB.clear();
		CH_CODE2REC.clear();
		CH_WEB2CODE.clear();
		
		// 読み込み
		String ccFile = datfile+"."+recId;
		
		if ( ! new File(ccFile).exists() ) {
			System.err.println(ERRID+"設定ファイルがみつかりません： "+ccFile);
			return false;
		}
		
		this.clear();
		
		String buf = CommonUtils.read4file(ccFile, false);
		if ( buf == null ) {
			System.err.println(ERRID+"設定ファイルの取得に失敗しました： "+ccFile);
			return false;
		}
		
		String[] codes = buf.split("[\r\n]+");
		for ( String code : codes ) {
			Matcher ma = Pattern.compile("^\\s*\"(.+?)\",\"(.+?)\",\"(.+?)\"").matcher(code);
			if (ma.find()) {
				if (getCH_WEB2CODE(ma.group(1)) == null) setCH_WEB2CODE(ma.group(1),ma.group(3));
				if (getCH_REC2WEB(ma.group(2)) == null || ma.group(1).startsWith("外部入力")) setCH_REC2WEB(ma.group(2),ma.group(1));
				if (getCH_CODE2REC(ma.group(3)) == null) setCH_CODE2REC(ma.group(3),ma.group(2));
				
				this.add(ma.group(1));
			}
		}
		
		boolean oldflag = false;
		/* - もういらない
		if ( recId.equals("EpgDataCap_Bon") ) {	// 本当はプラグインからレコーダIDの文字列を取得したかったのだけど
			oldflag = ccModEDCB();
		}
		*/
		if ( recId.equals("TVTest") ) {
			oldflag = ccModTVTest();
		}
		if ( oldflag ) {
			System.out.println(MSGID+"CHコード設定が自動修正されました： "+ccFile);
			this.save();
		}
		
		if (logging) {
			System.out.println(DBGID+"--- "+ccFile+" Start---");
			
			String[] keys = CH_REC2WEB.keySet().toArray(new String[0]);
			for ( String recChName : keys ) {
				String webChName = CH_REC2WEB.get(recChName);
				String chCode = CH_WEB2CODE.get(webChName);
				if ( chCode.equals("error") ) {
					System.out.printf("CH: %7s %-32s %-8s %s\n","<Error>",webChName,recChName,chCode);
				}
				else {
					System.out.printf("CH: %7s %-32s %-8s %s\n","",webChName,recChName,chCode);
				}
			}
			
			System.out.println(DBGID+"--- "+ccFile+" End ---");
		}
		
		return true;
	}

	/**
	 * 古いEDCBの情報を新形式に更新する。
	 */
	/*
	private boolean ccModEDCB() {
		boolean oldflag = false;
		String[] keys = CH_REC2WEB.keySet().toArray(new String[0]);
		for ( String recChName : keys ) {
			if ( ! recChName.matches("^\\d+$") ) {
				oldflag = true;
				String webChName = CH_REC2WEB.get(recChName);
				String chCode = CH_WEB2CODE.get(webChName);
				CH_REC2WEB.remove(recChName);
				CH_CODE2REC.remove(chCode);
				CH_REC2WEB.put(chCode,webChName);
				CH_CODE2REC.put(chCode,chCode);
			}
		}
		return oldflag;
	}
	*/

	/**
	 * 古いEDCBの情報を新形式に更新する。
	 */
	private boolean ccModTVTest() {
		boolean oldflag = false;
		if (CH_REC2WEB.remove("-") != null) {
			oldflag = true;
			String[] keys = CH_CODE2REC.keySet().toArray(new String[0]);
			for ( String chCode : keys ) {
				String recChName = chCode;
				CH_CODE2REC.remove(chCode);
				CH_CODE2REC.put(chCode, recChName);
				for ( Entry<String,String> ent : CH_WEB2CODE.entrySet() ) {	// NAME2CODEは変更の必要がない
					if ( ent.getValue().equals(chCode) ) {
						String webChName = ent.getKey();
						CH_REC2WEB.put(recChName,webChName);
						break;
					}
				}
			}
		}
		return oldflag;
	}
	
	/**
	 * 保存します。
	 */
	public boolean save(ArrayList<String> webChNames, ArrayList<String> recChNames, ArrayList<String> chCodes) {
		
		String ccFile = datfile+"."+recId;
		
		System.err.println(MSGID+"設定ファイルを保存します： "+ccFile);
		StringBuilder sb = new StringBuilder();
		sb.append("# "+CommonUtils.getDateTime(0)+"\n");
		for ( int i=0; i<webChNames.size(); i++ ) {
			sb.append(String.format("\"%s\",\"%s\",\"%s\"\n", webChNames.get(i), recChNames.get(i), chCodes.get(i)));
		}
		if ( ! CommonUtils.write2file(ccFile, sb.toString()) ) {
			System.err.println(MSGID+"設定ファイルの保存に失敗しました： "+ccFile);
			return false;
		}
		return true;
	}

	/**
	 * 自動修正時に利用するよ
	 */
	private boolean save() {
		ArrayList<String> webChNames = new ArrayList<String>();
		ArrayList<String> recChNames = new ArrayList<String>();
		ArrayList<String> chCodes = new ArrayList<String>();
		for ( String webChName : this ) {
			String chCode = CH_WEB2CODE.get(webChName);
			String recChName = CH_CODE2REC.get(chCode);
			
			webChNames.add(webChName);
			recChNames.add(recChName);
			chCodes.add(chCode);
		}
		return this.save(webChNames, recChNames, chCodes);
	}

	//
	@Override
	public ChannelCode clone() {
		ChannelCode cc = (ChannelCode) super.clone();
		FieldUtils.deepCopy(cc, this); // ディープコピーするよ
		return cc;
	}
}
