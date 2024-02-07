/*
 * Copyright (c) 2016, WSO2 LLC. (https://www.wso2.com).
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

package org.wso2.carbon.identity.challenge.questions.recovery.endpoint;

import org.wso2.carbon.identity.recovery.endpoint.dto.*;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionsApiService;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.factories.SecurityQuestionsApiServiceFactory;

import io.swagger.annotations.ApiParam;

import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.dto.InitiateAllQuestionResponseDTO;
import org.wso2.carbon.identity.recovery.endpoint.dto.ErrorDTO;

import java.util.List;

import java.io.InputStream;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import javax.ws.rs.core.Response;
import javax.ws.rs.*;

@Path("/security-questions")
@Consumes({ "application/json" })
@Produces({ "application/json" })
@io.swagger.annotations.Api(value = "/security-questions", description = "the security-questions API")
public class SecurityQuestionsApi  {

   private final SecurityQuestionsApiService delegate = SecurityQuestionsApiServiceFactory.getSecurityQuestionsApi();

    @GET
    
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @io.swagger.annotations.ApiOperation(value = "", notes = "This API is used to initiate password recovery using user challenge questions at once. Response will be a random challenge questions with a confirmation key.\n", response = InitiateAllQuestionResponseDTO.class)
    @io.swagger.annotations.ApiResponses(value = { 
        @io.swagger.annotations.ApiResponse(code = 200, message = "Successful response"),
        
        @io.swagger.annotations.ApiResponse(code = 204, message = "No content"),
        
        @io.swagger.annotations.ApiResponse(code = 400, message = "Bad Request"),
        
        @io.swagger.annotations.ApiResponse(code = 500, message = "Server Error") })

    public Response securityQuestionsGet(@ApiParam(value = "username of the user",required=true) @QueryParam("username") String username,
    @ApiParam(value = "`User Store Domain` which user belongs. If not specified, it will be `PRIMARY` domain.\n") @QueryParam("realm") String realm,
    @ApiParam(value = "`Tenant Domain` which user belongs. If not specified, it will be `carbon.super` domain.\n") @QueryParam("tenant-domain") String tenantDomain)
    {
    return delegate.securityQuestionsGet(username,realm,tenantDomain);
    }
}

