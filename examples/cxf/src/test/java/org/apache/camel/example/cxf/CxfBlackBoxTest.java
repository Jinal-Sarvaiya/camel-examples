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
package org.apache.camel.example.cxf;

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
 * Black-box test for the CXF WebServiceProvider example.
 * Sends a raw SOAP request over HTTP and validates the response,
 * with no dependency on generated WSDL stubs.
 */
class CxfBlackBoxTest {

    static AbstractApplicationContext context;
    static int port;

    @BeforeAll
    static void startServer() {
        port = AvailablePortFinder.getNextAvailable();
        System.setProperty("port", String.valueOf(port));
        context = new ClassPathXmlApplicationContext("/META-INF/spring/CamelCXFProviderRouteConfig.xml");
    }

    @AfterAll
    static void stopServer() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void sendSoapRequestAndVerifyResponse() throws Exception {
        String response = sendSoap(buildGreetMeRequest("Hello Camel!!"));

        assertTrue(response.contains("greetMeResponse"),
                "Response should contain greetMeResponse element");
        assertTrue(response.contains("Greetings from Apache Camel!!!!"),
                "Response should contain the greeting");
        assertTrue(response.contains("Hello Camel!!"),
                "Response should echo the request text");
    }

    @Test
    void sendDifferentMessageAndVerifyEcho() throws Exception {
        String response = sendSoap(buildGreetMeRequest("Testing 123"));

        assertTrue(response.contains("Testing 123"),
                "Response should echo the different request text");
    }

    @Test
    void verifyResponseStructure() throws Exception {
        String response = sendSoap(buildGreetMeRequest("StructureCheck"));

        assertTrue(response.contains("Envelope"), "Response should be a SOAP Envelope");
        assertTrue(response.contains("Body"), "Response should contain a SOAP Body");
        assertTrue(response.contains("responseType"),
                "Response should contain responseType element");
        assertTrue(response.contains("Greetings from Apache Camel!!!! Request was  StructureCheck"),
                "Response body should contain expected greeting with echoed request");
    }

    private static String buildGreetMeRequest(String message) {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:typ=\"http://apache.org/hello_world_soap_http/types\">"
                + "<soapenv:Body>"
                + "<typ:greetMe>"
                + "<typ:requestType>" + message + "</typ:requestType>"
                + "</typ:greetMe>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";
    }

    private String sendSoap(String soapXml) throws Exception {
        URL url = new URL("http://localhost:" + port + "/GreeterContext/SOAPMessageService");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        conn.setRequestProperty("SOAPAction", "\"\"");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(soapXml.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "HTTP response should be 200 OK");

        try (InputStream is = conn.getInputStream()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        } finally {
            conn.disconnect();
        }
    }
}
