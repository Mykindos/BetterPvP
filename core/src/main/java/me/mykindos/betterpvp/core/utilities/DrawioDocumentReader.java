package me.mykindos.betterpvp.core.utilities;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Inflater;

public final class DrawioDocumentReader {

    private DrawioDocumentReader() {
    }

    public static Document parse(InputStream in) throws Exception {
        byte[] bytes = in.readAllBytes();
        Document outer = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(bytes));

        if (outer.getDocumentElement().getTagName().equals("mxfile")) {
            Element diagram = (Element) outer.getElementsByTagName("diagram").item(0);

            NodeList children = diagram.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Document unwrapped = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                    unwrapped.appendChild(unwrapped.importNode(children.item(i), true));
                    return unwrapped;
                }
            }

            String xml = inflate(diagram.getTextContent().trim());
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        }

        return outer;
    }

    public static String drawioId(Element wrapper) {
        return firstAttribute(wrapper, "id", "node_id");
    }

    public static String firstAttribute(Element element, String... names) {
        for (String name : names) {
            String value = element.getAttribute(name);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String inflate(String content) throws Exception {
        String trimmed = content.trim();
        if (trimmed.startsWith("<")) {
            return trimmed;
        }

        byte[] compressed = Base64.getDecoder().decode(trimmed);

        String result = tryInflate(compressed, true);
        if (!result.isEmpty()) {
            return result;
        }

        result = tryInflate(compressed, false);
        if (!result.isEmpty()) {
            return result;
        }

        throw new IllegalArgumentException("Could not decompress draw.io diagram");
    }

    private static String tryInflate(byte[] compressed, boolean nowrap) throws Exception {
        Inflater inflater = new Inflater(nowrap);
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while (!inflater.finished() && !inflater.needsInput()) {
            n = inflater.inflate(buf);
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
        inflater.end();
        return out.toString(StandardCharsets.UTF_8);
    }
}
