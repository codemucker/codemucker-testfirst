package org.codemucker.testfirst.inject;

import com.google.inject.Provider;

/**
 * Holds a dependency to inject into a bean
 */
class InjectProvider {
	private final Class<?> type;
	private final String bindName;
	private final Object value;
	private final Provider<?> provider;

	public InjectProvider(Class<?> type, String name, Object value) {
		super();
		this.type = type;
		this.bindName = name;
		this.value = value;
		this.provider = null;
	}

	public InjectProvider(Class<?> type, String name, Provider<?> provider) {
		super();
		this.type = type;
		this.bindName = name;
		this.value = null;
		this.provider = provider;
	}

	public Object get() {
		if (provider != null) {
			try {
				return provider.get();
			}
			catch(Exception e){
				throw new InjectionException("Error obtaining value from provider for type '" + type.getName() + "', bindName '" + bindName + "', provider " + provider.getClass().getName(), e);
			}
		}
		return value;
	}

	public Class<?> getBindType() {
		return type;
	}

	public String getBindName() {
		return bindName;
	}
	
	public boolean supplies(Class<?> requireType, String name){
		return type.isAssignableFrom(requireType) && this.bindName == name;
	}
	
}