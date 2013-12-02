package com.adentify.server;

import java.net.URLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
public class Radio implements Runnable
{
	String _strName;
	String _strURL;
	String _strOutputFile;

	public Radio( String name, String url, String outputFileName)
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


    public static void main (String[] args)
    {
    	Radio kqed = new Radio("KQED", "http://kqed-ice.streamguys.org:80/kqedradio-wd-e1", "C:/tmp/kqed.mp3");
    	Radio klok = new Radio("KLOK", "http://2353.live.streamtheworld.com:80/KLOKAM_SC", "C:/tmp/klok.mp3");

    	(new Thread(kqed)).start();
    	(new Thread(klok)).start();
    }
}