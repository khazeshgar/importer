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
package com.norconex.importer.handler.filter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.handler.filter.AbstractDocumentFilter;
import com.norconex.importer.handler.filter.OnMatch;
import com.norconex.importer.util.FormatUtil;
/**
 * <p>Accepts or rejects a document based on the date value(s) of a metadata
 * field, stored in a specified format. If multiple values are found for a 
 * field, only one of them needs to match for this filter to take effect. 
 * If the value cannot be parsed to a valid date, it is considered not to be 
 * matching. 
 * </p>
 * 
 * <h3>Metadata date field format:</h3>
 * <p>To successfully parse a date, an optional date format can be specified,
 * as per the formatting options found on {@link SimpleDateFormat}. 
 * The default format when not specified is EPOCH (the difference, measured in
 * milliseconds, between the date and midnight, January 1, 1970).</p>
 * 
 * <h3>Dynamic vs static dates:</h3>
 * <p>When adding a condition, you can specify a static date (i.e. a constant
 * date value), or you can tell this filter you want to use a date
 * relative to the current type. There is a distinction to be made between
 * TODAY and NOW.  TODAY is the current day without the hours, minutes, and 
 * seconds, where as NOW is the current day with the hours, minutes, and 
 * seconds. You can also decide whether you want the current date to be fixed
 * (does not change after being set for the first time), or whether
 * it should be refreshed on every call to reflect system date time changes.
 * </p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.DateMetadataFilter"
 *          onMatch="[include|exclude]" 
 *          field="(name of metadata field to match)"
 *          format="(date format)" &gt;
 *          
 *      &lt;!-- Use one or two (for ranges) conditions where:
 *           
 *           Possible operators are: 
 *
 *               gt -&gt; greater than
 *               ge -&gt; greater equal
 *               lt -&gt; lower than
 *               le -&gt; lowe equal
 *               eq -&gt; equals
 *               
 *           Condition date value format are either one of:
 *           
 *               yyyy-MM-dd            -&gt; date (e.g. 2015-05-31) 
 *               yyyy-MM-ddThh:mm:ss[.SSS]   -&gt; date and time with optional 
 *                                              milliseconds (e.g. 2015-05-31T22:44:15)
 *               TODAY[-+]9[YMDhms][*] -&gt; the string "TODAY" (at 0:00:00) minus
 *                                        or plus a number of years, months, days, 
 *                                        hours, minutes, or seconds
 *                                        (e.g. 1 week ago: TODAY-7d).
 *                                        * means TODAY can change from one invocation
 *                                        to another to adjust to a change of current day
 *               NOW[-+]9[YMDhms][*]  -&gt; the string "NOW" (at current time) minus
 *                                       or plus a number of years, months, days,
 *                                       hours, minutes, or seconds
 *                                       (e.g. 1 week ago: NOW-7d).
 *                                       * means NOW changes from one invocation
 *                                       to another to adjust to the current time.
 *               
 *        --&gt;
 *
 *      &lt;condition operator="[gt|ge|lt|le|eq]" date="(a date)" /&gt;
 *      
 *  &lt;/filter&gt;
 * </pre>
 * <p>For example, let's say you want to keep only documents from the last
 *    seven days, not including today. The following would achieve that:</p>
 * <pre>
 *  &lt;filter class="com.norconex.importer.handler.filter.impl.DateMetadataFilter"
 *          onMatch="include" field="publish_date" &gt;
 *      &lt;condition operator="ge" date="TODAY-7" /&gt;
 *      &lt;condition operator="lt" date="TODAY" /&gt;
 *  &lt;/filter&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 2.2.0
 */
public class DateMetadataFilter extends AbstractDocumentFilter {

    private static final Logger LOG = 
            LogManager.getLogger(DateMetadataFilter.class);
    
    public enum Operator {
        GREATER_THAN("gt") {@Override
        public boolean evaluate(long fieldDate, long conditionDate) {
            return fieldDate > conditionDate;
        }},
        GREATER_EQUAL("ge") {@Override
        public boolean evaluate(long fieldDate, long conditionDate) {
            return fieldDate >= conditionDate;
        }},
        EQUALS("eq") {@Override
        public boolean evaluate(long fieldDate, long conditionDate) {
            return fieldDate == conditionDate;
        }},
        LOWER_EQUAL("le") {@Override
        public boolean evaluate(long fieldDate, long conditionDate) {
            return fieldDate <= conditionDate;
        }},
        LOWER_THAN("lt") {@Override
        public boolean evaluate(long fieldDate, long conditionDate) {
            return fieldDate < conditionDate;
        }};
        String abbr;
        private Operator(String abbr) {
            this.abbr = abbr;
        }
        public static Operator getOperator(String op) {
            if (StringUtils.isBlank(op)) {
                return null;
            }
            for (Operator c : Operator.values()) {
                if (c.abbr.equalsIgnoreCase(op)) {
                    return c;
                }
            }
            return null;
        }
        @Override
        public String toString() {
            return abbr;
        }
        public abstract boolean evaluate(
                long fieldDate, long conditionDate);
    }
    
    public enum TimeUnit {
        YEAR(Calendar.YEAR, "Y"), 
        MONTH(Calendar.MONTH, "M"),
        DAY(Calendar.DAY_OF_MONTH, "D"),
        HOUR(Calendar.HOUR, "h"),
        MINUTE(Calendar.MINUTE, "m"), 
        SECOND(Calendar.SECOND, "s");
        private final int field;
        private final String abbr;
        private TimeUnit(int field, String abbr) {
            this.field = field;
            this.abbr = abbr;
        }
        public int getField() {
            return field;
        }
        @Override
        public String toString() {
            return abbr;
        }
        public static TimeUnit getTimeUnit(String unit) {
            if (StringUtils.isBlank(unit)) {
                return null;
            }
            for (TimeUnit tu : TimeUnit.values()) {
                if (tu.abbr.equalsIgnoreCase(unit)) {
                    return tu;
                }
            }
            return null;
        }
    }
    
    private String field;
    private String format;
    private final List<Condition> conditions = new ArrayList<>(2);

    public DateMetadataFilter() {
        this(null);
    }
    public DateMetadataFilter(String field) {
        this(field, OnMatch.INCLUDE);
    }
    public DateMetadataFilter(String field, OnMatch onMatch) {
        super();
        this.field = field;
        setOnMatch(onMatch);
    }
    
    public String getField() {
        return field;
    }
    public void setField(String property) {
        this.field = property;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    public void addCondition(Operator operator, Date date) {
        conditions.add(new Condition(operator, date.getTime()));
    }
    public void addConditionFromNow(
            Operator operator, TimeUnit timeUnit, int value, boolean fixed) {
        conditions.add(new Condition(operator, timeUnit, value, fixed, false));
    }
    public void addConditionFromToday(
            Operator operator, TimeUnit timeUnit, int value, boolean fixed) {
        conditions.add(new Condition(operator, timeUnit, value, fixed, true));
    }
    
    @Override
    protected boolean isDocumentMatched(String reference, InputStream input,
            ImporterMetadata metadata, boolean parsed)
            throws ImporterHandlerException {

        if (StringUtils.isBlank(field)) {
            throw new IllegalArgumentException("\"field\" cannot be empty.");
        }
        Collection<String> values =  metadata.getStrings(field);
        for (String value : values) {
            if (meetsAllConditions(value)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean meetsAllConditions(String fieldValue) {
        String epochString = 
                FormatUtil.formatDateString(fieldValue, format, null, field);
        if (StringUtils.isBlank(epochString)) {
            return false;
        }
        long fieldEpoch = Long.parseLong(epochString);
        for (Condition condition : conditions) {
            if (!condition.operator.evaluate(
                    fieldEpoch, condition.getEpochDate())) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected void loadFilterFromXML(XMLConfiguration xml) throws IOException {
        setField(xml.getString("[@field]", getField()));
        setFormat(xml.getString("[@format]", getFormat()));
        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("condition");
        for (HierarchicalConfiguration node : nodes) {
            String op = node.getString("[@operator]", null);
            String date = node.getString("[@date]", null);
            if (StringUtils.isBlank(op) || StringUtils.isBlank(date)) {
                LOG.warn("Both \"operator\" and \"date\" must be provided.");
                break;
            }
            Operator operator = Operator.getOperator(op);
            if (operator == null) {
                LOG.warn("Unsupported operator: " + op);
                break;
            }
            Condition condition = Condition.parse(operator, date);
            if (condition == null) {
                LOG.debug("Not a valid date value: " + date);
                break;
            }
            conditions.add(condition);
        }
    }
    
    @Override
    protected void saveFilterToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute("field", field);
        writer.writeAttribute("format", format);
        for (Condition condition : conditions) {
            writer.writeStartElement("condition");
            writer.writeAttributeString("operator", condition.operator.abbr);
            writer.writeAttributeString("date", condition.getDateString());
            writer.writeEndElement();
        }        
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof DateMetadataFilter)) {
            return false;
        }
        DateMetadataFilter castOther = (DateMetadataFilter) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(conditions, castOther.conditions)
                .append(field, castOther.field)
                .append(format, castOther.format)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .appendSuper(super.hashCode())
                .append(conditions)
                .append(field)
                .append(format)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("conditions", conditions)
                .append("field", field)
                .append("format", format)
                .toString();
    }

    public static class Condition {
        private enum Type { STATIC, DYN_FIXED, DYN_FLOAT }
        private final Operator operator;
        private final Type type;
        
        // static date fields
        private final long epochDate;

        // dynamic date fields
        private final TimeUnit unit;
        private final int amount;
        private final boolean today; // default is false == NOW

        
        // static date constructor
        public Condition(Operator operator, long epochDate) {
            super();
            this.operator = operator;
            this.epochDate = epochDate;
            this.type = Type.STATIC;
            this.unit = null;
            this.amount = -1;
            this.today = false;
        }
        // dynamic date constructor
        public Condition(
                Operator operator, 
                TimeUnit unit, 
                int amount, 
                boolean fixed,
                boolean today) {
            super();
            this.operator = operator;
            this.unit = unit;
            this.amount = amount;
            this.today = today;
            if (fixed) {
                this.type = Type.DYN_FIXED;
                Calendar cal = Calendar.getInstance();
                if (today) {
                    cal = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH);
                }
                cal.add(unit.field, amount);
                this.epochDate = cal.getTimeInMillis();
            } else {
                this.type = Type.DYN_FLOAT;
                this.epochDate = -1;
            }
        }
        
        public String getDateString() {
            if (type == Type.STATIC) {
                return FastDateFormat.getInstance(
                        "yyyy-MM-dd'T'HH:mm:ss.SSS").format(epochDate);
            }
            StringBuilder b = new StringBuilder();
            if (today) {
                b.append("TODAY");
            } else {
                b.append("NOW");
            }
            if (amount >= 0) {
                b.append('+');
            }
            b.append(amount);
            b.append(unit.toString());
            if (type == Type.DYN_FLOAT) {
                b.append('*');
            }
            return b.toString();
        }
        
        public long getEpochDate() {
            if (type == Type.STATIC || type == Type.DYN_FIXED) {
                return epochDate;
            }
            // has to be DYN_FLOAT at this point
            Calendar cal = Calendar.getInstance();
            if (today) {
                cal = DateUtils.truncate(cal, Calendar.DAY_OF_MONTH);
            }
            cal.add(unit.field, amount);
            return cal.getTimeInMillis();
        }
        
        private static final Pattern RELATIVE_PARTS = Pattern.compile(
                "^(\\w{3,5})([-+]{1})(\\d+)([YMDhms]{1})(\\*{0,1})$");
        public static Condition parse(Operator operator, String dateString) {
            try {
                String d = dateString.trim();

                // NOW[-+]9[YMDhms][*]
                // TODAY[-+]9[YMDhms][*]
                if (d.startsWith("NOW") || d.startsWith("TODAY")) {
                    Matcher m = RELATIVE_PARTS.matcher(d);
                    if (!m.matches() || m.groupCount() != 5) {
                        LOG.debug("Invalid format for value: " + dateString);
                        return null;
                    }
                    int amount = NumberUtils.toInt(m.group(3));
                    if  ("-".equals(m.group(2))) {
                        amount = -amount;
                    }
                    TimeUnit unit = TimeUnit.getTimeUnit(m.group(4));
                    if (unit == null) {
                        LOG.debug("Invalid time unit: " + m.group(4));
                    }
                    boolean fixed = !"*".equals(m.group(5));
                    boolean today = "TODAY".equals(m.group(1));
                    return new Condition(operator, unit, amount, fixed, today);
                }

                // yyyy-MM-ddThh:mm:ss.SSS
                if (d.contains(".")) {
                    return new Condition(operator, FastDateFormat.getInstance(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS").parse(d).getTime());
                }
                // yyyy-MM-ddThh:mm:ss
                if (d.contains("T")) {
                    return new Condition(operator, 
                            DateFormatUtils.ISO_DATETIME_FORMAT.parse(
                                    d).getTime());
                }

                // yyyy-MM-dd
                return new Condition(operator, 
                        DateFormatUtils.ISO_DATE_FORMAT.parse(d).getTime());
            } catch (ParseException e) {
                LOG.debug("Date parse error for value: " + dateString, e);
            }
            return null;
        }

        
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof Condition)) {
                return false;
            }
            Condition castOther = (Condition) other;
            return new EqualsBuilder()
                    .append(operator, castOther.operator)
                    .append(epochDate, castOther.epochDate)
                    .append(type, castOther.type)
                    .append(unit, castOther.unit)
                    .append(amount, castOther.amount)
                    .append(today, castOther.today)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(operator)
                    .append(epochDate)
                    .append(type)
                    .append(unit)
                    .append(amount)
                    .append(today)
                    .toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                    .append("operator", operator)
                    .append("epochDate", epochDate)
                    .append("type", type)
                    .append("unit", unit)
                    .append("amount", amount)
                    .append("today", today)
                    .toString();
        }
    }
}

