/*=============================================================================#
 # Copyright (c) 2013-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the GNU Lesser General Public License
 # v2.1 or newer, which accompanies this distribution, and is available at
 # http://www.gnu.org/licenses/lgpl.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.server.gr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.walware.rj.server.GDCmdItem;
import de.walware.rj.server.RJ;


public class RjsGraphicManager {
	
	
	private static final Comparator<RjsGraphic> GRAPHIC_COMPARATOR = new Comparator<RjsGraphic>() {
		
		@Override
		public int compare(final RjsGraphic o1, final RjsGraphic o2) {
			return o1.getDevId() - o2.getDevId();
		}
		
	};
	
	
	private final List<RjsGraphic> graphicList = new ArrayList<RjsGraphic>();
	
	private RjsGraphic activeGraphic;
	
	private int inOperation;
	private RjsGraphic inOperationActiveGraphic;
	
	private final RJ rj;
	
	
	public RjsGraphicManager(final RJ rj) {
		this.rj = rj;
	}
	
	
	void registerGraphic(final RjsGraphic graphic) {
		final int idx = Collections.binarySearch(this.graphicList, graphic, GRAPHIC_COMPARATOR);
		if (idx >= 0) {
			this.graphicList.set(idx, graphic);
		}
		else {
			this.graphicList.add(-(idx+1), graphic);
		}
	}
	
	void unregisterGraphic(final RjsGraphic graphic) {
		if (this.activeGraphic == graphic) {
			this.activeGraphic = null;
		}
		if (this.inOperationActiveGraphic == graphic) {
			this.inOperationActiveGraphic = null;
		}
		this.graphicList.remove(graphic);
	}
	
	void activate(final RjsGraphic graphic) {
		this.activeGraphic = graphic;
		if (this.inOperation == 0) {
			this.rj.sendMainCmd(new GDCmdItem.CSetActiveOn(
					graphic.getDevId(), graphic.getSlot() ));
		}
	}
	
	void deactivate(final RjsGraphic graphic) {
		if (this.activeGraphic == graphic) {
			this.activeGraphic = null;
		}
		if (this.inOperation == 0) {
			this.rj.sendMainCmd(new GDCmdItem.CSetActiveOff(
					graphic.getDevId(), graphic.getSlot() ));
		}
	}
	
	
	public RjsGraphic getGraphic(final int devId) {
		for (int i = 0; i < this.graphicList.size(); i++) {
			final RjsGraphic graphic = this.graphicList.get(i);
			if (graphic.getDevId() < devId) {
				continue;
			}
			else if (graphic.getDevId() > devId) {
				break;
			}
			else {
				return graphic;
			}
		}
		return null;
	}
	
	
	protected void beginOperation() {
		if (this.inOperation == 0) {
			this.inOperationActiveGraphic = this.activeGraphic;
		}
		this.inOperation++;
	}
	
	protected void endOperation() {
		this.inOperation--;
		if (this.inOperation == 0) {
			if (this.inOperationActiveGraphic != this.activeGraphic) {
				if (this.inOperationActiveGraphic != null) {
					this.rj.sendMainCmd(new GDCmdItem.CSetActiveOff(
							this.inOperationActiveGraphic.getDevId(), this.inOperationActiveGraphic.getSlot() ));
				}
				if (this.activeGraphic != null) {
					this.rj.sendMainCmd(new GDCmdItem.CSetActiveOn(
							this.activeGraphic.getDevId(), this.activeGraphic.getSlot() ));
				}
			}
			this.inOperationActiveGraphic = null;
		}
	}
	
}
