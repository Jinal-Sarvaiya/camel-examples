/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.example.cxf.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.camel.test.AvailablePortFinder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Black-box test for the CXF JAX-RS example.
 * Uses raw HTTP to POST/GET books — no CXF client stubs, no javax/jakarta imports.
 * Designed to pass both before and after Camel 3 → 4 migration.
 */
class JaxrsBlackBoxTest {

    static AbstractApplicationContext context;
    static int restPort;

    @BeforeAll
    static void startServer() {
        int soapPort = AvailablePortFinder.getNextAvailable();
        restPort = AvailablePortFinder.getNextAvailable();
        System.setProperty("soapEndpointPort", String.valueOf(soapPort));
        System.setProperty("restEndpointPort", String.valueOf(restPort));
        context = new ClassPathXmlApplicationContext("/META-INF/spring/JAXRSCamelContext.xml");
    }

    @AfterAll
    static void stopServer() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void addBookAndRetrieveIt() throws Exception {
        String bookXml = "<Book><id>200</id><name>Camel in Action</name></Book>";
        String addResponse = httpPost(
                "http://localhost:" + restPort + "/rest/bookstore/books",
                "application/xml", bookXml);

        assertTrue(addResponse.contains("Camel in Action"),
                "Add response should contain the book name");
        assertTrue(addResponse.contains("200"),
                "Add response should contain the book id");

        String getResponse = httpGet(
                "http://localhost:" + restPort + "/rest/bookstore/200",
                "application/xml");

        assertTrue(getResponse.contains("Camel in Action"),
                "GET response should contain the book name");
        assertTrue(getResponse.contains("200"),
                "GET response should contain the book id");
    }

    @Test
    void getPreloadedBook() throws Exception {
        String response = httpGet(
                "http://localhost:" + restPort + "/rest/bookstore/101",
                "application/xml");

        assertTrue(response.contains("CXF User Guide"),
                "Pre-loaded book 101 should be 'CXF User Guide'");
    }

    @Test
    void getNonExistentBookReturns404() throws Exception {
        URL url = new URL("http://localhost:" + restPort + "/rest/bookstore/999");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/xml");
        try {
            int code = conn.getResponseCode();
            assertEquals(404, code, "Non-existent book should return 404");
        } finally {
            conn.disconnect();
        }
    }

    private static String httpGet(String urlStr, String accept) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", accept);
        try {
            assertEquals(200, conn.getResponseCode(), "GET should return 200");
            return readBody(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static String httpPost(String urlStr, String contentType, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", contentType);
        conn.setRequestProperty("Accept", contentType);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        try {
            assertEquals(200, conn.getResponseCode(), "POST should return 200");
            return readBody(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static String readBody(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }
}
