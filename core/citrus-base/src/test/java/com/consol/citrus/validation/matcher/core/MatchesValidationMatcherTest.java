/*
 * Copyright 2006-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.validation.matcher.core;

import java.util.Arrays;
import java.util.List;

import com.consol.citrus.UnitTestSupport;
import com.consol.citrus.exceptions.ValidationException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MatchesValidationMatcherTest extends UnitTestSupport {

	private MatchesValidationMatcher matcher = new MatchesValidationMatcher();

    @Test
    public void testValidateSuccess() {
    	matcher.validate("field", "This is a test", Arrays.asList(".*"), context);
        matcher.validate("field", "This is a test", Arrays.asList("Thi.*"), context);
        matcher.validate("field", "This is a test", Arrays.asList(".*test"), context);
        matcher.validate("field", "aaaab", Arrays.asList("a*b"), context);
    }

    @Test
    public void testValidateError() {
    	assertException("field", "a", Arrays.asList("[^a]"));
    	assertException("field", "aaaab", Arrays.asList("aaab*"));
    }

    private void assertException(String fieldName, String value, List<String> control) {
    	try {
    		matcher.validate(fieldName, value, control, context);
    		Assert.fail("Expected exception not thrown!");
    	} catch (ValidationException e) {
			Assert.assertTrue(e.getMessage().contains(fieldName));
			Assert.assertTrue(e.getMessage().contains(value));
			Assert.assertTrue(e.getMessage().contains(control.get(0)));
		}
    }
}
