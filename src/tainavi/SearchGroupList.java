package tainavi;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class SearchGroupList implements Iterator<SearchGroup>, Iterable<SearchGroup>{

	private final ArrayList<SearchGroup> gList = new ArrayList<SearchGroup>();

	private int idx;

	private String gFile = "env"+File.separator+"keywordgroup.xml";


	// コンストラクタ
	public SearchGroupList() {
		//
	}


	// 検索系
	@Override
	public Iterator<SearchGroup> iterator() {
		idx = -1;
		return this;
	}
	@Override
	public boolean hasNext() {
		return (gList.size() > (idx+1));
	}
	@Override
	public SearchGroup next() {
		return gList.get(++idx);
	}
	@Override
	public void remove() {
		gList.remove(idx);
	}

	public int size() {
		return gList.size();
	}

	// グループ・メンバー追加
	public boolean add(String name) {
		for ( SearchGroup gl : gList ) {
			if ( gl.getName().equals(name) ) {
				// 既に存在している
				return false;
			}
		}
		SearchGroup gl = new SearchGroup();
		gl.setName(name);
		gList.add(gl);
		return true;
	}

	public boolean add(SearchGroup gr){
		if (gr == null || gr.getName() == null)
			return false;

		for ( SearchGroup gl : gList ) {
			if ( gl.getName().equals(gr.getName()) ) {
				// 既に存在している
				return false;
			}
		}

		gList.add(gr);

		return true;
	}

	public boolean add(String name, String member) {

		if ( name == null || member == null ) {
			// グループとメンバーの指定は必須
			return false;
		}

		for ( SearchGroup gl : gList ) {
			if ( gl.getName().equals(name) ) {
				gList.iterator();
				for ( String gmember : gl ) {
					if ( gmember.equals(member) ) {
						// 既に存在している
						return false;
					}
				}
				// メンバーを追加する
				gl.add(member);
				return true;
			}
		}
		// グループごと追加する
		SearchGroup gl = new SearchGroup();
		gl.setName(name);
		gl.add(member);
		return true;
	}

	// 削除する
	public boolean remove(String name) {
		for ( SearchGroup gl : gList ) {
			if ( gl.getName().equals(name) ) {
				// グループごと削除
				gList.remove(gl);
				return true;
			}
		}
		return false;
	}
	public boolean remove(String name, String member) {

		if ( member == null ) {
			// グループ指定は必須
			return false;
		}

		if ( name == null ) {
			// グループ横断削除
			for ( SearchGroup gl : gList ) {
				for ( String gmember : gl ) {
					if ( gmember.equals(member) ) {
						// メンバー削除
						gl.remove();
						break;
					}
				}
			}
			// あってもなくてもよしとする
			return true;
		}
		else {
			// 単独グループ削除
			for ( SearchGroup gl : gList ) {
				if ( gl.getName().equals(name) ) {
					for ( String gmember : gl ) {
						if ( gmember.equals(member) ) {
							// メンバー削除
							gl.remove();
							return true;
						}
					}
					// メンバーが存在しない
					return false;
				}
			}
			// グループが存在しない
			return false;
		}
	}

	// クリアする
	public void clear(){
		gList.clear();
	}

	// 検索する
	public boolean isFind(String name, String member) {

		if ( name == null || member == null ) {
			return false;
		}

		for ( SearchGroup gl : gList ) {
			if ( gl.getName().equals(name) ) {
				for ( String gmember : gl ) {
					if ( gmember.equals(member) ) {
						// 存在している
						return true;
					}
				}
			}
		}
		return false;
	}

	// 改名する
	public boolean rename(String oldName, String newName) {
		for ( SearchGroup gl : gList ) {
			if ( gl.getName().equals(oldName) ) {
				gl.setName(newName);
				return true;
			}
		}
		return false;
	}
	public boolean rename(String name, String oldMember, String newMember) {
		if ( name == null ) {
			boolean f = false;
			for ( SearchGroup gl : gList ) {
				if ( gl.replace(oldMember, newMember) ) {
					f = true;
				}
			}
			return f;
		}
		else {
			for ( SearchGroup gl : gList ) {
				if ( gl.getName().equals(name) ) {
					if ( gl.replace(oldMember, newMember) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// セーブ・ロード
	public boolean save() {
		System.out.println("検索キーワードグループ設定を保存します: "+gFile);
		if ( ! CommonUtils.writeXML(gFile, gList) ) {
			System.err.println("検索キーワードグループ設定の保存に失敗しました： "+gFile);
			return false;
		}
		return true;
	}

	public boolean load() {
		System.out.println("検索キーワードグループ設定を読み込みます: "+gFile);
		if ( new File(gFile).exists() ) {
			@SuppressWarnings("unchecked")
			ArrayList<SearchGroup> tmpList = (ArrayList<SearchGroup>) CommonUtils.readXML(gFile);
			if ( tmpList != null ) {
				FieldUtils.deepCopy(gList, tmpList);
				return true;
			}
		}

		System.err.println("検索キーワードグループ設定が読み込みに失敗しました： "+gFile);
		return false;
	}
}
