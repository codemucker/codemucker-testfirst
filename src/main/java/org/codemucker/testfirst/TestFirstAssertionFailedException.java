package org.codemucker.testfirst;

public class TestFirstAssertionFailedException  extends AssertionError {

	private static final long serialVersionUID = 1L;

	public TestFirstAssertionFailedException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public TestFirstAssertionFailedException(String msg) {
		super(msg);
	}

}
