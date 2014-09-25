package org.codemucker.testfirst;

import java.util.concurrent.Callable;

import org.codemucker.testfirst.Scenario.Deleter;
import org.codemucker.testfirst.Scenario.Inserter;
import org.codemucker.testfirst.Scenario.Invoker;

public class GivenStep extends ThenStep {
	
	public GivenStep(Scenario scenario, Object... objs) {
		super(scenario,objs);
	}
	
	@Override
	String getShortName() {
		return "given";
	}
	
	public GivenStep given(Invoker invoker) {
		GivenStep step = new GivenStep(scenario, invoker);
		step.run(invoker);
		return step;
	}
	
	public GivenStep given(Inserter inserter) {
		GivenStep step = new GivenStep(scenario, inserter);
		step.run(inserter);
		return step;
	}
	
	public GivenStep given(Deleter deleter) {
		GivenStep step = new GivenStep(scenario, deleter);
		step.run(deleter);
		return step;
	}
	
	public GivenStep given(Runnable runnable) {
		GivenStep step = new GivenStep(scenario, runnable);
		step.run(runnable);
		return step;
	}
	
	public <T> GivenStep given(Callable<T> callable) {
		GivenStep step = new GivenStep(scenario, callable);
		step.run(callable);
		return step;
	}
}