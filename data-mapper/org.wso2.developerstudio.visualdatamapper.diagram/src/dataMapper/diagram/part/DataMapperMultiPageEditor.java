package dataMapper.diagram.part;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.apache.avro.Schema;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.visualdatamapper.diagram.Activator;

import dataMapper.Attribute;
import dataMapper.Concat;
import dataMapper.DataMapperLink;
import dataMapper.DataMapperRoot;
import dataMapper.Element;
import dataMapper.InNode;
import dataMapper.OutNode;
import dataMapper.TreeNode;
import dataMapper.diagram.custom.configuration.function.Function;
import dataMapper.diagram.custom.persistence.AvroSchemaTransformer;
import dataMapper.diagram.custom.persistence.DataMapperConfiguration;
import dataMapper.diagram.custom.persistence.DataMapperConfigurationGenerator;
import dataMapper.diagram.custom.persistence.DataMapperModelTransformer;
import dataMapper.diagram.tree.generator.TreeFromAvro;
import dataMapper.impl.DataMapperRootImpl;
import dataMapper.impl.TreeNodeImpl;

public class DataMapperMultiPageEditor extends MultiPageEditorPart implements IGotoMarker {

	private static DataMapperDiagramEditor graphicalEditor;

	private DataMapperObjectSourceEditor sourceEditor;

	public static final String TEMPORARY_RESOURCES_DIRECTORY = "org.wso2.developerstudio.visualdatamapper";

	private static final int SOURCE_VIEW_PAGE_INDEX = 1;

	private static final int DESIGN_VIEW_PAGE_INDEX = 0;
	
	private static IDeveloperStudioLog log = Logger.getLog(Activator.PLUGIN_ID);

	private Set<IFile> tempFiles = new HashSet<IFile>();
	
//	private static ArrayList<Integer> OPERATION_LIST = new ArrayList<Integer>();

	// private static IDeveloperStudeioLog Log
	// =Logger.getLog(Activator.PLUGIN_ID);

	private boolean sourceDirty;

	public DataMapperMultiPageEditor() {
		super();
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

		try {
			workbench.showPerspective("org.wso2.developerstudio.visualdatamapper.diagram.custom.perspective", window);
		} catch (WorkbenchException e) {
		}

	}

	void createPage0() {
		try {
			graphicalEditor = new DataMapperDiagramEditor(this);
			addPage(DESIGN_VIEW_PAGE_INDEX, graphicalEditor, getEditorInput());
			setPageText(DESIGN_VIEW_PAGE_INDEX, "Design");

			// if(getDiagramGraphicalViewer() != null){
			// getDiagramGraphicalViewer().setProperty(
			// MouseWheelHandler.KeyGenerator.getKey(SWT.CTRL));
			// }
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "ErrorCreating", null, e.getStatus());
		}

		// EditorUtil.setLockmode(graphicalEditor,false);

	}

	/**
	 * Creates page 1 of the multi-page editor, which allows you to edit the
	 * xml.
	 */
	void createPage1() {

		try {
			sourceEditor = new DataMapperObjectSourceEditor(getTemporaryFile("xml"));
			addPage(SOURCE_VIEW_PAGE_INDEX, sourceEditor.getEditor(), sourceEditor.getInput());
			setPageText(SOURCE_VIEW_PAGE_INDEX, "Source");

			sourceEditor.getDocument().addDocumentListener(new IDocumentListener() {

				public void documentAboutToBeChanged(final DocumentEvent event) {
					// nothing to do
				}

				public void documentChanged(final DocumentEvent event) {
					sourceDirty = true;
					// firePropertyChange(PROP_DIRTY);
				}
			});
			Composite composite = new Composite(getContainer(), SWT.NONE);
			FillLayout layout = new FillLayout();
			composite.setLayout(layout);

			// listViewer = new ListViewer(composite);

			// Initialize source editor.
			// updateSourceEditor();
		} catch (Exception ex) {
			// log.error("Error while initializing source viewer control.",ex);
		}

	}

	private IFile getTemporaryFile(String extension) throws Exception {
		String fileName = String.format("%s.%s", UUID.randomUUID().toString(), extension);
		IFile tempFile = getTemporaryDirectory().getFile(fileName);
		if (!tempFile.exists()) {
			tempFile.create(new ByteArrayInputStream(new byte[0]), true, null);
		}
		tempFiles.add(tempFile);
		return tempFile;
	}

	private IFolder getTemporaryDirectory() throws Exception {
		IEditorInput editorInput = getEditorInput();
		if (editorInput instanceof IFileEditorInput || editorInput instanceof FileStoreEditorInput) {

			IProject tempProject = ResourcesPlugin.getWorkspace().getRoot().getProject(".tmp");

			if (!tempProject.exists()) {
				tempProject.create(new NullProgressMonitor());
			}

			if (!tempProject.isOpen()) {
				tempProject.open(new NullProgressMonitor());
			}

			if (!tempProject.isHidden()) {
				tempProject.setHidden(true);
			}

			IFolder folder = tempProject.getFolder(TEMPORARY_RESOURCES_DIRECTORY);

			if (!folder.exists()) {
				folder.create(true, true, new NullProgressMonitor());
			}

			return folder;
		} else {
			throw new Exception("Unable to create temporary resources directory.");
		}
	}

	protected void createPages() {
		createPage0();

		createPage1();

		/*
		 * EditorUtils.setLockmode(graphicalEditor, true); IFile file =
		 * ((IFileEditorInput)getEditorInput()).getFile(); ElementDuplicator
		 * endPointDuplicator = new
		 * ElementDuplicator(file.getProject(),getGraphicalEditor());
		 * endPointDuplicator.updateAssociatedDiagrams(this);
		 * EditorUtils.setLockmode(graphicalEditor, false);
		 */

		// createPage2();
	}

	public void pageChange(int pageIndex) {
		super.pageChange(pageIndex);

		if (pageIndex == 1) {
			// sourceEditor.update();
			updateSourceEditor();
		}

	}
	
	/*
	 * function generator
	 */

	
	
/*	public static String generateFunction() {
		DataMapperRoot rootDiagram = (DataMapperRoot) graphicalEditor.getDiagram().getElement();
		// String input =
		// rootDiagram.getDataMapperDiagram().getInput().getTreeNode().get(0).getName().split(",")[1];
		// String output =
		// rootDiagram.getDataMapperDiagram().getOutput().getTreeNode().get(0).getName().split(",")[1];

		ArrayList<String> functionsList = new ArrayList<String>();
		functionsList = DataMapperConfigurationGenerator.findForAction(rootDiagram.getInput().getTreeNode());

		String allFunctions = "";

		for (String function : functionsList) {
			allFunctions = allFunctions + "\n" + function;
		}

		
		 * String flagLSInput = "S"; // @param for set List or Single flag in
		 * configuration if(TreeFromAvro.multipleData) flagLSInput = "L";
		 
		// String function =
		// "function map_"+flagLSInput+"_"+input.toLowerCase()+"_"+flagLSInput+"_"+output.toLowerCase()+"( "+"input"
		// +" , "+"output"+" ){ \n "+
		// allActions.toLowerCase()+" \n return output;"+" \n}";
		return allFunctions;
	}*/



	/*
	 * walk through tree structure and return each data field.
	 */
	private String goUpOnOutputTree(TreeNode node) {
		String temp = "";
		if (node.getOutputParent() == null) {
			//temp = goUpOnOutputTree(node.getFieldParent()) + node.getName().split(",")[1] + ".";
			temp = goUpOnOutputTree(node.getFieldParent()) + node.getName() + ".";
		} else {
			return "output.";
		}
		return temp;
	}

	private String goUpOnInputTree(TreeNode node) {
		String temp = "";

		if (node.getInputParent() == null) {
			//temp = goUpOnInputTree(node.getFieldParent()) + node.getName().split(",")[1] + ".";
			temp = goUpOnInputTree(node.getFieldParent()) + node.getName() + ".";
		} else {
			return "input.";
		}
		return temp;
	}

	public void updateSourceEditor() {

//		sourceEditor.update(DataMapperConfigurationGenerator.generateFunction());
//		DataMapperConfiguration temp = DataMapperModelTransformer.getInstance().transform((DataMapperRoot) graphicalEditor.getDiagram().getElement());
//		String temp2="";
//		if (temp != null) {
//			for (Function temp3 : temp.getFunctionList()) {
//				temp2 += temp3.toString();
//			}
//		}
//		sourceEditor.update(temp2);
//		sourceDirty = false;
//		firePropertyChange(PROP_DIRTY);
		
		DataMapperRoot rootDiagram = (DataMapperRoot) DataMapperMultiPageEditor.getGraphicalEditor().getDiagram().getElement();
		String source = DataMapperModelTransformer.getInstance().transform(rootDiagram);
		sourceEditor.update(source);
		sourceDirty = false;
		firePropertyChange(PROP_DIRTY);
	}
	
	
	private void updateAssociatedConfigFile(IProgressMonitor monitor) {
		IEditorInput editorInput = getEditor(0).getEditorInput();
		
		if (editorInput instanceof IFileEditorInput) {
			IFile diagramFile = ((FileEditorInput) editorInput).getFile();
			String configFilePath = diagramFile.getFullPath().toString();
			configFilePath = configFilePath
					.replaceAll(".datamapper_diagram$", ".dmc");
			IFile configFile = diagramFile.getWorkspace().getRoot().getFile(new Path(configFilePath));
			InputStream is = null;
			try {
				DataMapperRoot rootDiagram = (DataMapperRoot) DataMapperMultiPageEditor.getGraphicalEditor().getDiagram().getElement();
				String source = DataMapperModelTransformer.getInstance().transform(rootDiagram);
				if (source == null) {
					log.warn("Could get source");
					return;
				}
				is = new ByteArrayInputStream(source.getBytes());
				if (configFile.exists()) {
					configFile.setContents(is, true, true, monitor);
				} else {
					configFile.create(is, true, monitor);
				}

			} catch (Exception e) {
				log.warn("Could not save file " + configFile);
			} finally {
				if(is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// ignore.
					}
				}
			}
		}
	}	
	
	/**
	 * Traverses input and output trees and generates respective avro schema
	 */
	private void updateAvroSchema() {
		// Get model root of the active DataMapperDiagramEditor
		EObject modelRoot = ((DataMapperDiagramEditor)getEditor(0)).getDiagram().getElement();
		DataMapperRootImpl datamapperRoot = (DataMapperRootImpl) modelRoot;
		
		// Model root of input schema tree
		TreeNodeImpl inputTreeNode = (TreeNodeImpl)((DataMapperRoot)datamapperRoot).getInput().getTreeNode().get(0);
		// Model root of output schema tree
		TreeNodeImpl outputTreeNode = (TreeNodeImpl)((DataMapperRoot)datamapperRoot).getOutput().getTreeNode().get(0);
		
		// This traverses both tree views and returns updated avro schema
		AvroSchemaTransformer avroSchemaTransformer = new AvroSchemaTransformer();
		Schema inputAvroSchema = avroSchemaTransformer.transform(inputTreeNode);
		avroSchemaTransformer = new AvroSchemaTransformer();
		Schema outputAvroSchema = avroSchemaTransformer.transform(outputTreeNode);		
	}
	
	/**
	 * Writes the avro schema to target file
	 * 
	 * @param schemaFile File suffix is either _inputSchema.avsc or _outputSchema.avsc
	 * @param schema Avro schema respective to modified tree
	 */
	private void updateSchemaFile(File schemaFile, Schema schema){
		
	}
	
	public void init(IEditorSite site, IEditorInput editorInput)
	           throws PartInitException {    	
		
		        if (!(editorInput instanceof IFileEditorInput))
		            throw new PartInitException("InvalidInput"); //$NON-NLS-1$     
		       
		       super.init(site, editorInput);
		       String name = editorInput.getName();
		       setTitleOfDataMapperDiagramConfiguration(name);
		    }
		
			private void setTitleOfDataMapperDiagramConfiguration(String name) {
				String title = name.replace("datamapper_diagram","dmc");
				setTitle(title);
			}    

	
	public static DataMapperDiagramEditor getGraphicalEditor() {
		return graphicalEditor;
	}

	@Override
	public void gotoMarker(IMarker marker) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		updateAvroSchema();
		updateAssociatedConfigFile(monitor);
		getEditor(0).doSave(monitor);
	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}
}
