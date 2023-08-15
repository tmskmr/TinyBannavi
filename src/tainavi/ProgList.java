package tainavi;

import java.awt.Color;
import java.util.ArrayList;

public class ProgList {
	public String Area;
	public String SubArea;
	public String Type;
	public String Center;
	public String CenterId;
	public String ChId;
	public Color BgColor;
	public boolean enabled;
	public ArrayList<ProgDateList> pdate;

	private static final String PDEND = "#PDEND\n";	// 文字列エンコード時のProgDateListのターミネータ

	public ProgList() {
		Area = "";
		SubArea = "";
		Type = "";
		Center = "";
		CenterId = "";
		ChId = "";
		BgColor = new Color(180,180,180);
		enabled = true;
		pdate = new ArrayList<ProgDateList>();
	}

	/*
	 * インスタンスを文字列に変換する
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();

		// 属性をヘッダー行にエンコードする
		sb.append(Area + ":");
		sb.append(SubArea + ":");
		sb.append(Type + ":");
		sb.append(Center + ":");
		sb.append(CenterId + ":");
		sb.append(ChId + ":");
		sb.append(CommonUtils.color2str(BgColor) + ":");
		sb.append((enabled ? "1" : "0") + ":");
		sb.append(pdate.size());
		sb.append("\n");

		// 日付の配列を文字列に変換して追加する。PDENDでターミネートする
		for ( ProgDateList c : pdate ) {
			sb.append(c.toString());
			sb.append(PDEND);
		}

		return sb.toString();
	}

	/*
	 * 文字列からインスタンスを生成する
	 */
	public ProgList(String txt){
		Area = "";
		SubArea = "";
		Type = "";
		Center = "";
		CenterId = "";
		ChId = "";
		BgColor = new Color(180,180,180);
		enabled = true;
		pdate = new ArrayList<ProgDateList>();

		// ヘッダー行を取り出す
		int he = txt.indexOf("\n");
		String th = txt.substring(0,  he);
		if (th == null)
			return;

		// ヘッダー行をトークンに分割する
		String []tokens = th.split(":");
		if (tokens.length < 9)
			return;

		// トークンを属性にコピーする
		Area = tokens[0];
		SubArea = tokens[1];
		Type = tokens[2];
		Center = tokens[3];
		CenterId = tokens[4];
		ChId = tokens[5];
		BgColor = CommonUtils.str2color(tokens[6]);
		enabled = tokens[7].equals("1");
		int size = Integer.parseInt(tokens[8]);

		// 残りの文字列から pdate を生成する
		String tdl = txt.substring(he+1);
		for (int n=0; n<size; n++){
			// ターミネートまでの文字列を切りだす
			int dd = tdl.indexOf(PDEND);
			String td = tdl.substring(0,  dd);

			// 文字列からインスタンスを生成して配列に追加する
			ProgDateList d = new ProgDateList(td);
			pdate.add(d);

			// 残りの文字列を切り出す
			if (dd == -1)
				break;
			tdl = tdl.substring(dd+PDEND.length());
		}
	}
}
