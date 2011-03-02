################################################################################
##
##  ADOBE SYSTEMS INCORPORATED
##  Copyright 2008 Adobe Systems Incorporated
##  All Rights Reserved.
##
##  NOTICE: Adobe permits you to use, modify, and distribute this file
##  in accordance with the terms of the license agreement accompanying it.
##
################################################################################

#
# This shell script sets the environment variables JAVA_HOME, ANT_HOME,
# and PATH appropriately for building, testing, and developing
# with this branch of the Flex SDK.
#
# It ensures that the right version of Java and Ant are used
# and that when you invoke a Flex SDK tool such as mxmlc,
# the right copy of that tool is executed.
#
# Usage:
#   cd <some_branch_of_the_Flex_SDK>
#   source setup.sh
#
# It assumes that Java and Ant are installed at standard locations
# in your system. If you have placed them elsewhere, edit the paths.
#
# Note that you must run this script with the 'source' command.
#
# After running it, you can execute commands such as
#   ant -q main checkintests
# and
#   mxmlc MyApp.mxml
#

# IMPORTANT: If you update this script, be sure to update README.txt

# Which platform are we on?
case `uname` in
	CYGWIN*)
		OS="Windows"
	;;
	*)
		OS="Unix"
esac

# Remember what the PATH is before we prepend any branch-specific directories,
# so that when we switch to another branch we can start over.
if [ "$ORIG_PATH" = "" ]; then
	ORIG_PATH="$PATH"
fi

# Use the right version of Java and Ant to build this branch
if [ $OS = "Windows" ]; then
	JAVA_HOME="/cygdrive/c/j2sdk1.4.2_14"
	ANT_HOME="/cygdrive/c/apache-ant-1.6.2"

elif [ $OS = "Unix" ]; then
	JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Home"
	ANT_HOME=~/bin/apache-ant-1.6.2
fi

echo "setup.sh: Setting default ANT_HOME=$ANT_HOME"
echo "setup.sh: Setting default JAVA_HOME=$JAVA_HOME"

# Ensure that this branches' tools (such as bin/mxmlc) are found.
PATH="`pwd`/bin:$ANT_HOME/bin:$JAVA_HOME/bin:$ORIG_PATH"

# test that each path exists
for aPath in "$ANT_HOME" "$JAVA_HOME";
do
    if [ ! -d "$aPath" ]; then
        echo "setup.sh: WARNING: path does not exist:"
        echo "    ${aPath}"
        echo
    fi
done

