/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.developerstudio.eclipse.carbonserver.base.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.server.generic.core.internal.GenericServer;
import org.eclipse.jst.server.generic.servertype.definition.ServerRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.wso2.developerstudio.eclipse.carbonserver.base.Activator;
import org.wso2.developerstudio.eclipse.carbonserver.base.authentication.AuthenticationAdminStub;
import org.wso2.developerstudio.eclipse.carbonserver.base.manager.CarbonServerManager;
import org.wso2.developerstudio.eclipse.logging.core.IDeveloperStudioLog;
import org.wso2.developerstudio.eclipse.logging.core.Logger;
import org.wso2.developerstudio.eclipse.platform.ui.utils.SSLUtils;
import org.wso2.developerstudio.eclipse.utils.file.FileUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class CarbonServerUtils {
	private static final String AXIS2_XML = "axis2.xml";
	private static final String AXIS2 = "axis2";
	private static final String TRANSPORTS_XML = "transports.xml";
	private static final String SERVER_WEB_CONTEXT_ROOT = "/:Server/:WebContextRoot";
	private static final String RAWTYPES = "rawtypes";
	private static final String WEB_CONTEXT_ROOT = "http://wso2.org/projects/carbon/carbon.xml";
	private static final String CARBON_XML = "carbon.xml";
	private static final String CONF = "conf";
	private static final String REPOSITORY = "repository";
	private static IDeveloperStudioLog log=Logger.getLog(Activator.PLUGIN_ID);
	private static String servicePath;

	public static String createSessionCookie(String serverURL, String username, String pwd) throws Exception{
		AuthenticationAdminStub authenticationStub;
		URL url = new URL(serverURL);
		authenticationStub = new AuthenticationAdminStub(getURL(serverURL) + "/"+getServicePath()+"/AuthenticationAdmin");
		SSLUtils.setSSLProtocolHandler(authenticationStub);
		authenticationStub._getServiceClient().getOptions().setManageSession(true);
		if (authenticationStub.login(username, pwd, url.getHost())){
			ServiceContext serviceContext = authenticationStub._getServiceClient().getLastOperationContext().getServiceContext();
			String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
			return sessionCookie;
		}else{
			return null;
		}
	}
	
	public static String getURL(String serverURL) throws MalformedURLException{
		URL url = new URL(serverURL);
		String endPart = url.getFile();
		String validURL = "";
		if(endPart.endsWith("/carbon") || endPart.endsWith("/"+getServicePath()) || endPart.endsWith("/registry")){
			String[] stringParts = serverURL.split("/");
			int length = stringParts[stringParts.length - 1].length();
//			for (int i = 0; i < stringParts.length - 1; i++) {
			validURL = serverURL.substring(0, serverURL.length() - length);
//			}
		}else{
			validURL = serverURL;
		}
		return validURL;
		
	}
	public static ServerPort[] getServerPorts(String serverHome){
		String transportsXml = FileUtils.addNodesToPath(serverHome, new String[]{CONF,TRANSPORTS_XML});
		XPathFactory factory = XPathFactory.newInstance();
		File xmlDocument = new File(transportsXml);
		ServerPort[] serverPorts=new ServerPort[2];
    	try {
			InputSource inputSource =  new InputSource(new FileInputStream(xmlDocument));
	    	XPath xPath=factory.newXPath();
	    	XPathExpression  xPathExpression=xPath.compile("/transports/transport[@name='http']/parameter[@name='port']");
	    	String evaluate = xPathExpression.evaluate(inputSource);
	    	serverPorts[0]=new ServerPort("server","",Integer.parseInt(evaluate),"http");
			inputSource =  new InputSource(new FileInputStream(xmlDocument));
	    	xPathExpression=xPath.compile("/transports/transport[@name='https']/parameter[@name='port']");
	    	evaluate = xPathExpression.evaluate(inputSource);
	    	serverPorts[1]=new ServerPort("server","",Integer.parseInt(evaluate),"https");
	    } catch (FileNotFoundException e) {
			log.error(e);
		} catch (XPathExpressionException e) {
			log.error(e);
		}
		return serverPorts;
	}
	
	
	public static String getWebContextRoot(IServer server){
		String transportsXml = FileUtils.addNodesToPath(CarbonServerManager
				.getServerHome(server).toOSString(), new String[] { REPOSITORY,CONF, CARBON_XML });
		String webContextRoot = null;
		if (transportsXml != null) {
			File xmlDocument = new File(transportsXml);
			if (xmlDocument != null) {
				webContextRoot = null;
				NamespaceContext ctx = new NamespaceContext() {
					public String getNamespaceURI(String prefix) {
						return WEB_CONTEXT_ROOT;
					}

					public String getPrefix(String arg0) {
						return null;
					}

					@SuppressWarnings(RAWTYPES)
					public Iterator getPrefixes(String arg0) {
						return null;
					}
				};
				try {
					InputSource inputSource = null;
					try {
						inputSource = new InputSource(new FileInputStream(
								xmlDocument));
					} catch (FileNotFoundException e) {
						log.error(
								"Could not found the file 'carbon.xml' ",
								e);
					}

					if (inputSource != null) {
						XPathFactory factory = XPathFactory.newInstance();
						XPath xPath = factory.newXPath();
						xPath.setNamespaceContext(ctx);
						XPathExpression xPathExpression = xPath
								.compile(SERVER_WEB_CONTEXT_ROOT);
						webContextRoot = xPathExpression.evaluate(inputSource);
						webContextRoot = webContextRoot.equals("/") ? ""
								: webContextRoot;
					}
				} catch (XPathExpressionException e) {
					log.error(
							"Exception in XPath expression evaluation",
							e);
				}
			}
		}
		return webContextRoot;
	}
	
	public static void setServicePath(IServer server) {
		String axis2Xml= FileUtils.addNodesToPath(CarbonServerManager
				.getServerHome(server).toOSString(), new String[] { REPOSITORY,CONF,AXIS2,AXIS2_XML });
		XPathFactory factory = XPathFactory.newInstance();
		
		File xmlDocument = new File(axis2Xml);
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(xmlDocument);
			XPath xPath=factory.newXPath();
			Node servicePathNode = (Node)xPath.evaluate("/axisconfig/parameter[@name='servicePath']", document, XPathConstants.NODE);
			servicePath = servicePathNode.getTextContent();
		} catch (ParserConfigurationException e) {
			log.error(e);
		} catch (SAXException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} catch (XPathExpressionException e) {
			log.error(e);
		}

	}
	
	public static void setRemoteServicePath(String remoteServicepath) {
		servicePath = remoteServicepath;
	}
	
	public static String getServicePath() {
		return servicePath;
	}

//	public static void setTrustoreProperties(IServer server){
//		String transportsXml = FileUtils.addNodesToPath(WSASServerManager.getServerHome(server).toOSString(), new String[]{"conf","server.xml"});
//		XPathFactory factory = XPathFactory.newInstance();
//		File xmlDocument = new File(transportsXml);
//    	try {
//			InputSource inputSource =  new InputSource(new FileInputStream(xmlDocument));
//	    	XPath xPath=factory.newXPath();
//	    	XPathExpression  xPathExpression=xPath.compile("/Server/Security/KeyStore/Location");
//	    	String evaluate = xPathExpression.evaluate(inputSource);
//	    	String trustoreLocation = resolveProperties(server,evaluate);
//			inputSource =  new InputSource(new FileInputStream(xmlDocument));
//	    	xPathExpression=xPath.compile("/Server/Security/KeyStore/Password");
//	    	evaluate = xPathExpression.evaluate(inputSource);
//	    	String trustStorePassword=resolveProperties(server,evaluate);
//	    	System.setProperty("javax.net.ssl.trustStore", trustoreLocation);
//		    System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
//	    } catch (FileNotFoundException e) {
//			log.error(e);
//		} catch (XPathExpressionException e) {
//			log.error(e);
//		}
//	}
	
	public static ServerPort[] getServerPorts(IServer server){
    	//return getServerPorts(CommonOperations.getWSASHome(server).toOSString());
		return server.getServerPorts(new NullProgressMonitor());
	}
	
//	public static String getRepositoryPath(String serverXmlPath){
//		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder docBuilder;
//        String nodeValue="";
//		try {
//			docBuilder = docFactory.newDocumentBuilder();
//
//	        Document doc = docBuilder.parse(serverXmlPath);
//	
//	        NodeList nodeList = doc.getElementsByTagName("RepositoryLocation");
//	        Node node = nodeList.item(0);
//	        nodeValue = node.getFirstChild().getNodeValue();
//	        
//		} catch (ParserConfigurationException e) {
//			log.error(e);
//		} catch (SAXException e) {
//			log.error(e);
//		} catch (IOException e) {
//			log.error(e);
//		} catch (TransformerFactoryConfigurationError e) {
//			log.error(e);
//		} 
//		return nodeValue;
//	}
//	
//	public static boolean updateAndSaveCarbonXml(String serverXmlPath, String repoPath, String newServerXmlPath,IServer server){
//		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder docBuilder;
//		try {
//			docBuilder = docFactory.newDocumentBuilder();
//
//	        Document doc = docBuilder.parse(serverXmlPath);
//	        
//	        NodeList nodeList = doc.getElementsByTagName("RepositoryLocation");
//	        Node node = nodeList.item(0);
//	        node.getFirstChild().setNodeValue(repoPath);
//	        Transformer t = TransformerFactory.newInstance().newTransformer();
//	        File confPath = new File((new File(newServerXmlPath)).getParent());
//	        if (!confPath.exists()) confPath.mkdirs();
//	        Result result = new StreamResult(new File(newServerXmlPath));
//	        Source source = new DOMSource(doc);
//	        t.transform(source, result);
//	        return true;
//		} catch (ParserConfigurationException e) {
//			log.error(e);
//		} catch (SAXException e) {
//			log.error(e);
//		} catch (IOException e) {
//			log.error(e);
//		} catch (TransformerConfigurationException e) {
//			log.error(e);
//		} catch (TransformerFactoryConfigurationError e) {
//			log.error(e);
//		} catch (TransformerException e) {
//			log.error(e);
//		}
//		return false;
//	}
//	
//	public static String getServerXmlPathFromLocalWorkspaceRepo(String workspaceRepo){
//		return FileUtils.addNodesToPath(workspaceRepo,new String[]{"conf","carbon.xml"});
//	}
//	
//	public static String getTransportsXmlPathFromLocalWorkspaceRepo(String workspaceRepo){
//		return FileUtils.addNodesToPath(workspaceRepo,new String[]{"conf","transports.xml"});
//	}
//	
	public static String resolveProperties(IServer server, String property){
		String propertyValue;
//		if (CarbonServerUtils.getServerConfigMapValue(server,property)!=null){
//			return CarbonServerUtils.getServerConfigMapValue(server,property).toString();
//		}
		GenericServer gserver = (GenericServer)server.loadAdapter(ServerDelegate.class, null);
		if (gserver==null || gserver.getServerDefinition()==null || gserver.getServerDefinition().getResolver()==null) return null;
		if (!property.startsWith("${"))
			property="${"+property+"}";
    	ServerRuntime serverDefinition = gserver.getServerDefinition();
    	propertyValue = serverDefinition.getResolver().resolveProperties(property);
    	return propertyValue;
	}
//	
//	public static boolean updateAndSaveTransportsXml(String transportsXml, String newTransportsXmlPath, IServer server){
//		XPathFactory factory = XPathFactory.newInstance();
//		ServerPort[] serverPorts=WSASServerManager.getServerPorts(server);
//    	try {
//    		File xmlDocument = new File(transportsXml);
//    		DocumentBuilder builder =
//                DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            Document document = builder.parse(xmlDocument);
//	    	XPath xPath=factory.newXPath();
//	    	Node httpNode = (Node)xPath.evaluate("/transports/transport[@name='http']/parameter[@name='port']", document, XPathConstants.NODE);
//	    	Node httpsNode = (Node)xPath.evaluate("/transports/transport[@name='https']/parameter[@name='port']", document, XPathConstants.NODE);
//	    	for(ServerPort serverPort:serverPorts){
//	    		if (serverPort.getProtocol().equalsIgnoreCase("http")) httpNode.setTextContent(Integer.toString(serverPort.getPort()));
//	    		if (serverPort.getProtocol().equalsIgnoreCase("https")) httpsNode.setTextContent(Integer.toString(serverPort.getPort()));
//	    	}
//	        Transformer t = TransformerFactory.newInstance().newTransformer();
//	        File confPath = new File((new File(newTransportsXmlPath)).getParent());
//	        if (!confPath.exists()) confPath.mkdirs();
//	    	Result result = new StreamResult(new File(newTransportsXmlPath));
//	        Source source = new DOMSource(document);
//	        t.transform(source, result);
//			return true;
//	    } catch (FileNotFoundException e) {
//			log.error(e);
//	    } catch (XPathExpressionException e) {
//			log.error(e);
//	    } catch (ParserConfigurationException e) {
//			log.error(e);
//		} catch (SAXException e) {
//			log.error(e);
//		} catch (IOException e) {
//			log.error(e);
//		} catch (TransformerConfigurationException e) {
//			log.error(e);
//		} catch (TransformerFactoryConfigurationError e) {
//			log.error(e);
//		} catch (TransformerException e) {
//			log.error(e);
//		}
//    	return false;
//	}
//	
//	public static boolean updateTransportXML(IServer server){
//		IPath serverHome = WSASServerManager.getServerHome(server);
//		String serverLocalWorkspacePath = WSASServerManager.getServerLocalWorkspacePath(server);
//		return CarbonServerUtils.updateAndSaveTransportsXml(FileUtils.addNodesToPath(serverHome.toOSString(),new String[]{"conf","transports.xml"}), CarbonServerUtils.getTransportsXmlPathFromLocalWorkspaceRepo(serverLocalWorkspacePath),server); 
//	}
//	
//	public static boolean isHotUpdateEnabled(IServer server){
//		String axis2Xml=getAxis2FilePath(server);
//		XPathFactory factory = XPathFactory.newInstance();
//    	try {
//    		File xmlDocument = new File(axis2Xml);
//    		DocumentBuilder builder =
//                DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            Document document = builder.parse(xmlDocument);
//	    	XPath xPath=factory.newXPath();
//	    	Node httpNode = (Node)xPath.evaluate("/axisconfig/parameter[@name='hotupdate']", document, XPathConstants.NODE);
//	    	return httpNode.getTextContent().toString().equalsIgnoreCase("true");
//	    } catch (FileNotFoundException e) {
//			log.error(e);
//	    } catch (XPathExpressionException e) {
//			log.error(e);
//	    } catch (ParserConfigurationException e) {
//			log.error(e);
//		} catch (SAXException e) {
//			log.error(e);
//		} catch (IOException e) {
//			log.error(e);
//		} catch (TransformerFactoryConfigurationError e) {
//			log.error(e);
//		}
//    	return false;
//	}
	
	public static IPath getCarbonHome(IServer server){
    	return new Path(resolveProperties(server, "wsas.home"));
	}
	
	public static IPath getCarbonServerHome(IServer server){
    	return new Path(resolveProperties(server, "carbon.home"));
	}
	
	public static String getAxis2FilePath(IServer server){
		IPath serverHome = getCarbonHome(server);	
		String axis2Xml = FileUtils.addNodesToPath(serverHome.toOSString(),
				new String[] { CONF, AXIS2_XML });
		return axis2Xml;
	}
//	
//	public static boolean setHotUpdateEnabled(IServer server,boolean enabled){
//		if (isHotUpdateEnabled(server)==enabled) return true;
//		String axis2Xml=getAxis2FilePath(server);
//		XPathFactory factory = XPathFactory.newInstance();
//    	try {
//    		File xmlDocument = new File(axis2Xml);
//    		DocumentBuilder builder =
//                DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            Document document = builder.parse(xmlDocument);
//	    	XPath xPath=factory.newXPath();
//	    	Node httpNode = (Node)xPath.evaluate("/axisconfig/parameter[@name='hotupdate']", document, XPathConstants.NODE);
//	    	httpNode.setTextContent(enabled ? "true":"false");
//	        Transformer t = TransformerFactory.newInstance().newTransformer();
//	        File confPath = new File((new File(axis2Xml)).getParent());
//	        if (!confPath.exists()) confPath.mkdirs();
//	    	Result result = new StreamResult(new File(axis2Xml));
//	        Source source = new DOMSource(document);
//	        t.transform(source, result);
//			return true;
//	    } catch (FileNotFoundException e) {
//			log.error(e);
//	    } catch (XPathExpressionException e) {
//			log.error(e);
//	    } catch (ParserConfigurationException e) {
//			log.error(e);
//		} catch (SAXException e) {
//			log.error(e);
//		} catch (IOException e) {
//			log.error(e);
//		} catch (TransformerConfigurationException e) {
//			log.error(e);
//		} catch (TransformerFactoryConfigurationError e) {
//			log.error(e);
//		} catch (TransformerException e) {
//			log.error(e);
//		}
//    	return false;
//	}
//	
//	public static String getServerConfigMapValue(IServer server, String key){
//		String loaded="loaded";
//		GenericServer gserver = (GenericServer) server.getAdapter(GenericServer.class);
//		if (gserver==null ||gserver.getServerInstanceProperties()==null) return null;
//		if (gserver.getServerInstanceProperties().get(loaded)==null){
//			loadServerInstanceProperties(server);
//		}
//		Object object = gserver.getServerInstanceProperties().get(key);
//		if (object!=null)
//			return object.toString();
//		return null;
//	}
//	
//	public static void setServerConfigMapValue(IServer server, String key, String value){
//		GenericServer gserver = (GenericServer) server.getAdapter(GenericServer.class);
//		if (gserver!=null){
//			gserver.getServerInstanceProperties().put(key,value);
//			saveServerInstanceProperties(server);
//		}
//	}
//	
//	private static void loadServerInstanceProperties(IServer server){
//		GenericServer gserver = (GenericServer) server.getAdapter(GenericServer.class);
//		if (gserver==null) return;
//		try {
//			String serverLocalWorkspacePath = WSASServerManager.getServerLocalWorkspacePath(server);
//			String objConfigPath = FileUtils.addNodesToPath(serverLocalWorkspacePath, new String[]{"conf","config"});
//			if (new File(objConfigPath).exists()){
//				FileInputStream f_in = new FileInputStream(objConfigPath);
//				ObjectInputStream obj_in = new ObjectInputStream (f_in);
//				Map obj = (Map)obj_in.readObject();
//				gserver.getServerInstanceProperties().putAll(obj);
//			}
//		} catch (IOException e) {
//			log.error(e);
//		} catch (ClassNotFoundException e) {
//			log.error(e);
//		}
//		gserver.getServerInstanceProperties().put("loaded", "true");
//	}
//	
//	private static void saveServerInstanceProperties(IServer server){
//		GenericServer gserver = (GenericServer) server.getAdapter(GenericServer.class);
//		if (gserver==null) return;
//		try {
//			String serverLocalWorkspacePath = WSASServerManager.getServerLocalWorkspacePath(server);
//			String objConfigPath = FileUtils.addNodesToPath(serverLocalWorkspacePath, new String[]{"conf","config"});
//			FileOutputStream f_out = new FileOutputStream(objConfigPath);
//			ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
//			obj_out.writeObject ( gserver.getServerInstanceProperties());
//		} catch (IOException e) {
//			log.error(e);
//		}
//	}

}
