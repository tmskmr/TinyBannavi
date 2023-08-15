package tainavi;

import java.util.ArrayList;

import tainavi.ProgDetailList.WrHeader;

public class ProgDateList {
	public String Date;
	public int row;
	public ArrayList<ProgDetailList> pdetail;

	public ProgDateList() {
		Date = "";
		row = 0;
		pdetail = new ArrayList<ProgDetailList>();
	}

	/*
	 * 文字列に変換する
	 */
	public String toString(){
		StringBuilder sb = new StringBuilder();
		// 属性をヘッダー行にエンコードする
		sb.append(Date + ":");
		sb.append(String.valueOf(row) + ":");
		sb.append(pdetail.size());
		sb.append("\n");

		// pdetail を文字列にエンコードする
		for (ProgDetailList d : pdetail) {
			sb.append(d.toString());
		}

		return sb.toString();
	}

	/*
	 * 文字列からインスタンスを生成する
	 */
	public ProgDateList(String txt){
		Date = "";
		row = 0;
		pdetail = new ArrayList<ProgDetailList>();

		// ヘッダー行を切り出す
		int he = txt.indexOf("\n");
		String th = txt.substring(0,  he);
		if (th == null)
			return;

		// ヘッダー行をトークンに分割する
		String []tokens = th.split(":");
		if (tokens.length < 3)
			return;

		// トークンを属性にコピーする
		Date = tokens[0];
		row = Integer.parseInt(tokens[1]);
		int size = Integer.parseInt(tokens[2]);

		// 残りの文字列からpdetail を生成する
		int index = 0;
		txt = txt.substring(he+1);
		for (int n=0; n<size; n++){
			// 番組ヘッダを探す
			int newtop = txt.indexOf(WrHeader.STARTMARK.toString(),index);
			if ( newtop == -1 ) {
				break;
			}
			newtop += WrHeader.STARTMARK.toString().length()+1;

			// 番組フッタを探す
			int newtail = txt.indexOf(WrHeader.ENDMARK.toString(),newtop);
			if ( newtail == -1 ) {
				break;
			}
			index = newtail+WrHeader.ENDMARK.toString().length()+1;

			// 解析する
			ProgDetailList pdl = new ProgDetailList(txt.substring(newtop,newtail));
			pdetail.add(pdl);
		}
	}
}
