package org.wso2.carbon.identity.challenge.questions.recovery.endpoint.factories;

import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionApiService;
import  org.wso2.carbon.identity.challenge.questions.recovery.endpoint.impl.SecurityQuestionApiServiceImpl;

public class SecurityQuestionApiServiceFactory {

   private final static SecurityQuestionApiService service = new SecurityQuestionApiServiceImpl();

   public static SecurityQuestionApiService getSecurityQuestionApi()
   {
      return service;
   }
}
