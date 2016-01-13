package org.zgif.icred.plugin.zgif.read;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import eu.icred.model.datatype.Amount;
import eu.icred.model.datatype.Area;
import eu.icred.model.datatype.enumeration.AreaMeasurement;
import eu.icred.model.datatype.enumeration.AreaType;
import eu.icred.model.datatype.enumeration.Subset;
import eu.icred.model.node.AbstractNode;

public abstract class AbstractReader {
    private static Logger logger = Logger.getLogger(AbstractReader.class);
    
    protected abstract Object getObject();
    protected abstract Method getFieldSetter(AbstractNode node, String key);


    protected void setValue(AbstractNode node, String key, String value) {
        setValue(node, key, value, new HashMap<String, String>());
    }
    protected void setValue(AbstractNode node, String key, String value, Map<String, String> attr) {
        Method setter = getFieldSetter(node, key);

        Object setValue = null;
        Class<?> targetType = setter.getParameterTypes()[0];

        try {
            if (value.equals("")) {
                setValue = null;
            } else if (String.class.isAssignableFrom(targetType)) {
                setValue = value;
            } else if (Integer.class.isAssignableFrom(targetType)) {
                setValue = (int)Double.parseDouble(value);
            } else if (Double.class.isAssignableFrom(targetType)) {
                setValue = Double.parseDouble(value);
            } else if (BigDecimal.class.isAssignableFrom(targetType)) {
                setValue = BigDecimal.valueOf(Double.parseDouble(value));
            } else if (Currency.class.isAssignableFrom(targetType)) {
                setValue = Currency.getInstance(value);
            } else if (LocalDate.class.isAssignableFrom(targetType)) {
                setValue = LocalDate.parse(value);
            } else if (LocalDateTime.class.isAssignableFrom(targetType)) {
                setValue = LocalDateTime.parse(value);
            } else if (Period.class.isAssignableFrom(targetType)) {
                setValue = Period.parse(value);
            } else if (Locale.class.isAssignableFrom(targetType)) {
                setValue = Locale.GERMANY;
            } else if (Boolean.class.isAssignableFrom(targetType)) {
                setValue = Boolean.parseBoolean(value);
            } else if (XMLGregorianCalendar.class.isAssignableFrom(targetType)) {
                Date date = LocalDateTime.parse(value).toDate();
                GregorianCalendar c = new GregorianCalendar();
                c.setTime(date);
                setValue = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
            } else if (javax.xml.datatype.Duration.class.isAssignableFrom(targetType)) {
                setValue = DatatypeFactory.newInstance().newDuration(value);
            } else if (Amount.class.isAssignableFrom(targetType)) {
                setValue = new Amount(Double.parseDouble(value), Currency.getInstance(attr.get("currency")));
            } else if (Area.class.isAssignableFrom(targetType)) {
                setValue = new Area(Double.parseDouble(value), AreaMeasurement.valueOf(attr.get("areaMeasurement")), AreaType.valueOf(attr.get("areaType")));
            } else if (targetType.getPackage() == Subset.class.getPackage()) {
                // type is enumeration:
                try {
                    Method fromString = targetType.getDeclaredMethod("fromString", String.class);
                    setValue = fromString.invoke(null, value);
                } catch (Throwable t) {
                }

                if (setValue == null) {
                    try {
                        Method valueOf = targetType.getDeclaredMethod("valueOf", String.class);
                        setValue = valueOf.invoke(null, value);
                    } catch (Throwable t) {
                        Object x = Arrays.asList(targetType.getEnumConstants());
                        logger.warn("cannot set value '" + value + "' as " + targetType.getSimpleName() + " - allowed values: " + x);
                    }
                }
            } else {
                // unknown type
                setValue = null;
                logger.warn("cannot set value '" + value + "' - unknown data type: " + targetType + " - value will be set to empty");
            }

            setter.invoke(node, setValue);
        } catch (Throwable e) {
            throw new RuntimeException("could not set '" + key + "' (value=" + value + ")", e);
        }
    }
}
