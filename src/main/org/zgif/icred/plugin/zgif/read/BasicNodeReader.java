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

import eu.icred.model.annotation.DataField;
import eu.icred.model.node.AbstractNode;
import eu.icred.model.node.Data;

public class BasicNodeReader extends AbstractReader {
    private static Logger logger = Logger.getLogger(BasicNodeReader.class);
    private AbstractNode node;

    private static Map<String, Method> fieldSetter;

    static {
        fieldSetter = new HashMap<String, Method>();

        for (Field field : Data.class.getDeclaredFields()) {
            if (field.getAnnotation(DataField.class) != null) {
                String fieldName = field.getName();
                PropertyDescriptor pd;
                try {
                    pd = new PropertyDescriptor(fieldName, Data.class);
                    Method method = pd.getWriteMethod();

                    fieldSetter.put(fieldName, method);
                } catch (IntrospectionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public BasicNodeReader(AbstractNode node) {
        this.node = node;
    }

    public void read(InputStream inStream) throws XMLStreamException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader metaStream = factory.createXMLStreamReader(inStream, "UTF-8");

        while (metaStream.hasNext()) {
            metaStream.next();
            if (metaStream.isStartElement()) {
                String name = metaStream.getLocalName();

                if (fieldSetter.containsKey(name)) {
                    setValue(null, name, metaStream.getElementText());
                }
            }

        }
    }

    @Override
    protected Object getObject() {
//        return data;
        return null;
    }

    @Override
    protected Method getFieldSetter(AbstractNode node, String key) {
        return fieldSetter.get(key);
    }
}
