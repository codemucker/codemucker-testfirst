package org.codemucker.testfirst;

public class TestFirstException  extends RuntimeException {

	private static final long serialVersionUID = -6924067074191709186L;

	public TestFirstException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public TestFirstException(String msg) {
		super(msg);
	}

}
