/*******************************************************************************
 * Copyright (c) 2013 Stephan Wahlbrink (www.walware.de/goto/opensource)
 * and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * v2.1 or newer, which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.server.jri;

import static de.walware.rj.server.jri.JRIServerErrors.LOGGER;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.rosuda.JRI.Rengine;

import de.walware.rj.RjInitFailedException;
import de.walware.rj.server.ConsoleWriteCmdItem;


class JRIServerIOStreams {
	
	
	private static final int CHAR_BUFFER_SIZE= 0x2000;
	
	
	static interface StreamHandler {
		
		byte getStreamId();
		
		void domexFlush();
		
	}
	
	
	abstract class AbstractOutPipe extends Thread implements StreamHandler {
		
		
		private final byte id;
		
		protected final ByteBuffer bb;
		
		protected final ByteBuffer bbNoInput = ByteBuffer.allocate(0);
		
		private final CharsetDecoder decoder;
		
		private boolean working;
		
		
		public AbstractOutPipe(final byte id, final Charset charset) {
			super("OutPipe-" + id); //$NON-NLS-1$
			setDaemon(true);
			
			this.id= id;
			this.bb= ByteBuffer.allocateDirect(CHAR_BUFFER_SIZE * 2);
			
			this.decoder= charset.newDecoder();
			this.decoder.onMalformedInput(CodingErrorAction.REPLACE);
			this.decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		}
		
		
		@Override
		public final byte getStreamId() {
			return this.id;
		}
		
		protected ConsoleHandler[] getConsoleHandlers() {
			final List<ConsoleHandler> consoleHandlers= new ArrayList<ConsoleHandler>();
			final LogManager logManager= LogManager.getLogManager();
			for (final Enumeration<String> loggerNames= logManager.getLoggerNames(); loggerNames.hasMoreElements(); ) {
				final Logger logger= logManager.getLogger(loggerNames.nextElement());
				if (logger != null) {
					for (final Handler handler : logger.getHandlers()) {
						if (handler instanceof ConsoleHandler
								&& !consoleHandlers.contains(handler) ) {
							handler.flush();
							consoleHandlers.add((ConsoleHandler) handler);
						}
					}
				}
			}
			return consoleHandlers.toArray(new ConsoleHandler[consoleHandlers.size()]);
		}
		
		@Override
		public final void run() {
			while (true) {
				final int read= doRead();
				
				this.bb.position(this.bb.position() + read);
				this.bb.flip();
				
				JRIServerIOStreams.this.server.mainExchangeLock.lock();
				try {
					domexBeginStream(this);
					
					DECODE: while (true) {
						this.working= true;
						
						CoderResult result= null;
						if (read > 0) {
							result= this.decoder.decode(this.bb,
									JRIServerIOStreams.this.outputBuffer, false );
						}
						else if (read < 0) {
							result= this.decoder.decode(this.bb,
									JRIServerIOStreams.this.outputBuffer, true );
						}
						if (result == CoderResult.OVERFLOW) {
							domexSendOut();
							continue DECODE;
						}
						else if (read < 0) {
							this.bb.clear();
							return;
						}
						else { // result == CoderResult.UNDERFLOW
							break DECODE;
						}
					}
				}
				finally {
					JRIServerIOStreams.this.server.mainExchangeLock.unlock();
				}
				
				this.bb.compact();
			}
		}
		
		@Override
		public final void domexFlush() {
			try {
				do {
					this.working= false;
					try {
						JRIServerIOStreams.this.mexCondition.awaitNanos(50000000L);
					}
					catch (final InterruptedException e) {
					}
				} while (this.working);
				
				CoderResult result= this.decoder.decode(this.bbNoInput,
						JRIServerIOStreams.this.outputBuffer, true );
				while (result == CoderResult.OVERFLOW) {
					domexSendOut();
					result= this.decoder.flush(JRIServerIOStreams.this.outputBuffer);
				}
			}
			finally {
				this.decoder.reset();
			}
		}
		
		protected abstract int doRead();
		
	}
	
	private class SysOutPipe extends AbstractOutPipe {
		
		public SysOutPipe(final Charset charset) throws RjInitFailedException {
			super(ConsoleWriteCmdItem.SYS_OUTPUT, charset);
			
			final ConsoleHandler[] consoleHandlers= getConsoleHandlers();
			System.out.flush();
			System.err.flush();
			
			final int code= Rengine.rniInitSysPipes(this.bb, consoleHandlers);
			if (code != 0) {
				throw new RjInitFailedException("Initializing sys pipes failed: " + code + ".");
			}
		}
		
		
		@Override
		protected int doRead() {
			return Rengine.rniReadSysOut(this.bb.position());
		}
		
	}
	
	
	private final JRIServer server;
	
	
	//-- mex --
	
	private final Condition mexCondition;
	
	private byte currentStreamId;
	
	private final CharBuffer outputBuffer= CharBuffer.allocate(CHAR_BUFFER_SIZE);
	
	private StreamHandler currentHandler;
	
	
	public JRIServerIOStreams(final JRIServer server) {
		this.server= server;
		this.mexCondition= this.server.mainExchangeLock.newCondition();
	}
	
	
	void init() {
		// Sys pipes
		if (!Boolean.parseBoolean(System.getProperty("de.walware.rj.sysout.disable"))) { //$NON-NLS-1$ 
			Charset charset= null;
			{	final String enc= System.getProperty("de.walware.rj.sysout.encoding"); //$NON-NLS-1$
				if (enc != null) {
					try {
						charset= Charset.forName(enc);
					}
					catch (final Exception e) {
						LOGGER.log(Level.WARNING, "Failed to setup specified encoding for system output.", e);
					}
				}
			}
			{	final String enc= Rengine.rniGetSysOutEnc();
				if (enc != null) {
					try {
						charset= Charset.forName(enc);
					}
					catch (final Exception e) {}
				}
			}
			if (charset == null) {
				charset= Charset.defaultCharset();
			}
			
			try {
				final AbstractOutPipe abstractOutPipe= new SysOutPipe(charset);
				abstractOutPipe.start();
			}
			catch (final RjInitFailedException e) {
				LOGGER.log(Level.WARNING, "Failed to setup redirect of system pipes.", e);
			}
		}
	}
	
	void domexAppendOut(final byte streamId, final String text) {
		if (this.currentStreamId != streamId) {
			domexFlushOut();
			this.currentStreamId= streamId;
		}
		else {
			if (this.outputBuffer.position() + text.length() > CHAR_BUFFER_SIZE) {
				domexSendOut();
			}
		}
		
		if (text.length() >= CHAR_BUFFER_SIZE) {
			this.server.domlAppendCmd(new ConsoleWriteCmdItem(streamId, text));
			return;
		}
		
		{	final int pos= this.outputBuffer.position();
			text.getChars(0, text.length(), this.outputBuffer.array(), pos);
			this.outputBuffer.position(pos + text.length());
		}
	}
	
	void domexBeginStream(final StreamHandler handler) {
		if (this.currentStreamId != handler.getStreamId()) {
			domexFlushOut();
			this.currentStreamId= handler.getStreamId();
			this.currentHandler= handler;
		}
	}
	
	boolean domexHasOut() {
		return (this.outputBuffer.position() > 0);
	}
	
	void domexSendOut() {
		if (this.outputBuffer.position() > 0) {
			this.outputBuffer.flip();
			this.server.domlAppendCmd(new ConsoleWriteCmdItem(this.currentStreamId,
					this.outputBuffer.toString() ));
			this.outputBuffer.clear();
		}
	}
	
	void domexFlushOut() {
		if (this.currentHandler != null) {
			try {
				this.currentHandler.domexFlush();
			}
			catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "An error occurred when flushing output stream.", e);
			}
			finally {
				this.currentHandler= null;
			}
		}
		if (this.outputBuffer.position() > 0) {
			domexSendOut();
		}
	}
	
}
