/*=============================================================================#
 # Copyright (c) 2008-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
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
import java.util.logging.Level;
import java.util.logging.LogRecord;


final class JRIServerUtils {
	
	
	private final static int TEMP_CHAR_BUFFER_LENGTH = 0x800;
	
	private final char[] rTempCharBuffer = new char[TEMP_CHAR_BUFFER_LENGTH];
	
	private final StringBuilder rTempStringBuilder = new StringBuilder(0x2000);
	
	
	public JRIServerUtils() {
	}
	
	
	public String concat(final String[] array, final char sep) {
		this.rTempStringBuilder.setLength(0);
		for (int i = 0; i < array.length; i++) {
			this.rTempStringBuilder.append(array[i]);
			this.rTempStringBuilder.append(sep);
		}
		return this.rTempStringBuilder.toString();
	}
	
	
	public String readFile(final File file, final Charset charset) {
		this.rTempStringBuilder.setLength(0);
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(new FileInputStream(file), charset);
			int read;
			while ((read = reader.read(this.rTempCharBuffer, 0, TEMP_CHAR_BUFFER_LENGTH)) >= 0) {
				this.rTempStringBuilder.append(this.rTempCharBuffer, 0, read);
			}
			return reader.toString();
		}
		catch (final IOException e) {
			final LogRecord record = new LogRecord(Level.WARNING,
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
	
}
