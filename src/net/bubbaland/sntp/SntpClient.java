package net.bubbaland.sntp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * SNTP client algorithm, specified in RFC 4330.
 */
public class SntpClient {

	/**
	 * @return the offset
	 */
	public Duration getOffset() {
		return this.offset != null ? this.offset : Duration.ZERO;
	}

	// Weight of the most recent packet on the offset
	private static double					avgWeight	= 0.5;

	private final int						port;
	private Duration						pollInterval;
	private final String					host;

	// offset = server - client
	private Duration						offset;
	private Timer							timer;

	private final ArrayList<SntpListener>	listeners;

	public SntpClient(final String host, final int port, final Duration pollInterval) {
		this.port = port;
		this.host = new String(host);
		this.pollInterval = pollInterval;

		this.offset = null;
		this.timer = null;

		this.listeners = new ArrayList<SntpListener>(0);

		System.out.println("SNTP client configured to synchronize with  " + this.host + ":" + this.port);
	}

	public void start() {
		this.timer = new Timer();
		this.timer.schedule(new TimerTask() {
			@Override
			public void run() {
				SntpClient.this.sync();
			}
		}, 0, this.pollInterval.toMillis());
	}

	public void stop() {
		this.timer.cancel();
		this.timer = null;
	}

	public void sync() {
		try {
			if (this.port <= 0 || this.host == null) {
				throw new IllegalArgumentException("Invalid parameters!");
			}

			final DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(10000);
			final InetAddress hostAddr = InetAddress.getByName(this.host);

			// Create request
			SntpMessage req = new SntpMessage();
			req.setVersion((byte) 4);
			req.setMode((byte) 3); // client
			req.setStratum((byte) 3);
			req.setRefId("LOCL".getBytes());
			req.setXmtTime(NtpTimestamp.now()); // returns as originate timestamp
			final byte[] buffer = req.toByteArray();
			req = null;

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, hostAddr, this.port);
			socket.send(packet);

			packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);

			// Set the time response arrived
			final NtpTimestamp destTime = NtpTimestamp.now();

			final SntpMessage resp = new SntpMessage(packet.getData());
			socket.close();

			// Timestamp Name ID When Generated
			// ------------------------------------------------------------
			// Originate Timestamp T1 time request sent by client
			// Receive Timestamp T2 time request received by server
			// Transmit Timestamp T3 time reply sent by server
			// Destination Timestamp T4 time reply received by client
			//
			// The roundtrip delay d, and system clock offset t are defined as:
			//
			// d = (T4 - T1) - (T3 - T2) t = ((T2 - T1) + (T3 - T4)) / 2

			final double t = ( ( resp.getRecTime().value - resp.getOrgTime().value )
					+ ( resp.getXmtTime().value - destTime.value ) ) / 2;

			// System clock offset in millis
			final Duration newOffset = Duration.ofNanos((long) ( t * 1000000000 ));
			if (this.offset == null) {
				this.offset = newOffset;
			} else {
				this.offset = newOffset.dividedBy((long) ( 1.0 / avgWeight ))
						.plus(this.offset.dividedBy((long) ( 1.0 / ( 1 - avgWeight ) )));
				// this.offset = ( 1 - avgWeight ) * this.offset + avgWeight * newOffset;
			}

			// System.out.println("Average Offset: " + this.offset + " ms");
			SntpClient.this.onSync(Instant.now());
		} catch (final Exception e) {
			System.out.println("Error communicating with SNTP server");
			SntpClient.this.onError(Instant.now());
			return;
		}
	}

	public Duration getPollInteval() {
		return this.pollInterval;
	}

	public void setPollInteval(final Duration pollInteval) {
		this.pollInterval = pollInteval;
	}

	public void onError(final Instant when) {
		this.listeners.parallelStream().forEach(l -> l.onSntpError(when));
	}

	public void onSync(final Instant when) {
		this.listeners.parallelStream().forEach(l -> l.onSntpSync(when));
	}

	public void addSntpListener(final SntpListener newListener) {
		this.listeners.add(newListener);
	}

	public void removeSntpListener(final SntpListener oldListener) {
		this.listeners.remove(oldListener);
	}

}