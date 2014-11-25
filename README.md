# Capture ALL The Things

Net-Monitor (NM) is toolkit for auditing "phone home" behavior of all user and
system-level processes on Mac OS X Yosemite.

Example data extracted by Net-Monitor is provided for collaborative review
and analysis via the [Yosemite Phone Home](https://github.com/fix-macosx/yosemite-phone-home)
project.

Features include:

* Transparent plaintext logging of TCP/TLS/HTTPS traffic via pf(4) and a custom version of SSLSplit. No custom proxy configuration is required.
* Automatic correlation of connections with initiating application, user, and group.
* Logging of non-TCP traffic via pf(4), pflog(4), and tcpdump.
* Automatic generation and trust of a local, per-machine MITM certificate authority.

By default, NM generates the following logs:

* TCP/SSL: /var/log/sslsplit/<application path>/<time>-<src>-<dest>.log
* UDP/other: /var/log/udp-monitor/\*.pcap

NM relies on [SSLsplit](http://www.roe.ch/SSLsplit) to provide TLS introspection; all
of our previous [local patches](https://github.com/fix-macosx/sslsplit) have been
integrated upstream.

Additional contributions to improve accuracy/transparency of the collected data are always
very welcome.

## Caveats

* NM is intended to be used on a dedicated VM or research installation; it
overrides default configuration files and interposes itself in TLS network communications,
and is *not* currently recommended for day-to-day use.
* Correlation of sockets, processes, and file system executable paths is imperfect; there
are cases where connections will be ascribed to the wrong application path.
* TLS traffic using client certificates cannot be captured in plaintext by default. For
example, NM captures the key exchange performed by apsd (Apple Push Services Daemon),
that establishes a client certificate, but NM can't transparently sniff future communications
protected by that certificate without the addition of apsd-specific protocol handling.

## Developing

Installation is handled entirely by `install.sh`; the pf(4) and launchd configuration files
may be found in `conf/`.

To update the embedded copy of SSLsplit:

* Clone and build [sslsplit](https://github.com/droe/sslsplit) locally.
* Update NM's standalone sslsplit binary via `sslsplit-create-standalone.sh`, e.g., `sslsplit-create-standalone.sh ~/sslsplit/sslsplit ~/net-monitor`

## Uninstalling

* restore your old packet filter config file, the install script will back it up in a file called /etc/pf.conf.$dateofinstall
* check the install script for all the plist settings, you want to undo all the writes and delete these settings (launchctl unload and disable)
* remove the executable: /usr/local/lib/sslsplit and the logs /var/log/udp-monitor
