package com.sw_engineering_candies.xing;

public interface Constants {

	/** my XING profile site */
	static final String MY_CONTACTS_URL = "https://www.xing.com/app/profile?op=contacts;"
			+ "name=%s;offset=%d";
	
	static final String XPATH_MY_CONTACTS = "//*[@id=\"profile-contacts\"]/*[local-name()='tbody']/*"
			+ "[local-name()='tr']/*[local-name()='td']/*[local-name()='strong']/*[local-name()='a']";

	/** my indirect contact information on XING */
	static final String IDIRECT_CONTACTS_URL = "https://www.xing.com/app/profile?op="
			+ "showroutes;except=1;name=%s;offset=%d";
	
	final static String XPATH_INDIRECT_CONTACTS = "//*[@id=\"maincontent\"]/*[local-name()='ul']";

}