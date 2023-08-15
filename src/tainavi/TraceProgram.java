package tainavi;

import java.io.File;
import java.util.ArrayList;

public class TraceProgram {

	private static final String MSGID = "[番組追跡設定] ";
	private static final String ERRID = "[ERROR]"+MSGID;
	
	//
	private final String traceKeyFile = "env"+File.separator+"tracekey.xml";
	private ArrayList<TraceKey> traceKeys = new ArrayList<TraceKey>();
	
	public static String getNewLabel(String title, String center) { return title.trim()+" ("+center+")"; }
	
	// 設定ファイルに書き出し
	public boolean save() {
		System.out.println(MSGID+"保存します: "+traceKeyFile);
		if ( ! CommonUtils.writeXML(traceKeyFile, traceKeys) ) {
			System.err.println(ERRID+"保存に失敗しました： "+traceKeyFile);
			return false;
		}
		return true;
	}
	
	// 設定ファイルから読み出し
	@SuppressWarnings("unchecked")
	public void load() {
		if ( ! new File(traceKeyFile).exists() ) {
			System.out.println(MSGID+"設定を読み込めなかったので登録なしで起動します： "+traceKeyFile);
			return;
		}
		
		System.out.println(MSGID+"読み込みます: "+traceKeyFile);
		
		traceKeys = (ArrayList<TraceKey>)CommonUtils.readXML(traceKeyFile);
		if ( traceKeys == null ) {
    		System.out.println(ERRID+"設定を読み込めなかったので登録なしで起動します： "+traceKeyFile);
    		return;
		}
		
		// ファイルに保存しないようにするので
        for (TraceKey tr : traceKeys) {
        	if ( tr.getTitle() == null ) {
        		int index = tr._getLabel().indexOf(" (");
        		tr.setTitle(index > 0 ? tr._getLabel().substring(0,index) : "");
        	}
        	
        	tr.setTitlePop(replacePop(tr.getTitle()));
        	
        	tr.setLabel(getNewLabel(tr.getTitle(), tr.getCenter()));
		}
	}
	
	// 検索用
	public ArrayList<TraceKey> getTraceKeys() {
		return(traceKeys);
	}
	
	// 番組追跡の追加
	public void add(TraceKey newkey) {
		traceKeys.add(newkey);
	}
	
	// 番組追跡の削除
	public void remove(String key) {
		for ( TraceKey k : traceKeys ) {
			if (k._getLabel().equals(key)) {
				traceKeys.remove(k);
	        	break;
			}
		}
	}
	
	private static final String popSrc = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゐゆゑよらりるれろわをんぁぃぅぇぉっゃゅょがぎぐげござじずぜぞだぢづでどばびぶべぼぱぴぷぺぽァィゥェォッャュョガギグゲゴザジズゼゾダヂヅデドバビブベボパピプペポヴｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝｧｨｩｪｫｯｬｭｮａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺabcdefghijklmnopqrstuvwxyz１２３４５６７８９０！＠＃＄％＾＆＊（）－＿＝＋「｛」｝￥｜；：’”、＜。＞・？‘～　一二三四五六七八九〇ⅠⅡⅢⅣⅤⅥⅦⅧⅨ①②③④⑤⑥⑦⑧⑨壱弐肆零";
	private static final String popDst = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤイユエヨラリルレロワヲンアイウエオツヤユヨカキクケコサシスセソタチツテトハヒフヘホハヒフヘホアイウエオツヤユヨカキクケコサシスセソタチツテトハヒフヘホハヒフヘホウアイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンアイウエオツヤユヨABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?`~ 12345678901234567891234567891240";
	private static char[] popDstA = null;
	
	// 文字列を比較しやすい値にする
	public static boolean isOmittedChar(char ch) {
		return (ch == ' ' || ch == '　' || ch == 'ー' || ch == 'ﾞ' || ch == 'ﾟ');
	}
	public static String replacePop(String src)
	{
		if (popDstA == null) {
			popDstA = popDst.toCharArray();
		}

		src = src.replaceAll("[ 　ーﾞﾟ]", "");
		
		StringBuilder sb = new StringBuilder();
		for ( char c : src.toCharArray() ) {
			int index = popSrc.indexOf(c);
			if ( index >= 0 ) {
				sb.append(popDstA[index]);
			}
			else {
				sb.append(c);
			}
		}
		return(sb.toString());
	}

	// 最少比較単位は２文字（バイグラム）
	private static final int COMPCHARLEN = 2;

	/**
	 * 2つの文字を比較してスコアを計算する(special thanks to ◆kzz0PzTAMM)
	 * @param searchkey 番組追跡の検索キーワード（「検索キーワード」の成分が「番組表のタイトルにどれくらい含まれているかを判定する」）
	 * @param target 番組表のタイトル
	 * @return
	 */
	public static int sumScore(String searchkey, String target)
	{
	
	    // 検索ワードが空なら検索終了
		if (searchkey == null || target == null || "".equals(searchkey) || "".equals(target)) {
			return 0;
		}

		int searchCountMax = searchkey.length();
		if ( searchCountMax > COMPCHARLEN ) {
			// 検索キーが最少比較単位より長い
			searchCountMax = searchCountMax - COMPCHARLEN + 1;
			int score = 0;
			int searchCount = 0;
			for ( ; searchCount < searchCountMax; searchCount++ ) {
				if ( target.indexOf(searchkey.substring(searchCount,searchCount+COMPCHARLEN)) != -1 ) {
					score++;
				}
			}
			return Math.round(score * 100 / searchCount);
		}
		else {
			if ( target.indexOf(searchkey) != -1 ) {
				return 100;
			}
		}
		
		return 0;
    }
	
	
	
	// コンストラクタ
	public TraceProgram() {
		traceKeys = new ArrayList<TraceKey>();
	}
}
