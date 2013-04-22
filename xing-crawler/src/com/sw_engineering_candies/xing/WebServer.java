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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WebServer extends Thread {

	private static final Log LOGGER = LogFactory.getLog(WebServer.class);

	private static final String NL = System.getProperty("line.separator");

	private final int port;

	private final Model model;

	public WebServer(final int port, final Model model) {
		assert model != null : "Null is not allowed";
		assert port > 0 && port < 65535 : "port is not in valid range [1..65535[";

		this.port = port;
		this.model = model;
	}

	@Override
	public void run() {
		Socket connection = null;
		while (true) {
			try {
				final ServerSocket server = new ServerSocket(port);
				connection = server.accept();
				final OutputStream out = new BufferedOutputStream(connection.getOutputStream());
				final InputStream in = new BufferedInputStream(connection.getInputStream());
				final String request = readFirstLineOfRequest(in).toString();
				LOGGER.debug("get request " + request.toString());

				if (request.startsWith("GET /index.html")) {

					// Create content of response
					final String contentText = getPage().toString();
					final byte[] content = contentText.getBytes();

					// For HTTP/1.0 or later send a MIME header
					if (request.indexOf("HTTP/") != -1) {
						final String headerString = "HTTP/1.0 200 OK" + NL
								+ "Server: YacaAgent 1.0" + NL + "Content-length: "
								+ content.length + NL + "Content-type: text/html" + NL + NL;
						final byte[] header = headerString.getBytes("ASCII");
						out.write(header);
					}
					out.write(content);
					out.flush();
				} else if (request.startsWith("GET /terminate")) {
					server.close();
					throw new RuntimeException("terminate");
				}
				// Close the socket
				connection.close();
				in.close();
				out.close();
				server.close();
			} catch (final IOException e) {
				LOGGER.error(e.getMessage());
				System.exit(-1);
			}
		}
	}

	private StringBuffer readFirstLineOfRequest(final InputStream in) throws IOException {
		final StringBuffer request;
		request = new StringBuffer(100);
		while (true) {
			final int character = in.read();
			if (character == '\n' || character == '\r' || character == -1) {
				break;
			}
			request.append((char) character);
		}
		return request;
	}

	public StringBuffer getPage() {
		final StringBuffer fw = new StringBuffer(10000);
		try {
			final InputStream inputstream = this.getClass().getClassLoader()
					.getResourceAsStream("com/sw_engineering_candies/xing/XingPageResultTemplate.html");
			final InputStreamReader is = new InputStreamReader(inputstream);
			final BufferedReader br = new BufferedReader(is);
			for (String s = br.readLine(); s != null; s = br.readLine()) {
				if (s.contains("XX_MODEL_PLACE_HOLDER")) {
					fw.append(model.getJSON());
				} else {
					fw.append(s).append('\n');
				}
			}
			inputstream.close();
		} catch (final Exception xc) {
			xc.printStackTrace();
		}
		return fw;
	}

}
