package org.orienteer.core.loader;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.wicket.Application;
import org.apache.wicket.IInitializer;
import org.eclipse.aether.artifact.Artifact;
import org.kevoree.kcl.api.FlexyClassLoader;
import org.orienteer.core.OrienteerWebApplication;
import org.orienteer.core.loader.util.metadata.OModuleMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * @author Vitaliy Gonchar
 */
@Singleton
public class OrienteerOutsideModulesManager {
    private static final Logger LOG = LoggerFactory.getLogger(OrienteerOutsideModulesManager.class);
    private static final Map<FlexyClassLoader, Class<? extends IInitializer>> INIT_CLASSES = Maps.newConcurrentMap();
    private static final String INIT_METHOD = "init";
    private static final String DESTROY_METHOD = "destroy";

    @Inject
    private OrienteerWebApplication application;

    public synchronized FlexyClassLoader registerModule(String initializerClassName, OModuleMetadata metadata)
            throws ClassNotFoundException{
        if (application == null) {
            LOG.error("Application cannot be null");
            return null;
        }
        FlexyClassLoader classLoader = getClassLoader(metadata);
        OLoaderStorage.getRootLoader().attachChild(classLoader);

        Class<? extends IInitializer> loadClass = (Class<? extends IInitializer>) classLoader.loadClass(initializerClassName);
        boolean isInvoke = invoke(loadClass, INIT_METHOD);
        if (isInvoke) {
            INIT_CLASSES.put(classLoader,  loadClass);
            return classLoader;
        }
        return null;
    }

    public synchronized FlexyClassLoader unregisterModule(FlexyClassLoader classLoader) {
        if (INIT_CLASSES.containsKey(classLoader)) {
            Class<? extends IInitializer> initClass = INIT_CLASSES.get(classLoader);
            boolean isInvoke = invoke(initClass, DESTROY_METHOD);
            if (isInvoke) {
                INIT_CLASSES.remove(classLoader);
                return classLoader;
            }
        }
        return null;
    }

    private boolean invoke(Class<? extends IInitializer> loadClass, String methodName) {
        try {
            Object initializer = loadClass.newInstance();
            Method method = loadClass.getMethod(methodName, Application.class);
            method.invoke(initializer, application);
            return true;
        } catch (NoSuchMethodException e) {
            LOG.error("Cannot find method: " + methodName + "(Application app)");
            if (LOG.isDebugEnabled()) e.printStackTrace();
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.error("Cannot invoke init method!");
            if (LOG.isDebugEnabled())
                e.printStackTrace();
        } catch (InstantiationException e) {
            LOG.error("Cannot create new instance of " + loadClass.getName());
            if (LOG.isDebugEnabled()) e.printStackTrace();
        }
        return false;
    }

    private FlexyClassLoader getClassLoader(OModuleMetadata metadata) {
        FlexyClassLoader classLoader = OLoaderStorage.getModuleLoader(true);
        try {
            classLoader.load(metadata.getMainArtifact().getFile().toURI().toURL());
            for (Artifact dependency : metadata.getDependencies()) {
                classLoader.load(dependency.getFile().toURI().toURL());
            }
        } catch (MalformedURLException e) {
            LOG.error("Cannot create URl from " + metadata.getMainArtifact().getFile());
            if (LOG.isDebugEnabled()) e.printStackTrace();
        } catch (IOException e) {
            LOG.error("Cannot open file " + metadata.getMainArtifact().getFile());
            if (LOG.isDebugEnabled()) e.printStackTrace();
        }
        return classLoader;
    }

}
