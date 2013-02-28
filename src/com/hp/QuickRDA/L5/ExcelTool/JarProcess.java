package com.hp.QuickRDA.L5.ExcelTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JarProcess extends Thread {
	public void run() {
		String userInput = "";
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (!"q".equals ( userInput ) && Start.gInitialized) {
				try {
					Thread.sleep ( 1000 ); // sleep for a second
				} catch  ( InterruptedException e) {
					// do nothing
				}
				System.out.print ( "." );
				if (console.ready()) {
					userInput = console.readLine();
				}
				System.out.print ( "." );
			}
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	Start.gShutDown = true;
	}
}