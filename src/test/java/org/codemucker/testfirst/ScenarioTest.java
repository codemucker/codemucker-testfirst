package org.codemucker.testfirst;

import java.util.concurrent.Callable;

import org.codemucker.testfirst.Scenario.Deleter;
import org.codemucker.testfirst.Scenario.Inserter;
import org.codemucker.testfirst.Scenario.Invoker;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class ScenarioTest {
	
	private Mockery mocks = new Mockery();
	
	@Rule public TestName name = new TestName();
	
	@Test
	public void nameCanBeSet(){
		Assert.assertEquals("MyName",new Scenario("MyName").getName());
	}
	
	@Test
	public void noStepsScenarioPassed(){
		Scenario s = scenario();
		s.assertHasRunAndPassed();
	}
	
	@Test
	public void lasStepsIsNotAnAssertionStepThrowsException(){
		final Runnable r = mocks.mock(Runnable.class);
		mocks.checking(new Expectations(){{atLeast(1).of(r).run();}});
		
		Scenario s = scenario();
		s.given(r);
		
		TestFirstAssertionFailedException thrown = null;
		try{
			s.assertHasRunAndPassed();
		}
		catch(TestFirstAssertionFailedException e){
			thrown = e;
		}
		Assert.assertNotNull("expected failure when last step is not a 'then'",thrown);
	}
	
	@Test
	public void givensAreInvoked() throws Exception {
		final Runnable r = mocks.mock(Runnable.class);
		final Callable<String> callable = mocks.mock(Callable.class);
		final Invoker invoker = mocks.mock(Invoker.class);
		final Inserter inserter = mocks.mock(Inserter.class);
		final Deleter deleter= mocks.mock(Deleter.class);
		
		
		mocks.checking(new Expectations(){{
				exactly(1).of(r).run();
				exactly(1).of(callable).call();
					will(returnValue("something"));
				exactly(1).of(invoker).invoke();
				exactly(1).of(inserter).insert();
				exactly(1).of(deleter).delete();
		}});
		
		Scenario s = scenario();
		s
			.given(r)
			.given(callable)
			.given(invoker)
			.given(inserter)
			.given(deleter)
			//required to pass scenario.assertHasRunAndPassed
			.whenNothing()
			.thenNothing();
			
		s.assertHasRunAndPassed();
		mocks.assertIsSatisfied();
	}
	
	private Scenario scenario(){
		return new Scenario(name.getMethodName());
	}
	
}
