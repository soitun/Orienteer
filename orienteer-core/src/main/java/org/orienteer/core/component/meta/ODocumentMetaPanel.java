package org.orienteer.core.component.meta;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.crypt.StringUtils;
import org.apache.wicket.util.string.Strings;
import org.orienteer.core.CustomAttribute;
import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.component.property.DisplayMode;
import org.orienteer.core.component.visualizer.IVisualizer;
import org.orienteer.core.component.visualizer.UIVisualizersRegistry;

import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import ru.ydn.wicket.wicketorientdb.model.DynamicPropertyValueModel;
import ru.ydn.wicket.wicketorientdb.model.OPropertyNamingModel;
import ru.ydn.wicket.wicketorientdb.security.OSecurityHelper;
import ru.ydn.wicket.wicketorientdb.security.OrientPermission;
import ru.ydn.wicket.wicketorientdb.validation.OPropertyValueValidator;

/**
 * Meta panel for {@link ODocument}
 *
 * @param <V> type of a value
 */
public class ODocumentMetaPanel<V> extends AbstractModeMetaPanel<ODocument, DisplayMode, OProperty, V> implements IDisplayModeAware {
	
	private static final long serialVersionUID = 1L;
	private String featureSpecification;
	
	public ODocumentMetaPanel(String id, IModel<DisplayMode> modeModel,
			IModel<ODocument> entityModel, IModel<OProperty> propertyModel,
			IModel<V> valueModel)
	{
		super(id, modeModel, entityModel, propertyModel, valueModel);
	}

	public ODocumentMetaPanel(String id, IModel<DisplayMode> modeModel,
			IModel<ODocument> entityModel, IModel<OProperty> propertyModel)
	{
		super(id, modeModel, entityModel, propertyModel);
	}
	
	
	@Override
	protected IModel<V> resolveValueModel() {
		return new DynamicPropertyValueModel<V>(getEntityModel(), getPropertyModel());
	}
	
	

	@Override
	protected void onPostResolveComponent(Component component, OProperty critery) {
		super.onPostResolveComponent(component, critery);
		
		if(component instanceof FormComponent)
		{
			if(critery.isNotNull()) ((FormComponent<?>)component).setRequired(true);
			((FormComponent<?>)component).add(new OPropertyValueValidator<Object>(critery, getEntityModel()));
		}
	}
	
	@Override
	protected void onConfigure() {
		featureSpecification = CustomAttribute.FEATURE.getValue(getPropertyObject());
		super.onConfigure();
		setVisible(Strings.isEmpty(featureSpecification)
					|| OSecurityHelper.isAllowed(OSecurityHelper.FEATURE_RESOURCE, featureSpecification, OrientPermission.READ));
	}
	
	@Override
	protected DisplayMode getEffectiveMode(DisplayMode mode, OProperty property) {
		if(mode.canModify() && property!= null
                && ((property.isReadonly() || (Boolean)CustomAttribute.UI_READONLY.getValue(property))
                		&& !(property.isMandatory() && !getEntityObject().containsField(property.getName())))
                	|| (!Strings.isEmpty(featureSpecification) 
                		&& !OSecurityHelper.isAllowed(OSecurityHelper.FEATURE_RESOURCE, featureSpecification, OrientPermission.UPDATE)))
		{
			return DisplayMode.VIEW;
		}
		return mode;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Component resolveComponent(String id, DisplayMode mode,
			OProperty property) {
		OType oType = property.getType();
		UIVisualizersRegistry registry = OrienteerWebApplication.get().getUIVisualizersRegistry();
		String visualizationComponent = CustomAttribute.VISUALIZATION_TYPE.getValue(property);
		if(visualizationComponent!=null)
		{
			IVisualizer visualizer = registry.getComponentFactory(oType, visualizationComponent);
			if(visualizer!=null) 
			{
				Component ret = visualizer.createComponent(id, mode, getEntityModel(), getPropertyModel(), getModel());
				if(ret!=null) return ret;
			}
		}
		return registry.getComponentFactory(oType, IVisualizer.DEFAULT_VISUALIZER)
							.createComponent(id, mode, getEntityModel(), getPropertyModel(), getModel());
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Comparable<? super T> & Serializable> T toRangePoint(String str, Class<?> clazz)
	{
		if(Strings.isEmpty(str)) return null;
		try
		{
			Method method = clazz.getMethod("valueOf", String.class);
			return (T) method.invoke(null, str);
		} catch (Exception e)
		{
			return null;
		} 	
	}

	@Override
	public IModel<String> newLabelModel() {
		return new OPropertyNamingModel(getPropertyModel());
	}
	
}
