/*
 * Copyright (c) 2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.developerstudio.eclipse.distribution.project.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.wso2.developerstudio.eclipse.carbonserver.base.util.CarbonUtils;
import org.wso2.developerstudio.eclipse.distribution.project.Activator;
import org.wso2.developerstudio.eclipse.distribution.project.model.ArtifactData;
import org.wso2.developerstudio.eclipse.distribution.project.model.DependencyData;
import org.wso2.developerstudio.eclipse.distribution.project.util.ArtifactTypeMapping;
import org.wso2.developerstudio.eclipse.distribution.project.util.DistProjectUtils;
import org.wso2.developerstudio.eclipse.distribution.project.validator.ProjectList;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.eclipse.maven.util.MavenUtils;
import org.wso2.developerstudio.eclipse.platform.core.model.AbstractListDataProvider.ListData;
import org.wso2.developerstudio.eclipse.platform.core.project.export.ProjectArtifactHandler;
import org.wso2.developerstudio.eclipse.platform.core.project.export.util.ExportUtil;
import org.wso2.developerstudio.eclipse.platform.core.utils.XMLUtil;
import org.wso2.developerstudio.eclipse.utils.archive.ArchiveManipulator;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.wso2.developerstudio.eclipse.utils.file.TempFileUtils;

public class CarExportHandler extends ProjectArtifactHandler {
	private static final String GOVERNANCE_REGISTRY_SERVER_ROLE = "capp/GovernanceRegistry";
	private static IDeveloperStudioLog log = Logger.getLog(Activator.PLUGIN_ID);
	private static final String EXECUTIONCLASS = "executionclass";
	private static final String SERVER_ROLE = "server.role";
	private static final String CAPP_PROJECT_EXPORT_HANDLER_EXTENSION_ID =
	                                                                       "org.wso2.developerstudio.eclipse.capp.project.export.handler";
	private static final String POM_FILE = "pom.xml";
	private static final String SPLIT_DIR_NAME = "split_esb_resources";
	CarbonUtils carbonUtils = new CarbonUtils();

	public List<IResource> exportArtifact(IProject project) throws Exception {
		List<IResource> exportResources = new ArrayList<IResource>();
		List<ArtifactData> artifactList = new ArrayList<ArtifactData>();
		Map<IProject, Map<String, IResource>> resourceProjectList = new HashMap<IProject, Map<String, IResource>>();
		Map<IProject, Map<String, IResource>> graphicalSynapseProjectList =
		                                                                    new HashMap<IProject, Map<String, IResource>>();
		IFile pomFileRes;
		File pomFile;
		MavenProject parentPrj;
		DistProjectUtils distProjectUtils = new DistProjectUtils();
		ArchiveManipulator archiveManipulator = new ArchiveManipulator();

		clearTarget(project);

		// Let's create a temp project
		File tempProject = createTempProject();

		File carResources = createTempDir(tempProject, "car_resources");
		IFolder splitESBResources = getTempDirInWorksapce(project.getName(), SPLIT_DIR_NAME);
		pomFileRes = project.getFile(POM_FILE);
		if (!pomFileRes.exists()) {
			throw new Exception("not a valid carbon application project");
		}
		pomFile = pomFileRes.getLocation().toFile();

		ProjectList projectListProvider = new ProjectList();
		List<ListData> projectListData = projectListProvider.getListData(null, null);
		Map<String, DependencyData> projectList = new HashMap<String, DependencyData>();
		Map<String, String> serverRoleList = new HashMap<String, String>();
		for (ListData data : projectListData) {
			DependencyData dependencyData = (DependencyData) data.getData();
			projectList.put(DistProjectUtils.getArtifactInfoAsString(dependencyData.getDependency()), dependencyData);
		}

		parentPrj = MavenUtils.getMavenProject(pomFile);

		for (Dependency dependency : (List<Dependency>) parentPrj.getDependencies()) {
			String dependencyKey = DistProjectUtils.getArtifactInfoAsString(dependency);
			serverRoleList.put(dependencyKey, DistProjectUtils.getServerRole(parentPrj, dependency));
			if (projectList.containsKey(dependencyKey)) {
				DependencyData dependencyData = projectList.get(dependencyKey);
				Object parent = dependencyData.getParent();
				Object self = dependencyData.getSelf();
				String serverRole = serverRoleList.get(DistProjectUtils.getArtifactInfoAsString(dependency));
				dependencyData.setServerRole(serverRole.replaceAll("^capp/", ""));
				if (parent != null && self != null) { // multiple artifact
					selectExporterExecCalss(serverRole, artifactList, graphicalSynapseProjectList, splitESBResources,
					                        dependencyData, parent, self);
				} else if (parent == null && self != null) { // artifacts as
					// single
					// archive
					ArtifactExportHandler artifactExportHandler = new ArtifactExportHandler();
					artifactExportHandler.exportArtifact(artifactList, null, null, dependencyData, parent, null);
				} else if (parent != null && self == null) { // these are
				// registry resources that may have some other server role,
				// to get the correct artifact exporter we need to set the
				// server role here as GovernanceRegistry
					selectExporterExecCalss(GOVERNANCE_REGISTRY_SERVER_ROLE, artifactList, resourceProjectList, null,
					                        dependencyData, parent, null);
				} else {
					// TODO : give an error message
				}
			}
		}

		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMElement artifactsDocRoot = factory.createOMElement(new QName("artifacts"));
		OMElement artifactElt = factory.createOMElement(new QName("artifact"));
		artifactElt.addAttribute("name", parentPrj.getModel().getArtifactId(), null);
		artifactElt.addAttribute("version", parentPrj.getModel().getVersion(), null);
		artifactElt.addAttribute("type", "carbon/application", null);

		/*
		 * Sort artifacts in order to arrange them based on their priorities.
		 * Fixing TOOLS-2335, TOOLS-2197
		 */
		Collections.sort(artifactList);

		for (ArtifactData artifact : artifactList) {
			File artifactDir = new File(carResources, getArtifactDir(artifact.getDependencyData()));
			if (artifact.getResource() instanceof IFolder) {
				FileUtils.copyDirectory(artifact.getResource().getLocation().toFile(), artifactDir);
			} else if (artifact.getResource() instanceof IFile) {
				FileUtils.copy(artifact.getResource().getLocation().toFile(), new File(artifactDir, artifact.getFile()));
			}
			artifactElt.addChild(createDependencyElement(factory, artifact));
			createArtifactXML(artifactDir, artifact);
		}

		artifactsDocRoot.addChild(artifactElt);
		File artifactsXml = new File(carResources, "artifacts.xml");
		XMLUtil.prettify(artifactsDocRoot, new FileOutputStream(artifactsXml));

		File tmpArchive =
		                  new File(tempProject, project.getName().concat("_").concat(parentPrj.getVersion())
		                                               .concat(".car"));
		archiveManipulator.archiveDir(tmpArchive.toString(), carResources.toString());

		IFile carbonArchive = getTargetArchive(project, parentPrj.getVersion(), "car");
		FileUtils.copy(tmpArchive, carbonArchive.getLocation().toFile());
		exportResources.add((IResource) carbonArchive);
		clearTempDirInWorksapce(project.getName(), SPLIT_DIR_NAME);
		TempFileUtils.cleanUp();

		return exportResources;
	}

	private String getArtifactDir(DependencyData dependencyData) {
		String artifactDir =
		                     String.format("%s_%s", dependencyData.getDependency().getArtifactId(),
		                                   dependencyData.getDependency().getVersion());
		return artifactDir;
	}

	private void createArtifactXML(File artifactDir, ArtifactData artifact) {
		OMFactory factory = OMAbstractFactory.getOMFactory();
		OMElement artifactElt = factory.createOMElement(new QName("artifact"));
		artifactElt.addAttribute("name", artifact.getDependencyData().getDependency().getArtifactId(), null);
		artifactElt.addAttribute("version", artifact.getDependencyData().getDependency().getVersion(), null);
		artifactElt.addAttribute("type", artifact.getDependencyData().getCApptype(), null);
		artifactElt.addAttribute("serverRole", artifact.getDependencyData().getServerRole(), null);
		OMElement fileElt = factory.createOMElement(new QName("file"));
		fileElt.setText(artifact.getFile());
		artifactElt.addChild(fileElt);
		File artifactXml = new File(artifactDir, "artifact.xml");
		try {
			XMLUtil.prettify(artifactElt, new FileOutputStream(artifactXml));
		} catch (Exception e) {
			log.error("Error creating artifact.xml", e);
		}
	}

	private OMElement createDependencyElement(OMFactory factory, ArtifactData artifact) {
		OMElement dependencyElt = factory.createOMElement(new QName("dependency"));
		dependencyElt.addAttribute("artifact", artifact.getDependencyData().getDependency().getArtifactId(), null);
		dependencyElt.addAttribute("version", artifact.getDependencyData().getDependency().getVersion(), null);
		dependencyElt.addAttribute("include", "true", null);
		dependencyElt.addAttribute("serverRole", artifact.getDependencyData().getServerRole(), null);
		return dependencyElt;
	}

	/*
	 * This method will recognize all the classes registered to execute artifact
	 * export for capps
	 */
	private void selectExporterExecCalss(String serverRole, List<ArtifactData> artifactList,
	                                     Map<IProject, Map<String, IResource>> synapseProjectList,
	                                     IFolder splitESBResources, DependencyData dependencyData, Object parent,
	                                     Object self) {
		IConfigurationElement[] elements =
		                                   carbonUtils.getExtensionPointmembers(CAPP_PROJECT_EXPORT_HANDLER_EXTENSION_ID);
		if (elements != null) {
			for (int j = 0; j < elements.length; j++) {
				IConfigurationElement config = elements[j];
				String configServerRole = config.getAttribute(SERVER_ROLE);
				if (configServerRole != null && configServerRole.equals(serverRole)) {
					Object execClassObject;
					try {
						execClassObject = config.createExecutableExtension(EXECUTIONCLASS);
						if (execClassObject instanceof DefaultArtifactExportHandler) {
							executeExtension(execClassObject, artifactList, synapseProjectList, splitESBResources,
							                 dependencyData, parent, self);
						}
					} catch (InvalidRegistryObjectException e) {
						log.error("Exception thrown in trying to export the car file, ", e);
					} catch (CoreException e) {
						log.error("Exception thrown in trying to export the car file, ", e);
					}
				}
			}
		} else {
			log.info("No classes were found extending the extension point " +
			         CAPP_PROJECT_EXPORT_HANDLER_EXTENSION_ID + "to perform the artifact export");
		}
	}

	private void executeExtension(final Object execClass, final List<ArtifactData> artifactList,
	                              final Map<IProject, Map<String, IResource>> synapseProjectList,
	                              final IFolder splitESBResources, final DependencyData dependencyData,
	                              final Object parent, final Object self) {
		ISafeRunnable runnable = new ISafeRunnable() {
			@Override
			public void handleException(Throwable e) {
				log.error("Exception thrown in trying to execute the class to export car file artifacts " + execClass,
				          e);
			}
			@Override
			public void run() throws Exception {
				((DefaultArtifactExportHandler) execClass).exportArtifact(artifactList, synapseProjectList, splitESBResources,
				                                            dependencyData, parent, self);
			}
		};
		SafeRunner.run(runnable);
	}

}
