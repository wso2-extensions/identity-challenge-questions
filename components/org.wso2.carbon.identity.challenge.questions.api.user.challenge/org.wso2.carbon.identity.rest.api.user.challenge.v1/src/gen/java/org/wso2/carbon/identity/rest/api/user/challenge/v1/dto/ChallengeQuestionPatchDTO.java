/*
 * Copyright (c) 2019, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.rest.api.user.challenge.v1.dto;

import org.wso2.carbon.identity.rest.api.user.challenge.v1.dto.ChallengeQuestionDTO;

import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.*;

import javax.validation.constraints.NotNull;





@ApiModel(description = "")
public class ChallengeQuestionPatchDTO  {
  
  
  @NotNull
  private ChallengeQuestionDTO challengeQuestion = null;
  
  @NotNull
  private String operation = null;

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("challengeQuestion")
  public ChallengeQuestionDTO getChallengeQuestion() {
    return challengeQuestion;
  }
  public void setChallengeQuestion(ChallengeQuestionDTO challengeQuestion) {
    this.challengeQuestion = challengeQuestion;
  }

  
  /**
   **/
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("operation")
  public String getOperation() {
    return operation;
  }
  public void setOperation(String operation) {
    this.operation = operation;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChallengeQuestionPatchDTO {\n");
    
    sb.append("  challengeQuestion: ").append(challengeQuestion).append("\n");
    sb.append("  operation: ").append(operation).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
