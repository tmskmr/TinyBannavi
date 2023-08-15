package httpDump;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Server extends Thread {
	// 共有変数
	private Socket sock = null;
	private String id = "";
	private String password = "";

	// コンストラクタ
	public Server(Socket sock, String id, String password) {
		this.sock = sock;
		this.id = id;
		this.password = password;
		
		// スレッド実行
		this.start();
	}
	
	// 認証をどうこうするクラス
	public class MyAuthenticator extends Authenticator {
		private String username;
		private String password;

		public MyAuthenticator(String username, String password) {
			this.username = username;
			this.password = password;
		}
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password.toCharArray());
		}
	}
	
	// スレッド
	@Override
	public void run() {

		byte[] b = new byte[65536];
		char[] c = new char[65536];
		
		try {
			// 認証のためのおまじない
			Authenticator.setDefault(new MyAuthenticator(id, password));
			
			// Cookie
			CookieManager manager = new CookieManager();
			manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(manager);
			
			// ブラウザからリクエストを受け取る
			ArrayList<String> reqHeaders = new ArrayList<String>();
			String str = null;
			
			OutputStream w = sock.getOutputStream(); 
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(w));
			InputStream r = sock.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(r));
			
			int clen = -1;
			String method = null;
			String location = null;
			String protocol = null;
			boolean findHeader = true;
			while ((str = in.readLine()) != null) {
				if (findHeader) {
					Matcher ma = Pattern.compile("^(POST|GET) (.+?) (HTTP/.+?)$").matcher(str);
					if (ma.find()) {
						method = ma.group(1);
						location = ma.group(2);
						protocol = ma.group(3);
					}
					if (method.equals("GET")) {
						break;
					}
					findHeader = false;
					continue;
				}

				// ヘッダの終了
				Matcher ma = null;
				ma = Pattern.compile("^\\s*$").matcher(str);
				if (ma.find()) {
					break;
				}

				// Content-Lengthの取得
				ma = Pattern.compile("Content-Length: (\\d+)").matcher(str);
				if (ma.find()) {
					clen = Integer.valueOf(ma.group(1));
				}
				
				reqHeaders.add(str);
			}
			if (clen > 0) {
				in.read(c, 0, clen);
			}

			// ログに出力してみよう（１）
			PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("dump.txt"),true)));
			ps.println("\n\n### Request for RECORDER");
			ps.println(method+" "+location+" "+protocol);
			for ( String s : reqHeaders ) {
				ps.println(s);
			}
			ps.println("");
			for (int i=0; i<clen; i++) {
				ps.print(c[i]);
			}
			ps.flush();

			// メッセージも出力してみよう（１）
			System.out.println("\n### Request for RECORDER");
			System.out.println(method+" "+location+" "+protocol);
			for ( String s : reqHeaders ) {
				System.out.println(s);
			}
			System.out.println("");
			for (int i=0; i<clen; i++) {
				System.out.print(c[i]);
			}

			// Proxy先にリクエストを送る
			URL url = new URL(location);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			if (method.equals("POST")) {
				conn.setDoOutput(true);
				for ( String s : reqHeaders ) {
					Matcher ma = Pattern.compile("^(.+?): (.+?)$").matcher(s);
					if (ma.find()) {
						conn.setRequestProperty(ma.group(1), ma.group(2));
					}
				}
				conn.setRequestMethod("POST");
				conn.connect();
				
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(),"MS932"));
				for (int i=0; i<clen; i++) {
					writer.write(c[i]);
				}
				writer.close();
			}
			else {
				conn.setRequestMethod("GET");
				conn.connect();
			}
			
			// Proxy先からもらったヘッダを処理する
			String resCode = String.valueOf(conn.getResponseCode());
			Matcher ma = Pattern.compile("text").matcher(conn.getContentType());
			boolean isText = ma.find();
			//
			Map<String, List<String>> h = conn.getHeaderFields();
			Iterator<String> it = h.keySet().iterator();
			String header = "";
			while (it.hasNext()){
				String key = (String)it.next();
				header += ((key != null)?(key+": "):(""))+h.get(key).get(0)+"\r\n";
			}
			out.write(header);
			out.write("\r\n");
			out.flush();
			
			// 出力してみよう（２）
			ps.println("\n\n### Response from RECORDER");
			ps.println(header);
	        
			System.out.println("\n### Response from RECORDER");
			System.out.println(header);

			if (resCode.equals("200")) {
				int len;
				InputStream rr = conn.getInputStream();
				while ((len = rr.read(b, 0, b.length)) > 0) {
					if (isText) {
						ps.write(b, 0, len);
						ps.flush();
					}
					if ( true ) {
						int m = 4096;
						for (int n=0; n<len; n+=m) {
							if ((n+m) < len) {
								System.out.print("*"+len+"("+m+")");
								w.write(b, n, m);
							}
							else {
								int s = len-n;
								System.out.print("#"+len+"("+s+")");
								w.write(b, n, s);
							}
						}
						System.out.println("");
					}
					else {
						w.write(b, 0, len);
					}
				}
				rr.close();
			}
			
			ps.close();
			out.close();
			in.close();
			
		    conn.disconnect();
		    
		    sock.close();
		    
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
