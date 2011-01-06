/*******************************************************************************
 * Copyright (c) 2009-2011 WalWare/RJ-Project (www.walware.de/goto/opensource).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Stephan Wahlbrink - initial API and implementation
 *******************************************************************************/

package de.walware.rj.eclient.graphics;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

import de.walware.ecommons.ui.util.LayoutUtil;


/**
 * Preference page with options to configure R graphic options:
 * <ul>
 *    <li>Default font families ('serif', 'sans', 'mono')</li>
 * </ul>
 * 
 * The page is not registered by this plug-in.
 */
public class RGraphicsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	
	private class FontPref {
		
		String defaultName;
		String currentName;
		
		Font currentFont;
		
		Label valueLabel;
		
		String prefKey;
		
		public FontPref(final String key) {
			prefKey = key;
		}
		
	}
	
	private FontPref fSerifPref;
	private FontPref fSansPref;
	private FontPref fMonoPref;
	private FontPref[] fFontPrefs;
	
	private int fSize = 10;
	
	
	public void init(final IWorkbench workbench) {
		fSerifPref = new FontPref(RGraphics.PREF_FONTS_SERIF_FONTNAME_KEY);
		fSansPref = new FontPref(RGraphics.PREF_FONTS_SANS_FONTNAME_KEY);
		fMonoPref = new FontPref(RGraphics.PREF_FONTS_MONO_FONTNAME_KEY);
		fFontPrefs = new FontPref[] { fSerifPref, fSansPref, fMonoPref };
		
		final IEclipsePreferences node = new DefaultScope().getNode(RGraphics.PREF_FONTS_QUALIFIER);
		for (final FontPref pref : fFontPrefs) {
			pref.defaultName = (node != null) ? node.get(pref.prefKey, "") : "";
		}
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Composite pageComposite = new Composite(parent, SWT.NONE);
		pageComposite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 1));
		
		final Group fontGroup = createFontGroup(pageComposite);
		fontGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		loadFonts();
		return pageComposite;
	}
	
	
	protected Group createFontGroup(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText("Fonts:");
		group.setLayout(LayoutUtil.applyGroupDefaults(new GridLayout(), 3));
		
		addFont(group, fSerifPref, "Default &Serif Font ('serif'):");
		addFont(group, fSansPref, "Default S&ansserif Font ('sans'):");
		addFont(group, fMonoPref, "Default &Monospace Font ('mono'):");
		
		return group;
	}
	
	private void addFont(final Group group, final FontPref pref, final String text) {
		final Label label = new Label(group, SWT.NONE);
		label.setText(text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		pref.valueLabel = new Label(group, SWT.BORDER);
		pref.valueLabel.setBackground(label.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		pref.valueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Button button = new Button(group, SWT.PUSH);
		button.setText("Edit...");
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		button.addSelectionListener(new SelectionListener() {
			public void widgetSelected(final SelectionEvent e) {
				final FontDialog dialog = new FontDialog(button.getShell(), SWT.NONE);
				dialog.setFontList((pref.currentFont != null) ? pref.currentFont.getFontData() : null);
				final FontData result = dialog.open();
				if (result != null) {
					set(pref, result.getName());
				}
			}
			public void widgetDefaultSelected(final SelectionEvent e) {
			}
		});
	}
	
	
	protected void set(final FontPref pref, final String value) {
		if (value.equals(pref.currentName)) {
			return;
		}
		pref.valueLabel.setText("");
		Font font;
		try {
			font = new Font(pref.valueLabel.getDisplay(), value, fSize, SWT.NONE);
			pref.valueLabel.setFont(font);
			pref.valueLabel.setText(value);
		}
		catch (final SWTError e) {
			font = pref.valueLabel.getDisplay().getSystemFont();
			pref.valueLabel.setFont(font);
			pref.valueLabel.setText(value + " (not available)");
		}
		if (pref.currentFont != null && !pref.currentFont.isDisposed()) {
			pref.currentFont.dispose();
		}
		pref.currentName = value;
		pref.currentFont = font;
	}
	
	@Override
	protected void performDefaults() {
		for (final FontPref pref : fFontPrefs) {
			set(pref, pref.defaultName);
		}
		super.performDefaults();
	}
	
	@Override
	protected void performApply() {
		saveFonts(true);
	}
	
	@Override
	public boolean performOk() {
		saveFonts(false);
		return true;
	}
	
	protected void loadFonts() {
		if (fFontPrefs != null) {
			final IEclipsePreferences node = new InstanceScope().getNode(RGraphics.PREF_FONTS_QUALIFIER);
			for (final FontPref pref : fFontPrefs) {
				final String value = node.get(pref.prefKey, ""); //$NON-NLS-1$
				set(pref, (value.length() > 0) ? value : pref.defaultName);
			}
		}
	}
	
	protected void saveFonts(final boolean flush) {
		if (fFontPrefs != null) {
			final IEclipsePreferences node = new InstanceScope().getNode(RGraphics.PREF_FONTS_QUALIFIER);
			for (final FontPref pref : fFontPrefs) {
				if (pref.currentName == null || pref.currentName.equals(pref.defaultName)) {
					node.remove(pref.prefKey);
				}
				else {
					node.put(pref.prefKey, pref.currentName);
				}
			}
			if (flush) {
				try {
					node.flush();
				}
				catch (final BackingStoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, -1,
							"An error occurred when storing font preferences.", e));
				}
			}
		}
	}
	
	@Override
	public void dispose() {
		if (fFontPrefs != null) {
			for (final FontPref pref : fFontPrefs) {
				if (pref.currentFont != null && !pref.currentFont.isDisposed()) {
					pref.currentFont.dispose();
					pref.currentFont = null;
				}
			}
		}
		super.dispose();
	}
	
}
