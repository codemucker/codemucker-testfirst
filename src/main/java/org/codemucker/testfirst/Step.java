package org.codemucker.testfirst;

import java.util.concurrent.Callable;

import org.codemucker.jmatch.DefaultDescription;
import org.codemucker.jmatch.Description;
import org.codemucker.jmatch.MatchDiagnostics;
import org.codemucker.jmatch.Matcher;
import org.codemucker.testfirst.Scenario.Deleter;
import org.codemucker.testfirst.Scenario.Fetcher;
import org.codemucker.testfirst.Scenario.Inserter;
import org.codemucker.testfirst.Scenario.Invoker;

public abstract class Step {
	
	protected final Scenario scenario;
	protected final Object[] args;
	
	protected boolean passed;
	
	protected Step(Scenario scenario, Object... args){
		this.args = args;
		this.scenario = scenario;
		scenario.addStep(this);
	}
	
	abstract String getShortName();
	
	protected void runNothing(){
		passed();
	}
	
	protected void run(Invoker invoker){
		try {
			inject(invoker);
			invoker.invoke();
			passed();
		} catch(Exception e){
			throw failed(e);
		}
	}
	
	protected void run(Inserter inserter){
		try {
			inject(inserter);
			inserter.insert();
			passed();
		} catch(Exception e){
			throw failed(e);
		}
	}
	
	protected void run(Deleter deleter){
		try {
			inject(deleter);
			deleter.delete();
			passed();
		} catch(Exception e){
			throw failed(e);
		}
	}
	
	protected void run(Runnable runnable){
		try {
			inject(runnable);
			runnable.run();
			passed();
		} catch(Exception e){
			throw failed(e);
		}
	}

	protected <T> void run(Callable<T> callable){
		try {
			inject(callable);
			callable.call();
			passed();
		} catch(Exception e){
			throw failed(e);
		}
	}

	
	/*protected void run(Action action){
		try {
			action.invoke();
		} catch(Exception e){
			throw failed(e);
		}
	}*/
	
	protected <T> void run(Fetcher<T> fetcher, Matcher<? super T> matcher){
		T actual = null; 
		try {
			inject(fetcher);
			actual = fetcher.fetch();
			passed();
		} catch(Exception e){
			throw failed(e);
		}
		checkMatches(actual, matcher);
	}
	
	protected <T> void checkMatches(T actual, Matcher<? super T> matcher){
		MatchDiagnostics diag = this.scenario.newDiagnostics();
		
		if( !matcher.matches(actual, diag)){

            Description desc = new DefaultDescription();
            desc.child("Steps were",  scenario.stepsToString());
            desc.child("expected", matcher);
            desc.child("but was", actual);
            desc.text("==== Diagnostics ====");
            desc.child(diag);
		}
	}
	
	protected <T> T inject(T instance){
		return scenario.inject(instance);
	}

	protected void passed(){
		passed = true;
	}
	
	protected boolean hasPassed(){
		return passed;
	}
	
	protected TestFirstRuntimeException failed(Exception e){
		return this.scenario.stepFailed(this, e);
	}
	
}