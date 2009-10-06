/**
 * 
 */
package artnet4j;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import artnet4j.packets.ArtPollPacket;
import artnet4j.packets.ArtPollReplyPacket;

public class ArtNetNodeDiscovery extends Thread {

	public static final int POLL_INTERVAL = 10000;

	public static final Logger logger = Logger
	.getLogger(ArtNetNodeDiscovery.class.getClass().getName());

	protected final ArtNet artNet;
	protected ConcurrentHashMap<InetAddress, ArtNetNode> discoveredNodes = new ConcurrentHashMap<InetAddress, ArtNetNode>();
	protected List<ArtNetNode> lastDiscovered = new ArrayList<ArtNetNode>();
	protected List<ArtNetDiscoveryListener> listeners = new ArrayList<ArtNetDiscoveryListener>();

	protected boolean isActive = true;

	protected long discoveryInterval;

	public ArtNetNodeDiscovery(ArtNet artNet) {
		this.artNet = artNet;
		setInterval(POLL_INTERVAL);
	}

	public void addListener(ArtNetDiscoveryListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	public void discoverNode(ArtPollReplyPacket reply) {
		InetAddress nodeIP = reply.getIPAddress();
		ArtNetNode node = discoveredNodes.get(nodeIP);
		if (node == null) {
			logger.info("discovered new node: " + nodeIP);
			node = reply.getNodeStyle().createNode();
			discoveredNodes.put(nodeIP, node);
			for(ArtNetDiscoveryListener l : listeners) {
				l.discoveredNewNode(node);
			}
		}
		lastDiscovered.add(node);
		node.extractConfig(reply);
	}

	public void removeListener(ArtNetDiscoveryListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	@Override
	public void run() {
		try {
			while (isActive) {
				lastDiscovered.clear();
				ArtPollPacket poll = new ArtPollPacket();
				artNet.broadcastPacket(poll);
				Thread.sleep(ArtNet.ARTPOLL_REPLY_TIMEOUT);
				if (isActive) {
					synchronized (listeners) {
						for(ArtNetNode node : discoveredNodes.values()) {
							if (!lastDiscovered.contains(node)) {
								discoveredNodes.remove(node.getIPAddress());
								for (ArtNetDiscoveryListener l : listeners) {
									l.discoveredNodeDisconnected(node);
								}
							}
						}
						for (ArtNetDiscoveryListener l : listeners) {
							l.discoveryCompleted(new ArrayList<ArtNetNode>(
									discoveredNodes.values()));
						}
					}
					Thread.sleep(discoveryInterval
							- ArtNet.ARTPOLL_REPLY_TIMEOUT);
				}
			}
		} catch (InterruptedException e) {
			logger.warning("node discovery interrupted");
		}
	}

	public void setInterval(int interval) {
		discoveryInterval = Math.max(interval, ArtNet.ARTPOLL_REPLY_TIMEOUT);
	}
}