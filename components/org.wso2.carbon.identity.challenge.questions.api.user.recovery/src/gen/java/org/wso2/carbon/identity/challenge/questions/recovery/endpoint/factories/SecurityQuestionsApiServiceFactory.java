package org.wso2.carbon.identity.challenge.questions.recovery.endpoint.factories;

import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.SecurityQuestionsApiService;
import org.wso2.carbon.identity.challenge.questions.recovery.endpoint.impl.SecurityQuestionsApiServiceImpl;

public class SecurityQuestionsApiServiceFactory {

   private final static SecurityQuestionsApiService service = new SecurityQuestionsApiServiceImpl();

   public static SecurityQuestionsApiService getSecurityQuestionsApi()
   {
      return service;
   }
}
