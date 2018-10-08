import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
import com.sun.org.apache.xpath.internal.operations.Bool;
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
		long startTime = System.currentTimeMillis();
		try {
			File fXmlFile = new File("/Users/ilya/IdeaProjects/xmlParser/src/test.xml");
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
				Boolean exist = false;

			    while (!exist && nodeParent != null) {
					exist = findParent(nodeParent, fields);
			        nodeParent = nodeParent.getParentNode();
                }
                if (nodeParent == null && !exist) {
					fields.put(getFieldName(node.getParentNode()), node.getParentNode());
				}

            }

            Document flatDoc = dBuilder.newDocument();
            Element root = flatDoc.createElement("flatFields");
            flatDoc.appendChild(root);

            for (Map.Entry<String, Node> nodeEntry: doSort(fields)) {
                flatDoc.getDocumentElement().appendChild(flatDoc.importNode(nodeEntry.getValue(), true));
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(flatDoc);
            StreamResult resultByte = new StreamResult(new ByteArrayOutputStream());
            StreamResult resultFile = new StreamResult(new File("/Users/ilya/IdeaProjects/xmlParser/src/file.xml"));

            transformer.transform(source, resultByte);
            transformer.transform(source, resultFile);

            long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
            long maxMem=Runtime.getRuntime().maxMemory();

			long time = System.currentTimeMillis();
            System.out.println(String.format("before: %s, after: %s, max: %s", afterUsedMem, beforeUsedMem, maxMem));
			System.out.println(String.format("start: %s, end: %s, longs: %s", startTime, time, time - startTime));

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
        return fields.containsKey(getFieldName(node));
    }

	private static String getFieldName(Node node) {
		return String.format("%s%s", node.getNodeName(), node.getTextContent());
	}

	private static List<HashMap.Entry<String, Node>> doSort(Map<String, Node> fields) {
    	List<HashMap.Entry<String, Node>> list = new ArrayList(fields.entrySet());
    	Collections.sort(list, new Comparator<HashMap.Entry<String, Node>>() {
			@Override
			public int compare(HashMap.Entry<String, Node> o1, HashMap.Entry<String, Node> o2) {
				return ((DeferredElementImpl) o1.getValue()).getNodeIndex()
								- ((DeferredElementImpl) o2.getValue()).getNodeIndex();
			}
		});

    	return list;
	}
}
