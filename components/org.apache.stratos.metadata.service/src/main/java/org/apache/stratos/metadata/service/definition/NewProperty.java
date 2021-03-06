/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.metadata.service.definition;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlRootElement(name="properties")
public class NewProperty implements Serializable{

    private String key;
    private List<String> values = new ArrayList<String>();

    public NewProperty(){}
    public NewProperty(String key, String value){
        this.key=key;
        this.values.add(value);
    }
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String[] getValues(){
        String[] values = new String[this.values.size()];
        values = this.values.toArray(values);
        return values;
    }

    public void setValues(String[] values) {
        this.values.addAll(Arrays.asList(values));
    }


    public void setValues(String value) {
        this.values.add(value);
    }

    public void addValue(String value){
        this.values.add(value);
    }
}
