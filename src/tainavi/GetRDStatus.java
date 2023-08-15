package tainavi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 */
public class GetRDStatus implements Cloneable {

	private byte[] bb = new byte[65536];
	private byte[] bb2 = new byte[231];
	
	public String title = "";
	public String enc = "";
	public String ch = "";
	public String mod = "";
	public String typ = "";
	public String okk = "";
	public String unk = "";
	public String dvd = "";
	public String opn = "";
	public String ply = "";
	public String dig = "";
	public int title_no = 0;
	public int title_len = 0;
	public String title_len_s = "";
	public int chapter = 0;
	public String chapter_name = "";
	public int time_all = 0;
	public String time_all_s = "";
	public int time_chap = 0;
	public String time_chap_s = "";
	public String title_chno = "";
	public String title_chname = "";
	public String title_date = "";
	public String title_time = "";
	public String title_gnr = "";
	public String title_chcode = "";
	
	private final int port = 1048;

	//byte[] dummy  = {0x00,0x0c,0x00,0x00,0x00,0x00,0x00,0x40,0x00,0x00,0x00,0x00};
	//byte[] init   = {0x01,0x79,0x00,0x00,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff};
	//byte[] init2  = {0x00,(byte) 0xe9,0x00,0x00,0x00,0x02,(byte) 0xbc,(byte) 0x81};
	//byte[] none   = {0x00,0x08,0x00,0x00,0x00,0x00,0x00,0x00};
	//byte[] chgchA = {0x00,0x11,0x00,0x00,0x00,0x02,0x00,(byte) 0x80};
	//byte[] chgchB = {0x00,0x0D,0x00,0x00,0x00,0x02,0x00,0x00};
	//byte[] chgTit = {0x00,(byte) 0xE0,0x00,0x00,0x00,0x00,(byte) 0xBC,0x01};
	
	// 現在どのチャンネルを見ているのか確認する
	public String getCurChannel(String host) {
		
		Socket sock = null;
		DataOutputStream out = null;
		DataInputStream in = null;

		if (sock == null) {
			
			sock = new Socket();
			
			try {
				sock.setSoTimeout(1*1000);
				
				sock.connect(new InetSocketAddress(host,port),1000);
				
				out = new DataOutputStream(sock.getOutputStream());
				in = new DataInputStream(sock.getInputStream());
				
			} catch (SocketTimeoutException e) {
				System.err.println("ConnectException : Connection timeout");
				ply = "";
				sock = null;
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				sock = null;
				return null;
			}
		}
		
		while (true) {
			try {
				
				/*
				out.writeByte(0x10);
				out.writeByte(0x48);
				out.flush();
				*/
				
				byte[] hdr = new byte[2];
				in.read(hdr,0,2);
				
				int datlen = b2d(new byte[] { 0, 0, hdr[0],hdr[1] });
				
				// 例外回避
				if (datlen <= 1) {
					// 終了する
					break;
				}

				byte[] b = new byte[datlen-2];
				in.read(b,0,datlen-2);
				
				switch (datlen) {
				case 8:		// none
					System.out.println(" <-none");
					break;
					
				case 12:	// dummy
					System.out.println(" <-dummy");
					break;
					
				case 233:	// init2
					System.out.println(" <-init2");
					
					for ( int x=6; x<231; x++ ) {
						if (bb2[x] != b[x]) {
							System.err.println(x+": "+Integer.valueOf(bb2[x])+" -> "+Integer.valueOf(b[x]));
						}
						
						bb2[x] = b[x];
					}
					break;
					
				default:
					if (datlen >= 373) {
						// A600以降(共通部)
						{
							System.out.println(" <-init");
							
							int typd = b2d(new byte[] { b[6],b[7],b[8],b[9] });
							if (typd == 0) {
								typ = "DVD";
							}
							else if (typd == 1) {
								typ = "HDD";
							}
							else {
								typ = "UNKNOWN("+typd+")";
							}
							
							int okkd = b2d(new byte[] { b[10],b[11],b[12],b[13] });
							if (okkd == 0) {
								okk = "なし";
							}
							else if (okkd == 1) {
								okk = "追っかけ再生";
							}
							else if (okkd == 2) {
								okk = "TVお好み再生";
							}
							else {
								okk = "UNKNOWN("+okkd+")";
							}
							
							int unkd = b2d(new byte[] { b[14],b[15],b[16],b[17] });
							if (unkd == 0) {
								unk = "";
							}
							else if (unkd == 1) {
								unk = "録画";
							}
							else if (unkd == 2) {
								unk = "再生";
							}
							else if (unkd == 3) {
								unk = "録画+再生";
							}
							else {
								unk = "UNKNOWN("+unkd+")";
							}
							
							int dvdd = b2d(new byte[] { b[18],b[19],b[20],b[21] });
							if (dvdd == 0) {
								dvd = "";
							}
							else if (dvdd == 1) {
								dvd = "録画";
							}
							else if (dvdd == 2) {
								dvd = "再生";
							}
							else if (dvdd == 3) {
								dvd = "録画+再生";
							}
							else {
								dvd = "UNKNOWN("+dvdd+")";
							}
							
							int plyd = b2d(new byte[] { b[22],b[23],b[24],b[25] });
							if (plyd == 1) {
								ply = "録画";
							}
							else if (plyd == 2) {
								ply = "録画一時停止";
							}
							else if (plyd == 3) {
								ply = "停止";
							}
							else if (plyd == 4) {
								ply = "再生";
							}
							else if (plyd == 5) {
								ply = "一時停止";
							}
							else if (plyd == 6) {
								ply = "早送り";
							}
							else if (plyd == 7) {
								ply = "巻戻し";
							}
							else if (plyd == 8) {
								ply = "スロー送り";
							}
							else if (plyd == 9) {
								ply = "スロー戻し";
							}
							else if (plyd == 10) {
								ply = "コマ送り";
							}
							else if (plyd == 11) {
								ply = "コマ戻し";
							}
							else if (plyd == 12) {
								ply = "読込み中";
							}
							else if (plyd == 13) {
								ply = "処理中";
							}
							else if (plyd == 15) {
								ply = "ダビング";
							}
							else if (plyd == 24) {
								ply = "ネットdeレック";
							}
							else {
								ply = "不明("+plyd+")";
							}
							
							int opnd = b2d(new byte[] { b[26],b[27],b[28],b[29] });
							if (opnd == 0) {
								opn = "OPEN";
							}
							else if (opnd == 1) {
								opn = "CLOSE";
							}
							else if (opnd == 7) {
								opn = "LOADED";
							}
							else if (opnd == 15) {
								opn = "BD-RE(?)";
							}
							else {
								opn = "UNKNOWN("+opnd+")";
							}
							
							int modd = b2d(new byte[] { b[30],b[31],b[32],b[33] });
							if (modd == 0x00) {
								mod = "なし";
							}
							else if (modd == 1) {
								mod = "見るナビ";
							}
							else if (modd == 3) {
								mod = "編集ナビ";
							}
							else if (modd == 5) {
								mod = "設定メニュー";
							}
							else if (modd == 6) {
								mod = "番組ナビ";
							}
							else if (modd == 7) {
								mod = "スタートメニュー";
							}
							else if (modd == 8) {
								mod = "見ながら";
							}
							else if (modd == 9) {
								mod = "簡単ダビング";
							}
							else if (modd == 10) {
								mod = "はじめての設定";
							}
							else if (modd == 11) {
								mod = "番組説明";
							}
							else if (modd == 13) {
								mod = "つぎこれ";
							}
							else {
								mod = "不明("+modd+")";
							}
							
							time_all = b2d(new byte[] { b[46],b[47],b[48],b[49] });
							
							time_chap = b2d(new byte[] { b[50],b[51],b[52],b[53] });
							
							title_no = b2d(new byte[] { b[54],b[55],b[56],b[57] });
							
							title = new String(b,58,96,"MS932");
							int idx = title.indexOf('\0');
							if (idx != -1) {
								title = title.substring(0,idx);
							}
							
							chapter = b2d(new byte[] { b[154],b[155],b[156],b[157] });
							
							chapter_name = new String(b,158,96,"MS932");
							idx = chapter_name.indexOf('\0');
							if (idx != -1) {
								chapter_name = chapter_name.substring(0,idx);
							}
							
							title_len = b2d(new byte[] { b[354],b[355],b[356],b[357] });
							
							title_len_s = getHMS(title_len);
							time_all_s = getHMS(time_all);
							time_chap_s = getHMS(time_chap);
							
							int encd = b2d(new byte[] { b[358],b[359],b[360],b[361] });
							if (encd == 0x04) {
								enc = "TS1";
							}
							else if (encd == 0x05) {
								enc = "TS2";
							}
							else if (encd == 0x06) {
								enc = "RE";
							}
							else {
								enc = "不明("+encd+")";
							}
							
							int digd = b2d(new byte[] { b[34],b[35],b[36],b[37] });
							if (digd == 0) {
								// デジタル放送
								ch = new String(b,362,5);
								idx = ch.indexOf('\0');
								if (idx != -1) {
									ch = ch.substring(0,idx);
								}
							}
							else if (digd < 200) {
								ch = String.format("CH %d", digd);
							}
							else {
								ch = String.format("L %d", digd%200);
							}
						}
						// BZ700世代以降
						if (datlen >= 462) {
							int idx = 0;
							
							title_chno = new String(b,371,5,"MS932");
							idx = title_chno.indexOf('\0');
							if (idx != -1) {
								title_chno = title_chno.substring(0,idx);
							}
							
							title_chname = new String(b,378,42,"MS932");
							idx = title_chname.indexOf('\0');
							if (idx != -1) {
								title_chname = title_chname.substring(0,idx);
							}
							
							title_date = new String(b,424,10);
							idx = title_date.indexOf('\0');
							if (idx != -1) {
								title_date = title_date.substring(0,idx);
							}
							
							title_time = new String(b,436,5);
							idx = title_time.indexOf('\0');
							if (idx != -1) {
								title_time = title_time.substring(0,idx);
							}
							
							int title_gnrd = b2d(new byte[] { 0,0,0,b[447] });
							if (title_gnrd == 0x00) {
								title_gnr = "ジャンルなし または ニュース報道";
							}
							else if (title_gnrd == 1) {
								title_gnr = "スポーツ";
							}
							else if (title_gnrd == 2) {
								title_gnr = "情報ワイドショー";
							}
							else if (title_gnrd == 3) {
								title_gnr = "ドラマ";
							}
							else if (title_gnrd == 4) {
								title_gnr = "音楽";
							}
							else if (title_gnrd == 5) {
								title_gnr = "バラエティ";
							}
							else if (title_gnrd == 6) {
								title_gnr = "映画";
							}
							else if (title_gnrd == 7) {
								title_gnr = "アニメ／特撮";
							}
							else if (title_gnrd == 8) {
								title_gnr = "ドキュメンタリー教養";
							}
							else if (title_gnrd == 9) {
								title_gnr = "劇場公演";
							}
							else if (title_gnrd == 10) {
								title_gnr = "趣味教育";
							}
							else if (title_gnrd == 11) {
								title_gnr = "福祉";
							}
							else if (title_gnrd == 15) {
								title_gnr = "その他";
							}
							else {
								title_gnr = "不明("+title_gnrd+")";
							}
							
							Matcher ma = null;
							ma = Pattern.compile("(\\d+)").matcher(title_chno);
							if (ma.find()) {
								int code1 = b2d(new byte[] { 0,0,b[452],b[453] });
								int code2 = b2d(new byte[] { 0,0,b[454],b[455] });
								Matcher mb = null;
								mb = Pattern.compile("^L").matcher(title_chno);
								if (mb.find()) {
									title_chcode = String.format("%s:%08d:%03d", ma.group(1), code1, code2);
								} else {
									if (code1 != 0 || code2 != 0) {
										title_chcode = String.format("%s:%d:%d", ma.group(1), code1, code2);
									} else {
										title_chcode = "不明";
									}
								}
							} else {
								title_chcode = "不明";
							}
						}
						
						// 項目の変化を見るための差分出力（デバッグ用）
						for ( int x=6; x<datlen-2; x++ ) {
							if ( x == 9 || x == 25 || x == 29 || x ==  33 || x == 157 || x == 361 ||
									(x >= 34 && x <= 37) || (x >= 46 && x <= 49) || (x >= 50 && x <= 53) || (x >= 54 && x <= 57) || (x >= 154 && x <= 157) || (x >= 354 && x <= 357) ||
									(x >= 58 && x <= 58+95) || (x >= 362 && x <= 366)) {
								continue;
							}
							if (bb[x] != b[x]) {
								System.err.println(x+": "+Integer.valueOf(bb[x])+" -> "+Integer.valueOf(b[x]));
							}
							
							bb[x] = b[x];
						}
						
						//sock.close();
						//sock = null;
						return(new String(b,361,6));
					}
					else {
						// 旧default:
						System.out.println(" <-unknown("+datlen+")");
						
						// 永久ループではなく、373バイト未満の未知のサイズのデータはエラーにしてしまうことにした
						//sock.close();
						//sock = null;
						return null;
					}
				}
			}
			catch (SocketTimeoutException e) {
				e.printStackTrace();
				return null;
			}
			catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			finally {
				CommonUtils.closing(sock);
			}
		}
		
		return null;
	}
	
	public String getHMS(int t)	{
		String flg = "";
		if ( t < 0 ) {
			t = -t;
			flg = "-";
		}
		
		int s = t % 60;
		int m = (t-s)/60 % 60;
		int h = (t - m * 60 - s)/3600;
		
		return String.format("%s%02d:%02d:%02d", flg,h,m,s);
	}
	
	private int b2d(byte[] data) {
		return( (((data[0] & 0x80) == 0)?(1):(-1)) * (((data[0] & 0x7f) << 24) | ((data[1] & 0xff) << 16) | ((data[2] & 0xff) << 8) | (data[3] & 0xff)));
	}
	
	//
	public GetRDStatus clone() {
		return this;
	}
}
