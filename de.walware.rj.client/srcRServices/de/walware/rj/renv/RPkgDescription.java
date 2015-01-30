/*=============================================================================#
 # Copyright (c) 2010-2015 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.renv;


/**
 * Basic immutable R package description, implementation {@link IRPkgDescription}.
 * 
 * @since 2.0
 */
public class RPkgDescription extends RPkg implements IRPkgDescription {
	
	
	private final String title;
	private final String description;
	
	private final String author;
	private final String maintainer;
	private final String url;
	
	private final String built;
	
	
	public RPkgDescription(final String name, final RNumVersion version,
			final String title, final String desription,
			final String author, final String maintainer, final String url,
			final String built) {
		super(name, version);
		this.title= title;
		this.description= desription;
		this.author= author;
		this.maintainer= maintainer;
		this.url= url;
		this.built= built;
	}
	
	
	@Override
	public String getTitle() {
		return this.title;
	}
	
	@Override
	public String getDescription() {
		return this.description;
	}
	
	@Override
	public String getAuthor() {
		return this.author;
	}
	
	@Override
	public String getMaintainer() {
		return this.maintainer;
	}
	
	@Override
	public String getUrl() {
		return this.url;
	}
	
	@Override
	public String getBuilt() {
		return this.built;
	}
	
	
	@Override
	public boolean equals(final Object obj) {
		return (obj instanceof IRPkgDescription && super.equals(obj));
	}
	
}
