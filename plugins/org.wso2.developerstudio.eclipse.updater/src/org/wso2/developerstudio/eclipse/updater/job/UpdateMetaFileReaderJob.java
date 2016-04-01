/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.developerstudio.eclipse.updater.job;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.eclipse.platform.ui.preferences.PreferenceInitializer;
import org.wso2.developerstudio.eclipse.platform.ui.preferences.UpdateCheckerPreferencePage;
import org.wso2.developerstudio.eclipse.updater.Messages;
import org.wso2.developerstudio.eclipse.updater.UpdaterPlugin;
import org.wso2.developerstudio.eclipse.updater.core.UpdateManager;

/**
 * This class defines the job in updater checker to read possible updates
 * availability from the meta file.
 *
 */
public class UpdateMetaFileReaderJob extends Job {

	private static final String ERROR_TITLE = "Automatic Updater error";
	private static final String ERROR_MESSAGE = "Error in retrieving available updates automatically";
	private static final String UPDATES_TXT_FILE = "updates.txt";
	private static final int TIME_OUT_MS = 1000;
	protected UpdateManager updateManager;
	public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
	public static final String folderLoc = System.getProperty(JAVA_IO_TMPDIR) + File.separator + "DevStudioUpdater";
	public static final String fileLoc = folderLoc + File.separator + UPDATES_TXT_FILE;
	protected static IDeveloperStudioLog log = Logger.getLog(UpdaterPlugin.PLUGIN_ID);

	public UpdateMetaFileReaderJob(UpdateManager updateManager) {
		super(Messages.UpdateCheckerJob_0);
		this.updateManager = updateManager;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {
			SubMonitor progress = SubMonitor.convert(monitor, Messages.UpdateCheckerJob_1, 2);
			Collection<IInstallableUnit> installedWSO2Features = updateManager
					.getInstalledWSO2Features(progress.newChild(1));
			// read the meta file
			downloadMetaFile();
			HashMap<String, String> availaleDevStudioFeatureVerions = new HashMap<String, String>();
			availaleDevStudioFeatureVerions = readMetaDataFiletoMap();
			if (availaleDevStudioFeatureVerions.isEmpty()) {
				log.error(Messages.UpdateCheckerJob_4);
				promptUserError(ERROR_MESSAGE, ERROR_TITLE);
				return Status.CANCEL_STATUS;
			}
			List<String> updatedFeatureList = new ArrayList<String>();
			for (IInstallableUnit iInstallableUnit : installedWSO2Features) {
				if (availaleDevStudioFeatureVerions.containsKey(iInstallableUnit.getId())) {
					Version availableVersion = generateVersionFromString(
							availaleDevStudioFeatureVerions.get(iInstallableUnit.getId()));
					if (availableVersion != null && iInstallableUnit.getVersion().compareTo(availableVersion) == 1) {
						updatedFeatureList.add(iInstallableUnit.getId());
						return Status.OK_STATUS;// if at least one update is
												// available return OK
					} else if (availableVersion == null) {
						log.error(Messages.UpdateCheckerJob_4);
						promptUserError(ERROR_MESSAGE, ERROR_TITLE);
						return Status.CANCEL_STATUS;// return OK since we need to
					}
				}
			}
		} catch (Exception e) {
			log.error(Messages.UpdateCheckerJob_4, e);
			promptUserError(ERROR_MESSAGE, ERROR_TITLE);
			return Status.CANCEL_STATUS;
		}
		return Status.CANCEL_STATUS;// if no exceptions and no updates to be
									// installed return cancel
	}

	/**
	 * version would come as 4.0.0-201512221344 we need to create the
	 * installable unit version object from the version string
	 * 
	 * @param versionString
	 */
	private Version generateVersionFromString(String versionString) {
		String[] versionVal = versionString.split("-");
		String[] minorMajorNumbers = versionVal[0].split("\\."); // . is \\. in
																	// regex
		Version availableVersion = Version.createOSGi(Integer.parseInt(minorMajorNumbers[0]),
				Integer.parseInt(minorMajorNumbers[1]), Integer.parseInt(minorMajorNumbers[2]), versionVal[1]);
		return availableVersion;
	}

	private HashMap<String, String> readMetaDataFiletoMap() {
		HashMap<String, String> featureMap = new HashMap<String, String>();
		try (BufferedReader br = new BufferedReader(new FileReader(fileLoc))) {
			for (String line; (line = br.readLine()) != null;) {
				if (line.contains(",")) {
					String[] feature = line.split(",");
					featureMap.put(feature[0], feature[1]);
				}
			}
		} catch (IOException e) {
			log.error(Messages.UpdateCheckerJob_3, e);
		}
		return featureMap;
	}

	private void downloadMetaFile() {
		try {
			File updateFile = new File(fileLoc);
			// needs to enable once the pref store values are up
			IPreferenceStore preferenceStore = PlatformUI.getPreferenceStore();
			String url = preferenceStore.getString(UpdateCheckerPreferencePage.RELESE_SITE_URL) + UPDATES_TXT_FILE;// TODO
																													// replace
			if (url == null || url.isEmpty()) {
				url = PreferenceInitializer.DEFAULT_UPDATE_SITE + UPDATES_TXT_FILE;
			}
			URL link = new URL(url); // The file that you want to download
			// apache commons file utils, creates the directory structure if not
			// exist and write the content from the online file to the file
			// system.
			FileUtils.copyURLToFile(link, updateFile, TIME_OUT_MS, TIME_OUT_MS);
		} catch (IOException e) {
			// log while reading file
			log.error("error in writing the the content from the downloaded meta data file for updates", e);
		}
	}

	public static void promptUserError(final String message, final String title) {

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				// if exception occurred dont run automatic updater,
				// prompt user.
				MessageDialog.openError(Display.getDefault().getActiveShell(), title, message);
			}
		});
	}

}
