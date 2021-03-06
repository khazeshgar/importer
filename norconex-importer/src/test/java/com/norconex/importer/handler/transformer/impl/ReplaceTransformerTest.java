/* Copyright 2010-2016 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.handler.transformer.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.transformer.impl.ReplaceTransformer;

public class ReplaceTransformerTest {

    private final String restrictionTestConfig = "<transformer>"
            + "<replace><fromValue>CAKES</fromValue>"
            + "<toValue>FRUITS</toValue></replace>"
            + "<replace><fromValue>candies</fromValue>"
            + "<toValue>vegetables</toValue></replace>"
            + "<restrictTo caseSensitive=\"false\" "
            + "field=\"document.reference\">.*test.*</restrictTo>"
            + "</transformer>";
    private final String restrictionTestContent = 
            "I like to eat cakes and candies.";


    @Test
    public void testReplaceEolWithWhiteSpace() 
            throws ImporterHandlerException, IOException {
        String input = "line1\r\nline2\rline3\nline4";
        String expectedOutput = "line1 line2 line3 line4";

        String preserveTestConfig = "<transformer>"
                + "<replace><fromValue>[\\r\\n]+</fromValue>"
                + "<toValue xml:space=\"preserve\"> </toValue></replace>"
                + "</transformer>";
        String response1 = transformTextDocument(
                preserveTestConfig, "N/A", input);
        Assert.assertEquals(expectedOutput, response1);
    }

    @Test
    public void testTransformRestrictedTextDocument() 
            throws IOException, ImporterHandlerException {
        String response = transformTextDocument(
                restrictionTestConfig, "rejectme.html", restrictionTestContent);
        Assert.assertEquals(StringUtils.EMPTY, response.toLowerCase());
    }

    @Test
    public void testTransformUnrestrictedTextDocument() 
            throws IOException, ImporterHandlerException {
        String response = transformTextDocument(
                restrictionTestConfig, "test.html", restrictionTestContent);
        Assert.assertEquals(
                "i like to eat fruits and vegetables.", 
                response.toLowerCase());
    }
    
    private String transformTextDocument(
            String config, String reference, String content)
            throws IOException, ImporterHandlerException {
        
        ReplaceTransformer t = new ReplaceTransformer();

        Reader reader = new InputStreamReader(
                IOUtils.toInputStream(config, CharEncoding.UTF_8));
        t.loadFromXML(reader);
        reader.close();
        
        InputStream is = IOUtils.toInputStream(content, CharEncoding.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        
        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString("document.reference", reference);

        t.transformDocument(reference, is, os, metadata, true);
        
        String response = os.toString();
        is.close();
        os.close();
        return response;
    }
    
    
    @Test
    public void testWriteRead() throws IOException {
        ReplaceTransformer t = new ReplaceTransformer();
        t.setMaxReadSize(128);
        Reader reader = new InputStreamReader(IOUtils.toInputStream(
                restrictionTestConfig, CharEncoding.UTF_8));
        t.loadFromXML(reader);
        reader.close();
        System.out.println("Writing/Reading this: " + t);
        ConfigurationUtil.assertWriteRead(t);
    }

}
