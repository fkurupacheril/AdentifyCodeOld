/*

 * The application needs to have the permission to write to external storage
 * if the output file is written to the external storage, and also the
 * permission to record audio. These permissions must be set in the
 * application's AndroidManifest.xml file, with something like:
 *
 * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 * <uses-permission android:name="android.permission.RECORD_AUDIO" />
 *
 */
package com.example.AdEntify;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.widget.LinearLayout;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.MediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.PutItemResult;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableRequest;
import com.amazonaws.services.dynamodb.model.DescribeTableResult;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.PutItemResult;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.amazonaws.services.dynamodb.model.TableDescription;
import com.amazonaws.services.dynamodb.model.TableStatus;

public class AdEntify extends Activity
{
    private static final String LOG_TAG = "AdEntify";
    private static String mFileName = null;

    
    private RecordButton mRecordButton = null;
    private MediaRecorder mRecorder = null;

    private PlayButton   mPlayButton = null;
    private MediaPlayer   mPlayer = null;

    private String UserName = null;
    private long startRecordingTime = 0;
    private long stopRecordingTime = 0;
    
    String tableName = "Ad-entify-listener-data";

    static AmazonDynamoDBClient dynamoDB;
    
    private void onRecord(boolean start) {
     	dynamoDB = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider());
        	
        if (start) {
            startRecording();
        } else {
            stopRecording();
            /*
             * Add the recording to the DynamoDB database          
             */
            setName();
            System.out.println("UserName: " + UserName);
            System.out.println("startTime: " + startRecordingTime);
            System.out.println("stopTime: " + stopRecordingTime);
             
           new commitDynamoDBEntryTask().execute(new ItemStructClass(getNextRowNumber(), UserName, startRecordingTime, stopRecordingTime, uploadRecording()));
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
    
    private void setName() {
    
    	AccountManager accountManager = AccountManager.get(this);
    	Account[] accounts = accountManager.getAccountsByType("com.google");
        UserName=accounts[0].name;
    }
    
    private int getNextRowNumber(){
       	int row = 0;
    	Random r = new Random();
        row = r.nextInt(500-400) + 400;
    	return (row); 
    }
    
	private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {

            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
    	
    	startRecordingTime = System.currentTimeMillis();
   	
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        stopRecordingTime = System.currentTimeMillis();

    }
    
  /*  private String fileAudioRecordedAsString() {
    	
    	File file = new File(mFileName);
    	byte[] fileBytes = null;
		try {
			fileBytes = FileUtils.readFileToByteArray(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String fileContent = new String(fileBytes);
    	return(fileContent);
    }*/

    private ByteBuffer createBody() {
    	FileInputStream fis = null;
    	byte[] body;
    	try {
			fis= new FileInputStream(mFileName);
			body = new byte[(int) fis.getChannel().size()];
			fis.read(body);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    	finally{
    		try {
				fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
		ByteBuffer buffer = ByteBuffer.wrap(body);
		return buffer;
	}

	private ByteBuffer uploadRecording() {
		return createBody();
		
	}

    class RecordButton extends Button {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends Button {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    setText("Stop playing");
                } else {
                    setText("Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }

    public AdEntify() {
        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/adentify.3gp";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        LinearLayout ll = new LinearLayout(this);
        mRecordButton = new RecordButton(this);
        ll.addView(mRecordButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0));
        mPlayButton = new PlayButton(this);
        ll.addView(mPlayButton,
            new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                0));
        setContentView(ll);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
    
    class ItemStructClass {
    	public ItemStructClass(int row, String userName, long startTime, long stopTime,
				ByteBuffer byteFile) {
			super();
			
			Row = row;
			UserName = userName;
			this.startTime = startTime;
			this.stopTime = stopTime;
			this.byteFile = byteFile;
		}
    	
    	int Row;
		String UserName;
    	long startTime;
    	long stopTime;
    	ByteBuffer byteFile; 
    }
    
    
    class commitDynamoDBEntryTask extends AsyncTask<ItemStructClass, Void, Void> {

		@Override
		protected Void doInBackground(ItemStructClass... args) {
    		 Map<String, AttributeValue> item = newItem(args[0].Row, args[0].UserName, args[0].startTime, args[0].stopTime, args[0].byteFile);
    		 PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
                 PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
                 System.out.println("Result: " + putItemResult);
				return null;
    	}
    }
}
    