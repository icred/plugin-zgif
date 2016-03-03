package org.zgif.icred.plugin.zgif;

import org.zgif.icred.plugin.zgif.read.ZGifReader;
import org.zgif.icred.plugin.zgif.write.ZGifWriter;

import eu.icred.plugin.IPlugin;
import eu.icred.plugin.worker.input.IImportWorker;
import eu.icred.plugin.worker.output.IExportWorker;

public class Plugin implements IPlugin {

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#isModelVersionSupported(java.lang.String)
     */
    @Override
    public boolean isModelVersionSupported(String version) {
        return version.startsWith("1-0.");
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginId()
     */
    @Override
    public String getPluginId() {
        return "zgif";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginVersion()
     */
    @Override
    public String getPluginVersion() {
        return "0.6.2.1";
    }

    /* (non-Javadoc)
     * @see eu.icred.plugin.IPlugin#getPluginName()
     */
    @Override
    public String getPluginName() {
        return "zgif-Plugin";
    }
    
    @Override
    public IImportWorker getImportPlugin() {
        return new ZGifReader();
    }

    @Override
    public IExportWorker getExportPlugin() {
        return new ZGifWriter();
    }

}
