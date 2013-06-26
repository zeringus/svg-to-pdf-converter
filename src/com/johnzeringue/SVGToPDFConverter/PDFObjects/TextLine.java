package com.johnzeringue.SVGToPDFConverter.PDFObjects;

import java.util.Objects;

/**
 * An object representing a mutable line of text.
 *
 * @author John Zeringue
 */
public class TextLine {

    StringBuilder _contents;

    /**
     * A constructor that generates an empty line.
     */
    public TextLine() {
        _contents = new StringBuilder();
    }

    /**
     * A constructor that generates a new line with the specified contents.
     *
     * @param contents this line's contents
     */
    public TextLine(String contents) {
        _contents = new StringBuilder(contents);
    }

    /**
     * A constructor that generates a new line with the same data as the given
     * line.
     *
     * @param aLine the line to be copied
     */
    public TextLine(TextLine aLine) {
        this(aLine.toString());
    }

    /**
     * Appends the given string to this line.
     *
     * @param s the string to append
     * @return a reference to this object
     */
    public TextLine append(CharSequence s) {
        _contents.append(s);

        return this;
    }

    /**
     * Inserts the given string at the given index.
     *
     * @param index the index at which to insert
     * @param s the string to insert
     * @return a reference to this object
     */
    public TextLine insertAt(int index, CharSequence s) {
        _contents.insert(index, s);

        return this;
    }

    /**
     * Prepends the given string to this line.
     *
     * @param s the string to prepend
     * @return a reference to this object
     */
    public TextLine prepend(CharSequence s) {
        return this.insertAt(0, s);
    }

    /**
     * Returns the length of this line.
     *
     * @return this line's length
     */
    public int length() {
        return _contents.length();
    }

    /**
     * The default method generated by Netbeans using only the _contents field.
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this._contents);
        return hash;
    }

    /**
     * Return <tt>true</tt> for all other lines containing equivalent contents.
     *
     * @param obj the other object to be compared
     * @return whether the two objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TextLine other = (TextLine) obj;
        if (!Objects.equals(this.toString(), other.toString())) {
            return false;
        }
        return true;
    }

    /**
     * A delegation to StringBuilder's toString method.
     *
     * @return the string representing this line's contents
     */
    @Override
    public String toString() {
        return _contents.toString();
    }
}
