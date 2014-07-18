/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;

import com.norconex.commons.lang.io.CachedInputStream;
import com.norconex.commons.lang.unit.DataUnit;

/**
 * This class is not thread-safe.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class Content {
    //TODO make this a unique representation of content, which can be 
    // a file, an inputstream and maybe others.
    // Provide utility methods too.
    
    public static final int DEFAULT_MAX_MEMORY_CACHE_SIZE = 
            (int) DataUnit.MB.toBytes(1);
    
    private CachedInputStream cacheStream;
    
    public Content(File file) throws FileNotFoundException {
        this(new FileInputStream(file), DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(File file, int maxMemoryCacheSize) 
            throws FileNotFoundException {
        this(new FileInputStream(file), maxMemoryCacheSize);
    }
    public Content(InputStream is) {
        this(is, DEFAULT_MAX_MEMORY_CACHE_SIZE);
    }
    public Content(InputStream is, int maxMemoryCacheSize) {
        super();
        if (is == null) {
            cacheStream = new CachedInputStream(new NullInputStream(0), 0);
        } else {
            cacheStream = new CachedInputStream(is, maxMemoryCacheSize);
        }
    }
    public Content(CachedInputStream is) {
        cacheStream = is;
    }
    
    /**
     * Gets the content input stream. 
     * This method is not thread-safe.   
     * @return input stream
     */
    public CachedInputStream getInputStream() {
        cacheStream.rewind();
        return cacheStream;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (cacheStream != null) {
            IOUtils.closeQuietly(cacheStream);
        }
    }
}
