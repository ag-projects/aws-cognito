package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.*;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

import java.util.HashMap;
import java.util.Map;

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoUserService cognitoUserService;
    private final String appClientId;
    private final String appClientSecret;

    public LoginUserHandler() {
        this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
        this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
        this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        LambdaLogger logger = context.getLogger();
        try {
            JsonObject loginDetails = JsonParser.parseString(input.getBody()).getAsJsonObject();
            JsonObject loginResult = cognitoUserService.userLogin(loginDetails, appClientId, appClientSecret);
            response.withStatusCode(200);
            response.withBody(new Gson().toJson(loginResult, JsonObject.class));
        } catch (AwsServiceException e) {
            logger.log(e.awsErrorDetails().errorMessage());
            ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
            String errorResponseJsonString = new Gson().toJson(errorResponse, ErrorResponse.class);
            response.withBody(errorResponseJsonString);
            response.withStatusCode(e.awsErrorDetails().sdkHttpResponse().statusCode());
        } catch (Exception e) {
            logger.log(e.getMessage());
            ErrorResponse errorResponse = new ErrorResponse("An error occurred: " + e.getMessage());
            String errorResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(errorResponse, ErrorResponse.class);
            response.withBody(errorResponseJsonString);
            response.withStatusCode(500);
        }

        return response;
    }
}