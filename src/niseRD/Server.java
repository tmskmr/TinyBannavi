package niseRD;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tainavi.CommonUtils;


public class Server extends Thread {
	// 共有変数
	private Socket sock = null;
	private String folder = null;

	// コンストラクタ
	public Server(Socket sock, String folder) {
		this.sock = sock;
		this.folder = folder;
		
		// スレッド実行
		this.start();
	}
	
	// スレッド
	@Override
	public void run() {

		byte[] b = new byte[1024*1024];
		char[] c = new char[65536];
		
		OutputStream w = null;
		BufferedWriter out = null;
		InputStream r = null;
		BufferedReader in = null;
		FileInputStream reader = null;
		try {
			
			// ブラウザからリクエストを受け取る
			ArrayList<String> reqHeaders = new ArrayList<String>();
			String str = null;
			
			w = sock.getOutputStream(); 
			out = new BufferedWriter(new OutputStreamWriter(w));
			r = sock.getInputStream();
			in = new BufferedReader(new InputStreamReader(r));
			
			int clen = -1;
			String method = null;
			String location = null;
			String protocol = null;
			String poststr = null;
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
				Matcher ma = Pattern.compile("^\\s*$").matcher(str);
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
				System.err.println("PSTR: "+String.valueOf(c));
			}

			if (method == null)
				return; // 謎だ
			
			// ファイルを探そう
			{
				ArrayList<String> header = new ArrayList<String>();
				
				int body_length = 0;
				int len = -1;
				int m = 4096;
				
				Matcher ma = null;
				String filename = "";
				String file = null;
				String command = null;
				if ( method.equals("GET") ) {
					ma = Pattern.compile("/([^/]+\\.(htm|cgi|css|png|js|do|css))\\??(.*?)$").matcher(location);
					if (ma.find()) {
						file = ma.group(1);
						if ( ma.groupCount() >= 3 && (ma.group(1).equals("prevLogin.cgi") || ma.group(1).equals("dispframe.cgi") || ma.group(1).equals("folderlist.htm")) ) {
							filename = folder+File.separator+ma.group(1)+"."+ma.group(3);
						}
						else if ( ma.groupCount() >= 3 && ma.group(1).equals("dvdr_ctrl.cgi") ) {
							String ps = ma.group(3);
							filename = folder+File.separator+ma.group(1)+"."+ps;
							if ( ps.contains("cCMD_") ) {
								int a = ps.indexOf("cCMD_");
								int z = ps.indexOf(".",a);
								command = ps.substring(a,z+1);
								System.out.println("command: "+command);
							}
							else {
							}
						}
						else if ( ma.groupCount() >= 3 && ma.group(1).equals("reserve_list.cgi") ) {
							String ps = ma.group(3);
							filename = folder+File.separator+ma.group(1)+"."+ps;
							int a = ps.indexOf("ANC_RSVLSTNO");
							if ( a != -1 ) {
								int z = ps.indexOf("&",a);
								if ( z == -1 ) {
									z = ps.length();
								}
								command = ps.substring(a,z+1);
								System.out.println("command: "+command);
							}
						}
						else {
							filename = folder+File.separator+ma.group(1);
						}
					}
					else if (location.endsWith("/")){
						file = "";
						filename = folder+File.separator+"index.htm";
					}
					else {
						filename = folder+File.separator+"404.htm";
					}
				}
				else {
					ma = Pattern.compile("/([^/]+\\.(cgi|htm|css|png))").matcher(location);
					if (ma.find()) {
						file = ma.group(1);
						if ( ma.group(1).equals("dvdr_ctrl.cgi") || ma.group(1).equals("reserve_list.cgi") || ma.group(1).equals("dispframe.cgi") ) {
							String ps = new String(c);
							ps = ps.replaceAll("(cCMD_[A-Z]+\\.[xy]=)\\d+", "$1");
							filename = folder+File.separator+ma.group(1)+"."+ps;
							if ( ps.contains("cCMD_") ) {
								int a = ps.indexOf("cCMD_");
								int z = ps.indexOf(".",a);
								command = ps.substring(a,z);
								System.out.println("command: "+command);
							}
							else {
							}
						}
						else {
							filename = folder+File.separator+ma.group(1);
						}
					}
					else if (location.endsWith("/")){
						filename = folder+File.separator+"index.htm";
					}
					else {
						filename = folder+File.separator+"404.htm";
					}
				}
				
				System.out.println("size: "+body_length+", "+location+", "+method);
				
				File f = null;
				
				// 新デバッグログ対応
				File dbg = new File(folder+File.separator+"debug");
				if ( file != null && dbg.exists() ) {
					for ( File fd : dbg.listFiles() ) {
						if ( fd.isFile() && fd.getName().endsWith(".htm") ) {
							BufferedReader br = new BufferedReader(new FileReader(fd));
							String s = null;
							while ( (s = br.readLine()) != null ) {
								if ( s.startsWith("# GET:") || s.startsWith("# POST:") ) {
									//System.err.println(File.separator+file+", "+s);
									if ( s.contains("/"+file) ) {
										if ( command == null || s.contains(command) ) {
											f = fd;
										}
									}
									break;
								}
							}
							br.close();
							br = null;
						}
						if ( f != null ) {
							break;
						}
					}
				}
				
				if ( f == null && ! (f = new File(filename+".txt")).exists() && ! (f = new File(filename)).exists() ) {
					//System.out.println("Not exist: "+f.getName());
					
					header.add("HTTP/1.0 404 Not Found\r\n");
					header.add("\r\n");
				}
				else {
					System.out.println("Exist: "+f.getName());
					
					header.add("HTTP/1.0 200 OK\r\n");
					header.add("Connection: close\r\n");
					header.add("Content-Type: text/html\r\n");
					header.add("Pragma: no-cache\r\n");
					header.add("Cache-Control: no-cache\r\n");
					header.add("Content-Length: #CSIZE#\r\n");
					header.add("\r\n");
					
					reader = new FileInputStream(f);
					
					while ((len = reader.read(b, body_length, m)) > 0 ) {
						body_length += len;
					}
					reader.close();
					reader = null;
				}
				
				for (String s : header) {
					ma = Pattern.compile("#CSIZE#").matcher(s);
					if (ma.find()) {
						s = ma.replaceFirst(String.valueOf(body_length+1));
					}
					out.write(s);
					out.flush();
				}
				
				for (int n=0; n<body_length; n+=m) {
					if ((n+m) < body_length) {
						w.write(b, n, m);
					}
					else {
						int s = body_length-n;
						w.write(b, n, s);
					}
				}
				w.flush();
			}
				
		}
		catch (SocketException e) {
			System.err.println(e.toString());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			CommonUtils.closing(reader);
			CommonUtils.closing(in);
			CommonUtils.closing(r);
			CommonUtils.closing(out);
			CommonUtils.closing(w);
			CommonUtils.closing(sock);
		}
	}
}
