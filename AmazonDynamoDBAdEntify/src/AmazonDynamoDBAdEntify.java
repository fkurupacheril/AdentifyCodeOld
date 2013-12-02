import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class AmazonDynamoDBAdEntify {

    /*
     * Important: Be sure to fill in your AWS access credentials in the
     *            AwsCredentials.properties file before you try to run this
     *            sample.
     * http://aws.amazon.com/security-credentials
     */

    static AmazonDynamoDBClient dynamoDBClient;

    /**
     * The only information needed to create a client are security credentials
     * consisting of the AWS Access Key ID and Secret Access Key. All other
     * configuration, such as the service endpoints, are performed
     * automatically. Client parameters, such as proxies, can be specified in an
     * optional ClientConfiguration object when constructing a client.
     *
     * @see com.amazonaws.auth.BasicAWSCredentials
     * @see com.amazonaws.auth.PropertiesCredentials
     * @see com.amazonaws.ClientConfiguration
     */
   
    private static void createClient() throws Exception {
        AWSCredentials credentials = new PropertiesCredentials(
        		AmazonDynamoDBAdEntify.class.getResourceAsStream("AwsCredentials.properties"));

        dynamoDBClient = new AmazonDynamoDBClient(credentials);
    }
    
    private static void init() throws Exception {
    	/*
		 * This credentials provider implementation loads your AWS credentials
		 * from a properties file at the root of your classpath.
		 */
        createClient();
    }
        
   
   private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
           String hashKeyName, String hashKeyType) {
       
       createTable(tableName, readCapacityUnits, writeCapacityUnits, hashKeyName,  hashKeyType, null, null);    
   }
   
   private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
           String hashKeyName, String hashKeyType, String rangeKeyName, String rangeKeyType) {
       
       try {
           System.out.println("Creating table " + tableName);
   		ArrayList<KeySchemaElement> ks = new ArrayList<KeySchemaElement>();
   		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();

   		ks.add(new KeySchemaElement().withAttributeName(
   				hashKeyName).withKeyType(KeyType.HASH));
      		attributeDefinitions.add(new AttributeDefinition().withAttributeName(
      				hashKeyName).withAttributeType(hashKeyType));

           if (rangeKeyName != null){
          		ks.add(new KeySchemaElement().withAttributeName(
       				rangeKeyName).withKeyType(KeyType.RANGE));
       		attributeDefinitions.add(new AttributeDefinition().withAttributeName(
       				rangeKeyName).withAttributeType(rangeKeyType));
           }
    		        	            
           // Provide initial provisioned throughput values as Java long data types
           ProvisionedThroughput provisionedthroughput = new ProvisionedThroughput()
               .withReadCapacityUnits(readCapacityUnits)
               .withWriteCapacityUnits(writeCapacityUnits);
           
           CreateTableRequest request = new CreateTableRequest()
               .withTableName(tableName)
               .withKeySchema(ks)
               .withProvisionedThroughput(provisionedthroughput);
           
           request.setAttributeDefinitions(attributeDefinitions);

           CreateTableResult result = dynamoDBClient.createTable(request);
           
       } catch (AmazonServiceException ase) {
           System.err.println("Failed to create table " + tableName + " " + ase);
       }
   }
    
    
    public static void main(String[] args) throws Exception {
        init();

        try {
            String tableName = "Ad-entify-listener-data";
            
            createTable(tableName, 10L, 5L, "RowNumber", "N");

            // Wait for it to become active
            waitForTableToBecomeAvailable(tableName);

            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDBClient.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

 	private static Map<String, AttributeValue> newItem(int row, String name, long startTime, long stopTime, ByteBuffer byteFile) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("RowNumber", new AttributeValue().withN(String.valueOf(row)));
        item.put("UserName", new AttributeValue().withS(name));
        item.put("StartTime", new AttributeValue().withN(String.valueOf(startTime)));
        item.put("EndTime", new AttributeValue().withN(String.valueOf(stopTime)));
        item.put("Recording", new AttributeValue().withB(byteFile));
        return item;

    }

    private static void waitForTableToBecomeAvailable(String tableName) {
        System.out.println("Waiting for " + tableName + " to become ACTIVE...");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (10 * 60 * 1000);
        while (System.currentTimeMillis() < endTime) {
            try {Thread.sleep(1000 * 20);} catch (Exception e) {}
            try {
                DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
                TableDescription tableDescription = dynamoDBClient.describeTable(request).getTable();
                String tableStatus = tableDescription.getTableStatus();
                System.out.println("  - current state: " + tableStatus);
                if (tableStatus.equals(TableStatus.ACTIVE.toString())) return;
            } catch (AmazonServiceException ase) {
                if (ase.getErrorCode().equalsIgnoreCase("ResourceNotFoundException") == false) throw ase;
            }
        }

        throw new RuntimeException("Table " + tableName + " never went active");
    }

}