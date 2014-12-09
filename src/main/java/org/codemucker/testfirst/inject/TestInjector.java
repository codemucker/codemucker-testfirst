package org.codemucker.testfirst.inject;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.lang.annotation.ThreadSafe;
import org.codemucker.testfirst.IRunOnScenarioEnd;

import com.google.inject.ImplementedBy;
import com.google.inject.Provider;

/**
 * An injector for testing. Honours the basic {@link Inject} and {@link com.google.inject.Inject} annotations on fields and methods, along with named bindings 
 *
 * <p>If enabled tracks what has been injected and only injects once, what implements {@link Closeable} and {@ link IRunOnScenarioEnd} and runs these when the injector is destroyed</p>
 * 
 * <p>Sub class this to provide custom behaviour. Extension points are {@link #beforeInject(Object)}, {@link #afterInject(Object)}, {@link #isInjectable(Object)} and {@link #beforeDestroy(Object)}</p>
 * 
 * 
 */
@ThreadSafe(caveats="the threadsafety is rather crude and it is intended any multi threaded code not rely on a fast injector (for say performance testing), instead setting up all objects before the test run")
public class TestInjector implements org.codemucker.testfirst.Scenario.Injector,IRunOnScenarioEnd,Closeable {

	private final static Logger log = LogManager.getLogger(TestInjector.class);
	
	private Map<String,InjectProvider> providers = new HashMap<>();
	
	private Map<Class<?>, InjectMapper> mappers = new HashMap<>();
	
	private Set<Object> injected = new LinkedHashSet<>();
	private Set<Closeable> closeables= new LinkedHashSet<>();
	private Set<IRunOnScenarioEnd> runAtScenarioEnd = new LinkedHashSet<>();
	
	private final Object lock = new Object();
	
	private boolean closed = false;
	
	/**
	 * If true, then for field injection only set dependency if null, or method injection if the getter returns null
	 */
	private boolean checkIfDependencySet = true;
	
	@Override
	public void close() throws IOException {
		onScenarioEnd();
	}
	
	@Override
	public void onScenarioEnd() {
		synchronized (lock) {
			closed = true;
			List<Object> reversedInjected = reversedCopy(injected);		
			for (Object obj : reversedInjected) {
				try {
					beforeDestroy(obj);
				} catch (Exception e) {
					log.warn("error while destroying" + obj.getClass().getName() + ", ignoring", e);
				}
			}
			
			List<Closeable> reversedCloseables = reversedCopy(closeables);
			for (Closeable c : reversedCloseables) {
				try {
					c.close();
				} catch (Exception e) {
					log.warn("error while closing " + c.getClass().getName() + ", ignoring", e);
				}
			}
			closeables.clear();
	
			List<IRunOnScenarioEnd> reversedOnEnds= reversedCopy(runAtScenarioEnd);
			for (IRunOnScenarioEnd ender : reversedOnEnds) {
				try {
					ender.onScenarioEnd();
				} catch (AssertionError e) {
					throw e;
				} catch (Exception e) {
					log.warn("error running on end" + ender.getClass().getName() + ", ignoring", e);
				}
			}
			runAtScenarioEnd.clear();
			
			providers.clear();
			mappers.clear();
			injected.clear();
		}
	}

	private static <T> List<T> reversedCopy(Collection<T> col) {
		List<T> reversed= new ArrayList<>(col);
		Collections.reverse(reversed);
		return reversed;
	}
	
	@Override
	public final <T> T inject(T obj) {
		if (obj == null || obj.getClass().isPrimitive()) {
			return obj;
		}
		if (isInjectable(obj)) {
			synchronized (lock) {
				if(closed){
					throw new InjectionException("Injector has been closed");
				}
				if (injected.contains(obj)) {
					return obj;
				}
				injected.add(obj);
				
				obj = beforeInject(obj);
				
				InjectMapper mapper = getOrCreateMapper(obj);
				//register destrcutor as soon as possible to ensure resources are properly cleaned up
				IRunOnScenarioEnd destructor = mapper.getDestructorOrNull(obj);
				if(destructor != null){
					registerLifecycles(destructor);
				}
				mapper.inject(obj);
				obj = afterInject(obj);
				registerLifecycles(obj);
			}
		}
		
		return obj;
	}
	
	/**
	 * Override if needing to exclude certain objects from being injected. Default is a true
	 * @param obj
	 * @return
	 */
	protected <T> boolean isInjectable(T obj){
		return true;
	}
	
	/**
	 * Override me to customise injection
	 * @param obj
	 * @return
	 */
	protected <T> T beforeInject(T obj){
		return obj;
	}

	/**
	 * Override to perform custom injection. Default is a no-op so no need to call on override
	 * @param obj
	 * @return the injected object, or a wrapped version
	 */
	protected <T> T afterInject(T obj){
		//do nothing
		return obj;
	}
	
	/**
	 * Override to perform additional custom cleanup on the given object. This is called before the object lifecycle methods are called. Objects passed in are in reverse order of creation. Default
	 * implementation is a no op
	 * 
	 * @param obj
	 * @return
	 */
	protected <T> T beforeDestroy(T obj){
		return obj;
	}

	private void registerLifecycles(Object obj) {
		if (obj instanceof Closeable) {
			closeables.add((Closeable) obj);
		}
		if (obj instanceof IRunOnScenarioEnd) {
			runAtScenarioEnd.add((IRunOnScenarioEnd) obj);
		}
	}

	private <T> InjectMapper getOrCreateMapper(T obj) {
		InjectMapper mapper = mappers.get(obj.getClass());
		if(mapper == null){
			mapper = new InjectMapper(this, obj.getClass(),checkIfDependencySet);
			mappers.put(obj.getClass(), mapper);
		}
		return mapper;
	}
	
	/**
	 * Register a dependency to be injected
	 * 
	 * @param value
	 * @return
	 */
	public TestInjector provide(Object value) {
		provide(value.getClass(), null, value);
		return this;
	}

	/**
	 * Register a dependency to be injected with the given name
	 * 
	 * @param name
	 * @param value
	 * @return
	 */
	public TestInjector provide(String name, Object value) {
		provide(value.getClass(), name, value);
		return this;
	}

	/**
	 * Register a dependency to be injected
	 * 
	 * @param forType the type required to cause this value to be injected
	 * @param value the value to inject
	 * @return
	 */
	public TestInjector provide(Class<?> forType, Object value) {
		provide(forType, null, value);
		return this;
	}

	/**
	 * Register a dependency to be injected
	 * 
	 * @param forType  the type required to cause this value to be injected
	 * @param name 
	 * @param value the value to inject
	 * @return
	 */
	public TestInjector provide(Class<?> forType, String name, Object value) {
		synchronized (lock) {		
			value = inject(value);
			InjectProvider p = new InjectProvider(forType, name, value);
			//use a key so we can override previously bound providers
			providers.put(getProviderKey(p),p);
		}
		return this;
	}
	
	/**
	 * Register a dependency to be injected
	 * 
	 * @param forType
	 * @param name
	 * @param provider called each time. Can return anew instance or the same as required
	 * @return
	 */
	public <T> TestInjector provide(Class<T> forType, String name, Provider<? extends T> provider) {
		synchronized (lock) {		
			inject(provider);
			InjectProvider p = new InjectProvider(forType, name, provider);
			//use a key so we can override previously bound providers
			providers.put(getProviderKey(p),p);			
		}
		return this;
	}
	
	private String getProviderKey(InjectProvider provider){
		return provider.getBindName() + "." + provider.getBindType().getName();
	}
	
	protected Object obtain(Class<?> requireType, String name) {
		return obtain(requireType,name,true);
	}

	protected Object obtain(Class<?> requireType, String name, boolean failOnMissing){
		synchronized (lock) {
			for (InjectProvider holder : providers.values()) {
				if (holder.supplies(requireType, name)) {
					return holder.get();
				}
			}
			//use defaults if found
			Object val = getDefaultDependencyOrNull(requireType, name);
			if (val != null) {
				return val;
			}
			if(failOnMissing){
				throw new InjectionException("could not find dependency " + requireType.getName() + ", name " + name);
			}
		}
		return null;
	}

	/**
	 * Called when dependency not found.  By default looks for the {@link ImplementedBy} annotation and attempts to instantiate the no arg constructor
	 * @param valueType
	 * @param name
	 * @return a dependency matching the given type, or null if none found 
	 */
	protected Object getDefaultDependencyOrNull(Class<?> valueType, String name){
		ImplementedBy implementedBy = valueType.getDeclaredAnnotation(ImplementedBy.class);
		if(implementedBy != null){
			try {
				if(!valueType.isAssignableFrom(implementedBy.value())){
					throw new InjectionException("Default implementedBy annotation on " + valueType.getName() + " uses value " + implementedBy.value().getName() + " which doesn't implement " + valueType.getName() );
				}
				Object value = implementedBy.value().newInstance();
				provide(valueType,name,value);
				return value;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new InjectionException("Could not create instance of " + implementedBy.value() + " as declared in ImplementedBy annotation on " + valueType.getName() ,e);
			}
		}
		return null;
	}

}