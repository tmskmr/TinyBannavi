package tainavi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 *  ARIB外字のマッピング情報
 */
public enum AribCharMap {
	HDTV		("HV"	,"\uE0F8"),
	SDTV		("SD"	,"\uE0F9"),
	PROGRESSIVE	("Ｐ"	,"\uE0FA"),
	WIDE		("Ｗ"	,"\uE0FB"),
	MULTIVIEW	("MV"	,"\uE0FC"),
	SIGN		("手"	,"\uE0FD"),
	SUBTITLE	("字"	,"\uE0FE"),
	TWOWAY		("双"	,"\uE0FF"),
	DATA		("デ"	,"\uE180"),
	STEREO		("Ｓ"	,"\uE181"),
	BILINGUAL	("二"	,"\uE182"),
	MULTIPLEX	("多"	,"\uE183"),
	COMMENTARY	("解"	,"\uE184"),
	SURROUND	("SS"	,"\uE185"),
	BMODE		("Ｂ"	,"\uE186"),
	NEWS		("Ｎ"	,"\uE187"),
	WEATHER		("天"	,"\uE18A"),
	TRAFFIC		("交"	,"\uE18B"),
	MOVIE		("映"	,"\uE18C"),
	FREE		("無"	,"\uE18D"),
	PAY			("料"	,"\uE18E"),
	FORMER		("前"	,"\uE190"),
	LATTER		("後"	,"\uE191"),
	REAIR		("再"	,"\uE192"),
	NEW			("新"	,"\uE193"),
	FIRST		("初"	,"\uE194"),
	END			("終"	,"\uE195"),
	LIVE		("生"	,"\uE196"),
	SHOPPING	("販"	,"\uE197"),
	VOICE		("声"	,"\uE198"),
	DUBBED		("吹"	,"\uE199"),
	PPV			("PPV"	,"\uE19A"),
//	SECRET		("秘"	,"\uE19B"),
//	OTHER		("ほか"	,"\uE19C"),
	;

	String noaribStr;
	String aribStr;

	private AribCharMap(String n, String a){
		noaribStr = n;
		aribStr = a;
	}

	public String getNoAribPattern(){
		return "\\[" + noaribStr + "\\]";
	}

	public String getNoAribStr(){
		return "[" + noaribStr + "]";
	}

	public String getAribStr(){
		return aribStr;
	}

	/*
	 *  ARIB外字を展開した文字列からARIB外字を含む文字列に変換する
	 */
	public static String ConvStringToArib(String s){
		if (s == null)
			return s;

		for (AribCharMap acm : AribCharMap.values()){
			Matcher ma = Pattern.compile(acm.getNoAribPattern()).matcher(s);
			if (ma.find()){
				s = ma.replaceAll(acm.getAribStr());
			}
		}

		return s;
	}

	/*
	 *  ARIB外字を含む文字列から展開した文字列に変換する
	 */
	public static String ConvStringFromArib(String s){
		if (s == null)
			return s;

		for (AribCharMap acm : AribCharMap.values()){
			Matcher ma = Pattern.compile(acm.getAribStr()).matcher(s);
			if (ma.find()){
				s = ma.replaceAll(acm.getNoAribStr());
			}
		}

		return s;
	}
};
