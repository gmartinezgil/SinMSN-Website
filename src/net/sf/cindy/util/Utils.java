/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.cindy.util;

/**
 * Miscellaneous utils. 
 * 
 * @author Roger Chen
 */
public class Utils {

    private Utils() {
    }

    /**
     * Get class name without packeage name.
     * 
     * @param c
     * 		class
     * @return
     * 		class simple name
     */
    public static String getClassSimpleName(Class c) {
        String className = c.getName();
        int index = className.lastIndexOf('.');
        if (index >= 0)
            return className.substring(index + 1);
        return className;
    }

    /**
     * Is JDK 1.4.
     * 
     * @return
     * 		is jdk 1.4
     */
    public static boolean isJdk14() {
        String[] version = System.getProperty("java.version").split("\\.");
        if (version.length >= 2) {
            try {
                int majorVersion = Integer.parseInt(version[0]);
                int minorVersion = Integer.parseInt(version[1]);
                return majorVersion == 1 && minorVersion == 4;
            } catch (NumberFormatException e) {
            }
        }
        return false;
    }

    /**
     * Is support JMX 1.2 specification.
     * 
     * @return
     * 		is support JMX 1.2 specification
     */
    public static boolean isSupportJmx12() {
        try {
            Class.forName("javax.management.StandardMBean", false, Utils.class
                    .getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}