package tainavi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EpgDataCap_BonプラグインとEDCBプラグインで利用するコンテンツIDを操作するstaticメソッドの集合
 */
public class ContentIdREGZA extends ContentIdEDCB {

	public static final String CIDMARK = "REGZA$";
	private static final int chid_len = 12;
	private static final int evid_len = 4;

	public static boolean isValid(String cId) {
		return (cId!=null)?(cId.startsWith(CIDMARK) && cId.length()>=CIDMARK.length()+chid_len+evid_len):(false);
	}
	
	public static String stripMark(String edcbid) { return edcbid.substring(CIDMARK.length()); }
	
	/**
	 * １０進表記のChIDを１６進表記に変更
	 */
	public static String getChId(String str) {
		Matcher ma = Pattern.compile("^(\\d+):(\\d+):(\\d+)$").matcher(str);
		if ( ma.find() ) {
			return String.format("%04X%04X%04X", Integer.valueOf(ma.group(2)), 0, Integer.valueOf(ma.group(3)));
		}
		return null;
	}
	
	public static String getContentId(String chid, int evid) {
		return String.format("%s%s%04X",CIDMARK,chid,evid);
	}
	
	public static boolean decodeContentId(String cId) {
		if ( isValid(cId) ) {
			try {
				String xId = stripMark(cId);
				if ( ! decodeChId(xId) ) {
					return false;
				}
				event_id = Integer.decode("0x"+xId.substring(chid_len,chid_len+evid_len));
				return true;
			}
			catch ( Exception e ) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
