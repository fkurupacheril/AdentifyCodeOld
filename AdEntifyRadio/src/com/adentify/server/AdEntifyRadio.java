package com.adentify.server;

import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;


import com.musicg.fingerprint.FingerprintSimilarity;
import com.musicg.wave.Wave;

public class AdEntifyRadio implements Runnable
{
	String _strName;
	String _strURL;
	String _strOutputFile;

	public AdEntifyRadio( String name, String url, String outputFileName)
	{
		_strName = name;
		_strURL = url;
		_strOutputFile = outputFileName;
	}

	public void run(){
        try{
			System.out.println("Saving " + _strName);
            URLConnection conn = new URL(_strURL).openConnection();
            InputStream is = conn.getInputStream();

            OutputStream outstream = new FileOutputStream(new File(_strOutputFile));
            byte[] buffer = new byte[4096];
            int len;
            long t = System.currentTimeMillis();
            while ((len = is.read(buffer)) > 0 && System.currentTimeMillis() - t <= 5000) {
                outstream.write(buffer, 0, len);
            }
            outstream.close();
            
            System.out.println("Done " + _strName);
        }
        catch(Exception e){
            System.out.print(e);
        }
    }
	
    public static void main (String[] args) throws IOException, InterruptedException
    {
    	AdEntifyRadio kqed = new AdEntifyRadio("KQED", "http://kqed-ice.streamguys.org:80/kqedradio-wd-e1", "/Users/navin/Downloads/kqed.mp3");
    	AdEntifyRadio klok = new AdEntifyRadio("KLOK", "http://2353.live.streamtheworld.com:80/KLOKAM_SC", "/Users/navin/Downloads/klok.mp3");
    	 
    	(new Thread(kqed)).start();
    	(new Thread(klok)).start();
    	 
    	long t = System.currentTimeMillis();
        while (System.currentTimeMillis() - t <= 7000) {
        }
        
        /*
         * 1. Define new table to accept resu
         */
    	
        String[] cmd1 = {"/Users/navin/Documents/eclipse_workspace/AdEntifyRadio/src/ffmpeg", "-i", "/Users/navin/Downloads/foo3158916378bar.3gp",  "-vn", "-acodec", "pcm_s16le", "-ar", "44100",  "-ac", "2", "/Users/navin/Downloads/foo3158916378bar.wav"};
        Process userSampleProcess = Runtime.getRuntime().exec(cmd1);
        userSampleProcess.waitFor();

        String[] cmd2 = {"/Users/navin/Documents/eclipse_workspace/AdEntifyRadio/src/ffmpeg", "-i", "/Users/navin/Downloads/kqed.mp3",  "-vn", "-acodec", "pcm_s16le", "-ar", "44100",  "-ac", "2", "/Users/navin/Downloads/kqed.wav"};
        Process radioStreamProcess = Runtime.getRuntime().exec(cmd2);
        radioStreamProcess.waitFor();

        String track1 = "/Users/navin/Downloads/kqed.wav", track2 = "/Users/navin/Downloads/foo3158916378bar.wav";
    	Wave wave1 = new Wave(track1), wave2 = new Wave(track2);
    	FingerprintSimilarity similarity;

    	// compare fingerprints:
    	similarity = wave1.getFingerprintSimilarity(wave2);
    	System.out.println("UserAudioSample is found at " + similarity.getsetMostSimilarTimePosition() + "s in " + track1+" with similarity " + similarity.getSimilarity());
    	
        String cmd4[] = {"rm", "/Users/navin/Downloads/kqed.wav"};
    	Process kqedCleanup = Runtime.getRuntime().exec(cmd4);
        kqedCleanup.waitFor();
        
        String cmd5[] = {"rm", "/Users/navin/Downloads/foo3158916378bar.wav"};
    	Process userCleanup = Runtime.getRuntime().exec(cmd5);
        userCleanup.waitFor();
    }
}