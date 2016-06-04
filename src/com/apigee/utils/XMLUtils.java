package com.apigee.utils;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.apigee.proxywriter.GenerateProxy;

import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XMLUtils {

	private static final Logger LOGGER = Logger.getLogger(XMLUtils.class.getName());
	private static final ConsoleHandler handler = new ConsoleHandler();
	
	static {
		LOGGER.setLevel(Level.WARNING);		
		// PUBLISH this level
		handler.setLevel(Level.WARNING);
		LOGGER.addHandler(handler);
	}

	private DocumentBuilder builder;
	
	private static final Set<String> blacklist = new HashSet<String>(Arrays.asList(
		     new String[] {"http://schemas.xmlsoap.org/wsdl/soap/",
		    		 "http://schemas.xmlsoap.org/wsdl/", 
		    		 "http://schemas.xmlsoap.org/ws/2003/05/partner-link/",
		    		 "http://www.w3.org/2001/XMLSchema",
		    		 "http://schemas.xmlsoap.org/soap/encoding/"}
		));	

	private static String elementName = ":{local-name()}";

	public XMLUtils() throws Exception {
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	public Document readXML(String filePath) throws Exception {

		LOGGER.entering(XMLUtils.class.getName(), new Object() {
		}.getClass().getEnclosingMethod().getName());

		try {
			File f = new File(filePath);
			return builder.parse(f);
		} catch (SAXParseException spe) {
			// Error generated by the parser
			LOGGER.severe("\n** Parsing error" + ", line " + spe.getLineNumber() + ", uri " + spe.getSystemId());
			LOGGER.severe("  " + spe.getMessage());
			throw spe;
		} catch (SAXException sxe) {
			LOGGER.severe(sxe.getMessage());
			throw sxe;
		} catch (IOException ioe) {
			LOGGER.severe(ioe.getMessage());
			throw ioe;
		}
	}

	public void writeXML(Document document, String filePath) throws Exception {

		LOGGER.entering(XMLUtils.class.getName(), new Object() {
		}.getClass().getEnclosingMethod().getName());

		try {
			document.setXmlStandalone(true);
			// Use a Transformer for output
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			DOMSource source = new DOMSource(document);
			File f = new File(filePath);
			FileOutputStream fos = new FileOutputStream(f, false);
			StreamResult result = new StreamResult(fos);
			transformer.transform(source, result);
		} catch (IOException ioe) {
			LOGGER.severe(ioe.getMessage());
			throw ioe;
		} catch (TransformerConfigurationException tce) {
			LOGGER.severe("* Transformer Factory error");
			LOGGER.severe(" " + tce.getMessage());
			throw tce;
		} catch (TransformerException te) {
			LOGGER.severe("* Transformation error");
			LOGGER.severe(" " + te.getMessage());
			throw te;
		}
	}

	public Document getXMLFromString(String xml) throws Exception {

		LOGGER.entering(XMLUtils.class.getName(), new Object() {
		}.getClass().getEnclosingMethod().getName());

		try {
			InputSource is = new InputSource(new StringReader(xml));
			return builder.parse(is);
		} catch (SAXException | IOException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		}
	}
	
	private String extractElement (String fullElementName) {
		if (fullElementName.indexOf(":") != -1) {
			String elements [] = fullElementName.split(":");
			return elements[1];
		} else {
			return fullElementName;
		}
	}

	public List<String> getElementList(String xml) throws Exception {

		LOGGER.entering(XMLUtils.class.getName(), new Object() {
		}.getClass().getEnclosingMethod().getName());

		List<String> elementList = new ArrayList<String>();
		try {
			Document doc = builder.parse(new InputSource(new StringReader(xml)));

			XPathFactory xpf = XPathFactory.newInstance();
			XPath xp = xpf.newXPath();

			NodeList nodes = (NodeList) xp.evaluate("//@* | //*[not(*)]", doc, XPathConstants.NODESET);

			for (int i = 0, len = nodes.getLength(); i < len; i++) {
				elementList.add(extractElement(nodes.item(i).getNodeName()));
			}
			return elementList;
		} catch (SAXException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		} catch (IOException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		} catch (XPathExpressionException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		}
	}

	public KeyValue<String, String> replacePlaceHolders(String xml) throws Exception {

		LOGGER.entering(XMLUtils.class.getName(), new Object() {
		}.getClass().getEnclosingMethod().getName());

		KeyValue<String, String> keyValue;
		try {
			Document doc = builder.parse(new InputSource(new StringReader(xml)));

			XPathFactory xpf = XPathFactory.newInstance();
			XPath xp = xpf.newXPath();
			NodeList nodes = (NodeList) xp.evaluate("//@* | //*[not(*)]", doc, XPathConstants.NODESET);

			for (int i = 0, len = nodes.getLength(); i < len; i++) {
				Node item = nodes.item(i);
				item.setTextContent("{" + extractElement(item.getNodeName()) + "}");
			}

			Node envelope = doc.getDocumentElement();
			NodeList nodeList = envelope.getChildNodes();
			Node body = null;
			Node temp = null;
			for (int i = 0; i < nodeList.getLength(); i++) {
				temp = nodeList.item(i);
				if (temp.getNodeName().indexOf(":Body") != -1) {
					body = temp;
					break;
				}
			}

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter fullSoapWriter = new StringWriter();
			StringWriter bodySoapWriter = new StringWriter();

			transformer.transform(new DOMSource(doc), new StreamResult(fullSoapWriter));
			String fullSoap = fullSoapWriter.getBuffer().toString().replaceAll("\n|\r", "");

			transformer.transform(new DOMSource(getFirstChildElement(body)), new StreamResult(bodySoapWriter));
			String bodySoap = bodySoapWriter.getBuffer().toString().replaceAll("\n|\r", "");
			String bodyJson = XML.toJSONObject(bodySoap).toString();

			keyValue = new KeyValue<String, String>(fullSoap, bodyJson);
			return keyValue;
		} catch (ParserConfigurationException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		} catch (SAXException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		} catch (IOException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		} catch (XPathExpressionException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		} catch (TransformerException e) {
			LOGGER.severe(e.getMessage());
			throw e;
		}
	}

	/**
	 * Gets the first child element of a node.
	 * 
	 * @param node
	 *            the node to get the child from
	 * @return the first element child of {@code node} or {@code null} if none
	 * @throws NullPointerException
	 *             if {@code node} is {@code null}
	 */
	public Element getFirstChildElement(Node node) throws Exception {

		LOGGER.entering(XMLUtils.class.getName(), new Object() {
		}.getClass().getEnclosingMethod().getName());

		node = node.getFirstChild();
		while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
			node = node.getNextSibling();
		}
		return (Element) node;
	}

	/**
	 * 
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	public Document cloneDocument(Document doc) throws Exception {
		Document clonedDoc = builder.newDocument();
		clonedDoc.appendChild(clonedDoc.importNode(doc.getDocumentElement(), true));
		return clonedDoc;
	}
	
	
	public void generateXSLT(String xsltTemplate, String target, String operationName, String prefix, String namespaceUri) throws Exception{
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(new File(xsltTemplate));
		
		Node stylesheet = document.getDocumentElement();
		for (Map.Entry<String, String> entry : GenerateProxy.namespace.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!blacklist.contains(value)) {
				((Element) stylesheet).setAttribute("xmlns:"+key, value);
			}
		}
		
		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		// there's no default implementation for NamespaceContext
		xp.setNamespaceContext(new NamespaceContext() {
			
			@Override
			public Iterator getPrefixes(String namespaceURI) {
		        throw new UnsupportedOperationException();
			}
			
			@Override
			public String getPrefix(String namespaceURI) {
		        throw new UnsupportedOperationException();
			}
			
			@Override
			public String getNamespaceURI(String prefix) {
		        if (prefix == null) throw new NullPointerException("Null prefix");
		        else if ("xsl".equals(prefix)) return "http://www.w3.org/1999/XSL/Transform";
		        else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
		        return XMLConstants.NULL_NS_URI;
			}
		});		

		NodeList nodes = (NodeList) xp.evaluate("/xsl:stylesheet/xsl:template/xsl:element", document, XPathConstants.NODESET);
		Node element = nodes.item(0);

		NamedNodeMap attr = element.getAttributes();
		Node nodeAttr = attr.getNamedItem("name");
		nodeAttr.setNodeValue(prefix+elementName);
		
		Node nspace = document.createElementNS("http://www.w3.org/1999/XSL/Transform", "xsl:namespace");
		((Element) nspace).setAttribute("name", prefix);
		((Element) nspace).setAttribute("select", "'"+namespaceUri+"'");
		
		element.insertBefore(nspace, element.getFirstChild());
		
		writeXML(document, target+operationName+"-add-namespace.xslt");
		
	}
}
