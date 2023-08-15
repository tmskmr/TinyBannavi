package tainavi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * しょぼかるの。
 */
public class ContentIdSyobo {

	public static final String CIDMARK = "SYOBO$";
	
	private static int tid = 0;
	private static int pid = 0;
	
	private static String EDCBID = null;

	public static boolean isValid(String link) {
		return (link!=null)?(link.matches(".*/tid/\\d+#\\d+$")):(false);
	} 

	public static int getTId() { return tid; }
	public static int getPId() { return pid; }

	public static String getContentId(int tid, int pid) {
		return String.format("%s%d,%d",CIDMARK,tid,pid);
	}

	public static boolean decodeContentId(String link) {
		if ( isValid(link) ) {
			Matcher ma = Pattern.compile("/tid/(\\d+)#(\\d+)$").matcher(link);
			if ( ma.find() ) {
				try {
					tid = Integer.valueOf(ma.group(1));
					pid = Integer.valueOf(ma.group(2));
					return true;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		tid = 0;
		pid = 0;
		return false;
	}
}
