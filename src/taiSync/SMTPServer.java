package taiSync;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.RecorderList;
import tainavi.TextValueSet;

public class SMTPServer extends Thread {
	
	private ServerSocket svsock = null;
	private ArrayList<RecorderInfo> recInfo = null;
	private ArrayList<ReserveInfo> rsvInfo = null;
	private ReserveCtrl rCtrl = null;
	private boolean fault = false;
	
	
	// コンストラクタ
	public SMTPServer(ServerSocket sock, ArrayList<RecorderInfo> recInfo, ArrayList<ReserveInfo> rsvInfo) {
		this.svsock = sock;
		this.rCtrl = new ReserveCtrl();
		
		this.recInfo = recInfo;
		this.rsvInfo = rsvInfo;
		
		this.start();
	}
	
	
	//
	public boolean isFault() { return fault; }
	public void clearFault() { this.fault=false; }
	
	// スレッド
	@Override
	public void run() {
		Socket sock = null;
		
		while (true) {
			try {
				sock = this.svsock.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			synchronized(rsvInfo) {
				try {
					recvRequest(sock);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//private ArrayList<String> reqQueue = new ArrayList<String>();
	//private RecorderInfo curRec = null;
	
	//
	private void recvRequest(Socket sock) throws IOException {
		
		String ipaddr = sock.getInetAddress().getHostAddress().toString();
		
		Date dt = new Date();
		System.out.println("RD["+ipaddr+"]からのSMTP接続がありました("+dt.toString()+")");
		
		OutputStream w = sock.getOutputStream(); 
		InputStream r = sock.getInputStream();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(w,"MS932"));
		BufferedReader in = new BufferedReader(new InputStreamReader(r,"ISO2022JP"));
		
		// RDからリクエストを受け取る
		String cmd = null;
		String res = null;

		// 接続OK
		String myAddr = sock.getLocalAddress().getHostAddress().toString();
		res = "220 TaiSync ESMTP\r\n";
		out.write(res);
		out.flush();
		System.err.print(res);

		while ((cmd = in.readLine()) != null) {
			// コマンド
			System.err.println(cmd);
			
			// リターン
			if (cmd.indexOf("HELO ") == 0) {
				res = "250 TaiSync.com\r\n";
			}
			else if (cmd.indexOf("MAIL FROM:") == 0) {
				res = "250 OK\r\n";
			}
			else if (cmd.indexOf("RCPT TO:") == 0) {
				res = "250 OK\r\n";
			}
			else if (cmd.indexOf("DATA") == 0) {
				res = "354 End data with .\r\n";
			}
			else if (cmd.indexOf("QUIT") == 0) {
				res = "221\r\n";
			}
			else {
				res = "250\r\n";
			}
			
			out.write(res);
			out.flush();
			System.err.print(res);
			
			if (cmd.indexOf("DATA") == 0) {
				ArrayList<String> sa = new ArrayList<String>();
				String dat = null;
				while ((dat = in.readLine()) != null) {
					sa.add(dat);
					System.err.println(dat);
					if (dat.equals(".")) {
						break;
					}
				}
				
				// 予約情報の更新
				updRsvInfo(ipaddr, sa);
				
				res = "250 OK\r\n";
				out.write(res);
				out.flush();
				System.err.print(res);
			}
			
			// 終了
			if (cmd.equals("QUIT")) {
				break;
			}
		}
		
		out.close();
		in.close();
	    
		System.out.println("RDからのSMTP接続を切断しました");
	}

	//
	private void updRsvInfo(String ipaddr, ArrayList<String> sa) {
		// SMTP本文から予約情報を取得
		GregorianCalendar c = new GregorianCalendar();
		Matcher ma = null;
		String start = null;
		String end = null;
		String channel = null;
		String tit = null;
		String id = null;
		String delId = null;
		for (String dat : sa) {
			if (dat != null) {
				ma = Pattern.compile("^録画日 (\\d\\d\\d\\d)/(\\d\\d)/(\\d\\d)").matcher(dat);
				if (ma.find()) {
					c.set(Calendar.YEAR, Integer.valueOf(ma.group(1)));
					c.set(Calendar.MONTH, Integer.valueOf(ma.group(2))-1);
					c.set(Calendar.DAY_OF_MONTH, Integer.valueOf(ma.group(3)));
					dat = null;
				}
			}
			if (dat != null) {
				ma = Pattern.compile("^録画開始時刻 (\\d\\d:\\d\\d)").matcher(dat);
				if (ma.find()) {
					start = ma.group(1);
					dat = null;
				}
			}
			if (dat != null) {
				ma = Pattern.compile("^録画終了時刻 (\\d\\d:\\d\\d)").matcher(dat);
				if (ma.find()) {
					dat = null;
					
					start = String.format("%04d/%02d/%02d %s", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH), start);
					end = String.format("%04d/%02d/%02d %s", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH), ma.group(1));
					if (start.compareTo(end) > 0) {
						c.add(Calendar.DAY_OF_MONTH,1);
						end = String.format("%04d/%02d/%02d %s", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH), ma.group(1));
					}
				}
			}
			if (dat != null) {
				ma = Pattern.compile("^チャンネル (.+?)$").matcher(dat);
				if (ma.find()) {
					channel = ma.group(1);
					dat = null;
				}
			}
			if (dat != null) {
				ma = Pattern.compile("%20del%20(.+?)$").matcher(dat);
				if (ma.find()) {
					id = ma.group(1);
					dat = null;
				}
			}
			if (dat != null) {
				ma = Pattern.compile("^予約を削除しました。予約ＩＤ：\\[(.+?)\\]$").matcher(dat);
				if (ma.find()) {
					delId = ma.group(1);
					dat = null;
				}
			}
			if (id == null) {
				tit = dat;
			}
		}
		
		//System.out.println(start+", "+end+", "+channel+", "+id);
		if (start != null && end != null && channel != null && tit != null && id != null) {
			// これは予約完了通知
			rCtrl.addRsvId(ipaddr, rsvInfo, start, end, channel, tit, id);
			fault = false;
		}
		else if (delId != null) {
			// これは削除完了通知
			rCtrl.delRsvId(ipaddr, rsvInfo, delId);
			fault = false;
		}
		else {
			// 予約完了通知でも削除完了通知でもない
			System.out.println("【警告】リクエストが受け付けられませんでした");
			fault = true;
		}
	}
	
}
