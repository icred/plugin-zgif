package org.zgif.icred.plugin.zgif.write;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import eu.icred.model.datatype.enumeration.Subset;
import eu.icred.model.node.Container;
import eu.icred.model.node.Data;
import eu.icred.model.node.Meta;
import eu.icred.model.node.Period;
import eu.icred.plugin.PluginComponent;
import eu.icred.plugin.worker.WorkerConfiguration;
import eu.icred.plugin.worker.output.ExportWorkerConfiguration;
import eu.icred.plugin.worker.output.IExportWorker;

/**
 * @author phoudek
 * 
 */
public class ZGifWriter implements IExportWorker {
    private static Logger logger = Logger.getLogger(ZGifWriter.class);

    public static final Subset[] SUPPORTED_SUBSETS = Subset.values();
    private static String PARAMETER_NAME = "zgif";

    private Container container = null;
    private ZipOutputStream zipOut;
    private List<String> fileList = new ArrayList<String>();
    private ExportWorkerConfiguration config;

    /*
     * (non-Javadoc)
     * 
     * @see eu.icred.plugin.output.ExportPlugin#getSupportedSubsets()
     */
    @Override
    public List<Subset> getSupportedSubsets() {
        return Arrays.asList(SUPPORTED_SUBSETS);
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.icred.plugin.output.IExportPlugin#
     * getRequiredConfigurationArguments()
     */
    @Override
    public ExportWorkerConfiguration getRequiredConfigurationArguments() {
        return new ExportWorkerConfiguration() {
            {
                SortedMap<String, OutputStream> streams = getStreams();
                streams.put(PARAMETER_NAME, null);
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.icred.plugin.output.ExportPlugin#getConfigGui()
     */
    @Override
    public PluginComponent<ExportWorkerConfiguration> getConfigGui() {
        // null => DefaultConfigGui
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.icred.plugin.IPlugin#load(eu.icred.plugin.
     * PluginConfiguration)
     */
    @Override
    public void load(WorkerConfiguration config) {
        throw new RuntimeException("not allowed");
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * eu.icred.plugin.output.ExportPlugin#load(eu.icred
     * .plugin.PluginConfiguration, eu.icred.model.node.AbstractContainer)
     */
    @Override
    public void load(ExportWorkerConfiguration config, Container zgif) {
        this.container = zgif;
        this.config = config;

        doExport();
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.icred.plugin.output.ExportPlugin#unload()
     */
    @Override
    public void unload() {
    }

    // //////////////////////////////////////////////////////////

    @SuppressWarnings({ "unused", "unchecked" })
    private void doExport() {
        try {
            OutputStream outStream = config.getStreams().get(PARAMETER_NAME);
            zipOut = new ZipOutputStream(outStream, Charset.forName("UTF-8"));
            zipOut.setLevel(ZipOutputStream.STORED);

            Meta meta = container.getMeta();
            nextZipEntry("mimetype");
            zipOut.write("application/vnd.gif-ev.zgif".getBytes());
            zipOut.closeEntry();

            nextZipEntry("type");
            zipOut.write("XML".getBytes());
            zipOut.closeEntry();

            zipOut.setLevel(ZipOutputStream.DEFLATED);

            nextZipEntry("meta.xml");
            MetaWriter metaWriter = new MetaWriter(zipOut);
            metaWriter.write(meta);

            Data data = container.getMaindata(false);
            if (data != null) {
                exportData(data, "maindata.xml");
            }

            Map<String, Period> periods = container.getPeriods(false);
            if (periods != null) {
                exportPeriods(periods.values());
            }

            if (data == null && periods == null) {
                logger.error("data and periods of zgif is null!");
            }

            nextZipEntry("META-INF/manifest.xml");
            ManifestWriter manifestWriter = new ManifestWriter(zipOut);
            manifestWriter.write(fileList);

            zipOut.closeEntry();
            zipOut.close();
        } catch (Exception e) {
            logger.error("Error writing zgif file", e);
        }
    }

    private void nextZipEntry(String path) throws IOException {
        zipOut.putNextEntry(new ZipEntry(path));
        fileList.add(path);
    }

    private void exportData(Data data, String path) throws Exception {
        nextZipEntry(path);
        DataWriter dataWriter = new DataWriter(zipOut);
        dataWriter.write(data);
    }

    private void exportPeriods(Collection<Period> periods) throws Exception {

        for (Period period : periods) {
            nextZipEntry("periods/" + period.getIdentifier() + ".xml");
            DataWriter dataWriter = new DataWriter(zipOut);
            dataWriter.write(period.getData());
        }

        nextZipEntry("periods.xml");
        new PeriodsWriter(zipOut).write(periods);

    }
}
