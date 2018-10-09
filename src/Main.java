import com.sun.org.apache.xerces.internal.dom.DeferredElementImpl;
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
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

    private static final  TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static final Transformer transformer = getTransformer();
    private static final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    private static final  DocumentBuilder dBuilder = getDocumentBuilder();

    public static void main(String[] args) throws TransformerException {
        try {
            File fXmlFile = new File("/home/ilya/IdeaProjects/xmlParser/src/test.xml");

            long beforeUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long startTime = System.currentTimeMillis();

            Document doc = dBuilder.parse(fXmlFile);

            doc.getDocumentElement().normalize();

            List<Node> deepestNodes = findDeepestNodes(
                    findElements(doc.getDocumentElement().getChildNodes(),
                            new CopyOnWriteArrayList<>()), new ArrayList<>()
            );

//            createOutput(getFlatDocument(dBuilder, getFieldsMap(deepestNodes)));

            long time = System.currentTimeMillis();
            long afterUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.out.println(String.format("before: %s, in use: %s", beforeUsedMem, afterUsedMem - beforeUsedMem));
            System.out.println(String.format("start: %s, end: %s, longs: %s", startTime, time, time - startTime));

//            dBuilder.parse(new ByteArrayInputStream(((ByteArrayOutputStream) result.outputStream).toByteArray())).getDocumentElement().getChildNodes().item(8);

        } catch (SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void createOutput(Document flatDoc) throws TransformerException {
        DOMSource source = new DOMSource(flatDoc);
        StreamResult resultByte = new StreamResult(new ByteArrayOutputStream());
//        StreamResult resultFile = new StreamResult(new File("/home/ilya/IdeaProjects/xmlParser/src/file.xml"));

        transformer.transform(source, resultByte);
//        transformer.transform(source, resultFile);
    }

    private static Document getFlatDocument(DocumentBuilder dBuilder, Map<String, Node> fields) {
        Document flatDoc = dBuilder.newDocument();
        flatDoc.appendChild(flatDoc.createElement("flatFields"));
        Element rootElement = flatDoc.getDocumentElement();

        for (Map.Entry<String, Node> nodeEntry : doSort(fields)) {
            rootElement.appendChild(flatDoc.importNode(nodeEntry.getValue(), true));
        }

        return flatDoc;
    }

    private static Map<String, Node> getFieldsMap(List<Node> deepestNodes) {
        Map<String, Node> fields = new HashMap<>();

        for (Node node : deepestNodes) {
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
        return fields;
    }

    private static List<Node> findDeepestNodes(List<Node> nodes, List<Node> deepestNodes) {
        ListIterator<Node> nodeIterator = nodes.listIterator();

        goNext:
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();

            if (checkForOneParent(nodeIterator, node)) {
                 nodeIterator = getNewNodeListIterator(nodes, nodeIterator);

                break goNext;
            }

            if (checkForTextField(node)) {
                deepestNodes.add(node);
            } else {
                findElements(node.getChildNodes(), nodes);
            }

             nodeIterator = getNewNodeListIterator(nodes, nodeIterator);
        }

        return deepestNodes;
    }

    private static boolean checkForTextField(Node node) {
        return node.getChildNodes().getLength() <= 1 && node.getFirstChild().getNodeType() == Node.TEXT_NODE;
    }

    private static boolean checkForOneParent(ListIterator<Node> nodeIterator, Node node) {
        return nodeIterator.previousIndex() > 0 && node.getParentNode().equals(nodeIterator.previous().getParentNode());
    }

    private static ListIterator<Node> getNewNodeListIterator(List<Node> nodes, ListIterator<Node> nodeIterator) {
        nodes.remove(nodeIterator.nextIndex() - 1);
        return nodes.listIterator();
    }

    private static List<Node> findElements(NodeList nodeList, List<Node> nodes) {
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                nodes.add(nodeList.item(i));
            }
        }

        return nodes;
    }

    private static boolean findParent(Node node, Map<String, Node> fields) {
        return fields.get(getFieldName(node)) != null;
    }

    private static String getFieldName(Node node) {
        final String name = node.getNodeName();
        final int hash = node.hashCode();
        return  name + hash;
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

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {

        }

        return null;
    }

    private static Transformer getTransformer() {
        try {
            return transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {

        }

        return null;
    }
}
