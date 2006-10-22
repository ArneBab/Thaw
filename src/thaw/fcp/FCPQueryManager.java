package thaw.fcp;

import java.util.Observable;

import thaw.core.Logger;

/**
 * Manage all fcp messages (see corresponding object of each kind of query).
 * Call observers each type a new message is received. The given object is
 * the 
 */
public class FCPQueryManager extends Observable implements Runnable {
	private Thread me;

	private FCPConnection connection;
	private FCPMessage latestMessage;


	public FCPQueryManager(FCPConnection connection) {
		this.me = null;
		this.latestMessage = null;
		this.setConnection(connection);
	}

	/**
	 * If you call yourself this function, you will probably have to call
	 * resetQueue() of FCPQueueManager.
	 */
	public void setConnection(FCPConnection connection) {
		this.connection = connection;
	}

	/**
	 * Try to not directly call functions from FCPConnection.
	 */
	public FCPConnection getConnection() {
		return this.connection;
	}

	public boolean writeMessage(FCPMessage message) {
		return this.connection.write(message.toString());
	}

	public boolean writeMessage(FCPMessage message, boolean checkLock) {
		return this.connection.write(message.toString(), checkLock);

	}

	/**
	 * Blocking until a message is reveived.
	 * More exactly, read until "Data\n" or "EndMessage\n" is read.
	 */
	public FCPMessage readMessage() {
		String whatsUp = new String("");
		FCPMessage result = new FCPMessage();
		boolean withData;
		
		withData = false;

		while(true) {

			String read = new String("");

			read = this.connection.readLine();
			
			if(read == null) {
				Logger.notice(this, "readLine() returned null => disconnected ?");
				return null;
			}

			if("Data".equals( read )) {
				withData = true;
				break;
			}

			if("EndMessage".equals( read )) {
				break;
			}

			whatsUp = whatsUp + read + "\n";
		}

		Logger.verbose(this, "Parsing message ...");

		result.loadFromRawMessage(whatsUp);

		if(withData) {
			long dataWaiting = (new Long(result.getValue("DataLength"))).longValue();
			this.connection.setRawDataWaiting(dataWaiting);
			Logger.info(this, "Achtung data: "+(new Long(dataWaiting)).toString());			
		}

		return result;
	}


	/**
	 * Will listen in loop for new incoming messages.
	 */
	public void run() {

		while(true) {
			this.latestMessage = this.readMessage();
			
			Logger.verbose(this, "Message received. Notifying observers");

			if(this.latestMessage != null) {
				try {
					this.setChanged();
					this.notifyObservers(this.latestMessage);
				} catch(Exception e) {
					/* it's really bad ... because if data are waiting on the socket ... */
					Logger.error(this, "EXCEPTION FROM ONE OF LISTENER : "+e.toString());
					Logger.error(this, "ERROR : "+e.getMessage());
					e.printStackTrace();						     
				}
			} else {
				Logger.info(this, "Stopping listening");
				return;
			}
		}

	}

	
	/**
	 * Create the thread listening for incoming message.
	 */
	public void startListening() {
		if(this.connection.isConnected()) {
			this.me = new Thread(this);
			this.me.start();
		} else {
			Logger.warning(this, "Not connected, so not listening on the socket");
		}
	}

	
	/**
	 * This function is mainly used by FCPClientGet to have a separate socket to transfer the files.
	 * If FCPConnection is allowed to duplicate itself, then it will duplicate it and create a dedicated FCPQueryManager for.
	 * A FCPClientHello is sent with the given id.
	 * @return This object if it cannot duplicate FCPConnection
	 */
	public FCPQueryManager duplicate(String connectionId) {
		FCPConnection newConnection;
		FCPQueryManager queryManager;

		newConnection = this.connection.duplicate();

		if (newConnection == this.connection)
			return this;

		queryManager = new FCPQueryManager(newConnection);

		queryManager.startListening();

		FCPClientHello clientHello = new FCPClientHello(queryManager, connectionId);
		
		if (!clientHello.start(null)) {
			Logger.warning(this, "ID already used ?! Using initial socket ...");
			newConnection.disconnect();
			return this;
		}

		return queryManager;
	}

}

