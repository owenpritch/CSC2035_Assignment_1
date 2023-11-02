import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	DatagramSocket socket;
	static final int RETRY_LIMIT = 4;	/* 
	 * UTILITY METHODS PROVIDED FOR YOU 
	 * Do NOT edit the following functions:
	 *      exitErr
	 *      checksum
	 *      checkFile
	 *      isCorrupted  
	 *      
	 */

	/* exit unimplemented method */
	public void exitErr(String msg) {
		System.out.println("Error: " + msg);
		System.exit(0);
	}	

	/* calculate the segment checksum by adding the payload */
	public int checksum(String content, Boolean corrupted)
	{
		if (!corrupted)  
		{
			int i; 
			int sum = 0;
			for (i = 0; i < content.length(); i++)
				sum += (int)content.charAt(i);
			return sum;
		}
		return 0;
	}


	/* check if the input file does exist */
	File checkFile(String fileName)
	{
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("SENDER: File does not exists"); 
			System.out.println("SENDER: Exit .."); 
			System.exit(0);
		}
		return file;
	}


	/* 
	 * returns true with the given probability 
	 * 
	 * The result can be passed to the checksum function to "corrupt" a 
	 * checksum with the given probability to simulate network errors in 
	 * file transfer 
	 */
	public boolean isCorrupted(float prob)
	{ 
		double randomValue = Math.random();   
		return randomValue <= prob; 
	}



	/*
	 * The main method for the client.
	 * Do NOT change anything in this method.
	 *
	 * Only specify one transfer mode. That is, either nm or wt
	 */

	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		if (args.length < 5) {
			System.err.println("Usage: java Client <host name> <port number> <input file name> <output file name> <nm|wt>");
			System.err.println("host name: is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.err.println("input file name: is the file to send");
			System.err.println("output file name: is the name of the output file");
			System.err.println("nm selects normal transfer|wt selects transfer with time out");
			System.exit(1);
		}

		Client client = new Client();
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		InetAddress ip = InetAddress.getByName(hostName);
		File file = client.checkFile(args[2]);
		String outputFile =  args[3];
		System.out.println ("----------------------------------------------------");
		System.out.println ("SENDER: File "+ args[2] +" exists  " );
		System.out.println ("----------------------------------------------------");
		System.out.println ("----------------------------------------------------");
		String choice=args[4];
		float loss = 0;
		Scanner sc=new Scanner(System.in);  


		System.out.println ("SENDER: Sending meta data");
		client.sendMetaData(portNumber, ip, file, outputFile); 

		if (choice.equalsIgnoreCase("wt")) {
			System.out.println("Enter the probability of a corrupted checksum (between 0 and 1): ");
			loss = sc.nextFloat();
		} 

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		switch(choice)
		{
		case "nm":
			client.sendFileNormal (portNumber, ip, file);
			break;

		case "wt": 
			client.sendFileWithTimeOut(portNumber, ip, file, loss);
			break; 
		default:
			System.out.println("Error! mode is not recognised");
		} 


		System.out.println("SENDER: File is sent\n");
		sc.close(); 
	}


	/*
	 * THE THREE METHODS THAT YOU HAVE TO IMPLEMENT FOR PART 1 and PART 2
	 * 
	 * Do not change any method signatures 
	 */

	/* TODO: send metadata (file size and file name to create) to the server 
	 * outputFile: is the name of the file that the server will create
	*/
	public void sendMetaData(int portNumber, InetAddress IPAddress, File file, String outputFile) throws IOException{
		// Creating a new metadata object
		MetaData metaDataObj = new MetaData();
		metaDataObj.setName(outputFile);
		metaDataObj.setSize((int) file.length());

		// Setting up the output stream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
		objectStream.writeObject(metaDataObj);

		// Convert object written to objectStream/outputStream to a byte array
		byte[] data = outputStream.toByteArray();

		// Create a new datagram packet
		DatagramPacket packet = new DatagramPacket(data, data.length, IPAddress, portNumber);

		// Create a socket
		socket = new DatagramSocket();

		// Send the packet over the socket
		socket.send(packet);
		System.out.println("SENDER: Metadata has been sent");
	}

	/* TODO: Send the file to the server without corruption*/
	public void sendFileNormal(int portNumber, InetAddress IPAddress, File file) throws IOException, ClassNotFoundException {
		// Set up the output objects
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		socket = new DatagramSocket();

		// Read the file into a buffer
		FileInputStream fileInputStream = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		int bytesToSend = fileInputStream.read(buffer);

		// Create a new segment
		Segment segment = new Segment();
		segment.setSq(0);
		segment.setType(SegmentType.Data);

		// Create indexes for iterating through the buffer
		int currentIndexOfBuffer = 0;
		int lastIndexOfBuffer = buffer.length;

		// Segment counters
		int segmentsToSend = (int) Math.ceil(file.length() / 4.0);
		int segmentsSent = 0;

		// Set up input objects
		byte[] ackData = new byte[1024];
		Segment ackSegment;

		// Send the rest of the segments
		while(bytesToSend > 0) {
			System.out.println(bytesToSend);
			// If there are less than 4 bytes left to send
			if(bytesToSend <= 4) {
				segment.setSize(bytesToSend);
				segment.setPayLoad(new String(buffer).substring(currentIndexOfBuffer, lastIndexOfBuffer));
			// More than 4 bytes
			} else {
				segment.setSize(4);
				String payload3 = new String(buffer).substring(currentIndexOfBuffer, currentIndexOfBuffer + 4);
				segment.setPayLoad(payload3);
			}
			segment.setChecksum(checksum(segment.getPayLoad(), false));

			objectOutputStream.writeObject(segment);
			byte[] data = outputStream.toByteArray();
			DatagramPacket outgoingPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
			System.out.println("\nSENDER: Start Sending File\n");
			System.out.println("----------------------------------------\n");
			System.out.println("SENDER: Sending segment: sq " + segment.getSq() + " size: " + segment.getSize() + " checksum: " + segment.getChecksum() + " content: " + segment.getPayLoad() + "\n");

			outgoingPacket.setData(data);
			outgoingPacket.setLength(data.length);
			socket.send(outgoingPacket);

			System.out.println("SENDER: Waiting for an ack\n");
			ackSegment = (Segment) receiveAck(socket, ackData).readObject();
			segmentsSent += 1;
			System.out.println("SENDER: ACK sq=" + ackSegment.getSq() + " RECEIVED.\n");
			System.out.println("SENDER: " + segmentsSent + " / " + segmentsToSend + " segments sent\n");
			System.out.println("----------------------------------------");

			bytesToSend -= 4;
			currentIndexOfBuffer += 4;

			// Alternate sequence number
			segment.setSq(1-segment.getSq());

			// Reset output stream
			outputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);

			}
		}

	/* TODO: This function is essentially the same as the sendFileNormal function
	 *      except that it resends data segments if no ACK for a segment is 
	 *      received from the server.*/
	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) throws IOException, ClassNotFoundException {
		// Set up the output objects
		socket = new DatagramSocket();
		socket.setSoTimeout(20);

		// Read the file into a buffer
		FileInputStream fileInputStream = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		int bytesToSend = fileInputStream.read(buffer);

		// Create a new segment
		Segment segment = new Segment();
		segment.setSq(0);
		segment.setType(SegmentType.Data);

		// Create indexes for iterating through the buffer
		int currentIndexOfBuffer = 0;
		int lastIndexOfBuffer = buffer.length;

		// Segment counters
		int segmentsToSend = (int) Math.ceil(file.length() / 4.0);
		int segmentsSent = 0;

		int retryNumber = 0;

		// Set up input objects
		byte[] ackData = new byte[1024];
		Segment ackSegment;

		System.out.println("\nSENDER: Start Sending File\n");
		System.out.println("----------------------------------------\n");

		// Main loop
		while(bytesToSend > 0) {

			boolean segmentSent = false;

			// If there are less than 4 bytes left to send
			if(bytesToSend <= 4) {
				segment.setSize(bytesToSend);
				segment.setPayLoad(new String(buffer).substring(currentIndexOfBuffer, lastIndexOfBuffer));
				// More than 4 bytes
			} else {
				segment.setSize(4);
				String payload3 = new String(buffer).substring(currentIndexOfBuffer, currentIndexOfBuffer + 4);
				segment.setPayLoad(payload3);
			}

			while(!(segmentSent)) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

				segment.setChecksum(checksum(segment.getPayLoad(), isCorrupted(loss)));

				objectOutputStream.writeObject(segment);
				byte[] data = outputStream.toByteArray();
				DatagramPacket outgoingPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
				System.out.println("SENDER: Sending segment: sq " + segment.getSq() + " size: " + segment.getSize() + " checksum: " + segment.getChecksum() + " content: " + segment.getPayLoad() + "\n");

				if(segment.getChecksum() == 0) {
					System.out.println("\t\t>>>>>>>Network ERROR: segment checksum is corrupted<<<<<<<\n");
				}



				outgoingPacket.setData(data);
				outgoingPacket.setLength(data.length);
				socket.send(outgoingPacket);


				try {

					// Receive Ack
					System.out.println("SENDER: Waiting for an ack\n");
					ackSegment = (Segment) receiveAck(socket, ackData).readObject();
					segmentsSent += 1;
					System.out.println("SENDER: ACK sq=" + ackSegment.getSq() + " RECEIVED.\n");
					System.out.println("SENDER: " + segmentsSent + " / " + segmentsToSend + " segments sent\n");
					System.out.println("----------------------------------------\n");
					bytesToSend -= 4;
					currentIndexOfBuffer += 4;

					// Alternate sequence number
					segment.setSq(1-segment.getSq());

					retryNumber = 0;
					segmentSent = true;

				} catch (SocketTimeoutException e) {
					retryNumber += 1;
					if(retryNumber == 3) {
						System.out.println("SENDER: TIMEOUT ALERT: Re-sending the same segment again, final attempt\n");
					} else if(retryNumber == RETRY_LIMIT) {
						System.out.println("SENDER: TIMEOUT ALERT: Re-submission limit reached, terminating transmission");
						System.exit(1);
					} else {
						System.out.println("SENDER: TIMEOUT ALERT: Re-sending the same segment again, current retry: " + retryNumber + "\n");
					}

				}
			}


		}
	}

	public ObjectInputStream receiveAck(DatagramSocket socket, byte[] ackData) throws IOException {
		DatagramPacket incomingPacket = new DatagramPacket(ackData, ackData.length);
		socket.receive(incomingPacket);
		byte[] payload = incomingPacket.getData();
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);

		return new ObjectInputStream(byteArrayInputStream);
	}
}