package org.codemucker.testfirst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.codemucker.jmatch.*;

import com.google.common.base.Preconditions;

public class Scenario {

	private Injector injector = NullInjector.Instance;
	
	private List<IRunOnScenarioEnd> onScenearioEndListeners = new ArrayList<IRunOnScenarioEnd>();
	private String name;
	
	private List<Step> steps = new ArrayList<Step>();
	
	enum State {
		NotRun, Passed,Failed
	}
	
	public Scenario(String name){
		this.name = name;
	}
	
	public Scenario(String name, Injector injector){
		Preconditions.checkNotNull(injector,"expect injector");
		this.name = name;
		this.injector = injector;
	}
	
	public String getName(){
		return name;
	}
	
	public <T> T inject(T instance){
		if( instance == null || instance.getClass().isPrimitive()){
			return instance;
		}
		T injected = injector.inject(instance);
		if(instance instanceof IRunOnScenarioEnd){
			registerOnEndListener((IRunOnScenarioEnd)instance);
		}
		return injected;
	}
	
	private void registerOnEndListener(IRunOnScenarioEnd listener){
		if( !onScenearioEndListeners.contains(listener)){
			onScenearioEndListeners.add(listener);
		}
	}
	
	public void assertHasRunAndPassed(){
		runOnEndListeners();
		
		//TODO. check all steps have run
		boolean requiresAssert = false;
		for(Step step : steps){
//			if(!step.hasPassed()){
//				String msg = stepsToString(step, "failed");
//				
//				//TODO:give more details
//				//TODO:make junit assert exception??
//				throw new TestFirstAssertionFailedException("step failed. " + step.failed(e));
//			}
			requiresAssert = (step.getClass() == GivenStep.class || step.getClass() == WhenStep.class);
		}
		
		if(requiresAssert){
			throw new TestFirstAssertionFailedException("Require atleast one 'then' step after any 'given' or 'when' step\nScenario steps:\n" + stepsToString());
		}
	}
	
	TestFirstRuntimeException stepFailed(Step step, Exception e){
		String msg = stepsToString(step, "failed  <-- " + e.getMessage());
		return new TestFirstRuntimeException(msg, e);
	}
	
	private void runOnEndListeners(){
		List<IRunOnScenarioEnd> listeners = new ArrayList<IRunOnScenarioEnd>(onScenearioEndListeners);
		Collections.reverse(listeners);
		
		onScenearioEndListeners = null;
		for(IRunOnScenarioEnd listener : listeners){
			try{
				listener.onScenarioEnd();
			} catch(Exception e){
				throw new TestFirstRuntimeException("Listener " + listener.getClass().getName() + " threw exception on scenario end", e);
			}			
		}
	}
	
	public GivenStep given(Invoker invoker) {
		GivenStep step = new GivenStep(this, invoker);
		step.run(invoker);
		return step;
	}
	
	public GivenStep given(Inserter inserter) {
		GivenStep step = new GivenStep(this, inserter);
		step.run(inserter);
		return step;
	}
	
	public GivenStep given(Deleter deleter) {
		GivenStep step = new GivenStep(this, deleter);
		step.run(deleter);
		return step;
	}
	
	public GivenStep given(Runnable runnable) {
		GivenStep step = new GivenStep(this, runnable);
		step.run(runnable);
		return step;
	}
	
	public <T> GivenStep given(Callable<T> callable) {
		GivenStep step = new GivenStep(this, callable);
		step.run(callable);
		return step;
	}
	
	<T extends Step> T addStep(T step){
		steps.add(step);
		return step;
	}
	
	String stepsToString(){
		return stepsToString(null, null);
	}
	
	String stepsToString(Step uptoStep, String msg){
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for(Step step:this.steps){
			count++;
			sb.append(count);
			sb.append(" ");
			sb.append(step.getShortName());
			sb.append("(");
			if(step.args == null){
				sb.append("null");
			} else {
				for(int i = 0; i < step.args.length; i++){
					if( i > 0){
						sb.append(",");
					}
					Object arg = step.args[i];
					if(arg==null){
						sb.append("null");
					} else {
						sb.append(arg.getClass().getSimpleName());
					}
					sb.append(arg);
				}
			}
			sb.append(step.args);
			sb.append(")");
			//if required, show which step failed
			if(uptoStep == step){
				sb.append(" <-- ").append(msg);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	MatchDiagnostics newDiagnostics(){
		return new DefaultMatchContext();
	}

	public static class NullInjector implements Injector 
	{
		public static final NullInjector Instance = new NullInjector(); 
		
		public <T> T inject(T obj){
			return obj;
		}
	}
	
	public interface Injector {
		public <T> T inject(T obj);
	}
	
	public interface Invoker {
		public void invoke() throws Exception;
	}
	
	public interface Inserter {
		public void insert() throws Exception;
	}
	
	public interface Deleter {
		public void delete() throws Exception;
	}
	
	public interface Updater {
		public void update() throws Exception;
	}
	
	public interface Fetcher<T> {
		public T fetch() throws Exception;
	}
}
