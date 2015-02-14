package com.rapplogic.sketchloader;

import java.io.IOException;
import java.util.Arrays;

import com.rapplogic.xbee.api.XBee;
import com.rapplogic.xbee.api.XBeeAddress64;
import com.rapplogic.xbee.api.XBeeException;
import com.rapplogic.xbee.api.zigbee.ZNetTxRequest;

public class XBeeSketchLoader extends ArduinoSketchLoader {

	public XBeeSketchLoader() {
		super();
	}
	
	final int PAGE_SIZE = 80;
	final int MAGIC_BYTE1 = 0xef;
	final int MAGIC_BYTE2 = 0xac;
	// make enum
	final int CONTROL_PROG_REQUEST = 0x10; 	//10000
	final int CONTROL_PROG_DATA = 0x20; 	//100000
	// somewhat redundant
	final int CONTROL_PROG_DONE = 0x40; 	//1000000
	
	public void process(String file, String device, int speed, String xbeeAddress) throws IOException {
		// page size is max packet size for the radio
		Sketch sketch = getSketch(file, PAGE_SIZE);
		
		XBee xbee = new XBee();
		
		try {
			xbee.open(device, speed);
			
			XBeeAddress64 xBeeAddress64 = new XBeeAddress64(xbeeAddress);
			
			// TODO send request to start programming and wait for reply
			// TODO more robust approach is to send async then wait for rx acknowledgement
			// TODO put a magic word in each packet to differentiate from other radios that might be trying to communicate during proramming. for now we'll say unsupported?
			// TODO consider sending version number, a weak hash of hex file so we can query what version is on the device. could simply add up the bytes and send as 24-bit value
			
			// send header:  size + #pages
			xbee.sendSynchronous(new ZNetTxRequest(xBeeAddress64, new int[] { MAGIC_BYTE1, MAGIC_BYTE2, CONTROL_PROG_REQUEST, (sketch.getSize() >> 8) & 0xff, sketch.getSize() & 0xff, (sketch.getPages().size() >> 8) & 0xff, sketch.getPages().size() & 0xff }));
			System.out.println("Sending sketch to xbee radio " + xBeeAddress64.toString() + ", size (bytes) " + sketch.getSize() + ", packets " + sketch.getPages().size());
			
			for (Page page : sketch.getPages()) {
				// send to radio, one page at a time
				// TODO handle errors and retries
				xbee.sendSynchronous(new ZNetTxRequest(xBeeAddress64, combine(new int[] {MAGIC_BYTE1, MAGIC_BYTE2, CONTROL_PROG_DATA}, page.getData())));
				System.out.print("#");
			}

			// TODO wait for rx packet to indicate success or failure
			// whoa, need ACK
			System.out.println("\nSuccessfully flashed Arduino!");
		} catch (Exception e) {
			try {
				xbee.close();
			} catch (Exception e2) {}
		}
	}
	
	private int[] combine(int[] a, int[] b) {
		int[] result = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}

	public static void main(String[] args) throws NumberFormatException, IOException, XBeeException {
		// sketch hex file, device, speed, xbee address, radio_type  
		new XBeeSketchLoader().process(args[0], args[1], Integer.parseInt(args[2]), args[3]);
	}
}