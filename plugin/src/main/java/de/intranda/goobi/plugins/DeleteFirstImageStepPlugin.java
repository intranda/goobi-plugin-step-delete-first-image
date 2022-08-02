package de.intranda.goobi.plugins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class DeleteFirstImageStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_deleteFirstImage";
    @Getter
    private Step step;
    private String namepartSplitter = "_";
    private String returnPath;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig(title, step);
        namepartSplitter = myconfig.getString("namepartSplitter", "_");
        log.info("DeleteFirstImage step plugin initialized");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_deleteFirstImage.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        boolean successfull = true;

        try {
            Path masterFolderPath = Paths.get(step.getProzess().getImagesOrigDirectory(false));

            Path mediaFolderPath = Paths.get(step.getProzess().getImagesTifDirectory(false));

            List<Path> imagesInAllFolder = new ArrayList<>();

            if (Files.exists(masterFolderPath)) {
                imagesInAllFolder.addAll(StorageProvider.getInstance().listFiles(masterFolderPath.toString()));
            }

            if (Files.exists(mediaFolderPath)) {
                imagesInAllFolder.addAll(StorageProvider.getInstance().listFiles(mediaFolderPath.toString()));
            }

            for (Path imageName : imagesInAllFolder) {
                String filenameWithoutExtension = imageName.getFileName().toString();
                filenameWithoutExtension = filenameWithoutExtension.substring(0, filenameWithoutExtension.lastIndexOf("."));
                String[] nameParts = filenameWithoutExtension.split(namepartSplitter);
                // just check the last token
                String part = nameParts[nameParts.length -1];
                // if all parts should be checked, uncomment this for loop
                //                for (String part : nameParts) {
                if (StringUtils.isNumeric(part) && part.length() > 1 ) {
                    // check if it is 0, 00, 000, 0000, ....
                    if (Integer.parseInt(part) == 0) {
                        // delete image
                        StorageProvider.getInstance().deleteFile(imageName);
                    }
                }
                //                }
            }

        } catch (IOException | SwapException | DAOException e) {
            log.error(e);
        }

        log.info("DeleteFirstImage step plugin executed");
        if (!successfull) {
            return PluginReturnValue.ERROR;
        }
        return PluginReturnValue.FINISH;
    }
}
