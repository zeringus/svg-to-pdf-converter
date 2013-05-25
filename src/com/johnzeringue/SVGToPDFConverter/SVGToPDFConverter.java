package com.johnzeringue.SVGToPDFConverter;

import com.johnzeringue.SVGToPDFConverter.ElementHandler.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Stack;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The DefaultHandler that is actually used by the XMLReader for converting SVGs
 * to PDFs. However, this functionality is not yet complete.
 *
 * @author John Zeringue
 * @version 04/03/2013
 */
public class SVGToPDFConverter extends DefaultHandler {

    // The PDF version used in this implementation
    public final static double PDF_VERSION = 1.7;
    // The formatting string for a PDF reference
    public final static String PDF_REFERENCE_FORMAT = "%d 0 R";
    // The formatting string for a PDF object
    public final static String PDF_OBJECT_FORMAT = ""
            + "%d 0 obj\n"
            + "%s\n"
            + "endobj\n\n";
    // The formatting string for a PDF pages object
    public final static String PDF_PAGES_FORMAT = ""
            + "  <</Type /Pages\n"
            + "    /Kids [" + PDF_REFERENCE_FORMAT + "]>>";
    // The formatting string for a PDF catalog object
    public final static String PDF_CATALOG_FORMAT = ""
            + "  <</Type /Catalog\n"
            + "    /Pages " + PDF_REFERENCE_FORMAT + ">>";
    // The formatting string for a PDF trailer
    public final static String PDF_TRAILER_FORMAT = ""
            + "trailer\n"
            + "  <</Root %d 0 R>>\n\n"
            + "%%%%EOF";
    // A PrintWriter printing to the output file
    private PrintWriter outputPrintWriter;
    // The stack of all active ElementHandlers
    private Stack<ElementHandler> elementHandlers;
    // The number of objects that have been written by this converter
    private int pdfObjectCount;

    /**
     * Creates a new SVGToPDFConverter for the specified output file.
     *
     * @param filename
     * @throws FileNotFoundException
     */
    public SVGToPDFConverter(String filename) throws FileNotFoundException {
        this(new File(filename));
    }

    /**
     * Creates a new SVGToPDFConverter for the specified output file.
     *
     * @param file
     * @throws FileNotFoundException
     */
    public SVGToPDFConverter(File file) throws FileNotFoundException {
        outputPrintWriter = new PrintWriter(file);
        elementHandlers = new Stack<>();
        pdfObjectCount = 0;
    }

    /**
     * Keeps the program from going online for the SVG 1.0 DTD file. The local
     * copy is at ../resources/svg10.dtd. This improves performance drastically.
     *
     * @param publicId
     * @param systemId
     * @return
     * @throws IOException
     * @throws SAXException
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws IOException, SAXException {
        InputStream inputStream = new FileInputStream("resources/svg10.dtd");
        return (new InputSource(inputStream));
    }

    /**
     * The parser calls this once at the beginning of a document. This simply
     * writes the PDF version to the new file.
     *
     * @throws SAXException
     */
    @Override
    public void startDocument() throws SAXException {
        write(String.format("%%PDF-%1.1f\n\n", PDF_VERSION));
    }

    /**
     * The parser calls this for each element in a document.
     *
     * @param namespaceURI
     * @param localName
     * @param qName
     * @param atts
     * @throws SAXException
     */
    @Override
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        // Push the appropriate ElementHandler to the stack
        if (qName.equalsIgnoreCase("SVG")) {
            elementHandlers.push(new SVGElementHandler());
        } else if (qName.equalsIgnoreCase("G")) {
            elementHandlers.push(new GElementHandler());
        } else if (qName.equalsIgnoreCase("Text")) {
            elementHandlers.push(new TextElementHandler());
        } else if (qName.equalsIgnoreCase("Rect")) {
            elementHandlers.push(new RectElementHandler());
        } else if (qName.equalsIgnoreCase("Path")) {
            elementHandlers.push(new PathElementHandler());
        } else {
            elementHandlers.push(new ElementHandler());
        }

        // Call startElement for the top element on the stack
        elementHandlers.peek()
                .startElement(namespaceURI, localName, qName, atts);
    }

    /**
     * The parser calls this for each element in a document.
     *
     * @param ch
     * @param start
     * @param length
     * @throws SAXException
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        // Call characters for the top element on the stack
        elementHandlers.peek().characters(ch, start, length);
    }

    /**
     * The parser calls this for each element in a document.
     *
     * @param namespaceURI
     * @param localName
     * @param qName
     * @throws SAXException
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        // Call endElement for the top element on the stack.
        elementHandlers.peek().endElement(qName, localName, qName);

        // See if this ElementHandler has a PDF object and write it if so.
        String pdfObject = elementHandlers.peek().getPDFObject();
        if (pdfObject != null) {
            writePDFObject(pdfObject);
        }

        // Pop the top ElementHandler off of the stack.
        elementHandlers.pop();
    }

    /**
     * Parser calls this once at the end of a document
     *
     * @throws SAXException
     */
    @Override
    public void endDocument() throws SAXException {
        // Write the contents object
        StringBuilder contentsObject = new StringBuilder("  [");
        for (int i = 1; i < pdfObjectCount; i++) {
            contentsObject.append(String.format(PDF_REFERENCE_FORMAT, i));
            contentsObject.append("\n   ");
        }
        if (pdfObjectCount > 0) {
            contentsObject.append(
                    String.format(PDF_REFERENCE_FORMAT, pdfObjectCount));
        }
        contentsObject.append("]");
        writePDFObject(contentsObject.toString());
        int contentsIndex = pdfObjectCount;

        /**
         * Write the page tree
         */
        // Write the page object (clean this up!)
        String pageObject = "  <</Type /Page\n";
        pageObject += "    /Contents "
                + String.format(PDF_REFERENCE_FORMAT, contentsIndex) + "\n";
        // Add the resources reference here, after I add in fonts
        pageObject += "    /MediaBox ["
                + DocumentAttributes.getInstance().getValue("viewBox")
                + "]\n";
        pageObject += "    /Parent "
                + String.format(PDF_REFERENCE_FORMAT, pdfObjectCount + 2)
                + ">>";
        writePDFObject(pageObject);

        // Write the pages object
        String pagesObject = String.format(PDF_PAGES_FORMAT, pdfObjectCount);
        writePDFObject(pagesObject);

        // Write the catalog object
        String catalogObject =
                String.format(PDF_CATALOG_FORMAT, pdfObjectCount);
        writePDFObject(catalogObject);

        writePDFTrailer();

        outputPrintWriter.close(); // Close the PrintWriter to finish the file
    }

    /**
     * Writes the given string to the new PDF file.
     *
     * @param s
     */
    private void write(String s) {
        outputPrintWriter.print(s);
        outputPrintWriter.flush(); // This is need to actually write the data.
    }

    /**
     * Writes the given "unwrapped" PDF object to the new PDF file.
     *
     * @param s
     */
    private void writePDFObject(String pdfObject) {
        // Takes the next index (starting from 1) and the object's contents
        String wrappedPDFObject = String.format(PDF_OBJECT_FORMAT,
                ++pdfObjectCount, pdfObject);

        // Write the wrapped object
        write(wrappedPDFObject);
    }

    /**
     * Writes the trailer for the PDF.
     */
    private void writePDFTrailer() {
        // Takes just the index of the catalog object
        String pdfTrailer = String.format(PDF_TRAILER_FORMAT, pdfObjectCount);

        write(pdfTrailer);
    }
}
