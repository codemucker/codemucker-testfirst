package org.codemucker.testfirst.inject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.codemucker.lang.BeanNameUtil;
import org.codemucker.testfirst.IRunOnScenarioEnd;

import com.google.inject.Inject;
import com.google.inject.name.Named;

class InjectMapper {
	
	private static final Object[] EMPTY_ARGS = new Object[]{};
	private static final Class<?>[] EMPTY_CLASS_ARGS = new Class[]{};
	
	private final Class<?> forType;
	private final TestInjector injector;
	private final Method postInjectMethod;
	private final Method preDestroyMethod;//TODO:implement me
	private List<InjectMapper.ValueInjector> valueInjectors = new ArrayList<>();

	InjectMapper(TestInjector injector, Class<?> forType, boolean checkIfDependencySet) {
		this.forType = forType;
		this.injector = injector;
		registerFieldInjectors(forType, checkIfDependencySet);
		registerMethodInjectors(forType, checkIfDependencySet);
		this.postInjectMethod = extractPostInjectMethodOrNull(forType);
		this.preDestroyMethod = extractPreDestoryMethodOrNull(forType);
	}

	private void registerFieldInjectors(Class<?> forType,
			boolean checkIfDependencySet) {
		Class<?> t = forType;
		while (t != null && t != Object.class) {
			for (Field f : forType.getDeclaredFields()) {
				if (isInjectorField(f)) {
					f.setAccessible(true);
					valueInjectors.add(new ValueInjector(extractNameOrNull(f), f, isOptional(f), checkIfDependencySet));
				}
			}
			t = t.getSuperclass();
		}
	}

	private void registerMethodInjectors(Class<?> forType,
			boolean checkIfDependencySet) {
		//TODO:find getter/setter pair
		for (Method setterMethod : forType.getMethods()) {
			if (isInjectorMethod(setterMethod)) {
				setterMethod.setAccessible(true);
				String getterName = BeanNameUtil.toGetterName(setterMethod.getName(), setterMethod.getParameterTypes()[0]);
				Method getterMethod = null;
				try {
					getterMethod = forType.getMethod(getterName, EMPTY_CLASS_ARGS);
				} catch (NoSuchMethodException | SecurityException e) {
					//never mind, can't access getter
				}
				valueInjectors.add(new ValueInjector(extractNameOrNull(setterMethod), setterMethod, getterMethod, isOptional(setterMethod), checkIfDependencySet));
			}
		}
	}

	private Method extractPostInjectMethodOrNull(Class<?> forType) {
		Method postInjectMethod = null;
		for (Method m : forType.getDeclaredMethods()) {
			if(m.getAnnotation(PostConstruct.class) != null){
				if(postInjectMethod!= null){
					throw new InjectionException("only one method marked with PostInject is allowed on class " + forType.getName());
				}
				m.setAccessible(true);
				postInjectMethod = m;
			}
		}
		return postInjectMethod;
	}

	private Method extractPreDestoryMethodOrNull(Class<?> forType) {
		Method preDestroyMethod = null;
		for (Method m : forType.getDeclaredMethods()) {
			if(m.getAnnotation(PreDestroy.class) != null){
				if(preDestroyMethod!= null){
					throw new InjectionException("only one method marked with PreDestroy is allowed on class " + forType.getName());
				}
				m.setAccessible(true);
				preDestroyMethod = m;
			}
		}
		return preDestroyMethod;
	}
	
	private static boolean isInjectorField(Field f) {
		return f.getAnnotation(Inject.class) != null
				|| f.getAnnotation(javax.inject.Inject.class) != null
				|| f.getAnnotation(Resource.class) != null;
	}

	private static boolean isOptional(Field f) {
		return f.getAnnotation(Inject.class) != null && f.getAnnotation(Inject.class).optional();
	}
	
	private static boolean isInjectorMethod(Method m) {
		return m.getParameters().length == 1 
				&& (m.getAnnotation(Inject.class) != null || m .getAnnotation(javax.inject.Inject.class) != null || m .getAnnotation(Resource.class) != null );
	}

	private static boolean isOptional(Method m) {
		return m.getAnnotation(Inject.class) != null && m.getAnnotation(Inject.class).optional();
	}
	
	private static String extractNameOrNull(Field f) {
		Named named = f.getAnnotation(Named.class);
		if (named != null) {
			return named.value();
		}
		
		javax.inject.Named named2 = f
				.getAnnotation(javax.inject.Named.class);
		if (named != null) {
			return named2.value();
		}
		return null;
	}

	private static String extractNameOrNull(Method m) {
		Named named = m.getAnnotation(Named.class);
		if (named != null) {
			return named.value();
		}
		javax.inject.Named named2 = m
				.getAnnotation(javax.inject.Named.class);
		if (named != null) {
			return named2.value();
		}
		return null;
	}

	public void inject(Object instance) {
		for (InjectMapper.ValueInjector vi : valueInjectors) {
			vi.inject(injector, instance);
		}
		postInject(instance);
	}

	private void postInject(Object instance) {
		if(postInjectMethod != null){
			try {
				postInjectMethod.invoke(instance, EMPTY_ARGS);
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				throw new InjectionException(
						"Error invoking post inject method "
								+ instance.getClass().getName()
								+ "." + postInjectMethod.getName()
								+ " ", e);

			}
		}
	}
	
	/**
	 * Return a wrapper if the instance supports container managed destruction
	 * @param instance
	 * @return
	 */
	public IRunOnScenarioEnd getDestructorOrNull(Object instance){
		if(preDestroyMethod == null){
			return null;
		}
		return new PreDestroyAdapter(instance, preDestroyMethod);
	}
	
	
	private static class PreDestroyAdapter implements IRunOnScenarioEnd {
		private final Object instance;
		private final Method noArgDestroyMethod;
		
		public PreDestroyAdapter(Object instance, Method method) {
			super();
			this.instance = instance;
			this.noArgDestroyMethod = method;
		}

		@Override
		public void onScenarioEnd() {
			try {
				noArgDestroyMethod.invoke(instance, EMPTY_ARGS);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new InjectionException("Error calling destroy method  " + instance.getClass().getName() + ". "+ noArgDestroyMethod.getName() +  "() on " + instance, e);
			} catch (InvocationTargetException e) {
				throw new InjectionException("Error calling destroy method  " + instance.getClass().getName() + ". "+ noArgDestroyMethod.getName() +  "() on " + instance, e.getTargetException());
			}
		}
		
	}

	private static class ValueInjector {
		private final Class<?> valueType;
		private final String valueTypeName;
		private final Field fieldSetter;
		private final Method methodSetter;
		private final Method methodGetter;
		
		private final boolean failOnMissingDependency;
		private final boolean checkIfDependencySet;

		public ValueInjector(String valueTypeName, Field fieldSetter, boolean optional,boolean checkIfDependencySet) {
			super();
			this.valueType = fieldSetter.getType();
			this.valueTypeName = valueTypeName;
			this.fieldSetter = fieldSetter;
			this.methodSetter = null;
			this.methodGetter = null;
			this.failOnMissingDependency = !optional;
			this.checkIfDependencySet = checkIfDependencySet;
		}

		public ValueInjector(String valueTypeName, Method methodSetter, Method methodGetter, boolean optional,boolean checkIfDependencySet) {
			super();
			this.valueType = methodSetter.getParameterTypes()[0];
			this.valueTypeName = valueTypeName;
			this.fieldSetter = null;
			this.methodSetter = methodSetter;
			this.methodGetter = methodGetter;
			this.failOnMissingDependency = !optional;
			this.checkIfDependencySet = checkIfDependencySet;
		}

		public void inject(TestInjector injector, Object instance) {
			Object injectValue = injector.obtain(valueType, valueTypeName,failOnMissingDependency);
			if (injectValue == null) {
				return;
			}
			if (fieldSetter != null) {
				setFieldValue(instance, injectValue);
			} else {
				setMethodValue(instance, injectValue);
			}
		}

		private void setMethodValue(Object instance, Object injectValue) {
			if(checkIfDependencySet && methodGetter != null){
				try {
					boolean valueSet = methodGetter.invoke(instance,EMPTY_ARGS) != null;					
					if(valueSet){
						return;
					}					
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new InjectionException("Error trying to determine if value is  set for " + instance.getClass().getName() + "." + methodGetter.getName() + " ",e);
				} catch (InvocationTargetException e) {
					throw new InjectionException("Error trying to determine if value is  set for " + instance.getClass().getName() + "." + methodGetter.getName() + " ",e.getTargetException());
				}
			}
			try {
				methodSetter.invoke(instance,new Object[] { injectValue });
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new InjectionException("Error injecting value into method "+ instance.getClass().getName()+ "." + methodSetter.getName()+ " ", e);
			} catch (InvocationTargetException e) {
				throw new InjectionException("Error injecting value into method "+ instance.getClass().getName()+ "." + methodSetter.getName()+ " ", e.getTargetException());
			}
		}

		private void setFieldValue(Object instance, Object injectValue) {			
			if(checkIfDependencySet){
				try {
					boolean valueSet = fieldSetter.get(instance) != null;					
					if(valueSet){
						return;
					}					
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new InjectionException("Error trying to determine if field value is  set for " + instance.getClass().getName() + "." + fieldSetter.getName() + " ",e);
				}
			}
			try {
				fieldSetter.set(instance, injectValue);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new InjectionException("Error injecting value into field " + instance.getClass().getName() + "." + fieldSetter.getName() + " ",e);
			}
		
		}

	}
}