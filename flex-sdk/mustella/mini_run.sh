cd $FLEX_HOME/sdk/mustella

echo export TMP_FLEX_HOME=$FLEX_HOME >env.tmp
sed s/[\\]/\\//g env.tmp >env2.tmp
. env2.tmp
echo $TMP_FLEX_HOME

cd ../../qa/sdk/testsuites/mustella

##
## mini_run.sh - does an ant run on subdirectories.  Assumes you have synced
##            //depot/flex/qa/frameworks/mustella
##            //depot/flex/qa/sdk/testsuites/mustella (or a relevant portion thereof)
##

if [ $# -lt 1 ]
	then
	echo "usage: mini_run.sh [FLAGS] <dir>|<file> ... "
	echo ""
	echo "   where [FLAGS] represents optional flags (before other args). Currently supported: "
	echo ""
	echo "   -skipcheck - will skip checks for keywords and duplicate testIDs"
	echo ""
	echo "   where <dir> represents a top-level test directory. For example: "
	echo "   components/Menu"
	echo ""
	echo "   or where <file> represents a specific test file. For example: "
	echo "   components/Menu/Properties/Menu_Properties.mxml"
	echo ""
	echo "   You can specify any number of directories or files. (Delimited by spaces)"
	exit
fi

if [ "$ANT_HOME" = "" ] 
	then
	echo "ANT_HOME is not set. Please set this variable"
	exit
fi

propfile=../../../properties/mustella_tmp.properties
inherit_propfile=../../../properties/mustella1.properties

run_one_file=""

skipcheck=1
skipexclude=false
sourcepath=$TMP_FLEX_HOME/sdk/frameworks/projects/framework/src
echo ${sourcepath}
mxmlcargs="\"-verbose-stacktraces --source-path=${sourcepath}\""

for i in $*
do
		if [ "$i" = "-skipcheck" ]
			then
			skipcheck=1		
			continue
		fi
		if [ "$i" = "-skipexclude" ]
			then
			skipexclude=true
			continue
		fi


		### okay, we dug this up, if it's a -f, we should 
		### set a -D property for ant run_this_script. 
		### otherwise, run a whole directory
		### the other args are the same, however. :(

		tmpx=tests/${i}
		echo "i=$i"
		echo "Looking for $tmpx"


		if [ -f "$tmpx" ]
			then


			echo "It's a file"

			dir=`dirname $i`
			dir=`dirname $dir`


			end=`echo $tmpx | awk -F"." '{print $NF}'`

			## if it's not a .png file, just run the file. If it is a png file, we'll generalize the run
			## to the directory (not knowing any better)
			if [ "$end" != ".png" ]
				then
						
				## append to a semicolon delimted list of individual files
				if [ "$run_one_file" != "" ]
					then
					tmpz=`basename $i`
					run_one_file="${run_one_file};${tmpz}"
					# echo "$run_one_file"
				else
					run_one_file=`basename $i`
					# echo "$run_one_file"
				fi
			fi

			### 
			### 
			if [ "$sdk_mustella_includes" != "" ]
				then
				sdk_mustella_includes="${sdk_mustella_includes},${dir}/**/*.mxml"
				sdk_mustella_swfs="${sdk_mustella_swfs},${dir}/**/*.swf" 
			else
				sdk_mustella_includes="${dir}/**/*.mxml"
				sdk_mustella_swfs="${dir}/**/*.swf"
			fi


		elif [ -d "$tmpx" ]
			then

			echo "It was a directory"
			if [ "$sdk_mustella_includes" != "" ]
				then
				sdk_mustella_includes="${sdk_mustella_includes},${i}/**/*.mxml"
				sdk_mustella_swfs="${sdk_mustella_swfs},${i}/**/*.swf" 
			else
				sdk_mustella_includes="${i}/**/*.mxml"
				sdk_mustella_swfs="${i}/**/*.swf"
			fi

		else
		
			echo "Not found"

		fi



done


rm  $propfile  2>/dev/null

echo "propfile: $propfile"


if [ "$sdk_mustella_includes" = "" ]
	then
	echo "Nothing was found to include. Will exit"
	exit 1
fi


egrep "sdk.mustella.excludes" $inherit_propfile > $propfile
echo "sdk.mustella.includes=${sdk_mustella_includes}" >> $propfile
echo "sdk.mustella.swfs=${sdk_mustella_swfs}" >> $propfile



# avoid race to know where the build lives (hack)
mkdir build 2>/dev/null
mkdir build/lib 2>/dev/null

## give a run id so it doesn't bother getting one (we don't care).
if [ "$skipcheck" = "0" ]
	then
	echo "Running testcase checks...."
	
	if [ "$TEST_REVIEW" = "1" ] 
		then
		echo "$ANT_HOME/bin/ant -Dsend_check_email=true -Ddebug_insert=true -Dfeature=mustella_tmp insert_tests -Dplayer.dir=$FLEX_HOME/sdk/in/player/win -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}"
		$ANT_HOME/bin/ant -Dsend_check_email=true -Ddebug_insert=true -Dfeature=mustella_tmp insert_tests -Dplayer.dir=$FLEX_HOME/sdk/in/player/win -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}
		ret=$?
	else
		echo "$ANT_HOME/bin/ant -Ddebug_insert=true  -Dfail_on_testcase_check=true -Dfeature=mustella_tmp insert_tests -Dplayer.dir=$FLEX_HOME/sdk/in/player/win" -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}
		$ANT_HOME/bin/ant -Ddebug_insert=true  -Dfail_on_testcase_check=true -Dfeature=mustella_tmp insert_tests -Dplayer.dir=$FLEX_HOME/sdk/in/player/win -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}
		ret=$?
	fi

	if [ "$ret" != "0" ]
		then
		echo "Bad return from testcase check. exiting."
		exit 1
	fi
else
	echo "Skipping testcase check"

fi




if [ "$TEST_REVIEW" = "1" ] 
	then

	echo "Doing this as a Test Review. (always mainline)"

	## let it mail a compile error	
	## we'll do a comparison, so we need the database

	echo "$ANT_HOME/bin/ant -Drun_type=mini -Dbranch_name=Mainline -Dexit_on_compile_error=false -Drun_this_script=\"$run_this_script\" -Dinsert_results=true -Dfeature=mustella_tmp -Dsubject=\"$subject\"  -Dsocket_mixin=SocketAddress sendResults -Dplayer.dir=$FLEX_HOME/sdk/in/player/win" -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}
	$ANT_HOME/bin/ant -Drun_type=mini -Dbranch_name=Mainline -Dexit_on_compile_error=false -Drun_this_script="$run_this_script" -Dinsert_results=true -Dfeature=mustella_tmp -Dsubject="$subject"  -Dsocket_mixin=SocketAddress sendResults -Dplayer.dir=$FLEX_HOME/sdk/in/player/win -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}


else

	echo "Doing a regular mini run"

	echo $ANT_HOME/bin/ant -Drun_this_script="$run_one_file" -Dcurrent.run.id=-1 -Dexit_on_compile_error=true -Dinsert_results=false  -Dokay_to_exit=true -Dskip_exclude=${skipexclude} -Dfeature=mustella_tmp run  -Dplayer.dir=$FLEX_HOME/sdk/in/player/win -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}

	$ANT_HOME/bin/ant -Drun_this_script="$run_one_file" -Dcurrent.run.id=-1 -Dexit_on_compile_error=true -Dinsert_results=false -Dokay_to_exit=true -Dskip_exclude=${skipexclude} -Dfeature=mustella_tmp run  -Dplayer.dir=$FLEX_HOME/sdk/in/player/win -Dsdk.dir=$FLEX_HOME/sdk -Dmxmlc.args=${mxmlcargs}

fi


cd ../../../../sdk/mustella






