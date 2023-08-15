package taiSync;

import java.awt.Toolkit;
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
import tainavi.ReserveList;
import tainavi.RecorderList;



public class HTTPServer extends Thread {
	
	private ServerSocket svsock = null;
	private RecorderList rec = null;
	private ArrayList<ReserveInfo> rsvInfo = null;
	private ReserveCtrl rCtrl = null;
	private tainavi.GetRDStatus cCtrl = null;
	
	
	// コンストラクタ
	public HTTPServer(ServerSocket sock, RecorderList rec, ArrayList<ReserveInfo> rsvInfo) {
		this.svsock = sock;
		this.rCtrl = new ReserveCtrl();
		this.cCtrl = new tainavi.GetRDStatus();
		
		this.rec = rec;			// 共有オブジェクト
		this.rsvInfo = rsvInfo;	// 共有オブジェクト
		
		this.start();
	}
	
	
	//
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
	
	
	//
	private void recvRequest(Socket sock) throws IOException {
		
		Date dt = new Date();
		System.out.println("鯛ナビからの接続がありました("+dt.toString()+")");
		
		// ブラウザからリクエストを受け取る
		ArrayList<String> reqHeaders = new ArrayList<String>();
		String str = null;
		
		OutputStream w = sock.getOutputStream(); 
		InputStream r = sock.getInputStream();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(w));
		BufferedReader in = new BufferedReader(new InputStreamReader(r));
		
		// リクエスト処理
		int clen = -1;
		String method = null;
		String location = null;
		String protocol = null;
		String poststr = null;
		/*
		char[] buff = new char[65536];
		int len = in.read(buff,0,buff.length);
		String buf = new String(buff,0,len);
		Matcher mh = Pattern.compile("^(POST|GET) (.+?) (HTTP/.+?)").matcher(buf);
		if (mh.find()) {
			method = mh.group(1);
			location = mh.group(2);
			protocol = mh.group(3);
			
			if (method.equals("POST")) {
				clen = in.read(buff,0,buff.length);
				poststr = new String(buff,0,clen);
			}
		}
		else {
			System.out.println("不正なリクエストです。");
		}
		*/
		
		boolean findHeader = true;
		while ((str = in.readLine()) != null) {
			//
			System.err.println("# Header: "+str);
			
			if (findHeader) {
				Matcher ma = Pattern.compile("^(POST|GET) (.+?) (HTTP/.+?)$").matcher(str);
				if (ma.find()) {
					method = ma.group(1);
					location = ma.group(2);
					protocol = ma.group(3);
				}
				//if (method.equals("GET")) {
				//	break;
				//}
				findHeader = false;
				continue;
			}
			
			// ヘッダの終了
			Matcher ma = Pattern.compile("^\\s*$").matcher(str);
			if (ma.find()) {
				break;
			}
			
			// Content-Lengthの取得
			if (method != null && method.equals("POST")) {
				ma = Pattern.compile("Content-Length: (\\d+)").matcher(str);
				if (ma.find()) {
					clen = Integer.valueOf(ma.group(1));
				}
			}
			
			reqHeaders.add(str);
		}
		if (clen > 0) {
			poststr = in.readLine();
			System.err.println("# Body: "+poststr);
		}
		if (location == null) {
			System.out.println("エラー：鯛ナビが空のリクエストを送ってきました");
			in.close();
			out.close();
			sock.close();
			return;
		}
		
		// レスポンス処理
		{
			// データ部の生成
			StringBuilder bsb = new StringBuilder();
			
			if (location.indexOf("/reserve.htm") >= 0) {
				bsb.append(rCtrl.getRsvStr(rec, rsvInfo));
				ReserveInfo.save(rsvInfo);
				ReserveInfo.showReserveInfo(rsvInfo,rec.getRecorderIPAddr());
			}
			else if (location.indexOf("/entry.htm") >= 0) {
				String addId = rCtrl.addReservedInfo(rec, poststr, rsvInfo);
				bsb.append(addId);
				ReserveInfo.save(rsvInfo);
				ReserveInfo.showReserveInfo(rsvInfo,rec.getRecorderIPAddr());
			}
			else if (location.indexOf("/update.htm") >= 0) {
				String updId = rCtrl.updReservedInfo(rec, poststr, rsvInfo);
				bsb.append(updId);
				ReserveInfo.save(rsvInfo);
				ReserveInfo.showReserveInfo(rsvInfo,rec.getRecorderIPAddr());
			}
			else if (location.indexOf("/delete.htm") >= 0) {
				Matcher ma = Pattern.compile("/delete\\.htm\\?id=(\\d+)").matcher(location);
				if (ma.find()) {
					rCtrl.delReservedInfo(rec, ma.group(1), rsvInfo);
					bsb.append(ma.group(1));
					ReserveInfo.save(rsvInfo);
					ReserveInfo.showReserveInfo(rsvInfo,rec.getRecorderIPAddr());
				}
			}
			else if (location.indexOf("/remote.htm") >= 0) {
				rCtrl.remoteCtrl(rec, location);
			}
			else if (location.indexOf("/getcurchannel.htm") >= 0) {
				bsb.append(cCtrl.getCurChannel(rec.getRecorderIPAddr()));
			}
			
			byte[] body = bsb.toString().getBytes("MS932");
			
			// ヘッダ部の生成
			StringBuilder hsb = new StringBuilder();
			
			hsb.append("HTTP/1.0 200 OK\r\n");
			hsb.append("Connection: close\r\n");
			hsb.append("Content-Type: text/html\r\n");
			hsb.append("Pragma: no-cache\r\n");
			hsb.append("Cache-Control: no-cache\r\n");
			hsb.append("Content-Length: "+body.length+"\r\n");
			hsb.append("\r\n");
			
			String header = hsb.toString();
			
			// ヘッダ・データの送出
			out.write(header);
			out.flush();
			w.write(body, 0, body.length);
			w.flush();
		}
		
		out.close();
		in.close();
		sock.close();
		
		System.out.println("鯛ナビからの接続を切断しました");
	}

}
