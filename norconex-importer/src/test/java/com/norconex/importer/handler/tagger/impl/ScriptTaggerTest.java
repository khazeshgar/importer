/* Copyright 2015 Norconex Inc.
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
package com.norconex.importer.handler.tagger.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.importer.TestUtil;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;

public class ScriptTaggerTest {

    @Test
    public void testJavaScript() 
            throws IOException, ImporterHandlerException {

        String script =
                "metadata.addString('test', 'success');"
              + "var story = content.replace(/Alice/g, 'Roger');"
              + "metadata.addString('story', story);";
        
        ScriptTagger t = new ScriptTagger();
        t.setScript(script);

        File htmlFile = TestUtil.getAliceHtmlFile();
        FileInputStream is = new FileInputStream(htmlFile);

        ImporterMetadata metadata = new ImporterMetadata();
        metadata.setString(ImporterMetadata.DOC_CONTENT_TYPE, "text/html");
        t.tagDocument(htmlFile.getAbsolutePath(), is, metadata, false);

        is.close();

        String successField = metadata.getString("test");
        String storyField = metadata.getString("story");

        Assert.assertEquals("success", successField);
        Assert.assertEquals(0, StringUtils.countMatches(storyField, "Alice"));
        Assert.assertEquals(34, StringUtils.countMatches(storyField, "Roger"));
    }
    
    @Test
    public void testWriteRead() throws IOException {
        ScriptTagger tagger = new ScriptTagger();
        tagger.setScript("a script");
        tagger.setEngineName("an engine name");
        tagger.setMaxReadSize(256);
        System.out.println("Writing/Reading this: " + tagger);
        ConfigurationUtil.assertWriteRead(tagger);
    }

}
