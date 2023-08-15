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
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.CommonUtils;
import tainavi.RecorderList;
import tainavi.TextValueSet;

public class POPServer extends Thread {
	
	Env env = null;
	private ServerSocket svsock = null;
	private ArrayList<RecorderInfo> recInfo = null;
	private ArrayList<ReserveInfo> rsvInfo = null;
	private ReserveCtrl rCtrl = null;
	private boolean fatal = false;
	private boolean fault = false;
	
	
	// コンストラクタ
	public POPServer(ServerSocket sock, ArrayList<RecorderInfo> recInfo, ArrayList<ReserveInfo> rsvInfo, Env env) {
		this.env = env;
		this.svsock = sock;
		this.rCtrl = new ReserveCtrl();
		this.fatal = false;
		this.fault = false;
		
		this.recInfo = recInfo;
		this.rsvInfo = rsvInfo;
		
		this.start();
	}
	
	//
	public boolean isFatal() { return fatal; }
	public boolean isFault() { return fault; }
	
	
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
	
	private ArrayList<String> reqQueue = new ArrayList<String>();
	private RecorderInfo curRec = null;
	
	//
	private void recvRequest(Socket sock) throws IOException {
		
		String ipaddr = sock.getInetAddress().getHostAddress().toString();
		
		Date dt = new Date();
		System.out.println("RD["+ipaddr+"]からのPOP接続がありました("+dt.toString()+")");
		
		boolean drop = false;
		
		// 溜まっているリクエストの整理
		if (curRec == null) {
			for (RecorderInfo rec : recInfo) {
				if (rec.getRecorderIPAddr().equals(ipaddr)) {
					curRec = rec;
					// リクエストキューを作成する
					drop = rCtrl.getReqQueue(curRec, rsvInfo, reqQueue, env.getAppendDT(), env.getPeriod());
					if (drop) {
						fault = true;
					}
					break;
				}
			}
			if (curRec == null) {
				System.out.println(ipaddr+"は不正なIPアドレスです");
				return;
			}
			if (reqQueue.size() == 0) {
				curRec = null;
			}
		}
		else {
			if ( ! ipaddr.equals(curRec.getRecorderIPAddr())) {
				System.out.println(curRec.getRecorderIPAddr()+"の処理中のため"+ipaddr+"からの接続をキャンセルしました");
				return;
			}
		}
		
		OutputStream w = sock.getOutputStream(); 
		InputStream r = sock.getInputStream();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(w,"MS932"));
		BufferedReader in = new BufferedReader(new InputStreamReader(r));
		
		// RDからリクエストを受け取る
		String cmd = null;
		String res = null;

		// 接続OK
		String myAddr = sock.getLocalAddress().getHostAddress().toString();
		res = "+OK TaiSync at "+myAddr+" starting.\r\n";
		out.write(res);
		out.flush();
		System.err.print(res);

		String reqStr = "";
		boolean delivered = false;
		int topidx = -1;
		while ((cmd = in.readLine()) != null) {
			// コマンド
			System.err.println(cmd);
			
			if (delivered == true && cmd.indexOf("DELE ") != 0) {
				System.out.println("【警告】リクエストが受け付けられませんでした");
				System.out.println("====");
				System.out.print(reqStr);
				System.out.println("====");
				
				fault = true;	// 異常発生
				delivered = false;	// 回答受信済み
			}
			
			// リターン
			if (cmd.equals("STAT")) {
				res = "+OK "+reqQueue.size()+" 0\r\n";
			}
			else if (cmd.indexOf("TOP ") == 0) {
				topidx = -1;
				Matcher ma = Pattern.compile("^TOP (\\d+) ").matcher(cmd);
				if (ma.find()) {
					topidx = Integer.valueOf(ma.group(1));
				}
				
				reqStr = reqQueue.get(reqQueue.size()-topidx);
				
				StringBuilder sb = new StringBuilder();
				sb.append("+OK\r\n");
				sb.append("From: tainavi\r\n");
				sb.append("To: rdstyle\r\n");
				sb.append("Subject: reserve program\r\n");
				sb.append("Content-Type: text/plain; charset=Shift_JIS\r\n");
				sb.append("\r\n");
				sb.append(reqStr);
				sb.append(".\r\n");
				
				res = sb.toString();
				
				delivered = true;	// リクエスト送信済み、回答(DELE)待ち
			}
			else if (cmd.indexOf("DELE ") == 0) {
				int delidx = -1;
				Matcher ma = Pattern.compile("^DELE (\\d+)").matcher(cmd);
				if (ma.find()) {
					delidx = Integer.valueOf(ma.group(1));
				}
				
				reqQueue.remove(reqQueue.size()-delidx);
				
				System.out.println("リクエストが受け付けられました");
				System.out.println("====");
				System.out.print(reqStr);
				System.out.println("====");
				
				res = "+OK Marked to be deleted.\r\n";
				
				delivered = false;	// 回答受信済み
			}
			else if (cmd.equals("QUIT")) {
				res = "+OK Logging out.\r\n";
			}
			else {
				res = "+OK\r\n";
			}
			
			out.write(res);
			out.flush();
			System.err.print(res);
			
			// 終了
			if (cmd.equals("QUIT")) {
				break;
			}
		}
		
		if ( topidx == 1 ) {
			//System.out.println("キューが全部チェックされたのでリセットします");
			
			if ( reqQueue.size() == 0 && ! drop ) {
				fault = false;	// 復帰
			}
			
			curRec = null;
			reqQueue.clear();
		}
		
		out.close();
		in.close();
	    
		System.out.println("RDからのPOP接続を切断しました");
	}
	
}
