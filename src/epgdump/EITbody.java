package epgdump;

public class EITbody {

	int event_id;
	byte[] start_time = new byte[5];
	byte[] duration = new byte[3];
	int running_status;
	int free_CA_mode;
	int descriptors_loop_length;
	/* 以下は解析結果保存用 */
	int yy;
	int mm;
	int dd;
	int hh;
	int hm;
	int ss;
	int dhh;
	int dhm;
	int dss;

}
