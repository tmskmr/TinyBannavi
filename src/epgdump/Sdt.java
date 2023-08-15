package epgdump;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Sdt {

	//
	private int parseSDThead(byte[] data, int ptr, SDThead h) {
		int[] boff = {0};

		//memset(h, 0, sizeof(SDThead));

		h.table_id = Util.getBit(data, ptr, boff, 8);
		h.section_syntax_indicator = Util.getBit(data, ptr, boff, 1);
		h.reserved_future_use1 = Util.getBit(data, ptr, boff, 1);
		h.reserved1 = Util.getBit(data, ptr, boff, 2);
		h.section_length = Util.getBit(data, ptr, boff, 12);
		h.transport_stream_id = Util.getBit(data, ptr, boff, 16);
		h.reserved2 = Util.getBit(data, ptr, boff, 2);
		h.version_number = Util.getBit(data, ptr, boff, 5);
		h.current_next_indicator = Util.getBit(data, ptr, boff, 1);
		h.section_number = Util.getBit(data, ptr, boff, 8);
		h.last_section_number = Util.getBit(data, ptr, boff, 8);
		h.original_network_id = Util.getBit(data, ptr, boff, 16);
		h.reserved_future_use2 = Util.getBit(data, ptr, boff, 8);

		return 11;
	}
	
	//
	private int parseSDTbody(byte[] data, int ptr, SDTbody b) {
		int[] boff = {0};

		//memset(b, 0, sizeof(SDTbody));

		b.service_id = Util.getBit(data, ptr, boff, 16);
		b.reserved_future_use1 = Util.getBit(data, ptr, boff, 3);
		b.EIT_user_defined_flags = Util.getBit(data, ptr, boff, 3);
		b.EIT_schedule_flag = Util.getBit(data, ptr, boff, 1);
		b.EIT_present_following_flag = Util.getBit(data, ptr, boff, 1);
		b.running_status = Util.getBit(data, ptr, boff, 3);
		b.free_CA_mode = Util.getBit(data, ptr, boff, 1);
		b.descriptors_loop_length = Util.getBit(data, ptr, boff, 12);

		return 5;
	}
	
	//
	private int parseSVCdesc(byte[] data, int ptr, SVCdesc desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(SVCdesc));

		desc.descriptor_tag = Util.getBit(data, ptr, boff, 8);
		desc.descriptor_length = Util.getBit(data, ptr, boff, 8);
		desc.service_type = Util.getBit(data, ptr, boff, 8);
		desc.service_provider_name_length = Util.getBit(data, ptr, boff, 8);
		Util.getStr(desc.service_provider_name, data, ptr, boff, desc.service_provider_name_length);
		desc.service_name_length = Util.getBit(data, ptr, boff, 8);
		Util.getStr(desc.service_name, data, ptr, boff, desc.service_name_length);

		return desc.descriptor_length + 2;
	}
	
	//
	private int serachid(ArrayList<SVT_CONTROL> top, int service_id) {
		for ( SVT_CONTROL cur : top ) {
			if ( cur.servive_id == service_id ) {
				return 1 ;
			}
		}
		return 0 ;
	}
	
	//
	private void enqueue_sdt(ArrayList<SVT_CONTROL> top, SVT_CONTROL sdtptr) {
		for ( int i=0; i<top.size(); i++ ) {
			SVT_CONTROL cur = top.get(i); 
			if ( sdtptr.servive_id < cur.servive_id ) {
				top.add(i,sdtptr);
				break;
			}
		}
		top.add(sdtptr);
	}


	//
	public boolean dumpSDT(byte[] data, ArrayList<SVT_CONTROL> top) {
		try {
			return _dumpSDT(data, top);
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		return false;
	}
	private boolean _dumpSDT(byte[] data, ArrayList<SVT_CONTROL> top) throws UnsupportedEncodingException {

		// SDT
		SDThead  sdth = new SDThead();
		int len = parseSDThead(data, 0, sdth); 
		int ptr = len;
		int loop_len = sdth.section_length - (len - 3 + 4); // 3は共通ヘッダ長 4はCRC
		while ( loop_len > 0 ) {
			SDTbody  sdtb = new SDTbody();
			len = parseSDTbody(data, ptr, sdtb);
			ptr += len;
			loop_len -= len;
			
			SVCdesc  desc = new SVCdesc();
			parseSVCdesc(data, ptr, desc);

			int rc = serachid(top, sdtb.service_id);
			if ( rc == 0 ) {
				SVT_CONTROL svtptr = new SVT_CONTROL();
				svtptr.servive_id = sdtb.service_id;
				svtptr.original_network_id = sdth.original_network_id;
				svtptr.transport_stream_id = sdth.transport_stream_id;
				svtptr.servive_id = sdtb.service_id;
				svtptr.servicename = Util.getText(desc.service_name, Util.strlen(desc.service_name), Util.thisEncoding); 
				//memcpy(svtptr->servicename, desc.service_name, strlen(desc.service_name));
				enqueue_sdt(top, svtptr);
			}

			ptr += sdtb.descriptors_loop_length;
			loop_len -= sdtb.descriptors_loop_length;
		}
	  
		return true;
	}
}
