/*
 *  Copyright 2017 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.common;

import com.google.common.base.StandardSystemProperty;

public final class OsHelper {

	private OsHelper() {

	}

	public enum OperatingSystem {
		WINDOWS, LINUX
	}

	public static OperatingSystem getOs() {

		final String os = StandardSystemProperty.OS_NAME.value();

		if (os.toLowerCase().startsWith("linux")) {
			return OperatingSystem.LINUX;
		} else if (os.toLowerCase().startsWith("windows")) {
			return OperatingSystem.WINDOWS;
		} else {
			throw new UnsupportedOperationException("Operating system \"" + os + "\" not supported.");
		}
	}

	public static void main(final String[] args) {
		System.out.println(getOs());
	}
}
