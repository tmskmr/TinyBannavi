package tainavi;

import javax.swing.JLabel;

public class JTreeLabel extends JLabel {

	private static final long serialVersionUID = 1L;
	
	/*******************************************************************************
	 * 定数
	 ******************************************************************************/
	
	private static final String NODESEPARATOR = ">";

	public static final String PREVIEW = "ﾌﾟﾚﾋﾞｭｰ";

	public static enum Nodes {
		// 共通
		ROOT			("root"),
		NONE			("*NONE*"),
		NOW				("現在放送中"),
		
		// リスト形式
		SEARCHED		("検索結果"),
		FILTERED		("絞込検索結果"),
		
		SEARCHHIST		("過去ログ検索履歴"),
		START			("新番組一覧"),
		END				("最終回一覧"),
		SYOBOCAL		("しょぼかる"),
		SYOBOALL		("全しょぼかる"),
		STANDBY			("予約待機"),
		NEWARRIVAL		("新着一覧"),
		MODIFIED		("詳細更新一覧"),
		SYOBOONLY		("しょぼかるのみに存在"),
		TRACE			("番組追跡"),
		KEYWORD			("キーワード検索"),
		KEYWORDGROUP	("キーワードグループ"),
		PICKUP			("ピックアップ"),
		GENRE			("ジャンル別"),
		BCASTLIST		("放送局別"),
		EXTENTION		("延長警告管理"),

		// 新聞形式
		DATE			("日付"),
		TERRA			("地上波"),
		BS				("ＢＳ"),
		CS				("ＣＳ"),
		RADIO			("ラジオ"),
		BCAST			("放送局"),
		PASSED			("過去ログ"),
		;
		
		private String label;
		
		private Nodes(String s) {
			label = s;
		}
		
		public String getLabel() { return label; }
		
		public static Nodes getNode(String label) {
			for ( Nodes nd : Nodes.values() ) {
				if ( nd.getLabel().equals(label) ) {
					return nd;
				}
			}
			return Nodes.NONE;	// nullを返すとswitch()がぬるぽるよ！
		}

	};

	/*******************************************************************************
	 * 部品
	 ******************************************************************************/
	
	private Nodes selected = Nodes.NONE;
	private String value;

	
	/*******************************************************************************
	 * コンストラクタ
	 ******************************************************************************/


	/*******************************************************************************
	 * アクション
	 ******************************************************************************/
	
	public Nodes getNode() { return selected; }
	
	public String getValue() { return value; }
	
	public void setView(Nodes sel, String val) {
		selected = (sel == null) ? Nodes.NONE : sel;
		value = val;
		if ( value == null ) {
			super.setText(selected.getLabel());
		}
		else {
			super.setText(selected.getLabel()+NODESEPARATOR+value);
		}
	}
	
	public String getView() { return super.getText(); }
	
	@Deprecated
	@Override
	public String getText() {
		return super.getText();
	}
	
	@Deprecated
	@Override
	public void setText(String text) {
		selected = Nodes.NONE;
		value = text;
		if ( value == null ) {
			super.setText(selected.getLabel());
		}
		else {
			super.setText(selected.getLabel()+NODESEPARATOR+value);
		}
	}
}
