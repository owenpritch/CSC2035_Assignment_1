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
		System.out.println("SENDER: Metadata has been sent\n");
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

		// Send initial segment
		if(bytesToSend >= 0 && bytesToSend <= 4) {
			segment.setSize(bytesToSend);
			System.out.println("Bytes to send = " + bytesToSend);
			System.out.println("Current index = " + currentIndexOfBuffer);
			System.out.println("Last index = " + lastIndexOfBuffer);
			String payload = new String(buffer).substring(currentIndexOfBuffer, lastIndexOfBuffer);
			segment.setPayLoad(payload);
			System.out.println(currentIndexOfBuffer);
			segment.setChecksum(checksum(segment.getPayLoad(), false));
		} else {
			segment.setSize(4);
			System.out.println("Bytes to send = " + bytesToSend);
			System.out.println("Current index = " + currentIndexOfBuffer);
			System.out.println("Last index = " + lastIndexOfBuffer);
			String payload = new String(buffer).substring(currentIndexOfBuffer, currentIndexOfBuffer + 4);
			segment.setPayLoad(payload);
			segment.setChecksum(checksum(segment.getPayLoad(), false));
		}

		objectOutputStream.writeObject(segment);
        byte[] data = outputStream.toByteArray();
        DatagramPacket outgoingPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);

		System.out.println("packet - " + outgoingPacket);
		System.out.println("packet data - " + Arrays.toString(outgoingPacket.getData()));
		System.out.println("packet data length - " + outgoingPacket.getLength());
		System.out.println("SENDER: Sending file\n");
		socket.send(outgoingPacket);

		// Set up input objects
		byte[] ackData = new byte[1024];
		Segment ackSegment = new Segment();

		// Send the rest of the segments
		while(bytesToSend > 0) {

			// Receive Ack
			DatagramPacket incomingPacket = new DatagramPacket(ackData, ackData.length);
			socket.receive(incomingPacket);
			byte[] payload = incomingPacket.getData();
			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
			ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

			ackSegment = (Segment) objectInputStream.readObject();
			System.out.println("SENDER: A Segment with sq "+ ackSegment.getSq()+" is received: ");
			bytesToSend -= 4;
			System.out.println("Updated bytesToSend = " + bytesToSend);
			currentIndexOfBuffer += 4;
			System.out.println("Updated currentIndexOfBuffer = " + currentIndexOfBuffer);

			// Alternate sequence number
			segment.setSq(1-segment.getSq());

			// If there are less than 4 bytes left to send
			if(bytesToSend > 0 && bytesToSend <= 4) {
				segment.setSize(bytesToSend);
				System.out.println("Bytes to send = " + bytesToSend);
				System.out.println("Current index = " + currentIndexOfBuffer);
				System.out.println("Last index = " + lastIndexOfBuffer);
				String payload2 = new String(buffer).substring(currentIndexOfBuffer, lastIndexOfBuffer);
				segment.setPayLoad(payload2);
				System.out.println("Payload = " + segment.getPayLoad());
				segment.setChecksum(checksum(segment.getPayLoad(), false));
			// More than 4 bytes
			} else if(bytesToSend > 0){
				segment.setSize(4);
				System.out.println("Bytes to send = " + bytesToSend);
				System.out.println("Current index = " + currentIndexOfBuffer);
				System.out.println("Last index = " + lastIndexOfBuffer);
				System.out.println(fileInputStream.read(buffer));
				String payload3 = new String(buffer).substring(currentIndexOfBuffer, currentIndexOfBuffer + 4);
				segment.setPayLoad(payload3);
				System.out.println("Payload = " + segment.getPayLoad());
				segment.setChecksum(checksum(segment.getPayLoad(), false));
			}
			outputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(segment);
            data = outputStream.toByteArray();
            outgoingPacket.setData(data);
            outgoingPacket.setLength(data.length);
			socket.send(outgoingPacket);
		}
	} 

	/* TODO: This function is essentially the same as the sendFileNormal function
	 *      except that it resends data segments if no ACK for a segment is 
	 *      received from the server.*/
	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) throws IOException, ClassNotFoundException {
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

		// Set up input objects
		byte[] ackData = new byte[1024];
		Segment ackSegment = new Segment();

		// Send the rest of the segments
		while(bytesToSend > 0) {

			// If there are less than 4 bytes left to send
			if(bytesToSend <= 4) {
				segment.setSize(bytesToSend);
				System.out.println("Bytes to send = " + bytesToSend);
				System.out.println("Current index = " + currentIndexOfBuffer);
				System.out.println("Last index = " + lastIndexOfBuffer);
				String payload2 = new String(buffer).substring(currentIndexOfBuffer, lastIndexOfBuffer);
				segment.setPayLoad(payload2);
				System.out.println("Payload = " + segment.getPayLoad());
				segment.setChecksum(checksum(segment.getPayLoad(), false));
				// More than 4 bytes
			} else {
				segment.setSize(4);
				System.out.println("Bytes to send = " + bytesToSend);
				System.out.println("Current index = " + currentIndexOfBuffer);
				System.out.println("Last index = " + lastIndexOfBuffer);
				System.out.println(fileInputStream.read(buffer));
				String payload3 = new String(buffer).substring(currentIndexOfBuffer, currentIndexOfBuffer + 4);
				segment.setPayLoad(payload3);
				System.out.println("Payload = " + segment.getPayLoad());
				segment.setChecksum(checksum(segment.getPayLoad(), false));

				// Receive Ack
				DatagramPacket incomingPacket = new DatagramPacket(ackData, ackData.length);
				socket.receive(incomingPacket);
				byte[] payload = incomingPacket.getData();
				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
				ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);

				ackSegment = (Segment) objectInputStream.readObject();
				System.out.println("SENDER: A Segment with sq "+ ackSegment.getSq()+" is received: ");
				bytesToSend -= 4;
				System.out.println("Updated bytesToSend = " + bytesToSend);
				currentIndexOfBuffer += 4;
				System.out.println("Updated currentIndexOfBuffer = " + currentIndexOfBuffer);

				// Alternate sequence number
				segment.setSq(1-segment.getSq());
			}
			outputStream = new ByteArrayOutputStream();
			objectOutputStream = new ObjectOutputStream(outputStream);
			objectOutputStream.writeObject(segment);

			// Send initial segment
			if(bytesToSend <= 4) {
				segment.setSize(bytesToSend);
				System.out.println("Bytes to send = " + bytesToSend);
				System.out.println("Current index = " + currentIndexOfBuffer);
				System.out.println("Last index = " + lastIndexOfBuffer);
				String payload = new String(buffer).substring(currentIndexOfBuffer, lastIndexOfBuffer);
				segment.setPayLoad(payload);
				System.out.println(currentIndexOfBuffer);
				segment.setChecksum(checksum(segment.getPayLoad(), false));
			} else {
				segment.setSize(4);
				System.out.println("Bytes to send = " + bytesToSend);
				System.out.println("Current index = " + currentIndexOfBuffer);
				System.out.println("Last index = " + lastIndexOfBuffer);
				String payload = new String(buffer).substring(currentIndexOfBuffer, currentIndexOfBuffer + 4);
				segment.setPayLoad(payload);
				segment.setChecksum(checksum(segment.getPayLoad(), false));
			}

			objectOutputStream.writeObject(segment);
			byte[] data = outputStream.toByteArray();
			DatagramPacket outgoingPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);

			System.out.println("packet - " + outgoingPacket);
			System.out.println("packet data - " + Arrays.toString(outgoingPacket.getData()));
			System.out.println("packet data length - " + outgoingPacket.getLength());
			System.out.println("SENDER: Sending file\n");

			outgoingPacket.setData(data);
			outgoingPacket.setLength(data.length);
			socket.send(outgoingPacket);
		}
	}
}