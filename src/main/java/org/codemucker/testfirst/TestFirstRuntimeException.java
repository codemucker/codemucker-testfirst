package org.codemucker.testfirst;

public class TestFirstRuntimeException  extends RuntimeException {

	private static final long serialVersionUID = -6924067074191709186L;

	public TestFirstRuntimeException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public TestFirstRuntimeException(String msg) {
		super(msg);
	}

}
