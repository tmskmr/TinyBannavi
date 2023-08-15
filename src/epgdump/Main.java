package epgdump;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/*
 * これはデバッグ用のダミーコードです。
 * 実際の本体はEpgdump.javaです。
 */

public class Main {

	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws UnsupportedEncodingException {
		
		Epgdump epg = new Epgdump();
		
		int mode = 0;
		
		boolean showSvt = false;
		boolean showEit = true;
		int evid = 0xB6B0;
		//int evid = -1;
		
		String root;
		String fnext;
		String fnpre = "00044671";
		//String fnpre = null;
		if ( mode == 1 ) {
			root = "F:/Videos/";
			fnext = ".ts";
		}
		else {
			root = "D:/PT2/EpgDataCap_Bon/Setting/EpgData/";
			fnext = ".dat";
		}
		
		new File("Z:/epgdump.log").delete();
		System.setOut(new tainavi.DebugPrintStream(System.out,"Z:/epgdump.log",true));
		System.setErr(new tainavi.DebugPrintStream(System.err,"Z:/epgdump.log",true));
		
		ArrayList<SVT_CONTROL> svta = null;
		File d = new File(root);
		if ( d.isDirectory() ) {
			svta = (ArrayList<SVT_CONTROL>) tainavi.CommonUtils.readXML("Z:/svt.xml");
			if ( svta == null ) {
				svta  = new ArrayList<SVT_CONTROL>();
				// SVT
				for ( String fn : d.list() ) {
					File f = new File(root+fn);
					if ( f.isFile() && fn.endsWith(fnext) ) {
						System.out.print("fn="+fn);
						ArrayList<SVT_CONTROL> svttop = epg.getSvtControl(root+fn);
						for ( SVT_CONTROL svt : svttop ) {
							epg.enqueueSVT(svta, svt);
						}
						System.out.println(", sz="+svta.size());
					}
				}
				
				tainavi.CommonUtils.writeXML("Z:/svt.xml",svta);
			}
			for ( SVT_CONTROL svt : svta ) {
				svt.enabled = true;
				if ( showSvt ) {
					System.out.print(String.format("+ onid=%04x",svt.original_network_id));
					System.out.print(String.format(" tid=%04x",svt.transport_stream_id));
					System.out.print(String.format(" evid=%04x",svt.servive_id));
					System.out.println(String.format(" %s",svt.servicename));
				}
			}
			
			// EIT
			for ( String fn : d.list() ) {
				File f = new File(root+fn);
				if ( f.isFile() && fn.endsWith(fnext) && (fnpre == null || fnpre!=null && fn.startsWith(fnpre)) ) {
					System.out.print(fn);
					epg.getEitControl(root+fn, svta);
					int cnt = 0;
					int cnte = 0;
					for ( SVT_CONTROL svt : svta ) {
						for ( EIT_CONTROL eit : svt.eittop ) {
							if (eit.title!=null&&eit.content_type.size()>0) cnt++;
						}
						cnte += svt.eittop.size();
					}
					System.out.println(", sz="+cnt+"/"+cnte);
				}
			}
			if ( showEit ) {
				for ( SVT_CONTROL svt : svta ) {
					System.out.println("@ "+svt.servicename);
					if ( svt.eittop.size() <= 0 ) {
						continue;
					}
					System.out.print(String.format("+ onid=%04x",svt.original_network_id));
					System.out.print(String.format(" tid=%04x",svt.transport_stream_id));
					System.out.print(String.format(" svid=%04x",svt.servive_id));
					System.out.println(String.format(" %s",svt.servicename));
					for ( EIT_CONTROL eit : svt.eittop ) {
						if ( evid != -1 && eit.event_id != evid ) {
							continue;
						}
						if ( eit.title == null ) {
							continue;
						}
						System.out.println(String.format("%04x %04d-%02d-%02dT%02d:%02d - %02d:%02d - %02d:%02d - %s - %s - %s",
								eit.event_id,eit.yy,eit.mm,eit.dd,eit.hh,eit.hm,eit.ehh,eit.ehm,eit.dhh,eit.dhm,
								eit.title, eit.subtitle, (eit.detail!=null)?(eit.detail.replaceAll("\n", "+")):("")));
						if (eit.detail != null) System.out.println("+ "+eit.detail);
						if (eit.performer != null) System.out.println("- "+eit.performer);
						for ( int i=0; i<eit.content_type.size(); i++ ) {
							System.out.print(String.format(" * %s",eit.content_type.get(i)));
						}
						System.out.println("");
					}
				}
			}
		}
	}

}
