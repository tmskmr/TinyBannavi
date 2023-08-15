package epgdump;

import java.io.FileInputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;

/*
 * 本パッケージは epgdumpr2 (tomy ◆CfWlfzSGyg氏) をベースに鯛ナビ用のJavaコードに書き換えたものです。
 * 本パッケージのライセンスは原著を踏襲するものとします。
 * 
 * 以下 readme.txtよりの抜粋
 * ----------
 * xmltv-epg
 * 
 * MPEG-TSに含まれるepgをxmlで出力するプログラムです。
 * ◆N/E9PqspSk氏がrecfriio Solaris版(http://2sen.dip.jp/cgi-bin/friioup/source/up0737.zip)に含まれるepgdumpを
 * Linux版を改造したものをベースにxmltv用のxmlファイルを作成します。
 * 
 * （中略）
 * 
 * epgdumpライセンス(Solaris版より引用):
 * >epgdumpに関しては、BonTest Ver.1.40からそのままソースを持ってきている部分も
 * >あるため、そのライセンスに従いします。
 * >BonTestのReadme.txtより
 * >>
 * >>３．ライセンスについて
 * >>　　・本パッケージに含まれる全てのソースコード、バイナリについて著作権は一切主張しません。
 * >>　　・オリジナルのまま又は改変し、各自のソフトウェアに自由に添付、組み込むことができます。
 * >>　　・但しGPLに従うことを要求しますのでこれらを行う場合はソースコードの開示が必須となります。
 * >>　　・このとき本ソフトウェアの著作権表示を行うかどうかは任意です。
 * >>　　・本ソフトウェアはFAAD2のライブラリ版バイナリを使用しています。
 * >>
 * >>　　　"Code from FAAD2 is copyright (c) Nero AG, www.nero.com"
 * >>
 * >>　　・ビルドに必要な環境
 * >>　　　- Microsoft Visual Studio 2005 以上　※MFCが必要
 * >>　　　- Microsoft Windows SDK v6.0 以上　　※DirectShow基底クラスのコンパイル済みライブラリが必要
 * >>　　　- Microsoft DirectX 9.0 SDK 以上
 * 
 * Special Thanks:
 * ・Solaris版開発者の方
 * ・拡張ツール中の人
 * ・◆N/E9PqspSk氏
 * ・ARIB(資料の無料ダウンロードに対して)
 * 
 */

/*
 * 書き換え中に見つかった元ソースに存在する既存バグは以下の通り。応急処置はしたが「例外を回避する」ことしかしてないので正しい動作かどうかは不明。
 * 
 * 1. TSpacket.payloadの操作でoverrunが発生する（Cだと厳密に境界チェックされないので特にどうこうはないが）　→応急処置２箇所：Sdt.java、TSpacket.java
 * 2. SECcache.bufの操作でoverrunが発生する（こちらはcygwinバイナリでもコアダンプが発生しているので問題）　→応急処置１箇所：SECcache.java
 * 3. Eit.dumpEITの操作でポインタの操作ミスがあった（詳細データの取得漏れが発生していた）　→処置済み：dumpEIT()は全般的に整理
 */

public class Epgdump {

	// 興味のあるpidを指定
	private static final int[] pids = {0x11,0x12,0x26,0x27};

	private static final int SECCOUNT = 4;
	
	private static final Ts ts = new Ts();
	private static final Sdt sdt = new Sdt();
	private static final Eit eit = new Eit();
	
	//
	private void GetSDT(PushbackInputStream infile, ArrayList<SVT_CONTROL> svttop, ArrayList<SECcache> secs, int count) {
		SECcache bsecs;
		
		while ( (bsecs = ts.readTS(infile, secs, count)) != null ) {
			// SDT
			if ( (bsecs.pid & 0xFF) == 0x11 ) {
				sdt.dumpSDT(bsecs.buf, svttop);
			}
		}
	}
	
	//
	private void GetEIT(PushbackInputStream infile, ArrayList<SVT_CONTROL> svttop, ArrayList<SECcache> secs, int count) {
		SECcache bsecs;
		
		while ( (bsecs = ts.readTS(infile, secs, count)) != null ) {
			// EIT
			switch ( bsecs.pid & 0xFF ) {
			case 0x12:
			case 0x26:
			case 0x27:
				eit.dumpEIT(bsecs.buf, svttop);
				break;
			}
		}
	}
	
	//
	public ArrayList<SVT_CONTROL> getSvtControl(String file) {
		
		// 興味のあるpidを指定
		ArrayList<SECcache> secs = new ArrayList<SECcache>();
		for ( int pid : pids ) {
			SECcache sec = new SECcache(); 
			sec.pid = pid;
			secs.add(sec);
		}
		
		FileInputStream in = null;
		PushbackInputStream infile = null;
		try {
			in = new FileInputStream(file); 
			infile = new PushbackInputStream(in);
			ArrayList<SVT_CONTROL> svttop = new ArrayList<SVT_CONTROL>();
			GetSDT(infile, svttop, secs, SECCOUNT);
			if ( svttop.size() > 0 ) {
				return svttop;
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( infile != null ) try { infile.close(); } catch (Exception e) {}
			if ( in != null ) try { in.close(); } catch (Exception e) {}
		}
		return null;
	}

	//
	public ArrayList<SVT_CONTROL> getEitControl(String file, ArrayList<SVT_CONTROL> svttop) {
		
		// 興味のあるpidを指定
		ArrayList<SECcache> secs = new ArrayList<SECcache>();
		for ( int pid : pids ) {
			SECcache sec = new SECcache(); 
			sec.pid = pid;
			secs.add(sec);
		}
		
		FileInputStream in = null;
		PushbackInputStream infile = null;
		try {
			in = new FileInputStream(file); 
			infile = new PushbackInputStream(in);
			GetEIT(infile, svttop, secs, SECCOUNT);
			return svttop;
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		finally {
			if ( infile != null ) try { infile.close(); } catch (Exception e) {}
			if ( in != null ) try { in.close(); } catch (Exception e) {}
		}
		return null;
	}
	
	//
	public void enqueueSVT(ArrayList<SVT_CONTROL> svta, SVT_CONTROL svtnew) {
		if ( svtnew.servicename.matches("^[-\\s]*$") ) {
			// 拒否
			return;
		}
		int i=0;
		for ( ; i<svta.size(); i++ ) {
			SVT_CONTROL svtcur = svta.get(i);
			//
			if ( svtcur.original_network_id < svtnew.original_network_id ) {
				continue;
			}
			else if ( svtcur.original_network_id > svtnew.original_network_id ) {
				// 決定
				break;
			}
			//
			if ( svtcur.transport_stream_id < svtnew.transport_stream_id ) {
				continue;
			}
			else if ( svtcur.transport_stream_id > svtnew.transport_stream_id ) {
				break;
			}
			//
			if ( svtcur.servive_id == svtnew.servive_id ) {
				// 拒否
				return;
			}
			else if ( svtcur.servive_id < svtnew.servive_id ) {
				continue;
			}
			else {
				break;
			}
		}
		svta.add(i,svtnew);
	}
}
