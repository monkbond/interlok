/*
 * Copyright 2015 Adaptris Ltd.
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

package com.adaptris.taglet;

import com.sun.tools.doclets.Taglet;

import java.util.Map;

/**
 * Simple taglet that allows us to quickly specify the license requirements.
 * 
 * @author lchan
 * 
 */
public class LicenseTaglet extends AbstractTaglet {
  private static final String START = "<p>License Required: <strong>";
  private static final String END = "</strong></p>";
  private static final String NAME = "license";

  /**
   * Return the name of this custom tag.
   */
  public String getName() {
      return NAME;
  }

  @Override
  public String getStart() {
    return START;
  }

  @Override
  public String getEnd() {
    return END;
  }

  @SuppressWarnings("unchecked")
  public static void register(Map tagletMap) {
    LicenseTaglet tag = new LicenseTaglet();
     Taglet t = (Taglet) tagletMap.get(tag.getName());
     if (t != null) {
         tagletMap.remove(tag.getName());
     }
     tagletMap.put(tag.getName(), tag);
  }

}
