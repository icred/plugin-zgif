/**
 * 
 */
package org.zgif.icred.plugin.zgif.write;

import java.beans.PropertyDescriptor;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import eu.icred.model.NodeInformation;
import eu.icred.model.datatype.Amount;
import eu.icred.model.datatype.Area;
import eu.icred.model.node.AbstractNode;
import eu.icred.model.node.Data;
import eu.icred.model.node.ExtensionMap;
import eu.icred.model.node.entity.LeasedUnit;
import eu.icred.model.node.group.AbstractGroupNode;
import eu.icred.model.node.group.Address;
import eu.icred.model.node.group.EnergyRating;

/**
 * @author Pascal Houdek
 * 
 */
public class BasicNodeWriter {
    protected enum WriteRules {
        WRITE_ALWAYS, NO_WRITE_IF_NULL, NO_WRITE_IF_NULl_OR_EMPTY
    }

    private Boolean writeUnhashedUnit = false;

    private static Logger logger = Logger.getLogger(BasicNodeWriter.class);
    private OutputStream outStream;
    private XMLStreamWriter writer;

    public BasicNodeWriter(OutputStream outStream) throws XMLStreamException {
        this.outStream = outStream;
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        this.writer = factory.createXMLStreamWriter(this.getOutStream(), "UTF-8");
    }

    protected void writeNode(AbstractNode node) throws Exception {
        writer.writeStartElement(getTagnameOfNode(node));

        // if (!(node instanceof Unit) || writeUnhashedUnit) {
        NodeInformation info = new NodeInformation(node.getClass());
        writeAttributes(node, info);
        writeDataFields(node, info);
        writeGroupNodes(node, info);
        writeExtensionMap(node, info);

        // if (node instanceof Property) {
        // Property property = (Property) node;
        //
        // List<Unit> units = new ArrayList<Unit>();
        // Map<String, Building> buildings = property.getBuildings();
        // if (buildings != null) {
        // for (Building building : buildings.values()) {
        // Map<String, Unit> buildingUnits = building.getUnits();
        // if (buildingUnits != null) {
        // units.addAll(buildingUnits.values());
        // }
        // }
        // }
        // Map<String, Land> lands = property.getLands();
        // if (lands != null) {
        // for (Land land : lands.values()) {
        // Map<String, Unit> landUnits = land.getUnits();
        // if (landUnits != null) {
        // units.addAll(landUnits.values());
        // }
        // }
        // }
        //
        // writeUnhashedUnit = true;
        // writeNodes(units);
        // writeUnhashedUnit = false;
        // }
        writeSubNodes(node, info);
        // }

        writer.writeEndElement();
    }

    // //////////////////////////////////////

    protected void writeAttributes(AbstractNode node, NodeInformation info) throws Exception {
        List<Field> attributeFields = info.getAttributeFields();
        for (Field field : attributeFields) {
            String fieldName = field.getName();
            Object fieldValue = new PropertyDescriptor(fieldName, node.getClass()).getReadMethod().invoke(node);
            if (fieldValue != null) {
                writeAttribute(fieldName, fieldValue, WriteRules.NO_WRITE_IF_NULL);
            }
        }
    }

    protected void writeDataFields(AbstractNode node, NodeInformation info) throws Exception {
        List<Field> dataFields = info.getDataFields();
        for (Field field : dataFields) {
            writeDataField(field, node);
        }
    }

    protected void writeDataField(Field field, AbstractNode node) throws Exception {
        String fieldName = field.getName();
        Object fieldValue = new PropertyDescriptor(fieldName, node.getClass()).getReadMethod().invoke(node);
        if (fieldValue != null) {
            writeValue(fieldName, fieldValue, WriteRules.NO_WRITE_IF_NULL);
        }
    }

    protected void writeGroupNodes(AbstractNode node, NodeInformation info) throws Exception {
        List<Field> groupnodeFields = info.getGroupNodes();
        for (Field field : groupnodeFields) {
            String fieldName = field.getName();
            Object fieldValue = new PropertyDescriptor(fieldName, node.getClass()).getReadMethod().invoke(node);
            if (fieldValue != null) {
                writeNode((AbstractGroupNode) fieldValue);
            }
        }
    }

    protected void writeExtensionMap(AbstractNode node, NodeInformation info) throws Exception {
        ExtensionMap em = node.getExtensionMap();

        Set<String> valuesKeySet = em.getValuesKeySet();
        Set<String> subListsKeySet = em.getSubListsKeySet();
        Set<String> subMapsKeySet = em.getSubMapsKeySet();

        if ((valuesKeySet != null && !valuesKeySet.isEmpty()) || (subListsKeySet != null && !subListsKeySet.isEmpty())
                || (subMapsKeySet != null && !subMapsKeySet.isEmpty())) {

            writer.writeStartElement("extension");

            if (valuesKeySet != null) {
                for (String curKey : valuesKeySet) {
                    writer.writeStartElement("key");
                    writer.writeAttribute("name", curKey);
                    writer.writeCharacters(em.getValue(curKey));
                    writer.writeEndElement();
                }
            }
            if (subListsKeySet != null) {
                for (String curKey : subListsKeySet) {
                    writer.writeStartElement("subList");
                    writer.writeAttribute("name", curKey);

                    List<String> curSubList = em.getSubList(curKey);
                    for (String entry : curSubList) {
                        writer.writeStartElement("item");
                        writer.writeCharacters(entry);
                        writer.writeEndElement();
                    }

                    writer.writeEndElement();
                }
            }
            // TODO: Submaps

            writer.writeEndElement();
        }
    }

    @SuppressWarnings("unchecked")
    protected void writeSubNodes(AbstractNode node, NodeInformation info) throws Exception {
        List<Field> nodelistFields = info.getNodeLists();
        for (Field field : nodelistFields) {
            String fieldName = field.getName();
            Object fieldValue = new PropertyDescriptor(fieldName, node.getClass()).getReadMethod().invoke(node);
            if (fieldValue != null) {
                writeNodes(((Map<String, AbstractNode>) fieldValue).values());
            }
        }
    }

    // ////////////////////////////////////////////////

    protected void writeAttribute(String name, Object value, WriteRules rule) throws XMLStreamException {
        if (rule == WriteRules.WRITE_ALWAYS || (rule == WriteRules.NO_WRITE_IF_NULL && value != null)
                || (rule == WriteRules.NO_WRITE_IF_NULl_OR_EMPTY && value != null)) {
            writeAttribute(name, value);
        }
    }

    protected void writeAttribute(String name, Object value) throws XMLStreamException {
        writer.writeAttribute(name, value.toString());
    }

    protected void writeValue(String name, Object value, WriteRules rule) throws XMLStreamException {
        if (rule == WriteRules.WRITE_ALWAYS || (rule == WriteRules.NO_WRITE_IF_NULL && value != null)
                || (rule == WriteRules.NO_WRITE_IF_NULl_OR_EMPTY && value != null)) {
            writeValue(name, value);
        }
    }

    protected String formatNumber(Number number) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
        DecimalFormat myFormatter = (DecimalFormat)nf;
        myFormatter.applyPattern("##0.#####");
        return myFormatter.format(number);
    }

    protected void writeValue(String name, Object value) throws XMLStreamException {
        String setValue = null;

        writer.writeStartElement(name);
        if (value instanceof Area) {
            Area area = (Area) value;

            writer.writeAttribute("areaMeasurement", area.getAreaMessurement().toString());
            writer.writeAttribute("areaType", area.getAreaType().toString());

            setValue = formatNumber(area.getValue());
        } else if (value instanceof Amount) {
            Amount amount = (Amount) value;

            writer.writeAttribute("currency", amount.getCurrency().toString());

            setValue = formatNumber(amount.getValue());
        } else if (value instanceof Number) {
            setValue = formatNumber((Number) value);
        } else {
            setValue = value.toString();
        }

        writer.writeCharacters(setValue);
        writer.writeEndElement();
    }

    protected void writeNodes(Collection<? extends AbstractNode> nodes) throws Exception {
        if (nodes != null && !nodes.isEmpty()) {
            Boolean firstNode = true;
            for (AbstractNode node : nodes) {
                if (firstNode) {
                    String nodeName = "LIST_OF_" + getTagnameOfNode(node);
                    writer.writeStartElement(nodeName);
                    firstNode = false;
                }
                writeNode(node);
            }
            writer.writeEndElement();
        }
    }

    private static String camelCase2UpperCase(String camelCase) {
        return camelCase.replaceAll("(.)(\\p{Upper})", "$1_$2").toUpperCase();
    }

    protected String getTagnameOfNode(AbstractNode node) {
        String simpleName = node.getClass().getSimpleName();

        if (node instanceof Data || node instanceof Address || node instanceof EnergyRating) {
            return simpleName.toLowerCase();
        } else if (node instanceof LeasedUnit) {
            return "UNIT";
        } else {
            return camelCase2UpperCase(simpleName);
        }
    }

    /**
     * @author Pascal Houdek
     * @return the outStream
     */
    protected OutputStream getOutStream() {
        return outStream;
    }

    /**
     * @author Pascal Houdek
     * @return the writer
     */
    protected XMLStreamWriter getWriter() {
        return writer;
    }
}
