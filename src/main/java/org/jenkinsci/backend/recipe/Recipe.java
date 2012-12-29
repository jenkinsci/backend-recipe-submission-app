package org.jenkinsci.backend.recipe;

import hudson.util.VersionNumber;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author Kohsuke Kawaguchi
 */
public class Recipe {
    private final Document dom;

    public Recipe(Document dom) {
        this.dom = dom;
    }

    public Recipe(String payload) throws DocumentException {
        this(new SAXReader().read(new StringReader(payload)));
    }

    public String getProperty(String name) {
        return dom.getRootElement().elementText(name);
    }

    public String getId() {
        return getProperty("id");
    }

    public String getDisplayName() {
        return getProperty("displayName");
    }

    public String getDescription() {
        return getProperty("description");
    }

    public VersionNumber getVersion() {
        return new VersionNumber(getProperty("version"));
    }

    public String getAuthor() {
        return getProperty("author");
    }

    public void setAuthor(String author) {
        Element a = dom.getRootElement().element("author");
        if (a==null)
            a = dom.getRootElement().addElement("author");
        a.setText(author);
    }

    @Override
    public String toString() {
        try {
            StringWriter sw = new StringWriter();
            new XMLWriter(sw).write(dom);
            return sw.toString();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String getFileName() {
        return getId()+".jrcp";
    }
}
