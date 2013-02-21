package org.vaadin.artur.testcodegenerator;

import org.junit.Assert;
import org.junit.Test;

public class TestWriterUtil {
    @Test
    public void lowerFirstNull() {
        Assert.assertNull(WriterUtil.lowerFirst(null));
    }

    @Test
    public void lowerFirstEmpty() {
        Assert.assertEquals("", WriterUtil.lowerFirst(""));
    }
    @Test
    public void lowerFirstSingle() {
        Assert.assertEquals("a", WriterUtil.lowerFirst("A"));
        Assert.assertEquals("a", WriterUtil.lowerFirst("a"));
        Assert.assertEquals("!", WriterUtil.lowerFirst("!"));
    }
    
    @Test
    public void lowerFirstMultiple() {
        Assert.assertEquals("aBC", WriterUtil.lowerFirst("aBC"));
        Assert.assertEquals("aBC", WriterUtil.lowerFirst("ABC"));
        Assert.assertEquals("abc", WriterUtil.lowerFirst("abc"));
        Assert.assertEquals("abc", WriterUtil.lowerFirst("Abc"));
        Assert.assertEquals("!AA", WriterUtil.lowerFirst("!AA"));
    }


}
