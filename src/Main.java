import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

    public static void main(String[] args) {
        long beforeUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		try {
			File fXmlFile = new File("/home/ilya/IdeaProjects/xmlParser/src/test.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			Node newLineNode = nodeList.item(0);

			List<Node> nodes = new CopyOnWriteArrayList<>();
			List<Node> deepestNodes = new ArrayList<>();

			findElemens(nodeList, nodes);

			ListIterator<Node> nodeIterator = nodes.listIterator();

			while (nodeIterator.hasNext()) {
				Node node = nodeIterator.next();
				if (node.getChildNodes().getLength() <=1
						&& node.getFirstChild().getNodeType() == Node.TEXT_NODE) {
					deepestNodes.add(node);
					nodes.remove(nodeIterator.nextIndex() - 1);
                    nodeIterator = nodes.listIterator();
				} else {
					findElemens(node.getChildNodes(), nodes);
					nodes.remove(nodeIterator.nextIndex() - 1);
					nodeIterator = nodes.listIterator();
				}
			}

			Map<String, Node> fields = new HashMap<>();

			for (Node node: deepestNodes) {
			    Node nodeParent = node.getParentNode();
			    while (findParent(nodeParent, fields)) {
			        nodeParent = nodeParent.getParentNode();
                }
                fields.put(nodeParent.getNodeName(), nodeParent);
            }

            Document flatDoc = dBuilder.newDocument();
            Element root = flatDoc.createElement("flatFields");
            flatDoc.appendChild(root);

            for (Map.Entry<String, Node> nodeEntry: fields.entrySet()) {
                flatDoc.getDocumentElement().appendChild(flatDoc.importNode(nodeEntry.getValue(), true));
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(flatDoc);
            StreamResult resultByte = new StreamResult(new ByteArrayOutputStream());
            StreamResult resultFile = new StreamResult(new File("/home/ilya/IdeaProjects/xmlParser/src/file.xml"));

            transformer.transform(source, resultByte);
            transformer.transform(source, resultFile);

            long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            long maxMem=Runtime.getRuntime().maxMemory();

            System.out.println(String.format("before: %s, after: %s, max: %s", afterUsedMem, beforeUsedMem, maxMem));

//            dBuilder.parse(new ByteArrayInputStream(((ByteArrayOutputStream) result.outputStream).toByteArray())).getDocumentElement().getChildNodes().item(8);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {

        } catch (TransformerException e) {

        }
	}

	private static void findElemens(NodeList nodeList, List<Node> nodes) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
				nodes.add(nodeList.item(i));
			}
		}
	}

	private static boolean findParent(Node node, Map<String, Node> fields) {
        node = node.getParentNode();
        return fields.containsKey(node.getNodeName());
    }
}
