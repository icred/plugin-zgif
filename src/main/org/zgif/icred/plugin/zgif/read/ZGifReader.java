package org.zgif.icred.plugin.zgif.read;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

import eu.icred.model.datatype.enumeration.Subset;
import eu.icred.model.node.Container;
import eu.icred.model.node.Data;
import eu.icred.model.node.Meta;
import eu.icred.model.node.Period;
import eu.icred.plugin.PluginComponent;
import eu.icred.plugin.worker.WorkerConfiguration;
import eu.icred.plugin.worker.input.IImportWorker;
import eu.icred.plugin.worker.input.ImportWorkerConfiguration;

public class ZGifReader implements IImportWorker {
    private static Logger logger = Logger.getLogger(ZGifReader.class);

    public static final Subset[] SUPPORTED_SUBSETS = Subset.values();
    private static String PARAMETER_NAME = "zgif";

    private Container container = null;
    private InputStream zgifStream = null;

    @Override
    public List<Subset> getSupportedSubsets() {
        return Arrays.asList(SUPPORTED_SUBSETS);
    }

    @Override
    public void load(WorkerConfiguration config) {
        throw new RuntimeException("not allowed");
    }

    @Override
    public void unload() {
        try {
            zgifStream.close();
        } catch(Throwable t) {
        }
        zgifStream = null;
    }

    @Override
    public void load(ImportWorkerConfiguration config) {
        zgifStream = config.getStreams().get(PARAMETER_NAME);

        ZipInputStream zipStream = new ZipInputStream(zgifStream, Charset.forName("UTF-8")){
            public void close() throws IOException {
            //Do nothing
            }
        };

        try {
            container = new Container();
            
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.equals("mimetype")) {
                    checkMimeType(zipStream);
                }
                if (entryName.equals("type")) {
                    checkType(zipStream);
                }
                
                if(entryName.equals("meta.xml")) {
                    readMeta(zipStream);
                }
                if(entryName.equals("periods.xml")) {
                    readPeriods(zipStream);
                }
                if(entryName.startsWith("periods/")) {
                    readPeriod(entryName.substring(8).replaceAll("[.]xml", ""), zipStream);
                }
                if(entryName.equals("maindata.xml")) {
                    readData(zipStream);
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public ImportWorkerConfiguration getRequiredConfigurationArguments() {
        return new ImportWorkerConfiguration() {
            {
                SortedMap<String, InputStream> streams = getStreams();
                streams.put(PARAMETER_NAME, null);
            }
        };
    }

    @Override
    public PluginComponent<ImportWorkerConfiguration> getConfigGui() {
        // null => DefaultConfigGui
        return null;
    }

    @Override
    public Container getContainer() {
        return container;
    }

    private void checkMimeType(ZipInputStream zipStream) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(zipStream, Charset.forName("UTF-8")));
        if (!br.readLine().equals("application/vnd.gif-ev.zgif")) {
            throw new Exception("invalid mimetype");
        }
    }

    private void checkType(ZipInputStream zipStream) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(zipStream, Charset.forName("UTF-8")));
        if (!br.readLine().equals("XML")) {
            throw new Exception("invalid type");
        }
    }


    private void readMeta(ZipInputStream zipStream) throws Exception {
        Meta meta = container.getMeta();
        
        MetaReader reader = new MetaReader(meta);
        reader.read(zipStream);
    }
    private void readPeriods(ZipInputStream zipStream) throws Exception {
        Map<String, Period> periods = container.getPeriods();
        
        PeriodsReader reader = new PeriodsReader(periods);
        reader.read(zipStream);
    }
    
    private void readPeriod(String identifier, ZipInputStream zipStream) throws Exception {
        Map<String, Period> periods = container.getPeriods();
        if(!periods.containsKey(identifier)) {
            periods.put(identifier, new Period());
        }
        
        DataReader reader = new DataReader(periods.get(identifier));
        reader.read(zipStream);
    }
    
    private void readData(ZipInputStream zipStream) throws Exception {
        Data data = container.getMaindata();
        
        DataReader reader = new DataReader(data);
        reader.read(zipStream);
    }
}
