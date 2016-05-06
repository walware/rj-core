/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.jri;

import static de.walware.rj.server.jri.JRIServerErrors.LOGGER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;


final class ServerUtils {
	
	
	private final static int TEMP_CHAR_BUFFER_LENGTH= 0x800;
	
	private final char[] rTempCharBuffer= new char[TEMP_CHAR_BUFFER_LENGTH];
	
	private final StringBuilder rTempStringBuilder= new StringBuilder(0x2000);
	
	private final Map<String, Object> platformData;
	private int[] rVersion;
	
	
	public ServerUtils(final Map<String, Object> platformData) {
		this.platformData= platformData;
	}
	
	
	public String concat(final String[] array, final char sep) {
		this.rTempStringBuilder.setLength(0);
		for (int i= 0; i < array.length; i++) {
			this.rTempStringBuilder.append(array[i]);
			this.rTempStringBuilder.append(sep);
		}
		return this.rTempStringBuilder.toString();
	}
	
	
	public String readFile(final File file, final Charset charset) {
		this.rTempStringBuilder.setLength(0);
		InputStreamReader reader= null;
		try {
			reader= new InputStreamReader(new FileInputStream(file), charset);
			int read;
			while ((read= reader.read(this.rTempCharBuffer, 0, TEMP_CHAR_BUFFER_LENGTH)) >= 0) {
				this.rTempStringBuilder.append(this.rTempCharBuffer, 0, read);
			}
			return reader.toString();
		}
		catch (final IOException e) {
			final LogRecord record= new LogRecord(Level.WARNING,
					"An error occurred when reading source file ''{0}''.");
			record.setParameters(new Object[] { file.getAbsolutePath() });
			record.setThrown(e);
			LOGGER.log(record);
			return null;
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (final IOException e) {}
			}
		}
	}
	
	
	public String checkFilename(final String filename) {
		if (filename.charAt(0) == '\\') {
			return (filename.indexOf('/', 1) > 0) ? filename.replace('/', '\\') : filename;
		}
		else {
			return (filename.indexOf('\\', 1) > 0) ?filename.replace('\\', '/') : filename;
		}
	}
	
	public Object getPlatformDataValue(final String key) {
		return this.platformData.get(key);
	}
	
	public int[] getRVersion() {
		if (this.rVersion == null) {
			final Object value= this.platformData.get("version.string"); //$NON-NLS-1$
			if (value instanceof String) {
				final String[] segments= ((String) value).split("\\."); //$NON-NLS-1$
				if (segments.length >= 3) {
					try {
						final int[] version= new int[3];
						for (int i= 0; i < 3; i++) {
							version[i]= Integer.parseInt(segments[i]);
						}
						this.rVersion= version;
					}
					catch (final NumberFormatException e) {}
				}
			}
		}
		return this.rVersion;
	}
	
	public boolean isRVersionEqualGreater(final int major, final int minor) {
		final int[] rVersion= getRVersion();
		return (rVersion != null && (rVersion[0] > major
				|| (rVersion[0] == major && rVersion[1] >= minor) ));
	}
	
	public boolean isRVersionLess(final int major, final int minor) {
		final int[] rVersion= getRVersion();
		return (rVersion != null && (rVersion[0] < major
				|| (rVersion[0] == major && rVersion[1] < minor) ));
	}
	
}
