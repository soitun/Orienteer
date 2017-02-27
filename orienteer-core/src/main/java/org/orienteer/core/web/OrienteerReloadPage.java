package org.orienteer.core.web;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.ResourceModel;

/**
 * @author Vitaliy Gonchar
 */
public class OrienteerReloadPage extends BasePage<Void> {

    @Override
    protected void onInitialize() {
        add(new Label("title", new ResourceModel("reload.title")));
        super.onInitialize();
        add(new Label("reload-info", new ResourceModel("reload.info")));
    }
}
