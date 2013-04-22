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
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebForm;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class XingPageReader {

	/** you may change log4j.properties */
	private static final Log LOGGER = LogFactory.getLog(XingPageReader.class);

	/** my password used for XING login */
	public static String pwd;

	/** my user name used for XING login */
	public static String userName;

	/** my name in the URL for accessing the XING profile */
	public static String name;

	/** all crawled data for creating the visualization of the network graph */
	private final Model model;

	/** context for a series of HTTP requests */
	final WebConversation conversation = new WebConversation();

	public XingPageReader(final Model analyser) {
		model = analyser;
	}

	public void run() {

		if (!readProperties()) {
			LOGGER.error("unable to read ini file with login information");
			System.exit(0);
		}

		if (!login()) {
			LOGGER.error("unable to login (xing.userId or xing.userPwd may be wrong)");
			System.exit(0);
		}

		final List<String> contacts = crawlAllMyContacts();
		if (!contacts.isEmpty()) {
			for (final String contact : contacts) {
				crawlInderectContacts(contact, contacts);
			}
		}
		LOGGER.info("crawling is ready");
	}

	private boolean readProperties() {
		final java.util.Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("XingCrawler.ini"));
			if (prop.containsKey("https.proxySet")) {
				System.setProperty("https.proxySet", prop.getProperty("https.proxySet"));
			}
			if (prop.containsKey("https.proxyHost")) {
				System.setProperty("https.proxyHost", prop.getProperty("https.proxyHost"));
			}
			if (prop.containsKey("https.proxyPort")) {
				System.setProperty("https.proxyPort", prop.getProperty("https.proxyPort"));
			}
			userName = prop.getProperty("xing.userId").trim();
			pwd = prop.getProperty("xing.userPwd").trim();
			name = prop.getProperty("xing.userName").trim();

			LOGGER.info("file 'XingCrawler.ini' read");
			return true;

		} catch (final FileNotFoundException e1) {
			LOGGER.error(e1.getMessage());
		} catch (final IOException e1) {
			LOGGER.error(e1.getMessage());
		}

		return false;
	}

	public boolean login() {
		LOGGER.info("login to XING ");
		boolean result = false;
		final WebRequest req = new GetMethodWebRequest("https://www.xing.com");
		try {
			HttpUnitOptions.setScriptingEnabled(true);
			HttpUnitOptions.setExceptionsThrownOnScriptError(false);
			final WebResponse res = conversation.getResource(req);
			final WebForm form = res.getForms()[0];
			form.setParameter("login_form[username]", userName);
			form.setParameter("login_form[password]", pwd);
			WebResponse response = form.submit();
			result = "/app/user".equalsIgnoreCase(response.getURL().getPath());
		} catch (final IOException e) {
			LOGGER.error(e.getMessage());
		} catch (final SAXException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	public List<String> crawlAllMyContacts() {
		LOGGER.info("crawl all my XING contacts (this may last some minutes) ");
		final List<String> result = new ArrayList<String>();
		int offset = 0;
		boolean hasMoreContacts = true;
		do {
			final String currentContactURL = String.format(Constants.MY_CONTACTS_URL, name, offset);
			final NodeList nodes = crawlCurrentContacts(currentContactURL, Constants.XPATH_MY_CONTACTS);
			if (null != nodes && 0 < nodes.getLength()) {
				for (int i = 0; i < nodes.getLength(); i++) {
					final Node namedItem = nodes.item(i).getAttributes().getNamedItem("href");
					final String name = namedItem.getNodeValue().split("/")[2];
					result.add(name);
					LOGGER.debug(i + " add '" + name);
				}
				offset += 10;
			} else {
				hasMoreContacts = false;
			}
		} while (hasMoreContacts);

		return result;
	}

	public NodeList crawlCurrentContacts(final String requestedURL, String xpathExpression) {
		LOGGER.debug("requestedURL=" + requestedURL);
		final WebRequest req = new GetMethodWebRequest(requestedURL);
		try {
			final WebResponse response = conversation.getResource(req);
			if (200 == response.getResponseCode()) {
				final HtmlDocumentBuilder b = new HtmlDocumentBuilder();
				final org.w3c.dom.Document doc = b.parse(response.getInputStream());
				final XPath xpath = XPathFactory.newInstance().newXPath();
				final Element document = doc.getDocumentElement();
				return (NodeList) xpath.evaluate(xpathExpression, document, XPathConstants.NODESET);
			}
		} catch (final IOException e) {
			LOGGER.error(e.getMessage());
		} catch (final SAXException e) {
			LOGGER.error(e.getMessage());
		} catch (final XPathExpressionException e) {
			LOGGER.error(e.getMessage());
		}

		return null;
	}

	public void crawlInderectContacts(final String name, List<String> myContacts) {
		LOGGER.info("crawl all inderect contacts of " + name);
		int offset = 0;
		boolean hasMoreContacts = true;
		do {
			final String currentContactURL = String.format(Constants.IDIRECT_CONTACTS_URL, name, offset);
			final NodeList allElements;
			allElements = crawlCurrentContacts(currentContactURL, Constants.XPATH_INDIRECT_CONTACTS);
			if (null != allElements) {
				hasMoreContacts = analyseAllElements(myContacts, allElements);
			}
			offset += 10;
		} while (hasMoreContacts);
	}

	private boolean analyseAllElements(List<String> myContacts, final NodeList elements) {
		boolean hasPageinator = false;
		for (int i = 0; i < elements.getLength(); i++) {

			// This is an indirect connection
			final Node itemNode = elements.item(i);
			final String itemAttr = itemNode.getAttributes().getNamedItem("class").getNodeValue();
			if (itemAttr.startsWith("image-list contact-path-list contact-path-list-")) {
				final List<Model.Item> listItems = new ArrayList<Model.Item>();
				final NodeList nodes = itemNode.getChildNodes();
				for (int col = 3; col < nodes.getLength(); col++) {
					final Node item2 = nodes.item(col);
					if ("LI".equalsIgnoreCase(item2.getNodeName())) {
						final NodeList nodes3 = item2.getChildNodes();
						for (int attr = 0; attr < nodes3.getLength(); attr++) {
							final Node item3 = nodes3.item(attr);
							if ("A".equalsIgnoreCase(item3.getNodeName())) {
								NamedNodeMap attr3 = item3.getAttributes();
								final String nodeValue = attr3.getNamedItem("href").getNodeValue().split("/")[2];
								final String item3Class = attr3.getNamedItem("class").getNodeValue();
								if ("user-name".equalsIgnoreCase(item3Class)) {
									final Model.Item entrySource = model.new Item();
									entrySource.setNodeName(nodeValue.replace('_', ' '));
									if (myContacts.contains(nodeValue)) {
										entrySource.setClusterName("Level-1-Contacts");
									} else {
										entrySource.setClusterName("Level-" + col / 2 + "-Contacts");
									}
									listItems.add(entrySource);
									LOGGER.debug("found " + nodeValue);
								}
							}
						}
					}
				}
				model.append(listItems);
			}

			// If we have more than one page with indirect contacts
			if (itemAttr.equals("pagination")) {
				hasPageinator = false;
				final NodeList nodes2 = itemNode.getChildNodes();
				for (int f = 0; f < nodes2.getLength(); f++) {
					final Node currentChild = nodes2.item(f);
					NamedNodeMap attributes = currentChild.getAttributes();
					if (null != attributes) {
						Node namedItem = attributes.getNamedItem("class");
						if (null != namedItem && "next".equalsIgnoreCase(namedItem.getNodeValue())) {
							hasPageinator = true;
							break;
						}
					}
				}
			}
		}
		return hasPageinator;
	}
}
