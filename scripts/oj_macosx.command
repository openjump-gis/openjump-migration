#/bin/sh
##
# See oj_linux.sh for some parameters you might want to tune
# e.g. max memory, language, jre to use
##
CDIR=`dirname "$0"`
## define a default look&feel(laf)
#  "" - empty for system default
#  "javax.swing.plaf.metal" - for a cross platform ui, if problems occure
JAVA_LOOKANDFEEL=""
## some parameters to make OJ shiny ;)
JAVA_OPTS_OVERRIDE="-Xdock:name=OpenJUMP -Xdock:icon=./bin/OpenJUMP.app/Contents/Resources/oj.icns"

# Export environment variables for C-coded functions.
export DYLD_LIBRARY_PATH="$DYLD_LIBRARY_PATH:./lib/native:./lib/ext"
# we have only x86 compiled osx native libs, try force 32bit jre
JAVA_OPTS_OVERRIDE="$JAVA_OPTS_OVERRIDE -d32"

## run the real magic now
. "$CDIR/oj_linux.sh"
