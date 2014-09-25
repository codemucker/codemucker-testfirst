package org.codemucker.testfirst;

import org.codemucker.jmatch.Matcher;
import org.codemucker.testfirst.Scenario.Deleter;
import org.codemucker.testfirst.Scenario.Fetcher;
import org.codemucker.testfirst.Scenario.Inserter;
import org.codemucker.testfirst.Scenario.Invoker;

public class ThenStep extends Step {
	
	public ThenStep(Scenario scenario, Object... objs) {
		super(scenario,objs);
	}

	@Override
	String getShortName() {
		return "then";
	}
	
	public WhenStep whenNothing() {
		WhenStep step = new WhenStep(scenario);
		step.runNothing();
		return step;
	}
	
	public WhenStep when(Invoker invoker) {
		WhenStep step = new WhenStep(scenario, invoker);
		step.run(invoker);
		return step;
	}
	
	public WhenStep when(Inserter inserter) {
		WhenStep step = new WhenStep(scenario, inserter);
		step.run(inserter);
		return step;
	}
	
	public WhenStep when(Deleter deleter) {
		WhenStep step = new WhenStep(scenario, deleter);
		step.run(deleter);
		return step;
	}
	
	public WhenStep when(Runnable runnable) {
		WhenStep step = new WhenStep(scenario, runnable);
		step.run(runnable);
		return step;
	}
	
	public <T> ThenStep then(Fetcher<T> fetcher, Matcher<? super T> matcher) {
		ThenStep step = new ThenStep(scenario, fetcher, matcher);
		step.run(fetcher,matcher);
		return step;
	}
	
	public <T> ThenStep then(T actual, Matcher<? super T> matcher) {
		ThenStep step = new ThenStep(scenario, actual, matcher);
		step.checkMatches(actual,matcher);
		return step;
	}
	
	public ThenStep thenNothing() {
		ThenStep step = new ThenStep(scenario);
		step.runNothing();
		return step;
	}
}