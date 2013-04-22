/**
 * Copyright (C) 2012-2013, Markus Sprunck
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - The name of its contributor may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 
 */

package com.sw_engineering_candies.xing;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import nu.validator.htmlparser.dom.HtmlDocumentBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.WebClient;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class XingPageReader {

	private static final Log LOGGER = LogFactory.getLog(XingPageReader.class);

	private static final String NL = System.getProperty("line.separator");

	private static final int MAX_NUMBER_OF_DIRECT_CONTACT = 160;

	public static  String pwd;

	public static  String userName;
	
	public static  String name;

	final WebClient webClient = new WebClient();

	final WebConversation conversation = new WebConversation();

	List<String> myContacts = new ArrayList<String>();

	private final Model model;

	public XingPageReader(final Model analyser) {
		this.model = analyser;
	}

	public void run() {
		readProperties();
		login();
		crawlAllMyContacts();
		crawlAllMyIndirectContacts();
	}

	public int login() {
		
	
		
		final String requestedURL = "https://www.xing.com";
		LOGGER.info("requestedURL=" + requestedURL + NL);
		final WebRequest req = new GetMethodWebRequest(requestedURL);
		final NodeList childNodes = null;
		int responseCode = 0;
		try {
			WebResponse response;
			HttpUnitOptions.setScriptingEnabled(true);
			HttpUnitOptions.setExceptionsThrownOnScriptError(false);
			final WebResponse res = conversation.getResource(req);
			final WebForm form = res.getForms()[0];
			form.setParameter("login_form[username]", userName);
			form.setParameter("login_form[password]", pwd);
			response = form.submit();
			responseCode = response.getResponseCode();
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final SAXException e) {
			e.printStackTrace();
		}
		LOGGER.info("childNodes =" + (null != childNodes ? childNodes.getLength() : 0) + NL);
		return responseCode;
	}

	private void readProperties() {
		java.util.Properties prop = new Properties();		
	    try {
			prop.load(new FileInputStream("XingCrawler.ini"));
			if (prop.containsKey("https.proxySet") ) {
				System.setProperty("https.proxySet", prop.getProperty("https.proxySet"));
			}
			if (prop.containsKey("https.proxyHost") ) {
				System.setProperty("https.proxyHost", prop.getProperty("https.proxyHost"));
			}
			if (prop.containsKey("https.proxyPort") ) {
				System.setProperty("https.proxyPort", prop.getProperty("https.proxyPort"));
			}
		
			userName = prop.getProperty("xing.userId").trim();
			pwd   = prop.getProperty("xing.userPwd").trim();
			name = prop.getProperty("xing.userName").trim();
			
		} catch (FileNotFoundException e1) {
			LOGGER.error(e1.getMessage());
		} catch (IOException e1) {
			LOGGER.error(e1.getMessage());			
		}
	}

	public NodeList readNodeListXING(final String requestedURL, String xpathExpression) {
		LOGGER.info("requestedURL=" + requestedURL + NL);
		final WebRequest req = new GetMethodWebRequest(requestedURL);
		try {
			final WebResponse response = conversation.getResource(req);
			if (200 == response.getResponseCode()) {

				HtmlDocumentBuilder b = new HtmlDocumentBuilder();
				org.w3c.dom.Document doc = b.parse(response.getInputStream());
				XPath xpath = XPathFactory.newInstance().newXPath();

				NodeList nodes = (NodeList) xpath.evaluate(xpathExpression, doc.getDocumentElement(),
						XPathConstants.NODESET);
				LOGGER.info("childNodes  =" + (null != nodes ? nodes.getLength() : 0) + NL);

				return nodes;
			}

		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final SAXException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void crawlAllMyContacts() {
		LOGGER.info("crawl all my contacts " + NL);
		for (int offset = 0; offset < MAX_NUMBER_OF_DIRECT_CONTACT; offset += 10) {
			final String contactURL = "https://www.xing.com/app/profile?op=contacts;name="+name+";offset="
					+ offset;

			// XPATH //*[@id="profile-contacts"]/tbody/tr[1]/td[2]/strong/a
			String xpathExpression = "//*[@id=\"profile-contacts\"]/*[local-name()='tbody']/*[local-name()='tr']/*[local-name()='td']/*[local-name()='strong']/*[local-name()='a']";
			NodeList nodes = readNodeListXING(contactURL, xpathExpression);
			if (null != nodes) {
				for (int i = 0; i < nodes.getLength(); i++) {
					String name = nodes.item(i).getAttributes().getNamedItem("href").getNodeValue().split("/")[2];
					myContacts.add(name);
					LOGGER.info(i + " add '" + name + "'\n");
				}

			} else {
				LOGGER.info("ready" + NL);
				break;
			}

		}
		LOGGER.info("crawled " + myContacts.size() + " contacts" + NL);
	}

	private void crawlAllMyIndirectContacts() {

		for (String contact : myContacts) {
			crawlInderectContacts(contact);
		}

	}

	public void crawlInderectContacts(final String name) {
		// this site uses maximal 10 items per page
		for (int offset = 0; offset < MAX_NUMBER_OF_DIRECT_CONTACT; offset += 10) {
			LOGGER.info("crawl all contacts of " + name + NL);
			final String ContactURL = "https://www.xing.com/app/profile?op=showroutes;except=1;name=" + name
					+ ";offset=" + offset;

			String xpathExpression = "//*[@id=\"maincontent\"]/*[local-name()='ul']";
			final NodeList allElements = readNodeListXING(ContactURL, xpathExpression);
			if (null != allElements) {
				boolean hasPageinator = false;
				for (int i = 0; i < allElements.getLength(); i++) {
					final Node item = allElements.item(i);
					final List<Model.Item> listItems = new ArrayList<Model.Item>();

					final String classAttribute = item.getAttributes().getNamedItem("class").getNodeValue();
					if (classAttribute.startsWith("image-list contact-path-list contact-path-list-")) {
						final NodeList nodes = item.getChildNodes();
						for (int col = 3; col < nodes.getLength(); col++) {
							final Node item2 = nodes.item(col);
							if ("LI".equalsIgnoreCase(item2.getNodeName())) {
								final NodeList nodes3 = item2.getChildNodes();
								for (int attr = 0; attr < nodes3.getLength(); attr++) {
									final Node item3 = nodes3.item(attr);
									if ("A".equalsIgnoreCase(item3.getNodeName())) {
										final String nodeValue = item3.getAttributes().getNamedItem("href")
												.getNodeValue().split("/")[2];
										final String item3Class = item3.getAttributes().getNamedItem("class")
												.getNodeValue();
										if ("user-name".equalsIgnoreCase(item3Class)) {
											final Model.Item entrySource = model.new Item();
											entrySource.setNodeName(nodeValue.replace('_', ' '));
											entrySource
													.setClusterName(myContacts.contains(nodeValue) ? "Level-1-Contacts"
															: "Level-" + col / 2 + "-Contacts");
											listItems.add(entrySource);
											LOGGER.info("found " + nodeValue + NL);
										}
									}

								}
							}
						}
						model.append(listItems);

						if (classAttribute.equals("pagination")) {
							hasPageinator = true;
							final NodeList nodes2 = item.getChildNodes();
							for (int f = 0; f < nodes2.getLength(); f++) {
								final NodeList nodes3 = nodes2.item(f).getChildNodes();
								for (int g = 0; g < nodes3.getLength(); g++) {
									if (nodes3.item(g).hasChildNodes()) {
										final Node firstChild = nodes3.item(g).getFirstChild();
										if ("Next".equalsIgnoreCase(firstChild.getNodeValue())
												&& "SPAN".equals(nodes3.item(g).getLocalName())) {
											offset = MAX_NUMBER_OF_DIRECT_CONTACT;
										}
									}
								}
							}
						}
					}
				}
				if (!hasPageinator) {
					offset = MAX_NUMBER_OF_DIRECT_CONTACT;
				}
			}
		}
	}
}