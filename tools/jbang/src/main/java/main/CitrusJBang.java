///usr/bin/env jbang "$0" "$@" ; exit $?

/*
 * Copyright the original author or authors.
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

//JAVA 17+
//REPOS mavencentral
//DEPS org.citrusframework:citrus:${citrus.jbang.version:4.3.3}@pom
//DEPS org.citrusframework:citrus-base:${citrus.jbang.version:4.3.3}
//DEPS org.citrusframework:citrus-main:${citrus.jbang.version:4.3.3}
//DEPS org.citrusframework:citrus-jbang:${citrus.jbang.version:4.3.3}
//DEPS org.citrusframework:citrus-groovy:${citrus.jbang.version:4.3.3}
//DEPS org.citrusframework:citrus-xml:${citrus.jbang.version:4.3.3}
//DEPS org.citrusframework:citrus-yaml:${citrus.jbang.version:4.3.3}
package main;

import org.citrusframework.jbang.CitrusJBangMain;

/**
 * Main to run CitrusJBang
 */
public class CitrusJBang {

    public static void main(String... args) {
        CitrusJBangMain.run(args);
    }
}
