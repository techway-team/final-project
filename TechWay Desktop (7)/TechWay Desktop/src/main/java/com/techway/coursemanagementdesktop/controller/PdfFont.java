package com.techway.coursemanagementdesktop.controller;

import com.itextpdf.io.font.otf.Glyph;
import com.itextpdf.io.font.otf.GlyphLine;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfOutputStream;
import com.itextpdf.kernel.pdf.PdfString;

import java.util.List;

public class PdfFont extends com.itextpdf.kernel.font.PdfFont {
    @Override
    public Glyph getGlyph(int i) {
        return null;
    }

    @Override
    public GlyphLine createGlyphLine(String s) {
        return null;
    }

    @Override
    public int appendGlyphs(String s, int i, int i1, List<Glyph> list) {
        return 0;
    }

    @Override
    public int appendAnyGlyph(String s, int i, List<Glyph> list) {
        return 0;
    }

    @Override
    public byte[] convertToBytes(String s) {
        return new byte[0];
    }

    @Override
    public byte[] convertToBytes(GlyphLine glyphLine) {
        return new byte[0];
    }

    @Override
    public String decode(PdfString pdfString) {
        return "";
    }

    @Override
    public GlyphLine decodeIntoGlyphLine(PdfString pdfString) {
        return null;
    }

    @Override
    public float getContentWidth(PdfString pdfString) {
        return 0;
    }

    @Override
    public byte[] convertToBytes(Glyph glyph) {
        return new byte[0];
    }

    @Override
    public void writeText(GlyphLine glyphLine, int i, int i1, PdfOutputStream pdfOutputStream) {

    }

    @Override
    public void writeText(String s, PdfOutputStream pdfOutputStream) {

    }

    @Override
    protected PdfDictionary getFontDescriptor(String s) {
        return null;
    }
}
