package com.appsdeveloperblog.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.appsdeveloperblog.aws.lambda.common.Constants;
import com.appsdeveloperblog.aws.lambda.service.CognitoUserService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateUserHandlerTest {

    @Mock
    CognitoUserService cognitoUserService;
    @Mock
    APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
    @Mock
    Context context;
    @Mock
    LambdaLogger lambdaLoggerMock;
    @InjectMocks
    CreateUserHandler createUserHandler;


    @BeforeEach
    public void runBeforeEachTestMethod() {
        when(context.getLogger()).thenReturn(lambdaLoggerMock);
    }

    @AfterEach
    public void runAfterEachMethod() {
        System.out.println("Cleaning up test data");
    }

    @BeforeAll
    public static void runBeforeAllTests() {
        System.out.println("Running Before All method");
    }

    @AfterAll
    public static void runAfterAllMethods() {
        System.out.println("Running after all the tests execution");
    }

    @Test
    public void  testHandleRequest_whenValidDetailsProvided_returnsSuccessfulResponse() {

        // Given
        JsonObject userDetails = new JsonObject();
        userDetails.addProperty("firstName", "Armen");
        userDetails.addProperty("lastName", "Gharibi");
        userDetails.addProperty("email", "armen.gharibi@gmail.com");
        userDetails.addProperty("password", "12345678");
        String userDetailsJsonString = new Gson().toJson(userDetails, JsonObject.class);

        when(apiGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsJsonString);

        JsonObject createUserResult = new JsonObject();
        createUserResult.addProperty(Constants.IS_SUCCESSFUL, true);
        createUserResult.addProperty(Constants.STATUS_CODE, 200);
        createUserResult.addProperty(Constants.COGNITO_USER_ID, UUID.randomUUID().toString());
        createUserResult.addProperty(Constants.IS_CONFIRMED, false);
        when(cognitoUserService.createUser(any(), any(), any())).thenReturn(createUserResult);

        // When
        APIGatewayProxyResponseEvent responseEvent = createUserHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

        // Then
        verify(lambdaLoggerMock, times(1)).log(anyString());
        assertTrue(responseBodyJson.get(Constants.IS_SUCCESSFUL).getAsBoolean());
        assertEquals(200, responseBodyJson.get(Constants.STATUS_CODE).getAsInt());
        assertNotNull(responseBodyJson.get(Constants.COGNITO_USER_ID).getAsString());
        assertEquals(200, responseEvent.getStatusCode());
        assertFalse(responseBodyJson.get(Constants.IS_CONFIRMED).getAsBoolean());
        verify(cognitoUserService, times(1)).createUser(any(), any(), any());
    }

    @Test
    public void  testHandleRequest_whenEmptyRequestBodyIsProvided_returnsErrorMessage() {
        // Given
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("");

        // When
        APIGatewayProxyResponseEvent responseEvent = createUserHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

        // Then
        assertEquals(500, responseEvent.getStatusCode());
        assertNotNull(responseBodyJson.get("message"), "Missing the 'message' property in the JSON response.");
        assertFalse(responseBodyJson.get("message"). getAsString().isEmpty(), "Error message should not be empty");
    }

    @Test
    public void  testHandleRequest_whenAwsServiceExceptionOccurs_returnsErrorMessage() {
        // Given
        when(apiGatewayProxyRequestEvent.getBody()).thenReturn("{}");

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
                .errorCode("")
                .sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
                .errorMessage("AwsServiceException Occurred")
                .build();

        AwsServiceException awsServiceException = AwsServiceException.builder()
                .statusCode(500)
                .awsErrorDetails(awsErrorDetails)
                .build();

        when(cognitoUserService.createUser(any(), any(), any())).thenThrow(awsServiceException);

        // When
        APIGatewayProxyResponseEvent responseEvent = createUserHandler.handleRequest(apiGatewayProxyRequestEvent, context);
        String responseBody = responseEvent.getBody();
        JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();

        // Then
        assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), responseEvent.getStatusCode());
        assertNotNull(responseBodyJson.get("message"));
    }


}
