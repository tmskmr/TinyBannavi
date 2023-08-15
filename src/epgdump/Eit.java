package epgdump;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Eit {

	private static final byte[] convtmp = new byte[Util.MAXSECLEN];
	
	//
	private int parseEIThead(byte[] data, EIThead h) {
		int[] boff = {0};

		//memset(h, 0, sizeof(EIThead));

		h.table_id = (byte) Util.getBit(data, 0, boff, 8);
		h.section_syntax_indicator = Util.getBit(data, 0, boff, 1);
		h.reserved_future_use = Util.getBit(data, 0, boff, 1);
		h.reserved1 = Util.getBit(data, 0, boff, 2);
		h.section_length =Util.getBit(data, 0, boff,12);
		h.service_id = Util.getBit(data, 0, boff, 16);
		h.reserved2 = Util.getBit(data, 0, boff, 2);
		h.version_number = Util.getBit(data, 0, boff, 5);
		h.current_next_indicator = Util.getBit(data, 0, boff, 1);
		h.section_number = Util.getBit(data, 0, boff, 8);
		h.last_section_number = Util.getBit(data, 0, boff, 8);
		h.transport_stream_id = Util.getBit(data, 0, boff, 16);
		h.original_network_id = Util.getBit(data, 0, boff, 16);
		h.segment_last_section_number = Util.getBit(data, 0, boff, 8);
		h.last_table_id = Util.getBit(data, 0, boff, 8);
	  
		return 14;
	}

	//
	private int parseEITbody(byte[] data, int ptr, EITbody b) throws NumberFormatException {
		int[] boff = {0};
		int tnum;

		//memset(b, 0, sizeof(EITbody));

		b.event_id = Util.getBit(data, ptr, boff, 16);

		System.arraycopy(data, ptr+boff[0]/8, b.start_time, 0, 5);
		//memcpy(b.start_time, data + boff / 8, 5);
		/* b.start_time = getBit(data, &boff, 40); */
		boff[0] += 40;
		System.arraycopy(data, ptr+boff[0]/8, b.duration, 0, 3);
		//memcpy(b.duration, data + boff / 8, 3);
		/* b.duration = getBit(data, &boff, 24); */
		boff[0] += 24;
		b.running_status = Util.getBit(data, ptr, boff, 3);
		b.free_CA_mode = Util.getBit(data, ptr, boff, 1);
		b.descriptors_loop_length = Util.getBit(data, ptr, boff, 12);

		// 日付変換 - [鯛ナビ] どういう計算？
		tnum = (b.start_time[0] & 0xFF) << 8 | (b.start_time[1] & 0xFF);
		
		b.yy = (int) ((tnum - 15078.2) / 365.25);
		b.mm = (int) (((tnum - 14956.1) - (int)(b.yy * 365.25)) / 30.6001);
		b.dd = (tnum - 14956) - (int)(b.yy * 365.25) - (int)(b.mm * 30.6001);

		if ( b.mm == 14 || b.mm == 15 ) {
			b.yy += 1;
			b.mm = b.mm - 1 - (1 * 12);
		}
		else {
			b.mm = b.mm - 1;
		}

		b.yy += 1900;
	  
		b.hh = Util.hex2dec(b.start_time[2]);
		b.hm = Util.hex2dec(b.start_time[3]);
		b.ss = Util.hex2dec(b.start_time[4]);
		
		if ( ((b.duration[0]&0xFF) == 0xFF) && ((b.duration[1]&0xFF) == 0xFF) && ((b.duration[2]&0xFF) == 0xFF) ) {
			b.dhh = b.dhm = b.dss = 0;
		}
		else {
			b.dhh = Util.hex2dec(b.duration[0]);
			b.dhm = Util.hex2dec(b.duration[1]);
			b.dss = Util.hex2dec(b.duration[2]);
		}
		
		return 12;
	}
	
	//
	private EIT_CONTROL	searcheit(SVT_CONTROL svt, int servid, int eventid) {
		if ( svt == null ) {
			return null;
		}
		for ( EIT_CONTROL cur : svt.eittop ) {
			if ( (cur.event_id == eventid) && (cur.service_id == servid) ){
				return cur ;
			}
		}
		return null ;
	}

	//
	private int parseSEVTdesc(byte[] data, int ptr, SEVTdesc desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(SEVTdesc));

		desc.descriptor_tag = Util.getBit(data, ptr, boff, 8);
		if ( (desc.descriptor_tag & 0xFF) != ShortEventDescriptor ) {
			return -1;
		}
		desc.descriptor_length = Util.getBit(data, ptr, boff, 8);
		System.arraycopy(data, ptr+boff[0]/8, desc.ISO_639_language_code, 0, 3);
		//memcpy(desc.ISO_639_language_code, data + boff / 8, 3);
		/* desc.ISO_639_language_code = Util.getBit(data, ptr, boff, 24); */
		boff[0] += 24;
		desc.event_name_length = Util.getBit(data, ptr, boff, 8);
		Util.getStr(desc.event_name, data, ptr, boff, desc.event_name_length);
		desc.text_length = Util.getBit(data, ptr, boff, 8);
		Util.getStr(desc.text, data, ptr, boff, desc.text_length);

		return desc.descriptor_length + 2;
	}
	
	//
	private int parseContentDesc(byte[] data, int ptr, ContentDesc desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(ContentDesc));
	
		desc.descriptor_tag = Util.getBit(data, ptr, boff, 8);
		if ( (desc.descriptor_tag & 0xFF) != ContentDescriptor ) {
			return -1;
		}
		desc.descriptor_length = Util.getBit(data, ptr, boff, 8);
		System.arraycopy(data, ptr+boff[0]/8, desc.content, 0, desc.descriptor_length);
		//memcpy(desc.content, data+(boff/8), desc.descriptor_length);
		//getStr(desc.content, data, &boff, desc.descriptor_length);
		return desc.descriptor_length + 2;
	}
	
	//
	private int parseSeriesDesc(byte[] data, int ptr, SeriesDesc desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(SeriesDesc));

		desc.descriptor_tag = Util.getBit(data, ptr, boff, 8);
		if ( (desc.descriptor_tag & 0xFF) != 0xD5 ) {
			return -1;
		}
		desc.descriptor_length = Util.getBit(data, ptr, boff, 8);
		desc.series_id = Util.getBit(data, ptr, boff, 16);
		desc.repeat_label = Util.getBit(data, ptr, boff, 4);
		desc.program_pattern = Util.getBit(data, ptr, boff, 3);
		desc.expire_date_valid_flag = Util.getBit(data, ptr, boff, 1);

		desc.expire_date = Util.getBit(data, ptr, boff, 16);
		//memcpy(desc.expire_date, data + boff / 8, 2);
		//boff += 16;

		desc.episode_number = Util.getBit(data, ptr, boff, 12);
		desc.last_episode_number = Util.getBit(data, ptr, boff, 12);

		Util.getStr(desc.series_name_char, data, ptr, boff, desc.descriptor_length - 8);
		return desc.descriptor_length + 2;
	}


	//
	private int parseEEVTDhead(byte[] data, int ptr, EEVTDhead desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(EEVTDhead));
	
		desc.descriptor_tag = Util.getBit(data, ptr, boff, 8);
		if ( (desc.descriptor_tag & 0xFF) != ExtendedEventDescriptor ) {
			return -1;
		}
		desc.descriptor_length = Util.getBit(data, ptr, boff, 8);
		desc.descriptor_number = Util.getBit(data, ptr, boff, 4);
		desc.last_descriptor_number = Util.getBit(data, ptr, boff, 4);
		System.arraycopy(data, ptr+boff[0]/8, desc.ISO_639_language_code, 0, 3);
		//memcpy(desc.ISO_639_language_code, data + boff[0] / 8, 3);
		/* desc.ISO_639_language_code = Util.getBit(data, ptr, boff, 24); */
		boff[0] += 24;
	
		desc.length_of_items = Util.getBit(data, ptr, boff, 8);
	
		return 7;
	}
	
	//
	private int parseEEVTDitem(byte[] data, int ptr, EEVTDitem desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(EEVTDitem));
	
		desc.item_description_length = Util.getBit(data, ptr, boff, 8);
		System.arraycopy(data, ptr+boff[0]/8, desc.item_description, 0, desc.item_description_length);
		//Util.getStr(desc.item_description, data, ptr, boff, desc.item_description_length);
		boff[0] += desc.item_description_length * 8;
	
		desc.item_length = Util.getBit(data, ptr, boff, 8);
		System.arraycopy(data, ptr+boff[0] / 8, desc.item, 0, desc.item_length);
		//memcpy(desc.item, data + (boff / 8), desc.item_length);
		/* getStr(desc.item, data, &boff, desc.item_length); */
	
		return desc.item_description_length + desc.item_length + 2;
	}
	
	//
	private int parseEEVTDtail(byte[] data, int ptr, EEVTDtail desc) {
		int[] boff = {0};
	  
		//memset(desc, 0, sizeof(EEVTDtail));

		desc.text_length = Util.getBit(data, ptr, boff, 8);
		Util.getStr(desc.text, data, ptr, boff, desc.text_length);

		return desc.text_length + 1;
	}
	
	// [鯛ナビ] タイトルの整形は鯛ナビ本体でやるので省略
	private void enqueue(ArrayList<EIT_CONTROL> top, EIT_CONTROL eitptr) {
		//
		for ( int i=0; i<top.size(); i++ ) {
			EIT_CONTROL cur = top.get(i); 
			int ryy = cur.yy - eitptr.yy;
			int rmm = cur.mm - eitptr.mm;
			int rdd = cur.dd - eitptr.dd;
			int rc = (ryy == 0)?((rmm == 0)?((rdd == 0)?(0):(rdd)):(rmm)):(ryy); 
			if ( rc == 0 ) {
				int rhh = cur.hh - eitptr.hh;
				int rhm = cur.hm - eitptr.hm;
				int rss = cur.ss - eitptr.ss;
				rc = (rhh == 0)?((rhm == 0)?((rss == 0)?(0):(rss)):(rhm)):(rhh);
				if ( rc == 0 ) {
					return;
				}
				if ( rc > 0 ) {
					top.add(i,eitptr);
					return;
				}
			}
			if ( rc > 0 ) {
				top.add(i,eitptr);
				return;
			}
		}
		top.add(eitptr);
	}

	//
	private static SVT_CONTROL preSearchedSvt = null;
	private SVT_CONTROL searchsvt(ArrayList<SVT_CONTROL> svttop, EIThead eith) {
		if ( preSearchedSvt != null ) {
			if ( preSearchedSvt.enabled &&
					eith.service_id == preSearchedSvt.servive_id &&
					eith.original_network_id == preSearchedSvt.original_network_id &&
					eith.transport_stream_id == preSearchedSvt.transport_stream_id ) {
				return preSearchedSvt;
			}
		}
		for ( SVT_CONTROL svtcur : svttop ) {
			if ( svtcur.enabled &&
					eith.service_id == svtcur.servive_id &&
					eith.original_network_id == svtcur.original_network_id &&
					eith.transport_stream_id == svtcur.transport_stream_id ) {
				return preSearchedSvt = svtcur;
			}
		}
		return preSearchedSvt = null;
	}

	/*
	 *  [鯛ナビ] ここが肝
	 */
	
	private static final int ShortEventDescriptor		= 0x4D;
	private static final int ExtendedEventDescriptor	= 0x4E;
	private static final int ContentDescriptor			= 0x54;
	
	private static byte[] d_tmp = new byte[Util.MAXSECLEN];
	private static int d_tmp_len = -1;
	private static byte[] p_tmp = new byte[Util.MAXSECLEN];
	private static int p_tmp_len = -1;
	private static String dDesc;
	private static String pDesc;
	
	public boolean dumpEIT(byte[] data, ArrayList<SVT_CONTROL> svttop) {
		try {
			return _dumpEIT(data, svttop);
		}
		catch ( NumberFormatException e ) {
			System.err.println(e.toString());
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		return false;
	}
	private boolean _dumpEIT(byte[] data, ArrayList<SVT_CONTROL> svttop) throws UnsupportedEncodingException,NumberFormatException {
		
		// EIT
		EIThead eith = new EIThead();
		int len = parseEIThead(data, eith);
		int ptr = len;
		int loop_len = eith.section_length - (len - 3 + 4);	// 3は共通ヘッダ長 4はCRC
		
		SVT_CONTROL svt = searchsvt(svttop, eith);
		
		while ( loop_len > 0 ) {
			
			// 番組情報を一個ずつ処理する
			
			EITbody eitb = new EITbody();
			len = parseEITbody(data, ptr, eitb);
			ptr += len;
			loop_len -= len;

			// [鯛ナビ] ptr、loop_lenの扱いに不具合があったので修正
			int ptrbody = ptr;
			
			ptr += eitb.descriptors_loop_length;
			loop_len -= eitb.descriptors_loop_length;
			
			if ( svt == null ) {
				// 登録先がなければスキップ
				continue ;
			}

			if (Util.debug) System.err.println(String.format("on=%02x tr=%02x sv=%02x ev=%02x", eith.original_network_id, eith.transport_stream_id, eith.service_id, eitb.event_id));
		    
			// [鯛ナビ] cur->ehh = eitb.dhhはバグ？
			EIT_CONTROL cur = searcheit(svt, eith.service_id, eitb.event_id);
			if ( cur == null ) {
				switch ( data[ptrbody]&0xFF ) {
				case ShortEventDescriptor:		// parseSEVTdesc - タイトル
				case ExtendedEventDescriptor:	// parseEEVTDhead - 番組詳細
				case ContentDescriptor:			// parseContentDesc - ジャンル
					break;
				default:
					continue;	// 上記以外は無視しちゃえ
				}
				
				int[] ehms = { eitb.hh, eitb.hm, eitb.ss };
				timecmp(ehms, eitb.dhh, eitb.dhm, eitb.dss);
				int ehh = ehms[0];
				int ehm = ehms[1];
				int ess = ehms[2];
				
				cur = new EIT_CONTROL();
				cur.title = "(タイトルがありません)";	// タイトルがみつからない場合もあるので一応ダミーを設定しておく
				cur.event_id = eitb.event_id ;
				cur.service_id = eith.service_id ;
				cur.yy = eitb.yy;
				cur.mm = eitb.mm;
				cur.dd = eitb.dd;
				cur.hh = eitb.hh;
				cur.hm = eitb.hm;
				cur.ss = eitb.ss;
				cur.dhh = eitb.dhh;	// ここらへん
				cur.dhm = eitb.dhm;	// ここらへん
				cur.dss = eitb.dss;	// ここらへん
				cur.ehh = ehh;		// ここらへん
				cur.ehm = ehm;		// ここらへん
				cur.ess = ess ;		// ここらへん
				cur.table_id = eith.table_id ;
				enqueue(svt.eittop, cur);
				
				if ( Util.debug ) {
					System.err.println(String.format("%04d-%02d-%02dT%02d:%02d - %02d:%02d - %02d:%02d - %s - %s",
							cur.yy,cur.mm,cur.dd,cur.hh,cur.hm,cur.ehh,cur.ehm,cur.dhh,cur.dhm,
							cur.title, cur.subtitle));
				}
			}
			
			// テンポラリをリセット
			p_tmp_len = -1;
			pDesc = null;
			d_tmp_len = -1;
			dDesc = null;
			
			int pretag = -1;
			int loop_blen = eitb.descriptors_loop_length;
			
			while ( loop_blen > 0 ) {
				SEVTdesc sevtd = new SEVTdesc();
				len = parseSEVTdesc(data, ptrbody, sevtd);
				if ( len > 0 ) {
					
					/*
					 * 基本情報
					 */
					
					cur.title = Util.getText(sevtd.event_name, Util.strlen(sevtd.event_name), Util.thisEncoding).trim();
					cur.subtitle = Util.getText(sevtd.text, Util.strlen(sevtd.text), Util.thisEncoding).trim();
					cur.subtitle = ( ! cur.subtitle.matches("^[ 　\\t\\r\\n]*$"))?(cur.subtitle):(null);
				}
				else {
					
					/*
					 * 詳細情報だが、基本情報よりも先に来る場合があるので注意 
					 */
					
					EEVTDhead eevthead = new EEVTDhead();
					len = parseEEVTDhead(data, ptrbody, eevthead);

					if ( len > 0 ) {
						
						// 連続する拡張イベントは、漢字コードが泣き別れして分割されるようだ。
						// 連続かどうかは、item_description_lengthが設定されているかどうかで判断できるようだ。
						
						// 拡張形式イベント記述子の場合
						ptrbody += len;
						loop_blen -= len;
						
						int loop_elen = eevthead.length_of_items;
						while ( loop_elen > 0 ) {
							EEVTDitem eevtitem = new EEVTDitem();
							len = parseEEVTDitem(data, ptrbody, eevtitem);
							
							/* 例:
							 	on=04 tr=40d1 sv=b5 ev=44dd
								# 08 210 200 8 <-- ここから
								# 00 90 88 0
								# 08 210 200 8
								# 00 108 106 0
								# 08 210 200 8
								# 00 97 95 0
								# 08 210 200 8
								# 00 29 27 0   <-- ここまで連続している
								# 06 163 155 6
							*/
							
							int curtag = data[ptrbody]&0xFF;
							String curdesc = getDesc(eevtitem);
							
							if (Util.debug) System.err.println(String.format("# %02x %d %d %d %s",curtag,len,eevtitem.item_length,eevtitem.item_description_length,curdesc));
						
							if ( d_tmp_len == -1 ) {
								// 初回ならリセットしよう
								cur.detail = "";
								d_tmp_len = 0;
								cur.performer = "";
								p_tmp_len = 0;

							}
							
							attachDesc(cur,eevtitem,curdesc,curtag,pretag);
							if ( curtag != 0x00 ) {
								pretag = curtag;
							}

							ptrbody += len;
							loop_elen -= len;
							loop_blen -= len;
						}
						
						EEVTDtail eevttail = new EEVTDtail();
						len = parseEEVTDtail(data, ptrbody, eevttail);
					}
					else {
						
						/*
						 * ここから先は、実は使っていない 
						 */
						
						// 拡張形式イベント記述子以外の場合
						ContentDesc contentDesc = new ContentDesc();
						len = parseContentDesc(data, ptrbody, contentDesc);
						
						if ( len > 0 ) {
							if ( cur != null ) {
								if ( cur.content_type.size() == 0 ) {
									for ( int i=0; i<contentDesc.descriptor_length; i+=2 ) {
										cur.content_type.add(String.format("%02X", contentDesc.content[i]&0xFF));
									}
								}
								else {
									//System.err.println("[DUP G]");
								}
							}
						}
						else {
							SeriesDesc seriesDesc = new SeriesDesc();
							len = parseSeriesDesc(data, ptrbody, seriesDesc);
							if ( len > 0 ) {
								// 処理はない
							}
							else {
								len = Util.parseOTHERdesc(data, ptrbody);
							}
						}
					}
				}
				
				ptrbody += len;
				loop_blen -= len;
			}
			
			// 拡張形式イベント記述子のまとめ処理
			attachDesc(cur, null, null, -1, -1);
			if (cur.detail != null) cur.detail = cur.detail.replaceFirst("\n+$", "");
			if (cur.performer != null) cur.performer = cur.performer.replaceFirst("\n+$", "");
		}
		
		return true;
	}

	// 拡張形式イベント記述子を番組詳細として再構築するよ
	private void attachDesc(EIT_CONTROL cur, EEVTDitem eevtitem, String curdesc, int curtag, int pretag) {
		if ( eevtitem == null || curtag == 0x06 || (curtag == 0x00 && pretag == 0x06) ) {
			// 出演者情報（それ以外もあるっぽいけど）
			if ( curtag != 0x00 ) {
				// 先頭タグで
				if ( p_tmp_len > 0 ) {
					// なんかデータあるなら文字列化しておく
					if ( pDesc != null && pDesc.length() > 0) {
						if ( ! pDesc.matches("^[\\[［(（【].*") ) {
							cur.performer += "【"+pDesc+"】\n";
						}
						else {
							cur.performer += pDesc+"\n";
						}
					}
					int[] bo = {0};
					int ln = Util.getStr(convtmp, p_tmp, 0, bo, p_tmp_len);
					cur.performer += Util.getText(convtmp, ln, Util.thisEncoding).trim()+"\n\n";
					p_tmp_len = 0;
				}
				//
				pDesc = curdesc;
			}
			if ( eevtitem != null ) {
				// まとめ処理の場合はここは通らない
				System.arraycopy(eevtitem.item, 0, p_tmp, p_tmp_len, eevtitem.item_length);
				p_tmp_len += eevtitem.item_length;
			}
		}
		if ( eevtitem == null || (curtag != 0x00 && curtag != 0x06) || (curtag == 0x00 && pretag != 0x06) ) {
			// 出演者情報以外
			if ( curtag != 0x00 ) {
				// 先頭タグで
				if ( d_tmp_len > 0 ) {
					// なんかデータあるなら文字列化しておく
					if ( dDesc != null && dDesc.length() > 0) {
						if ( ! dDesc.matches("^[\\[［(（【].*") ) {
							cur.detail += "【"+dDesc+"】\n";
						}
						else {
							cur.detail += dDesc+"\n";
						}
					}
					int[] bo = {0};
					int ln = Util.getStr(convtmp, d_tmp, 0, bo, d_tmp_len);
					cur.detail += Util.getText(convtmp, ln, Util.thisEncoding).trim()+"\n\n";
					d_tmp_len = 0;
				}
				//
				dDesc = curdesc;
			}
			if ( eevtitem != null ) {
				// まとめ処理の場合はここは通らない
				System.arraycopy(eevtitem.item, 0, d_tmp, d_tmp_len, eevtitem.item_length);
				d_tmp_len += eevtitem.item_length;
			}
		}
	}
	
	//
	private String getDesc(EEVTDitem eevtitem) {
		if ( eevtitem.item_description_length > 0 ) {
			// どうも"番組内容"とか"制作著作"とかかいてあるみたい
			int[] bo = {0};
			int ln = Util.getStr(convtmp, eevtitem.item_description, 0, bo, eevtitem.item_description_length);
			return Util.getText(convtmp, ln, Util.thisEncoding);
		}
		return null;
	}
	
	//
	private void timecmp(int[] thms, int dhh, int dmm, int dss) {
	
		int ama;
	
		thms[2] += dss;
		ama = thms[2] % 60;
		thms[1] += (thms[2] / 60);
		thms[2] = ama;
	
		thms[1] += dmm;
		ama   = thms[1] % 60;
		thms[0] += (thms[1] / 60);
		thms[1]  = ama;
	
		thms[0] += dhh;
	
	}
}
