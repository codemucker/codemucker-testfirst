package org.codemucker.testfirst;

public class WhenStep extends ThenStep {
	
	public WhenStep(Scenario scenario, Object... objs) {
		super(scenario,objs);
	}
	
	@Override
	String getShortName() {
		return "when";
	}
}