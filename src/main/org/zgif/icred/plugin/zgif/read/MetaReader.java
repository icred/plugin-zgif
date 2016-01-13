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
import eu.icred.model.node.Meta;

public class MetaReader extends AbstractReader {
    private static Logger logger = Logger.getLogger(MetaReader.class);
    private Meta meta;

    private static Map<String, Method> fieldSetter;

    static {
        fieldSetter = new HashMap<String, Method>();

        for (Field field : Meta.class.getDeclaredFields()) {
            if (field.getAnnotation(DataField.class) != null) {
                String fieldName = field.getName();
                PropertyDescriptor pd;
                try {
                    pd = new PropertyDescriptor(fieldName, Meta.class);
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
    public MetaReader(Meta meta) {
        this.meta = meta;
    }

    public void read(InputStream inStream) throws XMLStreamException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader metaStream = factory.createXMLStreamReader(inStream, "UTF-8");

        while (metaStream.hasNext()) {
            metaStream.next();
            if (metaStream.isStartElement()) {
                String name = metaStream.getLocalName();

                if (fieldSetter.containsKey(name)) {
                    setValue(meta, name, metaStream.getElementText());
                }
            }

        }
    }

    @Override
    protected Object getObject() {
        return meta;
    }

    @Override
    protected Method getFieldSetter(AbstractNode node, String key) {
        return fieldSetter.get(key);
    }
}
