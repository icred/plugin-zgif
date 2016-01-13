/**
 * 
 */
package org.zgif.icred.plugin.zgif.write;

import java.beans.PropertyDescriptor;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.log4j.Logger;

import eu.icred.model.NodeInformation;
import eu.icred.model.datatype.Amount;
import eu.icred.model.datatype.Area;
import eu.icred.model.node.AbstractNode;
import eu.icred.model.node.Data;
import eu.icred.model.node.group.AbstractGroupNode;

/**
 * @author Pascal Houdek
 * 
 */
public class DataWriter extends BasicNodeWriter {
    private static Logger   logger = Logger.getLogger(DataWriter.class);
    /**
     * @author Pascal Houdek
     * @throws XMLStreamException 
     */
    public DataWriter(OutputStream outStream) throws XMLStreamException {
        super(outStream);
    }

    public void write(Data data) {
        try {
            XMLStreamWriter writer = this.getWriter();
            writer.writeStartDocument();

            writeNode(data);

            writer.writeEndDocument();
            writer.flush();
        } catch (Exception e) {
            logger.warn("unknown exception", e);
        }
    }

}
