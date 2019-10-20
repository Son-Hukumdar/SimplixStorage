package de.leonhard.storage.internal.datafiles.config;

import com.sun.istack.internal.NotNull;
import de.leonhard.storage.internal.base.exceptions.InvalidFileTypeException;
import de.leonhard.storage.internal.datafiles.raw.TomlFile;
import de.leonhard.storage.internal.enums.ReloadSettings;
import java.io.File;
import java.io.InputStream;


public class TomlConfig extends TomlFile {

	public TomlConfig(@NotNull File file, InputStream inputStream, ReloadSettings reloadSettings) throws InvalidFileTypeException {
		super(file, inputStream, reloadSettings);
	}
}