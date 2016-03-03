package org.zgif.icred.plugin.zgif.read;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;

import eu.icred.model.NodeInformation;
import eu.icred.model.node.AbstractNode;
import eu.icred.model.node.Data;
import eu.icred.model.node.ExtensionMap;
import eu.icred.model.node.Period;
import eu.icred.model.node.entity.Property;
import eu.icred.model.node.group.Address;
import eu.icred.model.node.group.EnergyRating;

public class DataReader extends AbstractReader {
    private static Logger logger = Logger.getLogger(DataReader.class);
    private Data data;
    private AbstractNode curNode;
    private Stack<AbstractNode> nodeStack = new Stack<AbstractNode>();

    public DataReader(Data data) {
        this.data = data;

        init();
    }

    /*
     * protected Map<String, Company> companies;
     * 
     * @ChildList protected Map<String, Property> properties;
     * 
     * @ChildList protected Map<String, Account> accounts;
     * 
     * public Map<String, Company> getCompanies() { return getCompanies(true); }
     */
    public DataReader(Period period) {
        this.data = period.getData();
        if (data == null) {
            data = new Data();
            period.setData(data);
        }

        init();
    }

    private void glueNodes(AbstractNode parent, AbstractNode child) throws Exception {
        Class<AbstractNode> parentClass = (Class<AbstractNode>) parent.getClass();
        Class<AbstractNode> childClass = (Class<AbstractNode>) child.getClass();
        try {
            for (Field field : parentClass.getDeclaredFields()) {
                if (field.getGenericType().toString().contains(childClass.getSimpleName() + ">")) {
                    PropertyDescriptor pd = new PropertyDescriptor(field.getName(), parentClass);
                    Method getter = pd.getReadMethod();

                    Map<String, AbstractNode> map = (Map<String, AbstractNode>) getter.invoke(parent);
                    if (map == null) {
                        map = new HashMap<String, AbstractNode>();
                        Method setter = pd.getWriteMethod();
                        setter.invoke(parent, map);
                    }

                    NodeInformation info = new NodeInformation(childClass);
                    Field identifierField = info.getIdentifierField();
                    pd = new PropertyDescriptor(identifierField.getName(), childClass);
                    Method getIdentifier = pd.getReadMethod();
                    // TODO glue leased unit correct via hash. key is missing atm
                    String objectId = (String) getIdentifier.invoke(child);

                    map.put(objectId, child);
                }
            }
        } catch (Exception e) {
            throw new Exception("parent: " + parent + " - child: " + child, e);
        }
    }

    private void init() {
        curNode = data;
        nodeStack.add(data);
    }

    private void readExtension(XMLStreamReader dataStream) throws XMLStreamException {
        ExtensionMap extension = curNode.getExtensionMap();
        while (dataStream.hasNext()) {
            dataStream.next();
            if (dataStream.isStartElement()) {
                String name = dataStream.getLocalName();

                if (name.endsWith("key")) {
                    String keyName = dataStream.getAttributeValue(0);
                    String value = dataStream.getElementText();

                    extension.setValue(keyName, value);
                }

            }

            if (dataStream.isEndElement() && dataStream.getLocalName().equals("extension")) {
                return;
            }
        }
    }

    public void read(InputStream inStream) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader dataStream = factory.createXMLStreamReader(inStream, "UTF-8");

        while (dataStream.hasNext()) {
            dataStream.next();
            if (dataStream.isStartElement()) {
                String name = dataStream.getLocalName();

                if (isEntityTag(name)) {
                    AbstractNode newNode = getNodeByName(name);
                    logger.debug("new node: " + name);
                    for (int i = 0; i < dataStream.getAttributeCount(); i++) {
                        String attrName = null;
                        try {
                            attrName = dataStream.getAttributeLocalName(i);
                            String attrValue = dataStream.getAttributeValue(i);

                            setValue(newNode, attrName, attrValue);
                        } catch (Throwable t) {
                            throw new Exception("error writing attribute '" + attrName + "'", t);
                        }
                    }

                    glueNodes(curNode, newNode);

                    nodeStack.push(curNode);
                    curNode = newNode;
                } else if (name.equals("extension")) {
                    readExtension(dataStream);
                } else if (name.equals("data")) {

                } else if (name.equals("address")) {

                } else if (!name.startsWith("LIST_OF_")) {
                    try {
                        Map<String, String> attr = new HashMap<String, String>();
                        for (int i = 0; i < dataStream.getAttributeCount(); i++) {
                            String attrName = dataStream.getAttributeLocalName(i);
                            String attrValue = dataStream.getAttributeValue(i);
                            attr.put(attrName, attrValue);
                        }

                        String value = dataStream.getElementText();
                        logger.debug("setValue(" + curNode + ", " + name + ", " + value + ", " + attr + ")");
                        setValue(curNode, name, value, attr);
                    } catch (Throwable t) {
                        logger.error("error writing datafield '" + name + "'", t);
                    }
                }
            }

            if (dataStream.isEndElement()) {
                String name = dataStream.getLocalName();

                if (isEntityTag(name)) {
                    curNode = nodeStack.pop();
                }
            }
        }
    }

    @Override
    protected Object getObject() {
        return data;
    }

    @Override
    protected Method getFieldSetter(AbstractNode node, String key) {
        PropertyDescriptor pd = null;
        try {
            pd = new PropertyDescriptor(key, node.getClass());
        } catch (IntrospectionException e) {
        }
        return (pd == null) ? null : pd.getWriteMethod();
    }

    private boolean isEntityTag(String tagName) {
        return tagName.matches("[A-Z_]+") && !tagName.startsWith("LIST_OF_");
    }

    private AbstractNode getNodeByName(String tagName) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        AbstractNode node = null;

        String simpleClassName = upperCase2CamelCase(tagName);

        Class<?>[] specialClasses = { Address.class, EnergyRating.class };
        for (int i = 0; i < specialClasses.length; i++) {
            Class<AbstractNode> specialClass = (Class<AbstractNode>) specialClasses[i];
            if (specialClass.getSimpleName().equals(simpleClassName)) {
                node = specialClass.newInstance();
                break;
            }
        }

        if (node == null) {
            Package defaultPackage = Property.class.getPackage();
            String className = defaultPackage.getName() + "." + simpleClassName;
            Class<AbstractNode> clazz = (Class<AbstractNode>) Class.forName(className);
            node = clazz.newInstance();
        }

        return node;
    }

    private static String upperCase2CamelCase(String upperCase) {
        StringBuilder sb = new StringBuilder(upperCase.length());

        boolean lastWasUnderscore = upperCase.contains(".");
        for (int i = 0; i < upperCase.length(); i++) {
            char curChar = upperCase.charAt(i);

            if (curChar == '_') {
                lastWasUnderscore = true;
            } else if (lastWasUnderscore) {
                sb.append(curChar);
                lastWasUnderscore = false;
            } else if (i == 0) {
                sb.append(curChar);
            } else {
                sb.append(Character.toLowerCase(curChar));
            }
        }
        return sb.toString();
    }
}
