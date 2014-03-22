package org.codemucker.testfirst;

import junit.framework.Assert;

import org.codemucker.testfirst.Scenario;
import org.junit.Test;

public class ScenarioTest {

	@Test
	public void NameCanBeSet(){
		Assert.assertEquals("MyName",new Scenario("MyName").getName());
	}
	
}
