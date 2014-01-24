/*=============================================================================#
 # Copyright (c) 2009-2014 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.rj.eclient.graphics;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.AggregateValidationStatus;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.service.prefs.BackingStoreException;

import de.walware.ecommons.databinding.DecimalValidator;
import de.walware.ecommons.ui.components.StatusInfo;
import de.walware.ecommons.ui.util.DialogUtil;
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
	
	
	public static double[] parseDPI(final String prefValue) {
		if (prefValue != null) {
			final String[] strings = prefValue.split(",");
			if (strings.length == 2) {
				try {
					return new double[] {
							Double.parseDouble(strings[0]),
							Double.parseDouble(strings[1]),
					};
				}
				catch (final Exception e) {}
			}
		}
		return null;
	}
	
	
	private static class FontPref {
		
		final String prefKey;
		
		String defaultName;
		String currentName;
		
		Font currentFont;
		
		Label valueLabel;
		
		
		public FontPref(final String key) {
			prefKey = key;
		}
		
		
		@Override
		public int hashCode() {
			return prefKey.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			return (this == obj
					|| (obj instanceof FontPref
							&& prefKey.equals(((FontPref) obj).prefKey) ));
		}
		
	}
	
	private static class Encoding {
		
		final String label;
		final String prefValue;
		
		
		public Encoding(final String label, final String prefValue) {
			this.label = label;
			this.prefValue = prefValue;
		}
		
		
		@Override
		public int hashCode() {
			return prefValue.hashCode();
		}
		
		@Override
		public boolean equals(final Object obj) {
			return (this == obj
					|| (obj instanceof Encoding
							&& prefValue.equals(((Encoding) obj).prefValue) ));
		}
		
		@Override
		public String toString() {
			return label.toString();
		};
		
	}
	
	private static final Encoding[] SYMBOL_ENCODINGS = new Encoding[] {
			new Encoding("Unicode", "Unicode"),
			new Encoding("Adobe Symbol", "AdobeSymbol"),
	};
	
	private static final Encoding SYMBOL_ENCODING_DEFAULT = SYMBOL_ENCODINGS[1];
	
	
	private DataBindingContext fDbc;
	
	private FontPref fSerifPref;
	private FontPref fSansPref;
	private FontPref fMonoPref;
	private FontPref fSymbolFontPref;
	private FontPref[] fFontPrefs;
	private Button fSymbolUseControl;
	private ComboViewer fSymbolEncodingControl;
	private Control[] fSymbolChildControls;
	
	private final int fSize = 10;
	
	private Button fCustomDpiControl;
	private Composite fCustomDpiComposite;
	private Text fCustomHDpiControl;
	private Text fCustomVDpiControl;
	
	private boolean fCustomEnabled;
	private WritableValue fCustomHDpiVisibleValue;
	private WritableValue fCustomVDpiVisibleValue;
	private double fCustomHDpiUserValue;
	private double fCustomVDpiUserValue;
	
	
	/**
	 * Created via extension point
	 */
	public RGraphicsPreferencePage() {
	}
	
	
	@Override
	public void init(final IWorkbench workbench) {
		fSerifPref = new FontPref(RGraphics.PREF_FONTS_SERIF_FONTNAME_KEY);
		fSansPref = new FontPref(RGraphics.PREF_FONTS_SANS_FONTNAME_KEY);
		fMonoPref = new FontPref(RGraphics.PREF_FONTS_MONO_FONTNAME_KEY);
		fSymbolFontPref = new FontPref(RGraphics.PREF_FONTS_SYMBOL_FONTNAME_KEY);
		fFontPrefs = new FontPref[] { fSerifPref, fSansPref, fMonoPref, fSymbolFontPref };
		
		final IEclipsePreferences node = new DefaultScope().getNode(RGraphics.PREF_FONTS_QUALIFIER);
		for (final FontPref pref : fFontPrefs) {
			pref.defaultName = (node != null) ? node.get(pref.prefKey, "") : "";
		}
	}
	
	@Override
	protected Control createContents(final Composite parent) {
		final Composite pageComposite = new Composite(parent, SWT.NONE);
		pageComposite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 1));
		
		final Group displayGroup = createDisplayGroup(pageComposite);
		displayGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		final Group fontGroup = createFontGroup(pageComposite);
		fontGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		initBindings();
		loadDisplayOptions();
		loadFontOptions();
		
		applyDialogFont(pageComposite);
		
		return pageComposite;
	}
	
	protected Group createDisplayGroup(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText("Display:");
		group.setLayout(LayoutUtil.applyGroupDefaults(new GridLayout(), 2));
		
		fCustomDpiControl = new Button(group, SWT.CHECK);
		fCustomDpiControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		fCustomDpiControl.setText("Use custom DPI (instead of system setting):");
		
		fCustomDpiComposite = new Composite(group, SWT.NONE);
		
		{	final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalIndent = LayoutUtil.defaultIndent();
			fCustomDpiComposite.setLayoutData(gd);
			fCustomDpiComposite.setLayout(LayoutUtil.applyCompositeDefaults(new GridLayout(), 2));
		}
		
		{	final Label label = new Label(fCustomDpiComposite, SWT.LEFT);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText("&Horizontal (x):");
		}
		{	final Text text = new Text(fCustomDpiComposite, SWT.BORDER | SWT.RIGHT);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(text, 8);
			text.setLayoutData(gd);
			fCustomHDpiControl = text;
		}
		{	final Label label = new Label(fCustomDpiComposite, SWT.LEFT);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText("&Vertical (y):");
		}
		{	final Text text = new Text(fCustomDpiComposite, SWT.BORDER | SWT.RIGHT);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gd.widthHint = LayoutUtil.hintWidth(text, 8);
			text.setLayoutData(gd);
			fCustomVDpiControl = text;
		}
		
		fCustomDpiControl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateDisplayGroup();
			}
		});
		
		return group;
	}
	
	private void updateDisplayGroup() {
		fCustomEnabled = fCustomDpiControl.getSelection();
		DialogUtil.setEnabled(fCustomDpiComposite, null, fCustomEnabled);
		
		final double hvalue;
		final double vvalue;
		if (!fCustomEnabled || fCustomHDpiUserValue <= 0 || fCustomVDpiUserValue <= 0) {
			final Point dpi = Display.getDefault().getDPI();
			hvalue = dpi.x;
			vvalue = dpi.y;
			if (fCustomEnabled) {
				fCustomHDpiUserValue = hvalue;
				fCustomVDpiUserValue = vvalue;
			}
		}
		else {
			hvalue = fCustomHDpiUserValue;
			vvalue = fCustomVDpiUserValue;
		}
		fCustomHDpiVisibleValue.setValue(hvalue);
		fCustomVDpiVisibleValue.setValue(vvalue);
	}
	
	protected Group createFontGroup(final Composite parent) {
		final Group group = new Group(parent, SWT.NONE);
		group.setText("Fonts:");
		group.setLayout(LayoutUtil.applyGroupDefaults(new GridLayout(), 3));
		
		addFont(group, fSerifPref, "Default &Serif Font ('serif'):");
		addFont(group, fSansPref, "Default S&ansserif Font ('sans'):");
		addFont(group, fMonoPref, "Default &Monospace Font ('mono'):");
		
		LayoutUtil.addSmallFiller(group, false);
		
		fSymbolUseControl = new Button(group, SWT.CHECK);
		fSymbolUseControl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		fSymbolUseControl.setText("Use special S&ymbol Font");
		final List<Control> symbolControls = new ArrayList<Control>();
		addFont(group, fSymbolFontPref, "Symbol &Font:",
				LayoutUtil.defaultIndent(), symbolControls );
		{	final Label label = new Label(group, SWT.NONE);
			final GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
			gd.horizontalIndent = LayoutUtil.defaultIndent();
			label.setLayoutData(gd);
			label.setText("Encoding:");
			symbolControls.add(label);
		}
		{	fSymbolEncodingControl = new ComboViewer(group, SWT.DROP_DOWN | SWT.READ_ONLY);
			final GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
			gd.widthHint = LayoutUtil.hintWidth(fSymbolEncodingControl.getCombo(), 15);
			fSymbolEncodingControl.getControl().setLayoutData(gd);
			fSymbolEncodingControl.setContentProvider(new ArrayContentProvider());
			fSymbolEncodingControl.setInput(SYMBOL_ENCODINGS);
			symbolControls.add(fSymbolEncodingControl.getControl());
		}
		fSymbolChildControls = symbolControls.toArray(new Control[symbolControls.size()]);
		fSymbolUseControl.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateSymbolControls();
			}
		});
		
		return group;
	}
	
	private void addFont(final Group group, final FontPref pref, final String text) {
		addFont(group, pref, text, 0, null);
	}
	
	private void addFont(final Group group, final FontPref pref, final String text,
			final int indent, final List<Control> controls) {
		final Label label = new Label(group, SWT.NONE);
		final GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.horizontalIndent = indent;
		label.setLayoutData(gd);
		label.setText(text);
		
		pref.valueLabel = new Label(group, SWT.BORDER);
		pref.valueLabel.setBackground(label.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		pref.valueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		final Button button = new Button(group, SWT.PUSH);
		button.setText("Edit...");
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				final FontDialog dialog = new FontDialog(button.getShell(), SWT.NONE);
				dialog.setFontList((pref.currentFont != null) ? pref.currentFont.getFontData() : null);
				final FontData result = dialog.open();
				if (result != null) {
					set(pref, result.getName());
				}
			}
			@Override
			public void widgetDefaultSelected(final SelectionEvent e) {
			}
		});
		
		if (controls != null) {
			controls.add(label);
			controls.add(pref.valueLabel);
			controls.add(button);
		}
	}
	
	protected void initBindings() {
		final Realm realm = Realm.getDefault();
		fDbc = new DataBindingContext(realm);
		addBindings(fDbc, realm);
		
		final AggregateValidationStatus validationStatus = new AggregateValidationStatus(fDbc, AggregateValidationStatus.MAX_SEVERITY);
		validationStatus.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				final IStatus currentStatus = (IStatus) event.diff.getNewValue();
				updateStatus(currentStatus);
			}
		});
	}
	
	protected void addBindings(final DataBindingContext dbc, final Realm realm) {
		fCustomHDpiVisibleValue = new WritableValue(realm, 96.0, Double.class);
		fCustomVDpiVisibleValue = new WritableValue(realm, 96.0, Double.class);
		
		dbc.bindValue(SWTObservables.observeText(fCustomHDpiControl, SWT.Modify),
				fCustomHDpiVisibleValue,
				new UpdateValueStrategy().setAfterGetValidator(new DecimalValidator(10.0, 10000.0,
						"The value for Horizontal (x) DPI is invalid (10-10000)." )), null);
		dbc.bindValue(SWTObservables.observeText(fCustomVDpiControl, SWT.Modify),
				fCustomVDpiVisibleValue,
				new UpdateValueStrategy().setAfterGetValidator(new DecimalValidator(10.0, 10000.0,
						"The value for Vertical (x) DPI is invalid (10-10000)." )), null);
		
		fCustomHDpiVisibleValue.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				if (fCustomEnabled) {
					fCustomHDpiUserValue = (Double) fCustomHDpiVisibleValue.getValue();
				}
			}
		});
		fCustomVDpiVisibleValue.addValueChangeListener(new IValueChangeListener() {
			@Override
			public void handleValueChange(final ValueChangeEvent event) {
				if (fCustomEnabled) {
					fCustomVDpiUserValue = (Double) fCustomVDpiVisibleValue.getValue();
				}
			}
		});
	}
	
	protected void updateStatus(final IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusInfo.applyToStatusLine(this, status);
	}
	
	protected void setCustomDpi(final String prefValue) {
		final double[] dpi = parseDPI(prefValue);
		if (dpi != null) {
			fCustomHDpiUserValue = dpi[0];
			fCustomVDpiUserValue = dpi[1];
			fCustomDpiControl.setSelection(true);
		}
		else {
			fCustomDpiControl.setSelection(false);
		}
		updateDisplayGroup();
	}
	
	protected void updateSymbolControls() {
		DialogUtil.setEnabled(fSymbolChildControls, null, fSymbolUseControl.getSelection());
	}
	
	protected void set(final FontPref pref, final String value) {
		if (value.equals(pref.currentName)) {
			return;
		}
		pref.valueLabel.setText("");
		Font font;
		try {
			font = new Font(pref.valueLabel.getDisplay(), value, fSize, SWT.NONE);
			if (pref != fSymbolFontPref) {
				pref.valueLabel.setFont(font);
			}
			pref.valueLabel.setText(value);
		}
		catch (final SWTError e) {
			font = JFaceResources.getDialogFont();
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
		setCustomDpi(null);
		fSymbolUseControl.setSelection(true);
		fSymbolEncodingControl.setSelection(new StructuredSelection(SYMBOL_ENCODING_DEFAULT));
		for (final FontPref pref : fFontPrefs) {
			set(pref, pref.defaultName);
		}
		updateSymbolControls();
		super.performDefaults();
	}
	
	@Override
	protected void performApply() {
		saveDisplayOptions(true);
		saveFontOptions(true);
	}
	
	@Override
	public boolean performOk() {
		saveDisplayOptions(false);
		saveFontOptions(false);
		return true;
	}
	
	
	protected IScopeContext getScope() {
		return new InstanceScope();
	}
	
	protected void loadDisplayOptions() {
		final IEclipsePreferences node = getScope().getNode(RGraphics.PREF_DISPLAY_QUALIFIER);
		setCustomDpi(node.get(RGraphics.PREF_DISPLAY_CUSTOM_DPI_KEY, null));
	}
	
	protected void saveDisplayOptions(final boolean flush) {
		final IEclipsePreferences node = getScope().getNode(RGraphics.PREF_DISPLAY_QUALIFIER);
		if (fCustomEnabled) {
			node.put(RGraphics.PREF_DISPLAY_CUSTOM_DPI_KEY,
					Double.toString(fCustomHDpiUserValue) + "," + Double.toString(fCustomVDpiUserValue));
		}
		else {
			node.remove(RGraphics.PREF_DISPLAY_CUSTOM_DPI_KEY);
		}
		if (flush) {
			try {
				node.flush();
			}
			catch (final BackingStoreException e) {
				StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, -1,
						"An error occurred when storing R graphics display preferences.", e));
			}
		}
	}
	
	protected void loadFontOptions() {
		if (fFontPrefs != null) {
			final IEclipsePreferences node = getScope().getNode(RGraphics.PREF_FONTS_QUALIFIER);
			fSymbolUseControl.setSelection(node.getBoolean(RGraphics.PREF_FONTS_SYMBOL_USE_KEY, true));
			final String s = node.get(RGraphics.PREF_FONTS_SYMBOL_ENCODING_KEY, (String) null);
			fSymbolEncodingControl.setSelection(new StructuredSelection((s != null) ?
					new Encoding(null, s) : SYMBOL_ENCODING_DEFAULT ));
			for (final FontPref pref : fFontPrefs) {
				final String value = node.get(pref.prefKey, ""); //$NON-NLS-1$
				set(pref, (value.length() > 0) ? value : pref.defaultName);
			}
			updateSymbolControls();
		}
	}
	
	protected void saveFontOptions(final boolean flush) {
		if (fFontPrefs != null) {
			final IEclipsePreferences node = getScope().getNode(RGraphics.PREF_FONTS_QUALIFIER);
			node.putBoolean(RGraphics.PREF_FONTS_SYMBOL_USE_KEY, fSymbolUseControl.getSelection());
			final IStructuredSelection selection = (IStructuredSelection) fSymbolEncodingControl.getSelection();
			if (selection.getFirstElement() instanceof Encoding
					&& !SYMBOL_ENCODING_DEFAULT.equals(selection.getFirstElement())) {
				node.put(RGraphics.PREF_FONTS_SYMBOL_ENCODING_KEY,
						((Encoding) selection.getFirstElement()).prefValue );
			}
			else {
				node.remove(RGraphics.PREF_FONTS_SYMBOL_ENCODING_KEY);
			}
			for (final FontPref pref : fFontPrefs) {
				if (pref.currentName == null || pref.currentName.equals(pref.defaultName)) {
					node.remove(pref.prefKey);
				}
				else {
					node.put(pref.prefKey, pref.currentName);
				}
			}
			updateSymbolControls();
			if (flush) {
				try {
					node.flush();
				}
				catch (final BackingStoreException e) {
					StatusManager.getManager().handle(new Status(IStatus.ERROR, RGraphics.PLUGIN_ID, -1,
							"An error occurred when storing R graphics font preferences.", e));
				}
			}
		}
	}
	
	@Override
	public void dispose() {
		if (fDbc != null) {
			fDbc.dispose();
			fDbc = null;
		}
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
