/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.developerstudio.eclipse.wso2plugin.sample.ui.elements;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.wso2.developerstudio.eclipse.wso2plugin.sample.util.WSO2PluginConstants;

import com.google.gson.annotations.SerializedName;

/*
 * This is a WSO2 Plugin Element of which, the parameters should be defined when
 * a sample is added via the
 * provided extension point to the developer studio plugin samples.
 */
public class WSO2PluginSampleExt {

	

	@SerializedName("pluginName")
	String pluginName;

	@SerializedName("pluginDescription")
	String pluginDescription;

	@SerializedName("pluginArchive")
	String pluginArchive;

	String bundleID;

	Image image;

	@SerializedName("iconLoc")
	String iconLoc;
	
	@SerializedName("isUpdatedFromGit")
	String isUpdatedFromGit;

	public String getIsUpdatedFromGit() {
		return isUpdatedFromGit;
	}

	public void setIsUpdatedFromGit(String isUpdatedFromGit) {
		this.isUpdatedFromGit = isUpdatedFromGit;
	}

	public String getIconLoc() {
		return iconLoc;
	}

	public void setIconLoc(String iconLoc) {
		this.iconLoc = iconLoc;
	}

	public WSO2PluginSampleExt(String pluginName, String pluginArchive, String description, String pluginBundleID,
	                           String iconLoc, String isPluginUpdatedFromGit) {
		super();
		this.pluginName = pluginName;
		this.pluginArchive = pluginArchive;
		this.pluginDescription = description;
		this.bundleID = pluginBundleID;
		this.iconLoc = iconLoc;
		this.isUpdatedFromGit = isPluginUpdatedFromGit;
	}

	public Image getImage() {
		ImageDescriptor imageDescriptor = null;
		if (!Boolean.parseBoolean(this.isUpdatedFromGit)) {
			imageDescriptor =
			                  ImageDescriptor.createFromURL(FileLocator.find(Platform.getBundle(bundleID),
			                                                                 new Path(iconLoc), null));
			return imageDescriptor.createImage();
		} else {
			try {
				imageDescriptor = ImageDescriptor.createFromURL(new URL(WSO2PluginConstants.FILE_PROTOCOL + iconLoc));
				return imageDescriptor.createImage();
			} catch (MalformedURLException e) {
				// log image cannot be found at location
				return null;
			}
		}
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public String getBundleID() {
		return bundleID;
	}

	public void setBundleID(String bundleID) {
		this.bundleID = bundleID;
	}

	@Override
	public String toString() {
		return pluginName;
	}

	public String getPluginDescription() {
		return pluginDescription;
	}

	public void setPluginDescription(String pluginDescription) {
		this.pluginDescription = pluginDescription;
	}

	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public String getPluginArchive() {
		return pluginArchive;
	}

	public void setPluginArchive(String pluginArchive) {
		this.pluginArchive = pluginArchive;
	}
}
