/*=============================================================================#
 # Copyright (c) 2008-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.rmi.Remote;

import de.walware.rj.RjException;


/**
 * Communication exchange object for a binary array/file
 */
public class BinExchange implements RjsComObject, Externalizable {
	
	private final static int C2S =                  0x00010000;
	private final static int S2C =                  0x00020000;
	private final static int OM_2 =                 0x000f0000;
	private final static int OC_2 = ~OM_2;
	
	private final static int UPLOAD =               0x00100000;
	private final static int DOWNLOAD =             0x00200000;
	private final static int OM_TYPE =              0x00f00000;
	
	private static final int OM_CUSTOM =            0x0000ffff;
	
	private final static int DEFAULT_BUFFER_SIZE = 8192;
	
	
	private final static AutoIdMap<OutputStream> gCOutList = new AutoIdMap<OutputStream>();
	
	
	static RjsComConfig.PathResolver gSPathResolver = new RjsComConfig.PathResolver() {
		@Override
		public File resolve(final Remote client, final String path) throws RjException {
			final File file = new File(path);
			if (!file.isAbsolute()) {
				throw new RjException("Relative path not supported.");
			}
			return file;
		}
	};
	
	
	private int options;
	private Remote ref;
	
	private String remoteFilePath;
	private RjsStatus status;
	
	private long inputLength;
	private InputStream inputStream;
	private int outputId;
	private byte[] bytes;
	
	
	/**
	 * Constructor for clients to upload a file
	 * @param in input stream to read the content from
	 * @param length length
	 * @param path remote file path 
	 */
	public BinExchange(final InputStream in, final long length, final String path, final Remote ref, final int options) {
		if (path == null || in == null || ref == null) {
			throw new NullPointerException();
		}
		if (path.length() <= 0) {
			throw new IllegalArgumentException("Invalid path: empty.");
		}
		if (length < 0) {
			throw new IllegalArgumentException("Invalid length: negative.");
		}
		
		this.options = ((C2S | UPLOAD) | (OM_CUSTOM & options));
		this.ref = ref;
		this.remoteFilePath = path;
		this.inputLength = length;
		this.inputStream = in;
		this.outputId = 0;
	}
	
	/**
	 * Constructor for clients to download a file
	 * @param out output stream to write the content to
	 * @param path remote file path
	 * @param options
	 */
	public BinExchange(final OutputStream out, final String path, final Remote ref, final int options) {
		if (path == null || out == null || ref == null) {
			throw new NullPointerException();
		}
		if (path.length() == 0) {
			throw new IllegalArgumentException("Illegal path argument.");
		}
		
		this.options = (C2S | DOWNLOAD) | (OM_CUSTOM & options);
		this.ref = ref;
		this.inputLength = -1;
		this.inputStream = null;
		this.outputId = gCOutList.put(out);
		this.remoteFilePath = path;
	}
	
	/**
	 * Constructor for clients to download a file into a byte array
	 * @param path remote file path
	 * @param options
	 */
	public BinExchange(final String path, final Remote ref, final int options) {
		if (path == null || ref == null) {
			throw new NullPointerException();
		}
		if (path.length() == 0) {
			throw new IllegalArgumentException("Illegal path argument.");
		}
		
		this.options = (C2S | DOWNLOAD) | (OM_CUSTOM & options);
		this.ref = ref;
		this.inputLength = -1;
		this.inputStream = null;
		this.outputId = 0;
		this.remoteFilePath = path;
	}
	
	/**
	 * Constructor for automatic deserialization
	 */
	public BinExchange() {
	}
	
	
	@Override
	public int getComType() {
		return T_FILE_EXCHANGE;
	}
	
	public boolean isOK() {
		return (this.status == null || this.status.getSeverity() == RjsStatus.OK);
	}
	
	public RjsStatus getStatus() {
		return this.status;
	}
	
	public String getFilePath() {
		return this.remoteFilePath;
	}
	
	public byte[] getBytes() {
		return this.bytes;
	}
	
	public void clear() {
		gCOutList.remove(this.outputId);
	}
	
	
	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
		final int readOptions = in.readInt();
		this.remoteFilePath = in.readUTF();
		
		switch (readOptions & (OM_TYPE | OM_2)) {
		
		case C2S | UPLOAD: {
			this.ref = (Remote) in.readObject();
			long length = this.inputLength = in.readLong();
			FileOutputStream output = null;
			try {
				final File file = (gSPathResolver != null) ? gSPathResolver.resolve(this.ref, this.remoteFilePath) : new File(this.remoteFilePath);
				output = new FileOutputStream(file, false);
				final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				while (length > 0L) {
					final int n = in.read(buffer, 0, (int) Math.min(DEFAULT_BUFFER_SIZE, length));
					if (n == -1) {
						throw new IOException("Unexcepted end of stream.");
					}
					output.write(buffer, 0, n);
					length -= n;
				}
				this.status = RjsStatus.OK_STATUS;
			}
			catch (final RjException e) {
				this.status = new RjsStatus(RjsStatus.ERROR, 0, e.getMessage());
				throw new IOException("Failed to resolve file path.");
			}
			catch (final IOException e) {
				this.status = new RjsStatus(RjsStatus.ERROR, 0, e.getMessage());
				throw new IOException("Failed to write stream to file.");
			}
			finally {
				if (output != null) {
					try {
						output.close();
					}
					catch (final IOException e) {}
				}
			}
			this.options = (readOptions & OC_2) | S2C;
			return; }
		
		case C2S | DOWNLOAD: {
			this.ref = (Remote) in.readObject();
			this.outputId = in.readInt();
			this.options = (readOptions & OC_2) | S2C;
			return; }
			
		case S2C | UPLOAD: {
			this.status = new RjsStatus(in);
			this.options = (readOptions & OC_2);
			return; }
		
		case S2C | DOWNLOAD: {
			this.outputId = in.readInt();
			this.status = new RjsStatus(in);
			if (this.status.getSeverity() == RjsStatus.OK) {
				long length = this.inputLength = in.readLong();
				boolean writing = false;
				try {
					if (this.outputId > 0) {
						final OutputStream out = gCOutList.get(this.outputId);
						final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
						while (length > 0) {
							final int n = in.read(buffer, 0, (int) Math.min(DEFAULT_BUFFER_SIZE, length));
							if (n == -1) {
								throw new IOException("Unexcepted end of stream.");
							}
							length -= n;
							writing = true;
							out.write(buffer, 0, n);
							writing = false;
						}
					}
					else {
						if (length > Integer.MAX_VALUE) {
							throw new UnsupportedOperationException();
						}
						this.bytes = new byte[(int) length];
						while (length > 0) {
							final int n = in.read(this.bytes, (int) (this.inputLength-length), (int) length);
							if (n == -1) {
								throw new IOException("Unexcepted end of stream.");
							}
							length -= n;
						}
					}
				}
				catch (IOException e) {
					if (writing) {
						this.status = new RjsStatus(RjsStatus.ERROR, 0, "Writing download to stream failed: " + e.getMessage());
						try {
							while (length > 0) {
								final long n = in.skip(length);
								if (n == -1) {
									throw new IOException("Unexcepted end of stream.");
								}
								length -= n;
							}
							return;
						}
						catch (final IOException e2) {
							e = e2;
						}
					}
					throw e;
				}
			}
			this.options = (readOptions & OC_2);
			return; }
		
		default:
			throw new IllegalStateException();
		}
	}
	
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		out.writeInt(this.options);
		out.writeUTF(this.remoteFilePath);
		
		switch (this.options & (OM_TYPE | OM_2)) {
		
		case C2S | UPLOAD: {
			out.writeObject(this.ref);
			out.writeLong(this.inputLength);
			if (this.inputStream != null) {
				final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				long length = this.inputLength;
				while (length > 0) {
					final int n = this.inputStream.read(buffer, 0, (int) Math.min(DEFAULT_BUFFER_SIZE, length));
					if (n == -1) {
						throw new IOException("Unexcepted end of stream.");
					}
					out.write(buffer, 0, n);
					length -= n;
				}
			}
			else if (this.bytes != null) {
				out.writeLong(this.inputLength);
				out.write(this.bytes, 0, (int) this.inputLength);
			}
			else {
				throw new IOException("Missing file content.");
			}
			return; }
		
		case C2S | DOWNLOAD: {
			out.writeObject(this.ref);
			out.writeInt(this.outputId);
			return; }
		
		case S2C | UPLOAD: {
			this.status.writeExternal(out);
			return; }
		
		case S2C | DOWNLOAD: {
			out.writeInt(this.outputId);
			FileInputStream input = null;
			try {
				final File file = (gSPathResolver != null) ? gSPathResolver.resolve(this.ref, this.remoteFilePath) : new File(this.remoteFilePath);
				try {
					input = new FileInputStream(file);
					input.available();
				}
				catch (final IOException e) {
					if (input != null) {
						try {
							input.close();
							input = null;
						}
						catch (final IOException ignore) {}
					}
					if (!file.exists() || e instanceof FileNotFoundException) {
						new RjsStatus(RjsStatus.ERROR, 0, "Failed to find file '"+ this.remoteFilePath + "'.").writeExternal(out);
					}
					else {
						new RjsStatus(RjsStatus.ERROR, 0, "Failed to open file '"+ this.remoteFilePath + "'.").writeExternal(out);
					}
				}
				if (input != null) {
					RjsStatus.OK_STATUS.writeExternal(out);
					long length = this.inputLength = file.length();
					out.writeLong(length);
					final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
					while (length > 0) {
						final int n = input.read(buffer, 0, (int) Math.min(DEFAULT_BUFFER_SIZE, length));
						if (n == -1) {
							throw new IOException("Unexcepted end of file content.");
						}
						length -= n;
						out.write(buffer, 0, n);
					}
				}
			}
			catch (final RjException e) {
				this.status = new RjsStatus(RjsStatus.ERROR, 0, e.getMessage());
				throw new IOException("Failed to resolve file path.");
			}
			finally {
				if (input != null) {
					try {
						input.close();
					}
					catch (final IOException ignore) {}
				}
			}
			return; }
		
		default:
			throw new IllegalStateException();
		}
	}
	
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(128);
		sb.append("DataCmdItem ");
		switch (this.options & OM_TYPE) {
		case UPLOAD:
			sb.append("UPLOAD");
			break;
		case DOWNLOAD:
			sb.append("DOWNLOAD");
			break;
		default:
			sb.append((this.options & OM_TYPE));
			break;
		}
		sb.append("\n\t").append("direction= ");
		switch (this.options & OM_2) {
		case C2S:
			sb.append("CLIENT-2-SERVER");
			break;
		case S2C:
			sb.append("SERVER-2-CLIENT");
			break;
		default:
			sb.append((this.options & OM_2));
		}
		sb.append("\n\tlength= ");
		sb.append(this.inputLength);
		if (this.status != null) {
			sb.append("\n<STATUS>\n");
			this.status.getCode();
			sb.append("\n</STATUS>");
		}
		return sb.toString();
	}
	
}
