package tainavi;

import java.util.ArrayList;



public class b64 {
	
	private static final String list = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	public static String enc(byte[] src) {
		String out = "";
		
		if (src == null) {
			return null;
		}
		
		int n = src.length;
		byte[] b = new byte[n+1];
		b[n] = 0x00;
		System.arraycopy(src, 0, b, 0, n);
		
		int x;
		int y;
		int j;
		byte c = 0;
		for (int i=0; i<n*8; i+=6) {
			j = (i-i%8)/8;
			switch (i%8) {
			case 0:
				x = (int)b[j];
				x &= 0x000000fc;
				x >>= 2;
				c = (byte)x;
				break;
			case 1:
				x = (int)b[j];
				x &= 0x0000007e;
				x >>= 1;
				c = (byte)x;
				break;
			case 2:
				x = (int)b[j];
				x &= 0x0000003f;
				c = (byte)x;
				break;
			case 3:
				x = (int)b[j];
				x &= 0x0000001f;
				x <<= 1;
				y = (int)b[j+1];
				y &= 0x00000080;
				y >>= 7;
				c = (byte)(x|y);
				break;
			case 4:
				x = (int)b[j];
				x &= 0x0000000f;
				x <<= 2;
				y = (int)b[j+1];
				y &= 0x000000C0;
				y >>= 6;
				c = (byte)(x|y);
				break;
			case 5:
				x = (int)b[j];
				x &= 0x00000007;
				x <<= 3;
				y = (int)b[j+1];
				y &= 0x000000E0;
				y >>= 5;
				c = (byte)(x|y);
				break;
			case 6:
				x = (int)b[j];
				x &= 0x00000003;
				x <<= 4;
				y = (int)b[j+1];
				y &= 0x000000F0;
				y >>= 4;
				c = (byte)(x|y);
				break;
			case 7:
				x = (int)b[j];
				x &= 0x00000001;
				x <<= 5;
				y = (int)b[j+1];
				y &= 0x000000F8;
				y >>= 3;
				c = (byte)(x|y);
				break;
			}
			out += list.substring((int)c,(int)c+1);
		}
		for (int i=out.length(); i%4 != 0; i++) {
			out += "=";
		}
		return(out);
	}
		
	public static byte[] dec(String src){
		
		if (src == null) {
			return null;
		}
		
		ArrayList<Byte> ba = new ArrayList<Byte>();
		
		int x;
		int y;
		int m = 0;
		byte c = 0;
		int n = src.length();
		for (int i=0; i<n; i++) {
			//
			int b = list.indexOf(src.charAt(i));
			if (b == -1) {
				break;
			}
			
			//
			//int j = i / 4;
			switch (m = i%4) {
			case 0:
				x = b;
				x &= 0x0000003f;
				x <<= 2;
				c = (byte)x;
				ba.add(c);
				break;
			case 1:
				x = b;
				x &= 0x00000030;
				x >>= 4;
				c = ba.remove(ba.size()-1);
				c |= (byte)x;
				ba.add(c);
				//
				y = b;
				y &= 0x0000000f;
				y <<= 4;
				c = (byte)y;
				ba.add(c);
				break;
			case 2:
				x = b;
				x &= 0x0000003C;
				x >>= 2;
				c = ba.remove(ba.size()-1);
				c |= (byte)x;
				ba.add(c);
				//
				y = b;
				y &= 0x00000003;
				y <<= 6;
				c = (byte)y;
				ba.add(c);
				break;
			case 3:
				x = b;
				x &= 0x0000003f;
				c = ba.remove(ba.size()-1);
				c |= (byte)x;
				ba.add(c);
				break;
			}
		}
		
		// 端数切捨て
		if (ba.size() > 0 && m != 3) {
			ba.remove(ba.size()-1);
		}
		
		// リスト→配列
		byte[] bb = new byte[ba.size()];
		int i = 0;
		for (Byte b : ba) {
			bb[i++] = b;
		}
		
		return bb;
	}
}
