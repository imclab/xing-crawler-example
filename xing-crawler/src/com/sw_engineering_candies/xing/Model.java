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
 */

package com.sw_engineering_candies.xing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Model {

	public class Item {

		private String clusterName;

		private String nodeName;

		public String getClusterName() {
			return clusterName;
		}

		public String getNodeName() {
			return nodeName;
		}

		public void setNodeName(final String nodeName) {
			assert nodeName != null : "Null is not allowed";
			assert !nodeName.isEmpty() : "Empty is not allowed";

			this.nodeName = nodeName;
		}

		public void setClusterName(final String clusterName) {
			assert clusterName != null : "Null is not allowed";
			assert !clusterName.isEmpty() : "Empty is not allowed";

			this.clusterName = clusterName;
		}
	}

	private static final Log LOGGER = LogFactory.getLog(Model.class);

	private static final String NL = System.getProperty("line.separator");

	private static final int NUMBER_OF_CLUSTERS = 10;
	private final Map<String, String> clusters = new HashMap<String, String>(NUMBER_OF_CLUSTERS);
	private final List<String> clusterIds = new Vector<String>(NUMBER_OF_CLUSTERS);

	private static final int NUMBER_OF_NODES = 500;
	private final Map<String, String> nodes = new HashMap<String, String>(NUMBER_OF_NODES);
	private final List<String> nodeIds = new Vector<String>(NUMBER_OF_NODES);

	private static final int NUMBER_OF_LINKS = 5000;
	private final Map<String, String> links = new HashMap<String, String>(NUMBER_OF_LINKS);
	private final List<String> linkIds = new Vector<String>(NUMBER_OF_LINKS);

	private int lastLength = 1000;

	private void add(final Item targetEntry, final Item sourceEntry) {

		// add target cluster
		final String targetClusterKey = targetEntry.getClusterName();
		if (!clusters.containsKey(targetClusterKey)) {
			clusterIds.add(targetClusterKey);
			final String clusterString = "\t{\"id\":" + clusterIds.indexOf(targetClusterKey)
					+ ", \"name\":\"" + targetClusterKey + "\" }";
			clusters.put(targetClusterKey, clusterString);
		}

		// add target node
		final String targetKey = targetEntry.getNodeName();
		if (!nodes.containsKey(targetKey)) {
			nodeIds.add(targetKey);
			final String nodeString = "\t{\"id\":" + nodeIds.indexOf(targetKey) + ", \"clusterId\":"
					+ clusterIds.indexOf(targetClusterKey) + ", \"name\":\"" + targetKey + "\"}";
			nodes.put(targetKey, nodeString);
		}

		// add source cluster
		final String sourceClusterKey = sourceEntry.getClusterName();
		if (!clusters.containsKey(sourceClusterKey)) {
			clusterIds.add(sourceClusterKey);
			final String clusterString = "\t{\"id\":" + clusterIds.indexOf(sourceClusterKey)
					+ ", \"name\":\"" + sourceClusterKey + "\" }";
			clusters.put(sourceClusterKey, clusterString);
		}

		// add source node
		final String sourceKey = sourceEntry.getNodeName();
		if (!nodes.containsKey(sourceKey)) {
			nodeIds.add(sourceKey);
			final String nodeString = "\t{\"id\":" + nodeIds.indexOf(sourceKey) + ", \"clusterId\":"
					+ clusterIds.indexOf(sourceClusterKey) + ", \"name\":\"" + sourceKey + "\" }";
			nodes.put(sourceKey, nodeString);
		}

		// add link
		final String keyLink = targetKey + "<-" + sourceKey;
		final String keyLinkBack = sourceKey + "<-" + targetKey;
		if (!links.containsKey(keyLink) && !links.containsKey(keyLinkBack)) {
			linkIds.add(keyLink);
			final String nodeString = "\t{\"id\":" + linkIds.indexOf(keyLink) + ", \"sourceId\":"
					+ nodeIds.indexOf(sourceKey) + ", \"targetId\":" + nodeIds.indexOf(targetKey) + "}";
			links.put(keyLink, nodeString);
		}
	}

	public synchronized void append(final List<Item> entryList) {
		final int maxIndex = entryList.size() - 1;
		if (maxIndex > 0) {
			for (int i = 0; i < maxIndex; i++) {
				add(entryList.get(i), entryList.get(i + 1));
			}
		}
	}

	public synchronized StringBuffer getJSON() {

		final StringBuffer fw = new StringBuffer(lastLength * 2);

		final List<String> nodeskeys = new Vector<String>();
		nodeskeys.addAll(nodes.keySet());

		fw.append("{" + NL + "\"clusters\":[");
		fw.append(NL);
		boolean isFrist = true;
		for (final String key : clusterIds) {
			fw.append((isFrist ? "" : "," + NL) + "");
			isFrist = false;
			fw.append(clusters.get(key));
		}
		fw.append(NL + "],");
		fw.append(NL);

		fw.append("\"nodes\":[");
		fw.append(NL);
		isFrist = true;
		for (final String key : nodeIds) {
			fw.append((isFrist ? "" : "," + NL) + "");
			isFrist = false;
			fw.append(nodes.get(key));
		}
		fw.append(NL + "],");
		fw.append(NL);

		fw.append("\"links\":[");
		fw.append(NL);
		isFrist = true;
		for (final String key : linkIds) {
			fw.append((isFrist ? "" : "," + NL) + "");
			isFrist = false;
			fw.append(links.get(key));
		}
		fw.append(NL + "]}");
		fw.append(NL);

		final StringBuffer message = new StringBuffer(200);
		message.append("web request for model #clusters=").append(clusterIds.size());
		message.append(", #nodes=").append(nodeIds.size());
		message.append(", #links=").append(links.size());
		LOGGER.info(message);
		lastLength = fw.length();
		return fw;
	}
}
