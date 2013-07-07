/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex HTTP Collector.
 * 
 * Norconex HTTP Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex HTTP Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex HTTP Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.http.crawler;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class CrawlURL extends BaseURL {

    private static final long serialVersionUID = -2219206220476107409L;
    private CrawlStatus status;
    private String headChecksum;
    private String docChecksum;
    

    public CrawlURL(String url, int depth) {
        super(url, depth);
    }
    public CrawlStatus getStatus() {
        return status;
    }
    public void setStatus(CrawlStatus status) {
        this.status = status;
    }
    public String getHeadChecksum() {
        return headChecksum;
    }
    public void setHeadChecksum(String headChecksum) {
        this.headChecksum = headChecksum;
    }
    public String getDocChecksum() {
        return docChecksum;
    }
    public void setDocChecksum(String docChecksum) {
        this.docChecksum = docChecksum;
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(docChecksum)
            .append(headChecksum)
            .append(status)
            .toHashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CrawlURL)) {
            return false;
        }
        CrawlURL other = (CrawlURL) obj;
        return new EqualsBuilder()
            .appendSuper(super.equals(obj))
            .append(docChecksum, other.docChecksum)
            .append(headChecksum, other.headChecksum)
            .append(status, other.status)
            .isEquals();
    }
    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString())
                .append("status", status).append("headChecksum", headChecksum)
                .append("docChecksum", docChecksum).toString();
    }
}
