package org.zgif.icred.plugin.zgif.read;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import eu.icred.model.annotation.Attribute;
import eu.icred.model.annotation.DataField;
import eu.icred.model.node.AbstractNode;
import eu.icred.model.node.Period;

public class PeriodsReader extends AbstractReader {
    private static Logger logger = Logger.getLogger(PeriodsReader.class);
    private Map<String, Period> periods;
    private Period curPeriod;

    private static Map<String, Method> fieldSetter;

    static {
        fieldSetter = new HashMap<String, Method>();

        for (Field field : Period.class.getDeclaredFields()) {
            if (field.getAnnotation(DataField.class) != null || field.getAnnotation(Attribute.class) != null) {
                String fieldName = field.getName();
                PropertyDescriptor pd;
                try {
                    pd = new PropertyDescriptor(fieldName, Period.class);
                    Method method = pd.getWriteMethod();

                    fieldSetter.put(fieldName, method);
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @author Pascal Houdek
     */
    public PeriodsReader(Map<String, Period> periods) {
        this.periods = periods;
    }

    public void read(InputStream inStream) throws XMLStreamException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader metaStream = factory.createXMLStreamReader(inStream, "UTF-8");

        while (metaStream.hasNext()) {
            metaStream.next();
            if (metaStream.isStartElement()) {
                String name = metaStream.getLocalName();
                if(name.equals("period")) {

                    Map<String, String> attributes = new HashMap<String, String>();
                    for (int i = 0; i < metaStream.getAttributeCount(); i++) {
                        attributes.put(metaStream.getAttributeLocalName(i), metaStream.getAttributeValue(i));
                    }
                    
                    String identifier = attributes.get("identifier");
                    if(!periods.containsKey(identifier)) {
                        periods.put(identifier, new Period());
                    }
                    
                    curPeriod = periods.get(identifier);
                    
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        setValue(curPeriod, entry.getKey(), entry.getValue());   
                    }
                    
                    curPeriod = null;
                }
            }
        }
    }
    
    @Override
    protected Object getObject() {
        return curPeriod;
    }

    @Override
    protected Method getFieldSetter(AbstractNode node, String key) {
        return fieldSetter.get(key);
    }
}
