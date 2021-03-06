/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network;

import java.io.Serializable;

/**
* The model class for NetworkPartition definition at child level.
*/
public class ChildLevelNetworkPartition implements Serializable {

    private static final long serialVersionUID = -8043298009352097370L;
    private String id;
    private String partitionAlgo;
    private ChildLevelPartition[] childLevelPartitions;
    private int min;

    /**
     * Gets the value of the partitionAlgo property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPartitionAlgo() {
        return partitionAlgo;
    }

    /**
     * Sets the value of the partitionAlgo property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPartitionAlgo(String value) {
        this.partitionAlgo = value;
    }

    /**
     * Gets the value of the id.
     */
    public String getId() {
        return id;
    }

    /**
     * sets the value of the id.
     */
    public void setId(String id) {
        this.id = id;
    }

    public ChildLevelPartition[] getChildLevelPartitions() {
        return childLevelPartitions;
    }

    public void setChildLevelPartitions(ChildLevelPartition[] childLevelPartitions) {
        this.childLevelPartitions = childLevelPartitions;
    }

    public ChildLevelPartition getChildLevelPartition(String partitionId) {
        for(ChildLevelPartition partition : childLevelPartitions) {
            if(partition.getPartitionId().equals(partitionId)) {
                return partition;
            }
        }
        return null;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }
}
