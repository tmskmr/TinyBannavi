package epgdump;

public class SVCdesc {

	int  descriptor_tag;
	int  descriptor_length;
	int  service_type;
	int  service_provider_name_length;
	byte[] service_provider_name = new byte[Util.MAXSECLEN];
	int  service_name_length;
	byte[] service_name = new byte[Util.MAXSECLEN];

}
