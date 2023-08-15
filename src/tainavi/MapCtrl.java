package tainavi;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * フリーオプション文字列を操作するクラス
 * @since 3.15
 */
abstract public class MapCtrl {

	/*
	 * 抽象メソッド
	 */
	
	/**
	 * 呼び出し元固有の内容チェック
	 */
	abstract protected boolean chkOptString();
	
	/*
	 * 定数
	 */
	
	public static enum KeyType { DIR, FILE, URL, SYMLINK, DEC, HEX, BOOLEAN, OTHER };
	
	public static final String MSGID = "[MapCtrl] ";
	public static final String ERRID = "[ERROR]"+MSGID;
	public static final String DBGID = "[DEBUG]"+MSGID;
	
	
	/*
	 * 
	 */
	private HashMap<String,String> map = new HashMap<String, String>();
	private HashMap<String,KeyType> optkeys = new HashMap<String, KeyType>();
	private String fname = null;

	/*
	 * 公開メソッド
	 */
	
	/**
	 * 保存先を決定する
	 */
	public void setFilename(String s) {
		fname = s;
	}

	/**
	 * キーリストを登録する
	 */
	public KeyType putdef(String key, KeyType keytype) {
		return optkeys.put(key, keytype);
	}

	/**
	 *  ちょっとだけみせちゃう
	 */
	public String get(String key) {
		return map.get(key);
	}

	/**
	 *  ぜんぶみせちゃう
	 */
	@Override
	public String toString() {
		String s = "";
		for ( String key : map.keySet() ) {
			if ( map.get(key) != null ) {
				s += key+"="+map.get(key)+";";
			}
		}
		return s;
	}
	
	/**
	 * フリーワードオプションの操作（起動時）
	 */
	public boolean initOptString() {
		
		// ロード
		if ( ! load() ) {
			// 呼び出し元固有のチェック
			chkOptString();
			return false;
		}
		
		// 呼び出し元固有のチェック
		chkOptString();
		
		// 保存はしない
		return true;
	}
	
	/**
	 * フリーワードオプションの操作（更新時）
	 */
	public boolean setOptString(String s) {
		
		if ( s == null ) {
			map = new HashMap<String, String>();
		}
		else {
			// フリーテキストを変換
			if ( ! set(s) ) {
				//
			}
		}

		// 呼び出し元固有のチェック
		chkOptString();
		
		// 保存する
		if ( ! save() ) {
			return false;
		}
		return true;
	}
	
	/*
	 * 共有メソッド
	 */
	
	/**
	 * put into it
	 */
	protected void put(String key, String value) {
		for ( String k : optkeys.keySet() ) {
			if ( k.equals(key) ) {
				map.put(key, value);
				return;
			}
		}
	}

	// いるのか？
	protected Set<String> keySet() {
		return optkeys.keySet();
	}
	
	// いるのか？
	protected KeyType getType(String key) {
		return optkeys.get(key);
	}
	
	// いるのか？
	protected void remove(String key) {
		map.remove(key);
	}
	
	// いるのか？
	protected void clear() {
		map.clear();
	}
	
	/*
	 * 非公開メソッド
	 */
	
	/**
	 * 入力文字列を分解する
	 */
	private boolean set(String input) {
		map = new HashMap<String, String>();
		Matcher ma = Pattern.compile("(.+?)=(.+?);",Pattern.DOTALL).matcher(input);
		while ( ma.find() ) {
			map.put(ma.group(1).trim().toLowerCase(), ma.group(2).trim());
		}
		
		refresh();
		
		return checktype();
	}
	
	/**
	 * 定義されたキー以外が含まれていたら捨てる
	 */
	private void refresh() {
		HashMap<String,String> newmap = new HashMap<String, String>();
		for ( Entry<String, String> entry : map.entrySet() ) {
			for ( String key : optkeys.keySet() ) {
				if ( map.containsKey(key) ) {
					newmap.put(entry.getKey(), entry.getValue());
					break;
				}
			}
		}
		map = newmap;
	}
	
	/**
	 * 型をチェックする
	 */
	private boolean checktype() {
		HashMap<String,String> newmap = new HashMap<String, String>();
		for ( Entry<String, KeyType> entry : optkeys.entrySet() ) {
			//
			String val = map.get(entry.getKey());
			if ( val == null ) {
				System.err.println(ERRID+"キーがみつかりません： "+entry.getKey()+", "+entry.getValue());
				continue;
			}
			//
			try {
				switch ( entry.getValue() ) {
				case DIR:
					File fd = new File(val);
					if ( ! fd.isDirectory() ) {
						System.err.println(ERRID+"存在しないかディレクトリではありません： "+entry.getKey()+"="+val);
						continue;
					}
					val = fd.getCanonicalPath();
					if ( ! val.endsWith(File.separator)) val += File.separator;
					val = val.replace("\\", "/");
					break;
				case FILE:
					File ff = new File(val);
					if ( ! ff.isFile() ) {
						System.err.println(ERRID+"存在しないかファイルではありません： "+entry.getKey()+"="+val);
						continue;
					}
					val = ff.getCanonicalPath();
					if ( ! val.endsWith(File.separator)) val += File.separator;
					val = val.replace("\\", "/");
					break;
				case DEC:
					Integer.decode(val);
					break;
				case HEX:
					val = "0x"+val;
					Integer.decode(val);
					break;
				default:
					// のーちぇっく
					break;
				}
			}
			catch (NumberFormatException e) {
				System.err.println(ERRID+"数字の形式が不正出です： "+entry.getKey()+"="+val);
				continue;
			}
			catch (Exception e) {
				System.err.println(e.toString()+" "+entry.getKey()+", "+entry.getValue());
				continue;
			}
			
			System.out.println(DBGID+"有効なオプション： "+entry.getKey()+"="+val);
			newmap.put(entry.getKey(), val);
		}
		
		map = newmap;
		
		return true;
	}
	
	/**
	 *  保存
	 */
	private boolean save() {
		if ( fname == null ) {
			return true;
		}
		return CommonUtils.writeXML(fname, map);
	}

	/**
	 *  読み出し
	 */
	private boolean load() {
		if ( fname == null ) {
			map = new HashMap<String, String>();
			return true;
		}
		
		if ( ! new File(fname).exists() ) {
			return false;
		}
		@SuppressWarnings("unchecked")
		HashMap<String,String> newmap = (HashMap<String, String>) CommonUtils.readXML(fname);
		if ( newmap == null ) {
			return false;
		}
		map = newmap;
		
		refresh();
		
		return true;
	}

}
