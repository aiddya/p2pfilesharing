JCC = javac
JFLAGS = -g
PKGDIR = cnp2p

default: Config.class\
	ConnectionHandler.class\
	HandshakeMessage.class\
	Logger.class\
	Main.class\
	Message.class\
	MessageType.class\
	Peer.class\
	Tracker.class\
	peerProcess.class

Config.class: $(PKGDIR)/Config.java
	$(JCC) $(JFLAGS) $(PKGDIR)/Config.java

ConnectionHandler.class: $(PKGDIR)/ConnectionHandler.java
	$(JCC) $(JFLAGS) $(PKGDIR)/ConnectionHandler.java

HandshakeMessage.class: $(PKGDIR)/HandshakeMessage.java
	$(JCC) $(JFLAGS) $(PKGDIR)/HandshakeMessage.java

Logger.class: $(PKGDIR)/Logger.java
	$(JCC) $(JFLAGS) $(PKGDIR)/Logger.java

Main.class: $(PKGDIR)/Main.java
	$(JCC) $(JFLAGS) $(PKGDIR)/Main.java

Message.class: $(PKGDIR)/Message.java
	$(JCC) $(JFLAGS) $(PKGDIR)/Message.java

MessageType.class: $(PKGDIR)/MessageType.java
	$(JCC) $(JFLAGS) $(PKGDIR)/MessageType.java

Peer.class: $(PKGDIR)/Peer.java
	$(JCC) $(JFLAGS) $(PKGDIR)/Peer.java

Tracker.class: $(PKGDIR)/Tracker.java
	$(JCC) $(JFLAGS) $(PKGDIR)/Tracker.java

peerProcess.class: peerProcess.java
	$(JCC) $(JFLAGS) peerProcess.java

clean:
	$(RM) *.class
	$(RM) $(PKGDIR)/*.class
