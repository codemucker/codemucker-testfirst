package org.codemucker.testfirst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codemucker.match.*;
import org.codemucker.match.DefaultMatchContext;
import org.codemucker.match.Description;
import org.codemucker.match.MatchDiagnostics;
import org.codemucker.match.Matcher;

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
	
	public String getName(){
		return name;
	}
	
	public <T> T inject(T instance){
		if( instance == null || instance.getClass().isPrimitive()){
			return instance;
		}
		T injected = injector.inject(instance);
		if( instance instanceof IRunOnScenarioEnd){
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
		for(Step step : steps){
			if(!step.hasPassed()){
				//TODO:give more details
				//TODO:make junit assert exception??
				throw new TestFirstException("not all steps passed");
			}
		}
		
	}
	
	private void runOnEndListeners(){
		List<IRunOnScenarioEnd> listeners = new ArrayList<IRunOnScenarioEnd>(onScenearioEndListeners);
		Collections.reverse(listeners);
		
		onScenearioEndListeners = null;
		for(IRunOnScenarioEnd listener : listeners){
			try{
				listener.onScenarioEnd();
			} catch(Exception e){
				throw new TestFirstException("Listener " + listener.getClass().getName() + " threw exception on scenario end", e);
			}			
		}
	}
	
	private <T extends Step> T addStep(T step){
		steps.add(step);
		return step;
	}

	private MatchDiagnostics NewDiagnostics(){
		return new DefaultMatchContext();
	}

	class GivenStep extends ThenStep {
		public GivenStep(Scenario scenario, Object... objs) {
			super(scenario,objs);
		}
		
		public GivenStep given(Invoker invoker) {
			GivenStep step = new GivenStep(scenario, invoker);
			step.inject(invoker);
			step.run(invoker);
			return step;
		}
		
		public GivenStep given(Inserter inserter) {
			GivenStep step = new GivenStep(scenario, inserter);
			step.inject(inserter);
			step.run(inserter);
			return step;
		}
		
		public GivenStep given(Deleter deleter) {
			GivenStep step = new GivenStep(scenario, deleter);
			step.inject(deleter);
			step.run(deleter);
			return step;
		}
	}

	class WhenStep extends ThenStep {
		public WhenStep(Scenario scenario, Object... objs) {
			super(scenario,objs);
		}
	}
	
	class ThenStep extends Step {
		public ThenStep(Scenario scenario, Object... objs) {
			super(scenario,objs);
		}
		
		public WhenStep whenNothing() {
			WhenStep step = new WhenStep(scenario);
			step.runNothing();
			return step;
		}
		
		public WhenStep when(Invoker invoker) {
			WhenStep step = new WhenStep(scenario, invoker);
			step.inject(invoker);
			step.run(invoker);
			return step;
		}
		
		public WhenStep when(Inserter inserter) {
			WhenStep step = new WhenStep(scenario, inserter);
			step.inject(inserter);
			step.run(inserter);
			return step;
		}
		
		public WhenStep when(Deleter deleter) {
			WhenStep step = new WhenStep(scenario, deleter);
			step.inject(deleter);
			step.run(deleter);
			return step;
		}
		
		public <T> ThenStep then(Fetcher<T> fetcher, Matcher<? super T> matcher) {
			ThenStep step = new ThenStep(scenario, fetcher, matcher);
			step.run(fetcher,matcher);
			return step;
		}
		
		public <T> ThenStep then(T actual, Matcher<? super T> matcher) {
			ThenStep step = new ThenStep(scenario, actual, matcher);
			step.run(actual,matcher);
			return step;
		}
		
		public ThenStep thenNothing() {
			ThenStep step = new ThenStep(scenario);
			step.runNothing();
			return step;
		}
	}
	
	public abstract class Step {
		protected final Object[] args;
		protected final Scenario scenario;
		protected boolean passed;
		protected Step(Scenario scenario, Object... args){
			this.args = args;
			this.scenario = scenario;
			scenario.addStep(this);
		}
		
		protected void runNothing(){
			passed();
		}
		
		protected void run(Invoker invoker){
			try {
				invoker.invoke();
				passed();
			} catch(Exception e){
				throw failed(e);
			}
		}
		
		protected void run(Inserter inserter){
			try {
				inserter.insert();
				passed();
			} catch(Exception e){
				throw failed(e);
			}
		}
		
		protected void run(Deleter deleter){
			try {
				deleter.delete();
				passed();
			} catch(Exception e){
				throw failed(e);
			}
		}
		
		protected void run(Runnable runnable){
			try {
				runnable.run();
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
				actual = fetcher.fetch();
				passed();
			} catch(Exception e){
				throw failed(e);
			}
			run(actual, matcher);
		}
		
		protected <T> void run(T actual, Matcher<? super T> matcher){
			MatchDiagnostics diag = this.scenario.NewDiagnostics();
			
			if( !matcher.matches(actual, diag)){

                Description desc = new DefaultDescription();
                desc.child("Steps were",  StepsToString());
                desc.child("expected", matcher);
                desc.child("but was", actual);
                desc.text("==== Diagnostics ====");
                desc.child(diag);
			}
			
		}
		
		private String StepsToString(){
			StringBuilder sb = new StringBuilder();
			for(Step step:steps){
				sb.append(step.getClass().getSimpleName());
				sb.append("(");
				if( step.args == null){
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
				sb.append("\n");
			}
			return sb.toString();
		}
		
	
		protected <T> T inject(T instance){
			return scenario.inject(instance);
		}
	
		protected void passed(){
			//todo:
			//scenario.passed(this);
			passed = true;
		}
		
		protected boolean hasPassed(){
			return passed;
		}
		
		protected TestFirstException failed(Exception e){
			return failed("step failed", e);
		}
		
		protected TestFirstException failed(String msg, Exception e){
			//todo:
			//scenario.failed(this,e);
			
			return new TestFirstException(msg,e);
		}
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
