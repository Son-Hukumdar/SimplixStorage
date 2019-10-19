package de.leonhard.storage.internal.datafiles.raw;

import de.leonhard.storage.internal.base.FileData;
import de.leonhard.storage.internal.base.FileTypeUtils;
import de.leonhard.storage.internal.base.FlatFile;
import de.leonhard.storage.internal.base.exceptions.InvalidFileTypeException;
import de.leonhard.storage.internal.base.exceptions.LightningFileReadException;
import de.leonhard.storage.internal.enums.FileType;
import de.leonhard.storage.internal.enums.ReloadSettings;
import de.leonhard.storage.internal.utils.FileUtils;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"unused"})
public class LightningFile extends FlatFile {
	private PrintWriter writer;


	public LightningFile(final File file, final InputStream inputStream, final ReloadSettings reloadSettings) throws InvalidFileTypeException {
		if (FileTypeUtils.isType(file, FileType.LIGHTNING)) {
			if (create(file)) {
				if (inputStream != null) {
					FileUtils.writeToFile(this.file, inputStream);
				}
			}

			reload();
			if (reloadSettings != null) {
				setReloadSettings(reloadSettings);
			}
		} else {
			throw new InvalidFileTypeException("The given file if of no valid filetype.");
		}
	}


	@Override
	public Object get(final String key) {
		update();
		String finalKey = (this.pathPrefix == null) ? key : this.pathPrefix + "." + key;
		return fileData.get(finalKey);
	}

	@SuppressWarnings("Duplicates")
	@Override
	public synchronized void set(final String key, final Object value) {
		final String finalKey = (pathPrefix == null) ? key : pathPrefix + "." + key;

		reload();

		String oldData = fileData.toString();
		fileData.insert(finalKey, value);

		if (!oldData.equals(fileData.toString())) {
			write(fileData.toMap());
		}
	}

	@Override
	public synchronized void remove(final String key) {
		final String finalKey = (pathPrefix == null) ? key : pathPrefix + "." + key;

		reload();

		fileData.remove(finalKey);

		write(fileData.toMap());
	}

	@Override
	public void reload() {
		try {
			this.fileData = new FileData(read());
		} catch (IOException | LightningFileReadException e) {
			System.err.println("Exception while reading LightningFile");
			e.printStackTrace();
		}
	}


	protected final LightningFile getLightningInstance() {
		return this;
	}


	@SuppressWarnings("Duplicates")
	private Map<String, Object> read() throws LightningFileReadException, IOException {
		List<String> lines = Files.readAllLines(this.file.toPath());
		Map<String, Object> tempMap = new HashMap<>();
		String tempKey = null;
		while (lines.size() > 0) {
			String tempLine = lines.get(0).trim();
			lines.remove(0);

			if (tempLine.contains("}")) {
				throw new LightningFileReadException("Block closed without being opened");
			} else if (tempLine.isEmpty()) {
				tempMap.put("{=}emptyline" + lines.size(), null);
			} else if (tempLine.startsWith("#")) {
				tempMap.put(tempLine, null);
			} else if (tempLine.endsWith("{")) {
				if (!tempLine.equals("{")) {
					tempKey = tempLine.replace("{", "").trim();
				} else if (tempKey == null) {
					throw new LightningFileReadException("Key must not be null");
				}
				tempMap.put(tempKey, read(lines));
			} else {
				if (tempLine.contains(" = ")) {
					String[] line = tempLine.split(" = ");
					tempMap.put(line[0], line[1]);
				} else {
					if (lines.get(1).contains("{")) {
						tempKey = tempLine;
					} else {
						throw new LightningFileReadException("Key does not contain value or block");
					}
				}
			}
		}
		return tempMap;
	}

	@SuppressWarnings("Duplicates")
	private Map<String, Object> read(List<String> lines) throws LightningFileReadException {
		Map<String, Object> tempMap = new HashMap<>();
		String tempKey = null;

		while (lines.size() > 0) {
			String tempLine = lines.get(0).trim();
			lines.remove(0);

			if (tempLine.equals("}")) {
				return tempMap;
			} else if (tempLine.contains("}")) {
				throw new LightningFileReadException("Block closed without being opened");
			} else if (tempLine.isEmpty()) {
				tempMap.put("{=}emptyline", null);
			} else if (tempLine.startsWith("#")) {
				tempMap.put(tempLine, null);
			} else if (tempLine.endsWith("{")) {
				if (!tempLine.equals("{")) {
					tempKey = tempLine.replace("{", "").trim();
				} else if (tempKey == null) {
					throw new LightningFileReadException("Key must not be null");
				}
				tempMap.put(tempKey, read(lines));
			} else {
				if (tempLine.contains(" = ")) {
					String[] line = tempLine.split(" = ");
					tempMap.put(line[0], line[1]);
				} else {
					if (lines.get(1).contains("{")) {
						tempKey = tempLine;
					} else {
						throw new LightningFileReadException("Key does not contain value or block");
					}
				}
			}
		}
		return tempMap;
	}


	private void write(Map<String, Object> map) {
		try {
			writer = new PrintWriter(file);
			for (String localKey : map.keySet()) {
				if (localKey.startsWith("#") && map.get(localKey) == null) {
					writer.println(localKey);
				} else if (localKey.startsWith("{=}emptyline") && map.get(localKey) == null) {
					writer.println("");
				} else if (map.get(localKey) instanceof Map) {
					writer.println(localKey + " " + "{");
					//noinspection unchecked
					write((Map<String, Object>) map.get(localKey), "");
				} else {
					writer.println(localKey + " = " + map.get(localKey));
				}
			}
			writer.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void write(Map<String, Object> map, String indentationString) {
		for (String localKey : map.keySet()) {
			if (localKey.startsWith("#") && map.get(localKey) == null) {
				writer.println(indentationString + "  " + localKey);
			} else if (localKey.startsWith("{=}emptyline") && map.get(localKey) == null) {
				writer.println("");
			} else if (map.get(localKey) instanceof Map) {
				writer.println(indentationString + "  " + localKey + " " + "{");
				//noinspection unchecked
				write((Map<String, Object>) map.get(localKey), indentationString + "  ");
			} else {
				writer.println(indentationString + "  " + localKey + " = " + map.get(localKey));
			}
		}
		writer.println(indentationString + "}");
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		} else {
			LightningFile lightningFile = (LightningFile) obj;
			return super.equals(lightningFile.getFlatFileInstance());
		}
	}
}