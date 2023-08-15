package tainavi;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelConvert extends HashMap<String,String> {

	private static final long serialVersionUID = 1L;
	
	private static boolean debug = false;
	
	public final static String chFilterFile = "env"+File.separator+"ChannelConvert.dat";
	
	/**
	 * 一致するものがなければそのまま返す
	 */
	@Override
	public String get(Object from) {
		String to = super.get(from);
		return ((to!=null)?(to):((String)from));
	}
	
	//
	public boolean load()
	{
		if ( ! new File(chFilterFile).exists() ) {
			System.err.println("[ChannelConvert]ファイルがない： "+chFilterFile);
			return false;
		}
		String s = CommonUtils.read4file(chFilterFile, false);
		if ( s == null ) {
			System.err.println("[ChannelConvert]ファイルの取得に失敗： "+chFilterFile);
			return false;
		}
		
		this.clear();

		int lineno = 0;
		String[] d = s.split("[\\r\\n]+");
		for ( String v : d ) {
			lineno++;
			v = v.trim();
			if ( v.startsWith("#") ) {
				continue;
			}
			Matcher ma = Pattern.compile("\"(.+?)\"\\s*,\\s*\"(.+?)\"",Pattern.DOTALL).matcher(v);
			if ( ma.find() ) {
				if (this.containsKey(ma.group(1))) {
					if (this.get(ma.group(1)).equals(ma.group(2))) {
						System.err.println("【警告】重複したエントリーが存在します("+chFilterFile+"): key="+ma.group(1)+" lineno="+lineno);
					}
					else {
						System.err.println("【警告】重複したキーに異なる値が設定されています("+chFilterFile+"): key="+ma.group(1)+" lineno="+lineno);
					}
				}
				
				this.put(ma.group(1), ma.group(2));
			}
		}
		for ( String val : this.values() ) {
			if ( this.containsKey(val) ) {
				System.err.println("【警告】値が他のエントリのキーと同じです("+chFilterFile+"): val="+val+" lineno=?");
			}
		}
		
		if (debug) {
			for ( String key : this.keySet() ) {
				System.err.println(String.format("[DEBUG] channelconvert from=%s to=%s",key,this.get(key)));
			}
		}
		
		return true;
	}
	
	//
	/* まだない
	public boolean save()
	{
	}
	*/
	
}
