#!/bin/sh

if [ `id -u` != '0' ]; then
	echo "This script must be run as root"
	exit 1
fi

# If it does not already exist, a `_packets` group that we'll use to exempt
# sslsplit from our filter rules.
dscl -q . -read /Groups/_packets >/dev/null 2>&1
if [ $? != 0 ]; then
	echo "Adding _packets group"
	dscl . -create /Groups/_packets || exit 1
	dscl . -create /Groups/_packets PrimaryGroupID 100001 || exit 1
fi

# Add our pf ruleset; note that this also backs up and overwrites (but should be compatible with)
# Apple's default configuration.
now=$(date +"%Y%m%d%S")
echo "Copying /etc/pf.conf to /etc/pf.conf.$now"
cp /etc/pf.conf /etc/pf.conf.$now || exit 1

echo "Installing new /etc/pf.conf and /etc/pf.anchors/com.fix-macosx"
install -m 644 pf/pf.conf /etc/pf.conf || exit 1
install -m 644 pf/com.fix-macosx /etc/pf.anchors/com.fix-macosx || exit 1

# Enable pf at launch (from http://support.apple.com/kb/HT200259?viewlocale=en_US)
echo "Enabling pf at boot"
defaults write /System/Library/LaunchDaemons/com.apple.pfctl ProgramArguments '(pfctl, -f, /etc/pf.conf, -e)' || exit 1
plutil -convert xml1 /System/Library/LaunchDaemons/com.apple.pfctl.plist || exit 1
chmod 644 /System/Library/LaunchDaemons/com.apple.pfctl.plist || exit 1

# Generating a CA Certificate
if [ ! -f /usr/local/etc/sslsplit/cert.pem ]; then
	echo "Generating SSLSplit CA"
	install -d -m 700 /usr/local/etc/sslsplit
	openssl genrsa -out /usr/local/etc/sslsplit/key.pem 1024 || exit 1
	chmod 600 /usr/local/etc/sslsplit/key.pem || (rm -f /usr/local/etc/sslsplit/key.pem && exit 1)

	openssl req -new -nodes -x509 -sha1 -out /usr/local/etc/sslsplit/cert.pem -key /usr/local/etc/sslsplit/key.pem \
		-config conf/x509v3ca.cnf -extensions v3_ca \
		-subj '/O=SSLsplit Root CA/CN=SSLsplit Root CA/' \
		-set_serial 0 -days 3650 || exit 1

	echo "Installing SSLSplit as trusted system CA"
	security add-trusted-cert -d /usr/local/etc/sslsplit/cert.pem || exit 1
fi

# Install SSLSplit
echo "Installing SSLSplit"
install -d -m 755 /usr/local/bin /usr/local/lib/sslsplit || exit 1
install -m 755 bin/sslsplit /usr/local/bin || exit 1
install -m 644 lib/sslsplit/* /usr/local/lib/sslsplit || exit 1
install -m 644 conf/com.fix-macosx.sslsplit.plist /Library/LaunchDaemons/ || exit 1

launchctl load /Library/LaunchDaemons/com.fix-macosx.sslsplit.plist || exit 1
pfctl -f /etc/pf.conf
pfctl -e  || exit 1
