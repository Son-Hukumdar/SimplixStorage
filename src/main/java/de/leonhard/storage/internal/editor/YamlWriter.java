package de.leonhard.storage.internal.editor;

import de.leonhard.storage.utils.FileUtils;

import java.io.File;
import java.io.Writer;


/**
 * Enhanced Version of YamlWriter of EsotericSoftware which
 * implements AutoClosable
 */
public class YamlWriter extends com.esotericsoftware.yamlbeans.YamlWriter implements AutoCloseable {

    public YamlWriter(Writer writer) {
        super(writer);
    }

    public YamlWriter(File file) {
        super(FileUtils.createWriter(file));
    }
}
