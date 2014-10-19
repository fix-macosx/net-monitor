#!/bin/sh

#
# Ugly script to rewrite all external library references
# using @rpath
#

if [ ! -f "$1" ] || [ -z "$2" ]; then
	echo "Usage: <path to sslsplit> <output dir>"
	exit 1
fi

BINARY=$1
OUTPUT_DIR=$2
BIN_DIR=$OUTPUT_DIR/bin
LIB_DIR=$OUTPUT_DIR/lib/sslsplit
OUTPUT_BINARY=$BIN_DIR/`basename $BINARY`

if [ ! -d "${LIB_DIR}" ]; then
	mkdir -p "${LIB_DIR}" || exit 1
fi

if [ ! -d "${BIN_DIR}" ]; then
	mkdir -p "${BIN_DIR}" || exit 1
fi

unwritten_libraries() {
	local target=$1
	otool -L "$target" | tail +2 | egrep '(libssl|libcrypto|libz|libevent)' | grep -v '@rpath/' | awk '{print $1}'
}

rewrite_dependencies() {
	local target=$1
	local rpath=$2

	install_name_tool -add_rpath "$rpath" "$target" || exit 1

	for l in $(unwritten_libraries $target); do
		install_name_tool -change "$l" @rpath/$(basename $l) "$target" || exit 1
		if [ "$l" -nt "$LIB_DIR/"`basename $l` ]; then
			install -m 644 "$l" "${LIB_DIR}/" || exit 1
			rewrite_dependencies "${LIB_DIR}/$(basename $l)" "@loader_path"
		fi
	done
}

install -m 755 "${BINARY}" "${OUTPUT_BINARY}" || exit 1
rewrite_dependencies "${OUTPUT_BINARY}" "@loader_path/../lib/sslsplit"
