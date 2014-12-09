package org.codemucker.testfirst.inject;

import org.codemucker.testfirst.TestFirstRuntimeException;

public class InjectionException extends TestFirstRuntimeException {

	private static final long serialVersionUID = 1L;

	public InjectionException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public InjectionException(String msg) {
		super(msg);
	}

}
