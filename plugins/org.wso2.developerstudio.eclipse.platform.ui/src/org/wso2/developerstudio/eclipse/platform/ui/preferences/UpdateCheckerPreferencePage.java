/* Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.developerstudio.eclipse.platform.ui.preferences;

import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wso2.developerstudio.eclipse.platform.ui.Activator;

public class UpdateCheckerPreferencePage extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {
	
	public static final String UPDATE_INTAVAL = "UPDATE_INTAVAL";
	public static final String RELESE_SITE_URL = "RELESE_SITE_URL";
	public static final String UPDATE_SITE_URL = "UPDATE_SITE_URL";

	private IPreferenceStore preferenceStore;
	
	@Override
	public void init(IWorkbench workbench) {
		preferenceStore = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(preferenceStore);
		setDescription("WSO2 Developer Studio UpdateChecker Preference");
		
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(UPDATE_SITE_URL, "Update site:", getFieldEditorParent()));
		addField(new StringFieldEditor(RELESE_SITE_URL, "Relase site:", getFieldEditorParent()));		
		String[][] types = { { "never", "never" },{ "Daily", "Daily" }, { "Weekly", "Weekly" }};
		ComboFieldEditor editor = new ComboFieldEditor(UPDATE_INTAVAL, "Check update", types,
				getFieldEditorParent());
		addField(editor);	
	}	

}
