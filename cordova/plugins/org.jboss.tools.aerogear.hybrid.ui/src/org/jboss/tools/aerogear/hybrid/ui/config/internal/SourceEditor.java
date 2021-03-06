/*******************************************************************************
 * Copyright (c) 2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.aerogear.hybrid.ui.config.internal;

import org.eclipse.jface.text.IDocument;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.StructuredTextEditor;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.w3c.dom.Document;
/**
 * Config.xml editor.
 * 
 * @author Gorkem Ercan
 *
 */
public class SourceEditor extends StructuredTextEditor {
	
	IStructuredModel model;

    public SourceEditor() {
    }


    /**
     * Gets DOM document from sourceEditor
     * 
     * @return DOM document of config.xml file
     */
    public Document getSourceDocument() {

	IDocument doc = getDocumentProvider().getDocument(getEditorInput());
	Document document = null;

	
	try {
	    model = StructuredModelManager.getModelManager()
		    .getExistingModelForEdit(doc);
	    if ((model != null) && (model instanceof IDOMModel)) {
		document = ((IDOMModel) model).getDocument();
	    }
	} finally {
	   
	}
	return document;
    }

    @Override
	public void dispose() {
		if (model != null) {
			model.releaseFromEdit();
		}
		super.dispose();
	}

}
